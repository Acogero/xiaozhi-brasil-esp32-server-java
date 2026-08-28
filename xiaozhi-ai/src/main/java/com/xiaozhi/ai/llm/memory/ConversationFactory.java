package com.xiaozhi.ai.llm.memory;

import com.xiaozhi.common.model.bo.RoleBO;

public interface ConversationFactory {
    /**
     * Diferentes implementações de ChatMemory podem ter estratégias de processamento distintas e podem inicializar diferentes subclasses de Conversation.
     *
     * @param ownerId   Identificador do participante do chat (cenário de dispositivo: deviceId; cenário Web: userId)
     * @param userId    ID do usuário
     * @param role      Papel/role
     * @param sessionId ID da sessão
     * @return Conversation (sessão)
     */
    Conversation initConversation(String ownerId, Integer userId, RoleBO role, String sessionId);
}
