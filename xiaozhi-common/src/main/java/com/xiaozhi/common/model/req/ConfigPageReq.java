package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Consulta paginada de configurações")
public class ConfigPageReq extends BasePageReq {

    @Schema(description = "Tipo de configuração")
    private String configType;

    @Schema(description = "Nome da configuração")
    private String configName;

    @Schema(description = "Tipo de modelo")
    private String modelType;

    @Schema(description = "Provedor de serviço")
    private String provider;

    @Schema(description = "Se é a configuração padrão (1 sim, 0 não)")
    private String isDefault;

    @Schema(description = "Status (1 habilitado, 0 desabilitado)")
    private String state;
}
