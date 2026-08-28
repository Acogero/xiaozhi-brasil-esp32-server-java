package com.xiaozhi.ai.tool.session;

/**
 * Localizador de sessão — substitui a dependência direta de SessionManager.
 */
public interface ToolSessionProvider {

    ToolSession getSession(String sessionId);

    ToolSession getSessionByDeviceId(String deviceId);
}
