package com.xiaozhi.common.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Nó da árvore de permissões")
public class PermissionTreeResp extends PermissionResp {

    @Schema(description = "Subpermissão")
    private List<PermissionTreeResp> children;
}
