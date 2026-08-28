package com.xiaozhi.role.domain.vo;

/**
 * Objeto de valor da estratégia de memória de conversa.
 * <p>
 * type corresponde ao campo memoryType do banco de dados (ex.: "memory_window", "memory_long_term").
 */
public record MemoryStrategy(String type) {

    public static MemoryStrategy defaults() {
        return new MemoryStrategy(null);
    }
}
