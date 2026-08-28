package com.xiaozhi.communication.domain.mcp.device.initialize;

import lombok.Data;

import java.util.Map;

@Data
public class DeviceMcpPayload {
    private String jsonrpc = "2.0";
    private String method;//nome do método
    private Object params;
    private Long id;//id da requisição
    private Map<String, Object> result;//resultado da requisição
    private Map<String, Object> error;//informações de falha da requisição

}