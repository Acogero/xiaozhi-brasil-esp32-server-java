package com.xiaozhi.event;

import com.xiaozhi.common.domain.AbstractDomainEvent;
import lombok.Getter;

/**
 * Evento de dispositivo voltando a ficar online / passando para o estado ocioso
 * Usado para disparar a verificação imediata do resultado da atualização OTA
 */
@Getter
public class DeviceOnlineEvent extends AbstractDomainEvent {
    private final String deviceId;

    public DeviceOnlineEvent(Object source, String deviceId) {
        super(source);
        this.deviceId = deviceId;
    }
}
