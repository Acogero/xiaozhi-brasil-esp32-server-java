package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Requisição de atualização de configuração")
public class ConfigUpdateReq {

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

    @Schema(description = "ApiKey atribuída pelo provedor de serviço")
    private String apiKey;

    @Schema(description = "ApiSecret atribuído pelo provedor de serviço")
    private String apiSecret;

    @Schema(description = "Access Key atribuída pelo provedor de serviço")
    private String ak;

    @Schema(description = "Secret Key atribuída pelo provedor de serviço")
    private String sk;

    @Schema(description = "Endereço da API do provedor de serviço")
    private String apiUrl;

    @Schema(description = "Status (1 habilitado, 0 desabilitado)")
    private String state;

    @Schema(description = "Se é a configuração padrão (1 sim, 0 não)")
    private String isDefault;

    @Schema(description = "Se o modo de pensamento está habilitado (efetivo quando o modelo suportar)")
    private Boolean enableThinking;
}
