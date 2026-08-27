package com.xiaozhi.config;

import com.xiaozhi.server.web.BaseController;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.annotation.CheckOwner;
import com.xiaozhi.common.model.req.ConfigCreateReq;
import com.xiaozhi.common.model.req.ConfigPageReq;
import com.xiaozhi.common.model.req.ConfigUpdateReq;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.config.ConfigAppService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;


/**
 * Gerenciamento de configurações
 * 
 * @author Joey
 * 
 */

@RestController
@RequestMapping("/api/config")
@Tag(name = "Gerenciamento de configurações", description = "Operações relacionadas a configurações")
public class ConfigController extends BaseController {

    @Resource
    private ConfigAppService configAppService;

    /**
     * Consulta de configurações
     *
     * @param config
     * @return configList
     */
    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:config:api:list")
    @Operation(summary = "Consulta configurações de acordo com os filtros", description = "Retorna a lista de configurações")
    public ApiResponse<?> list(@Valid ConfigPageReq req) {
        return ApiResponse.success(configAppService.page(req, StpUtil.getLoginIdAsInt()));
    }

    /**
     * Atualização das informações de configuração
     *
     * @param configId ID da configuração
     * @param param parâmetros de atualização
     * @return
     */
    @PutMapping("/{configId}")
    @ResponseBody
    @SaCheckPermission("system:config:api:update")
    @CheckOwner(resource = "config", id = "#configId")
    @AuditLog(module = "Gerenciamento de configurações", operation = "Atualizar configuração")
    @Operation(summary = "Atualizar informações de configuração", description = "Atualizar configuração de LLM/STT/TTS")
    public ApiResponse<?> update(@PathVariable Integer configId, @Valid @RequestBody ConfigUpdateReq req) {
        return ApiResponse.success(configAppService.update(configId, req));
    }

    /**
     * Adicionar configuração
     *
     * @param param parâmetros de criação
     */
    @PostMapping("")
    @ResponseBody
    @SaCheckPermission("system:config:api:create")
    @AuditLog(module = "Gerenciamento de configurações", operation = "Criar configuração")
    @Operation(summary = "Adicionar informações de configuração", description = "Adiciona uma nova configuração de LLM/STT/TTS")
    public ApiResponse<?> create(@Valid @RequestBody ConfigCreateReq req) {
        return ApiResponse.success(configAppService.create(req, StpUtil.getLoginIdAsInt()));
    }

    /**
     * Excluir informações de configuração
     *
     * @param configId ID da configuração
     * @return
     */
    @DeleteMapping("/{configId}")
    @ResponseBody
    @SaCheckPermission("system:config:api:delete")
    @CheckOwner(resource = "config", id = "#configId")
    @AuditLog(module = "Gerenciamento de configurações", operation = "Excluir configuração")
    @Operation(summary = "Excluir informações de configuração", description = "Exclui (de forma lógica) a configuração informada")
    public ApiResponse<?> delete(@PathVariable Integer configId) {
        configAppService.delete(configId);
        return ApiResponse.success("Excluído com sucesso");
    }
}
