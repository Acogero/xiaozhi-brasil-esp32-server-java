package com.xiaozhi.mcpserver;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.annotation.CheckOwner;
import com.xiaozhi.common.model.req.McpGlobalToolStatusReq;
import com.xiaozhi.common.model.req.McpRoleExcludeToolsReq;
import com.xiaozhi.common.model.req.McpRoleToolStatusReq;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.ai.mcp.server.McpToolQueryService;
import com.xiaozhi.mcptoolexclude.service.McpToolExcludeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mcpTool")
@Tag(name = "Gerenciamento de ferramentas MCP", description = "Operações de ativação/desativação de ferramentas MCP")
public class McpToolController {

    @Resource
    private McpToolExcludeService mcpToolExcludeService;

    @Resource
    private McpToolQueryService mcpToolQueryService;

    @PatchMapping("/role/{roleId}/tools")
    @SaCheckPermission("system:role:mcp-tools:api:update")
    @CheckOwner(resource = "role", id = "#roleId")
    @AuditLog(module = "Gerenciamento de ferramentas MCP", operation = "Alternar status de ferramenta do papel")
    @Operation(summary = "Alternar status de ferramenta do papel", description = "Ativa ou desativa uma ferramenta específica de um papel")
    public ApiResponse<?> toggleRoleToolStatus(@PathVariable Integer roleId, @Valid @RequestBody McpRoleToolStatusReq req) {
        mcpToolExcludeService.toggleRoleToolStatus(roleId, req.getToolName(), req.getServerName(), req.getEnabled());
        return ApiResponse.success("Operação realizada com sucesso");
    }

    @PostMapping("/role/{roleId}/exclude-tools")
    @SaCheckPermission("system:role:mcp-tools:api:update")
    @CheckOwner(resource = "role", id = "#roleId")
    @AuditLog(module = "Gerenciamento de ferramentas MCP", operation = "Definir ferramentas excluídas do papel em lote")
    @Operation(summary = "Definir ferramentas excluídas do papel em lote", description = "Define em lote a lista de ferramentas excluídas do papel informado")
    public ApiResponse<?> batchSetRoleExcludeTools(@PathVariable Integer roleId, @Valid @RequestBody McpRoleExcludeToolsReq req) {
        mcpToolExcludeService.batchSetRoleExcludeTools(roleId, req.getExcludeTools(), req.getServerName());
        return ApiResponse.success("Configuração em lote realizada com sucesso");
    }

    @PatchMapping("/global/tools")
    @SaCheckPermission("system:config:mcpServer:api:update")
    @AuditLog(module = "Gerenciamento de ferramentas MCP", operation = "Alternar status de ferramenta global")
    @Operation(summary = "Alternar status de ferramenta global", description = "Ativa ou desativa uma ferramenta global")
    public ApiResponse<?> toggleGlobalToolStatus(@Valid @RequestBody McpGlobalToolStatusReq req) {
        mcpToolExcludeService.toggleGlobalToolStatus(req.getToolName(), req.getServerName(), req.getEnabled());
        return ApiResponse.success("Operação realizada com sucesso");
    }

    @GetMapping("/role/{roleId}/disabled-tools")
    @SaCheckPermission("system:role:mcp-tools:api:list")
    @CheckOwner(resource = "role", id = "#roleId != null && #roleId > 0 ? #roleId : null")
    @Operation(summary = "Obter lista de ferramentas desativadas", description = "Obtém a lista de ferramentas desativadas do papel informado e as desativadas globalmente")
    public ApiResponse<?> getDisabledTools(@PathVariable Integer roleId) {
        List<String> roleDisabled = roleId != null && roleId > 0 ? mcpToolExcludeService.getRoleDisabledTools(roleId) : List.of();
        List<String> globalDisabled = mcpToolExcludeService.getGlobalDisabledTools();

        Map<String, List<String>> result = new HashMap<>();
        result.put("roleDisabled", roleDisabled);
        result.put("globalDisabled", globalDisabled);

        return ApiResponse.success(result);
    }

    @GetMapping("/system-global")
    @SaCheckPermission("system:role:mcp-tools:api:system-global")
    @Operation(summary = "Obter lista de ferramentas globais do sistema", description = "Obtém a lista de todas as ferramentas globais disponíveis no sistema")
    public ApiResponse<?> getSystemGlobalTools() {
        return ApiResponse.success(mcpToolQueryService.getSystemGlobalToolSummaries());
    }
}
