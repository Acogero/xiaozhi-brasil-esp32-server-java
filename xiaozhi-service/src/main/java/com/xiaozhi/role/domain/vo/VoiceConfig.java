package com.xiaozhi.role.domain.vo;

/**
 * Objeto de valor de configuração de síntese / reconhecimento de voz.
 */
public record VoiceConfig(Integer ttsId, Integer sttId, String voiceName,
                           Double ttsPitch, Double ttsSpeed) {

    public VoiceConfig {
        if (ttsId != null && ttsId <= 0) ttsId = null;
        if (sttId != null && sttId <= 0) sttId = null;
    }

    public static VoiceConfig defaults() {
        return new VoiceConfig(null, null, null, 1.0, 1.0);
    }
}
