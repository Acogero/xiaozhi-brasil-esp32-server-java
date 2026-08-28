package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Requisição para alternar o status global da ferramenta MCP")
public class McpGlobalToolStatusReq {

    @Schema(description = "Nome da ferramenta", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "O nome da ferramenta não pode ser vazio")
    private String toolName;

    @Schema(description = "Nome do servidor", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "O nome do servidor não pode ser vazio")
    private String serverName;

    @Schema(description = "Se está habilitado", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "O status de habilitação não pode ser vazio")
    private Boolean enabled;
}
