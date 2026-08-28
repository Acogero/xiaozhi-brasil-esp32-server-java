package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Consulta paginada de modelos de prompt")
public class TemplatePageReq extends BasePageReq {

    @Schema(description = "Nome do template")
    private String templateName;

    @Schema(description = "Categoria do template")
    private String category;
}
