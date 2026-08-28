package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Requisição de teste de síntese de voz")
public class TestVoiceReq {

    @Schema(description = "Texto da mensagem", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "O texto da mensagem não pode ser vazio")
    private String message;

    @Schema(description = "Provedor de síntese de voz", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "O provedor não pode ser vazio")
    private String provider;

    @Schema(description = "ID da configuração TTS")
    private Integer ttsId;

    @Schema(description = "Nome do timbre de voz")
    private String voiceName;

    @Schema(description = "Tom de voz (0.5-2.0)")
    private Double ttsPitch;

    @Schema(description = "Velocidade da fala (0.5-2.0)")
    private Double ttsSpeed;
}
