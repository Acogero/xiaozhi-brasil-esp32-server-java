package com.xiaozhi.message;

import com.xiaozhi.common.model.req.ConversationPageReq;
import com.xiaozhi.common.model.req.MessagePageReq;
import com.xiaozhi.common.model.resp.ConversationResp;
import com.xiaozhi.common.model.resp.MessageResp;
import com.xiaozhi.common.model.resp.PageResp;
import com.xiaozhi.message.service.MessageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * Serviço de aplicação do domínio Message.
 * <p>
 * Responsabilidade: orquestra o fluxo entre o Controller e o Domain Service, incluindo:
 * <ul>
 *   <li>Conversão Req/Resp ↔ BO</li>
 *   <li>Validações entre domínios</li>
 * </ul>
 * <p>
 * Nota: os metadados do UserMessage (timestamp/interlocutor/emoção) já são representados de forma estruturada por {@code MessageMetadataBO}
 * e armazenados na coluna JSON sys_message.metadata; a coluna {@code message} já é o texto puro do usuário, exibido diretamente pelo frontend sem necessidade de tratamento.
 * A montagem do prefixo de projeção é feita pela camada {@code Conversation} conforme necessário, antes de enviar ao LLM.
 */
@Service
public class MessageAppService {

    @Resource
    private MessageService messageService;

    public PageResp<MessageResp> page(MessagePageReq req, Integer userId) {
        MessagePageReq r = req == null ? new MessagePageReq() : req;
        return messageService.page(r.getPageNo(), r.getPageSize(), r.getDeviceId(), r.getDeviceName(),
                r.getSender(), r.getMessageType(), r.getRoleId(), r.getStartTime(), r.getEndTime(),
                userId, r.getSessionId(), r.getSource());
    }

    public PageResp<ConversationResp> conversationPage(ConversationPageReq req, Integer userId) {
        ConversationPageReq r = req == null ? new ConversationPageReq() : req;
        return messageService.conversationPage(r.getPageNo(), r.getPageSize(), userId, r.getRoleId(), r.getSource());
    }

    public void delete(Integer messageId) {
        messageService.delete(messageId);
    }

    public int deleteByDeviceId(String deviceId) {
        return messageService.deleteByDeviceId(deviceId);
    }
}
