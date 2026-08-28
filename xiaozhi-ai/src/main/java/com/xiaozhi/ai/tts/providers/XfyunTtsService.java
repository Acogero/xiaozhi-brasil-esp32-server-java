package com.xiaozhi.ai.tts.providers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


import com.xiaozhi.ai.tts.TtsService;
import com.xiaozhi.ai.tts.XiaozhiTtsOptions;
import com.xiaozhi.common.model.bo.ConfigBO;

import cn.xfyun.api.TtsClient;
import cn.xfyun.model.response.TtsResponse;
import cn.xfyun.service.tts.AbstractTtsWebSocketListener;
import okhttp3.Response;
import okhttp3.WebSocket;

import lombok.extern.slf4j.Slf4j;
/**
 * Serviço de síntese de voz iFLYTEK
 */
@Slf4j
public class XfyunTtsService implements TtsService {
    private static final String PROVIDER_NAME = "xfyun";
    // Tempo limite de reconhecimento (60 segundos)
    private static final long RECOGNITION_TIMEOUT_MS = 60000;

    // Constantes do mecanismo de retry
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final XiaozhiTtsOptions options;

    // Caminho de saída do áudio
    private String outputPath;

    // appid, apiKey e apiSecret são obtidos no console da plataforma aberta (https://console.xfyun.cn/)
    private String appId;
    private String apiKey;
    private String apiSecret;

    public XfyunTtsService(ConfigBO config, String voiceName, Double pitch, Double speed, String outputPath) {
        this.options = XiaozhiTtsOptions.builder().voiceName(voiceName).pitch(pitch).speed(speed).build();
        this.outputPath = outputPath;
        this.appId = config.getAppId();
        this.apiKey = config.getApiKey();
        this.apiSecret = config.getApiSecret();
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
    public String audioFormat() {
        return "mp3";
    }

    @Override
    public Path textToSpeech(String text) throws Exception {
        if (text == null || text.isEmpty()) {
            log.warn("Conteúdo de texto vazio!");
            return null;
        }

        int attempts = 0;
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                // Gera o nome do arquivo de áudio
                String audioFileName = getAudioFileName();
                String audioFilePath = outputPath + audioFileName;
                File file = new File(audioFilePath);
                // Envia a requisição POST
                boolean success = sendRequest(text, file);

                if (success) {
                    return Path.of(audioFilePath);
                } else {
                    throw new Exception("Falha na síntese de voz");
                }
            } catch (Exception e) {
                attempts++;
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    log.warn("Falha na síntese de voz da iFLYTEK, tentando novamente ({}/{}): {}", attempts, MAX_RETRY_ATTEMPTS, e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Espera de retry interrompida", ie);
                        throw e;
                    }
                } else {
                    log.error("Falha na síntese de voz da iFLYTEK; número máximo de tentativas atingido", e);
                    throw e;
                }
            }
        }
        throw new Exception("Falha na síntese de voz");
    }

    /**
     * Envia a requisição POST para o xfyun, obtendo o resultado da síntese de voz
     */
    private boolean sendRequest(String text, File file) throws Exception {
        CountDownLatch recognitionLatch = new CountDownLatch(1);
        try {
            // Mapeia de forma não linear nosso parâmetro (0.5-2.0) para o parâmetro do xfyun (0-100)
            // Regra de mapeamento: 0.5→0, 1.0→50 (padrão do xfyun), 2.0→100
            int xfyunSpeed;
            if (getSpeed() <= 1.0f) {
                xfyunSpeed = (int)Math.round((getSpeed() - 0.5f) * 100f);
            } else {
                xfyunSpeed = (int)Math.round(50f + (getSpeed() - 1.0f) * 50f);
            }

            int xfyunPitch;
            if (getPitch() <= 1.0f) {
                xfyunPitch = (int)Math.round((getPitch() - 0.5f) * 100f);
            } else {
                xfyunPitch = (int)Math.round(50f + (getPitch() - 1.0f) * 50f);
            }

            // Garante que o valor esteja dentro do intervalo válido
            xfyunSpeed = Math.max(0, Math.min(100, xfyunSpeed));
            xfyunPitch = Math.max(0, Math.min(100, xfyunPitch));

            // Define os parâmetros de síntese
            TtsClient ttsClient = new TtsClient.Builder()
                    .signature(appId, apiKey, apiSecret)
                    .aue("lame")
                    .vcn(getVoiceName())
                    .speed(xfyunSpeed)
                    .pitch(xfyunPitch)
                    .build();
            ttsClient.send(text, new AbstractTtsWebSocketListener() {
                // O formato de retorno é o array binário bytes do arquivo de áudio
                @Override
                public void onSuccess(byte[] bytes) {
                    FileOutputStream outputStream = null;
                    try {
                        outputStream = new FileOutputStream(file);
                        outputStream.write(bytes);
                        outputStream.flush();
                        
                        // Garante que o handle do arquivo seja liberado
                        if(outputStream != null){
                            try {
                                outputStream.close();
                                outputStream = null;  // Marca como fechado
                            } catch (IOException e) {
                                log.error("Falha ao fechar o fluxo de arquivo de síntese de voz do xfyun", e);
                                throw new RuntimeException("Falha ao fechar o arquivo", e);
                            }
                        }
                        
                        // Verifica se o arquivo foi gravado com sucesso
                        if (!file.exists() || file.length() == 0) {
                            throw new RuntimeException("Falha ao gravar o arquivo de áudio");
                        }
                        
                    } catch (Exception e) {
                        log.error("Falha ao gravar o arquivo de áudio", e);
                        throw new RuntimeException(e);
                    } finally {
                        // Por fim, garante que countDown seja chamado
                        recognitionLatch.countDown();
                    }
                }

                // Em caso de falha de autorização, obtém a mensagem de erro correspondente via throwable.getMessage()
                @Override
                public void onFail(WebSocket webSocket, Throwable throwable, Response response) {
                    log.error("Falha no TTS do xfyun, motivo: {}", throwable.getMessage());
                    recognitionLatch.countDown();
                }

                // Em caso de falha de negócio, obtém o código e a mensagem de erro via ttsResponse
                @Override
                public void onBusinessFail(WebSocket webSocket, TtsResponse ttsResponse) {
                    log.error(ttsResponse.toString());
                    recognitionLatch.countDown();
                }
            });
        } catch (Exception e) {
            log.error("Erro ao enviar a requisição TTS", e);
            recognitionLatch.countDown();
            throw new Exception("Falha ao enviar a requisição TTS", e);
        }
        // Aguarda a conclusão da síntese de voz ou o timeout
        boolean recognized = recognitionLatch.await(RECOGNITION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (!recognized) {
            log.warn("Timeout na síntese de voz da iFLYTEK Cloud");
        }
        return true;
    }

}