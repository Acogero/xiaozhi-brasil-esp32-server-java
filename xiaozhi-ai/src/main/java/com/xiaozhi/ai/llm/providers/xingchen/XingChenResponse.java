package com.xiaozhi.ai.llm.providers.xingchen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Modelo de resposta da API do Agent XingChen
 * Baseado na documentação: https://www.xfyun.cn/doc/spark/Agent04-API%E6%8E%A5%E5%85%A5.html
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class XingChenResponse {
    /**
     * Código de erro (0 indica sucesso)
     */
    private Integer code;

    /**
     * Mensagem de erro
     */
    private String message;

    /**
     * ID da mensagem
     */
    private String id;

    /**
     * Timestamp de criação
     */
    private Long created;

    /**
     * Lista de opções de resposta
     */
    private List<Choices> choices;

    /**
     * Dados do evento (relacionados à chamada de ferramenta)
     */
    @JsonProperty("event_data")
    private EventData eventData;

    /**
     * Opção de resposta
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choices {
        /**
         * Conteúdo incremental
         */
        private Delta delta;
        
        /**
         * Índice da opção
         */
        private Integer index;

        /**
         * Motivo de conclusão: stop/length/null
         */
        @JsonProperty("finish_reason")
        private String finishReason;

        /**
         * Conteúdo incremental da mensagem
         */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Delta {
            /**
             * Papel: assistant
             */
            private String role;
            
            /**
             * Conteúdo da mensagem
             */
            private String content;
        }
    }

    /**
     * Dados do evento (cenários de chamada de ferramenta/interrupção etc.)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventData {
        /**
         * ID do evento
         */
        @JsonProperty("event_id")
        private String eventId;
        
        /**
         * Tipo de evento: function_call/interrupt, etc.
         */
        @JsonProperty("event_type")
        private String eventType;
        
        /**
         * Se requer resposta: true/false
         */
        @JsonProperty("need_reply")
        private String needReply;
        
        /**
         * Valor do evento
         */
        private ReplayValue value;

        /**
         * Valor da resposta
         */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ReplayValue {
            /**
             * ID do evento
             */
            @JsonProperty("event_id")
            private String eventId;
            
            /**
             * Tipo de evento
             */
            @JsonProperty("event_type")
            private String eventType;
            
            /**
             * Se requer resposta
             */
            @JsonProperty("need_reply")
            private String needReply;
            
            /**
             * Valor
             */
            private String value;
            
            /**
             * Tipo de interrupção
             */
            private String type;
            
            /**
             * Conteúdo da interrupção
             */
            private String content;
        }
    }
}
