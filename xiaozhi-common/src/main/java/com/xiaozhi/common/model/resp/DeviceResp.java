package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Informações do dispositivo")
public class DeviceResp {

    @Schema(description = "ID do dispositivo")
    private String deviceId;

    @Schema(description = "ID da sessão atual")
    private String sessionId;

    @Schema(description = "Nome do dispositivo")
    private String deviceName;

    @Schema(description = "ID do papel")
    private Integer roleId;

    @Schema(description = "Nome do papel")
    private String roleName;

    @Schema(description = "Status do dispositivo")
    private String state;

    @Schema(description = "Total de mensagens")
    private Integer totalMessage;

    @Schema(description = "Código de verificação")
    private String code;

    @Schema(description = "Caminho do áudio")
    private String audioPath;

    @Schema(description = "Nome do WiFi")
    private String wifiName;

    @Schema(description = "IP")
    private String ip;

    @Schema(description = "Modelo do chip")
    private String chipModelName;

    @Schema(description = "Tipo de dispositivo")
    private String type;

    @Schema(description = "Versão do firmware")
    private String version;

    @Schema(description = "Lista de capacidades MCP do dispositivo")
    private String mcpList;

    @Schema(description = "Localização geográfica")
    private String location;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Data de criação")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Data de atualização")
    private LocalDateTime updateTime;
}
