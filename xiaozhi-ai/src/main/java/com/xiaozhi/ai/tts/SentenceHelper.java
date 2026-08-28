package com.xiaozhi.ai.tts;

import com.xiaozhi.utils.EmojiUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe auxiliar de processamento de frases, unificando a lógica de segmentação.
 * Instância com estado; não reutilize — descarte após o uso.
 *
 * Oferece duas formas de uso:
 * 1. Reativa: convert(Flux<String>) → Flux<String>, usada pelo FileSynthesizer
 * 2. Imperativa: take(String token) / take(), usada internamente pela assinatura WebSocket do TTS Provider
 */
public class SentenceHelper implements ChatConverter {

    /**
     * Resultado da segmentação, contendo o texto puro sem emoticons e a palavra de emoção extraída.
     */
    public record SentenceResult(String text, String mood) {}
    // Padrão de pontuação de fim de frase (ponto, exclamação, interrogação em chinês e inglês)
    private static final Pattern SENTENCE_END_PATTERN = Pattern.compile("[。！？!?]");

    // Pontuação de pausa como vírgula, ponto e vírgula, etc.
    private static final Pattern PAUSE_PATTERN = Pattern.compile("[，、；,;]");

    // Pontuação especial como dois-pontos e aspas
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[：:\"]");

    // Quebra de linha
    private static final Pattern NEWLINE_PATTERN = Pattern.compile("[\n\r]");

    // Padrão numérico (usado para detectar se o ponto decimal está dentro de um número)
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+\\.\\d+");

    // Comprimento mínimo da frase (número de caracteres)
    private static final int MIN_SENTENCE_LENGTH = 8;

    // Comprimento máximo do buffer de contexto (usado para detecção de ponto decimal e outras verificações de contexto)
    private static final int CONTEXT_BUFFER_MAX_LENGTH = 20;

    private final StringBuilder currentSentence = new StringBuilder();
    private final StringBuilder contextBuffer = new StringBuilder();

    public SentenceHelper() {
    }

    /**
     * Segmentação imperativa: recebe tokens um a um, retornando a frase completa detectada; retorna string vazia se ainda não formar uma frase.
     * Usada internamente pelo callback de assinatura WebSocket do TTS Provider.
     */
    public List<SentenceResult> take(String token) {
        List<SentenceResult> sentences = new ArrayList<>();
        if (token == null || token.isEmpty()) {
            return sentences;
        }

        for (int i = 0; i < token.length();) {
            int codePoint = token.codePointAt(i);
            String charStr = new String(Character.toChars(codePoint));

            contextBuffer.append(charStr);
            if (contextBuffer.length() > CONTEXT_BUFFER_MAX_LENGTH) {
                contextBuffer.delete(0, contextBuffer.length() - CONTEXT_BUFFER_MAX_LENGTH);
            }

            currentSentence.append(charStr);

            boolean isEndMark = SENTENCE_END_PATTERN.matcher(charStr).find();
            boolean isPauseMark = PAUSE_PATTERN.matcher(charStr).find();
            boolean isSpecialMark = SPECIAL_PATTERN.matcher(charStr).find();
            boolean isNewline = NEWLINE_PATTERN.matcher(charStr).find();
            boolean isEmoji = EmojiUtils.isEmoji(codePoint);

            boolean containsKaomoji = false;
            if (currentSentence.length() >= 3) {
                containsKaomoji = EmojiUtils.containsKaomoji(currentSentence.toString());
            }

            if (isEndMark && charStr.equals(".")) {
                String context = contextBuffer.toString();
                Matcher numberMatcher = NUMBER_PATTERN.matcher(context);
                if (numberMatcher.find() && numberMatcher.end() >= context.length() - 3) {
                    isEndMark = false;
                }
            }

            boolean shouldSendSentence = false;
            if (isEndMark || isNewline) {
                shouldSendSentence = true;
            } else if ((isPauseMark || isSpecialMark || isEmoji || containsKaomoji)
                    && currentSentence.length() >= MIN_SENTENCE_LENGTH) {
                shouldSendSentence = true;
            }

            if (shouldSendSentence && currentSentence.length() >= MIN_SENTENCE_LENGTH) {
                String rawSentence = currentSentence.toString().trim();
                List<String> moods = new ArrayList<>();
                String cleanSentence = EmojiUtils.processSentence(rawSentence, moods);
                if (containsSubstantialContent(cleanSentence)) {
                    String mood = moods.isEmpty() ? null : moods.get(0);
                    sentences.add(new SentenceResult(cleanSentence, mood));
                    currentSentence.setLength(0);
                }
            }

            i += Character.charCount(codePoint);
        }

        return sentences;
    }

    /**
     * Segmentação imperativa: libera o conteúdo restante do buffer (chamado ao final do fluxo de texto).
     */
    public SentenceResult take() {
        String rawSentence = currentSentence.toString().trim();
        if (rawSentence.isEmpty()) {
            return new SentenceResult("", null);
        }
        List<String> moods = new ArrayList<>();
        String cleanSentence = EmojiUtils.processSentence(rawSentence, moods);
        String mood = moods.isEmpty() ? null : moods.get(0);
        return new SentenceResult(cleanSentence, mood);
    }

    public void onToken(String token, FluxSink<SentenceResult> sink) {
        for (SentenceResult result : take(token)) {
            sink.next(result);
        }
    }

    public void onComplete(FluxSink<SentenceResult> sink) {
        SentenceResult result = take();
        if (StringUtils.hasText(result.text())) {
            sink.next(result);
        }
        sink.complete();
    }

    public Flux<SentenceResult> convert(Flux<String> stringFlux) {
        return Flux.create(sink ->
                stringFlux.subscribe(
                        token -> this.onToken(token, sink),
                        sink::error,
                        () -> this.onComplete(sink)));
    }

    private boolean containsSubstantialContent(String text) {
        if (text == null || text.trim().length() < MIN_SENTENCE_LENGTH) {
            return false;
        }
        String stripped = text.replaceAll("[\\p{P}\\s]", "");
        return stripped.length() >= 2;
    }
}
