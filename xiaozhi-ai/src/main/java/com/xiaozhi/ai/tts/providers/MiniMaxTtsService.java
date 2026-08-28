package com.xiaozhi.ai.tts.providers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.xiaozhi.ai.tts.TtsService;
import com.xiaozhi.ai.tts.XiaozhiTtsOptions;
import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.ai.utils.HttpUtil;
import com.xiaozhi.utils.JsonUtil;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


@Slf4j
public class MiniMaxTtsService implements TtsService {

    private static final String PROVIDER_NAME = "minimax";

    // Constantes do mecanismo de retry
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final String groupId;
    private final String apiKey;

    private final String outputPath;

    // Parâmetros de voz (voiceName, pitch, speed)
    private final XiaozhiTtsOptions options;
    private int minimaxPitch;
    private final String model;

    private final OkHttpClient client = HttpUtil.client;
    public static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=utf-8";
    private static final MediaType JSON = MediaType.parse(APPLICATION_JSON_CHARSET_UTF_8);

    public MiniMaxTtsService(ConfigBO config, String voiceName, Double pitch, Double speed, String outputPath) {
        this.groupId = config.getAppId();
        this.apiKey = config.getApiKey();
        this.options = XiaozhiTtsOptions.builder().voiceName(voiceName).pitch(pitch).speed(speed).build();
        this.outputPath = outputPath;
        this.model = config.getConfigName();
        // Define o tom (é necessário mapear: nosso [0.5, 2] → o [-12, 12] do MiniMax)
        // Fórmula de mapeamento: minimax_pitch = (our_pitch - 1.0) × 24
        minimaxPitch = (int)Math.round((getPitch() - 1.0) * 24);
        // Garante que o valor esteja dentro do intervalo válido
        minimaxPitch = Math.max(-12, Math.min(12, minimaxPitch));
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
        int attempts = 0;
        Exception lastException = null;

        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                Path output = Paths.get(outputPath, getAudioFileName());
                sendRequest(text, output.toString());
                return output;
            } catch (Exception e) {
                lastException = e;
                attempts++;
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    log.warn("Falha na síntese de voz do MiniMax, tentando novamente ({}/{}): {}", attempts, MAX_RETRY_ATTEMPTS, e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Espera de retry interrompida", ie);
                        throw e;
                    }
                } else {
                    log.error("Falha na síntese de voz do MiniMax; número máximo de tentativas atingido", e);
                }
            }
        }
        throw lastException != null ? lastException : new Exception("Falha na síntese de voz");
    }

    private void sendRequest(String text, String filepath) {
        // Cria os parâmetros da requisição
        var params = new Text2AudioParams(model, getVoiceName(), text);

        // Define a velocidade (o intervalo do MiniMax [0.5, 2] é igual ao nosso; usado diretamente)
        params.voiceSetting.setSpeed(getSpeed());
        params.voiceSetting.setPitch(minimaxPitch);

        var request = new Request.Builder()
                .url("https://api.minimaxi.com/v1/t2a_v2?Groupid=%s".formatted(groupId))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer %s".formatted(apiKey)) // Adiciona o header Authorization
                .post(RequestBody.create(JsonUtil.toJson(params), JSON))
                .build();

        try (var resp = client.newCall(request).execute()) {
            if (resp.isSuccessful()) {
                var respBody = JsonUtil.fromJson(resp.body().string(), Text2AudioResp.class);
                if (respBody.baseResp.statusCode == 0) {
                    var bytes = HexFormat.of().parseHex(respBody.data.audio);
                    Files.write(Paths.get(filepath), bytes);
                } else {
                    log.error("Falha no TTS {}:{}", respBody.baseResp.statusCode, respBody.baseResp.statusMsg);
                }
            } else {
                log.error("Falha na requisição TTS {}", resp.body().string());
            }
        } catch (IOException e) {
            log.error("Erro ao enviar a requisição TTS", e);
            throw new RuntimeException("Falha ao enviar a requisição TTS", e);
        }
    }
    
    @Data
    @Accessors(chain = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Text2AudioParams {

        public Text2AudioParams(String model, String voiceId, String text) {
            this.model = model;
            this.text = text;
            this.audioSetting = new AudioSetting();
            this.voiceSetting = new VoiceSetting().setVoiceId(voiceId);
        }

        private String model;
        private String text;
        private boolean stream = false;
        private StreamOptions streamOptions = new StreamOptions();
        private String languageBoost = "auto";
        private String outputFormat = "hex";
        private VoiceSetting voiceSetting;
        private AudioSetting audioSetting;

        @Data
        public static class StreamOptions{
            @JsonProperty("exclude_aggregated_audio")
            boolean excludeAggregatedAudio= true;
        }
        @Data
        @Accessors(chain = true)
        public static class VoiceSetting {
            @JsonProperty("voice_id")
            private String voiceId;
            private double speed = 1;
            private double vol = 1;
            private int pitch = 0;
            //private String emotion = "happy";
        }

        @Data
        public static class AudioSetting {
            @JsonProperty("sample_rate")
            private int sampleRate = 32000;
            private int bitrate = 128000;
            private String format = "mp3";
        }
    }

    @Data
    public static class Text2AudioResp {
        @JsonProperty("is_final")
        private boolean isFinal;
        @JsonProperty("session_id")
        private String sessionId;
        @JsonProperty("trace_id")
        private String traceId;
        @JsonProperty("event")
        private String event;
        @JsonProperty("data")
        private Data data;
        @JsonProperty("extra_info")
        private ExtraInfo extraInfo;
        @JsonProperty("base_resp")
        private BaseResp baseResp;

        /**
         * No protocolo WebSocket do MiniMax, Data nem sempre tem status. O record do Java, por padrão, tem um construtor com todos os parâmetros.
         */
        @lombok.Data
        public static class Data {
            private int status;
            private String audio;
        }

        @lombok.Data
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        class ExtraInfo {
            @JsonProperty("audio_channel")
            private int audioChannel;
            @JsonProperty("audio_format")
            private String audioFormat;
            // Duração do áudio, em milissegundos
            @JsonProperty("audio_length")
            private int audioLength;
            @JsonProperty("audio_sample_rate")
            private int audioSampleRate;
            // Tamanho do arquivo de áudio, em bytes
            @JsonProperty("audio_size")
            private int audioSize;
            // Taxa de bits do áudio
            @JsonProperty("bitrate")
            private int bitrate;
            // Proporção de caracteres inválidos. Se não ultrapassar 10% (inclusive), o áudio é gerado normalmente e a proporção é retornada; acima disso, ocorre erro
            @JsonProperty("invisible_character_ratio")
            private int invisibleCharacterRatio;
            // Número de caracteres cobrados nesta geração de voz
            @JsonProperty("usage_characters")
            private int usageCharacters;
            // Contagem de caracteres pronunciados, incluindo ideogramas, números e letras, sem contar pontuação
            @JsonProperty("word_count")
            private int wordCount;
        }
        record BaseResp(@JsonProperty("status_code") int statusCode, @JsonProperty("status_msg") String statusMsg) {
        }
    }

}
