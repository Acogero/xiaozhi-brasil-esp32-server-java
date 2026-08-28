package com.xiaozhi.communication.domain;

import com.xiaozhi.communication.domain.mcp.device.initialize.DeviceMcpPayload;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Classe de requisição MCP, usada para tratar requisições MCP enviadas ao dispositivo
 */
@Data
@EqualsAndHashCode(callSuper = true)
public final  class DeviceMcpMessage extends Message {
    public DeviceMcpMessage() {
        super("mcp");
    }

    private String sessionId;//id da sessão
    private String type = "mcp";
    private DeviceMcpPayload payload;
}