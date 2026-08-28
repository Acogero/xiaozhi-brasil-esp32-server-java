package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Schema(description = "Informações do agente")
public class AgentResp {

    @Schema(description = "ID da configuração")
    private Integer configId;

    @Schema(description = "ID do usuário")
    private Integer userId;

    @Schema(description = "ID do dispositivo")
    private String deviceId;

    @Schema(description = "ID do papel")
    private Integer roleId;

    @Schema(description = "Nome da configuração")
    private String configName;

    @Schema(description = "Descrição da configuração")
    private String configDesc;

    @Schema(description = "Tipo de configuração")
    private String configType;

    @Schema(description = "Tipo de modelo")
    private String modelType;

    @Schema(description = "Provedor de serviço")
    private String provider;

    @Schema(description = "AppId atribuído pelo provedor de serviço")
    private String appId;

    @Schema(description = "Endereço da API do provedor de serviço")
    private String apiUrl;

    @Schema(description = "Status (1 habilitado, 0 desabilitado)")
    private String state;

    @Schema(description = "Se é a configuração padrão (1 sim, 0 não)")
    private String isDefault;

    @Schema(description = "ID do agente")
    private Integer agentId;

    @Schema(description = "Nome do agente")
    private String agentName;

    @Schema(description = "ID do agente da plataforma")
    private String botId;

    @Schema(description = "Descrição do agente")
    private String agentDesc;

    @Schema(description = "URL do ícone")
    private String iconUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Data de publicação")
    private Date publishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Data de criação")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Data de atualização")
    private LocalDateTime updateTime;
}
