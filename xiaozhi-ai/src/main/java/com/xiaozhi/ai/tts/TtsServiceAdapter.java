package com.xiaozhi.ai.tts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechOptions;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import reactor.core.publisher.Flux;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Adaptador: faz a ponte entre {@link TtsService} / {@link StreamingTextToSpeech} do projeto e
 * a interface padrão {@link TextToSpeechModel} do Spring AI.
 * <p>
 * Forma de uso:
 * <pre>
 * TtsService ttsService = ttsServiceFactory.getTtsService(config, voiceName, pitch, speed);
 * TextToSpeechModel springAiTts = new TtsServiceAdapter(ttsService);
 * byte[] audio = springAiTts.call("Olá");
 * </pre>
 * <p>
 * Dessa forma, os TTS Providers existentes ficam compatíveis com o ecossistema TTS do Spring AI sem precisar de modificações.
 */
@Slf4j
public class TtsServiceAdapter implements TextToSpeechModel {

    private final TtsService ttsService;

    public TtsServiceAdapter(TtsService ttsService) {
        this.ttsService = ttsService;
    }

    @Override
    public TextToSpeechResponse call(TextToSpeechPrompt prompt) {
        String text = prompt.getInstructions().getText();
        try {
            Path audioPath = ttsService.textToSpeech(text);
            byte[] audioBytes = Files.readAllBytes(audioPath);
            // Limpa os arquivos temporários
            Files.deleteIfExists(audioPath);
            return new TextToSpeechResponse(List.of(new Speech(audioBytes)));
        } catch (Exception e) {
            log.error("TTS call failed for provider {}: {}", ttsService.getProviderName(), e.getMessage(), e);
            throw new RuntimeException("TTS synthesis failed", e);
        }
    }

    @Override
    public Flux<TextToSpeechResponse> stream(TextToSpeechPrompt prompt) {
        throw new RuntimeException("TTS streaming failed");
    }

    @Override
    public TextToSpeechOptions getDefaultOptions() {
        return ttsService.getOptions();
    }

    /**
     * Obtém a instância original subjacente de TtsService.
     */
    public TtsService unwrap() {
        return ttsService;
    }
}
