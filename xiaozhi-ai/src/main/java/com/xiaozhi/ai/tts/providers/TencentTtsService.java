package com.xiaozhi.ai.tts.providers;

import com.tencent.core.ws.Credential;
import com.tencent.core.ws.SpeechClient;
import com.tencent.ttsv2.*;
import com.xiaozhi.ai.tts.TtsService;
import com.xiaozhi.ai.tts.XiaozhiTtsOptions;
import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.utils.AudioUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TencentTtsService implements TtsService {
    private static final String PROVIDER_NAME = "tencent";
    // Endereço WebSocket padrão do TTS da Tencent Cloud
    private static final String DEFAULT_TTS_REQ_URL = "wss://tts.cloud.tencent.com/stream_ws";
    // Tempo limite de reconhecimento (60 segundos)
    private static final long SYNTHESIS_TIMEOUT_MS = 60000;

    // Constantes do mecanismo de retry
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;

    // Informações de autenticação da Tencent Cloud
    private String appId;
    private String secretId;
    private String secretKey;

    // Parâmetros de voz (voiceName, pitch, speed)
    private final XiaozhiTtsOptions options;

    // Basta criar um SpeechClient globalmente na aplicação; seu ciclo de vida pode acompanhar toda a aplicação
    private static final SpeechClient speechClient = new SpeechClient(DEFAULT_TTS_REQ_URL);

    public TencentTtsService(ConfigBO config, String voiceName, Double pitch, Double speed, String outputPath) {
        this.options = XiaozhiTtsOptions.builder().voiceName(voiceName).pitch(pitch).speed(speed).build();
        this.appId = config.getAppId();
        this.secretId = config.getApiKey();
        this.secretKey = config.getApiSecret();
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

    private Flux<byte[]> stream(String text) throws Exception {
        if (text == null || text.isEmpty()) {
            log.warn("Conteúdo de texto vazio!");
            return Flux.empty();
        }

        // O método start() do SDK da Tencent Cloud é não bloqueante; os dados de áudio são enviados de forma assíncrona pelo callback onAudioResult.
        // Usa Sinks.Many em vez de CountDownLatch.await(), evitando bloquear a thread do scheduler do Reactor.
        return Flux.defer(() -> {
            Sinks.Many<byte[]> dataSink = Sinks.many().unicast().onBackpressureBuffer();

            Credential credential = new Credential(appId, secretId, secretKey);
            SpeechSynthesizerRequest request = new SpeechSynthesizerRequest();
            request.setText(text);

            int voiceType = Integer.parseInt(getVoiceName());
            request.setVoiceType(voiceType);

            // Mapeia nosso parâmetro (0.5-2.0) para o parâmetro da Tencent Cloud (-2 a 6)
            float tencentSpeed = (float) ((getSpeed() - 0.5) * (4.0 / 1.5) - 2.0);
            tencentSpeed = Math.max(-2.0f, Math.min(6.0f, tencentSpeed));
            request.setSpeed(tencentSpeed);

            request.setVolume(0f);
            request.setCodec("pcm");
            request.setSampleRate(AudioUtils.SAMPLE_RATE);
            request.setSessionId(UUID.randomUUID().toString());

            SpeechSynthesizerListener listener = new SpeechSynthesizerListener() {
                @Override
                public void onSynthesisStart(SpeechSynthesizerResponse response) {}

                @Override
                public void onAudioResult(ByteBuffer buffer) {
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    dataSink.tryEmitNext(data);
                }

                @Override
                public void onTextResult(SpeechSynthesizerResponse response) {}

                @Override
                public void onSynthesisEnd(SpeechSynthesizerResponse response) {
                    dataSink.tryEmitComplete();
                }

                @Override
                public void onSynthesisFail(SpeechSynthesizerResponse response) {
                    String message = response.getMessage() != null ? response.getMessage() : "erro desconhecido";
                    log.error("Falha na síntese TTS da Tencent Cloud - SessionId: {}, erro: {}",
                            response.getSessionId(), message);
                    dataSink.tryEmitError(new Exception(message));
                }
            };

            // Cria o sintetizador de voz (o synthesizer não pode ser reutilizado; a cada síntese é necessário criar um novo objeto)
            SpeechSynthesizer[] synthRef = new SpeechSynthesizer[1];
            try {
                SpeechSynthesizer synthesizer = new SpeechSynthesizer(speechClient, credential, request, listener);
                synthRef[0] = synthesizer;
                synthesizer.start();
            } catch (Exception e) {
                log.error("Erro durante a síntese TTS da Tencent Cloud", e);
                return Flux.error(e);
            }

            return dataSink.asFlux()
                    .timeout(Duration.ofMillis(SYNTHESIS_TIMEOUT_MS))
                    .doFinally(signal -> {
                        SpeechSynthesizer synth = synthRef[0];
                        if (synth != null) {
                            try { synth.stop(); } catch (Exception e) { log.warn("Erro ao parar o TTS da Tencent Cloud", e); }
                            try { synth.close(); } catch (Exception e) { log.error("Erro ao fechar o sintetizador TTS da Tencent Cloud", e); }
                        }
                    });
        }).retryWhen(Retry.fixedDelay(MAX_RETRY_ATTEMPTS - 1, Duration.ofMillis(RETRY_DELAY_MS)))
          .doOnError(e -> log.error("Falha na síntese de voz em streaming da Tencent Cloud; número máximo de tentativas atingido", e));
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
                // Usa a interface de streaming para sintetizar o áudio e depois mescla todos os fragmentos
                ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
                final Exception[] error = new Exception[1];

                stream(text).subscribe(audioData -> {
                    if (audioData != null) {
                        try {
                            audioBuffer.write(audioData);
                        } catch (Exception e) {
                            log.error("Falha ao gravar os dados de áudio", e);
                            error[0] = e;
                        }
                    }
                });

                // Se houver erro, lança exceção
                if (error[0] != null) {
                    throw error[0];
                }

                // Converte os dados de áudio PCM mesclados para o formato WAV e salva
                byte[] pcmData = audioBuffer.toByteArray();
                if (pcmData.length == 0) {
                    log.warn("Dados de áudio sintetizados vazios");
                    return null;
                }

                // Converte para WAV e salva
                String filePath = AudioUtils.saveAsWav(pcmData);

                return Path.of(filePath);

            } catch (Exception e) {
                attempts++;
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    log.warn("Falha na síntese de voz da Tencent Cloud, tentando novamente ({}/{}): {}", attempts, MAX_RETRY_ATTEMPTS, e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Espera de retry interrompida", ie);
                        throw e;
                    }
                } else {
                    log.error("Falha na síntese de voz da Tencent Cloud; número máximo de tentativas atingido", e);
                    throw new Exception("Falha na síntese de voz não streaming", e);
                }
            }
        }
        throw new Exception("Falha na síntese de voz");
    }

}
