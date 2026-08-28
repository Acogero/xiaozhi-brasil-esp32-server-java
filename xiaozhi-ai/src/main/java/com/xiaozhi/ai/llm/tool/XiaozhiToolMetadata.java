package com.xiaozhi.ai.llm.tool;

import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * @param returnDirect Após chamar a ferramenta, retorna diretamente o resultado da chamada, sem chamar novamente o modelo de linguagem
 * @param disturbed A instrução de linguagem específica que chama esta ferramenta polui o contexto da conversa; indica que a UserMessage que disparou esta chamada de ferramenta deve ser removida da Conversation.
 */
public record XiaozhiToolMetadata(boolean returnDirect) implements ToolMetadata {
}
