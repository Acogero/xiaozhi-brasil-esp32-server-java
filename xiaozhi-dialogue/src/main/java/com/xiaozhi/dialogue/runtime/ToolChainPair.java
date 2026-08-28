package com.xiaozhi.dialogue.runtime;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;

/**
 * Par de request/response de uma chamada de ferramenta:
 * <ul>
 *   <li>{@code toolCallMessage}: AssistantMessage com toolCalls (solicitado pelo modelo ou simulado por um decorator)</li>
 *   <li>{@code toolResponseMessage}: ToolResponseMessage, resultado da execução da ferramenta</li>
 * </ul>
 * <p>
 * Uma rodada de DialogueTurn pode ter de 0 a N pares, em ordem cronológica. Ao persistir, são gravados em ordem em sys_message.
 */
public record ToolChainPair(AssistantMessage toolCallMessage, ToolResponseMessage toolResponseMessage) {
}
