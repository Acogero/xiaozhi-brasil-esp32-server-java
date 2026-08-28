package com.xiaozhi.security;

import com.xiaozhi.utils.CommonUtils;
import org.springframework.stereotype.Service;

/**
 * Criptografia e verificação de senha
 *
 * @author Joey
 *
 */
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final String salt = "joey@zhou";

    /**
     * @param rawPassword
     * @return Senha criptografada
     */
    public String encryptPassword(String rawPassword) {
        String saltPassword = rawPassword + salt;
        return CommonUtils.md5(saltPassword);
    }

    /**
     * Verificação de senha
     * 
     * @param rawPassword
     * @param encryptPassword
     * @return Se são iguais
     */
    public Boolean isPasswordValid(String rawPassword, String encryptPassword) {
        String encodePassword = encryptPassword(rawPassword);
        return encodePassword.equals(encryptPassword);
    }

}