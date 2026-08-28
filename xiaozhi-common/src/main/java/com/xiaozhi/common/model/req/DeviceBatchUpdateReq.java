package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Requisição de atualização em lote de dispositivos")
public class DeviceBatchUpdateReq {

    @NotBlank(message = "O ID do dispositivo não pode ser vazio")
    @Schema(description = "Lista de IDs de dispositivos, separados por vírgula", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceIds;

    @Schema(description = "ID do papel")
    private Integer roleId;
}
