package com.xiaozhi.ai.llm.factory.providers;

import com.xiaozhi.ai.llm.factory.ChatModelProvider;
import com.xiaozhi.ai.llm.providers.CozeChatModel;
import com.xiaozhi.common.port.TokenResolver;
import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.common.model.bo.RoleBO;
import com.xiaozhi.common.port.ConfigLookup;

import java.util.List;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
/**
 * Provedor de modelo Coze
 */
@Slf4j
@Component
public class CozeModelProvider implements ChatModelProvider {
    
    @Autowired
    private ConfigLookup configLookup;
    
    @Autowired
    private TokenResolver tokenResolver;
    
    @Override
    public String getProviderName() {
        return "coze";
    }
    
    @Override
    public ChatModel createChatModel(ConfigBO config, RoleBO role) {
        String model = config.getConfigName();
        
        // Coze precisa consultar a configuração do agent para obter o Token
        List<ConfigBO> configs = configLookup.listConfigs(
                config.getUserId(),
                "agent",
                "coze",
                null,
                null,
                ConfigBO.STATE_ENABLED);
        if (configs == null || configs.isEmpty()) {
            throw new IllegalStateException("Configuração de agent Coze não encontrada, userId=" + config.getUserId());
        }
        ConfigBO queryConfig = configs.get(0);
        String token = tokenResolver.getToken(queryConfig);
        
        var chatModel = new CozeChatModel(token, model);
        
        log.info("Created Coze ChatModel: model={}", model);
        return chatModel;
    }
}
