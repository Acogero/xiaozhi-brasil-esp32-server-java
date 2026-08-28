package com.xiaozhi.user.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaozhi.user.service.WxLoginService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementação do serviço de login do WeChat
 */
@Service
public class WxLoginServiceImpl implements WxLoginService {
    
    @Value("${wechat.appid:}")
    private String appid;
    
    @Value("${wechat.secret:}")
    private String secret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public Map<String, String> getWxLoginInfo(String code) {
        // Endereço da API de login do miniaplicativo do WeChat
        String url = "https://api.weixin.qq.com/sns/jscode2session";
        
        // Monta a URL completa, incluindo os parâmetros de consulta
        String fullUrl = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                url, appid, secret, code);
        
        // Envia a requisição
        String response = restTemplate.getForObject(fullUrl, String.class);
        
        // Analisa a resposta
        Map<String, String> result = new HashMap<>();
        try {
            // Usa o Jackson para analisar o JSON
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            if (responseMap.containsKey("openid")) {
                result.put("openid", (String) responseMap.get("openid"));
                result.put("session_key", (String) responseMap.get("session_key"));
                
                // Se houver unionid, também salva
                if (responseMap.containsKey("unionid")) {
                    result.put("unionid", (String) responseMap.get("unionid"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao analisar a resposta de login do WeChat", e);
        }
        
        return result;
    }
}