package com.xiaozhi.config.infrastructure;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xiaozhi.config.dal.mysql.dataobject.ConfigDO;
import com.xiaozhi.config.dal.mysql.mapper.ConfigMapper;
import com.xiaozhi.config.domain.AiConfig;
import com.xiaozhi.config.domain.repository.ConfigRepository;
import com.xiaozhi.config.infrastructure.convert.ConfigConverter;
import com.xiaozhi.config.service.ConfigService;
import com.xiaozhi.event.AiConfigChangedEvent;
import jakarta.annotation.Resource;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Implementação do repositório da raiz de agregação AiConfig.
 * <p>
 * Mantém o invariante "único padrão": ao salvar, se o sinal DEFAULT_CHANGED for detectado, primeiro limpa em lote os outros padrões do mesmo tipo e depois salva.
 */
@Repository
public class ConfigRepositoryImpl implements ConfigRepository {

    @Resource
    private ConfigMapper configMapper;

    @Resource
    private ConfigConverter configConverter;

    @Resource
    private CacheManager cacheManager;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Override
    public Optional<AiConfig> findById(Integer configId) {
        if (configId == null) return Optional.empty();
        ConfigDO d = configMapper.selectById(configId);
        return Optional.ofNullable(configConverter.toDomain(d));
    }

    @Override
    @Transactional
    public void save(AiConfig config) {
        ConfigDO d = configConverter.toDO(config);

        var signals = config.pullSignals();
        if (signals.contains(AiConfig.DomainSignal.DEFAULT_CHANGED)) {
            resetDefault(config.getUserId(), config.getConfigType(),
                    config.getModelType(), config.getConfigId());
        }

        if (config.getConfigId() == null) {
            configMapper.insert(d);
            config.assignId(d.getConfigId());
        } else {
            configMapper.updateById(d);
        }

        evictCache(config);

        if (signals.contains(AiConfig.DomainSignal.UPDATED) || signals.contains(AiConfig.DomainSignal.DISABLED)) {
            eventPublisher.publishEvent(new AiConfigChangedEvent(this, config.getConfigType(), config.getConfigId()));
        }
    }

    @Override
    @Transactional
    public void delete(Integer configId) {
        findById(configId).ifPresent(config -> {
            config.disable();
            ConfigDO d = configConverter.toDO(config);
            configMapper.updateById(d);
            evictCache(config);
            eventPublisher.publishEvent(new AiConfigChangedEvent(this, config.getConfigType(), configId));
        });
    }

    // ── Auxiliares privados ─────────────────────────────────────────────────

    private void resetDefault(Integer userId, String configType, String modelType, Integer excludeId) {
        LambdaUpdateWrapper<ConfigDO> w = new LambdaUpdateWrapper<ConfigDO>()
                .eq(ConfigDO::getUserId, userId)
                .eq(ConfigDO::getConfigType, configType)
                .eq(ConfigDO::getState, AiConfig.STATE_ENABLED)
                .eq(ConfigDO::getIsDefault, "1")
                .set(ConfigDO::getIsDefault, "0");
        if (StringUtils.hasText(modelType)) {
            w.eq(ConfigDO::getModelType, modelType);
        }
        if (excludeId != null) {
            w.ne(ConfigDO::getConfigId, excludeId);
        }
        configMapper.update(null, w);
    }

    private void evictCache(AiConfig config) {
        Cache cache = cacheManager.getCache(ConfigService.CACHE_NAME);
        if (cache == null) return;
        if (config.getConfigId() != null) {
            cache.evict(String.valueOf(config.getConfigId()));
        }
        if (StringUtils.hasText(config.getConfigType())) {
            cache.evict("default:" + config.getConfigType());
            if (StringUtils.hasText(config.getModelType())) {
                cache.evict("default:" + config.getConfigType() + ":" + config.getModelType());
            }
        }
    }
}
