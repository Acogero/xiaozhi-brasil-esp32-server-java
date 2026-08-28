package com.xiaozhi.dialogue.adapter;

import com.xiaozhi.ai.tool.session.ToolSession;
import com.xiaozhi.ai.tool.session.ToolSessionProvider;
import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.communication.common.SessionManager;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * Implementação de ToolSessionProvider da camada dialogue.
 * Delega as requisições de busca de sessão da camada ai ao SessionManager do dialogue,
 * encapsulando com ChatSessionToolAdapter para isolar os detalhes da camada de comunicação.
 */
@Component
public class DialogueToolSessionProvider implements ToolSessionProvider {

    @Resource
    private SessionManager sessionManager;

    @Override
    public ToolSession getSession(String sessionId) {
        ChatSession session = sessionManager.getSession(sessionId);
        return session != null ? new ChatSessionToolAdapter(session) : null;
    }

    @Override
    public ToolSession getSessionByDeviceId(String deviceId) {
        ChatSession session = sessionManager.getSessionByDeviceId(deviceId);
        return session != null ? new ChatSessionToolAdapter(session) : null;
    }
}
