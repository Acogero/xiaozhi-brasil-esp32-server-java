package com.xiaozhi.common.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta paginada")
public class PageResp<T> implements Serializable {

    @Schema(description = "Lista de dados")
    private List<T> list;

    @Schema(description = "Total de registros")
    private Long total;

    @Schema(description = "Número da página")
    private Integer pageNo;

    @Schema(description = "Itens por página")
    private Integer pageSize;
}
