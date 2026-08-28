package com.xiaozhi.event;

import com.xiaozhi.common.domain.AbstractDomainEvent;
import lombok.Getter;

/**
 * Evento de canal de áudio conectado.
 * Publicado no WebSocketHandler após o dispositivo enviar o hello e concluir o handshake do canal de áudio.
 *
 * <p>Pontos de extensão (atualmente sem listeners):
 * <ul>
 *   <li>Anúncio de boas-vindas: reproduz um som de boas-vindas ao dispositivo quando o canal de áudio fica pronto</li>
 *   <li>Métricas de monitoramento: mede a latência de estabelecimento do canal de áudio (tempo entre a conexão WebSocket e o áudio ficar pronto)</li>
 *   <li>Pré-aquecimento de VAD/AEC: inicializa antecipadamente os componentes de processamento de áudio, reduzindo a latência da primeira interação</li>
 * </ul>
 */
@Getter
public class ChatAudioOpenedEvent extends AbstractDomainEvent {

    private final String sessionId;
    private final String deviceId;

    public ChatAudioOpenedEvent(Object source, String sessionId, String deviceId) {
        super(source);
        this.sessionId = sessionId;
        this.deviceId = deviceId;
    }
}
