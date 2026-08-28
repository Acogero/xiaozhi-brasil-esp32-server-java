package com.xiaozhi.role.domain;

import com.xiaozhi.role.domain.vo.AudioConfig;
import com.xiaozhi.role.domain.vo.LlmConfig;
import com.xiaozhi.role.domain.vo.MemoryStrategy;
import com.xiaozhi.role.domain.vo.VoiceConfig;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Role Raiz de agregação.
 * <p>
 * Responsabilidade: mantém a configuração do papel (LLM / voz / VAD / estratégia de memória),
 * altera o estado por meio de métodos de comportamento e coleta sinais de domínio para o Repository publicar eventos.
 */
@Getter
public class Role {

    /** Sinais de domínio */
    public enum DomainSignal { UPDATED }

    // --- Identity ---
    private Integer roleId;

    // --- Basic info ---
    private Integer userId;
    private String avatar;
    private String roleName;
    private String roleDesc;
    private String state;
    private boolean isDefault;

    // --- Value objects (grouping flat DB columns) ---
    private LlmConfig llmConfig;
    private VoiceConfig voiceConfig;
    private AudioConfig audioConfig;
    private MemoryStrategy memoryStrategy;

    // --- Timestamps ---
    private final LocalDateTime createTime;
    private LocalDateTime updateTime;

    private final List<DomainSignal> signals = new ArrayList<>();

    /** Reconstrói a raiz de agregação a partir da camada de persistência (uso exclusivo do Repository) */
    public Role(Integer roleId, Integer userId, String avatar, String roleName, String roleDesc,
                String state, boolean isDefault,
                LlmConfig llmConfig, VoiceConfig voiceConfig,
                AudioConfig audioConfig, MemoryStrategy memoryStrategy,
                LocalDateTime createTime, LocalDateTime updateTime) {
        this.roleId = roleId;
        this.userId = userId;
        this.avatar = avatar;
        this.roleName = roleName;
        this.roleDesc = roleDesc;
        this.state = state;
        this.isDefault = isDefault;
        this.llmConfig = llmConfig != null ? llmConfig : LlmConfig.defaults();
        this.voiceConfig = voiceConfig != null ? voiceConfig : VoiceConfig.defaults();
        this.audioConfig = audioConfig != null ? audioConfig : AudioConfig.defaults();
        this.memoryStrategy = memoryStrategy != null ? memoryStrategy : MemoryStrategy.defaults();
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    /** Método de fábrica: cria um novo papel */
    public static Role newRole(Integer userId, String roleName, String roleDesc, String avatar,
                               LlmConfig llmConfig, VoiceConfig voiceConfig,
                               AudioConfig audioConfig, MemoryStrategy memoryStrategy,
                               boolean isDefault) {
        Role role = new Role(null, userId, avatar, roleName, roleDesc, "1", isDefault,
                llmConfig, voiceConfig, audioConfig, memoryStrategy,
                null, null);
        role.signals.add(DomainSignal.UPDATED);
        return role;
    }

    // ===================== Métodos de comportamento =====================

    /** Define este papel como padrão (o Repository é responsável por redefinir os demais papéis do mesmo usuário) */
    public void setAsDefault() {
        this.isDefault = true;
        signals.add(DomainSignal.UPDATED);
    }

    /** Remove a marcação de padrão */
    public void clearDefault() {
        this.isDefault = false;
    }

    /** Atualiza os campos editáveis e os objetos de valor de configuração */
    public void update(String roleName, String roleDesc, String avatar,
                       LlmConfig llmConfig, VoiceConfig voiceConfig,
                       AudioConfig audioConfig, MemoryStrategy memoryStrategy,
                       Boolean isDefault) {
        if (roleName != null && !roleName.isBlank()) this.roleName = roleName;
        if (roleDesc != null) this.roleDesc = roleDesc;
        if (avatar != null) this.avatar = avatar;
        if (llmConfig != null) this.llmConfig = llmConfig;
        if (voiceConfig != null) this.voiceConfig = voiceConfig;
        if (audioConfig != null) this.audioConfig = audioConfig;
        if (memoryStrategy != null) this.memoryStrategy = memoryStrategy;
        if (isDefault != null) this.isDefault = isDefault;
        signals.add(DomainSignal.UPDATED);
    }

    /** void setRoleId — permitido apenas ao Repository para preencher o ID autoincrementado após o insert */
    public void assignId(Integer roleId) {
        if (this.roleId != null) throw new IllegalStateException("roleId já definido, não é permitido sobrescrever");
        this.roleId = roleId;
    }

    /** Extrai e limpa os sinais de domínio; chamado por Repository.save() */
    public List<DomainSignal> pullSignals() {
        List<DomainSignal> result = List.copyOf(signals);
        signals.clear();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role r)) return false;
        return Objects.equals(roleId, r.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId);
    }
}
