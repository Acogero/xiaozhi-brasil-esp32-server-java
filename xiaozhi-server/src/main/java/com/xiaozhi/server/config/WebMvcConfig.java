package com.xiaozhi.server.config;

import com.xiaozhi.common.config.RuntimePathConfig;
import com.xiaozhi.server.web.LogInterceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;

import jakarta.annotation.Resource;

import java.io.File;

@Configuration
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private LogInterceptor logInterceptor;

    @Resource
    private RateLimitInterceptor rateLimitInterceptor;

    @Resource
    private RuntimePathConfig runtimePathConfig;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/api/**")
                .order(100);

        // Limitação de taxa para endpoints de login, cadastro, código de verificação etc.
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns(
                        "/api/user/login",
                        "/api/user/tel-login",
                        "/api/user/wx-login",
                        "/api/user",                    // cadastro POST
                        "/api/user/resetPassword",
                        "/api/user/sendEmailCaptcha",
                        "/api/user/sendSmsCaptcha"
                )
                .order(10);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            String audioPath = runtimePathConfig.resolveAudioDir().toUri().toString();
            String uploadsPath = new File("uploads").getAbsoluteFile().toURI().toString();

            registry.addResourceHandler("/audio/**")
                    .addResourceLocations(audioPath);

            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations(uploadsPath);

        } catch (Exception e) {
            log.error("Falha ao adicionar recurso", e);
        }
    }

    /**
     * Configura os parâmetros de correspondência de caminho
     */
    @Override
    @SuppressWarnings("deprecation") // Suprime temporariamente o aviso de depreciação
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Usa o método recomendado para configurar a correspondência de barra final
        configurer.setUseTrailingSlashMatch(true);
    }

    /**
     * Configura o suporte a requisições assíncronas
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // Define o tempo limite da requisição assíncrona em 120 segundos, maior que o timeout de 60 segundos do SSE
        configurer.setDefaultTimeout(120000L);
    }
}
