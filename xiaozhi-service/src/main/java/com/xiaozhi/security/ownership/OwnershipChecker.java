package com.xiaozhi.security.ownership;

/**
 * Verificador de propriedade de recurso.
 */
public interface OwnershipChecker {

    /**
     * Identificador do tipo de recurso, por exemplo role/config/device.
     */
    String getResource();

    /**
     * Valida se o recurso pertence ao usuário atual; deve lançar uma exceção de negócio caso não pertença.
     *
     * @param resourceId ID do recurso
     * @param userId     ID do usuário atualmente logado
     */
    void check(Object resourceId, Integer userId);
}
