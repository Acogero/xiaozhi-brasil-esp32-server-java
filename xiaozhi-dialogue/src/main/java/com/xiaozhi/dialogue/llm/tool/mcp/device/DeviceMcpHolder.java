package com.xiaozhi.dialogue.llm.tool.mcp.device;

import com.xiaozhi.communication.domain.DeviceMcpMessage;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Atributos relacionados ao MCP do dispositivo
 */
@Data
public class DeviceMcpHolder {
    /**
     * ID da requisição MCP
     */
    private final AtomicLong mcpRequestId = new AtomicLong(10000L);
    /**
     * Inicialização do MCP concluída
     */
    private boolean mcpInitialized = false;
    /**
     * Tabela de requisições bloqueantes de comandos MCP
     */
    private Map<Long, CompletableFuture<DeviceMcpMessage>> mcpPendingRequests = new HashMap<>();
    /**
     * Cursor de obtenção de ferramentas MCP, usado para paginação; string vazia na primeira requisição
     */
    private String mcpCursor = "";

    public Long getMcpRequestId() {
        return mcpRequestId.getAndIncrement();
    }
}