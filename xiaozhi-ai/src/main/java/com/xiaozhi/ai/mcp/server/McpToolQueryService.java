package com.xiaozhi.ai.mcp.server;


import java.util.List;
import java.util.Map;

/**
 * Serviço de consulta de ferramentas MCP.
 * Fornece consulta da lista de ferramentas do MCP Server e consulta dos metadados globais de ferramentas do sistema.
 */
public interface McpToolQueryService {


    /**
     * Obtém o resumo das ferramentas globais embutidas do sistema (name + description)
     */
    List<Map<String, String>> getSystemGlobalToolSummaries();
}
