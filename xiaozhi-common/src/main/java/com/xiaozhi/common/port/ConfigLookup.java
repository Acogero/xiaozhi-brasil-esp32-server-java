package com.xiaozhi.common.port;

import com.xiaozhi.common.model.bo.ConfigBO;

import java.util.List;

/**
 * Porta mínima de consulta de configuração usada em cenários de AI/runtime, evitando dependência direta do serviço completo de configuração.
 */
public interface ConfigLookup {

    ConfigBO getConfig(Integer configId);

    ConfigBO getDefaultConfig(String configType);

    ConfigBO getDefaultConfig(String configType, String modelType);

    List<ConfigBO> listConfigs(Integer userId, String configType, String provider, String modelType, String isDefault, String state);
}
