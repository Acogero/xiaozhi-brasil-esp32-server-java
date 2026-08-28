package com.xiaozhi.server.web.chat;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.common.model.ChatToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * API de chat Web: fornece diálogo de texto em streaming via SSE.
 */
@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat Web", description = "Operações de chat de texto via Web")
public class WebChatController {

    @Resource
    private WebChatService webChatService;

    /**
     * Abre uma sessão de chat.
     * Quando {@code sessionId} não é informado, cria uma nova sessão; quando um sessionId existente é informado, tenta retomá-la (com verificação de posse).
     *
     * @param roleId    ID do papel
     * @param sessionId opcional, ID da sessão a ser retomada
     * @return sessionId
     */
    @PostMapping("/open")
    @SaCheckPermission("system:chat:api:open")
    @Operation(summary = "Abrir sessão de chat", description = "Cria ou retoma uma sessão de chat Web e retorna o sessionId")
    public Map<String, String> open(@RequestParam Integer roleId,
                                    @RequestParam(required = false) String sessionId) {
        Integer userId = StpUtil.getLoginIdAsInt();
        String openedSessionId = webChatService.openSession(userId, roleId, sessionId);
        return Map.of("sessionId", openedSessionId);
    }

    /**
     * Chat em streaming (SSE)
     *
     * @param sessionId ID da sessão
     * @param text      mensagem do usuário
     * @return fluxo de texto da resposta da IA
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaCheckPermission("system:chat:api:stream")
    @Operation(summary = "Chat em streaming", description = "Retorna o fluxo de tokens da resposta da IA via SSE, com os tipos thinking e content")
    public Flux<ChatToken> stream(@RequestParam String sessionId, @RequestParam String text) {
        return webChatService.chatStream(sessionId, text);
    }

    /**
     * Fecha a sessão de chat
     */
    @PostMapping("/close")
    @SaCheckPermission("system:chat:api:close")
    @Operation(summary = "Encerrar sessão de chat", description = "Encerra a sessão de chat Web e libera os recursos")
    public Map<String, String> close(@RequestParam String sessionId) {
        webChatService.closeSession(sessionId);
        return Map.of("status", "closed");
    }
}
