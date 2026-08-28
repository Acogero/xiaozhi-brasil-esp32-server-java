package com.xiaozhi.communication.registry;

import lombok.Data;

import java.io.Serializable;

/**
 * Informações do servidor Dialogue — usado para registro/descoberta de serviço
 */
@Data
public class DialogueServerInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String instanceId;
    private String websocketAddress;
    private String udpAddress;
    private String otaAddress;
    private String mcpAddress;
    private String serverAddress;
    private long lastHeartbeat;
}
