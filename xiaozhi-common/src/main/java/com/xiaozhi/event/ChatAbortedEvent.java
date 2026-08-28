package com.xiaozhi.event;

import com.xiaozhi.common.domain.AbstractDomainEvent;
import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * Evento de interrupção iniciado pelo lado do dispositivo (cliente)
 */
@Getter
public class ChatAbortedEvent extends AbstractDomainEvent {

    private final String sessionId;
    private final String deviceId;
    private final String reason;

    public ChatAbortedEvent(Object source, String sessionId, String deviceId, String reason) {
        super(source);
        this.sessionId = sessionId;
        this.deviceId = deviceId;
        this.reason = StringUtils.hasText(reason) ? reason : "Interrompido pelo dispositivo";
    }
}
