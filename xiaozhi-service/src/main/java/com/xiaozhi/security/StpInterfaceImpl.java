package com.xiaozhi.security;

import cn.dev33.satoken.stp.StpInterface;
import com.xiaozhi.authrole.service.AuthRoleService;
import com.xiaozhi.common.model.bo.UserBO;
import com.xiaozhi.common.model.resp.AuthRoleResp;
import com.xiaozhi.common.model.resp.PermissionResp;
import com.xiaozhi.permission.service.PermissionService;
import com.xiaozhi.user.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
/**
 * Implementação da interface de permissões do Sa-Token
 * Usado pelo framework Sa-Token para obter as informações de permissões e papéis do usuário
 *
 * @author Joey
 */
@Slf4j
@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private PermissionService permissionService;

    @Resource
    private UserService userService;

    @Resource
    private AuthRoleService authRoleService;

    /**
     * Retorna a lista de permissões do usuário
     *
     * @param loginId ID do usuário
     * @param loginType Tipo de login
     * @return Lista de permissões
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<String> permissionList = new ArrayList<>();

        try {
            Integer userId = Integer.parseInt(loginId.toString());

            // Consulta as permissões do usuário
            List<PermissionResp> permissions = permissionService.listByUserId(userId);

            // Extrai a key da permissão
            for (PermissionResp permission : permissions) {
                if (permission.getPermissionKey() != null && !permission.getPermissionKey().isEmpty()) {
                    permissionList.add(permission.getPermissionKey());
                }
            }
        } catch (Exception e) {
            // Registra o log mas não lança exceção, retorna lista vazia
            log.error("Falha ao obter permissões do usuário: {}", e.getMessage());
        }

        return permissionList;
    }

    /**
     * Retorna a lista de papéis do usuário
     *
     * @param loginId ID do usuário
     * @param loginType Tipo de login
     * @return Lista de papéis
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> authRoleKeys = new ArrayList<>();

        try {
            Integer userId = Integer.parseInt(loginId.toString());

            // Consulta as informações do usuário
            UserBO user = userService.getBO(userId);

            if (user != null && user.getAuthRoleId() != null) {
                // Consulta as informações de papel de permissão do backoffice
                AuthRoleResp authRole = authRoleService.get(user.getAuthRoleId());

                if (authRole != null && authRole.getRoleKey() != null && !authRole.getRoleKey().isEmpty()) {
                    authRoleKeys.add(authRole.getRoleKey());
                }

                // Se for superadministrador, adiciona o papel admin
                if ("1".equals(user.getIsAdmin())) {
                    authRoleKeys.add("admin");
                }
            }
        } catch (Exception e) {
            // Registra o log mas não lança exceção, retorna lista vazia
            log.error("Falha ao obter papéis do usuário: {}", e.getMessage());
        }

        return authRoleKeys;
    }
}
