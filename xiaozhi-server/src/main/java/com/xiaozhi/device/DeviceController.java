package com.xiaozhi.device;

import com.xiaozhi.server.web.BaseController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.Map;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.annotation.CheckOwner;
import com.xiaozhi.common.model.req.DeviceBatchUpdateReq;
import com.xiaozhi.common.model.req.DeviceCreateReq;
import com.xiaozhi.common.model.req.DevicePageReq;
import com.xiaozhi.common.model.req.DeviceUpdateReq;
import com.xiaozhi.common.model.req.OtaReq;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.utils.JsonUtil;
import com.xiaozhi.utils.RequestContextUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Gerenciamento de dispositivos
 * 
 * @author Joey
 * 
 */

@Slf4j
@RestController
@RequestMapping("/api/device")
@Tag(name = "Gerenciamento de dispositivos", description = "Operações relacionadas a dispositivos")
public class DeviceController extends BaseController {

    @Resource
    private DeviceAppService deviceAppService;

    /**
     * Consulta de dispositivos
     */
    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:device:api:list")
    @Operation(summary = "Consulta dispositivos de acordo com os filtros", description = "Retorna a lista de dispositivos")
    public ApiResponse<?> list(@Valid DevicePageReq req) {
        return ApiResponse.success(deviceAppService.page(req, StpUtil.getLoginIdAsInt()));
    }

    /**
     * Atualizar dispositivos em lote
     */
    @PostMapping("/batchUpdate")
    @ResponseBody
    @SaCheckPermission("system:device:api:batch-update")
    @AuditLog(module = "Gerenciamento de dispositivos", operation = "Atualizar dispositivos em lote")
    @CheckOwner(resource = "device", id = "#param.deviceIds != null ? #param.deviceIds.split(',') : null")
    @CheckOwner(resource = "role", id = "#param.roleId")
    @Operation(summary = "Atualizar dispositivos em lote", description = "Atualiza em lote o papel de vários dispositivos")
    public ApiResponse<?> batchUpdate(@Valid @RequestBody DeviceBatchUpdateReq param) {
        Map<String, Object> data = deviceAppService.batchUpdate(param);
        return ApiResponse.success("Atualização concluída, total de " + data.get("successCount") + " dispositivo(s) atualizado(s)", data);
    }

    /**
     * Adicionar dispositivo
     */
    @PostMapping("")
    @ResponseBody
    @SaCheckPermission("system:device:api:create")
    @AuditLog(module = "Gerenciamento de dispositivos", operation = "Criar dispositivo")
    @Operation(summary = "Adicionar dispositivo", description = "Adiciona o dispositivo à conta do usuário atual usando o código de verificação do dispositivo")
    public ApiResponse<?> create(@Valid @RequestBody DeviceCreateReq param) {
        return ApiResponse.success(deviceAppService.create(param, StpUtil.getLoginIdAsInt()));
    }

    /**
     * Atualização das informações do dispositivo
     */
    @PutMapping("/{deviceId}")
    @ResponseBody
    @SaCheckPermission("system:device:api:update")
    @CheckOwner(resource = "device", id = "#deviceId")
    @CheckOwner(resource = "role", id = "#param.roleId")
    @AuditLog(module = "Gerenciamento de dispositivos", operation = "Atualizar dispositivo")
    @Operation(summary = "Atualizar informações do dispositivo", description = "Atualiza nome, papel, lista de funcionalidades e outras informações do dispositivo")
    public ApiResponse<?> update(@PathVariable String deviceId, @Valid @RequestBody DeviceUpdateReq param) {
        return ApiResponse.success(deviceAppService.update(deviceId, param));
    }

    /**
     * Excluir dispositivo
     */
    @DeleteMapping("/{deviceId}")
    @ResponseBody
    @SaCheckPermission("system:device:api:delete")
    @CheckOwner(resource = "device", id = "#deviceId")
    @AuditLog(module = "Gerenciamento de dispositivos", operation = "Excluir dispositivo")
    @Operation(summary = "Excluir dispositivo", description = "Remove o dispositivo informado da conta do usuário atual")
    public ApiResponse<?> delete(@PathVariable String deviceId) {
        deviceAppService.delete(deviceId);
        return ApiResponse.success("Excluído com sucesso");
    }

