package com.xiaozhi.server.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Classe de configuração do Sa-Token
 *
 * @author Joey
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }

    /**
     * Registra o interceptor do Sa-Token
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Registra o interceptor do Sa-Token, interceptando todas as requisições da API
        // Endpoints que não exigem login devem ser marcados com a anotação @SaIgnore
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()) {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                        // Requisições de preflight CORS (OPTIONS) são liberadas diretamente, sem verificar login
                        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                            return true;
                        }
                        // Demais requisições seguem o processamento normal
                        return super.preHandle(request, response, handler);
                    }
                }.isAnnotation(true))  // Habilita a autenticação baseada em anotações, com suporte a @SaIgnore etc.
                .addPathPatterns("/api/**");
    }
}
