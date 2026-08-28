package com.xiaozhi.event;

import com.xiaozhi.common.domain.AbstractDomainEvent;
import lombok.Getter;

/**
 * Evento de alteração do papel vinculado ao dispositivo.
 * Publicado por DeviceRepositoryImpl.save() ao detectar o sinal ROLE_CHANGED,
 * dispara o broadcast entre instâncias para reconstruir a Persona.
 */
@Getter
public class DeviceRoleChangedEvent extends AbstractDomainEvent {

    private final String deviceId;

    public DeviceRoleChangedEvent(Object source, String deviceId) {
        super(source);
        this.deviceId = deviceId;
    }
}