    @SaIgnore
    @RequestMapping(value = "/ota", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    @Operation(summary = "Processar solicitação OTA", description = "Retorna o resultado OTA")
    public ResponseEntity<byte[]> ota(
        @Parameter(description = "ID do dispositivo") @RequestHeader(value = "Device-Id", required = false) String deviceIdHeader,
        @RequestBody(required = false) String requestBody,
        HttpServletRequest request) {
        try {
            OtaReq otaReq = parseOtaRequest(deviceIdHeader, requestBody, request);
            Map<String, Object> otaResponse = deviceAppService.handleOta(otaReq);
            return buildJsonResponse(HttpStatus.OK, otaResponse);
        } catch (IllegalArgumentException e) {
            return buildJsonResponse(HttpStatus.BAD_REQUEST, Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Falha ao processar solicitação OTA", e);
            return buildJsonResponse(HttpStatus.INTERNAL_SERVER_ERROR, Map.of("error", "Falha ao processar a solicitação"));
        }
    }

    @SaIgnore
    @PostMapping("/ota/activate")
    @ResponseBody
    @Operation(summary = "Consultar status de ativação OTA", description = "Retorna o status de ativação OTA")
    public ResponseEntity<String> otaActivate(
        @Parameter(name = "Device-Id", description = "Identificador único do dispositivo", in = ParameterIn.HEADER)
        @RequestHeader(value = "Device-Id", required = false) String deviceId) {
        try {
            return deviceAppService.checkOtaActivation(deviceId)
                    ? ResponseEntity.ok("success")
                    : ResponseEntity.status(202).build();
        } catch (RuntimeException e) {
            log.error("Falha na ativação OTA", e);
            return ResponseEntity.status(202).build();
        }
    }

    /**
     * Extrai da requisição HTTP as informações do dispositivo necessárias para a OTA.
     */
    private OtaReq parseOtaRequest(String deviceIdHeader, String requestBody, HttpServletRequest request) {
        OtaReq req = new OtaReq();
        Map<String, Object> jsonData = Map.of();
        if (StringUtils.isNotBlank(requestBody)) {
            try {
                jsonData = JsonUtil.OBJECT_MAPPER.readValue(requestBody, new TypeReference<>() {});
            } catch (IOException e) {
                log.debug("Falha ao analisar JSON: {}", e.getMessage());
            }
        }

        // --- ID do dispositivo: prioriza o Header, depois o Body ---
        if (StringUtils.isNotBlank(deviceIdHeader)) {
            req.setDeviceId(deviceIdHeader);
        } else {
            Object macAddress = jsonData.get("mac_address");
            if (macAddress instanceof String mac && StringUtils.isNotBlank(mac)) {
                req.setDeviceId(mac);
            } else if (jsonData.get("mac") instanceof String mac) {
                req.setDeviceId(mac);
            }
        }

        // --- Informações de hardware / rede ---
        if (jsonData.get("chip_model_name") instanceof String chipModel) {
            req.setChipModelName(chipModel);
        }
        if (jsonData.get("application") instanceof Map<?, ?> application
            && application.get("version") instanceof String version) {
            req.setVersion(version);
        }
        if (jsonData.get("board") instanceof Map<?, ?> board) {
            if (board.get("ssid") instanceof String wifiName) {
                req.setWifiName(wifiName);
            }
            if (board.get("type") instanceof String deviceType) {
                req.setType(deviceType);
            }
        }

        req.setIp(RequestContextUtils.getClientIp(request));
        return req;
    }

    private ResponseEntity<byte[]> buildJsonResponse(HttpStatus status, Object responseData) {
        try {
            byte[] responseBytes = JsonUtil.OBJECT_MAPPER.writeValueAsBytes(responseData);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentLength(responseBytes.length);
            return new ResponseEntity<>(responseBytes, headers, status);
        } catch (JsonProcessingException e) {
            log.error("Falha ao serializar a resposta OTA", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
