package com.xiaozhi.ai.mcp.config;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Implementação customizada de tratamento do MCP
 */
@Component
public class CustomMcpSyncClientCustomizer implements McpSyncClientCustomizer {
    @Override
    public void customize(String serverConfigurationName, McpClient.SyncSpec spec) {
        spec.requestTimeout(Duration.ofSeconds(30));
        McpSchema.Root root = new McpSchema.Root("", "");
        spec.roots(root);
    }
}
