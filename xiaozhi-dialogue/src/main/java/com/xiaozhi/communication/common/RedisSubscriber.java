package com.xiaozhi.communication.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.dialogue.runtime.Persona;
import com.xiaozhi.ai.stt.SttServiceFactory;
import com.xiaozhi.token.TokenService;
import com.xiaozhi.ai.tts.TtsServiceFactory;
import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.config.service.ConfigService;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.utils.JsonUtil;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
/**
 * Configuração de assinatura de mensagens Redis.
 * Escuta broadcasts entre instâncias e executa a ação correspondente nesta instância.
 */
@Slf4j
@Configuration
public class RedisSubscriber {

    @Resource
    private SessionManager sessionManager;

    @Resource
    private DeviceRegistry deviceRegistry;

    @Resource
    private SttServiceFactory sttServiceFactory;

    @Resource
    private TtsServiceFactory ttsServiceFactory;

    @Resource
    private TokenService tokenService;

    @Resource
    private ConfigService configService;

    @Resource
    private DeviceService deviceService;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        addListener(container, "onClearConversation", RedisBroadcast.CHANNEL_CLEAR_CONVERSATION);
        addListener(container, "onRoleChanged", RedisBroadcast.CHANNEL_ROLE_CHANGED);
        addListener(container, "onConfigChanged", RedisBroadcast.CHANNEL_CONFIG_CHANGED);
        addListener(container, "onCloseSession", RedisBroadcast.CHANNEL_CLOSE_SESSION);
        addListener(container, "onRoleUpdated", RedisBroadcast.CHANNEL_ROLE_UPDATED);
        addListener(container, "onDeviceUpdated", RedisBroadcast.CHANNEL_DEVICE_UPDATED);

        return container;
    }

    private void addListener(RedisMessageListenerContainer container, String method, String channel) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(this, method);
        adapter.afterPropertiesSet();
        container.addMessageListener(adapter, new ChannelTopic(channel));
    }

    /**
     * Limpa o histórico do diálogo
     */
    public void onClearConversation(String deviceId) {
        sessionManager.findConversation(deviceId).ifPresent(conversation -> {
            conversation.clear();
            log.info("Histórico de diálogo do dispositivo limpo (via broadcast entre instâncias) - deviceId: {}", deviceId);
        });
    }

    /**
     * Mudança de papel do dispositivo: limpa o Persona, que será reconstruído na próxima ativação
     */
    public void onRoleChanged(String deviceId) {
        ChatSession session = sessionManager.getSessionByDeviceId(deviceId);
        if (session != null) {
            // Primeiro atualiza o device a partir do banco (com o novo roleId); caso contrário, a reconstrução do Persona ainda usaria o papel antigo
            DeviceBO freshDevice = deviceService.getBO(deviceId);
            if (freshDevice != null) {
                freshDevice.setSessionId(session.getSessionId());
                session.setDevice(freshDevice);
            }
            Persona persona = session.getPersona();
            if (persona != null) {
                persona.getConversation().clear();
                session.setPersona(null);
            }
            log.info("Persona do dispositivo limpo (via broadcast entre instâncias) - deviceId: {}", deviceId);
        }
    }

    /**
     * Mudança de atributo do papel (como o timbre de voz): percorre as sessões desta instância e limpa os Personas que usam esse papel
     */
    public void onRoleUpdated(String message) {
        try {
            Integer roleId = Integer.parseInt(message.trim());
            int count = 0;
            for (ChatSession session : sessionManager.getAllSessions()) {
                DeviceBO device = session.getDevice();
                if (device != null && roleId.equals(device.getRoleId())) {
                    Persona persona = session.getPersona();
                    if (persona != null) {
                        persona.getConversation().clear();
                        session.setPersona(null);
                        count++;
                    }
                }
            }
            if (count > 0) {
                log.info("Atributo do papel alterado; {} Persona(s) limpo(s) (roleId: {})", count, roleId);
            }
        } catch (Exception e) {
            log.error("Falha ao processar o broadcast roleUpdated", e);
        }
    }

    /**
     * Fecha a sessão do dispositivo: só processa se o dispositivo estiver nesta instância
     */
    public void onCloseSession(String deviceId) {
        ChatSession session = sessionManager.getSessionByDeviceId(deviceId);
        if (session != null) {
            sessionManager.closeSession(session);
            log.info("Sessão do dispositivo fechada (via broadcast entre instâncias) - deviceId: {}", deviceId);
        }
    }

    /**
     * Mudança nas informações do dispositivo: atualiza os dados de sessão desse dispositivo nesta instância
     */
    public void onDeviceUpdated(String deviceId) {
        ChatSession session = sessionManager.getSessionByDeviceId(deviceId);
        if (session != null) {
            DeviceBO freshDevice = deviceService.getBO(deviceId);
            if (freshDevice != null) {
                freshDevice.setSessionId(session.getSessionId());
                session.setDevice(freshDevice);
                log.info("Informações do dispositivo atualizadas (via broadcast entre instâncias) - deviceId: {}", deviceId);
            }
        }
    }

    /**
     * Mudança de configuração: limpa o cache da respectiva factory (STT/TTS/Token)
     */
    public void onConfigChanged(String message) {
        try {
            Map<String, Object> payload = JsonUtil.fromJson(message, new TypeReference<>() {});
            String configType = (String) payload.get("configType");
            Integer configId = (Integer) payload.get("configId");

            ConfigBO config = configService.getBO(configId);
            if (config != null) {
                if ("stt".equals(configType)) {
                    sttServiceFactory.removeCache(config);
                } else if ("tts".equals(configType)) {
                    ttsServiceFactory.removeCache(config);
                }
                // O cache de Token (Coze OAuth, Token do Alibaba Cloud etc.) independe do configType e é sempre limpo
                tokenService.removeCache(config);
                log.info("Cache da factory limpo - configType: {}, configId: {}", configType, configId);
            }
        } catch (Exception e) {
            log.error("Falha ao processar o broadcast configChanged", e);
        }
    }
}
