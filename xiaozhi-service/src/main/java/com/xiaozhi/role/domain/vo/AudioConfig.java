package com.xiaozhi.role.domain.vo;

/**
 * Objeto de valor de configuração de áudio VAD (detecção de atividade de voz).
 */
public record AudioConfig(Float vadEnergyTh, Float vadSpeechTh,
                           Float vadSilenceTh, Integer vadSilenceMs) {

    public static AudioConfig defaults() {
        return new AudioConfig(null, null, null, null);
    }
}
