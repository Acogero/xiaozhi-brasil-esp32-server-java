package com.xiaozhi.utils;

import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
/**
 * Classe utilitária de envio de código de verificação
 * Gerencia de forma unificada o envio de código de verificação por e-mail e SMS
 * 
 * @author Joey
 */
@Slf4j
@Component
public class CaptchaUtils {
    
    @Resource
    private EmailUtils emailUtils;
    
    @Resource
    private SmsUtils smsUtils;
    
    /**
     * Enum de tipo de código de verificação
     */
    public enum CaptchaType {
        EMAIL("E-mail"),
        SMS("SMS");
        
        private final String description;
        
        CaptchaType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Resultado do envio do código de verificação
     */
    public static class CaptchaResult {
        private boolean success;
        private String message;
        
        public CaptchaResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public static CaptchaResult success() {
            return new CaptchaResult(true, "Enviado com sucesso");
        }
        
        public static CaptchaResult error(String message) {
            return new CaptchaResult(false, message);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * Enviar código de verificação por e-mail
     * 
     * @param email endereço de e-mail
     * @param code código de verificação
     * @return resultado do envio
     */
    public CaptchaResult sendEmailCaptcha(String email, String code) {
        try {
            // Valida o formato do e-mail
            if (!isValidEmail(email)) {
                log.warn("Formato de e-mail inválido: {}", email);
                return CaptchaResult.error("Formato de e-mail inválido");
            }
            
            // Valida o código de verificação
            if (!isValidCode(code)) {
                log.warn("Formato do código de verificação inválido: {}", code);
                return CaptchaResult.error("Formato do código de verificação inválido");
            }
            
            // Envia o e-mail
            boolean success = emailUtils.sendCaptchaEmail(email, code);
            
            if (success) {
                log.info("Código de verificação enviado por e-mail com sucesso: {}", email);
                return CaptchaResult.success();
            } else {
                log.error("Falha ao enviar código de verificação por e-mail: {}", email);
                return CaptchaResult.error("Falha ao enviar e-mail, verifique a configuração de e-mail");
            }
            
        } catch (Exception e) {
            log.error("Exceção ao enviar código de verificação por e-mail: {}", e.getMessage(), e);
            return CaptchaResult.error("Falha no envio, tente novamente mais tarde");
        }
    }
    
    /**
     * Enviar código de verificação por SMS
     * 
     * @param phoneNumber número de celular
     * @param code código de verificação
     * @return resultado do envio
     */
    public CaptchaResult sendSmsCaptcha(String phoneNumber, String code) {
        try {
            // Valida o formato do número de celular
            if (!isValidPhoneNumber(phoneNumber)) {
                log.warn("Formato de número de celular inválido: {}", phoneNumber);
                return CaptchaResult.error("Formato de número de celular inválido");
            }
            
            // Valida o código de verificação
            if (!isValidCode(code)) {
                log.warn("Formato do código de verificação inválido: {}", code);
                return CaptchaResult.error("Formato do código de verificação inválido");
            }
            
            // Envia o SMS
            boolean success = smsUtils.sendVerificationCodeSms(phoneNumber, code);
            
            if (success) {
                log.info("Código de verificação enviado por SMS com sucesso: {}", phoneNumber);
                return CaptchaResult.success();
            } else {
                log.error("Falha ao enviar código de verificação por SMS: {}", phoneNumber);
                return CaptchaResult.error("Falha no envio do SMS, tente novamente mais tarde");
            }
            
        } catch (Exception e) {
            log.error("Exceção ao enviar código de verificação por SMS: {}", e.getMessage(), e);
            return CaptchaResult.error("Falha no envio do SMS, entre em contato com o administrador");
        }
    }
    
    /**
     * Valida o formato do e-mail
     * 
     * @param email endereço de e-mail
     * @return se é válido
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        // Validação do formato do e-mail: contém o símbolo @ e um ponto após o @
        return email.matches("^[^@]+@[^@]+\\.[^@]+$");
    }
    
    /**
     * Valida o formato do número de celular
     * 
     * @param phoneNumber número de celular
     * @return se é válido
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }
        // Validação de formato de celular chinês: 11 dígitos, começando com 1
        return phoneNumber.matches("^1\\d{10}$");
    }
    
    /**
     * Valida o formato do código de verificação
     * 
     * @param code código de verificação
     * @return se é válido
     */
    private boolean isValidCode(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        // O código de verificação normalmente tem de 4 a 6 dígitos ou letras
        return code.matches("^[0-9A-Za-z]{4,6}$");
    }
}

