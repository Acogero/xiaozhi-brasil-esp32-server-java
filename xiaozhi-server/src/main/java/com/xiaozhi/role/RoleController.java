package com.xiaozhi.role;

import com.xiaozhi.server.web.BaseController;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.annotation.CheckOwner;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.req.RoleCreateReq;
import com.xiaozhi.common.model.req.RolePageReq;
import com.xiaozhi.common.model.req.RoleUpdateReq;
import com.xiaozhi.common.model.req.TestVoiceReq;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.ai.tts.TtsServiceFactory;
import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.config.service.ConfigService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

/**
 * Gerenciamento de papéis
 * 
 * @author Joey
 * 
 */

@Slf4j
@RestController
@RequestMapping("/api/role")
@Tag(name = "Gerenciamento de papéis", description = "Operações relacionadas a papéis")
public class RoleController extends BaseController {

    @Resource
    private RoleAppService roleAppService;

    @Resource
    private SherpaVoiceService sherpaVoiceService;

    @Resource
    private TtsServiceFactory ttsService;

    @Resource
    private ConfigService configService;

    /**
     * Consulta de papéis
     *
     * @param req
     * @return roleList
     */
    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:role:api:list")
    @Operation(summary = "Consulta informações de papéis de acordo com os filtros", description = "Retorna a lista de papéis")
    public ApiResponse<?> list(@Valid RolePageReq req) {
        return ApiResponse.success(roleAppService.page(req, StpUtil.getLoginIdAsInt()));
    }

    /**
     * Atualização das informações do papel
     *
     * @param roleId ID do papel
     * @param param parâmetros de atualização
     * @return
     */
    @PutMapping("/{roleId}")
    @ResponseBody
    @SaCheckPermission("system:role:api:update")
    @CheckOwner(resource = "role", id = "#roleId")
    @AuditLog(module = "Gerenciamento de papéis", operation = "Atualizar papel")
    @CheckOwner(resource = "config", id = "#param.modelId")
    @CheckOwner(resource = "config", id = "#param.sttId != null && #param.sttId > 0 ? #param.sttId : null")
    @CheckOwner(resource = "config", id = "#param.ttsId != null && #param.ttsId > 0 ? #param.ttsId : null")
    @Operation(summary = "Atualizar informações do papel", description = "Atualizar configuração do papel do assistente de voz")
    public ApiResponse<?> update(@PathVariable Integer roleId, @Valid @RequestBody RoleUpdateReq param) {
        return ApiResponse.success(roleAppService.update(roleId, param));
    }

    /**
     * Adicionar papel
     *
     * @param param parâmetros de criação
     */
    @PostMapping("")
    @ResponseBody
    @SaCheckPermission("system:role:api:create")
    @AuditLog(module = "Gerenciamento de papéis", operation = "Criar papel")
    @CheckOwner(resource = "config", id = "#param.modelId")
    @CheckOwner(resource = "config", id = "#param.sttId != null && #param.sttId > 0 ? #param.sttId : null")
    @CheckOwner(resource = "config", id = "#param.ttsId != null && #param.ttsId > 0 ? #param.ttsId : null")
    @Operation(summary = "Adicionar informações do papel", description = "Adiciona um novo papel de assistente de voz")
    public ApiResponse<?> create(@Valid @RequestBody RoleCreateReq param) {
        return ApiResponse.success(roleAppService.create(param, StpUtil.getLoginIdAsInt()));
    }

    /**
     * Excluir papel
     *
     * @param roleId ID do papel
     * @return
     */
    @DeleteMapping("/{roleId}")
    @ResponseBody
    @SaCheckPermission("system:role:api:delete")
    @CheckOwner(resource = "role", id = "#roleId")
    @AuditLog(module = "Gerenciamento de papéis", operation = "Excluir papel")
    @Operation(summary = "Excluir informações do papel", description = "Exclui o papel do assistente de voz informado")
    public ApiResponse<?> delete(@PathVariable Integer roleId) {
        roleAppService.delete(roleId);
        return ApiResponse.success("Excluído com sucesso");
    }

    /**
     * Varre o diretório local dos modelos de TTS configurados e retorna dinamicamente a lista de vozes sherpa-onnx disponíveis
     */
    @GetMapping("/sherpaVoices")
    @ResponseBody
    @SaCheckPermission("system:role:api:list")
    @Operation(summary = "Obtém a lista de vozes locais do sherpa-onnx", description = "Varre o diretório local dos modelos de TTS configurados, identificando automaticamente o tipo de modelo e o speaker")
    public ApiResponse<?> listSherpaVoices() {
        return ApiResponse.success(sherpaVoiceService.listVoices());
    }

    @GetMapping("/testVoice")
    @ResponseBody
    @SaCheckPermission("system:role:api:list")
    @CheckOwner(resource = "config", id = "#param.provider != 'edge' ? #param.ttsId : null")
    @Operation(summary = "Testar síntese de voz", description = "Testa o resultado da síntese de voz da configuração informada")
    public ApiResponse<?> testAudio(@Valid TestVoiceReq param) {
        ConfigBO config = null;
        if (!param.getProvider().equals("edge")) {
            if (param.getTtsId() == null) {
                throw new IllegalArgumentException("Provedores que não sejam "edge" precisam ter uma configuração de voz definida");
            }
            config = configService.getBO(param.getTtsId());
            if (config == null) {
                throw new ResourceNotFoundException("Configuração de voz não encontrada ou sem permissão de acesso");
            }
        }

        try {
            Path audioFilePath = ttsService.getTtsService(config, param.getVoiceName(), param.getTtsPitch(), param.getTtsSpeed())
                    .textToSpeech(param.getMessage());

            return ApiResponse.success("Operação realizada com sucesso", audioFilePath != null ? audioFilePath.toString() : null);
        } catch (IndexOutOfBoundsException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException("Configure a chave correspondente na página de configuração de síntese de voz", e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new OperationFailedException("Falha ao testar a síntese de voz", e);
        }
    }
}
