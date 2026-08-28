package com.xiaozhi.ai.tts.providers;

import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.OutputFormatEnum;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizer;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerListener;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerResponse;
import com.xiaozhi.common.port.TokenResolver;
import com.xiaozhi.ai.tts.TtsService;
import com.xiaozhi.ai.tts.XiaozhiTtsOptions;
import com.xiaozhi.common.model.bo.ConfigBO;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
/**
 * Serviço padrão de síntese de voz Alibaba Cloud NLS
 * Implementa a funcionalidade TTS usando o SDK de Interação de Voz Inteligente da Alibaba Cloud
 */
@Slf4j
public class AliyunNlsTtsService implements TtsService {
    private static final String PROVIDER_NAME = "aliyun-nls";

    // URL padrão do serviço Alibaba Cloud NLS
    private static final String NLS_URL = "wss://nls-gateway.aliyuncs.com/ws/v1";

    // Constantes do mecanismo de retry
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;

    /**
     * Cache global de NlsClient (compartilhado por configId)
     * Diferentes configurações de timbre/velocidade do mesmo configId podem compartilhar a mesma conexão NlsClient
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
    private final XiaozhiTtsOptions options;
    private final String outputPath;

    // Gerenciador de Token
    private final TokenResolver tokenResolver;

    public AliyunNlsTtsService(ConfigBO config, String voiceName, Double pitch, Double speed, String outputPath, TokenResolver tokenResolver) {
        this.config = config;
        this.options = XiaozhiTtsOptions.builder().voiceName(voiceName).pitch(pitch).speed(speed).build();
        this.outputPath = outputPath;
        this.tokenResolver = tokenResolver;
    }

    /**
     * Obtém ou cria uma instância de NlsClient (suporta reutilização de conexão)
     * Usa o cache global, compartilhando o NlsClient por configId
     */
    private NlsClient getOrCreateClient() throws Exception {
        String currentToken = tokenResolver.getToken(config);
        if (currentToken == null) {
            throw new RuntimeException("Não foi possível obter o Token da Alibaba Cloud");
        }

        Integer configId = config.getConfigId();
        int currentHash = currentToken.hashCode();

        // Obtém o client atualmente em cache
        CachedNlsClient cached = globalClientCache.get(configId);

        // Verifica se pode ser reutilizado
        if (cached != null && cached.tokenHash == currentHash) {
            return cached.client;
        }

        // É necessário criar um novo client
        return globalClientCache.compute(configId, (k, existing) -> {
            // Dupla verificação: pode ter sido criado por outra thread durante a espera
            if (existing != null && existing.tokenHash == currentHash) {
                return existing;
            }

            // Verifica se houve mudança de token (na primeira criação, existing é null, tratado como mudança)
            boolean tokenChanged = (existing == null || existing.tokenHash != currentHash);

            if (tokenChanged) {
                // Fecha o client antigo (se houver)
                if (existing != null) {
                    try {
                        existing.client.shutdown();
                    } catch (Exception e) {
                        log.warn("Falha ao fechar o NlsClient antigo", e);
                    }
                }
            }

            // Cria um novo client
            NlsClient newClient = new NlsClient(NLS_URL, currentToken);
            return new CachedNlsClient(newClient, currentHash);
        }).client;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public XiaozhiTtsOptions getOptions() {
        return options;
    }

    @Override
    public Path textToSpeech(String text) throws Exception {
        if (text == null || text.isEmpty()) {
            log.warn("Conteúdo de texto vazio!");
            return null;
        }

        int attempts = 0;
        while (attempts < MAX_RETRY_ATTEMPTS) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CountDownLatch latch = new CountDownLatch(1);
            NlsClient client = null;
            SpeechSynthesizer synthesizer = null;

            try {
                // Obtém ou reutiliza o NlsClient (reutilização de conexão)
                client = getOrCreateClient();

                synthesizer = new SpeechSynthesizer(client, new SpeechSynthesizerListener() {
                    @Override
                    public void onComplete(SpeechSynthesizerResponse response) {
                        latch.countDown();
                    }

                    @Override
                    public void onFail(SpeechSynthesizerResponse response) {
                        log.error("Falha na síntese de voz do NLS - TaskId: {}, Status: {}, StatusText: {}",
                                response.getTaskId(), response.getStatus(), response.getStatusText());
                        latch.countDown();
                    }

                    @Override
                    public void onMessage(ByteBuffer message) {
                        byte[] buffer = new byte[message.remaining()];
                        message.get(buffer);
                        try {
                            outputStream.write(buffer);
                        } catch (IOException e) {
                            log.error("Falha ao gravar os dados de áudio", e);
                        }
                    }
                });

                // Define o appKey
                synthesizer.setAppKey(config.getApiKey());
                // Define o formato de saída de áudio
                synthesizer.setFormat(OutputFormatEnum.WAV);
                // Define a taxa de amostragem
                synthesizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
                // Define a voz
                synthesizer.setVoice(getVoiceName());
                // Define o volume
                synthesizer.setVolume(100);

                // Define a velocidade e o tom (mapeamento: 0.5-2.0 → -500~500)
                int nlsSpeed = (int)Math.round((getSpeed() - 1.0f) * 500);
                int nlsPitch = (int)Math.round((getPitch() - 1.0f) * 500);
                nlsSpeed = Math.max(-500, Math.min(500, nlsSpeed));
                nlsPitch = Math.max(-500, Math.min(500, nlsPitch));

                synthesizer.setSpeechRate(nlsSpeed);
                synthesizer.setPitchRate(nlsPitch);

                synthesizer.setText(text);
                synthesizer.start();

                // Define o timeout, evitando espera infinita
                if (!latch.await(30, java.util.concurrent.TimeUnit.SECONDS)) {
                    log.error("Timeout na síntese de voz do NLS");
                    throw new RuntimeException("Timeout na síntese de voz");
                }

                // Verifica se algum dado de áudio foi gerado
                byte[] audioData = outputStream.toByteArray();
                if (audioData.length == 0) {
                    throw new RuntimeException("Nenhum dado de áudio foi gerado");
                }

                String audioFileName = getAudioFileName();
                Path filePath = Path.of(outputPath, audioFileName);

                File outputDir = new File(outputPath);
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }

                try (FileOutputStream fileOutputStream = new FileOutputStream(filePath.toFile())) {
                    fileOutputStream.write(audioData);
                }

                return filePath;

            } catch (InterruptedException e) {
                // Thread interrompida (usuário interrompeu a conversa); faz parte do fluxo normal, não limpa o cache do NlsClient
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                attempts++;
                // Fecha apenas o synthesizer; o client é gerenciado e reutilizado de forma centralizada pelo cache, sem shutdown aqui
                if (synthesizer != null) {
                    try {
                        synthesizer.close();
                    } catch (Exception ex) {
                        log.warn("Falha ao fechar o SpeechSynthesizer", ex);
                    }
                }

                if (attempts < MAX_RETRY_ATTEMPTS) {
                    log.warn("Falha na síntese de voz Alibaba Cloud NLS, tentando novamente ({}/{}): {}", attempts, MAX_RETRY_ATTEMPTS, e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Espera de retry interrompida", ie);
                        // Em caso de exceção na conexão NLS, limpa o cache; o client será recriado na próxima chamada
                        globalClientCache.remove(config.getConfigId());
                        throw e;
                    }
                } else {
                    log.error("Falha na síntese de voz Alibaba Cloud NLS; número máximo de tentativas atingido: {}", e.getMessage(), e);
                    // Em caso de exceção na conexão NLS, limpa o cache; o client será recriado na próxima chamada
                    globalClientCache.remove(config.getConfigId());
                    throw e;
                }
            }
        }
        throw new Exception("Falha na síntese de voz");
    }

    /**
     * Limpa o cache de NlsClient para o configId especificado
     */
    public static void clearClientCache(Integer configId) {
        if (configId != null) {
            CachedNlsClient removed = globalClientCache.remove(configId);
            if (removed != null && removed.client != null) {
                removed.client.shutdown();
            }
        }
    }
}
