package com.xiaozhi.communication.domain;

import lombok.Data;

@Data
public class HelloFeatures {
    /**
     * Se o dispositivo tem o MCP habilitado
     */
    private Boolean mcp = false;
    /**
     * Se o dispositivo tem o AEC do lado do servidor habilitado
     */
    private Boolean aec = false;
}