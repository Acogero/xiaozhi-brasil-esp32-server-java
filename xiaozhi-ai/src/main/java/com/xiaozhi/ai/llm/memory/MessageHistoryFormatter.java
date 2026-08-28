package com.xiaozhi.ai.llm.memory;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serializa a lista de mensagens do Spring AI em um único bloco de texto, destinado exclusivamente ao cenário de "reenviar ao modelo para gerar um resumo".
 * <p>
 * Convenção de renderização:
 * <ul>
 *   <li>Mensagem TOOL → {@code TOOL:<text>}</li>
 *   <li>ASSISTANT com tool_calls → {@code ASSISTANT:[tool_call:<name1>,<name2>...]}
 *       (sem incluir o conteúdo de texto, para evitar enviar JSON não interpretado de volta ao modelo de resumo)</li>
 *   <li>Demais casos → {@code <TYPE>:<text>}</li>
 * </ul>
 * Originalmente esta lógica estava em {@code SummaryConversation#summaryMessages}
 */
public final class MessageHistoryFormatter {

    private MessageHistoryFormatter() {}

    /**
     * Renderiza a lista de mensagens em uma única string seguindo a convenção acima, separando as mensagens com {@link System#lineSeparator()}.
     */
    public static String format(List<Message> messages) {
        return messages.stream()
                .map(MessageHistoryFormatter::renderOne)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private static String renderOne(Message message) {
        if (message.getMessageType() == MessageType.TOOL) {
            return "TOOL:" + message.getText();
        }
        if (message.getMessageType() == MessageType.ASSISTANT
                && message instanceof AssistantMessage am
                && am.getToolCalls() != null && !am.getToolCalls().isEmpty()) {
            String toolNames = am.getToolCalls().stream()
                    .map(AssistantMessage.ToolCall::name)
                    .collect(Collectors.joining(","));
            return "ASSISTANT:[tool_call:" + toolNames + "]";
        }
        return message.getMessageType() + ":" + message.getText();
    }
}
