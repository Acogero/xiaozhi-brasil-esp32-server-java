package com.xiaozhi.template.domain;

import com.xiaozhi.common.model.bo.TemplateBO;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Template Raiz de agregação — representa um modelo de prompt de conversa.
 * <p>
 * Invariante: no máximo um modelo padrão por userId (mantido por TemplateRepository.save).
 */
@Getter
public class Template {

    public static final String STATE_ENABLED  = "1";
    public static final String STATE_DISABLED = "0";

    public enum DomainSignal { DEFAULT_CHANGED, UPDATED, DISABLED }

    private Integer       templateId;
    private Integer       userId;
    private String        templateName;
    private String        templateDesc;
    private String        templateContent;
    private String        category;
    private String        state;
    private boolean       isDefault;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private final List<DomainSignal> signals = new ArrayList<>();

    public Template() {}

    // ── Métodos de fábrica ──────────────────────────────────────────────────

    public static Template newTemplate(Integer userId, String templateName, String templateDesc,
                                       String templateContent, String category, boolean isDefault) {
        Template t = new Template();
        t.userId          = userId;
        t.templateName    = templateName;
        t.templateDesc    = templateDesc;
        t.templateContent = templateContent;
        t.category        = category;
        t.state           = STATE_ENABLED;
        t.isDefault       = isDefault;
        if (isDefault) t.signals.add(DomainSignal.DEFAULT_CHANGED);
        return t;
    }

    public static Template newTemplate(Integer userId, TemplateBO bo) {
        return newTemplate(userId, bo.getTemplateName(), bo.getTemplateDesc(),
                bo.getTemplateContent(), bo.getCategory(),
                "1".equals(bo.getIsDefault()));
    }

    /** Reconstrói a raiz de agregação a partir da camada de persistência (uso exclusivo do Repository, não gera sinais). */
    public static Template reconstitute(Integer templateId, Integer userId,
                                        String templateName, String templateDesc,
                                        String templateContent, String category,
                                        String state, boolean isDefault,
                                        LocalDateTime createTime, LocalDateTime updateTime) {
        Template t = new Template();
        t.templateId      = templateId;
        t.userId          = userId;
        t.templateName    = templateName;
        t.templateDesc    = templateDesc;
        t.templateContent = templateContent;
        t.category        = category;
        t.state           = state;
        t.isDefault       = isDefault;
        t.createTime      = createTime;
        t.updateTime      = updateTime;
        return t;
    }

    // ── Métodos de comportamento ────────────────────────────────────────────

    public void setAsDefault() {
        if (!this.isDefault) {
            this.isDefault = true;
            signals.add(DomainSignal.DEFAULT_CHANGED);
        }
    }

    public void clearDefault() {
        this.isDefault = false;
    }

    public void update(TemplateBO bo) {
        update(bo.getTemplateName(), bo.getTemplateDesc(), bo.getTemplateContent(),
                bo.getCategory(), bo.getIsDefault() == null ? null : "1".equals(bo.getIsDefault()));
    }

    public void update(String templateName, String templateDesc,
                       String templateContent, String category, Boolean isDefault) {
        if (templateName    != null) this.templateName    = templateName;
        if (templateDesc    != null) this.templateDesc    = templateDesc;
        if (templateContent != null) this.templateContent = templateContent;
        if (category        != null) this.category        = category;
        if (isDefault != null) {
            if (isDefault && !this.isDefault) {
                this.isDefault = true;
                signals.add(DomainSignal.DEFAULT_CHANGED);
            } else if (!isDefault) {
                this.isDefault = false;
            }
        }
        signals.add(DomainSignal.UPDATED);
    }

    public void disable() {
        this.state     = STATE_DISABLED;
        this.isDefault = false;
        signals.add(DomainSignal.DISABLED);
    }

    /** Preenchido pelo Repository após o insert com a chave primária autoincrementada, não gera sinais. */
    public void assignId(Integer templateId) {
        this.templateId = templateId;
    }

    public List<DomainSignal> pullSignals() {
        List<DomainSignal> s = List.copyOf(signals);
        signals.clear();
        return s;
    }
}
