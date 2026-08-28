package com.xiaozhi.ai.tool;

import com.xiaozhi.ai.tool.session.ToolSession;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ToolsGlobalRegistry implements ToolCallbackResolver {
    private static final String TAG = "FUNCTION_GLOBAL";

    // Usado para armazenar a lista de todas as functions
    protected static final ConcurrentHashMap<String, ToolCallback> allFunction
            = new ConcurrentHashMap<>();

    @Autowired(required = false)
    protected List<GlobalFunction> globalFunctions = List.of();

    @Autowired(required = false)
    private GlobalToolRedisRegistry globalToolRedisRegistry;

    /**
     * Após a inicialização da aplicação, publica no Redis os metadados de GlobalFunction registrados neste processo,
     * permitindo que o processo server obtenha, na interface de "excluir ferramentas", a lista de ferramentas mantida pelo processo dialogue.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void publishGlobalToolMetadata() {
        if (globalToolRedisRegistry == null || globalFunctions == null || globalFunctions.isEmpty()) {
            return;
        }
        List<GlobalToolRedisRegistry.ToolSummary> summaries = globalFunctions.stream()
                .map(f -> new GlobalToolRedisRegistry.ToolSummary(f.getToolName(), f.getToolDescription()))
                .toList();
        globalToolRedisRegistry.publish(summaries);
    }

    @Override
    public ToolCallback resolve(@NotNull String toolName) {
        return allFunction.get(toolName);
    }

    /**
     * Register a function by name
     *
     * @param name the name of the function to register
     * @return the registered function or null if not found
     */
    public ToolCallback registerFunction(String name, ToolCallback functionCallTool) {
        ToolCallback result = allFunction.putIfAbsent(name, functionCallTool);
        return result;
    }

    /**
     * Unregister a function by name
     *
     * @param name the name of the function to unregister
     * @return true if successful, false otherwise
     */
    public boolean unregisterFunction(String name) {
        // Check if the function exists before unregistering
        if (!allFunction.containsKey(name)) {
            return false;
        }
        allFunction.remove(name);
        return true;
    }

    /**
     * Get all registered functions
     *
     * @return a map of all registered functions
     */
    public Map<String, ToolCallback> getAllFunctions(ToolSession toolSession) {
        // Observação: aqui não se registra mais automaticamente todas as funções globais em allFunction
        // Em vez disso, retorna um Map temporário; o registro das ferramentas é gerenciado de forma unificada pelo ToolRegistrationService
        Map<String, ToolCallback> tempFunctions = new HashMap<>();
        globalFunctions.forEach(
                globalFunction -> {
                    ToolCallback toolCallback = globalFunction.getFunctionCallTool(toolSession);
                    if(toolCallback != null){
                        tempFunctions.put(toolCallback.getToolDefinition().name(), toolCallback);
                    }
                }
        );
        return tempFunctions;
    }

    /**
     * Obtém o resumo de todas as GlobalFunction registradas (name + description)
     */
    public List<Map<String, String>> getGlobalToolSummaries() {
        return globalFunctions.stream()
                .map(f -> Map.of("name", f.getToolName(), "description", f.getToolDescription()))
                .toList();
    }

    public interface GlobalFunction{
        ToolCallback getFunctionCallTool(ToolSession toolSession);

        /**
         * Nome da ferramenta
         */
        String getToolName();

        /**
         * Descrição da ferramenta
         */
        String getToolDescription();
    }
}
