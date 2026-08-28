package com.xiaozhi.user.service;

import java.util.Map;

/**
 * Interface do serviço de login do WeChat
 */
public interface WxLoginService {
    
    /**
     * Obtém as informações de login do WeChat
     * 
     * @param code Code de login do WeChat
     * @return Map contendo openid e session_key
     */
    Map<String, String> getWxLoginInfo(String code);
}