package com.xiaozhi.common.exception;

/**
 * Exceção de recurso não encontrado
 * Lançada quando o recurso solicitado (roteiro, script, papel, etc.) não existe
 * 
 * @author Joey
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

