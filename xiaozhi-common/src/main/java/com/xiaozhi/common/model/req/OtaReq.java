package com.xiaozhi.common.model.req;

import lombok.Data;

/**
 * Dados da requisição OTA, extraídos pelo Controller a partir da requisição HTTP e repassados ao AppService.
 */
@Data
public class OtaReq {

    /** ID do dispositivo (endereço MAC) */
    private String deviceId;

    /** Modelo do chip */
    private String chipModelName;

    /** Versão do firmware */
    private String version;

    /** Nome do WiFi */
    private String wifiName;

    /** Tipo de dispositivo */
    private String type;

    /** IP do cliente */
    private String ip;

    /** Localização geográfica (preenchida pelo AppService a partir da resolução do IP) */
    private String location;
}
