package com.xiaozhi.dialogue.runtime.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaozhi.ai.llm.memory.Conversation;
import com.xiaozhi.ai.llm.memory.ToolCallMessageCodec;
import com.xiaozhi.common.model.bo.MessageBO;
import com.xiaozhi.common.model.bo.MessageMetadataBO;
import com.xiaozhi.dialogue.runtime.DialogueContext;
import com.xiaozhi.dialogue.runtime.DialogueTurn;
import com.xiaozhi.dialogue.runtime.ToolChainPair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Conversor de DialogueTurn para uma lista de {@link MessageBO} da camada de persistência.
 * <p>
 * Divide uma rodada de diálogo (user + tool-call/tool-response opcional + assistant) em 2 a 4 MessageBO.
 * Sua responsabilidade é consistente com {@code RoleConverter} / {@code ConfigConverter}: converter objetos de domínio em BOs para persistência/transporte.
 */
@Slf4j
@Component
public class DialogueTurnConverter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Divide o DialogueTurn em uma lista de MessageBO (para persistência em lote pelo MessageService) */
    public List<MessageBO> toMessages(DialogueTurn turn) {
        ChatResponse chatResponse = turn.getChatResponse();
        Generation generation = chatResponse.getResult();
        Assert.notNull(generation, "Generation is null from ChatResponse");

        AssistantMessage finalAssistantMessage = generation.getOutput();

        List<MessageBO> messages = new ArrayList<>();

        // 1. UserMessage
        messages.add(toMessageBO(turn, turn.getUserMessage()));

        // 2. Insere em ordem todas as cadeias de chamadas de ferramentas: cada pair é dividido em Assistant(toolCall) + Tool(response)
        for (ToolChainPair chain : turn.getToolChains()) {
            if (chain == null || chain.toolCallMessage() == null || chain.toolResponseMessage() == null) {
                continue;
            }
            messages.add(toToolCallAssistantMessageBO(turn, chain.toolCallMessage()));
            messages.add(toToolResponseMessageBO(turn, chain.toolResponseMessage()));
        }

        // 3. AssistantMessage final
        messages.add(toMessageBO(turn, finalAssistantMessage));

        return messages;
    }

    private MessageBO toMessageBO(DialogueTurn turn, AbstractMessage message) {
        Conversation conversation = turn.getConversation();
        MessageBO messageBO = new MessageBO();
        messageBO.setUserId(conversation.getUserId());
        messageBO.setDeviceId(conversation.getOwnerId());
        messageBO.setSessionId(conversation.getSessionId());
        messageBO.setSource(MessageBO.SOURCE_DEVICE);
        messageBO.setSender(message.getMessageType().getValue());
        messageBO.setMessage(message.getText());
        messageBO.setRoleId(conversation.getRoleId());
        messageBO.setMessageType(MessageBO.MESSAGE_TYPE_NORMAL);

        Path userSpeechPath = turn.getUserSpeechPath();
        List<DialogueContext.ToolCallInfo> toolCallDetails = turn.getToolCallDetails();

        switch (message.getMessageType()) {
            case USER:
                if (userSpeechPath != null) {
                    messageBO.setAudioPath(userSpeechPath.toString());
                }
                messageBO.setCreateTime(LocalDateTime.ofInstant(turn.getUserMessageCreatedAt(), ZoneId.systemDefault()));
                // Extrai os metadados estruturados de UserMessage.metadata (speaker/emotion etc.) e grava em MessageBO.metadata
                if (message instanceof UserMessage userMessage
                        && userMessage.getMetadata() != null
                        && userMessage.getMetadata().get(MessageMetadataBO.METADATA_KEY) instanceof MessageMetadataBO metadata) {
                    messageBO.setMetadata(metadata);
                }
                break;
            case ASSISTANT:
                messageBO.setCreateTime(LocalDateTime.ofInstant(turn.getAssistantMessageCreatedAt(), ZoneId.systemDefault()));
                if (!toolCallDetails.isEmpty()) {
                    try {
                        messageBO.setToolCalls(OBJECT_MAPPER.writeValueAsString(toolCallDetails));
                    } catch (JsonProcessingException e) {
                        log.warn("Falha ao serializar os detalhes da chamada de ferramenta", e);
                    }
                }
                break;
            default:
                break;
        }

        return messageBO;
    }

    /** Constrói o MessageBO da requisição de chamada de ferramenta (sender=assistant, messageType=TOOL_CALL) */
    private MessageBO toToolCallAssistantMessageBO(DialogueTurn turn, AssistantMessage toolCallAssistantMessage) {
        Conversation conversation = turn.getConversation();

        MessageBO messageBO = new MessageBO();
        messageBO.setUserId(conversation.getUserId());
        messageBO.setDeviceId(conversation.getOwnerId());
        messageBO.setSessionId(conversation.getSessionId());
        messageBO.setSource(MessageBO.SOURCE_DEVICE);
        messageBO.setSender(MessageBO.SENDER_ASSISTANT);
        messageBO.setMessage(toolCallAssistantMessage.getText());
        messageBO.setRoleId(conversation.getRoleId());
        messageBO.setMessageType(MessageBO.MESSAGE_TYPE_TOOL_CALL);
        messageBO.setCreateTime(LocalDateTime.ofInstant(turn.getAssistantMessageCreatedAt(), ZoneId.systemDefault()));
        try {
            messageBO.setToolCalls(ToolCallMessageCodec.encodeToolCalls(toolCallAssistantMessage.getToolCalls()));
        } catch (JsonProcessingException e) {
            log.warn("Falha ao serializar a requisição de tool call", e);
        }
        return messageBO;
    }

    /** Constrói o MessageBO do resultado da execução da ferramenta (sender=tool, messageType=TOOL_RESPONSE) */
    private MessageBO toToolResponseMessageBO(DialogueTurn turn, ToolResponseMessage toolResponseMessage) {
        Conversation conversation = turn.getConversation();

        MessageBO messageBO = new MessageBO();
        messageBO.setUserId(conversation.getUserId());
        messageBO.setDeviceId(conversation.getOwnerId());
        messageBO.setSessionId(conversation.getSessionId());
        messageBO.setSource(MessageBO.SOURCE_DEVICE);
        messageBO.setSender(MessageBO.SENDER_TOOL);
        String responseText = toolResponseMessage.getResponses().stream()
                .map(ToolResponseMessage.ToolResponse::responseData)
                .collect(Collectors.joining("\n"));
        messageBO.setMessage(responseText);
        messageBO.setRoleId(conversation.getRoleId());
        messageBO.setMessageType(MessageBO.MESSAGE_TYPE_TOOL_RESPONSE);
        messageBO.setCreateTime(LocalDateTime.ofInstant(turn.getAssistantMessageCreatedAt(), ZoneId.systemDefault()));
        try {
            messageBO.setToolCalls(ToolCallMessageCodec.encodeToolResponses(toolResponseMessage.getResponses()));
        } catch (JsonProcessingException e) {
            log.warn("Falha ao serializar as informações de tool response", e);
        }
        return messageBO;
    }
}
