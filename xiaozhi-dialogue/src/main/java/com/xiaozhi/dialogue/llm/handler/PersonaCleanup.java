package com.xiaozhi.dialogue.llm.handler;

import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.communication.common.SessionManager;
import com.xiaozhi.ai.llm.memory.Conversation;
import com.xiaozhi.dialogue.runtime.Persona;
import com.xiaozhi.dialogue.playback.Player;
import com.xiaozhi.event.ChatSessionClosedEvent;
import jakarta.annotation.Resource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Limpa os recursos relacionados ao Persona quando a sessão é fechada (histórico do Conversation, player do Player).
 */
@Component
public class PersonaCleanup {

    @Resource
    private SessionManager sessionManager;

    @EventListener
    public void handleSessionClose(ChatSessionClosedEvent event) {
        ChatSession session = sessionManager.getSession(event.getSessionId());
        Optional.ofNullable(session)
                .map(s -> s.getPersona())
                .map(Persona::getConversation)
                .ifPresent(Conversation::clear);

        Optional.ofNullable(session)
                .map(ChatSession::getPlayer)
                .ifPresent(Player::stop);
    }
}
