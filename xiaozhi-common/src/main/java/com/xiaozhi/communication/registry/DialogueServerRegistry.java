package com.xiaozhi.communication.registry;

import java.util.List;

/**
 * Centro de registro do servidor Dialogue — usado para descoberta de serviço e balanceamento de carga em escalonamento horizontal
 */
public interface DialogueServerRegistry {

    /**
     * Registra a instância do servidor dialogue
     */
    void register(DialogueServerInfo serverInfo);

    /**
     * Remove o registro da instância do servidor dialogue
     */
    void unregister(String instanceId);

    /**
     * Atualização de heartbeat, estende o TTL
     */
    void heartbeat(DialogueServerInfo serverInfo);

    /**
     * Obtém todos os servidores dialogue disponíveis
     */
    List<DialogueServerInfo> getAvailableServers();

    /**
     * Seleciona um servidor dialogue por balanceamento de carga
     */
    DialogueServerInfo selectServer();
}
