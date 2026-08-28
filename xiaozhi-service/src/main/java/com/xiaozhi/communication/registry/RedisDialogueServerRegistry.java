package com.xiaozhi.communication.registry;

import com.xiaozhi.utils.JsonUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
/**
 * Implementação do centro de registro de servidores Dialogue baseado em Redis
 * <p>
 * Usa Redis Hash para armazenar as informações de todas as instâncias; cada instância tem uma chave TTL própria para verificação de saúde.
 * </p>
 */
@Slf4j
@Service
public class RedisDialogueServerRegistry implements DialogueServerRegistry {

    private static final String REGISTRY_HASH_KEY = "xiaozhi:dialogue:servers";
    private static final String HEARTBEAT_KEY_PREFIX = "xiaozhi:dialogue:heartbeat:";
    private static final long HEARTBEAT_TTL_SECONDS = 60;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void register(DialogueServerInfo serverInfo) {
        serverInfo.setLastHeartbeat(System.currentTimeMillis());
        String json = JsonUtil.toJson(serverInfo);
        stringRedisTemplate.opsForHash().put(REGISTRY_HASH_KEY, serverInfo.getInstanceId(), json);
        stringRedisTemplate.opsForValue().set(
                HEARTBEAT_KEY_PREFIX + serverInfo.getInstanceId(), "1",
                HEARTBEAT_TTL_SECONDS, TimeUnit.SECONDS);
        log.info("Servidor Dialogue registrado: {}", serverInfo.getInstanceId());
    }

    @Override
    public void unregister(String instanceId) {
        stringRedisTemplate.opsForHash().delete(REGISTRY_HASH_KEY, instanceId);
        stringRedisTemplate.delete(HEARTBEAT_KEY_PREFIX + instanceId);
        log.info("Servidor Dialogue removido: {}", instanceId);
    }

    @Override
    public void heartbeat(DialogueServerInfo serverInfo) {
        serverInfo.setLastHeartbeat(System.currentTimeMillis());
        String json = JsonUtil.toJson(serverInfo);
        stringRedisTemplate.opsForHash().put(REGISTRY_HASH_KEY, serverInfo.getInstanceId(), json);
        stringRedisTemplate.opsForValue().set(
                HEARTBEAT_KEY_PREFIX + serverInfo.getInstanceId(), "1",
                HEARTBEAT_TTL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public List<DialogueServerInfo> getAvailableServers() {
        List<DialogueServerInfo> result = new ArrayList<>();
        try {
            Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(REGISTRY_HASH_KEY);
            if (entries.isEmpty()) {
                return result;
            }

            List<Map.Entry<Object, Object>> serverEntries = new ArrayList<>(entries.entrySet());
            List<String> heartbeatKeys = new ArrayList<>(serverEntries.size());
            for (Map.Entry<Object, Object> entry : serverEntries) {
                heartbeatKeys.add(HEARTBEAT_KEY_PREFIX + entry.getKey());
            }

            List<String> heartbeatValues = stringRedisTemplate.opsForValue().multiGet(heartbeatKeys);
            for (int i = 0; i < serverEntries.size(); i++) {
                Map.Entry<Object, Object> entry = serverEntries.get(i);
                String instanceId = (String) entry.getKey();
                String heartbeatValue = heartbeatValues != null && i < heartbeatValues.size() ? heartbeatValues.get(i) : null;
                if (heartbeatValue != null) {
                    DialogueServerInfo info = JsonUtil.fromJson((String) entry.getValue(), DialogueServerInfo.class);
                    if (info != null) {
                        result.add(info);
                    }
                    continue;
                }

                // Heartbeat expirado, limpando registro órfão
                stringRedisTemplate.opsForHash().delete(REGISTRY_HASH_KEY, instanceId);
                log.info("Limpando servidor Dialogue expirado: {}", instanceId);
            }
        } catch (Exception e) {
            log.error("Falha ao obter lista de servidores Dialogue disponíveis", e);
        }
        return result;
    }

    @Override
    public DialogueServerInfo selectServer() {
        List<DialogueServerInfo> servers = getAvailableServers();
        if (servers.isEmpty()) {
            return null;
        }
        // Balanceamento de carga aleatório
        int index = ThreadLocalRandom.current().nextInt(servers.size());
        return servers.get(index);
    }
}
