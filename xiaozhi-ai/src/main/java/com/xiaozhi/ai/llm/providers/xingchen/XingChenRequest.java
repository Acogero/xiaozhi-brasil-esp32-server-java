package com.xiaozhi.ai.llm.providers.xingchen;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Modelo de requisição da API do Agent XingChen
 * Baseado na documentação: https://www.xfyun.cn/doc/spark/Agent04-API%E6%8E%A5%E5%85%A5.html
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XingChenRequest {
    /**
     * ID do workflow (obrigatório)
     */
    @JsonProperty("flow_id")
    private String flowId;
    
    /**
     * ID do usuário (obrigatório)
     */
    private String uid;
    
    /**
     * Parâmetros do workflow (obrigatório)
     * key: nome da variável definida no workflow
     * value: valor correspondente
     * 
     * Exemplo: {"AGENT_USER_INPUT": "Olá", "func_call": [...]}
     */
    private Map<String, Object> parameters;
    
    /**
     * Informações estendidas (opcional)
     */
    private Ext ext;
    
    /**
     * Se o retorno é em streaming (padrão false)
     */
    private boolean stream;
    
    /**
     * ID da sessão (opcional)
     */
    @JsonProperty("chat_id")
    private String chatId;
    
    /**
     * Mensagens do histórico (opcional)
     */
    private List<History> history;

    /**
     * Informações estendidas
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ext {
        /**
         * ID do bot (opcional)
         */
        @JsonProperty("bot_id")
        private String botId;
        
        /**
         * Identificador do chamador (opcional)
         */
        private String caller;
    }

    /**
     * Registro de mensagens do histórico
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class History {
        /**
         * Papel: user/assistant
         */
        private String role;
        
        /**
         * Tipo de conteúdo: text
         */
        @JsonProperty("content_type")
        private String contentType;
        
        /**
         * Conteúdo da mensagem
         */
        private String content;
    }
}