package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Requisição de redefinição de senha")
public class UserResetPasswordReq {

    @Schema(description = "E-mail", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "O e-mail não pode ser vazio")
    @Email(message = "Formato de e-mail inválido")
    private String email;

    @Schema(description = "Código de verificação", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "O código de verificação não pode ser vazio")
    private String code;

    @Schema(description = "Nova senha", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "A nova senha não pode ser vazia")
    @Size(min = 6, max = 20, message = "A senha deve ter entre 6 e 20 caracteres")
    private String password;
}
