package com.xiaozhi.common.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta de login")
public class LoginResp {

    @Schema(description = "Token de acesso")
    private String token;

    @Schema(description = "Token de atualização")
    private String refreshToken;

    @Schema(description = "Tempo de expiração (segundos)")
    private Integer expiresIn;

    @Schema(description = "ID do usuário")
    private Integer userId;

    @Schema(description = "Se é um novo usuário")
    private Boolean isNewUser;

    @Schema(description = "Informações do usuário")
    private UserResp user;

    @Schema(description = "Papel de permissão do backend")
    private AuthRoleResp authRole;

    @Schema(description = "Árvore de permissões")
    private List<PermissionTreeResp> permissions;
}
