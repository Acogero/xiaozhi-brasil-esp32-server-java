package com.xiaozhi.utils;

import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.common.model.bo.UserBO;
import com.xiaozhi.user.service.UserService;

/**
 * Classe utilitária de autenticação
 * Fornece métodos unificados para obtenção de informações do usuário
 *
 * @author Joey
 */
public class AuthUtils {

    private static UserService userService;

    /**
     * Injeta o UserService (via container do Spring)
     */
    public static void setUserService(UserService userService) {
        AuthUtils.userService = userService;
    }

    /**
     * Obtém o ID do usuário atualmente logado
     *
     * @return ID do usuário
     */
    public static Integer getCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsInt();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtém as informações do usuário atualmente logado
     *
     * @return Informações do usuário
     */
    public static UserBO getCurrentUser() {
        Integer userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }

        try {
            return userService.getBO(userId);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Verifica se está logado
     *
     * @return Se está logado
     */
    public static boolean isLogin() {
        return StpUtil.isLogin();
    }

    /**
     * Verifica se o usuário atual tem a permissão especificada
     *
     * @param permission Identificador da permissão
     * @return Se possui a permissão
     */
    public static boolean hasPermission(String permission) {
        try {
            StpUtil.checkPermission(permission);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica se o usuário atual tem o papel especificado
     *
     * @param role Identificador do papel
     * @return Se possui o papel
     */
    public static boolean hasRole(String role) {
        try {
            StpUtil.checkRole(role);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Encerra a sessão
     */
    public static void logout() {
        StpUtil.logout();
    }

    /**
     * Obtém o Token atual
     *
     * @return Token
     */
    public static String getToken() {
        try {
            return StpUtil.getTokenValue();
        } catch (Exception e) {
            return null;
        }
    }
}
