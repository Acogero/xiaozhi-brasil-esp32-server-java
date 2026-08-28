package com.xiaozhi.communication.domain.iot;

import com.xiaozhi.utils.JsonUtil;
import lombok.Data;

import java.util.Map;

/**
 * Definição do método de function_call
 */
@Data
public class IotMethod {
    /**
     * Descrição do método
     */
    private String description;
    /**
     * Parâmetros do método
     */
    private Map<String, IotMethodParameter> parameters;

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }
}
