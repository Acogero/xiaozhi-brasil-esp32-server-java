package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Consulta paginada de dispositivos")
public class DevicePageReq extends BasePageReq {

    @Schema(description = "ID do dispositivo")
    private String deviceId;

    @Schema(description = "Nome do dispositivo")
    private String deviceName;

    @Schema(description = "Nome do papel")
    private String roleName;

    @Schema(description = "Status do dispositivo")
    private String state;

    @Schema(description = "ID do papel")
    private Integer roleId;
}
