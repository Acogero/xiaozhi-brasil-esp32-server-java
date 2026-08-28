package com.xiaozhi.communication.common;

import com.xiaozhi.event.AiConfigChangedEvent;
import com.xiaozhi.event.ConversationHistoryClearedEvent;
import com.xiaozhi.event.DeviceRoleChangedEvent;
import com.xiaozhi.event.DeviceSessionClosedEvent;
import com.xiaozhi.event.DeviceUpdatedEvent;
import com.xiaozhi.event.RoleUpdatedEvent;
import com.xiaozhi.utils.JsonUtil;
import jakarta.annotation.Resource;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
/**
 * Broadcast de mensagens entre instâncias.
 * Notifica todas as instâncias via Redis Pub/Sub para executar a operação correspondente, suportando:
 * <ul>
 *   <li>clearConversation: limpa o histórico de conversa do dispositivo informado</li>
 *   <li>roleChanged: papel do dispositivo alterado, recarrega a Persona</li>
 *   <li>configChanged: configuração alterada, limpa o cache da factory correspondente</li>
 * </ul>
 */
@Slf4j
@Component
public class RedisBroadcast {

    public static final String CHANNEL_CLEAR_CONVERSATION = "xiaozhi:clear-conversation";
    public static final String CHANNEL_ROLE_CHANGED = "xiaozhi:role-changed";
    public static final String CHANNEL_CONFIG_CHANGED = "xiaozhi:config-changed";
    public static final String CHANNEL_CLOSE_SESSION = "xiaozhi:close-session";
    public static final String CHANNEL_ROLE_UPDATED = "xiaozhi:role-updated";
    public static final String CHANNEL_DEVICE_UPDATED = "xiaozhi:device-updated";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Orientado a eventos: transmite via Redis ao receber o evento de limpeza de conversa
     */
    @EventListener
    public void onConversationClear(ConversationHistoryClearedEvent event) {
        clearConversation(event.getDeviceId());
    }

    @EventListener
    public void onDeviceRoleChanged(DeviceRoleChangedEvent event) {
        roleChanged(event.getDeviceId());
    }

    @EventListener
    public void onDeviceSessionClosed(DeviceSessionClosedEvent event) {
        closeDeviceSession(event.getDeviceId());
    }

    @EventListener
    public void onAiConfigChanged(AiConfigChangedEvent event) {
        configChanged(event.getConfigType(), event.getConfigId());
    }

    @EventListener
    public void onRoleUpdated(RoleUpdatedEvent event) {
        roleUpdated(event.getRoleId());
    }

    @EventListener
    public void onDeviceUpdated(DeviceUpdatedEvent event) {
        if (event.getDevice() != null && event.getDevice().getDeviceId() != null) {
            deviceUpdated(event.getDevice().getDeviceId());
        }
    }

    public void clearConversation(String deviceId) {
        publish(CHANNEL_CLEAR_CONVERSATION, deviceId);
    }

    public void roleChanged(String deviceId) {
        publish(CHANNEL_ROLE_CHANGED, deviceId);
    }

    public void closeDeviceSession(String deviceId) {
        publish(CHANNEL_CLOSE_SESSION, deviceId);
    }

    public void roleUpdated(Integer roleId) {
        publish(CHANNEL_ROLE_UPDATED, String.valueOf(roleId));
    }

    public void deviceUpdated(String deviceId) {
        publish(CHANNEL_DEVICE_UPDATED, deviceId);
    }

    public void configChanged(String configType, Integer configId) {
        String payload = JsonUtil.toJson(Map.of("configType", configType, "configId", configId));
        publish(CHANNEL_CONFIG_CHANGED, payload);
    }

    private void publish(String channel, String message) {
        try {
            stringRedisTemplate.convertAndSend(channel, message);
            log.debug("Mensagem transmitida - channel: {}, message: {}", channel, message);
        } catch (Exception e) {
            log.error("Falha ao transmitir mensagem - channel: {}, message: {}", channel, message, e);
        }
    }
}
