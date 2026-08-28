package com.xiaozhi.communication.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.xiaozhi.communication.common.SessionManager;
import com.xiaozhi.ai.llm.service.VisionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import lombok.extern.slf4j.Slf4j;
/**
 * Diálogo visual (interface de reconhecimento de imagem via MCP)
 * Chamado pelo cliente MCP do dispositivo; o Bearer token é o sessionId.
 */
@Slf4j
@RestController
@RequestMapping("/api/vl")
@Tag(name = "Gerenciamento de diálogo visual", description = "Operações relacionadas ao diálogo visual")
public class VLChatController {

    @Resource
    private VisionService visionService;

    @Resource
    private SessionManager sessionManager;

    @SaIgnore
    @PostMapping(value = "/chat", produces = "application/json;charset=UTF-8")
    @Operation(summary = "Reconhecimento de imagem", description = "Retorna o resultado do reconhecimento com base na pergunta")
    public Map<String, Object> vlChat(
        @Parameter(description = "Arquivo") @RequestParam("file") MultipartFile file,
        @Parameter(description = "Pergunta") @RequestParam String question,
        HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            return failure("A imagem não pode estar vazia");
        }
        if (!StringUtils.hasText(question)) {
            return failure("A pergunta não pode estar vazia");
        }

        String authorization = request.getHeader("authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return failure("Informações de autenticação ausentes ou em formato inválido");
        }

        String sessionId = authorization.substring(7);
        var session = sessionManager.getSession(sessionId);
        if (session == null) {
            return failure("A sessão não existe");
        }

        try {
            String result = visionService.recognize(file, question);
            return success(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Falha na requisição de diálogo visual, sessionId={}, error={}", sessionId, e.getMessage());
            return failure(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Falha ao processar o diálogo visual, sessionId={}", sessionId, e);
            return failure("Falha ao processar o diálogo visual, tente novamente mais tarde");
        }
    }

    private Map<String, Object> success(String text) {
        return Map.of("success", true, "text", text);
    }

    private Map<String, Object> failure(String message) {
        return Map.of("success", false, "error", message);
    }
}
