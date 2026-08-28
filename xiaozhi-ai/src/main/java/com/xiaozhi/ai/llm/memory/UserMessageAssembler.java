package com.xiaozhi.ai.llm.memory;

import com.xiaozhi.common.model.bo.MessageMetadataBO;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Monta a UserMessage com prefixo de metadados.
 *
 * <p>Formato de saída (convenção F1):
 * <pre>
 * [2026-04-18T12:35:42][interlocutor:João][neutral] Quem é o protagonista de Jornada ao Oeste?
 * </pre>
 *
 * <p>Princípios de design:
 * <ul>
 *   <li>Os metadados são enviados ao LLM como prefixo da UserMessage (não como System Prompt, nem como Tool Call),
 *       System Prompt permanece estável, o que favorece o cache KV do prefixo; cada mensagem do histórico carrega seus próprios atributos de tempo/espaço.</li>
 *   <li>Ordem fixa dos campos: timestamp → interlocutor → emoção; quando ausente, o colchete correspondente é omitido.</li>
 *   <li>Timestamp sem key; interlocutor com <code>interlocutor:</code>; emoção sem key (mantendo a convenção <code>[neutral]</code>).</li>
 * </ul>
 *
 * <p>As regras de interpretação precisam estar no System Prompt, veja
 * {@link Conversation#roleSystemMessage(ConversationContext)}。
 */
public final class UserMessageAssembler {

    /**
     * ISO_LOCAL_DATE_TIME com precisão de segundos, garantindo que os segundos sejam sempre exibidos, facilitando o parsing pelo modelo.
     */
    public static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private UserMessageAssembler() {}

    /**
     * Alto nível: monta uma nova UserMessage com prefixo, com base no timestamp em UserMessage.metadata / {@link MessageMetadataBO}.
     * <ul>
     *   <li>Não é UserMessage: retornada sem alteração.</li>
     *   <li>Os três metadados estão ausentes: retornada sem alteração (compatível com mensagens antigas sem metadata).</li>
     * </ul>
     */
    public static Message assemble(Message m) {
        if (!(m instanceof UserMessage um)) {
            return m;
        }
        Map<String, Object> meta = um.getMetadata();
        Instant timestamp = meta != null && meta.get(ChatMemory.TIME_MILLIS_KEY) instanceof Instant i ? i : null;
        MessageMetadataBO bo = meta != null && meta.get(MessageMetadataBO.METADATA_KEY) instanceof MessageMetadataBO b
                ? b : null;
        String emotion = bo != null ? bo.getEmotion() : null;
        if (timestamp == null && !StringUtils.hasText(emotion)) {
            return um;
        }
        return UserMessage.builder()
                .text(assemble(um.getText(), timestamp, emotion))
                .metadata(meta == null ? new HashMap<>() : new HashMap<>(meta))
                .build();
    }

    /**
     * Baixo nível: concatenação simples de strings; normalmente não é usado diretamente por fora.
     */
    public static String assemble(String text, Instant timestamp, String emotion) {
        StringBuilder sb = new StringBuilder();
        if (timestamp != null) {
            LocalDateTime ldt = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
            sb.append('[').append(ldt.format(TIMESTAMP_FORMATTER)).append(']');
        }
        if (StringUtils.hasText(emotion)) {
            sb.append('[').append(emotion).append(']');
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(text == null ? "" : text);
        return sb.toString();
    }
}
