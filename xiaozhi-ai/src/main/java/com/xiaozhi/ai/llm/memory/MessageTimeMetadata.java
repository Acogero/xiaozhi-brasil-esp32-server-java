package com.xiaozhi.ai.llm.memory;

import org.springframework.ai.chat.messages.Message;

import java.time.Instant;

/**
 * Utilitário de metadados de mensagens em tempo de execução da Conversation.
 * Atualmente é responsável, principalmente, por ler e gravar o timestamp da conversa na Message do Spring AI,
 * para reutilização por componentes de execução/memória como DialogueTurn e SummaryConversation.
 */
public final class MessageTimeMetadata {

    private MessageTimeMetadata() {
    }

    public static void setTimeMillis(Message message, Instant timeMillis) {
        message.getMetadata().put(ChatMemory.TIME_MILLIS_KEY, timeMillis);
    }

    public static Instant getTimeMillis(Message message) {
        return (Instant) message.getMetadata().getOrDefault(ChatMemory.TIME_MILLIS_KEY, Instant.now());
    }
}
