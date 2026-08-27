package com.xiaozhi.device;

import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.RoleBO;
import com.xiaozhi.common.model.bo.VerifyCodeBO;
import com.xiaozhi.common.model.req.DeviceBatchUpdateReq;
import com.xiaozhi.common.model.req.DeviceCreateReq;
import com.xiaozhi.common.model.req.DevicePageReq;
import com.xiaozhi.common.model.req.DeviceUpdateReq;
import com.xiaozhi.common.model.req.OtaReq;
import com.xiaozhi.common.model.resp.DeviceResp;
import com.xiaozhi.common.model.resp.PageResp;
import com.xiaozhi.communication.ServerAddressProvider;
import com.xiaozhi.communication.registry.DialogueServerInfo;
import com.xiaozhi.communication.registry.DialogueServerRegistry;
import com.xiaozhi.device.convert.DeviceConvert;
import com.xiaozhi.device.domain.Device;
import com.xiaozhi.device.domain.repository.DeviceRepository;
import com.xiaozhi.device.domain.vo.VerifyCode;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.role.service.RoleService;
import com.xiaozhi.utils.CmsUtils;
import com.xiaozhi.utils.CommonUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
/**
 * Serviço de aplicação do domínio de dispositivos.
 * <p>
 * Responsabilidade: orquestra o fluxo entre o Controller e o Domain Service, incluindo:
 * <ul>
 *   <li>Conversão Req/Resp ↔ BO</li>
 *   <li>Validações entre domínios (verificação de posse do papel)</li>
 *   <li>Coordenação de efeitos colaterais (broadcast de mudança de sessão do dispositivo e troca de papel via Redis)</li>
 * </ul>
 */
@Slf4j
@Service
public class DeviceAppService {

    @Resource
    private DeviceService deviceService;

    @Resource
    private DeviceRepository deviceRepository;

    @Resource
    private DeviceConvert deviceConvert;

    @Resource
    private RoleService roleService;

    @Resource
    private ServerAddressProvider serverAddressProvider;

    @Resource
    private DialogueServerRegistry dialogueServerRegistry;


    public PageResp<DeviceResp> page(DevicePageReq req, Integer userId) {
        DevicePageReq r = req == null ? new DevicePageReq() : req;
        return deviceService.page(r.getPageNo(), r.getPageSize(),
            r.getDeviceId(), r.getDeviceName(), r.getRoleName(),
            r.getState(), r.getRoleId(), userId);
    }

    @Transactional
    public DeviceResp create(DeviceCreateReq req, Integer userId) {
        VerifyCode verifyCode = deviceRepository.findVerifyCode(req.getCode(), null, null)
                .orElseThrow(() -> new IllegalArgumentException("Código de verificação inválido"));

        if (!StringUtils.hasText(verifyCode.deviceId())) {
            throw new IllegalArgumentException("Código de verificação inválido");
        }

        // Dispositivo já existe: retorno idempotente (mesmo usuário) ou lança conflito
        java.util.Optional<Device> existingDevice = deviceRepository.findById(verifyCode.deviceId());
        if (existingDevice.isPresent()) {
            Device d = existingDevice.get();
            if (userId != null && userId.equals(d.getUserId())) {
                DeviceResp result = deviceService.get(d.getDeviceId());
                if (result == null) throw new IllegalStateException("Falha ao consultar dispositivo");
                return result;
            }
            throw new IllegalStateException("O dispositivo já está vinculado a outro usuário");
        }

        RoleBO selectedRole = roleService.getDefaultOrFirstBO(userId);
        if (selectedRole == null) {
            throw new IllegalStateException("Nenhum papel configurado");
        }

        String name = StringUtils.hasText(verifyCode.type()) ? verifyCode.type() : "Xiaozhi";
        Device device = Device.newDevice(verifyCode.deviceId(), name, verifyCode.type(),
                userId, selectedRole.getRoleId());
        deviceRepository.save(device);

        DeviceResp result = deviceService.get(device.getDeviceId());
        if (result == null) throw new IllegalStateException("Falha ao adicionar dispositivo");
        return result;
    }

    @Transactional
    public DeviceResp update(String deviceId, DeviceUpdateReq req) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispositivo não encontrado ou sem permissão de acesso"));

        if (req.getRoleId() != null) {
            RoleBO role = roleService.getBO(req.getRoleId());
            if (role == null) throw new IllegalArgumentException("Papel não encontrado ou sem permissão de acesso");
            if (!Objects.equals(role.getUserId(), device.getUserId()))
                throw new IllegalArgumentException("O papel não pertence ao usuário dono do dispositivo");
        }

        device.update(req.getDeviceName(), req.getRoleId(), req.getLocation());
        deviceRepository.save(device);

