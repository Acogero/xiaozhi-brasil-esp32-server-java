package com.xiaozhi.ai.llm.memory;

/**
 * Contexto de execução, passado toda vez que o Prompt é construído.
 * Diferente dos atributos de identidade da Conversation (ownerId/roleId/sessionId),
 * estes campos podem mudar durante a sessão (mudança de localização do dispositivo, etc.).
 *
 * <p>Campos <b>estáveis</b> durante a sessão são injetados aqui no System Prompt (como location);
 * já os que variam por mensagem são anexados pelo {@link UserMessageAssembler} como prefixo de texto em cada UserMessage,
 * evitando que o System Prompt mude a cada rodada e invalide o cache KV do prefixo.
 *
 * @param location Localização do dispositivo / geolocalização por IP no Web / null
 */
public record ConversationContext(
    String location
) {
    public static final ConversationContext EMPTY = new ConversationContext(null);
}
