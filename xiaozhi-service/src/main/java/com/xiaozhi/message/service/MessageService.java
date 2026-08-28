package com.xiaozhi.message.service;

import com.xiaozhi.common.model.bo.MessageBO;
import com.xiaozhi.common.model.resp.ConversationResp;
import com.xiaozhi.common.model.resp.MessageResp;
import com.xiaozhi.common.model.resp.PageResp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface MessageService {

    PageResp<MessageResp> page(int pageNo, int pageSize, String deviceId, String deviceName,
                               String sender, String messageType, Integer roleId,
                               Date startTime, Date endTime, Integer userId, String sessionId,
                               String source);

    PageResp<ConversationResp> conversationPage(int pageNo, int pageSize, Integer userId, Integer roleId, String source);

    void delete(Integer messageId);

    int deleteByDeviceId(String deviceId);

    MessageBO getBO(Integer messageId);

    int saveAll(List<MessageBO> messages);

    /**
     * Consulta as últimas "limit" mensagens do histórico por ownerId (deviceId) + roleId, retornadas em ordem crescente de tempo (ou seja, na ordem do contexto da conversa).
     * Aplicável ao cenário de dispositivo (agregação entre sessions).
     */
    List<MessageBO> listHistory(String deviceId, Integer roleId, int limit);

    /**
     * Consulta as últimas "limit" mensagens do histórico por sessionId, retornadas em ordem crescente de tempo (ou seja, na ordem do contexto da conversa).
     * Aplicável ao cenário Web (isolado por sessão).
     */
    List<MessageBO> listHistory(String sessionId, int limit);

    List<MessageBO> listHistoryAfter(String deviceId, Integer roleId, Instant time);

    /**
     * Atualiza o caminho de áudio da mensagem do assistant e o ttsDuration no registro de metrics associado.
     */
    void updateAssistantAudio(String deviceId, Integer roleId,
                              LocalDateTime createTime, String audioPath,
                              java.math.BigDecimal ttsDuration);
}
