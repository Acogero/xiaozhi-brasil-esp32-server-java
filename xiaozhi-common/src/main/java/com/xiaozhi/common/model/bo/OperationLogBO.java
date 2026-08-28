package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * BO de log de operação (correspondente à tabela sys_operation_log).
 */
@Data
public class OperationLogBO {

    private Long id;
    private Integer userId;
    private String ip;
    private String module;
    private String operation;
    private String method;
    private String url;
    private String handler;
    private String params;
    private Boolean success;
    private String errorMsg;
    private Integer costMs;
    private LocalDateTime createTime;
}
