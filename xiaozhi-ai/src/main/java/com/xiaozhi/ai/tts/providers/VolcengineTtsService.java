package com.xiaozhi.ai.tts.providers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xiaozhi.ai.tts.TtsService;
import com.xiaozhi.ai.tts.XiaozhiTtsOptions;
import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.utils.AudioUtils;
import com.xiaozhi.ai.utils.HttpUtil;

import okhttp3.*;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VolcengineTtsService implements TtsService {
    private static final String PROVIDER_NAME = "volcengine";
    private static final String API_URL = "https://openspeech.bytedance.com/api/v1/tts";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // Constantes do mecanismo de retry
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;

    // Caminho de saída do áudio
    private String outputPath;

    // Relacionado à API
    private String appId;
    private String accessToken; // Corresponde à apiKey

    // Parâmetros de voz (voiceName, pitch, speed)
    private final XiaozhiTtsOptions options;

    private final OkHttpClient client = HttpUtil.client;

    public VolcengineTtsService(ConfigBO config, String voiceName, Double pitch, Double speed, String outputPath) {
        this.options = XiaozhiTtsOptions.builder().voiceName(voiceName).pitch(pitch).speed(speed).build();
        this.outputPath = outputPath;
        this.appId = config.getAppId();
        this.accessToken = config.getApiKey();
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
            try {
                // Gera o nome do arquivo de áudio
                String audioFileName = getAudioFileName();
                String audioFilePath = outputPath + audioFileName;

                // Envia a requisição POST
                boolean success = sendRequest(text, audioFilePath);

                if (success) {
                    return Path.of(audioFilePath);
                } else {
                    throw new Exception("Falha na síntese de voz");
                }
            } catch (Exception e) {
                attempts++;
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    log.warn("Falha na síntese de voz da Volcengine, tentando novamente ({}/{}): {}", attempts, MAX_RETRY_ATTEMPTS, e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Espera de retry interrompida", ie);
                        throw e;
                    }
                } else {
                    log.error("Falha na síntese de voz da Volcengine; número máximo de tentativas atingido", e);
                    throw e;
                }
            }
        }
        throw new Exception("Falha na síntese de voz");
    }

    /**
     * Envia a requisição POST para a API da Volcengine, obtendo o resultado da síntese de voz
     */
    private boolean sendRequest(String text, String audioFilePath) throws Exception {
        try {
            // Monta os parâmetros da requisição
            JsonObject requestJson = new JsonObject();

            // Seção app
            JsonObject app = new JsonObject();
            app.addProperty("appid", appId);
            app.addProperty("token", accessToken);
            // Seleciona o cluster de acordo com o tipo de timbre: timbre clonado usa volcano_mega, timbre comum usa volcano_tts
            String cluster = (getVoiceName() != null && getVoiceName().startsWith("S_")) ? "volcano_mega" : "volcano_tts";
            app.addProperty("cluster", cluster);
            requestJson.add("app", app);

            // Seção user
            JsonObject user = new JsonObject();
            user.addProperty("uid", UUID.randomUUID().toString());
            requestJson.add("user", user);

            // Seção audio
            JsonObject audio = new JsonObject();
            audio.addProperty("voice_type", getVoiceName());
            audio.addProperty("encoding", "wav");
            audio.addProperty("speed_ratio", getSpeed());
            audio.addProperty("volume_ratio", 1.0);
            audio.addProperty("pitch_ratio", getPitch());
            audio.addProperty("rate", AudioUtils.SAMPLE_RATE);
            requestJson.add("audio", audio);

            // Seção request
            JsonObject request_JsonObject = new JsonObject();
            request_JsonObject.addProperty("reqid", UUID.randomUUID().toString());
            request_JsonObject.addProperty("text", text);
            request_JsonObject.addProperty("text_type", "plain");
            request_JsonObject.addProperty("operation", "query");
            request_JsonObject.addProperty("with_frontend", 1);
            request_JsonObject.addProperty("frontend_type", "unitTson");
            requestJson.add("request", request_JsonObject);

            // Usa autenticação por Bearer Token
            String bearerToken = "Bearer; " + accessToken; // Atenção: o ponto e vírgula é um formato específico da Volcengine

            RequestBody requestBody = RequestBody.create(JSON, requestJson.toString());

            // Define o header e o corpo da requisição
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", bearerToken) // Adiciona o header Authorization
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "sem corpo de resposta";
                    log.error("Falha na requisição TTS: {} {}, mensagem de erro: {}, conteúdo original: {}", response.code(), response.message(), errorBody, text);
                    return false;
                }

                // Interpreta a resposta
                if (response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

                    // Verifica se a resposta contém erro
                    if (jsonResponse.has("code") && jsonResponse.get("code").getAsInt() != 3000) {
                        log.error("A requisição TTS retornou erro: code={}, message={}",
                                jsonResponse.get("code").getAsInt(),
                                jsonResponse.get("message").getAsString());
                        return false;
                    }

                    // Obtém os dados de áudio
                    if (jsonResponse.has("data")) {
                        String base64Audio = jsonResponse.get("data").getAsString();
                        byte[] audioData = Base64.getDecoder().decode(base64Audio);

                        // Salva o arquivo de áudio
                        File audioFile = new File(audioFilePath);
                        try (FileOutputStream fout = new FileOutputStream(audioFile)) {
                            fout.write(audioData);
                        }

                        return true;
                    } else {
                        log.error("Nenhum dado de áudio encontrado na resposta TTS: {}", responseBody);
                        return false;
                    }
                } else {
                    log.error("Corpo da resposta TTS vazio");
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Erro ao enviar a requisição TTS", e);
            throw new Exception("Falha ao enviar a requisição TTS", e);
        }
    }
}
