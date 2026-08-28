package com.xiaozhi.ai.tool;

import com.xiaozhi.ai.tool.session.ToolSession;

import java.util.Set;

/**
 * Interface do registrador de ferramentas.
 * Cada origem de ferramenta corresponde a uma implementação: ferramentas globais do sistema, ferramentas MCP do dispositivo, ferramentas de MCP Server remoto, ferramentas de MCP Endpoint local.
 * Coletadas e injetadas automaticamente pelo Spring.
 */
public interface ToolRegistrar {

    /**
     * Registra ferramentas na sessão
     *
     * @param toolSession   Sessão atual do dispositivo
     * @param excludedTools Conjunto de nomes de ferramentas a excluir
     */
    void register(ToolSession toolSession, Set<String> excludedTools);
}
