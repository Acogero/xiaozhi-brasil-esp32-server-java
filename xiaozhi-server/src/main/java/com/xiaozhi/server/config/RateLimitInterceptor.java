package com.xiaozhi.server.config;

import com.xiaozhi.utils.RequestContextUtils;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
/**
 * Interceptor de limitação de taxa de login
 * <p>
 * Limita a frequência de requisições para endpoints de login, cadastro, código de verificação etc. com base no endereço IP,
 * prevenindo ataques de força bruta e abuso da API.
 * <p>
 * Regra de limitação: o mesmo IP pode fazer no máximo um número definido de requisições dentro de uma janela de tempo,
 * e ao exceder esse limite recebe 429 (Too Many Requests).
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    /** Prefixo da chave Redis */
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    /** Janela de tempo (segundos) */
    private static final int WINDOW_SECONDS = 60;

    /** Endpoints de login/cadastro: no máximo 10 requisições por minuto */
    private static final int MAX_AUTH_REQUESTS = 10;

    /** Endpoint de código de verificação: no máximo 5 requisições por minuto */
    private static final int MAX_CAPTCHA_REQUESTS = 5;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String clientIp = RequestContextUtils.getClientIp(request);
        String uri = request.getRequestURI();

        // Seleciona o limite de acordo com o tipo de endpoint
        int maxRequests = uri.contains("Captcha") ? MAX_CAPTCHA_REQUESTS : MAX_AUTH_REQUESTS;

        String redisKey = RATE_LIMIT_PREFIX + normalizeUri(uri) + ":" + clientIp;

        try {
            Long count = stringRedisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                // Primeira requisição, define o tempo de expiração
                stringRedisTemplate.expire(redisKey, WINDOW_SECONDS, TimeUnit.SECONDS);
            }

            if (count != null && count > maxRequests) {
                log.warn("Limite de requisições excedido - IP: {}, endpoint: {}, contagem: {}/{}", clientIp, uri, count, maxRequests);
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":429,\"message\":\"Muitas requisições em pouco tempo, tente novamente mais tarde\"}");
                return false;
            }
        } catch (Exception e) {
            // Em caso de exceção no Redis, libera a requisição para não afetar o uso normal
            log.error("Exceção na verificação de limite de requisições; a requisição foi liberada: {}", e.getMessage());
        }

        return true;
    }

    /**
     * Normaliza a URI para uso como chave Redis (remove caracteres especiais, padroniza o formato)
     */
    private String normalizeUri(String uri) {
        return uri.replaceAll("[^a-zA-Z0-9/]", "_");
    }
}
