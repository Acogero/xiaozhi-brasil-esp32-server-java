package com.xiaozhi.ai.llm.factory.providers;

import com.xiaozhi.ai.llm.factory.ChatModelProvider;
import com.xiaozhi.ai.llm.providers.XingHuoChatModel;
import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.common.model.bo.RoleBO;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
/**
 * Provedor do modelo de grande porte XingHuo (iFLYTEK)
 * Como o modelo XingHuo da iFLYTEK não é compatível com o Function Calling da OpenAI, este Provider foi criado especificamente para adaptar essa funcionalidade
 * Modelos suportados: Lite(general), Pro(generalv3), Pro-128K(generalv3-128k), 
 *          Max(generalv3.5), Max-32K(generalv3.5-32k), 4.0Ultra(generalv4)
 */
@Slf4j
@Component
public class XingHuoModelProvider implements ChatModelProvider {
    
    @Override
    public String getProviderName() {
        return "xinghuo";
    }
    
    @Override
    public ChatModel createChatModel(ConfigBO config, RoleBO role) {
        String apiPassword = config.getApiKey(); // XingHuo usa APIPassword como autenticação
        String model = config.getConfigName(); // ex.: general, generalv3, generalv3.5, generalv4
        
        var chatModel = new XingHuoChatModel(apiPassword, model);
        
        log.info("Created XingHuo ChatModel: model={}", model);
        return chatModel;
    }
}

