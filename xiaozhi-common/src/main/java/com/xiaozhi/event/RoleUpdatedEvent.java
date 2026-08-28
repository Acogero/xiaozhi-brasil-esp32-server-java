package com.xiaozhi.event;

import com.xiaozhi.common.domain.AbstractDomainEvent;
import lombok.Getter;

/**
 * Evento de alteração de configuração do papel (atualização ou exclusão).
 * Publicado por RoleRepositoryImpl.save() / delete(), usado pela camada de sessão de diálogo para atualizar o cache da Persona.
 */
@Getter
public class RoleUpdatedEvent extends AbstractDomainEvent {

    private final Integer roleId;

    public RoleUpdatedEvent(Object source, Integer roleId) {
        super(source);
        this.roleId = roleId;
    }
}
