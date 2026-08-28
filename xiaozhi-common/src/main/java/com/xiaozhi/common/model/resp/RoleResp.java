package com.xiaozhi.common.model.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Informações do papel")
public class RoleResp {

    @Schema(description = "ID do papel")
    private Integer roleId;

    @Schema(description = "Nome do papel")
    private String roleName;

    @Schema(description = "Descrição do papel")
    private String roleDesc;

    @Schema(description = "Avatar do papel")
    private String avatar;

    @Schema(description = "Nome da voz")
    private String voiceName;

    @Schema(description = "Tom de voz")
    private Double ttsPitch;

    @Schema(description = "Velocidade da fala")
    private Double ttsSpeed;

    @Schema(description = "Status (1 habilitado, 0 desabilitado)")
    private String state;

    @Schema(description = "ID do serviço TTS")
    private Integer ttsId;

    @Schema(description = "ID do modelo")
    private Integer modelId;

    @Schema(description = "Nome do modelo")
    private String modelName;

    @Schema(description = "ID do serviço STT")
    private Integer sttId;

    @Schema(description = "Parâmetro de temperatura")
    private Double temperature;

    @Schema(description = "Parâmetro Top-P")
    private Double topP;

    @Schema(description = "Detecção de atividade de voz - limiar de energia")
    private Float vadEnergyTh;

    @Schema(description = "Detecção de atividade de voz - limiar de voz")
    private Float vadSpeechTh;

    @Schema(description = "Detecção de atividade de voz - limiar de silêncio")
    private Float vadSilenceTh;

    @Schema(description = "Detecção de atividade de voz - milissegundos de silêncio")
    private Integer vadSilenceMs;

    @Schema(description = "Provedor do modelo")
    private String modelProvider;

    @Schema(description = "Provedor do serviço TTS")
    private String ttsProvider;

    @Schema(description = "Se é o papel padrão (1 sim, 0 não)")
    private String isDefault;

    @Schema(description = "Total de dispositivos")
    private Integer totalDevice;

    @Schema(description = "Tipo de memória")
    private String memoryType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Data de criação")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Data de atualização")
    private LocalDateTime updateTime;
}
