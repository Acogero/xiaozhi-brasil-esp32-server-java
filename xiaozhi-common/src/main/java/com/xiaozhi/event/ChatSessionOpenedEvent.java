package com.xiaozhi.event;

import com.xiaozhi.common.domain.AbstractDomainEvent;
import lombok.Getter;

/**
 * Evento de abertura (registro) de conexão de sessão.
 * Publicado em SessionManager.registerSession() após o registro da conexão WebSocket do dispositivo.
 *
 * <p>Pontos de extensão (atualmente sem listeners):
 * <ul>
 *   <li>Log de auditoria de conexão: registra o horário em que o dispositivo ficou online e o IP de origem</li>
 *   <li>Notificação de status online: notifica o painel administrativo para atualizar o status do dispositivo em tempo real</li>
 *   <li>Pré-alocação de recursos: carrega antecipadamente para este dispositivo a configuração do papel, recursos de TTS, etc.</li>
 * </ul>
 */
@Getter
public class ChatSessionOpenedEvent extends AbstractDomainEvent {

    private final String sessionId;
    private final String deviceId;

    public ChatSessionOpenedEvent(Object source, String sessionId, String deviceId) {
        super(source);
        this.sessionId = sessionId;
        this.deviceId = deviceId;
    }
}
