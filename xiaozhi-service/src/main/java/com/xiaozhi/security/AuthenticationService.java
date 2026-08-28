package com.xiaozhi.security;

/**
 * Criptografia e verificação de senha
 * 
 * @author Joey
 * 
 */

public interface AuthenticationService {
  /**
   * Criptografia de senha
   * 
   * @param rawPassword
   * @return Senha criptografada
   */
  public String encryptPassword(String rawPassword);

  /**
   * Verificação de senha
   * 
   * @param rawPassword
   * @param encryptPassword
   * @return Se são iguais
   */
  public Boolean isPasswordValid(String rawPassword, String encryptPassword);
}