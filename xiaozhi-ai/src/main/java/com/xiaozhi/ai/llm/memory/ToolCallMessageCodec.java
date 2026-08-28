package com.xiaozhi.ai.llm.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Codec JSON do campo MessageBO#toolCalls.
 *
 * <p>Como o contrato de formato compartilhado entre o lado de escrita (DialogueTurnConverter) e
 * o lado de leitura ({@link DatabaseChatMemory}): os nomes dos campos JSON são declarados centralmente aqui; qualquer alteração em um dos lados afeta o outro.
 *
 * <p>Dois tipos de payload previstos:
 * <ul>
 *   <li>Requisição de Tool call ({@code sender=assistant, messageType=TOOL_CALL}):
 *       {@code [{id, name, arguments}, ...]}</li>
 *   <li>Recibo de Tool response ({@code sender=tool, messageType=TOOL_RESPONSE}):
 *       {@code [{toolCallId, toolName}, ...]}, o texto da resposta em si é armazenado em MessageBO#message.</li>
 * </ul>
 */
public final class ToolCallMessageCodec {

    /** Campo de Tool call. */
    static final String FIELD_ID = "id";
    static final String FIELD_NAME = "name";
    static final String FIELD_ARGUMENTS = "arguments";

    /** Campo de Tool response. */
    static final String FIELD_TOOL_CALL_ID = "toolCallId";
    static final String FIELD_TOOL_NAME = "toolName";

    /** Spring AI {@link AssistantMessage.ToolCall#type()} atualmente é fixo como "function". */
    private static final String TOOL_CALL_TYPE_FUNCTION = "function";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<Map<String, String>>> RAW_LIST_TYPE = new TypeReference<>() {};

    private ToolCallMessageCodec() {}

    /** Serializa a lista de toolCalls da AssistantMessage. */
    public static String encodeToolCalls(List<AssistantMessage.ToolCall> toolCalls) throws JsonProcessingException {
        List<Map<String, String>> raw = toolCalls.stream()
                .map(tc -> Map.of(
                        FIELD_ID, tc.id(),
                        FIELD_NAME, tc.name(),
                        FIELD_ARGUMENTS, tc.arguments()))
                .toList();
        return OBJECT_MAPPER.writeValueAsString(raw);
    }

    /** Desserializa para a lista de AssistantMessage.ToolCall; entrada vazia/em branco retorna lista vazia. */
    public static List<AssistantMessage.ToolCall> decodeToolCalls(String json) throws IOException {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        List<Map<String, String>> raw = OBJECT_MAPPER.readValue(json, RAW_LIST_TYPE);
        return raw.stream()
                .map(m -> new AssistantMessage.ToolCall(
                        m.getOrDefault(FIELD_ID, ""),
                        TOOL_CALL_TYPE_FUNCTION,
                        m.getOrDefault(FIELD_NAME, ""),
                        m.getOrDefault(FIELD_ARGUMENTS, "")))
                .toList();
    }

    /** Serializa as responses da ToolResponseMessage (salva apenas id / name; o texto da resposta é persistido separadamente pelo chamador). */
    public static String encodeToolResponses(List<ToolResponseMessage.ToolResponse> responses) throws JsonProcessingException {
        List<Map<String, String>> raw = responses.stream()
                .map(r -> Map.of(
                        FIELD_TOOL_CALL_ID, r.id(),
                        FIELD_TOOL_NAME, r.name()))
                .toList();
        return OBJECT_MAPPER.writeValueAsString(raw);
    }

    /**
     * Desserializa para a lista de ToolResponseMessage.ToolResponse.
     * <p>Como, na persistência, o texto de múltiplas respostas é mesclado em um único MessageBO#message, na desserialização todas as respostas compartilham o mesmo {@code content}.
     * <p>Se {@code json} estiver vazio ou a lista resultante do parse estiver vazia, será retornada uma resposta única de fallback (id e name como strings vazias).
     */
    public static List<ToolResponseMessage.ToolResponse> decodeToolResponses(String json, String content) throws IOException {
        List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
        if (json != null && !json.isBlank()) {
            List<Map<String, String>> raw = OBJECT_MAPPER.readValue(json, RAW_LIST_TYPE);
            for (Map<String, String> m : raw) {
                responses.add(new ToolResponseMessage.ToolResponse(
                        m.getOrDefault(FIELD_TOOL_CALL_ID, ""),
                        m.getOrDefault(FIELD_TOOL_NAME, ""),
                        content));
            }
        }
        if (responses.isEmpty()) {
            responses.add(new ToolResponseMessage.ToolResponse("", "", content));
        }
        return responses;
    }
}
