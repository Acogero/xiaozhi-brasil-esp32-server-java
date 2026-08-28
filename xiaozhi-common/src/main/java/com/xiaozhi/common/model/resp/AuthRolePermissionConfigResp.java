package com.xiaozhi.common.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Configuração de autorização do papel de permissão do backend")
public class AuthRolePermissionConfigResp extends AuthRoleResp {

    @Schema(description = "Árvore de permissões")
    private List<PermissionTreeResp> permissionTree;

    @Schema(description = "IDs de permissões já selecionadas")
    private List<Integer> checkedPermissionIds;
}
