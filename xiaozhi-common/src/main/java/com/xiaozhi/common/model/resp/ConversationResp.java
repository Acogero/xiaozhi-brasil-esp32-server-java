package com.xiaozhi.common.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "Resposta de sessão")
public class ConversationResp {

    @Schema(description = "ID da sessão")
    private String sessionId;

    @Schema(description = "ID do papel")
    private Integer roleId;

    @Schema(description = "Nome do papel")
    private String roleName;

    @Schema(description = "Título da sessão (conteúdo da primeira mensagem)")
    private String title;

    @Schema(description = "Data da última atualização")
    private Date updateTime;
}
