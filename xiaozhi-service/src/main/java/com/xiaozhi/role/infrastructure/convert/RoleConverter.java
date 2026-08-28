package com.xiaozhi.role.infrastructure.convert;

import com.xiaozhi.common.model.bo.RoleBO;
import com.xiaozhi.role.dal.mysql.dataobject.RoleDO;
import com.xiaozhi.role.domain.Role;
import com.xiaozhi.role.domain.vo.AudioConfig;
import com.xiaozhi.role.domain.vo.LlmConfig;
import com.xiaozhi.role.domain.vo.MemoryStrategy;
import com.xiaozhi.role.domain.vo.VoiceConfig;
import org.springframework.stereotype.Component;

/**
 * Conversor Role raiz de agregação ↔ DO / BO.
 * <p>
 * Responsável por reconstruir o {@link RoleDO} "achatado" na raiz de agregação {@link Role} com objetos de valor,
 * responsabilidade diferente da MapStruct {@code RoleConvert} (DO ↔ BO); não confundir.
 */
@Component
public class RoleConverter {

    /** RoleDO → Role raiz de agregação (reconstrução a partir da camada de persistência) */
    public Role toDomain(RoleDO d) {
        LlmConfig llm = new LlmConfig(d.getModelId(),
                d.getTemperature() != null ? d.getTemperature() : 0.7d,
                d.getTopP() != null ? d.getTopP() : 0.9d);

        VoiceConfig voice = new VoiceConfig(d.getTtsId(), d.getSttId(), d.getVoiceName(),
                d.getTtsPitch() != null ? d.getTtsPitch() : 1.0,
                d.getTtsSpeed() != null ? d.getTtsSpeed() : 1.0);

        AudioConfig audio = new AudioConfig(d.getVadEnergyTh(), d.getVadSpeechTh(),
                d.getVadSilenceTh(), d.getVadSilenceMs());

        MemoryStrategy memory = new MemoryStrategy(d.getMemoryType());

        return new Role(
                d.getRoleId(), d.getUserId(), d.getAvatar(),
                d.getRoleName(), d.getRoleDesc(), d.getState(),
                "1".equals(d.getIsDefault()),
                llm, voice, audio, memory,
                d.getCreateTime(), d.getUpdateTime()
        );
    }

    /** Role raiz de agregação → RoleDO (gravação na camada de persistência) */
    public RoleDO toDataObject(Role r) {
        RoleDO d = new RoleDO();
        d.setRoleId(r.getRoleId());
        d.setUserId(r.getUserId());
        d.setAvatar(r.getAvatar());
        d.setRoleName(r.getRoleName());
        d.setRoleDesc(r.getRoleDesc());
        d.setState(r.getState());
        d.setIsDefault(r.isDefault() ? "1" : "0");

        LlmConfig llm = r.getLlmConfig();
        if (llm != null) {
            d.setModelId(llm.modelId());
            d.setTemperature(llm.temperature());
            d.setTopP(llm.topP());
        }

        VoiceConfig voice = r.getVoiceConfig();
        if (voice != null) {
            d.setTtsId(voice.ttsId());
            d.setSttId(voice.sttId());
            d.setVoiceName(voice.voiceName());
            d.setTtsPitch(voice.ttsPitch());
            d.setTtsSpeed(voice.ttsSpeed());
        }

        AudioConfig audio = r.getAudioConfig();
        if (audio != null) {
            d.setVadEnergyTh(audio.vadEnergyTh());
            d.setVadSpeechTh(audio.vadSpeechTh());
            d.setVadSilenceTh(audio.vadSilenceTh());
            d.setVadSilenceMs(audio.vadSilenceMs());
        }

        MemoryStrategy memory = r.getMemoryStrategy();
        if (memory != null) {
            d.setMemoryType(memory.type());
        }

        return d;
    }

    /** Role raiz de agregação → RoleBO (usado pela interface Service existente e pela publicação de eventos) */
    public RoleBO toBO(Role r) {
        RoleBO bo = new RoleBO();
        bo.setRoleId(r.getRoleId());
        bo.setUserId(r.getUserId());
        bo.setAvatar(r.getAvatar());
        bo.setRoleName(r.getRoleName());
        bo.setRoleDesc(r.getRoleDesc());
        bo.setState(r.getState());
        bo.setIsDefault(r.isDefault() ? "1" : "0");

        LlmConfig llm = r.getLlmConfig();
        if (llm != null) {
            bo.setModelId(llm.modelId());
            bo.setTemperature(llm.temperature());
            bo.setTopP(llm.topP());
        }

        VoiceConfig voice = r.getVoiceConfig();
        if (voice != null) {
            bo.setTtsId(voice.ttsId());
            bo.setSttId(voice.sttId());
            bo.setVoiceName(voice.voiceName());
            bo.setTtsPitch(voice.ttsPitch());
            bo.setTtsSpeed(voice.ttsSpeed());
        }

        AudioConfig audio = r.getAudioConfig();
        if (audio != null) {
            bo.setVadEnergyTh(audio.vadEnergyTh());
            bo.setVadSpeechTh(audio.vadSpeechTh());
            bo.setVadSilenceTh(audio.vadSilenceTh());
            bo.setVadSilenceMs(audio.vadSilenceMs());
        }

        MemoryStrategy memory = r.getMemoryStrategy();
        if (memory != null) {
            bo.setMemoryType(memory.type());
        }

        return bo;
    }

    /** RoleBO → value objects (usado pelo AppService para construir os parâmetros de comportamento de update/create) */
    public LlmConfig toLlmConfig(RoleBO bo) {
        return new LlmConfig(bo.getModelId(), bo.getTemperature(), bo.getTopP());
    }

    public VoiceConfig toVoiceConfig(RoleBO bo) {
        return new VoiceConfig(bo.getTtsId(), bo.getSttId(), bo.getVoiceName(),
                bo.getTtsPitch(), bo.getTtsSpeed());
    }

    public AudioConfig toAudioConfig(RoleBO bo) {
        return new AudioConfig(bo.getVadEnergyTh(), bo.getVadSpeechTh(),
                bo.getVadSilenceTh(), bo.getVadSilenceMs());
    }

    public MemoryStrategy toMemoryStrategy(RoleBO bo) {
        return new MemoryStrategy(bo.getMemoryType());
    }
}
