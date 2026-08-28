package com.xiaozhi.ai.llm.providers.xingchen;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modelo de requisição de Resume do Agent XingChen
 * Usado para continuar a conversa após a chamada de ferramenta
 * Baseado na documentação: https://www.xfyun.cn/doc/spark/Agent04-API%E6%8E%A5%E5%85%A5.html
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XingChenResume {
    /**
     * ID do evento (obtido a partir do evento de chamada de ferramenta)
     */
    @JsonProperty("event_id")
    private String eventId;
    
    /**
     * Tipo de evento: function_call
     */
    @JsonProperty("event_type")
    private String eventType;
    
    /**
     * Conteúdo retornado pela chamada de ferramenta
     */
    private String content;
}
