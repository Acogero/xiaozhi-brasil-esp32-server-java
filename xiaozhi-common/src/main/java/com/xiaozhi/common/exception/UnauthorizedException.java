package com.xiaozhi.common.exception;

/**
 * Exceção de permissão insuficiente
 * Lançada quando o usuário tenta operar um recurso que não lhe pertence
 * 
 * @author Joey
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}

