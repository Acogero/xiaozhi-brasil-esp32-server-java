package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
@Schema(description = "Requisição de registro de usuário")
public class UserRegisterReq {

    @Schema(description = "Nome de usuário", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "O nome de usuário não pode ser vazio")
    @Size(min = 3, max = 20, message = "O nome de usuário deve ter entre 3 e 20 caracteres")
    private String username;

    @Schema(description = "Senha", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "A senha não pode ser vazia")
    @Size(min = 6, max = 20, message = "A senha deve ter entre 6 e 20 caracteres")
    private String password;

    @Schema(description = "Nome/Apelido")
    @Size(max = 50, message = "O nome não pode ter mais de 50 caracteres")
    private String name;

    @Schema(description = "E-mail")
    @Email(message = "Formato de e-mail inválido")
    private String email;

    @Schema(description = "Número de telefone")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Formato de número de telefone inválido")
    private String tel;

    @Schema(description = "Código de verificação", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "O código de verificação não pode ser vazio")
    @Pattern(regexp = "^\\d{6}$", message = "Formato do código de verificação inválido")
    private String code;

    @AssertTrue(message = "Preencha pelo menos e-mail ou telefone")
    public boolean hasAccount() {
        return StringUtils.hasText(email) || StringUtils.hasText(tel);
    }
}
