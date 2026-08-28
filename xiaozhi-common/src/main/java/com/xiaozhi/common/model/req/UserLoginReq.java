package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Requisição de login com nome de usuário e senha")
public class UserLoginReq {

    @Schema(description = "Nome de usuário/E-mail/Telefone", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "O nome de usuário não pode ser vazio")
    private String username;

    @Schema(description = "Senha", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "A senha não pode ser vazia")
    private String password;
}
