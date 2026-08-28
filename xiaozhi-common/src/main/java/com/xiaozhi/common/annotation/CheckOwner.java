package com.xiaozhi.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declara que a interface atual precisa validar se um determinado recurso pertence ao usuário logado.
 * <p>
 * {@code id} usa uma expressão SpEL para extrair o identificador do recurso a partir dos parâmetros do método, por exemplo:
 * <pre>
 * {@code
 * @CheckOwner(resource = "role", id = "#roleId")
 * @CheckOwner(resource = "device", id = "#req.deviceId")
 * }
 * </pre>
 * Também suporta retornar array ou coleção; o aspecto validará a propriedade item por item.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CheckOwners.class)
public @interface CheckOwner {

    /**
     * Tipo do recurso, correspondente ao nome do ownership checker registrado no backend.
     */
    String resource();

    /**
     * Expressão SpEL do ID do recurso, por exemplo {@code #roleId}, {@code #req.deviceId}.
     */
    String id();

    /**
     * Se o administrador pode ignorar essa validação de recurso.
     */
    boolean adminBypass() default true;
}
