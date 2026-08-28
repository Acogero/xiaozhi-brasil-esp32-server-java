package com.xiaozhi.config;

import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.common.model.req.ConfigCreateReq;
import com.xiaozhi.common.model.req.ConfigPageReq;
import com.xiaozhi.common.model.req.ConfigUpdateReq;
import com.xiaozhi.common.model.resp.ConfigResp;
import com.xiaozhi.common.model.resp.PageResp;
import com.xiaozhi.config.convert.ConfigConvert;
import com.xiaozhi.config.domain.AiConfig;
import com.xiaozhi.config.domain.repository.ConfigRepository;
import com.xiaozhi.config.service.ConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de aplicação do domínio de configuração.
 * <p>
 * Responsabilidade: orquestra o fluxo entre o Controller e o Domain Service, incluindo:
 * <ul>
 *   <li>Conversão Req/Resp ↔ BO</li>
 *   <li>Validação de regras de negócio entre domínios (checagem de vínculo do modelo de embedding com a memória)</li>
 *   <li>Coordenação de efeitos colaterais (broadcast de mudança de configuração via Redis)</li>
 * </ul>
 * O Controller deve apenas vincular parâmetros e validar permissões; toda a lógica de orquestração de negócio pertence a esta classe.
 */
@Service
public class ConfigAppService {

    @Resource
    private ConfigService configService;

    @Resource
    private ConfigConvert configConvert;

    @Resource
    private ConfigRepository configRepository;

    public PageResp<ConfigResp> page(ConfigPageReq req, Integer userId) {
        ConfigPageReq r = req == null ? new ConfigPageReq() : req;
        return configService.page(r.getPageNo(), r.getPageSize(),
            r.getConfigType(), r.getConfigName(), r.getModelType(),
            r.getProvider(), r.getIsDefault(), r.getState(), userId);
    }

    @Transactional
    public ConfigResp create(ConfigCreateReq req, Integer userId) {
        ConfigBO bo = configConvert.toBO(req);
        bo.setUserId(userId);
        AiConfig config = AiConfig.newConfig(userId, bo);
        configRepository.save(config);
        ConfigBO created = configService.getBO(config.getConfigId());
        return configConvert.toResp(created);
    }

    @Transactional
    public ConfigResp update(Integer configId, ConfigUpdateReq req) {
        ConfigBO existing = configService.getBO(configId);
        if (existing == null) {
            throw new ResourceNotFoundException("Configuração não encontrada ou sem permissão de acesso");
        }
        AiConfig config = configRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("Configuração não encontrada ou sem permissão de acesso"));
        config.update(configConvert.toBO(req));
        configRepository.save(config);
        ConfigBO updated = configService.getBO(configId);
        return configConvert.toResp(updated);
    }

    @Transactional
    public void delete(Integer configId) {
        ConfigBO existing = configService.getBO(configId);
        if (existing == null) {
            throw new ResourceNotFoundException("Configuração não encontrada ou sem permissão de acesso");
        }
        configRepository.delete(configId);
    }

}
