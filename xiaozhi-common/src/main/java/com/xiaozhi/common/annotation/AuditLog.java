package com.xiaozhi.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação de log de auditoria de operação, marca interfaces sensíveis que precisam registrar log de operação.
 * <p>
 * O aspecto AOP grava o registro de operação de forma assíncrona na tabela sys_operation_log após a execução do método.
 * Interfaces de consulta não precisam adicionar esta anotação.
 * <p>
 * Exemplo de uso:
 * <pre>
 * {@code
 * @AuditLog(module = "Gerenciamento de Dispositivos", operation = "Criar Dispositivo")
 * @PostMapping("/")
 * public ApiResponse<?> create(...) { ... }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * Módulo da operação, por exemplo "Gerenciamento de Dispositivos", "Gerenciamento de Usuários".
     */
    String module();

    /**
     * Descrição da operação, por exemplo "Criar Dispositivo", "Excluir Papel".
     */
    String operation();
}
