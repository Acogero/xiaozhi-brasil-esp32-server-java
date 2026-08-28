package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Requisição de atualização de modelo de prompt")
public class TemplateUpdateReq {

    @Schema(description = "Nome do template")
    private String templateName;

    @Schema(description = "Descrição do template")
    private String templateDesc;

    @Schema(description = "Conteúdo do template")
    private String templateContent;

    @Schema(description = "Categoria do template")
    private String category;

    @Schema(description = "Se é o modelo padrão (1 sim, 0 não)")
    private String isDefault;

    @Schema(description = "Status (1 habilitado, 0 desabilitado)")
    private String state;
}
