package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Consulta paginada de papéis")
public class RolePageReq extends BasePageReq {

    @Schema(description = "ID do papel")
    private Integer roleId;

    @Schema(description = "Nome do papel")
    private String roleName;

    @Schema(description = "Se é o papel padrão (1 sim, 0 não)")
    private String isDefault;

    @Schema(description = "Status (1 habilitado, 0 desabilitado)")
    private String state;
}
