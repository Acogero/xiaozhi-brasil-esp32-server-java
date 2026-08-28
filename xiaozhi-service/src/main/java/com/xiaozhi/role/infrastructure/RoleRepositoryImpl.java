package com.xiaozhi.role.infrastructure;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xiaozhi.common.CacheHelper;
import com.xiaozhi.event.RoleUpdatedEvent;
import com.xiaozhi.role.dal.mysql.dataobject.RoleDO;
import com.xiaozhi.role.dal.mysql.mapper.RoleMapper;
import com.xiaozhi.role.domain.Role;
import com.xiaozhi.role.domain.repository.RoleRepository;
import com.xiaozhi.role.infrastructure.convert.RoleConverter;
import com.xiaozhi.role.service.RoleService;
import jakarta.annotation.Resource;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementação do repositório da raiz de agregação Role.
 * <p>
 * Encapsula o Mapper do MyBatis-Plus, responsável por:
 * <ul>
 *   <li>Conversão DO ↔ raiz de agregação (via {@link RoleConverter})</li>
 *   <li>Manutenção do invariante "único papel padrão" (reset dos demais papéis do mesmo usuário ao salvar)</li>
 *   <li>Invalidação de cache</li>
 * </ul>
 */
@Repository
public class RoleRepositoryImpl implements RoleRepository {

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private RoleConverter roleConverter;

    @Resource
    private CacheManager cacheManager;

    @Resource
    private CacheHelper cacheHelper;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Override
    public Optional<Role> findById(Integer roleId) {
        if (roleId == null) return Optional.empty();
        return Optional.ofNullable(roleMapper.selectById(roleId))
                .map(d -> toRole(d));
    }

    @Override
    @Transactional
    public void save(Role role) {
        RoleDO dataObject = roleConverter.toDataObject(role);

        if (role.getRoleId() == null) {
            if (role.isDefault()) {
                resetDefault(role.getUserId());
            }
            roleMapper.insert(dataObject);
            role.assignId(dataObject.getRoleId());
        } else {
            if (role.isDefault()) {
                resetDefault(role.getUserId(), role.getRoleId());
            }
            roleMapper.updateById(dataObject);
        }

        evictCache(role.getRoleId());

        var signals = role.pullSignals();
        if (signals.contains(Role.DomainSignal.UPDATED)) {
            eventPublisher.publishEvent(new RoleUpdatedEvent(this, role.getRoleId()));
        }
    }

    @Override
    @Transactional
    public void delete(Integer roleId) {
        if (roleId == null) return;
        RoleDO existing = roleMapper.selectById(roleId);
        if (existing != null) {
            roleMapper.delete(new LambdaUpdateWrapper<RoleDO>().eq(RoleDO::getRoleId, roleId));
            evictCache(roleId);
            eventPublisher.publishEvent(new RoleUpdatedEvent(this, roleId));
        }
    }

    /** Redefine a marcação de padrão de todos os papéis do mesmo usuário (chamado antes do insert) */
    private void resetDefault(Integer userId) {
        roleMapper.update(null, new LambdaUpdateWrapper<RoleDO>()
                .eq(RoleDO::getUserId, userId)
                .set(RoleDO::getIsDefault, "0"));
    }

    /** Redefine a marcação de padrão dos demais papéis do mesmo usuário (chamado antes do update, excluindo o próprio) */
    private void resetDefault(Integer userId, Integer excludeRoleId) {
        roleMapper.update(null, new LambdaUpdateWrapper<RoleDO>()
                .eq(RoleDO::getUserId, userId)
                .ne(RoleDO::getRoleId, excludeRoleId)
                .set(RoleDO::getIsDefault, "0"));
    }

    private void evictCache(Integer roleId) {
        if (roleId == null) return;
        Cache cache = cacheManager.getCache(RoleService.CACHE_NAME);
        if (cache != null) cache.evict(String.valueOf(roleId));
    }

    private Role toRole(RoleDO d) {
        return roleConverter.toDomain(d);
    }
}
