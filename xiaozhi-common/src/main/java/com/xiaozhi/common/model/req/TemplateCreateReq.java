package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Requisição de criação de modelo de prompt")
public class TemplateCreateReq {

    @Schema(description = "Nome do template", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "O nome do template não pode ser vazio")
    private String templateName;

    @Schema(description = "Descrição do template")
    private String templateDesc;

    @Schema(description = "Conteúdo do template", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "O conteúdo do template não pode ser vazio")
    private String templateContent;

    @Schema(description = "Categoria do template", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "A categoria do template não pode ser vazia")
    private String category;

    @Schema(description = "Se é o modelo padrão (1 sim, 0 não)")
    private String isDefault;

    @Schema(description = "Status (1 habilitado, 0 desabilitado)")
    private String state;
}
