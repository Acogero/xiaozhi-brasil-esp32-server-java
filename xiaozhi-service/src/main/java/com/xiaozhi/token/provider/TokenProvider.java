package com.xiaozhi.token.provider;

import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.token.TokenCache;

import java.util.Collection;

/**
 * Estratégia de obtenção de token de terceiros.
 */
public interface TokenProvider {

    Collection<String> getSupportedProviders();

    TokenCache fetchToken(ConfigBO config);
}
