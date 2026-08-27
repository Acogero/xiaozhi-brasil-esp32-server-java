package com.xiaozhi.server.config;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro de cabeçalhos de segurança: adiciona cabeçalhos de segurança padrão a todas as respostas HTTP.
 * <p>
 * Executado no início da cadeia de filtros, garantindo que, independentemente do resultado do processamento downstream,
 * os cabeçalhos de segurança sejam sempre adicionados à resposta.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SecurityHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Evita que o navegador faça sniffing do tipo MIME
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Impede que a página seja incorporada em um iframe, prevenindo clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // Controla a política de envio do cabeçalho Referer
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Desativa o filtro XSS legado, confiando na proteção via CSP
        response.setHeader("X-XSS-Protection", "0");

        // Restringe o acesso a funcionalidades do navegador
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        filterChain.doFilter(request, response);
    }
}
