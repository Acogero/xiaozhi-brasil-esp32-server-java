package com.xiaozhi.event;

import com.xiaozhi.common.domain.AbstractDomainEvent;
import lombok.Getter;

/**
 * Evento de encerramento de sessão do dispositivo (ao excluir ou reativar o dispositivo).
 * Publicado por DeviceRepositoryImpl em delete() ou ao detectar o sinal SESSION_CLOSED,
 * dispara o broadcast entre instâncias para encerrar a sessão WebSocket do dispositivo correspondente.
 */
@Getter
public class DeviceSessionClosedEvent extends AbstractDomainEvent {

    private final String deviceId;

    public DeviceSessionClosedEvent(Object source, String deviceId) {
        super(source);
        this.deviceId = deviceId;
    }
}
