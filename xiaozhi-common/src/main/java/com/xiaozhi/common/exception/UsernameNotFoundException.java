package com.xiaozhi.common.exception;

/**
 * Exceção de nome de usuário não encontrado
 * 
 * @author Joey
 */
public class UsernameNotFoundException extends RuntimeException {
  public UsernameNotFoundException() {
  }

  public UsernameNotFoundException(String msg) {
    super(msg);
  }
}