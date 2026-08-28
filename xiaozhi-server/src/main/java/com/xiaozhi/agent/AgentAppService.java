package com.xiaozhi.agent;

import com.xiaozhi.agent.service.AgentService;
import com.xiaozhi.common.model.req.AgentPageReq;
import com.xiaozhi.common.model.resp.AgentResp;
import com.xiaozhi.common.model.resp.PageResp;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * Serviço de aplicação do domínio Agent.
 * <p>
 * Responsabilidade: orquestra o fluxo entre o Controller e o Domain Service, incluindo:
 * <ul>
 *   <li>Conversão Req/Resp ↔ BO</li>
 *   <li>Validações entre domínios</li>
 * </ul>
 */
@Service
public class AgentAppService {

    @Resource
    private AgentService agentService;

    public PageResp<AgentResp> page(AgentPageReq req, Integer userId) {
        AgentPageReq r = req == null ? new AgentPageReq() : req;
        return agentService.page(r.getPageNo(), r.getPageSize(), r.getProvider(), r.getAgentName(), userId);
    }
}
