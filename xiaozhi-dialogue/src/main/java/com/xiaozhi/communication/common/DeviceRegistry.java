package com.xiaozhi.communication.common;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * Registro de dispositivo-instância.
 * Mantém o mapeamento device → instance via Redis, usado em cenários de implantação em cluster para:
 * <ul>
 *   <li>Vincular o dispositivo à instância atual quando ele ficar online</li>
 *   <li>Desvincular quando o dispositivo ficar offline</li>
 *   <li>Atualizar o TTL via heartbeat, evitando que o mapeamento expire</li>
 *   <li>Consultar, na inicialização, os dispositivos pertencentes a esta instância (para resetar o estado com precisão)</li>
 * </ul>
 */
@Component
public class DeviceRegistry {

    private static final String KEY_PREFIX = "xiaozhi:device:instance:";
    private static final Duration TTL = Duration.ofSeconds(300); // 5 minutos

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private InstanceIdHolder instanceIdHolder;

    /**
     * Dispositivo online: vincula à instância atual
     */
    public void bind(String deviceId) {
        stringRedisTemplate.opsForValue().set(
                KEY_PREFIX + deviceId, instanceIdHolder.getInstanceId(), TTL);
    }

    /**
     * Dispositivo offline: desvincula
     */
    public void unbind(String deviceId) {
        stringRedisTemplate.delete(KEY_PREFIX + deviceId);
    }

    /**
     * Atualiza o heartbeat (chamado periodicamente por InactiveSessionChecker)
     */
    public void refresh(String deviceId) {
        stringRedisTemplate.expire(KEY_PREFIX + deviceId, TTL);
    }

    /**
     * Consulta a instância em que o dispositivo está
     */
    public String getInstance(String deviceId) {
        return stringRedisTemplate.opsForValue().get(KEY_PREFIX + deviceId);
    }

    /**
     * Consulta todos os IDs de dispositivo pertencentes a esta instância.
     * Percorre {@code xiaozhi:device:instance:*} via SCAN, filtrando as chaves cujo valor é igual ao ID desta instância.
     */
    public Set<String> getOwnDeviceIds() {
        Set<String> ownDeviceIds = new HashSet<>();
        String ownInstanceId = instanceIdHolder.getInstanceId();
        ScanOptions options = ScanOptions.scanOptions().match(KEY_PREFIX + "*").count(100).build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String instanceId = stringRedisTemplate.opsForValue().get(key);
                if (ownInstanceId.equals(instanceId)) {
                    ownDeviceIds.add(key.substring(KEY_PREFIX.length()));
                }
            }
        }
        return ownDeviceIds;
    }

    /**
     * Verifica se o dispositivo pertence a esta instância
     */
    public boolean isOwned(String deviceId) {
        return instanceIdHolder.getInstanceId().equals(getInstance(deviceId));
    }
}
