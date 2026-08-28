package com.xiaozhi.memory;

import com.xiaozhi.server.web.BaseController;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.annotation.CheckOwner;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.summary.service.SummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/memory")
@Tag(name = "Gerenciamento de memória", description = "Gerencia a memória resumida e a memória de longo prazo relacionadas ao chat")
public class MemoryController extends BaseController {

    @Resource
    private SummaryService summaryService;

    @GetMapping("/summary/{roleId}/{deviceId}")
    @SaCheckPermission("system:role:memory:summary:api:list")
    @CheckOwner(resource = "role", id = "#roleId")
    @CheckOwner(resource = "device", id = "#deviceId")
    @Operation(summary = "Consulta a memória resumida do papel informado", description = "Retorna a lista de memórias resumidas, podendo ser filtrada por ID do dispositivo")
    public ApiResponse<?> querySummary(@PathVariable Integer roleId,
                                      @PathVariable String deviceId,
                                      @RequestParam(defaultValue = "1") Integer pageNo,
                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.success(summaryService.page(deviceId, roleId, pageNo, pageSize));
    }

    @DeleteMapping("/summary/{roleId}/{deviceId}")
    @SaCheckPermission("system:role:memory:summary:api:delete")
    @CheckOwner(resource = "role", id = "#roleId")
    @CheckOwner(resource = "device", id = "#deviceId")
    @AuditLog(module = "Gerenciamento de memória", operation = "Excluir memória resumida")
    @Operation(summary = "Exclui em lote a memória resumida do papel informado", description = "Exclui em lote a memória resumida com base no ID do papel e no ID do dispositivo")
    public ApiResponse<?> deleteSummary(@PathVariable Integer roleId,
                                       @PathVariable String deviceId,
                                       @RequestParam(required = false) Long id) {
        return ApiResponse.success(summaryService.delete(roleId, deviceId, id));
    }
}
