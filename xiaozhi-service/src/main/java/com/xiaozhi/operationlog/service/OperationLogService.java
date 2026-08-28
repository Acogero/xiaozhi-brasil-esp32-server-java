package com.xiaozhi.operationlog.service;

import com.xiaozhi.common.model.bo.OperationLogBO;

public interface OperationLogService {

    /**
     * Salva o log de operação de forma assíncrona, sem afetar o desempenho do fluxo principal.
     */
    void saveAsync(OperationLogBO log);
}
