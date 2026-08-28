package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Requisição de login por telefone e código de verificação")
public class UserTelLoginReq {

    @Schema(description = "Número de telefone", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "O número de telefone não pode ser vazio")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Formato de número de telefone inválido")
    private String tel;

    @Schema(description = "Código de verificação", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "O código de verificação não pode ser vazio")
    @Pattern(regexp = "^\\d{6}$", message = "Formato do código de verificação inválido")
    private String code;
}
