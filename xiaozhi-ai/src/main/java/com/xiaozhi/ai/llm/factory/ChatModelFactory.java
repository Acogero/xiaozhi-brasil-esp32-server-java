package com.xiaozhi.ai.llm.factory;

import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.common.model.bo.RoleBO;
import com.xiaozhi.common.port.ConfigLookup;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
/**
 * ChatModel
 * 
 * Padrão de projeto: Strategy + Factory
 * - Define uma estratégia de criação unificada através da interface ChatModelProvider
 * - Cada provedor de LLM implementa um Provider independente
 * - A classe factory injeta automaticamente todos os Providers via Spring, roteando para a implementação correspondente
 */
@Slf4j
@Component
public class ChatModelFactory {
    
    @Autowired
    private ConfigLookup configLookup;
    
    /**
     * Todos os provedores de ChatModel; o Spring injeta automaticamente todos os Beans que implementam a interface ChatModelProvider
     */
    private final Map<String, ChatModelProvider> providers;

    @Autowired
    private ObservationRegistry registry;
    /**
     * Construtor, injeta automaticamente todos os ChatModelProvider
     * @param providers Todas as implementações de Provider
     */
    @Autowired
    public ChatModelFactory(List<ChatModelProvider> providers) {
        // Converte a lista de Providers em um Map, chave = nome do provider (minúsculo), valor = instância do Provider
        this.providers = providers.stream()
                .collect(Collectors.toMap(
                        p -> p.getProviderName().toLowerCase(),
                        Function.identity()
                ));
    }
    
    public ChatModel getChatModel(RoleBO role) {
        RoleBO effectiveRole = role != null ? role : new RoleBO();
        Integer modelId = effectiveRole.getModelId();
        Assert.notNull(modelId, "ID de configuração não pode ser vazio");
        // Consulta a configuração pelo ID de configuração
        ConfigBO config = configLookup.getConfig(modelId);
        return createChatModel(config, effectiveRole);
    }

    public ChatModel getVisionModel() {
        ConfigBO config = configLookup.getDefaultConfig("llm", ConfigBO.ModelType.vision.getValue());
        Assert.notNull(config, "Modelo multimodal não configurado");
        return createChatModel(config, new RoleBO());
    }

    public ChatModel getIntentModel() {
        ConfigBO config = configLookup.getDefaultConfig("llm", ConfigBO.ModelType.intent.getValue());
        Assert.notNull(config, "Modelo de reconhecimento de intenção não configurado");
        return createChatModel(config, new RoleBO());
    }

    public EmbeddingModel getEmbeddingModel(Integer configId) {
        Assert.notNull(configId, "ID de configuração não pode ser vazio");
        ConfigBO config = configLookup.getConfig(configId);
        Assert.notNull(config, "Configuração não encontrada, configId=" + configId);
        return getEmbeddingModel(config);
    }

    public EmbeddingModel getEmbeddingModel(ConfigBO config) {
        Assert.notNull(config, "Modelo de vetor (embedding) não configurado");
        String providerName = config.getProvider().toLowerCase();
        ChatModelProvider provider = providers.get(providerName);
        if (provider != null) {
            return provider.createEmbeddingModel(config);
        }
        provider = providers.get("openai");
        if (provider != null) {
            return provider.createEmbeddingModel(config);
        }
        throw new IllegalArgumentException(
                String.format("Provider não suportado: %s, Providers disponíveis: %s", providerName, providers.keySet()));
    }

    /**
     * Cria o ChatModel
     *
     * @param config Configuração do modelo
     * @param role Configuração do papel/role
     * @return Instância de ChatModel
     */
    private ChatModel createChatModel(ConfigBO config, RoleBO role) {
        String providerName = config.getProvider().toLowerCase();
        
        // Obtém o Provider correspondente a partir do Map de providers
        ChatModelProvider provider = providers.get(providerName);
        
        if (provider != null) {
            return provider.createChatModel(config, role);
        }
        
        // Se não encontrar o Provider correspondente, tenta usar o Provider OpenAI como padrão (compatível com o protocolo OpenAI)
        provider = providers.get("openai");
        
        if (provider != null) {
            return provider.createChatModel(config, role);
        }
        
        // Se nem o Provider OpenAI existir, lança exceção
        throw new IllegalArgumentException(
                String.format("Provider não suportado: %s, Providers disponíveis: %s", 
                        providerName, 
                        providers.keySet())
        );
    }
}
