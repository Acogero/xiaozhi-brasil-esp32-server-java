package com.xiaozhi.common.domain;

import java.time.Instant;

/**
 * Interface marcadora de evento de domínio.
 * Todos os eventos de domínio devem implementar esta interface, fornecendo o ID do evento e o momento em que ocorreu.
 * <p>
 * Os eventos existentes herdam ao mesmo tempo de {@link org.springframework.context.ApplicationEvent} (despacho de eventos do Spring)
 * e implementam esta interface (identificação semântica de domínio); as duas responsabilidades se complementam.
 */
public interface DomainEvent {

    /**
     * Identificador único do evento, usado para idempotência e rastreamento de auditoria.
     */
    String eventId();

    /**
     * Momento em que o evento ocorreu.
     */
    Instant occurredOn();
}
