package com.xiaozhi.common.model.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * BO de código de verificação (correspondente à tabela sys_code).
 * <p>
 * A tabela sys_code é uma tabela de código de verificação multiuso, usando campos diferentes conforme o cenário:
 * <ul>
 *   <li>Ativação de dispositivo: deviceId, sessionId, type, code, audioPath</li>
 *   <li>Registro de usuário: email, code</li>
 * </ul>
 */
@Data
public class VerifyCodeBO {

    private String email;

    private String deviceId;

    private String sessionId;

    private String type;

    private String code;

    private String audioPath;

    private LocalDateTime createTime;
}
