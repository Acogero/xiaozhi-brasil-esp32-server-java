package com.xiaozhi.authrole;

import com.xiaozhi.server.web.BaseController;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.authrole.AuthRoleAppService;
import com.xiaozhi.common.model.req.AuthRolePageReq;
import java.util.List;
import com.xiaozhi.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth-role")
@Tag(name = "Papel de permissão", description = "Operações relacionadas a papéis de permissão")
public class AuthRoleController extends BaseController {

    @Resource
    private AuthRoleAppService authRoleAppService;

    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:auth-role:api:list")
    @Operation(summary = "Consulta papéis de permissão de acordo com os filtros", description = "Retorna a lista de papéis de permissão")
    public ApiResponse<?> list(@Valid AuthRolePageReq req) {
        return ApiResponse.success(authRoleAppService.page(req));
    }

    @GetMapping("/{authRoleId}/permissions")
    @ResponseBody
    @SaCheckPermission("system:auth-role:api:detail")
    @Operation(summary = "Obter configuração de permissões do papel de permissão", description = "Retorna a árvore de permissões do papel e as permissões já selecionadas")
    public ApiResponse<?> getPermissionConfig(@PathVariable Integer authRoleId) {
        return ApiResponse.success(authRoleAppService.getPermissionConfig(authRoleId));
    }

    @PutMapping("/{authRoleId}/permissions")
    @ResponseBody
    @SaCheckPermission("system:auth-role:api:assign")
    @AuditLog(module = "Gerenciamento de permissões", operation = "Atualizar permissões do papel")
    @Operation(summary = "Atualizar as permissões do papel de permissão", description = "Salva as permissões selecionadas do papel")
    public ApiResponse<?> assignPermissions(
        @PathVariable Integer authRoleId,
        @RequestBody(required = false) List<Integer> permissionIds
    ) {
        return ApiResponse.success(authRoleAppService.assignPermissions(authRoleId, permissionIds));
    }
}
