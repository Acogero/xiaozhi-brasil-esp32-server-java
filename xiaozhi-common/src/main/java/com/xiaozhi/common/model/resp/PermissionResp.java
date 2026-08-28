package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Informações da permissão")
public class PermissionResp {

    @Schema(description = "ID da permissão")
    private Integer permissionId;

    @Schema(description = "ID da permissão pai")
    private Integer parentId;

    @Schema(description = "Nome da permissão")
    private String name;

    @Schema(description = "Identificador da permissão")
    private String permissionKey;

    @Schema(description = "Tipo de permissão")
    private String permissionType;

    @Schema(description = "Caminho de rota")
    private String path;

    @Schema(description = "Caminho do componente frontend")
    private String component;

    @Schema(description = "Ícone")
    private String icon;

    @Schema(description = "Ordenação")
    private Integer sort;

    @Schema(description = "Se está visível")
    private String visible;

    @Schema(description = "Status")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Data de criação")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Data de atualização")
    private LocalDateTime updateTime;
}
