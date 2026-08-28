package com.xiaozhi.event;

import com.xiaozhi.common.domain.AbstractDomainEvent;
import lombok.Getter;

/**
 * Evento de alteração de configuração do modelo de IA (atualização ou exclusão).
 * Publicado por ConfigRepositoryImpl.save() / delete(), dispara o broadcast de invalidação de cache de STT/TTS/Token.
 */
@Getter
public class AiConfigChangedEvent extends AbstractDomainEvent {

    private final String configType;
    private final Integer configId;

    public AiConfigChangedEvent(Object source, String configType, Integer configId) {
        super(source);
        this.configType = configType;
        this.configId = configId;
    }
}
