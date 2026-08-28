package com.xiaozhi.ai.stt.providers;

import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriber;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberListener;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberResponse;
import com.xiaozhi.ai.stt.SttResult;
import com.xiaozhi.ai.stt.SttService;
import com.xiaozhi.common.port.TokenResolver;
import com.xiaozhi.common.model.bo.ConfigBO;
import reactor.core.publisher.Flux;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;
/**
 * Serviço de reconhecimento de voz em tempo real Alibaba Cloud NLS
 * Implementa a funcionalidade STT usando o SDK de Interação de Voz Inteligente da Alibaba Cloud
 * Documentação de referência: https://help.aliyun.com/zh/isi/developer-reference/sdk-for-java-8
 */
@Slf4j
public class AliyunNlsSttService implements SttService {
    private static final String PROVIDER_NAME = "aliyun-nls";

    // URL padrão do serviço Alibaba Cloud NLS
    private static final String NLS_URL = "wss://nls-gateway.aliyuncs.com/ws/v1";

    // Tempo de timeout
    private static final long RECOGNITION_TIMEOUT_MS = 90000; // Tempo limite de reconhecimento (90 segundos)

    /**
     * Cache global de NlsClient (compartilhado por configId)
     */
    private static final ConcurrentHashMap<Integer, CachedNlsClient> globalClientCache = new ConcurrentHashMap<>();

    /**
     * Classe wrapper do NlsClient em cache
     */
    private static class CachedNlsClient {
        final NlsClient client;
        final int tokenHash;

        CachedNlsClient(NlsClient client, int tokenHash) {
            this.client = client;
            this.tokenHash = tokenHash;
        }
    }

    // Configuração da Alibaba Cloud
    private final ConfigBO config;

    // Gerenciador de Token
    private final TokenResolver tokenResolver;

    public AliyunNlsSttService(ConfigBO config, TokenResolver tokenResolver) {
        this.config = config;
        this.tokenResolver = tokenResolver;
    }

    /**
     * Obtém ou cria uma instância de NlsClient (suporta reutilização de conexão)
     */
    private NlsClient getOrCreateClient() throws Exception {
        String currentToken = tokenResolver.getToken(config);
        if (currentToken == null) {
            throw new RuntimeException("Não foi possível obter o Token da Alibaba Cloud");
        }

        Integer configId = config.getConfigId();
        int currentHash = currentToken.hashCode();

        CachedNlsClient cached = globalClientCache.get(configId);
        if (cached != null && cached.tokenHash == currentHash) {
            return cached.client;
        }

        return globalClientCache.compute(configId, (k, existing) -> {
            if (existing != null && existing.tokenHash == currentHash) {
                return existing;
            }
            if (existing != null) {
                try {
                    existing.client.shutdown();
                } catch (Exception e) {
                    log.warn("Falha ao fechar o NlsClient antigo", e);
                }
            }
            NlsClient newClient = new NlsClient(NLS_URL, currentToken);
            return new CachedNlsClient(newClient, currentHash);
        }).client;
    }

