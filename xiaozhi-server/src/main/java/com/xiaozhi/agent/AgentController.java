package com.xiaozhi.agent;

import com.xiaozhi.server.web.BaseController;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.agent.AgentAppService;
import com.xiaozhi.common.model.req.AgentPageReq;
import com.xiaozhi.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gerenciamento de agentes
 * 
 * @author Joey
 */
@RestController
@RequestMapping("/api/agent")
@Tag(name = "Gerenciamento de agentes", description = "Operações relacionadas a agentes Coze e Dify")
public class AgentController extends BaseController {

    @Resource
    private AgentAppService agentAppService;

    /**
     * Consultar lista de agentes
     *
     * @param req condições de busca
     * @return lista de agentes
     */
    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:config:agent:api:list")
    @Operation(summary = "Consulta agentes de acordo com os filtros", description = "Retorna a lista de agentes; consulta automaticamente os agentes existentes na plataforma e sincroniza a configuração local")
    public ApiResponse<?> list(@Valid AgentPageReq req) {
        return ApiResponse.success(agentAppService.page(req, StpUtil.getLoginIdAsInt()));
    }
}
