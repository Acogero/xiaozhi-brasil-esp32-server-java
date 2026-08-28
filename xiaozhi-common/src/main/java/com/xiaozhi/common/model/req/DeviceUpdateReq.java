package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Requisição de atualização de dispositivo")
public class DeviceUpdateReq {

    @Schema(description = "Nome do dispositivo")
    private String deviceName;

    @Schema(description = "ID do papel")
    private Integer roleId;

    @Schema(description = "Localização geográfica")
    private String location;
}
