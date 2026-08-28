package com.xiaozhi.common.exception;

/**
 * Usada para representar uma falha conhecida em operação de negócio, que não é erro de parâmetro nem recurso inexistente.
 */
public class OperationFailedException extends RuntimeException {

    public OperationFailedException(String message) {
        super(message);
    }

    public OperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
