package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Informações da mensagem")
public class MessageResp {

    @Schema(description = "ID da mensagem")
    private Integer messageId;

    @Schema(description = "ID do dispositivo")
    private String deviceId;

    @Schema(description = "Nome do dispositivo")
    private String deviceName;

    @Schema(description = "Remetente")
    private String sender;

    @Schema(description = "Conteúdo da mensagem")
    private String message;

    @Schema(description = "Caminho do arquivo de áudio")
    private String audioPath;

    @Schema(description = "Status da mensagem")
    private String state;

    @Schema(description = "Tipo de mensagem")
    private String messageType;

    @Schema(description = "Detalhes da chamada da ferramenta")
    private String toolCalls;

    @Schema(description = "ID da sessão")
    private String sessionId;

    @Schema(description = "Origem da mensagem: web|device")
    private String source;

    @Schema(description = "ID do papel")
    private Integer roleId;

    @Schema(description = "Nome do papel")
    private String roleName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Data de criação")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Data de atualização")
    private LocalDateTime updateTime;
}
