package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Requisição de verificação de disponibilidade de usuário")
public class UserCheckReq {

    @Schema(description = "Nome de usuário")
    private String username;

    @Schema(description = "E-mail")
    @Email(message = "Formato de e-mail inválido")
    private String email;

    @Schema(description = "Número de telefone")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Formato de número de telefone inválido")
    private String tel;
}
