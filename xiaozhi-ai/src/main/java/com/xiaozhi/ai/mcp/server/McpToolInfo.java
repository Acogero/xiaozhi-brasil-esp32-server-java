package com.xiaozhi.ai.mcp.server;

/**
 * Informações da ferramenta MCP
 */
public class McpToolInfo {

    private final String serverCode;
    private final ToolDefinition definition;

    public McpToolInfo(String serverCode, ToolDefinition definition) {
        this.serverCode = serverCode;
        this.definition = definition;
    }

    public String getServerCode() {
        return serverCode;
    }

    public ToolDefinition getDefinition() {
        return definition;
    }

    public record ToolDefinition(String name, String description, Object inputSchema) {
    }
}
