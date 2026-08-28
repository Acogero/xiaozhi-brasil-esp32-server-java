package com.xiaozhi.dialogue.runtime;

import com.xiaozhi.utils.EmojiUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Objeto de frase, representa texto. Corresponde ao objeto Speech. Usado para representar e processar o texto correspondente no fluxo Flux.
 * A ordem garante a associação. É processado pelo Player. Sentence ainda precisa tratar texto comum, emojis etc.
 * Sem Path, sem armazenamento de áudio aqui; trate isso separadamente.
 */
@Slf4j
@Data
public class Sentence implements Comparable<Sentence>{
    // Usado para controlar o número de sequência da frase.
    private static final AtomicInteger sentenceCounter = new AtomicInteger(0);

    // Obtém o número de sequência da frase
    private int seq = sentenceCounter.incrementAndGet();

    // Texto original da frase, que pode conter emoticons.
    private final String text;

    // Texto puro que pode ser usado para gerar o TTS, com os emojis filtrados, pois emojis não são adequados para TTS
    private String text4Speech =null;

    // Contém todos os emojis correspondentes
    private List<String> moods=null;

    // Usado para registrar o timestamp de formação de cada frase.
    private final Instant createdAt = Instant.now();

    public Sentence(String text) {
        this.text = text;
    }

    /**
     * Obtém uma cópia não modificável da lista de palavras de emoção
     */
    public List<String> getMoods() {
        if(moods==null){
            moods = new ArrayList<>();
            this.text4Speech = EmojiUtils.processSentence(text,moods);
        }
        return Collections.unmodifiableList(moods);
    }

    public String getText4Speech() {
        if(text4Speech ==null){
            moods = new ArrayList<>();
            this.text4Speech = EmojiUtils.processSentence(text,moods);
        }
        return text4Speech;
    }

    public boolean isOnlyEmoji() {
        // Emojis geralmente não ultrapassam 4 caracteres
        return moods != null && !moods.isEmpty() &&
                (text.trim().length() <= 4);
    }

    @Override
    public int compareTo(Sentence other) {
        // Ordena pelo número de sequência da frase
        return Integer.compare(this.getSeq(), other.getSeq());
    }
}
