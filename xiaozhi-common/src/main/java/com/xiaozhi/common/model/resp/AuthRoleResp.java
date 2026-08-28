package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Papel de permissão do backend")
public class AuthRoleResp {

    @Schema(description = "ID do papel")
    private Integer authRoleId;

    @Schema(description = "Nome do papel")
    private String authRoleName;

    @Schema(description = "Identificador do papel")
    private String roleKey;

    @Schema(description = "Descrição do papel")
    private String description;

    @Schema(description = "Status")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Data de criação")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Data de atualização")
    private LocalDateTime updateTime;
}
