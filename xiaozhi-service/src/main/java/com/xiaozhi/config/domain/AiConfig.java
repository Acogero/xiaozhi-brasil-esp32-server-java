package com.xiaozhi.config.domain;

import com.xiaozhi.common.model.bo.ConfigBO;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AiConfig Raiz de agregação — representa uma configuração de modelo de IA (LLM / TTS / STT / VAD / Embedding etc.).
 * <p>
 * Invariante: no máximo uma configuração padrão por combinação de userId + configType + modelType (mantido por ConfigRepository.save).
 */
@Getter
public class AiConfig {

    public static final String STATE_ENABLED  = "1";
    public static final String STATE_DISABLED = "0";

    public enum DomainSignal { DEFAULT_CHANGED, UPDATED, DISABLED }

    // ── Identificação ───────────────────────────────────────────────────────
    private Integer       configId;
    private Integer       userId;

    // ── Metadados ───────────────────────────────────────────────────────────
    private String configName;
    private String configDesc;
    private String configType;
    private String modelType;
    private String provider;

    // ── Credenciais ─────────────────────────────────────────────────────────
    private String appId;
    private String apiKey;
    private String apiSecret;
    private String ak;
    private String sk;
    private String apiUrl;

    // ── Capacidades ─────────────────────────────────────────────────────────
    private Boolean enableThinking;

    // ── Estado ──────────────────────────────────────────────────────────────
    private String  state;
    private boolean isDefault;

    // ── Timestamps ──────────────────────────────────────────────────────────
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private final List<DomainSignal> signals = new ArrayList<>();

    /** Uso exclusivo do ConfigConverter para reconstrução */
    public AiConfig() {}

    // ── Métodos de fábrica ──────────────────────────────────────────────────

    public static AiConfig newConfig(Integer userId, String configType, String provider,
                                     String configName, String configDesc, String modelType,
                                     String appId, String apiKey, String apiSecret,
                                     String ak, String sk, String apiUrl,
                                     Boolean enableThinking, boolean isDefault) {
        AiConfig c = new AiConfig();
        c.userId     = userId;
        c.configType = configType;
        c.provider   = provider;
        c.configName = configName;
        c.configDesc = configDesc;
        c.modelType  = modelType;
        c.appId      = appId;
        c.apiKey     = apiKey;
        c.apiSecret  = apiSecret;
        c.ak         = ak;
        c.sk         = sk;
        c.apiUrl     = apiUrl;
        c.enableThinking = enableThinking;
        c.state      = STATE_ENABLED;
        c.isDefault  = isDefault;
        if (isDefault) c.signals.add(DomainSignal.DEFAULT_CHANGED);
        return c;
    }

    public static AiConfig newConfig(Integer userId, ConfigBO bo) {
        return newConfig(userId, bo.getConfigType(), bo.getProvider(),
                bo.getConfigName(), bo.getConfigDesc(), bo.getModelType(),
                bo.getAppId(), bo.getApiKey(), bo.getApiSecret(),
                bo.getAk(), bo.getSk(), bo.getApiUrl(),
                bo.getEnableThinking(), "1".equals(bo.getIsDefault()));
    }

    /** Reconstrói a raiz de agregação a partir da camada de persistência (uso exclusivo do Repository, não gera sinais). */
    public static AiConfig reconstitute(Integer configId, Integer userId,
                                        String configType, String provider,
                                        String configName, String configDesc, String modelType,
                                        String appId, String apiKey, String apiSecret,
                                        String ak, String sk, String apiUrl,
                                        Boolean enableThinking,
                                        String state, boolean isDefault,
                                        LocalDateTime createTime, LocalDateTime updateTime) {
        AiConfig c = new AiConfig();
        c.configId   = configId;
        c.userId     = userId;
        c.configType = configType;
        c.provider   = provider;
        c.configName = configName;
        c.configDesc = configDesc;
        c.modelType  = modelType;
        c.appId      = appId;
        c.apiKey     = apiKey;
        c.apiSecret  = apiSecret;
        c.ak         = ak;
        c.sk         = sk;
        c.apiUrl     = apiUrl;
        c.enableThinking = enableThinking;
        c.state      = state;
        c.isDefault  = isDefault;
        c.createTime = createTime;
        c.updateTime = updateTime;
        return c;
    }

    // ── Métodos de comportamento ────────────────────────────────────────────

    /** Define esta configuração como padrão (Repository.save é responsável por limpar as outras marcações padrão do mesmo tipo). */
    public void setAsDefault() {
        if (!this.isDefault) {
            this.isDefault = true;
            signals.add(DomainSignal.DEFAULT_CHANGED);
        }
    }

    /** Chamado pelo Repository no fluxo de resetDefault, não gera sinais. */
    public void clearDefault() {
        this.isDefault = false;
    }

    public void update(ConfigBO bo) {
        update(bo.getConfigName(), bo.getConfigDesc(), bo.getModelType(), bo.getProvider(),
                bo.getAppId(), bo.getApiKey(), bo.getApiSecret(), bo.getAk(), bo.getSk(),
                bo.getApiUrl(), bo.getEnableThinking(),
                bo.getIsDefault() == null ? null : "1".equals(bo.getIsDefault()));
    }

    public void update(String configName, String configDesc, String modelType, String provider,
                       String appId, String apiKey, String apiSecret, String ak, String sk,
                       String apiUrl, Boolean enableThinking, Boolean isDefault) {
        if (configName != null) this.configName = configName;
        if (configDesc != null) this.configDesc = configDesc;
        if (modelType  != null) this.modelType  = modelType;
        if (provider   != null) this.provider   = provider;
        if (appId      != null) this.appId      = appId;
        if (apiKey     != null) this.apiKey     = apiKey;
        if (apiSecret  != null) this.apiSecret  = apiSecret;
        if (ak         != null) this.ak         = ak;
        if (sk         != null) this.sk         = sk;
        if (apiUrl     != null) this.apiUrl     = apiUrl;
        if (enableThinking != null) this.enableThinking = enableThinking;
        if (isDefault  != null) {
            if (isDefault && !this.isDefault) {
                this.isDefault = true;
                signals.add(DomainSignal.DEFAULT_CHANGED);
            } else if (!isDefault) {
                this.isDefault = false;
            }
        }
        signals.add(DomainSignal.UPDATED);
    }

    /** Exclusão lógica: desabilita e remove a marcação de padrão. */
    public void disable() {
        this.state     = STATE_DISABLED;
        this.isDefault = false;
        signals.add(DomainSignal.DISABLED);
    }

    /** Preenchido pelo Repository após o insert com a chave primária autoincrementada, não gera sinais. */
    public void assignId(Integer configId) {
        this.configId = configId;
    }

    /** Extrai e limpa a fila de sinais, para o Repository publicar eventos de domínio. */
    public List<DomainSignal> pullSignals() {
        List<DomainSignal> s = List.copyOf(signals);
        signals.clear();
        return s;
    }
}
