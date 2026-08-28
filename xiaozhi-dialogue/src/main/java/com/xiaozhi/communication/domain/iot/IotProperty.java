package com.xiaozhi.communication.domain.iot;

import com.xiaozhi.utils.JsonUtil;
import lombok.Data;

/**
 * Definição de parâmetro de function_call
 */
@Data
public class IotProperty {
    /**
     * Descrição do parâmetro
     */
    private String description;
    /**
     * Tipo do parâmetro
     */
    private String type;
    /**
     * Valor do parâmetro
     */
    private Object value;

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }
}
