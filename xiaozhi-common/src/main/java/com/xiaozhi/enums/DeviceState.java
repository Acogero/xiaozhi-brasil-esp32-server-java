package com.xiaozhi.enums;

/**
 * Máquina de estados do dispositivo no lado do servidor
 * Usada para proteção contra timeout e percepção geral do estado, substituindo os antigos campos booleanos dispersos playing / musicPlaying / streamingState / inWakeupResponse.
 *
 * Fluxo de estados:
 * IDLE ──(wake/listen)──→ LISTENING ──(speech_end/STT done)──→ THINKING ──(TTS start)──→ SPEAKING ──(TTS stop)──→ IDLE
 *
 * Regra de proteção contra timeout: a desconexão por timeout só pode ser disparada no estado IDLE.
 */
public enum DeviceState {

    /**
     * Ocioso: dispositivo conectado, mas sem interação de áudio ativa.
     * Permite disparar timeout por inatividade.
     */
    IDLE,

    /**
     * Ouvindo: o dispositivo está gravando e realizando reconhecimento STT em streaming.
     * Inclui o antigo streamingState=true e a fase de espera após o recebimento da palavra de ativação.
     * Não dispara timeout.
     */
    LISTENING,

    /**
     * Pensando: STT concluído, LLM em inferência, TTS ainda não iniciado.
     * Não dispara timeout.
     */
    THINKING,

    /**
     * Falando: frames de áudio TTS estão sendo enviados ao dispositivo (incluindo a fase de resposta de ativação).
     * Não dispara timeout.
     */
    SPEAKING
}
