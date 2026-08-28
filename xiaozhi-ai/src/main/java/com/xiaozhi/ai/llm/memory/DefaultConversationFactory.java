package com.xiaozhi.ai.llm.memory;

import com.xiaozhi.common.model.bo.RoleBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Constrói a Conversation correspondente com base no memoryType do papel/role.
 * <p>
 * A Conversation é responsável apenas pelo contêiner de mensagens / estratégia de janela / estratégia de resumo.
 */
@Primary
@Service
@Slf4j
public class DefaultConversationFactory implements ConversationFactory {

    @Value("${conversation.max-messages:16}")
    private int maxMessages;

    @Autowired
    private ChatMemory chatMemory;
    @Autowired
    private SummaryConversationFactory summaryConversationFactory;

    @Override
    public Conversation initConversation(String ownerId, Integer userId, RoleBO role, String sessionId) {
        return switch (role.getMemoryType()) {
            case "summary" -> summaryConversationFactory.initConversation(ownerId, userId, role, sessionId);
            case "window" -> MessageWindowConversation.builder().chatMemory(chatMemory)
                    .maxMessages(maxMessages)
                    .ownerId(ownerId)
                    .roleId(role.getRoleId())
                    .roleDesc(role.getRoleDesc())
                    .userId(userId)
                    .sessionId(sessionId)
                    .build();
            default -> {
                log.warn("O sistema atualmente não suporta este tipo de memória desconhecido: {}, será usado o MessageWindowConversation padrão", role.getMemoryType());
                yield MessageWindowConversation.builder().chatMemory(chatMemory)
                    .maxMessages(maxMessages)
                    .ownerId(ownerId)
                    .roleId(role.getRoleId())
                    .roleDesc(role.getRoleDesc())
                    .userId(userId)
                    .sessionId(sessionId)
                    .build();
            }
        };
    }
}
