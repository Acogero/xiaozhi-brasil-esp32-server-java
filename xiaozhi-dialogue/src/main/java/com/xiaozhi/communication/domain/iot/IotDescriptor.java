package com.xiaozhi.communication.domain.iot;

import com.xiaozhi.utils.JsonUtil;
import lombok.Data;

import java.util.Map;

/**
 * Informações de descrição do dispositivo IoT
 */
@Data
public class IotDescriptor {
    private String name;
    private String description;
    private Map<String, IotProperty> properties;
    private Map<String, IotMethod> methods;

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }
}
