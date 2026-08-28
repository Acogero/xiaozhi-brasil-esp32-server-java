package com.xiaozhi.common.model.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Consulta paginada de mensagens")
public class MessagePageReq extends BasePageReq {

    @Schema(description = "ID do dispositivo")
    private String deviceId;

    @Schema(description = "Nome do dispositivo")
    private String deviceName;

    @Schema(description = "Remetente")
    private String sender;

    @Schema(description = "Tipo de mensagem")
    private String messageType;

    @Schema(description = "ID do papel")
    private Integer roleId;

    @Schema(description = "Data de início")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @Schema(description = "Data de término")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    @Schema(description = "ID da sessão")
    private String sessionId;

    @Schema(description = "Origem da mensagem: web|device")
    private String source;
}
