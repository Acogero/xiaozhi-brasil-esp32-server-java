package com.xiaozhi.message;

import com.xiaozhi.server.web.BaseController;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.annotation.CheckOwner;
import com.xiaozhi.common.model.req.ConversationPageReq;
import com.xiaozhi.common.model.req.MessagePageReq;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.message.MessageAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/message")
@Tag(name = "Gerenciamento de mensagens", description = "Operações relacionadas a mensagens")
public class MessageController extends BaseController {

    @Resource
    private MessageAppService messageAppService;

    @GetMapping("")
    @ResponseBody
    @SaCheckPermission(value = {"system:role:memory:chat:api:list", "system:chat"}, mode = SaMode.OR)
    @Operation(summary = "Consulta mensagens de conversa de acordo com os filtros", description = "Retorna a lista de mensagens de conversa")
    public ApiResponse<?> list(@Valid MessagePageReq req) {
        return ApiResponse.success(messageAppService.page(req, StpUtil.getLoginIdAsInt()));
    }

    @GetMapping("/conversations")
    @ResponseBody
    @SaCheckPermission("system:chat")
    @Operation(summary = "Consulta a lista de sessões do usuário", description = "Retorna o histórico de sessões do usuário atual, agrupado por sessionId")
    public ApiResponse<?> conversations(@Valid ConversationPageReq req) {
        return ApiResponse.success(messageAppService.conversationPage(req, StpUtil.getLoginIdAsInt()));
    }

    @DeleteMapping("/{messageId}")
    @ResponseBody
    @SaCheckPermission("system:role:memory:chat:api:delete")
    @CheckOwner(resource = "message", id = "#messageId")
    @AuditLog(module = "Gerenciamento de mensagens", operation = "Excluir mensagem")
    @Operation(summary = "Excluir mensagem de conversa", description = "Exclui a mensagem de conversa informada (exclusão lógica)")
    public ApiResponse<?> delete(@PathVariable Integer messageId) {
        messageAppService.delete(messageId);
        return ApiResponse.success("Excluído com sucesso");
    }

    @DeleteMapping("")
    @ResponseBody
    @SaCheckPermission("system:device:memory:api:delete")
    @CheckOwner(resource = "device", id = "#deviceId")
    @AuditLog(module = "Gerenciamento de mensagens", operation = "Excluir mensagens de dispositivo em lote")
    @Operation(summary = "Excluir mensagens de dispositivo em lote", description = "Limpa todo o histórico de chat do dispositivo informado")
    public ApiResponse<?> batchDelete(@RequestParam String deviceId) {
        int rows = messageAppService.deleteByDeviceId(deviceId);
        log.info("Memória do dispositivo limpa, histórico de chat removido: {} linha(s).", rows);
        return ApiResponse.success("Exclusão concluída, total de " + rows + " mensagem(ns) excluída(s)");
    }
}
