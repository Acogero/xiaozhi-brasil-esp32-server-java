package com.xiaozhi.template.domain.repository;

import com.xiaozhi.template.domain.Template;

import java.util.Optional;

/**
 * Interface de repositório da raiz de agregação Template.
 * <p>
 * save() mantém automaticamente o invariante "único modelo padrão por userId".
 */
public interface TemplateRepository {

    Optional<Template> findById(Integer templateId);

    /**
     * Persiste a raiz de agregação (criação ou atualização).
     * <p>Se o sinal DEFAULT_CHANGED for detectado, chama resetDefault antes de salvar.
     */
    void save(Template template);

    /** Exclusão lógica (state=disabled). */
    void delete(Integer templateId);
}
