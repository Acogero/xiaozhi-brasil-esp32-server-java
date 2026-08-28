package com.xiaozhi.communication.domain.mcp.device.initialize;

import lombok.Data;

/**
 * Relacionado à visão da câmera
 */
@Data
public class DeviceMcpVision {
    private String url;//câmera: endereço de processamento de imagem (deve ser um endereço http, não um endereço websocket)
    private String token;// url toke
}