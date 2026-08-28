package com.xiaozhi.common.domain;

import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Classe base de evento de domínio, compatível também com o mecanismo de despacho de eventos {@link ApplicationEvent} do Spring.
 * <p>
 * Todos os eventos de domínio que herdam desta classe obtêm ao mesmo tempo:
 * <ul>
 *   <li>Semântica de DomainEvent (eventId + occurredOn)</li>
 *   <li>Capacidade de despacho do Spring ApplicationEvent</li>
 * </ul>
 */
public abstract class AbstractDomainEvent extends ApplicationEvent implements DomainEvent {

    private final String eventId;
    private final Instant occurredOn;

    protected AbstractDomainEvent(Object source) {
        super(source);
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = Instant.now();
    }

    @Override
    public String eventId() {
        return eventId;
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }
}
