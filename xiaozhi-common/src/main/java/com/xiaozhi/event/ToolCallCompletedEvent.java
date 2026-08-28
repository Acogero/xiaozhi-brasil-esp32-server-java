package com.xiaozhi.event;

import com.xiaozhi.common.domain.AbstractDomainEvent;
import lombok.Getter;

/**
 * Evento de conclusão de chamada de ferramenta.
 * Publicado após a execução da ferramenta em XiaoZhiToolCallingManager, usado para registro de log e auditoria.
 */
@Getter
public class ToolCallCompletedEvent extends AbstractDomainEvent {

    private final String sessionId;
    private final String toolName;
    private final String arguments;
    private final String result;
    private final boolean success;
    private final long durationMs;

    public ToolCallCompletedEvent(Object source, String sessionId, String toolName, String arguments,
                     String result, boolean success, long durationMs) {
        super(source);
        this.sessionId = sessionId;
        this.toolName = toolName;
        this.arguments = arguments;
        this.result = result;
        this.success = success;
        this.durationMs = durationMs;
    }
}
