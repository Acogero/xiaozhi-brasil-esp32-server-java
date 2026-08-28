package com.xiaozhi.role.domain.repository;

import com.xiaozhi.role.domain.Role;

import java.util.Optional;

/**
 * Interface de repositório da raiz de agregação Role (definida na camada de domínio, implementada na infraestrutura).
 */
public interface RoleRepository {

    /** Carrega a raiz de agregação pelo ID (sem validar propriedade) */
    Optional<Role> findById(Integer roleId);

    /**
     * Persiste a raiz de agregação (criação ou atualização).
     * <p>Na criação, {@link Role#assignId(Integer)} é chamado para preencher o ID autoincrementado.
     * Se {@code role.isDefault()} for true, a implementação é responsável por redefinir o isDefault dos
     * demais papéis do mesmo usuário para "0" (mantendo o invariante "único papel padrão").
     */
    void save(Role role);

    /** Remove o papel (e limpa o cache) */
    void delete(Integer roleId);
}
