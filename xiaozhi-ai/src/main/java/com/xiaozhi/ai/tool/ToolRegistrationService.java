package com.xiaozhi.ai.tool;

import com.xiaozhi.ai.tool.session.ToolSession;
import com.xiaozhi.mcptoolexclude.service.McpToolExcludeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
/**
 * Coordenador de registro de ferramentas, orquestrando de forma unificada o registro.
 * <p>
 * Responsabilidades:
 * 1. Obtém a lista de ferramentas excluídas da sessão atual (nível global + nível de papel/role)
 * 2. Chama sequencialmente todas as implementações de {@link ToolRegistrar} injetadas
 */
@Slf4j
@Service
public class ToolRegistrationService {

    @Resource
    private McpToolExcludeService mcpToolExcludeService;

    /**
     * Todas as implementações de ToolRegistrar, coletadas automaticamente pelo Spring
     */
    @Resource
    private List<ToolRegistrar> registrars;

    /**
     * Registra todas as ferramentas disponíveis na sessão
     *
     * @param toolSession Sessão atual do dispositivo
     */
    public void register(ToolSession toolSession) {
        Integer roleId = toolSession.getRoleId();
        Set<String> excludedTools = mcpToolExcludeService.getExcludedTools(roleId);

        for (ToolRegistrar registrar : registrars) {
            try {
                registrar.register(toolSession, excludedTools);
            } catch (Exception e) {
                log.warn("Falha no registro do ToolRegistrar {}", registrar.getClass().getSimpleName(), e);
            }
        }

    }

}
