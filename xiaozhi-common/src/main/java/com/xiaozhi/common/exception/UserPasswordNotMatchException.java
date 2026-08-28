package com.xiaozhi.common.exception;

/**
 * Exceção de senha incorreta
 * 
 * @author Joey
 */

public class UserPasswordNotMatchException extends RuntimeException {
  public UserPasswordNotMatchException() {
  }

  public UserPasswordNotMatchException(String msg) {
    super(msg);
  }
}