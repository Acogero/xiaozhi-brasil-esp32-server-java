package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Informações do modelo de prompt")
public class TemplateResp {

    @Schema(description = "ID do template")
    private Integer templateId;

    @Schema(description = "ID do usuário")
    private Integer userId;

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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Data de criação")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Data de atualização")
    private LocalDateTime updateTime;
}
