package com.xiaozhi.ai.tts;

import lombok.Builder;
import lombok.Getter;
import org.springframework.ai.audio.tts.TextToSpeechOptions;

/**
 * Objeto de configuração de parâmetros do TTS, encapsulando parâmetros como voiceName/speed/pitch.
 * Implementa diretamente a interface {@link TextToSpeechOptions} do Spring AI, integrando-se perfeitamente ao ecossistema TTS do Spring AI.
 * <p>
 * Todos os TTS Providers recebem este objeto na construção, substituindo os 4 parâmetros independentes originais.
 */
@Getter
@Builder
public class XiaozhiTtsOptions implements TextToSpeechOptions {

    /**
     * Nome do timbre de voz
     */
    private final String voiceName;

    /**
     * Velocidade da fala (0.5-2.0); 1.0 é a velocidade padrão
     */
    @Builder.Default
    private final Double speed = 1.0;

    /**
     * Tom de voz (0.5-2.0); 1.0 é o tom padrão
     */
    @Builder.Default
    private final Double pitch = 1.0;

    // ---- Implementação da interface TextToSpeechOptions do Spring AI ----

    @Override
    public String getModel() {
        return null;
    }

    @Override
    public String getVoice() {
        return voiceName;
    }

    @Override
    public String getFormat() {
        return null;
    }

    @Override
    public Double getSpeed() {
        return speed;
    }

    @SuppressWarnings("unchecked")
    @Override
    public XiaozhiTtsOptions copy() {
        return XiaozhiTtsOptions.builder()
                .voiceName(voiceName)
                .speed(speed)
                .pitch(pitch)
                .build();
    }
}
