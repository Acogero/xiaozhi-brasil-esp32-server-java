package com.xiaozhi.common.port;

import com.xiaozhi.common.model.bo.ConfigBO;

/**
 * Interface enxuta que fornece a capacidade de resolução de token de plataformas de terceiros.
 */
public interface TokenResolver {

    String getToken(ConfigBO config);
}
