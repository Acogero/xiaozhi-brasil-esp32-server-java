package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Requisição de sincronização de dispositivo")
public class DeviceSyncReq {

    @Schema(description = "ID do dispositivo")
    private String deviceId;

    @Schema(description = "Nome do dispositivo")
    private String deviceName;

    @Schema(description = "Nome do WiFi")
    private String wifiName;

    @Schema(description = "IP")
    private String ip;

    @Schema(description = "Localização geográfica")
    private String location;

    @Schema(description = "Modelo do chip")
    private String chipModelName;

    @Schema(description = "Tipo de dispositivo")
    private String type;

    @Schema(description = "Versão do firmware")
    private String version;
}
