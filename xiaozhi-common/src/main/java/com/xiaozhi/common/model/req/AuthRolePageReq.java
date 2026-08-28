package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Consulta paginada de papéis de permissão do backend")
public class AuthRolePageReq extends BasePageReq {

    @Schema(description = "Nome do papel")
    private String authRoleName;

    @Schema(description = "Identificador do papel")
    private String roleKey;

    @Schema(description = "Status")
    private String status;
}
