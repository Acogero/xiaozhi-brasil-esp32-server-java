package com.xiaozhi.common.model;

/**
 * Unidade de Token da saída em streaming do LLM, distinguindo o processo de raciocínio da resposta final.
 * <p>
 * No pipeline de diálogo do dispositivo, o Synthesizer consome apenas os Tokens do tipo {@code content}; o conteúdo de raciocínio é filtrado.
 * No cenário de chat web, o frontend pode receber tanto {@code thinking} quanto {@code content}, exibindo o processo de raciocínio.
 *
 * @param type tipo: {@code "thinking"} indica processo de raciocínio/pensamento, {@code "content"} indica a resposta final
 * @param text conteúdo do texto
 */
public record ChatToken(String type, String text) {

    public static final String TYPE_THINKING = "thinking";
    public static final String TYPE_CONTENT = "content";

    public static ChatToken thinking(String text) {
        return new ChatToken(TYPE_THINKING, text);
    }

    public static ChatToken content(String text) {
        return new ChatToken(TYPE_CONTENT, text);
    }

    public boolean isThinking() {
        return TYPE_THINKING.equals(type);
    }

    public boolean isContent() {
        return TYPE_CONTENT.equals(type);
    }
}
