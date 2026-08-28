package com.xiaozhi.role.domain.vo;

/**
 * Objeto de valor de configuração do modelo LLM.
 */
public record LlmConfig(Integer modelId, Double temperature, Double topP) {

    public static LlmConfig defaults() {
        return new LlmConfig(null, 0.7d, 0.9d);
    }

}
