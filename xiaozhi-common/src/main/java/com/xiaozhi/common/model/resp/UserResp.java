package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Informações do usuário")
public class UserResp {

    @Schema(description = "ID do usuário")
    private Integer userId;

    @Schema(description = "Nome de usuário")
    private String username;

    @Schema(description = "Nome/Apelido")
    private String name;

    @Schema(description = "E-mail")
    private String email;

    @Schema(description = "Número de telefone")
    private String tel;

    @Schema(description = "Avatar")
    private String avatar;

    @Schema(description = "Status")
    private String state;

    @Schema(description = "Se é administrador")
    private String isAdmin;

    @Schema(description = "ID do papel de permissão do backend")
    private Integer authRoleId;

    @Schema(description = "Nome do papel de permissão do backend")
    private String authRoleName;

    @Schema(description = "Total acumulado de mensagens")
    private Integer totalMessage;

    @Schema(description = "Total de dispositivos")
    private Integer totalDevice;

    @Schema(description = "Número de dispositivos online")
    private Integer aliveNumber;

    @Schema(description = "IP do último login")
    private String loginIp;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Data do último login")
    private LocalDateTime loginTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Data de criação")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Data de atualização")
    private LocalDateTime updateTime;
}
