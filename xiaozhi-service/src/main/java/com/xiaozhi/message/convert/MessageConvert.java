package com.xiaozhi.message.convert;

import com.xiaozhi.common.model.bo.MessageBO;
import com.xiaozhi.common.model.bo.MessageMetadataBO;
import com.xiaozhi.common.model.resp.MessageResp;
import com.xiaozhi.message.dal.mysql.dataobject.MessageDO;
import com.xiaozhi.utils.JsonUtil;
import org.mapstruct.Mapper;
import org.springframework.util.StringUtils;

@Mapper(componentModel = "spring")
public interface MessageConvert {

    MessageBO toBO(MessageDO messageDO);

    MessageDO toDO(MessageBO messageBO);

    MessageResp toResp(MessageBO messageBO);

    /**
     * DO.metadata (string JSON) → BO.metadata (objeto de valor).
     * O MapStruct reconhece automaticamente a assinatura do método e o utiliza no mapeamento toBO.
     */
    default MessageMetadataBO jsonToMetadata(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        return JsonUtil.fromJson(json, MessageMetadataBO.class);
    }

    /**
     * BO.metadata (objeto de valor) → DO.metadata (string JSON).
     * O MapStruct reconhece automaticamente a assinatura do método e o utiliza no mapeamento toDO.
     */
    default String metadataToJson(MessageMetadataBO metadata) {
        if (metadata == null) {
            return null;
        }
        return JsonUtil.toJson(metadata);
    }
}
