package com.xiaozhi.device.domain.vo;

import java.time.LocalDateTime;

/**
 * Objeto de valor do código de verificação (correspondente ao cenário de ativação de dispositivo da tabela sys_code).
 * <p>
 * Imutável; a semântica de igualdade é garantida automaticamente pelo record.
 */
public record VerifyCode(
        String code,
        String deviceId,
        String sessionId,
        String type,
        String audioPath,
        LocalDateTime createTime
) {}
