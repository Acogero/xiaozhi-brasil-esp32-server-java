package com.xiaozhi.common.model.bo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadados adicionais do UserMessage, armazenados de forma estruturada em sys_message.metadata (coluna JSON).
 *
 * <p>Design: separado da coluna de texto message, garantindo que:
 * <ul>
 *   <li>a coluna message armazena apenas o texto puro do usuário, exibido diretamente pelo frontend sem necessidade de tratamento</li>
 *   <li>ao ler para o LLM, a camada Conversation faz a projeção "texto puro + metadata → texto com prefixo", amigável ao KV cache de prefixo</li>
 *   <li>futuras extensões de campos (como asrConfidence, speakerDirection, etc.) só precisam adicionar campos nesta classe, sem alterar o banco de dados</li>
 * </ul>
 *
 * <p>Campos vazios não são serializados, deixando o JSON mais compacto; na desserialização, campos ausentes assumem null por padrão.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageMetadataBO {

    /**
     * Chave usada para armazenar este objeto no Map {@code UserMessage.metadata} do Spring AI.
     * Os caminhos de escrita/leitura usam essa chave de forma unificada para anexar metadados ao UserMessage:
     * <pre>
     *   userMessage.getMetadata().put(METADATA_KEY, metadataBO);
     *   MessageMetadataBO m = (MessageMetadataBO) userMessage.getMetadata().get(METADATA_KEY);
     * </pre>
     */
    public static final String METADATA_KEY = "userMessageMetadata";

    /**
     * Rótulo de reconhecimento de emoção na voz (neutral/happy/sad/angry/...), com valor apenas quando o STT suporta reconhecimento de emoção.
     */
    private String emotion;

    /**
     * Confiança da emoção [0,1], com valor apenas quando há reconhecimento de emoção.
     */
    private Double emotionScore;

    /**
     * Intensidade da emoção (fraca/média/forte, etc.), com valor apenas quando há reconhecimento de emoção.
     */
    private String emotionDegree;
}
