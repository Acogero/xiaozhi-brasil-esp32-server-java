package com.xiaozhi.ai.tool.session;

import com.xiaozhi.ai.tool.ToolsSessionHolder;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;

/**
 * Abstração de sessão de ferramenta — substitui a dependência direta de ChatSession.
 * A camada ai interage com a sessão através desta interface, sem conhecer o protocolo de comunicação específico.
 */
public interface ToolSession {

    String getSessionId();

    Integer getRoleId();

    String getDeviceId();

    ToolsSessionHolder getToolsSessionHolder();

    /** Se o MCP do dispositivo já foi inicializado */
    boolean isDeviceMcpInitialized();

    void addToolCallDetail(String name, String args, String result);

    /** Armazena a mensagem intermediária da chamada de ferramenta (requisição tool_call do modelo + resultado da execução), usada para injetar no histórico da Conversation */
    void addToolCallMessages(AssistantMessage toolCallAssistantMessage,
                             ToolResponseMessage toolResponseMessage);

    void sendTextMessage(String message);

    boolean isOpen();

    /** Marca o estado da chamada de ferramenta (evita que o player chame sendStop antecipadamente) */
    void setToolCalling(boolean calling);
}
