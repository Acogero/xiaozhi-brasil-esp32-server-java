package com.xiaozhi.role;

import com.xiaozhi.common.model.bo.RoleBO;
import com.xiaozhi.common.model.req.RoleCreateReq;
import com.xiaozhi.common.model.req.RolePageReq;
import com.xiaozhi.common.model.req.RoleUpdateReq;
import com.xiaozhi.common.model.resp.PageResp;
import com.xiaozhi.common.model.resp.RoleResp;
import com.xiaozhi.role.convert.RoleConvert;
import com.xiaozhi.role.domain.Role;
import com.xiaozhi.role.domain.repository.RoleRepository;
import com.xiaozhi.role.domain.vo.AudioConfig;
import com.xiaozhi.role.domain.vo.LlmConfig;
import com.xiaozhi.role.domain.vo.MemoryStrategy;
import com.xiaozhi.role.domain.vo.VoiceConfig;
import com.xiaozhi.role.service.RoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de aplicação do domínio de papéis.
 * <p>
 * Responsabilidade: orquestra o fluxo entre o Controller e o Domain Service, incluindo:
 * <ul>
 *   <li>Conversão Req/Resp ↔ BO</li>
 *   <li>Conversão Req/Resp e orquestração de negócio</li>
 * </ul>
 */
@Service
public class RoleAppService {

    @Resource
    private RoleService roleService;

    @Resource
    private RoleRepository roleRepository;

    @Resource
    private RoleConvert roleConvert;

    public PageResp<RoleResp> page(RolePageReq req, Integer userId) {
        RolePageReq r = req == null ? new RolePageReq() : req;
        return roleService.page(r.getPageNo(), r.getPageSize(),
            r.getRoleId(), r.getRoleName(), r.getIsDefault(), r.getState(), userId);
    }

    @Transactional
    public RoleResp create(RoleCreateReq req, Integer userId) {
        Role role = Role.newRole(userId, req.getRoleName(), req.getRoleDesc(), req.getAvatar(),
                new LlmConfig(req.getModelId(), req.getTemperature(), req.getTopP()),
                new VoiceConfig(req.getTtsId(), req.getSttId(), req.getVoiceName(), req.getTtsPitch(), req.getTtsSpeed()),
                new AudioConfig(req.getVadEnergyTh(), req.getVadSpeechTh(), req.getVadSilenceTh(), req.getVadSilenceMs()),
                new MemoryStrategy(req.getMemoryType()),
                "1".equals(req.getIsDefault()));
        roleRepository.save(role);

        RoleBO created = roleService.getBO(role.getRoleId());
        if (created == null) throw new IllegalStateException("Falha ao criar papel");
        return roleConvert.toResp(created);
    }

    @Transactional
    public RoleResp update(Integer roleId, RoleUpdateReq req) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new com.xiaozhi.common.exception.ResourceNotFoundException("Papel não encontrado ou sem permissão de acesso"));

        role.update(req.getRoleName(), req.getRoleDesc(), req.getAvatar(),
                new LlmConfig(req.getModelId(), req.getTemperature(), req.getTopP()),
                new VoiceConfig(req.getTtsId(), req.getSttId(), req.getVoiceName(), req.getTtsPitch(), req.getTtsSpeed()),
                new AudioConfig(req.getVadEnergyTh(), req.getVadSpeechTh(), req.getVadSilenceTh(), req.getVadSilenceMs()),
                new MemoryStrategy(req.getMemoryType()),
                req.getIsDefault() == null ? null : "1".equals(req.getIsDefault()));
        roleRepository.save(role);

        RoleBO updated = roleService.getBO(roleId);
        if (updated == null) throw new IllegalStateException("Falha ao atualizar papel");
        return roleConvert.toResp(updated);
    }

    @Transactional
    public void delete(Integer roleId) {
        roleRepository.delete(roleId);
    }
}
