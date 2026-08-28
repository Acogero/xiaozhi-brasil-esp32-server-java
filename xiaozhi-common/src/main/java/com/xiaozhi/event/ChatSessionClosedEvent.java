package com.xiaozhi.event;

import com.xiaozhi.common.domain.AbstractDomainEvent;
import lombok.Getter;

/**
 * Evento de encerramento de sessão
 */
@Getter
public class ChatSessionClosedEvent extends AbstractDomainEvent {

    private final String sessionId;
    private final String deviceId;

    public ChatSessionClosedEvent(Object source, String sessionId, String deviceId) {
        super(source);
        this.sessionId = sessionId;
        this.deviceId = deviceId;
    }
}
