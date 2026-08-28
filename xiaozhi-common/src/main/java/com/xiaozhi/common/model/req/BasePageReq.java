package com.xiaozhi.common.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "Classe base de requisição paginada")
public abstract class BasePageReq implements Serializable {

    @Schema(description = "Número da página", example = "1")
    @Min(value = 1, message = "O número da página deve ser no mínimo 1")
    private Integer pageNo = 1;

    @Schema(description = "Itens por página", example = "10")
    @Min(value = 1, message = "A quantidade por página deve ser no mínimo 1")
    @Max(value = 1000, message = "A quantidade por página deve ser no máximo 1000")
    private Integer pageSize = 10;
}
