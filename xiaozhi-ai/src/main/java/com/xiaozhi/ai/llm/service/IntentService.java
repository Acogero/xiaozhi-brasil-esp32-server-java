package com.xiaozhi.ai.llm.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;
/**
 * Detector de intenção — detecta a intenção explícita da entrada do usuário por correspondência de palavras-chave antes de chamar o LLM (<1ms).
 * <p>
 * Trata apenas intenções determinísticas (como sair); no futuro pode ser expandido para mais intenções de caminho rápido.
 */
@Slf4j
@Service
public class IntentService {

    private final ExitKeywordDetector exitKeywordDetector = new ExitKeywordDetector();

    /**
     * Enum de intenções determinísticas. Pode ser expandido no futuro: HELP, RESET, SWITCH_ROLE, etc.
     */
    public enum Intent {
        /** Sair da conversa */
        EXIT,
        /** Sem intenção especial, continua o fluxo normal */
        NONE
    }

    /**
     * Detecta a intenção explícita da entrada do usuário.
     *
     * @param userText Texto de entrada do usuário
     * @return A intenção detectada; retorna NONE se não houver intenção especial
     */
    public Intent detect(String userText) {
        if (!StringUtils.hasText(userText)) {
            return Intent.NONE;
        }

        // Intenção de saída
        if (exitKeywordDetector.detectExitIntent(userText)) {
            log.info("Intenção de saída detectada: \"{}\"", userText);
            return Intent.EXIT;
        }

        // Mais detecções de intenção podem ser adicionadas aqui no futuro

        return Intent.NONE;
    }
}
