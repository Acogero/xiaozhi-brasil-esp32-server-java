package com.xiaozhi.ai.llm.factory.providers;

import com.xiaozhi.ai.llm.factory.ChatModelProvider;
import com.xiaozhi.ai.llm.providers.XingChenChatModel;
import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.common.model.bo.RoleBO;
import com.xiaozhi.common.port.ConfigLookup;

import java.util.List;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
/**
 * Provedor de modelo XingChen (iFLYTEK)
 */
@Slf4j
@Component
public class XingChenModelProvider implements ChatModelProvider {
    
    @Autowired
    private ConfigLookup configLookup;
    
    @Override
    public String getProviderName() {
        return "xingchen";
    }
    
    @Override
    public ChatModel createChatModel(ConfigBO config, RoleBO role) {
        String endpoint = config.getApiUrl();
        
        // XingChen precisa consultar a configuração do agent para obter ApiKey e Secret
        List<ConfigBO> configs = configLookup.listConfigs(
                config.getUserId(),
                "agent",
                "xingchen",
                null,
                null,
                ConfigBO.STATE_ENABLED);
        if (configs == null || configs.isEmpty()) {
            throw new IllegalStateException("Configuração de agent XingChen não encontrada, userId=" + config.getUserId());
        }
        ConfigBO queryConfig = configs.get(0);
        String apiKey = queryConfig.getApiKey();
        String apiSecret = queryConfig.getApiSecret();
        
        var chatModel = new XingChenChatModel(endpoint, apiKey, apiSecret);
        
        log.info("Created XingChen ChatModel: endpoint={}", endpoint);
        return chatModel;
    }
}
