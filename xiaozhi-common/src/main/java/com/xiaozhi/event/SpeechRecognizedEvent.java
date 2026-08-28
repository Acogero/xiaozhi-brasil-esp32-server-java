package com.xiaozhi.event;

import com.xiaozhi.common.domain.AbstractDomainEvent;
import lombok.Getter;

/**
 * Evento de conclusão do reconhecimento de voz STT.
 * Publicado de forma síncrona em DialogueService.startStt() assim que o STT reconhece o texto do usuário.
 *
 * <p><b>Atenção: este evento é processado de forma síncrona em uma virtual thread; os listeners não devem introduzir operações bloqueantes ou atrasos.</b>
 *
 * <p>Pontos de extensão (atualmente sem listeners):
 * <ul>
 *   <li>Log de análise de sentimento: registra o rótulo de emoção do usuário para análise de qualidade da conversa</li>
 *   <li>Filtro de palavras sensíveis: aplica filtragem de segurança no texto do usuário antes de chamar o LLM</li>
 * </ul>
 */
@Getter
public class SpeechRecognizedEvent extends AbstractDomainEvent {

    private final String sessionId;
    private final String text;
    /**
     * Rótulo de emoção detectado na voz do usuário, pode ser null.
     */
    private final String emotion;

    public SpeechRecognizedEvent(Object source, String sessionId, String text, String emotion) {
        super(source);
        this.sessionId = sessionId;
        this.text = text;
        this.emotion = emotion;
    }
}
