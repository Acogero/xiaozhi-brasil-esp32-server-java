package com.xiaozhi.event;

import com.xiaozhi.common.domain.AbstractDomainEvent;
import lombok.Getter;

/**
 * Evento de limpeza do histórico de conversa, notifica a camada de sessão para limpar o histórico de conversa entre instâncias via broadcast Redis
 */
@Getter
public class ConversationHistoryClearedEvent extends AbstractDomainEvent {

    private final String deviceId;

    public ConversationHistoryClearedEvent(Object source, String deviceId) {
        super(source);
        this.deviceId = deviceId;
    }
}
