package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Consulta paginada de sessões")
public class ConversationPageReq extends BasePageReq {

    @Schema(description = "ID do papel")
    private Integer roleId;

    @Schema(description = "Origem da mensagem: web|device")
    private String source;
}
