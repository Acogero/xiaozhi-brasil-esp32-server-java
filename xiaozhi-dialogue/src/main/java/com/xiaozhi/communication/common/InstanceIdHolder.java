package com.xiaozhi.communication.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
/**
 * Detentor do identificador da instância.
 * Usa preferencialmente o {@code xiaozhi.instance.id} configurado; se não configurado, gera automaticamente (hostname + sufixo aleatório).
 * A cada reinício do Pod/processo, um novo registro é feito.
 */
@Slf4j
@Component
public class InstanceIdHolder {

    private final String instanceId;

    public InstanceIdHolder(@Value("${xiaozhi.instance.id:}") String configuredInstanceId) {
        if (configuredInstanceId != null && !configuredInstanceId.isEmpty()) {
            this.instanceId = configuredInstanceId;
        } else {
            String host;
            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                host = "unknown";
            }
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            this.instanceId = host + "-" + suffix;
        }
        log.info("Identificador da instância gerado: {}", instanceId);
    }

    public String getInstanceId() {
        return instanceId;
    }
}
