package com.xiaozhi.ai.llm.memory;

import lombok.Builder;
import org.springframework.ai.chat.messages.*;

import java.util.*;

import lombok.extern.slf4j.Slf4j;
/**
 * Implementação de Conversation que limita a quantidade de mensagens (janela de mensagens). De acordo com diferentes estratégias, é possível implementar a persistência, o carregamento e a limpeza da sessão de chat.
 * Memória de curto prazo: só consegue lembrar uma quantidade limitada de mensagens da conversa atual (múltiplas rodadas).
 */
@Slf4j
public class MessageWindowConversation extends Conversation {
    private final int maxMessages;
    /**
     * Construtor com dimensão de carregamento alternável. A fábrica estática {@code builder()} e os setters encadeados são gerados pelo Lombok {@link Builder}.
     * <ul>
     *   <li>{@code sessionScoped=false} (padrão): consulta por ownerId + roleId via {@link ChatMemory#find(String, int, int)}, agregando entre sessions no cenário de dispositivo</li>
     *   <li>{@code sessionScoped=true}: consulta por sessionId via {@link ChatMemory#find(String, int)}, isolado por sessão no cenário Web</li>
     * </ul>
     */
    @Builder
    public MessageWindowConversation(String ownerId, Integer roleId, String sessionId, String roleDesc, Integer userId,
                                      int maxMessages, ChatMemory chatMemory, boolean sessionScoped){
        super(ownerId, roleId, sessionId, roleDesc, userId);
        this.maxMessages = maxMessages;

        List<Message> history = sessionScoped
                ? chatMemory.find(sessionId, maxMessages)
                : chatMemory.find(ownerId, roleId, maxMessages);
        log.info("Carregando histórico da conversa: sessionScoped={}, ownerId={}, sessionId={}, size={}",
                sessionScoped, ownerId, sessionId, history.size());
        super.messages.addAll(history);
    }

    @Override
    public synchronized void add(Message message) {
        if (message instanceof UserMessage || message instanceof AssistantMessage || message instanceof ToolResponseMessage) {
            messages.add(message);
        } else {
            log.warn("Tipo de mensagem não suportado: {}",message.getClass().getName());
        }
    }

    /**
     * Retorna a lista de mensagens com o System Prompt, recebendo o contexto de execução (localização, voiceprint etc.)
     */
    public synchronized List<Message> messages(ConversationContext context) {
        // Corte por grupo de conversa: grupo simples=[User,Assistant] (2 mensagens), grupo com ferramenta=[User,Assistant(toolCall),Tool,Assistant(final)] (4 mensagens)
        while (messages.size() > maxMessages + 1) {
            if (messages.size() >= 2 && messages.get(1) instanceof AssistantMessage am
                    && am.getToolCalls() != null && !am.getToolCalls().isEmpty()
                    && messages.size() >= 4) {
                // Grupo de conversa com ferramenta: remove 4 mensagens [User, Assistant(toolCall), Tool, Assistant(final)]
                for (int i = 0; i < 4 && !messages.isEmpty(); i++) {
                    messages.remove(0);
                }
            } else {
                // Grupo de conversa simples: remove 2 mensagens [User, Assistant]
                messages.remove(0);
                if (!messages.isEmpty()) {
                    messages.remove(0);
                }
            }
        }
        // Novo objeto de lista de mensagens, para evitar poluir o objeto de lista original durante o uso
        List<Message> historyMessages = new ArrayList<>();
        var roleSystemMessage = roleSystemMessage(context);
        if(roleSystemMessage.isPresent()){
            historyMessages.add(roleSystemMessage.get());
        }
        historyMessages.addAll(messages);
        // UserMessage é montada com uma cópia prefixada, de acordo com a metadata, para uso pelo LLM
        return historyMessages.stream().map(UserMessageAssembler::assemble).toList();
    }

    @Override
    public synchronized List<Message> messages() {
        return messages(ConversationContext.EMPTY);
    }

}
