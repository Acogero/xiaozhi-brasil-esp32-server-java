package com.xiaozhi.ai.stt.providers;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.translation.TranslationRecognizerParam;
import com.alibaba.dashscope.audio.asr.translation.TranslationRecognizerRealtime;
import com.alibaba.dashscope.audio.asr.translation.results.TranslationRecognizerResult;
import com.alibaba.dashscope.audio.omni.*;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.google.gson.JsonObject;
import com.xiaozhi.ai.stt.SttResult;
import com.xiaozhi.ai.stt.SttService;
import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.utils.AudioUtils;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import reactor.core.publisher.Flux;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AliyunSttService implements SttService {
    private static final String PROVIDER_NAME = "aliyun";

    private final String apiKey;
    private final String model;
    public AliyunSttService(ConfigBO config) {
        this.apiKey = config.getApiKey();
        this.model = config.getConfigName();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public SttResult stream(Flux<byte[]> audioSink) {
        try {
            if (model.toLowerCase().contains("gummy")) {
                return streamRecognitionGummy(audioSink);
            } else if (model.toLowerCase().contains("qwen") && model.toLowerCase().contains("realtime")) {
                return streamRecognitionQwen(audioSink);
            } else {
                // Lógica do paraformer
                String actualModel = model;
                // Compatibilidade com dados anteriores: se não corresponder a um tipo de modelo conhecido, usa o modelo padrão
                if (!model.toLowerCase().contains("paraformer")
                        && !model.toLowerCase().contains("fun-asr")) {
                    actualModel = "paraformer-realtime-8k-v2";
                    log.info("Tipo de modelo não reconhecido: {}, usando o modelo padrão: {}", model, actualModel);
                }
                return streamRecognitionParaformer(audioSink, actualModel);
            }
        } catch (Exception e) {
            log.error("Falha no reconhecimento de voz usando o modelo {}: ", model, e);
            return SttResult.textOnly("");
        }
    }

    /**
     * Reconhecimento em streaming do modelo Paraformer.
     * Modelos que suportam reconhecimento de emoção (como paraformer-realtime-8k-v2) retornam informações de emoção; nos demais modelos, o campo de emoção é null.
     */
    private SttResult streamRecognitionParaformer(Flux<byte[]> audioSink, String modelName) {
        var recognizer = new Recognition();

        var param = RecognitionParam.builder()
                .model(modelName)
                .format("pcm")
                .sampleRate(AudioUtils.SAMPLE_RATE)
                .apiKey(apiKey)
                .build();

        // Coleta o resultado de cada frase com isSentenceEnd=true
        var recognition = Flux.<SttResult>create(sink -> {
            try {
                recognizer.streamCall(param, Flowable.create(emitter -> {
                            audioSink.subscribe(
                                    chunk -> emitter.onNext(ByteBuffer.wrap(chunk)),
                                    emitter::onError,
                                    emitter::onComplete
                            );
                        }, BackpressureStrategy.BUFFER))
                        .timeout(90, TimeUnit.SECONDS)
                        .subscribe(result -> {
                                    if (result.isSentenceEnd()) {
                                        String text = result.getSentence().getText();
                                        String emoTag = result.getSentence().getEmoTag();
                                        Double emoConfidence = result.getSentence().getEmoConfidence();
                                        SttResult sttResult = SttResult.withEmotion(text, emoTag, emoConfidence);
                                        log.info("Resultado do reconhecimento de voz ({}): {} [emoção: {}, confiança: {}]",
                                                modelName, text, emoTag, emoConfidence);
                                        sink.next(sttResult);
                                    }
                                },
                                error -> {
                                    log.error("Erro durante o reconhecimento em streaming ({})", modelName, error);
                                    // Usa complete em vez de error, preservando o resultado parcial já reconhecido
                                    sink.complete();
                                },
                                sink::complete
                        );
            } catch (Exception e) {
                sink.error(e);
                log.info("Falha no reconhecimento de voz usando o modelo {}: ", modelName, e);
            }
        });

        // Mesclagem de múltiplas frases: concatena o texto; a emoção usa a frase de maior confiança
        try {
            return recognition.reduce(new SttResultAccumulator(), SttResultAccumulator::add)
                    .blockOptional()
                    .map(SttResultAccumulator::toSttResult)
                    .orElse(SttResult.textOnly(""));
        } finally {
            // Fecha proativamente a conexão WebSocket, evitando que ela entre em "estado sem referência" e demore 61 segundos para ser liberada
            try {
                recognizer.getDuplexApi().close(1000, "completed");
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Acumulador de resultados de múltiplas frases: mescla o texto; a emoção usa a frase de maior confiança.
     */
    private static class SttResultAccumulator {
        private final StringBuilder text = new StringBuilder();
        private String topEmoTag = null;
        private Double topEmoConfidence = null;

        SttResultAccumulator add(SttResult result) {
            text.append(result.text());
            if (result.hasEmotion() && result.emotionScore() != null) {
                if (topEmoConfidence == null || result.emotionScore() > topEmoConfidence) {
                    topEmoTag = result.emotion();
                    topEmoConfidence = result.emotionScore();
                }
            }
            return this;
        }

        SttResult toSttResult() {
            return topEmoTag != null
                    ? SttResult.withEmotion(text.toString(), topEmoTag, topEmoConfidence)
                    : SttResult.textOnly(text.toString());
        }
    }

    /**
     * Reconhecimento em streaming do modelo Gummy (com suporte a tradução em tempo real)
     */
    private SttResult streamRecognitionGummy(Flux<byte[]> audioSink) {
        StringBuilder result = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean hasError = new AtomicBoolean(false);

        // Inicializa os parâmetros da requisição
        var param = TranslationRecognizerParam.builder()
                .apiKey(apiKey)
                .model(model)
                .format("pcm")
                .sampleRate(AudioUtils.SAMPLE_RATE)
                .transcriptionEnabled(true)
                .sourceLanguage("auto")
                .build();
        // Inicializa a interface de callback
        ResultCallback<TranslationRecognizerResult> callback =
                new ResultCallback<TranslationRecognizerResult>() {
                    @Override
                    public void onEvent(TranslationRecognizerResult recognizerResult) {
                        try {

                            // Processa o resultado do reconhecimento
                            if (recognizerResult.getTranscriptionResult() != null) {
                                if (recognizerResult.isSentenceEnd()) {
                                    String text = recognizerResult.getTranscriptionResult().getText();
                                    log.info("Resultado do reconhecimento de voz ({}): {}", model, text);
                                    synchronized (result) {
                                        result.append(text);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("Erro ao processar o resultado do reconhecimento", e);
                        }
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Exception e) {
                        log.error("Erro no reconhecimento de voz ({}): {}", model, e.getMessage(), e);
                        hasError.set(true);
                        latch.countDown();
                    }
                };

        // Inicializa o serviço de reconhecimento em streaming
        TranslationRecognizerRealtime translator = new TranslationRecognizerRealtime();

        try {
            // Inicia o reconhecimento de voz em streaming
            translator.call(param, callback);

            // Assina o fluxo de áudio e envia os dados
            audioSink.subscribe(
                    audioChunk -> {
                        try {
                            ByteBuffer buffer = ByteBuffer.wrap(audioChunk);
                            translator.sendAudioFrame(buffer);
                        } catch (Exception e) {
                            log.error("Erro ao enviar os dados de áudio", e);
                        }
                    },
                    error -> {
                        log.error("Erro no fluxo de áudio", error);
                        translator.stop();
                        latch.countDown();
                    },
                    () -> {
                        translator.stop();
                    }
            );

            // Aguarda a conclusão do reconhecimento, no máximo 90 segundos
            boolean completed = latch.await(90, TimeUnit.SECONDS);

            if (!completed) {
                log.warn("Timeout no reconhecimento de voz ({})", model);
            }

        } catch (Exception e) {
            log.error("Erro durante o reconhecimento em streaming ({})", model, e);
            hasError.set(true);
        } finally {
            // Fecha a conexão websocket
            try {
                translator.getDuplexApi().close(1000, "bye");
            } catch (Exception e) {
                log.error("Erro ao fechar a conexão", e);
            }
        }

        if (hasError.get()) {
            return SttResult.textOnly("");
        }

        return SttResult.textOnly(result.toString());
    }

    /**
     * Reconhecimento em streaming do modelo Qwen (qwen3-asr-flash-realtime)
     */
    private SttResult streamRecognitionQwen(Flux<byte[]> audioSink) {
        StringBuilder result = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean hasError = new AtomicBoolean(false);
        AtomicBoolean isCompleted = new AtomicBoolean(false);
        AtomicReference<OmniRealtimeConversation> conversationRef = new AtomicReference<>(null);
        // Inicializa os parâmetros da requisição
        OmniRealtimeParam param = OmniRealtimeParam.builder()
                .model(model)
                .url("wss://dashscope.aliyuncs.com/api-ws/v1/realtime")
                .apikey(apiKey)
                .build();
        try {
            // Inicializa a interface de callback
            OmniRealtimeConversation conversation = new OmniRealtimeConversation(param, new OmniRealtimeCallback() {
                @Override
                public void onOpen() {
                }

                @Override
                public void onEvent(JsonObject message) {
                    String type = message.get("type").getAsString();
                    switch(type) {
                        case "session.created":
                            break;
                        case "conversation.item.input_audio_transcription.completed":
                            String transcript = message.get("transcript").getAsString();
                            log.info("Resultado do reconhecimento de voz ({}): {}", model, transcript);
                            synchronized (result) {
                                result.append(transcript);
                            }
                            // Fecha a conexão após receber o resultado do reconhecimento
                            if (conversationRef.get() != null && !isCompleted.get()) {
                                try {
                                    conversationRef.get().close(1000, "transcription_completed");
                                } catch (Exception e) {
                                    log.error("Erro ao fechar a conexão", e);
                                    // Se o fechamento falhar, aciona a conclusão manualmente
                                    if (isCompleted.compareAndSet(false, true)) {
                                        latch.countDown();
                                    }
                                }
                            }
                            break;
                        case "input_audio_buffer.speech_started":
                            break;
                        case "input_audio_buffer.speech_stopped":
                            break;
                        case "response.done":
                            if (isCompleted.compareAndSet(false, true)) {
                                latch.countDown();
                            }
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void onClose(int code, String reason) {
                    log.info("Conexão de reconhecimento de voz do Qwen encerrada - code: {}, reason: {}", code, reason);
                    if (isCompleted.compareAndSet(false, true)) {
                        latch.countDown();
                    }
                }
            });

            conversationRef.set(conversation);

            // Estabelece a conexão
            try {
                conversation.connect();
            } catch (NoApiKeyException e) {
                log.error("API Key inválida", e);
                hasError.set(true);
                return SttResult.textOnly("");
            }
            // Configura os parâmetros de transcrição
            OmniRealtimeTranscriptionParam transcriptionParam = new OmniRealtimeTranscriptionParam();
            // transcriptionParam.setLanguage("zh");
            transcriptionParam.setInputAudioFormat("pcm");
            transcriptionParam.setInputSampleRate(AudioUtils.SAMPLE_RATE);
            // Configura os parâmetros da sessão
            OmniRealtimeConfig config = OmniRealtimeConfig.builder()
                    .modalities(Collections.singletonList(OmniRealtimeModality.TEXT))
                    .transcriptionConfig(transcriptionParam)
                    .enableTurnDetection(false)  // Desativa o VAD do lado do servidor
                    .build();

            conversation.updateSession(config);

            // Assina o fluxo de áudio e envia os dados
            audioSink.subscribe(
                    audioChunk -> {
                        try {
                            // Converte os dados de áudio para Base64
                            String audioB64 = Base64.getEncoder().encodeToString(audioChunk);
                            conversation.appendAudio(audioB64);
                        } catch (Exception e) {
                            log.error("Erro ao enviar os dados de áudio", e);
                        }
                    },
                    error -> {
                        log.error("Erro no fluxo de áudio", error);
                        conversation.close(1000, "error");
                        if (isCompleted.compareAndSet(false, true)) {
                            latch.countDown();
                        }
                    },
                    () -> {
                        // Este callback é acionado quando o VAD local detecta o fim da fala (SPEECH_END)
                        // Como o VAD do lado do servidor está desativado, é necessário chamar commit() manualmente para acionar o reconhecimento
                        if (!isCompleted.get()) {
                            // Envia manualmente a requisição de reconhecimento (é obrigatório fazer commit manual após desativar o VAD do servidor)
                            conversation.commit();
                        }
                    }
            );

            // Aguarda a conclusão do reconhecimento, no máximo 90 segundos
            boolean completed = latch.await(90, TimeUnit.SECONDS);

            if (!completed) {
                log.warn("Timeout no reconhecimento de voz ({})", model);
                // Fecha proativamente a conexão em caso de timeout
                try {
                    conversation.close(1000, "timeout");
                } catch (Exception e) {
                    log.error("Erro ao fechar a conexão", e);
                }
            }
        } catch (Exception e) {
            log.error("Erro durante o reconhecimento em streaming ({})", model, e);
            hasError.set(true);
            // Em caso de exceção, tenta fechar a conexão
            try {
                if (conversationRef.get() != null) {
                    conversationRef.get().close(1000, "error");
                }
            } catch (Exception ex) {
                log.error("Erro ao fechar a conexão", ex);
            }
        }

        if (hasError.get()) {
            return SttResult.textOnly("");
        }

        return SttResult.textOnly(result.toString());
    }
}