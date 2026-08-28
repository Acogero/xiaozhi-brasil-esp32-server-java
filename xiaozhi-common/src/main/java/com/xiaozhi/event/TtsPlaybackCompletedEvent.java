package com.xiaozhi.event;

import com.xiaozhi.common.domain.AbstractDomainEvent;
import lombok.Getter;

/**
 * Evento de fim de reprodução TTS
 * Disparado por Player.sendStop(), usado para notificar os componentes de que a reprodução TTS terminou.
 * O VadService escuta este evento para resetar o estado oculto do Silero, eliminando a contaminação de estado durante a reprodução TTS.
 */
@Getter
public class TtsPlaybackCompletedEvent extends AbstractDomainEvent {

    private final String sessionId;

    public TtsPlaybackCompletedEvent(Object source, String sessionId) {
        super(source);
        this.sessionId = sessionId;
    }
}
