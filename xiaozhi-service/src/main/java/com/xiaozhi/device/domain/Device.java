package com.xiaozhi.device.domain;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Device Raiz de agregação.
 * <p>
 * Responsabilidade: mantém o estado do dispositivo, altera o estado por meio de métodos de comportamento (não setters) e coleta sinais de domínio.
 * Sem setter público; a mudança de estado deve ocorrer por meio de métodos de comportamento.
 */
@Getter
public class Device {

    /** Sinais de domínio: traduzidos pelo Repository.save() em publicação de Spring ApplicationEvent */
    public enum DomainSignal { UPDATED, ONLINE, ROLE_CHANGED, SESSION_CLOSED }

    /** Constantes de estado persistido do dispositivo */
    public static final String STATE_OFFLINE = "0";
    public static final String STATE_ONLINE  = "1";
    public static final String STATE_STANDBY = "2";

    // --- Identity ---
    private final String deviceId;

    // --- Core state ---
    private String deviceName;
    private Integer userId;
    private Integer roleId;
    private String mcpList;

    // --- Network / hardware info ---
    private String ip;
    private String location;
    private String wifiName;
    private String chipModelName;
    private String type;
    private String version;
    private String state;

    // --- Timestamps (read-only after creation) ---
    private final LocalDateTime createTime;
    private LocalDateTime updateTime;

    private final List<DomainSignal> signals = new ArrayList<>();

    /** Reconstrói a raiz de agregação a partir da camada de persistência (uso exclusivo do Repository) */
    public Device(String deviceId, String deviceName, Integer userId, Integer roleId,
           String mcpList, String ip, String location, String wifiName,
           String chipModelName, String type, String version, String state,
           LocalDateTime createTime, LocalDateTime updateTime) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.userId = userId;
        this.roleId = roleId;
        this.mcpList = mcpList;
        this.ip = ip;
        this.location = location;
        this.wifiName = wifiName;
        this.chipModelName = chipModelName;
        this.type = type;
        this.version = version;
        this.state = state;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    /** Método de fábrica: cria um dispositivo totalmente novo (primeira ativação) */
    public static Device newDevice(String deviceId, String deviceName, String type,
                                   Integer userId, Integer roleId) {
        Device device = new Device(
                deviceId,
                deviceName != null && !deviceName.isBlank() ? deviceName : (type != null ? type : "Xiaozhi"),
                userId, roleId, null, null, null, null, null, type,
                null, STATE_OFFLINE, null, null);
        device.signals.add(DomainSignal.UPDATED);
        device.signals.add(DomainSignal.SESSION_CLOSED);
        return device;
    }

    // ===================== Métodos de comportamento =====================

    /** Vincula o usuário (ativação do dispositivo) e define o papel padrão */
    public void bindUser(Integer userId, Integer roleId) {
        if (this.userId != null && !this.userId.equals(userId)) {
            throw new IllegalStateException("O dispositivo já está vinculado a outro usuário");
        }
        this.userId = userId;
        this.roleId = roleId;
        signals.add(DomainSignal.UPDATED);
    }

    /** Troca o papel associado */
    public void bindRole(Integer roleId) {
        if (!Objects.equals(this.roleId, roleId)) {
            this.roleId = roleId;
            signals.add(DomainSignal.UPDATED);
            signals.add(DomainSignal.ROLE_CHANGED);
        }
    }

    /** Atualiza os campos editáveis (a partir de DeviceUpdateReq) */
    public void update(String deviceName, Integer roleId, String location) {
        if (deviceName != null && !deviceName.isBlank()) this.deviceName = deviceName;
        if (roleId != null && !Objects.equals(this.roleId, roleId)) {
            this.roleId = roleId;
            signals.add(DomainSignal.ROLE_CHANGED);
        } else if (roleId != null) {
            this.roleId = roleId;
        }
        if (location != null && !location.isBlank()) this.location = location;
        signals.add(DomainSignal.UPDATED);
    }

    /** Sincroniza informações de rede quando o dispositivo entra online */
    public void reportOnline(String ip, String version, String wifiName, String location) {
        if (ip != null && !ip.isBlank()) this.ip = ip;
        if (version != null && !version.isBlank()) this.version = version;
        if (wifiName != null && !wifiName.isBlank()) this.wifiName = wifiName;
        if (location != null && !location.isBlank()) this.location = location;
        signals.add(DomainSignal.ONLINE);
    }

    /** Atualiza a lista de ferramentas MCP (reportada na conexão do dispositivo) */
    public void updateMcpList(String mcpList) {
        this.mcpList = mcpList;
        signals.add(DomainSignal.UPDATED);
    }

    /** Sincronização parcial de campos durante o relatório OTA */
    public void sync(String deviceName, String wifiName, String chipModelName,
                     String type, String version, String ip, String location) {
        if (deviceName != null && !deviceName.isBlank()) this.deviceName = deviceName;
        if (wifiName != null && !wifiName.isBlank()) this.wifiName = wifiName;
        if (chipModelName != null && !chipModelName.isBlank()) this.chipModelName = chipModelName;
        if (type != null && !type.isBlank()) this.type = type;
        if (version != null && !version.isBlank()) this.version = version;
        if (ip != null && !ip.isBlank()) this.ip = ip;
        if (location != null && !location.isBlank()) this.location = location;
        signals.add(DomainSignal.UPDATED);
    }

    /**
     * Extrai e limpa os sinais de domínio coletados.
     * <p>Chamado por Repository.save() após a conclusão da persistência, traduzido em Spring ApplicationEvent.
     */
    public List<DomainSignal> pullSignals() {
        List<DomainSignal> result = List.copyOf(signals);
        signals.clear();
        return result;
    }
}
