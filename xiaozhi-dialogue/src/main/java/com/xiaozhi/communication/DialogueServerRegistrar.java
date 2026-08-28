package com.xiaozhi.communication;

import com.xiaozhi.communication.common.InstanceIdHolder;
import com.xiaozhi.communication.registry.DialogueServerInfo;
import com.xiaozhi.communication.registry.DialogueServerRegistry;
import com.xiaozhi.storage.service.StorageServiceFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
/**
 * Registrador automático do servidor Dialogue — registra na inicialização, envia heartbeat periódico e cancela o registro no encerramento
 */
@Slf4j
@Component
public class DialogueServerRegistrar {

    @Resource
    private ServerAddressProvider serverAddressProvider;

    @Resource
    private DialogueServerRegistry dialogueServerRegistry;

    @Resource
    private InstanceIdHolder instanceIdHolder;

    @Resource
    private StorageServiceFactory storageServiceFactory;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void register() {
        String instanceId = instanceIdHolder.getInstanceId();
        try {
            dialogueServerRegistry.register(buildServerInfo());
            log.info("Servidor Dialogue registrado no centro de registro, instanceId={}", instanceId);
        } catch (Exception e) {
            log.warn("Falha no registro inicial do servidor Dialogue, tentará novamente nos próximos heartbeats, instanceId={}", instanceId, e);
        }

        checkStorageConfig();

        // Envia heartbeat a cada 30 segundos
        scheduler.scheduleAtFixedRate(() -> {
            try {
                dialogueServerRegistry.heartbeat(buildServerInfo());
            } catch (Exception e) {
                log.warn("Falha no heartbeat do servidor Dialogue, instanceId={}", instanceId, e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void unregister() {
        scheduler.shutdown();
        try {
            String instanceId = instanceIdHolder.getInstanceId();
            dialogueServerRegistry.unregister(instanceId);
            log.info("Servidor Dialogue removido do centro de registro, instanceId={}", instanceId);
        } catch (Exception e) {
            log.warn("Falha ao cancelar registro", e);
        }
    }

    private void checkStorageConfig() {
        try {
            String provider = storageServiceFactory.getStorageService().getProvider();
            if ("local".equals(provider)) {
                log.warn("O StorageService atual está no modo local, os arquivos de áudio são armazenados apenas localmente. Em implantações em cluster, configure COS/OSS, caso contrário o áudio entre instâncias ficará indisponível.");
            }
        } catch (Exception e) {
            log.warn("Falha ao verificar a configuração do StorageService: {}", e.getMessage());
        }
    }

    private DialogueServerInfo buildServerInfo() {
        DialogueServerInfo info = new DialogueServerInfo();
        info.setInstanceId(instanceIdHolder.getInstanceId());
        info.setWebsocketAddress(serverAddressProvider.getWebsocketAddress());
        info.setUdpAddress(serverAddressProvider.getUdpAddress());
        info.setOtaAddress(serverAddressProvider.getOtaAddress());
        info.setMcpAddress(serverAddressProvider.getMcpAddress());
        info.setServerAddress(serverAddressProvider.getServerAddress());
        return info;
    }
}
