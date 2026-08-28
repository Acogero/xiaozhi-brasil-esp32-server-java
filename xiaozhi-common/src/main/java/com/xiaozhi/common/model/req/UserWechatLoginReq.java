package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Requisição de login do WeChat")
public class UserWechatLoginReq {

    @Schema(description = "Código de login do WeChat", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "O código de login do WeChat não pode ser vazio")
    private String code;

    @Schema(description = "ID do convidante")
    private Integer inviterId;
}