    /**
     * Limpa o cache de NlsClient para o configId especificado
     */
    public static void clearClientCache(Integer configId) {
        if (configId == null) {
            return;
        }
        CachedNlsClient removed = globalClientCache.remove(configId);
        if (removed != null) {
            try {
                removed.client.shutdown();
            } catch (Exception e) {
                log.warn("Falha ao fechar o NlsClient", e);
            }
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public SttResult stream(Flux<byte[]> audioSink) {
        if (audioSink == null) {
            log.error("Fluxo de dados de áudio vazio");
            return SttResult.textOnly("");
        }

        // Usado para coletar o resultado do reconhecimento
        StringBuilder resultBuilder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);

        // Usado para indicar se o reconhecimento foi concluído
        AtomicBoolean recognitionCompleted = new AtomicBoolean(false);
        AtomicBoolean recognitionFailed = new AtomicBoolean(false);

        // Usado para armazenar informações de erro
        AtomicBoolean[] errorHolder = new AtomicBoolean[]{new AtomicBoolean(false)};

        NlsClient client = null;
        SpeechTranscriber transcriber = null;

        try {
            // Obtém ou reutiliza o NlsClient
            client = getOrCreateClient();

            // Cria o listener de reconhecimento
            SpeechTranscriberListener listener = new SpeechTranscriberListener() {
                @Override
                public void onTranscriberStart(SpeechTranscriberResponse response) {
                }

                @Override
                public void onSentenceBegin(SpeechTranscriberResponse response) {
                }

                @Override
                public void onSentenceEnd(SpeechTranscriberResponse response) {
                    String text = response.getTransSentenceText();
                    if (text != null && !text.isEmpty()) {
                        synchronized (resultBuilder) {
                            resultBuilder.append(text);
                        }
                    }
                }

                @Override
                public void onTranscriptionResultChange(SpeechTranscriberResponse response) {
                }

                @Override
                public void onTranscriptionComplete(SpeechTranscriberResponse response) {
                    log.info("Reconhecimento em tempo real do NLS concluído - TaskId: {}", response.getTaskId());
                    recognitionCompleted.set(true);
                    latch.countDown();
                }

                @Override
                public void onFail(SpeechTranscriberResponse response) {
                    log.error("Falha no reconhecimento em tempo real do NLS - TaskId: {}, Status: {}, StatusText: {}",
                            response.getTaskId(),
                            response.getStatus(),
                            response.getStatusText());
                    recognitionFailed.set(true);
                    errorHolder[0].set(true);
                    latch.countDown();
                }
            };

            // Cria o reconhecedor de voz
            transcriber = new SpeechTranscriber(client, listener);

            // Define o AppKey
            transcriber.setAppKey(config.getApiKey());

            // Define o formato de áudio como PCM
            transcriber.setFormat(InputFormatEnum.PCM);

            // Define a taxa de amostragem como 16000Hz
            transcriber.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);

            // Habilita resultados intermediários
            transcriber.setEnableIntermediateResult(true);

            // Habilita pontuação
            transcriber.setEnablePunctuation(true);

            // Inicia o reconhecimento
            transcriber.start();

            // Envia os dados de áudio em uma nova thread
            final SpeechTranscriber finalTranscriber = transcriber;
            Thread sendThread = new Thread(() -> {
                try {
                    // Assina o fluxo de áudio e envia os dados
                    audioSink.subscribe(
                            audioChunk -> {
                                if (audioChunk != null && audioChunk.length > 0) {
                                    try {
                                        // Envia os dados de áudio
                                        finalTranscriber.send(audioChunk);
                                    } catch (Exception e) {
                                        log.error("Falha ao enviar os dados de áudio", e);
                                    }
                                }
                            },
                            error -> {
                                log.error("Erro no processamento do fluxo de áudio", error);
                                errorHolder[0].set(true);
                                latch.countDown();
                            },
                            () -> {
                                try {
                                    // Fim do fluxo de áudio, interrompe o reconhecimento
                                    finalTranscriber.stop();
                                } catch (Exception e) {
                                    log.error("Falha ao interromper o reconhecimento", e);
                                }
                            }
                    );
                } catch (Exception e) {
                    log.error("Erro ao processar o fluxo de áudio", e);
                    errorHolder[0].set(true);
                    latch.countDown();
                }
            });
            sendThread.start();

            // Aguarda a conclusão do reconhecimento ou o timeout
            if (!latch.await(RECOGNITION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                log.error("Timeout no reconhecimento em tempo real do NLS");
                return SttResult.textOnly("");
            }

            // Verifica se o reconhecimento falhou
            if (recognitionFailed.get() || errorHolder[0].get()) {
                log.error("Erro durante o processo de reconhecimento");
                return SttResult.textOnly("");
            }

            // Retorna o resultado do reconhecimento
            String result;
            synchronized (resultBuilder) {
                result = resultBuilder.toString().trim();
            }
            log.debug("Resultado do reconhecimento Alibaba Cloud NLS: {}", result);
            return SttResult.textOnly(result);

        } catch (Exception e) {
            log.error("Falha no reconhecimento em tempo real Alibaba Cloud NLS", e);
            // Em caso de exceção na conexão, limpa o cache; o client será recriado na próxima chamada
            globalClientCache.remove(config.getConfigId());
            return SttResult.textOnly("");
        } finally {
            // Fecha apenas o transcriber; o client é gerenciado e reutilizado de forma centralizada pelo cache, sem shutdown aqui
            if (transcriber != null) {
                try {
                    transcriber.close();
                } catch (Exception e) {
                    log.warn("Falha ao fechar o SpeechTranscriber", e);
                }
            }
        }
    }
}

