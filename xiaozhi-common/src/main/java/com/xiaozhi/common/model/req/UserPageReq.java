package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Consulta paginada de usuários")
public class UserPageReq extends BasePageReq {

    @Schema(description = "Nome/Apelido")
    private String name;

    @Schema(description = "E-mail")
    private String email;

    @Schema(description = "Número de telefone")
    private String tel;

    @Schema(description = "Se é administrador")
    private String isAdmin;

    @Schema(description = "ID do papel de permissão do backend")
    private Integer authRoleId;
}
