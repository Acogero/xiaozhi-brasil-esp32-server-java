package com.xiaozhi.event;

import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.domain.AbstractDomainEvent;
import lombok.Getter;

/**
 * Evento de alteração de informações do dispositivo, notifica a camada de sessão para sincronizar as informações do dispositivo
 */
@Getter
public class DeviceUpdatedEvent extends AbstractDomainEvent {

    private final DeviceBO device;

    public DeviceUpdatedEvent(Object source, DeviceBO device) {
        super(source);
        this.device = device;
    }
}
