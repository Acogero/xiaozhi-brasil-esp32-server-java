package com.xiaozhi.common;

import lombok.Getter;

/**
 * Objeto de reprodução de áudio, que basicamente encapsula o array de bytes da fala (obrigatório) e adiciona informações de texto (opcional).
 * O formato dos dados pode ser PCM (padrão) ou um frame Opus pré-codificado (opusEncoded=true).
 */
public class Speech extends org.springframework.ai.audio.tts.Speech {

    @Getter
    private String text="";

    @Getter
    private String mood;

    /**
     * Indica se os dados já são um frame codificado em Opus.
     * true: getOutput() retorna um único frame Opus; o Player não precisa fazer a conversão PCM→Opus.
     * false (padrão): getOutput() retorna dados PCM, que precisam ser codificados em Opus.
     */
    @Getter
    private boolean opusEncoded = false;

    public Speech(byte[] speech) {
        super(speech);
    }
    public Speech(byte[] speech, String text) {
        super(speech);
        this.text = text;
    }

    /**
     * Define as informações de texto (usado em cenários de cache hit, anexando o texto ao primeiro frame)
     */
    public Speech withText(String text) {
        this.text = text;
        return this;
    }

    /**
     * Define as informações de emoção (extraídas de emojis, usadas para exibir expressões no dispositivo)
     */
    public Speech withMood(String mood) {
        this.mood = mood;
        return this;
    }

    /**
     * Cria um objeto Speech com frame Opus pré-codificado
     */
    public static Speech ofOpus(byte[] opusFrame) {
        Speech s = new Speech(opusFrame);
        s.opusEncoded = true;
        return s;
    }

    /**
     * Cria um objeto Speech com frame Opus pré-codificado (com texto)
     */
    public static Speech ofOpus(byte[] opusFrame, String text) {
        Speech s = new Speech(opusFrame, text);
        s.opusEncoded = true;
        return s;
    }
}
