package com.xiaozhi.ai.llm.memory;

import com.xiaozhi.common.model.bo.SummaryBO;
import org.springframework.ai.chat.messages.Message;

import java.time.Instant;
import java.util.List;

/**
 * Interface de memória de chat: um objeto global, não voltado a uma única sessão, responsável pela estratégia de armazenamento da memória global e pela adaptação a diferentes tipos de banco de dados.
 * Direção 1: diferente do MessageService, esta interface deveria ser uma camada de abstração mais alta, responsável principalmente pela estratégia de armazenamento, e não pelo CRUD de baixo nível do armazenamento.
 * Direção 2: entendida como uma interface de funcionalidade similar, no mesmo nível do MessageService, mas que precisa suportar salvamento em lote e adaptação a tipos de banco de dados.
 * O design atual optou pela Direção 2: suporta operações em lote, buscando reduzir o IO e permitir que o servidor suporte maior throughput.
 * A interface ChatMemory do Spring AI foi consultada como referência, mas por ora o ChatMemory do Spring AI foi descartado.
 * No futuro, ao usar ChatClient com Advisor, será implementado diretamente um ChatMemoryAdvisor mais amigável ao contexto local.
 * Já a Conversation foi inspirada no ChatMemory do langchain4j.
 *
 */
public interface ChatMemory {
    String TIME_MILLIS_KEY = "TIME_MILLIS";
    String AUDIO_PATH = "AUDIO_PATH";
    String USAGE_KEY = "llm_usage";  // Chave usada para armazenar o uso do LLM



    /**
     * Salva as informações básicas da sessão (ID, resumo, totalTokens, data de criação)
     * @param summary
     */
    void save(SummaryBO summary);

    /**
     * Consulta o Summary mais recente da Conversation
     * @param ownerId Identificador do participante do chat (cenário de dispositivo: deviceId; cenário Web: userId)
     * @param roleId
     * @return
     */
    SummaryBO findLastSummary(String ownerId, int roleId);

    /**
     * Obtém a lista de mensagens do histórico de conversa por ownerId + roleId (cenário de dispositivo: agregação entre sessions).
     *
     * @param ownerId Identificador do participante do chat (cenário de dispositivo: deviceId)
     * @param roleId ID do papel/role
     * @param limit Quantidade limite; este parâmetro é necessário para a performance.
     * @return Lista de mensagens, em ordem crescente por createTime
     */
    List<Message> find(String ownerId, int roleId, int limit);

    /**
     * Obtém a lista de mensagens do histórico de conversa por sessionId (cenário Web: isolado por sessão).
     * A diferença na quantidade de parâmetros em relação a {@link #find(String, int, int)} constitui uma sobrecarga de método.
     *
     * @param sessionId ID da sessão
     * @param limit Quantidade limite
     * @return Lista de mensagens, em ordem crescente por createTime
     */
    List<Message> find(String sessionId, int limit);

    /**
     * Obtém a lista de mensagens do histórico de conversa
     * @param ownerId Identificador do participante do chat (cenário de dispositivo: deviceId; cenário Web: userId)
     * @param roleId ID do papel/role
     * @param timeMillis Mensagens após este timestamp
     * @return
     */
    List<Message> find(String ownerId, int roleId, Instant timeMillis);
    /**
     * Limpa o histórico
     * Não é destinado ao uso pela Conversation, mas sim para cenários que forçam a perda de memória.
     *
     * @param ownerId Identificador do participante do chat (cenário de dispositivo: deviceId; cenário Web: userId)
     */
    void delete(String ownerId, int roleId);


}
