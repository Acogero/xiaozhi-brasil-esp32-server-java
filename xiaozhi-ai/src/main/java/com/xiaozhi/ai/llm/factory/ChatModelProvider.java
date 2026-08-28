package com.xiaozhi.ai.llm.factory;

import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.common.model.bo.RoleBO;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * Interface de provedor de ChatModel
 * Cada provedor de LLM implementa esta interface para criar seu próprio ChatModel
 */
public interface ChatModelProvider {

    /**
     * Obtém o nome do provedor (minúsculo)
     * @return Nome do provedor, ex.: openai, ollama, zhipu, dify, xingchen, coze, xinghuo
     */
    String getProviderName();

    /**
     * Cria uma instância de ChatModel
     * @param config Configuração do modelo
     * @param role Configuração do papel/role
     * @return Instância de ChatModel
     */
    ChatModel createChatModel(ConfigBO config, RoleBO role);

    /**
     * Cria uma instância de EmbeddingModel; provedores não suportados lançam exceção diretamente
     * @param config Configuração do modelo
     * @return Instância de EmbeddingModel
     */
    default EmbeddingModel createEmbeddingModel(ConfigBO config) {
        throw new UnsupportedOperationException(getProviderName() + " não suporta modelo de Embedding");
    }

    /**
     * Indica se este provedor é suportado
     * @param provider Nome do provedor (minúsculo)
     * @return true indica que é suportado
     */
    default boolean supports(String provider) {
        return getProviderName().equalsIgnoreCase(provider);
    }
}

