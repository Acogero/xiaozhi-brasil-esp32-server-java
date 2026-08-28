package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Requisição de adição de dispositivo")
public class DeviceCreateReq {

    @NotBlank(message = "O código de verificação do dispositivo não pode ser vazio")
    @Schema(description = "Código de verificação do dispositivo", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
}
