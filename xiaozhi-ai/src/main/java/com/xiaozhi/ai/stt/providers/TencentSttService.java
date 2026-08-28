package com.xiaozhi.ai.stt.providers;

import com.tencent.asrv2.SpeechRecognizer;
import com.tencent.asrv2.SpeechRecognizerListener;
import com.tencent.asrv2.SpeechRecognizerRequest;
import com.tencent.asrv2.SpeechRecognizerResponse;
import com.tencent.core.ws.Credential;
import com.tencent.core.ws.SpeechClient;
import com.xiaozhi.ai.stt.SttResult;
import com.xiaozhi.ai.stt.SttService;
import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.ai.utils.HttpUtil;

import okhttp3.*;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TencentSttService implements SttService {
    private static final String PROVIDER_NAME = "tencent";
    private static final String API_URL = "https://asr.tencentcloudapi.com";
    private static final int QUEUE_TIMEOUT_MS = 100; // Tempo limite de espera da fila
    private static final long RECOGNITION_TIMEOUT_MS = 90000; // Tempo limite de reconhecimento (90 segundos)

    // Usa a URL padrão do SDK da Tencent Cloud
    private static final String WS_API_URL = "wss://asr.cloud.tencent.com/asr/v2/";

    private String secretId;
    private String secretKey;
    private String appId;

    private final static OkHttpClient client = HttpUtil.client;

    // Instância compartilhada globalmente do SpeechClient
    private final SpeechClient speechClient = new SpeechClient(WS_API_URL);

    // Armazena as sessões de reconhecimento atualmente ativas
    private final ConcurrentHashMap<String, SpeechRecognizer> activeRecognizers = new ConcurrentHashMap<>();

    static {
        Thread.startVirtualThread(() -> {
            try {
                Request request = new Request.Builder().url(API_URL).head().build();
                Response response = client.newCall(request).execute();
                response.close(); // Não lê o conteúdo, apenas estabelece a conexão, para acelerar requisições futuras
            } catch (Exception e) {
                log.error("Erro ao inicializar o serviço STT do TencentSttService", e);
            }
        });
    }

    public TencentSttService(ConfigBO config) {
        if (config != null) {
            this.secretId = config.getApiKey();
            this.secretKey = config.getApiSecret();
            this.appId = config.getAppId();
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public SttResult stream(Flux<byte[]> audioSink) {
        // Verifica se a configuração já foi definida
        if (secretId == null || secretKey == null || appId == null) {
            log.error("A configuração de reconhecimento de voz da Tencent Cloud não foi definida; não é possível reconhecer");
            return null;
        }

        // Usa uma fila bloqueante para armazenar os dados de áudio
        BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
        AtomicBoolean isCompleted = new AtomicBoolean(false);
        AtomicReference<String> finalResult = new AtomicReference<>("");
        CountDownLatch recognitionLatch = new CountDownLatch(1);
        
        // Assina o Sink e coloca os dados na fila
        audioSink.subscribe(
            data -> audioQueue.offer(data),
            error -> {
                log.error("Erro no processamento do fluxo de áudio", error);
                isCompleted.set(true);
            },
            () -> isCompleted.set(true)
        );

        // Gera um ID de voz único
        String voiceId = UUID.randomUUID().toString();

        try {
            // Cria as credenciais da Tencent Cloud
            Credential credential = new Credential(appId, secretId, secretKey);

            // Cria a requisição de reconhecimento
            SpeechRecognizerRequest request = SpeechRecognizerRequest.init();
            request.setEngineModelType("16k_zh"); // Modelo em chinês com taxa de amostragem de 16k
            request.setVoiceFormat(1); // Formato PCM
            request.setVoiceId(voiceId);

            // Cria o listener de reconhecimento
            SpeechRecognizerListener listener = new SpeechRecognizerListener() {
                private final StringBuilder textBuilder = new StringBuilder();
                
                @Override
                public void onRecognitionStart(SpeechRecognizerResponse response) {
                    log.debug("Reconhecimento da Tencent Cloud iniciado - VoiceId: {}", voiceId);
                }

                @Override
                public void onSentenceBegin(SpeechRecognizerResponse response) {
                    // Início da frase; pode ser ignorado
                }

                @Override
                public void onRecognitionResultChange(SpeechRecognizerResponse response) {
                    // Resultado não estável, pode mudar
                    if (response.getResult() != null && response.getResult().getVoiceTextStr() != null) {
                        String text = response.getResult().getVoiceTextStr();
                        if (!text.isEmpty()) {
                            // Atualiza o resultado atual do reconhecimento
                            synchronized (textBuilder) {
                                textBuilder.setLength(0);
                                textBuilder.append(text);
                            }
                        }
                    }
                }

                @Override
                public void onSentenceEnd(SpeechRecognizerResponse response) {
                    // Resultado estável, não muda mais
                    if (response.getResult() != null && response.getResult().getVoiceTextStr() != null) {
                        String text = response.getResult().getVoiceTextStr();
                        if (!text.isEmpty()) {
                            // Atualiza o resultado final
                            synchronized (textBuilder) {
                                textBuilder.setLength(0);
                                textBuilder.append(text);
                            }
                            finalResult.set(text);
                        }
                    }
                }

                @Override
                public void onRecognitionComplete(SpeechRecognizerResponse response) {
                    // Reconhecimento concluído, obtém o resultado final
                    if (response.getResult() != null && response.getResult().getVoiceTextStr() != null) {
                        String text = response.getResult().getVoiceTextStr();
                        if (!text.isEmpty()) {
                            finalResult.set(text);
                        } else {
                            // Se o resultado final estiver vazio, usa o resultado acumulado anteriormente
                            synchronized (textBuilder) {
                                if (textBuilder.length() > 0) {
                                    finalResult.set(textBuilder.toString());
                                }
                            }
                        }
                    }
                    
                    // Libera o lock, indicando que o reconhecimento foi concluído
                    recognitionLatch.countDown();
                    
                    // Remove do conjunto de reconhecedores ativos
                    activeRecognizers.remove(voiceId);
                }

                @Override
                public void onFail(SpeechRecognizerResponse response) {
                    log.error("Falha no reconhecimento - VoiceId: {}, erro: {}", voiceId,
                            response.getMessage() != null ? response.getMessage() : "erro desconhecido");
                    
                    // Libera o lock, indicando que o reconhecimento falhou
                    recognitionLatch.countDown();
                    
                    // Remove do conjunto de reconhecedores ativos
                    activeRecognizers.remove(voiceId);
                }

                @Override
                public void onMessage(SpeechRecognizerResponse response) {
                    // Pode registrar todas as mensagens, mas não requer tratamento especial
                }
            };

            // Cria o reconhecedor
            SpeechRecognizer recognizer = new SpeechRecognizer(speechClient, credential, request, listener);

            // Armazena no mapa de reconhecedores ativos
            activeRecognizers.put(voiceId, recognizer);

            // Inicia o reconhecedor
            recognizer.start();

            // Marca se o sinal de parada já foi enviado
            AtomicBoolean stopSent = new AtomicBoolean(false);

            // Inicia uma virtual thread para enviar os dados de áudio
            Thread.startVirtualThread(() -> {
                try {
                    while (!isCompleted.get() || !audioQueue.isEmpty()) {
                        byte[] audioChunk = null;
                        try {
                            audioChunk = audioQueue.poll(QUEUE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            log.warn("Espera na fila de dados de áudio interrompida", e);
                            Thread.currentThread().interrupt(); // Restaura o flag de interrupção
                            break;
                        }
                        
                        if (audioChunk != null && activeRecognizers.containsKey(voiceId)) {
                            try {
                                recognizer.write(audioChunk);
                            } catch (Exception e) {
                                log.error("Erro ao enviar os dados de áudio - VoiceId: {}", voiceId, e);
                                break;
                            }
                        }
                    }
                    
                    // Envia o sinal de parada
                    if (activeRecognizers.containsKey(voiceId) && !stopSent.getAndSet(true)) {
                        try {
                            recognizer.stop();
                        } catch (Exception e) {
                            log.error("Erro ao parar o reconhecedor - VoiceId: {}", voiceId, e);
                        }
                    }
                } catch (Exception e) {
                    log.error("Erro ao processar o fluxo de áudio - VoiceId: {}", voiceId, e);
                }
            });

            // Aguarda a conclusão do reconhecimento ou o timeout
            boolean recognized = recognitionLatch.await(RECOGNITION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            if (!recognized) {
                // Limpa os recursos após o timeout
                if (activeRecognizers.containsKey(voiceId)) {
                    try {
                        recognizer.stop();
                        recognizer.close();
                        activeRecognizers.remove(voiceId);
                    } catch (Exception e) {
                        log.error("Erro ao limpar os recursos do reconhecedor após o timeout - VoiceId: {}", voiceId, e);
                    }
                }
            } else {
                // Após a conclusão normal, também fecha o recognizer para liberar recursos
                try {
                    recognizer.close();
                } catch (Exception e) {
                    log.error("Erro ao fechar o reconhecedor - VoiceId: {}", voiceId, e);
                }
            }

        } catch (Exception e) {
            log.error("Erro ao criar a sessão de reconhecimento de voz", e);
        }
        
        return SttResult.textOnly(finalResult.get());
    }

    // Libera recursos ao encerrar o serviço
    public void shutdown() {
        // Fecha todos os reconhecedores ativos
        activeRecognizers.forEach((id, recognizer) -> {
            try {
                recognizer.stop();
                recognizer.close();
            } catch (Exception e) {
                log.error("Erro ao fechar o reconhecedor - VoiceId: {}", id, e);
            }
        });
        activeRecognizers.clear();

        // Fecha o SpeechClient
        speechClient.shutdown();
    }

}