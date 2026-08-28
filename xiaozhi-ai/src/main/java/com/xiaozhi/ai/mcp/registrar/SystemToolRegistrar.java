package com.xiaozhi.ai.mcp.registrar;

import com.xiaozhi.ai.tool.ToolRegistrar;
import com.xiaozhi.ai.tool.ToolsGlobalRegistry;
import com.xiaozhi.ai.tool.ToolsSessionHolder;
import com.xiaozhi.ai.tool.session.ToolSession;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Registrador global de ferramentas do sistema.
 * Registra as ferramentas do sistema presentes em {@link ToolsGlobalRegistry#getAllFunctions(ToolSession)} (PlayMusic, ChangeRole, etc.).
 *
 */
@Component
@Order(3)
public class SystemToolRegistrar implements ToolRegistrar {

    @Resource
    private ToolsGlobalRegistry toolsGlobalRegistry;

    @Override
    public void register(ToolSession toolSession, Set<String> excludedTools) {
        ToolsSessionHolder functionSessionHolder = toolSession.getToolsSessionHolder();
        Map<String, ToolCallback> globalFunctions = toolsGlobalRegistry.getAllFunctions(toolSession);

        globalFunctions.forEach((toolName, toolCallback) -> {
            if (!excludedTools.contains(toolName)) {
                functionSessionHolder.registerFunction(toolName, toolCallback);
            }
        });
    }
}
