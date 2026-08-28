package com.xiaozhi.template;

import com.xiaozhi.server.web.BaseController;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.annotation.CheckOwner;
import com.xiaozhi.common.model.req.TemplateCreateReq;
import com.xiaozhi.common.model.req.TemplatePageReq;
import com.xiaozhi.common.model.req.TemplateUpdateReq;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.template.TemplateAppService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Controller de modelos de prompt
 */
@RestController
@RequestMapping("/api/template")
@Tag(name = "Gerenciamento de modelos de prompt", description = "Operações relacionadas a modelos de prompt")
public class TemplateController extends BaseController {

    @Resource
    private TemplateAppService templateAppService;

    /**
     * Consultar lista de modelos
     */
    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:prompt-template:api:list")
    @Operation(summary = "Consulta modelos de papel de acordo com os filtros", description = "Retorna a lista de modelos")
    public ApiResponse<?> list(@Valid TemplatePageReq req) {
        return ApiResponse.success(templateAppService.page(req, StpUtil.getLoginIdAsInt()));
    }

    /**
     * Adicionar modelo
     */
    @PostMapping("")
    @ResponseBody
    @SaCheckPermission("system:prompt-template:api:create")
    @AuditLog(module = "Gerenciamento de modelos", operation = "Criar modelo")
    @Operation(summary = "Adicionar modelo de papel", description = "Adiciona um novo modelo de prompt")
    public ApiResponse<?> create(@Valid @RequestBody TemplateCreateReq req) {
        return ApiResponse.success(templateAppService.create(req, StpUtil.getLoginIdAsInt()));
    }

    /**
     * Editar modelo
     */
    @PutMapping("/{templateId}")
    @ResponseBody
    @SaCheckPermission("system:prompt-template:api:update")
    @CheckOwner(resource = "template", id = "#templateId")
    @AuditLog(module = "Gerenciamento de modelos", operation = "Atualizar modelo")
    @Operation(summary = "Atualizar modelo de papel", description = "Atualiza as informações do modelo de prompt")
    public ApiResponse<?> update(@PathVariable Integer templateId, @Valid @RequestBody TemplateUpdateReq req) {
        return ApiResponse.success(templateAppService.update(templateId, req));
    }

    /**
     * Excluir modelo
     */
    @DeleteMapping("/{templateId}")
    @ResponseBody
    @SaCheckPermission("system:prompt-template:api:delete")
    @CheckOwner(resource = "template", id = "#templateId")
    @AuditLog(module = "Gerenciamento de modelos", operation = "Excluir modelo")
    @Operation(summary = "Excluir modelo de papel", description = "Exclui o modelo de prompt (exclusão lógica)")
    public ApiResponse<?> delete(@PathVariable Integer templateId) {
        templateAppService.delete(templateId);
        return ApiResponse.success("Excluído com sucesso");
    }
}
