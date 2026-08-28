package com.xiaozhi.device.domain.repository;

import com.xiaozhi.device.domain.Device;
import com.xiaozhi.device.domain.vo.VerifyCode;

import java.util.Optional;
import java.util.Set;

/**
 * Interface de repositório da raiz de agregação Device (definida na camada de domínio, implementada na infraestrutura).
 */
public interface DeviceRepository {

    /** Carrega a raiz de agregação pelo ID do dispositivo */
    Optional<Device> findById(String deviceId);

    /** Consulta pelo código de verificação (cenário de ativação do dispositivo) */
    Optional<VerifyCode> findVerifyCode(String code, String deviceId, String sessionId);

    /**
     * Persiste a raiz de agregação (criação ou atualização).
     * <p>A implementação deve chamar {@link Device#pullSignals()} após salvar e publicar o ApplicationEvent correspondente.
     */
    void save(Device device);

    /** Remove o dispositivo e limpa o cache */
    void delete(String deviceId);

    /**
     * Atualiza diretamente o estado do dispositivo (caminho crítico).
     * <p>Não carrega a raiz de agregação completa; executa diretamente UPDATE + invalidação de cache, sem eventos de domínio.
     */
    void updateState(String deviceId, String state);

    /**
     * Redefine o estado de dispositivos em lote (caminho crítico, ex.: colocar em lote como offline ao reiniciar a instância).
     */
    int batchUpdateState(Set<String> deviceIds, String state);
}
