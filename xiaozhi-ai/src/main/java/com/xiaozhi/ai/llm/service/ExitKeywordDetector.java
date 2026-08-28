package com.xiaozhi.ai.llm.service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Detector de palavras-chave de saída
 * Usado para detectar se a entrada do usuário contém palavras-chave que indicam claramente intenção de sair
 * Usado como classe utilitária pelo IntentService
 */
class ExitKeywordDetector {

    /**
     * Lista de palavras-chave de saída
     * Contém diversas expressões que indicam saída ou encerramento da conversa
     */
    private static final List<String> EXIT_KEYWORDS = Arrays.asList(
            "tchau",
            "até logo",
            "adeus",
            "vou embora",
            "eu vou embora",
            "já vou",
            "encerrar conversa",
            "sair",
            "desconectar",
            "encerrar",
            "até mais",
            "falou",
            "ir embora",
            "goodbye",
            "bye",
            "bye bye",
            "byebye",
            "see you",
            "see ya"
    );

    /**
     * Padrão de frase de correspondência exata
     * Corresponde a entradas que contêm palavras-chave de intenção de saída
     */
    private static final Pattern EXIT_PATTERN = Pattern.compile(
            ".*(?:tchau|até logo|adeus|encerrar conversa|sair|até mais|falou"
            + "|(?:eu\\s+)?(?:j[áa]\\s+)?(?:vou\\s+embora|vou\\s+sair|desconectar|ir\\s+embora)"
            + "|bye\\s*bye|goodbye|see\\s+(?:you|ya)).*",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Padrão de frase de exclusão
     * Quando contém estas frases, a saída não deve ser acionada
     * Exemplo: "não quero sair", "não vá", "não vou embora", etc.
     */
    private static final Pattern EXCLUDE_PATTERN = Pattern.compile(
            ".*(?:não|nunca|por que|como|poderia|pode|vai|o que).*(?:sair|ir embora|encerrar|desconectar).*"
            + "|.*(?:don't|not).*(?:leave|exit|quit|bye).*",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Detecta se o texto de entrada contém intenção de saída
     *
     * @param input Texto de entrada do usuário
     * @return true se a intenção de saída for detectada; caso contrário, false
     */
    public boolean detectExitIntent(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        // Remove espaços e pontuação, convertendo tudo para minúsculas
        String normalizedInput = input.trim().toLowerCase();

        // Primeiro verifica o padrão de exclusão; se corresponder, a saída não é acionada
        if (EXCLUDE_PATTERN.matcher(normalizedInput).matches()) {
            return false;
        }

        // Verifica o padrão de correspondência exata
        if (EXIT_PATTERN.matcher(normalizedInput).matches()) {
            return true;
        }

        // Verifica palavras-chave simples (aplicável a mensagens curtas isoladas)
        // A correspondência simples de palavras-chave só é usada quando a entrada é curta (menos de 15 caracteres)
        if (normalizedInput.length() <= 15) {
            for (String keyword : EXIT_KEYWORDS) {
                if (normalizedInput.contains(keyword.toLowerCase())) {
                    return true;
                }
            }
        }

        return false;
    }

}
