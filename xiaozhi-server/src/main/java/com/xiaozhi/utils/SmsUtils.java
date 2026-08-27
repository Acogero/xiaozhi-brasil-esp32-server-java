package com.xiaozhi.utils;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
/**
 * Classe utilitária de SMS
 *
 * @author Joey
 */
@Slf4j
@Component
public class SmsUtils {
    @Value("${sms.aliyun.access-key-id:}")
    private String accessKeyId;

    @Value("${sms.aliyun.access-key-secret:}")
    private String accessKeySecret;

    @Value("${sms.aliyun.sign-name:}")
    private String signName;

    @Value("${sms.aliyun.template-code:}")
    private String templateCode;

    /**
     * Enviar SMS com código de verificação
     * 
     * @param phoneNumber número de celular
     * @param verificationCode código de verificação
     * @return se o envio foi bem-sucedido
     */
    public boolean sendVerificationCodeSms(String phoneNumber, String verificationCode) {
        try {
            // Cria o cliente da Aliyun
            Client client = createClient();
            
            // Monta a requisição de SMS
            SendSmsRequest sendSmsRequest = new SendSmsRequest()
                .setSignName(signName)
                .setTemplateCode(templateCode)
                .setPhoneNumbers(phoneNumber)
                .setTemplateParam(String.format("{\"code\":\"%s\"}", verificationCode));
            
            // Envia o SMS
            RuntimeOptions runtime = new RuntimeOptions();
            SendSmsResponse sendSmsResponse = client.sendSmsWithOptions(sendSmsRequest, runtime);
            
            // Registra o ID da requisição
            log.info("requestID da resposta do envio de SMS: {}", sendSmsResponse.getBody().getRequestId());
            
            // Verifica o resultado do envio
            String code = sendSmsResponse.getBody().getCode();
            if ("OK".equals(code)) {
                log.info("SMS enviado com sucesso, número: {}", phoneNumber);
                return true;
            } else {
                log.error("Falha no envio do SMS, código de erro: {}, mensagem de erro: {}", code, sendSmsResponse.getBody().getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("Exceção ao enviar SMS: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Cria o cliente de SMS da Alibaba Cloud
     * 
     * @return cliente da Alibaba Cloud
     * @throws Exception se a criação falhar
     */
    private Client createClient() throws Exception {
        Config config = new Config()
            .setAccessKeyId(accessKeyId)
            .setAccessKeySecret(accessKeySecret);
        
        // Configura o Endpoint
        config.endpoint = "dysmsapi.aliyuncs.com";
        return new Client(config);
    }
}