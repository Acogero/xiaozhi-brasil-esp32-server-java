package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Requisição de atualização de usuário")
public class UserUpdateReq {

    @Schema(description = "Novo e-mail")
    @Email(message = "Formato de e-mail inválido")
    private String email;

    @Schema(description = "Novo número de telefone")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Formato de número de telefone inválido")
    private String tel;

    @Schema(description = "Nova senha")
    @Size(min = 6, max = 20, message = "A senha deve ter entre 6 e 20 caracteres")
    private String password;

    @Schema(description = "Novo nome/apelido")
    @Size(max = 50, message = "O nome não pode ter mais de 50 caracteres")
    private String name;

    @Schema(description = "Novo avatar")
    private String avatar;
}
