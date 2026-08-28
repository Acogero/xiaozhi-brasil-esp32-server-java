package com.xiaozhi.config.domain.repository;

import com.xiaozhi.config.domain.AiConfig;

import java.util.Optional;

/**
 * Interface de repositório da raiz de agregação AiConfig (definida na camada de domínio, implementada na infraestrutura).
 * <p>
 * save() mantém automaticamente o invariante "único padrão por userId + configType + modelType".
 */
public interface ConfigRepository {

    /** Carrega a raiz de agregação pelo configId */
    Optional<AiConfig> findById(Integer configId);

    /**
     * Persiste a raiz de agregação (criação ou atualização).
     * <p>Se a raiz de agregação carregar o sinal DEFAULT_CHANGED, a implementação deve chamar resetDefault antes de salvar e depois limpar o cache.
     */
    void save(AiConfig config);

    /**
     * Exclusão lógica da configuração (state=disabled, isDefault=0) e limpeza do cache.
     * <p>Executa {@link AiConfig#disable()} e este método conclui a persistência.
     */
    void delete(Integer configId);
}
