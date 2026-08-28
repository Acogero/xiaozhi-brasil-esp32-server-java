package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Consulta paginada de agentes")
public class AgentPageReq extends BasePageReq {

    @Schema(description = "Provedor de serviço")
    private String provider;

    @Schema(description = "Nome do agente")
    private String agentName;
}
