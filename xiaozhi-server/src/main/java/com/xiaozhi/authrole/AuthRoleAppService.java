package com.xiaozhi.authrole;

import com.xiaozhi.authrole.service.AuthRoleService;
import com.xiaozhi.common.model.req.AuthRolePageReq;
import com.xiaozhi.common.model.resp.AuthRolePermissionConfigResp;
import com.xiaozhi.common.model.resp.AuthRoleResp;
import com.xiaozhi.common.model.resp.PageResp;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço de aplicação do domínio AuthRole.
 * <p>
 * Responsabilidade: orquestra o fluxo entre o Controller e o Domain Service, incluindo:
 * <ul>
 *   <li>Conversão Req/Resp ↔ BO</li>
 *   <li>Orquestração do gerenciamento de papéis de permissão</li>
 * </ul>
 */
@Service
public class AuthRoleAppService {

    @Resource
    private AuthRoleService authRoleService;

    public PageResp<AuthRoleResp> page(AuthRolePageReq req) {
        AuthRolePageReq r = req == null ? new AuthRolePageReq() : req;
        return authRoleService.page(r.getPageNo(), r.getPageSize(), r.getAuthRoleName(), r.getRoleKey(), r.getStatus());
    }

    public AuthRolePermissionConfigResp getPermissionConfig(Integer authRoleId) {
        return authRoleService.getPermissionConfig(authRoleId);
    }

    public AuthRolePermissionConfigResp assignPermissions(Integer authRoleId, List<Integer> permissionIds) {
        authRoleService.assignPermissions(authRoleId, permissionIds);
        return authRoleService.getPermissionConfig(authRoleId);
    }
}
