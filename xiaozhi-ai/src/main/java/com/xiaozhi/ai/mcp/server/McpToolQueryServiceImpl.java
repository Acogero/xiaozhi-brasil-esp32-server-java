package com.xiaozhi.ai.mcp.server;

import com.xiaozhi.ai.tool.GlobalToolRedisRegistry;
import com.xiaozhi.ai.tool.ToolsGlobalRegistry;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
/**
 * Implementação do serviço de consulta de ferramentas MCP.
 * Delega ao ToolsGlobalRegistry a obtenção dos metadados globais das ferramentas.
 */
@Slf4j
@Service
public class McpToolQueryServiceImpl implements McpToolQueryService {

    @Resource
    private ToolsGlobalRegistry toolsGlobalRegistry;

    @Autowired(required = false)
    private GlobalToolRedisRegistry globalToolRedisRegistry;

    @Override
    public List<Map<String, String>> getSystemGlobalToolSummaries() {
        // Prioriza o GlobalFunction já registrado neste processo (processo dialogue / implantação monolítica)
        List<Map<String, String>> inMemory = toolsGlobalRegistry.getGlobalToolSummaries();
        if (!inMemory.isEmpty()) {
            return inMemory;
        }
        // Recorre ao registro compartilhado no Redis (o processo server lê entre processos os metadados publicados pelo dialogue)
        if (globalToolRedisRegistry == null) {
            return inMemory;
        }
        return globalToolRedisRegistry.getAll().stream()
                .map(t -> Map.of("name", t.getName(), "description", t.getDescription()))
                .toList();
    }
}