        DeviceResp result = deviceService.get(deviceId);
        if (result == null) throw new IllegalStateException("Falha ao atualizar dispositivo");
        return result;
    }

    @Transactional
    public Map<String, Object> batchUpdate(DeviceBatchUpdateReq req) {
        if (!StringUtils.hasText(req.getDeviceIds()) || req.getRoleId() == null) {
            throw new IllegalArgumentException("Falha ao atualizar, verifique se o ID do dispositivo está correto");
        }
        if (roleService.getBO(req.getRoleId()) == null) {
            throw new IllegalArgumentException("Papel não encontrado ou sem permissão de acesso");
        }

        int successCount = 0;
        for (String rawDeviceId : Arrays.asList(req.getDeviceIds().split(","))) {
            String deviceId = rawDeviceId.trim();
            if (!StringUtils.hasText(deviceId)) {
                continue;
            }
            deviceRepository.findById(deviceId).ifPresent(device -> {
                device.bindRole(req.getRoleId());
                deviceRepository.save(device);
            });
            successCount++;
        }
        if (successCount <= 0) {
            throw new IllegalArgumentException("Falha ao atualizar, verifique se o ID do dispositivo está correto");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("successCount", successCount);
        data.put("totalCount", req.getDeviceIds().split(",").length);
        return data;
    }

    public DeviceResp getResp(String deviceId) {
        return deviceService.get(deviceId);
    }

    public DeviceResp generateCode(String deviceId, String sessionId, String type) {
        VerifyCodeBO codeBO = deviceService.generateCode(deviceId, sessionId, type);
        return codeBO == null ? null : deviceConvert.toResp(codeBO);
    }

    public int sync(DeviceBO syncData) {
        if (syncData == null || !StringUtils.hasText(syncData.getDeviceId())) {
            return 0;
        }
        return deviceRepository.findById(syncData.getDeviceId()).map(device -> {
            device.sync(syncData.getDeviceName(), syncData.getWifiName(),
                    syncData.getChipModelName(), syncData.getType(),
                    syncData.getVersion(), syncData.getIp(), syncData.getLocation());
            deviceRepository.save(device);
            return 1;
        }).orElse(0);
    }

    @Transactional
    public void delete(String deviceId) {
        if (deviceRepository.findById(deviceId).isEmpty()) {
            throw new ResourceNotFoundException("Dispositivo não encontrado ou sem permissão de acesso");
        }
        deviceRepository.delete(deviceId);
    }

    /**
     * Lógica de negócio principal para processar a solicitação OTA.
     *
     * @param req informações do dispositivo extraídas da requisição HTTP pelo Controller
     * @return dados de resposta OTA (firmware / activation / websocket, etc.)
     * @throws IllegalArgumentException ID do dispositivo incorreto
     * @throws IllegalStateException    erro interno, como falha ao gerar o código de verificação
     */
    public Map<String, Object> handleOta(OtaReq req) {
        // --- Resolução de geolocalização por IP ---
        if (StringUtils.hasText(req.getIp())) {
            var ipInfo = CmsUtils.getIPInfoByAddress(req.getIp());
            if (ipInfo != null && StringUtils.hasText(ipInfo.getLocation())) {
                req.setLocation(ipInfo.getLocation());
            }
        }

        if (!StringUtils.hasText(req.getDeviceId()) || !CommonUtils.isMacAddressValid(req.getDeviceId())) {
            throw new IllegalArgumentException("ID do dispositivo incorreto");
        }

        String deviceId = req.getDeviceId();
        DeviceResp boundDevice = getResp(deviceId);
        Map<String, Object> otaResponse = new HashMap<>();

        // --- Informações de firmware ---
        Map<String, Object> firmwareInfo = new HashMap<>();
        firmwareInfo.put("url", serverAddressProvider.getOtaAddress());
        firmwareInfo.put("version", "1.0.0");
        otaResponse.put("firmware", firmwareInfo);
        otaResponse.put("server_time", Map.of(
            "timestamp", System.currentTimeMillis(),
            "timezone_offset", 480
        ));

        if (boundDevice == null) {
            // --- Dispositivo não vinculado: gera código de verificação ---
            DeviceResp codeResult = generateCode(deviceId, null, req.getType());
            if (codeResult == null || !StringUtils.hasText(codeResult.getCode())) {
                throw new IllegalStateException("Falha ao gerar código de verificação");
            }
            otaResponse.put("activation", Map.of(
                "code", codeResult.getCode(),
                "message", codeResult.getCode(),
                "challenge", deviceId
            ));
        } else {
            // --- Dispositivo já vinculado: retorna o endereço de comunicação ---
            DialogueServerInfo selectedServer = null;
            try {
                selectedServer = dialogueServerRegistry.selectServer();
            } catch (RuntimeException e) {
                log.warn("Falha ao selecionar o servidor de diálogo, revertendo para o endereço padrão, deviceId={}", deviceId, e);
            }
            String websocketAddress = selectedServer != null ? selectedServer.getWebsocketAddress() : serverAddressProvider.getWebsocketAddress();

            Map<String, Object> websocketData = new HashMap<>();
            websocketData.put("url", websocketAddress);
            websocketData.put("token", "");
            otaResponse.put("websocket", websocketData);

            // --- Sincroniza informações do dispositivo ---
            DeviceBO syncData = new DeviceBO();
            syncData.setDeviceId(boundDevice.getDeviceId());
            syncData.setDeviceName(boundDevice.getDeviceName());
            syncData.setIp(req.getIp());
            syncData.setLocation(req.getLocation());
            syncData.setWifiName(req.getWifiName());
            syncData.setChipModelName(req.getChipModelName());
            syncData.setType(req.getType());
            syncData.setVersion(req.getVersion());
            try {
                sync(syncData);
            } catch (RuntimeException e) {
                log.warn("Falha ao sincronizar informações do dispositivo (não afeta a resposta OTA), deviceId={}", deviceId, e);
            }
        }

        return otaResponse;
    }

    /**
     * Verifica o status de ativação OTA.
     *
     * @return true se o dispositivo estiver ativado, false se não estiver ativado ou o ID for inválido
     */
    public boolean checkOtaActivation(String deviceId) {
        if (!StringUtils.hasText(deviceId) || !CommonUtils.isMacAddressValid(deviceId)) {
            return false;
        }
        DeviceResp device = getResp(deviceId);
        if (device == null) {
            return false;
        }
        log.info("Consulta do resultado de ativação OTA concluída, deviceId: {} horário de ativação: {}", deviceId, device.getCreateTime());
        return true;
    }
}
