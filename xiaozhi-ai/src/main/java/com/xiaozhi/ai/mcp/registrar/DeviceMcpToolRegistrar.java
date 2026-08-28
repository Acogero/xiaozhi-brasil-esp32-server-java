package com.xiaozhi.ai.mcp.registrar;

import com.xiaozhi.ai.tool.ToolRegistrar;
import com.xiaozhi.ai.tool.session.ToolSession;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

import lombok.extern.slf4j.Slf4j;
/**
 * Registrador de ferramentas MCP do lado do dispositivo.
 * As ferramentas MCP do lado do dispositivo já são registradas no toolSession através de DeviceMcpService.initialize() durante a inicialização do dispositivo,
 * aqui serve apenas como placeholder, registrando em log o estado já registrado.
 */
@Slf4j
@Component
@Order(1)
public class DeviceMcpToolRegistrar implements ToolRegistrar {

    @Override
    public void register(ToolSession toolSession, Set<String> excludedTools) {
        if (toolSession.isDeviceMcpInitialized()) {
            log.debug("SessionId: {}, ferramentas MCP do dispositivo já inicializadas", toolSession.getSessionId());
        }
    }
}
