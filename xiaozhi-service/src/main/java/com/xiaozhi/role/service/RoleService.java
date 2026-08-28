package com.xiaozhi.role.service;

import com.xiaozhi.common.model.bo.RoleBO;
import com.xiaozhi.common.model.resp.PageResp;
import com.xiaozhi.common.model.resp.RoleResp;

import java.util.List;

public interface RoleService {

    /** Nome do cache de papéis (usado por RoleServiceImpl para leitura e por RoleRepositoryImpl para invalidação após escrita) */
    String CACHE_NAME = "XiaoZhi:Role";

    // ===================== Operações de consulta =====================

    PageResp<RoleResp> page(int pageNo, int pageSize, Integer roleId, String roleName,
                            String isDefault, String state, Integer userId);

    RoleBO getBO(Integer roleId);

    List<RoleBO> listBO(Integer userId, int limit);

    RoleBO getDefaultOrFirstBO(Integer userId);

    // ===================== Operações de escrita (a migrar para RoleAppService) =====================

    Integer copyDefaultRole(Integer sourceUserId, Integer targetUserId);
}
