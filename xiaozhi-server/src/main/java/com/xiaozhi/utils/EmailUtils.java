package com.xiaozhi.utils;

import io.github.biezhi.ome.OhMyEmail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static io.github.biezhi.ome.OhMyEmail.SMTP_QQ;

import lombok.extern.slf4j.Slf4j;
/**
 * Classe utilitária de envio de e-mail
 * 
 * @author Joey
 */
@Slf4j
@Component
public class EmailUtils {
    
    @Value("${email.smtp.username}")
    private String emailUsername;

    @Value("${email.smtp.password}")
    private String emailPassword;
    
    /**
     * Enviar e-mail
     * 
     * @param to e-mail do destinatário
     * @param subject assunto do e-mail
     * @param content conteúdo do e-mail
     * @return resultado do envio
     */
    public boolean sendEmail(String to, String subject, String content) {
        return sendEmail(to, subject, content, "Plataforma de gerenciamento IoT Xiaozhi");
    }
    
    /**
     * Enviar e-mail
     * 
     * @param to e-mail do destinatário
     * @param subject assunto do e-mail
     * @param content conteúdo do e-mail
     * @param fromName nome do remetente
     * @return resultado do envio
     */
    public boolean sendEmail(String to, String subject, String content, String fromName) {
        try {
            // Valida o formato do e-mail
            if (!isValidEmail(to)) {
                log.error("Formato de e-mail inválido: {}", to);
                return false;
            }
            
            // Verifica a configuração de e-mail
            if (!StringUtils.hasText(emailUsername) || !StringUtils.hasText(emailPassword)) {
                log.error("Informações de autenticação de e-mail de terceiros não configuradas");
                return false;
            }
            
            // Configura o envio de e-mail
            OhMyEmail.config(SMTP_QQ(false), emailUsername, emailPassword);
            
            // Envia o e-mail
            OhMyEmail.subject(subject)
                    .from(fromName)
                    .to(to)
                    .html(content)
                    .send();
            
            log.info("E-mail enviado com sucesso: {} -> {}", fromName, to);
            return true;
            
        } catch (Exception e) {
            String errorMsg = getErrorMessage(e);
            log.error("Falha ao enviar e-mail: {} -> {}, erro: {}", fromName, to, errorMsg, e);
            return false;
        }
    }
    
    /**
     * Enviar e-mail de código de verificação
     * 
     * @param to e-mail do destinatário
     * @param code código de verificação
     * @return resultado do envio
     */
    public boolean sendCaptchaEmail(String to, String code) {
        String subject = "Xiaozhi ESP32 - Plataforma de gerenciamento de IoT inteligente";
        String content = "Prezado(a) usuário(a), olá! Seu código de verificação é: <h3>" + code + "</h3>Se não foi você quem solicitou, ignore este e-mail. (Válido por 10 minutos)";
        return sendEmail(to, subject, content);
    }
    
    /**
     * Valida o formato do e-mail de forma simples
     * 
     * @param email endereço de e-mail
     * @return se é válido
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        // Validação simples do formato do e-mail: contém o símbolo @ e um ponto após o @
        return email.matches("^[^@]+@[^@]+\\.[^@]+$");
    }
    
    /**
     * Obtém a mensagem de erro de acordo com o tipo de exceção
     * 
     * @param e exceção
     * @return mensagem de erro
     */
    private String getErrorMessage(Exception e) {
        if (e.getMessage() == null) {
            return "Falha no envio";
        }
        
        String message = e.getMessage();
        if (message.contains("non-existent account") ||
                message.contains("550") ||
                message.contains("recipient")) {
            return "Endereço de e-mail inexistente ou inválido";
        } else if (message.contains("Authentication failed")) {
            return "Falha na autenticação do serviço de e-mail, entre em contato com o administrador";
        } else if (message.contains("timed out")) {
            return "Tempo limite ao enviar e-mail, tente novamente mais tarde";
        }
        
        return "Falha no envio: " + message;
    }
}
