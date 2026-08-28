package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Requisição de configuração em lote de ferramentas excluídas do papel")
public class McpRoleExcludeToolsReq {

    @Schema(description = "Lista de ferramentas excluídas", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "A lista de ferramentas excluídas não pode ser vazia")
    private List<String> excludeTools;

    @Schema(description = "Nome do servidor")
    private String serverName;
}
