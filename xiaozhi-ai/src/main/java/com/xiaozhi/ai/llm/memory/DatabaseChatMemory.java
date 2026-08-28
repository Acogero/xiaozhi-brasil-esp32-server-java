package com.xiaozhi.ai.llm.memory;

import com.xiaozhi.common.model.bo.MessageBO;
import com.xiaozhi.common.model.bo.MessageMetadataBO;
import com.xiaozhi.common.model.bo.SummaryBO;
import com.xiaozhi.message.service.MessageService;
import com.xiaozhi.summary.service.SummaryService;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import lombok.extern.slf4j.Slf4j;
/**
 * Implementação de memória de chat baseada em banco de dados.
 * Classe singleton global, responsável por obter, salvar e limpar as mensagens dentro da Conversation.
 */
@Slf4j
@Service
public class DatabaseChatMemory implements ChatMemory {

    private final SummaryService summaryService;
    private final MessageService messageService;

    @Autowired
    public DatabaseChatMemory(MessageService messageService, SummaryService summaryService) {
        this.messageService = messageService;
        this.summaryService = summaryService;
    }

    @Override
    public void save(SummaryBO summary) {
        summaryService.save(summary);
    }

    @Override
    public SummaryBO findLastSummary(String ownerId, int roleId) {
        return summaryService.findLast(ownerId, roleId);
    }

    @Override
    public List<Message> find(String ownerId, int roleId, int limit) {
        try {
            return toSpringMessages(messageService.listHistory(ownerId, roleId, limit));
        } catch (Exception e) {
            log.error("Erro ao obter mensagens do histórico (por ownerId+roleId): {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Message> find(String sessionId, int limit) {
        try {
            return toSpringMessages(messageService.listHistory(sessionId, limit));
        } catch (Exception e) {
            log.error("Erro ao obter mensagens do histórico (por sessionId): {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public static @NotNull Message toSpringMessage(MessageBO message) {
        String role = message.getSender();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("messageId", message.getMessageId());

        Message springMessage;
        if (MessageBO.SENDER_TOOL.equals(role)) {
            // ToolResponseMessage: restaura toolCallId e toolName a partir do campo toolCalls
            springMessage = buildToolResponseMessage(message);
        } else if (MessageBO.SENDER_ASSISTANT.equals(role)) {
            // AssistantMessage do tipo TOOL_CALL precisa ter os toolCalls restaurados
            if (MessageBO.MESSAGE_TYPE_TOOL_CALL.equals(message.getMessageType())) {
                springMessage = buildToolCallAssistantMessage(message, metadata);
            } else {
                springMessage = AssistantMessage.builder().content(message.getMessage()).properties(metadata).build();
            }
        } else if (MessageBO.SENDER_USER.equals(role)) {
            // Reinjeta a metadata estruturada persistida (speaker/emotion etc.) em UserMessage.metadata,
            // para que a camada Conversation a projete como prefixo de texto enviado ao LLM
            MessageMetadataBO userMetadata = message.getMetadata();
            if (userMetadata != null) {
                metadata.put(MessageMetadataBO.METADATA_KEY, userMetadata);
            }
            springMessage = UserMessage.builder().text(message.getMessage()).metadata(metadata).build();
        } else {
            throw new IllegalArgumentException("Invalid role: " + role);
        }

        if (message.getCreateTime() != null) {
            MessageTimeMetadata.setTimeMillis(
                springMessage,
                message.getCreateTime().atZone(ZoneId.systemDefault()).toInstant()
            );
        }
        return springMessage;
    }

    /**
     * Reconstrói, a partir do registro do banco, um AssistantMessage com toolCalls
     */
    private static AssistantMessage buildToolCallAssistantMessage(MessageBO message, Map<String, Object> metadata) {
        List<AssistantMessage.ToolCall> toolCalls = List.of();
        try {
            toolCalls = ToolCallMessageCodec.decodeToolCalls(message.getToolCalls());
        } catch (Exception e) {
            log.warn("Falha ao desserializar toolCalls: {}", e.getMessage());
        }
        return AssistantMessage.builder()
                .content(message.getMessage())
                .properties(metadata)
                .toolCalls(toolCalls)
                .build();
    }

    /**
     * Reconstrói, a partir do registro do banco, um ToolResponseMessage
     */
    private static ToolResponseMessage buildToolResponseMessage(MessageBO message) {
        List<ToolResponseMessage.ToolResponse> responses;
        try {
            responses = ToolCallMessageCodec.decodeToolResponses(message.getToolCalls(), message.getMessage());
        } catch (Exception e) {
            log.warn("Falha ao desserializar informações de tool response: {}", e.getMessage());
            responses = new ArrayList<>();
            responses.add(new ToolResponseMessage.ToolResponse("", "", message.getMessage()));
        }
        return ToolResponseMessage.builder().responses(responses).build();
    }

    public static List<Message> toSpringMessages(List<MessageBO> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }
        return messages.stream()
            .filter(message -> MessageBO.SENDER_ASSISTANT.equals(message.getSender())
                || MessageBO.SENDER_USER.equals(message.getSender())
                || MessageBO.SENDER_TOOL.equals(message.getSender()))
            .map(DatabaseChatMemory::toSpringMessage)
            .collect(Collectors.toList());
    }

    @Override
    public List<Message> find(String ownerId, int roleId, Instant timeMillis) {
        return toSpringMessages(messageService.listHistoryAfter(ownerId, roleId, timeMillis));
    }

    @Override
    public void delete(String ownerId, int roleId) {
        try {
            throw new IllegalAccessException("A exclusão do histórico ainda não é suportada");
        } catch (Exception e) {
            log.error("Erro ao limpar o histórico: {}", e.getMessage(), e);
        }
    }
}
