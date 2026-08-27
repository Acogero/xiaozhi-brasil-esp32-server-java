package com.xiaozhi.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

/**
 * Configuração da documentação da API Knife4j (Swagger)
 *
 * Endereços de acesso:
 * - Knife4j UI: http://localhost:porta/doc.html
 * - Swagger UI: http://localhost:porta/swagger-ui/index.html
 * - OpenAPI JSON: http://localhost:porta/v3/api-docs
 *
 * @author Joey
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API da plataforma IoT Xiaozhi")
                        .description("Documentação da API RESTful da plataforma de gerenciamento de IoT inteligente Xiaozhi ESP32\n\n" +
                                "### Módulos de funcionalidades\n" +
                                "- **Gerenciamento de usuários**: cadastro, login, controle de permissões\n" +
                                "- **Gerenciamento de dispositivos**: cadastro, configuração e monitoramento de status dos dispositivos ESP32\n" +
                                "- **Interação por voz**: STT, TTS, clonagem de voz\n" +
                                "- **Diálogo com IA**: suporte a diversos modelos de IA (OpenAI, Zhipu, iFlytek, etc.)\n" +
                                "- **Gerenciamento de firmware**: atualização OTA, controle de versões\n" +
                                "- **Assinatura e permissões**: sistema de assinatura, controle de permissões\n\n" +
                                "### Sobre a autenticação\n" +
                                "Esta API usa autenticação via Bearer Token; a maioria dos endpoints exige o token no cabeçalho da requisição:\n" +
                                "```\n" +
                                "Authorization: Bearer your-token-here\n" +
                                "```")
                        .version("5.0.0")
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT"))
                        .contact(new Contact()
                                .name("Joey")
                                .email("1277676045@qq.com")
                                .url("https://github.com/joey-zhou/xiaozhi-esp32-server-java")))
                .externalDocs(new ExternalDocumentation()
                        .description("Documentação do projeto Xiaozhi ESP32")
                        .url("https://github.com/joey-zhou/xiaozhi-esp32-server-java"))
                .servers(List.of(
                        new Server().url("http://localhost:8091").description("Ambiente de desenvolvimento local - Backend"),
                        new Server().url("http://localhost:8084").description("Ambiente de desenvolvimento local - Frontend")))
                .components(new Components()
                        .addSecuritySchemes("Bearer Token", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("Token")
                                .description("Informe o token obtido após o login (não é necessário adicionar o prefixo Bearer, o sistema faz isso automaticamente)")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"));
    }
}
