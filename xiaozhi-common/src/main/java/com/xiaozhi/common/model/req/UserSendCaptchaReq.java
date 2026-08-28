package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Requisição de envio de código de verificação")
public class UserSendCaptchaReq {

    @Schema(description = "E-mail")
    @Email(message = "Formato de e-mail inválido")
    private String email;

    @Schema(description = "Número de telefone")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Formato de número de telefone inválido")
    private String tel;

    @Schema(description = "Tipo de uso", allowableValues = {"register", "forget"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "O tipo não pode ser vazio")
    private String type;
}
