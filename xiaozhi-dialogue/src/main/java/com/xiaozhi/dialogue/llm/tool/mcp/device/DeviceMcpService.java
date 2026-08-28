package com.xiaozhi.dialogue.llm.tool.mcp.device;

import com.xiaozhi.communication.ServerAddressProvider;
import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.communication.common.SessionManager;
import com.xiaozhi.communication.domain.DeviceMcpMessage;
import com.xiaozhi.communication.domain.mcp.device.initialize.DeviceMcpClientInfo;
import com.xiaozhi.communication.domain.mcp.device.initialize.DeviceMcpInitialize;
import com.xiaozhi.communication.domain.mcp.device.initialize.DeviceMcpPayload;
import com.xiaozhi.communication.domain.mcp.device.initialize.DeviceMcpVision;
import com.xiaozhi.device.domain.repository.DeviceRepository;
import com.xiaozhi.ai.llm.tool.ToolCallStringResultConverter;
import com.xiaozhi.utils.JsonUtil;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DeviceMcpService {
    @Resource
    private Environment environment;

    @Resource
    private ServerAddressProvider serverAddressProvider;

    @Resource
    private DeviceRepository deviceRepository;

    @Resource
    private SessionManager sessionManager;

    @Value("${xiaozhi.mcp.device.max-tools-count:32}")
    private int maxToolsCount = 32;

    /**
     * Inicializa a lista de ferramentas MCP do dispositivo e persiste a lista de capacidades no banco de dados
     */
    public void initialize(ChatSession chatSession) {
        DeviceMcpMessage initResult = sendInitialize(chatSession);
        if (initResult != null) {
            chatSession.getDeviceMcpHolder().setMcpInitialized(true);
        }
        if (chatSession.getDeviceMcpHolder().isMcpInitialized()) {
            List<String> toolNames = sendToolsList(chatSession, null);
            persistMcpList(chatSession, toolNames);
        }
    }

    /**
     * Inicializa a lista de ferramentas MCP do dispositivo (incluindo ferramentas do usuário) e persiste a lista de capacidades no banco de dados
     */
    public void initializeWithUserTools(ChatSession chatSession) {
        DeviceMcpMessage initResult = sendInitialize(chatSession);
        if (initResult != null) {
            chatSession.getDeviceMcpHolder().setMcpInitialized(true);
        }
        if (chatSession.getDeviceMcpHolder().isMcpInitialized()) {
            List<String> toolNames = sendToolsList(chatSession, true);
            persistMcpList(chatSession, toolNames);
        }
    }

    /**
     * O servidor chama proativamente uma ferramenta MCP do dispositivo
     *
     * @param deviceId ID do dispositivo (o dispositivo deve estar online)
     * @param toolName nome original da ferramenta (ex.: "screenshot", "self.reboot")
     * @param args     parâmetros da ferramenta
     * @return o campo result da resposta MCP
     */
    public Map<String, Object> callDeviceTool(String deviceId, String toolName, Map<String, Object> args) {
        ChatSession chatSession = sessionManager.getSessionByDeviceId(deviceId);
        if (chatSession == null) {
            throw new IllegalStateException("Dispositivo offline ou não conectado: " + deviceId);
        }

        DeviceMcpMessage request = new DeviceMcpMessage();
        request.setSessionId(chatSession.getSessionId());
        DeviceMcpPayload payload = new DeviceMcpPayload();
        payload.setMethod("tools/call");
        payload.setId(chatSession.getDeviceMcpHolder().getMcpRequestId());
        payload.setParams(Map.of(
                "name", toolName,
                "arguments", args != null ? args : Map.of()
        ));
        request.setPayload(payload);

        DeviceMcpMessage response = sendMcpRequest(chatSession, request);
        if (response == null) {
            throw new IllegalStateException("Tempo limite excedido na resposta do dispositivo: " + toolName);
        }
        if (response.getPayload().getResult() == null) {
            throw new IllegalStateException("Falha na chamada da ferramenta: " + response.getPayload().getError());
        }
        return response.getPayload().getResult();
    }

    /**
     * Envia o comando de inicialização
     */
    protected DeviceMcpMessage sendInitialize(ChatSession chatSession) {
        DeviceMcpMessage message = new DeviceMcpMessage();
        message.setSessionId(chatSession.getSessionId());
        DeviceMcpPayload payload = new DeviceMcpPayload();
        payload.setId(chatSession.getDeviceMcpHolder().getMcpRequestId());
        payload.setMethod("initialize");
        payload.setParams(deviceMcpInitialize(chatSession));
        message.setPayload(payload);

        DeviceMcpMessage result = sendMcpRequest(chatSession, message);
        if (result != null) {
            log.debug("SessionId: {}, MCP initialized successfully", chatSession.getSessionId());
            return result;
        }
        return null;
    }

    @NotNull
    private DeviceMcpInitialize deviceMcpInitialize(ChatSession chatSession) {
        DeviceMcpInitialize initialize = new DeviceMcpInitialize();
        initialize.setClientInfo(new DeviceMcpClientInfo());

        DeviceMcpVision vision = new DeviceMcpVision();
        vision.setUrl(serverAddressProvider.getServerAddress() + "/api/vl/chat");
        vision.setToken(chatSession.getSessionId());
        initialize.setCapabilities(Map.of("vision", vision));
        return initialize;
    }

    /**
     * Envia a requisição de listagem de ferramentas (com suporte a paginação recursiva)
     *
     * @return todos os nomes originais de ferramentas coletados nesta página e nas seguintes
     */
    private List<String> sendToolsList(ChatSession chatSession, Boolean withUserTools) {
        DeviceMcpMessage message = new DeviceMcpMessage();
        message.setSessionId(chatSession.getSessionId());
        DeviceMcpPayload payload = new DeviceMcpPayload();
        payload.setId(chatSession.getDeviceMcpHolder().getMcpRequestId());
        payload.setMethod("tools/list");
        if (withUserTools != null && withUserTools) {
            payload.setParams(Map.of("withUserTools", true));
        } else if (chatSession.getDeviceMcpHolder().getMcpCursor() != null) {
            payload.setParams(Map.of("cursor", chatSession.getDeviceMcpHolder().getMcpCursor()));
        } else {
            payload.setParams(Map.of("cursor", ""));
        }
        message.setPayload(payload);

        List<String> collectedNames = new ArrayList<>();
        DeviceMcpMessage result = sendMcpRequest(chatSession, message);
        if (result == null) {
            return collectedNames;
        }

        List<Map<String, Object>> tools = (List<Map<String, Object>>) result.getPayload().getResult().get("tools");
        Object nextCursor = result.getPayload().getResult().get("nextCursor");
        int toolsCount = chatSession.getToolCallbacks().size();

        if (tools.isEmpty() || (toolsCount + tools.size()) > maxToolsCount) {
            return collectedNames;
        }

        // Constrói o mapeamento original -> sanitized em ordem decrescente do comprimento do nome original:
        // Substituir os nomes longos primeiro evita erros de posicionamento quando um nome curto é substring de um nome longo
        // (por exemplo, quando description contém tanto self.audio_speaker quanto self.audio_speaker.set_volume)
        Map<String, String> nameMapping = new LinkedHashMap<>();
        tools.stream()
                .map(t -> (String) t.get("name"))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(String::length).reversed())
                .forEach(original -> nameMapping.put(original, sanitizeToolName(original)));

        for (Map<String, Object> tool : tools) {
            final String name = (String) tool.get("name");
            String funcName = nameMapping.get(name);
            // Substitui de forma síncrona os nomes originais de ferramentas que aparecem em description, evitando que o modelo produza o nome original não registrado com base no texto da descrição
            String funcDescription = sanitizeDescription((String) tool.get("description"), nameMapping);
            Object inputSchema = tool.get("inputSchema");

            ToolCallback toolCallback = FunctionToolCallback
                    .builder(funcName, (Map<String, Object> params, ToolContext toolContext) -> {
                        DeviceMcpMessage req = new DeviceMcpMessage();
                        req.setSessionId(chatSession.getSessionId());
                        DeviceMcpPayload reqPayload = new DeviceMcpPayload();
                        reqPayload.setMethod("tools/call");
                        reqPayload.setId(chatSession.getDeviceMcpHolder().getMcpRequestId());
                        reqPayload.setParams(Map.of("name", name, "arguments", params));
                        req.setPayload(reqPayload);

                        DeviceMcpMessage resp = sendMcpRequest(chatSession, req);
                        if (resp == null) {
                            return "Falha na operação";
                        }
                        log.info("SessionId: {}, MCP function call response: {}", chatSession.getSessionId(), resp);
                        if (resp.getPayload().getResult() == null) {
                            return resp.getPayload().getError().get("message");
                        }
                        if ("false".equals(String.valueOf(resp.getPayload().getResult().get("isError")))) {
                            return resp.getPayload().getResult().get("content");
                        } else {
                            return resp.getPayload().getError();
                        }
                    })
                    .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                    .description(funcDescription)
                    .inputSchema(JsonUtil.toJson(inputSchema))
                    .inputType(Map.class)
                    .toolCallResultConverter(ToolCallStringResultConverter.INSTANCE)
                    .build();

            chatSession.getToolsSessionHolder().registerFunction(funcName, toolCallback);
            collectedNames.add(name);
        }

        if (nextCursor != null && !nextCursor.toString().isEmpty()) {
            chatSession.getDeviceMcpHolder().setMcpCursor(nextCursor.toString());
            collectedNames.addAll(sendToolsList(chatSession, null));
        } else {
            chatSession.getDeviceMcpHolder().setMcpCursor(null);
        }
        return collectedNames;
    }

    private void persistMcpList(ChatSession chatSession, List<String> toolNames) {
        if (toolNames.isEmpty()) {
            return;
        }
        String deviceId = chatSession.getDevice() != null ? chatSession.getDevice().getDeviceId() : null;
        if (!StringUtils.hasText(deviceId)) {
            return;
        }
        String mcpList = String.join(",", toolNames);
        // Compara com o valor em memória da session; se forem iguais, pula a consulta ao banco
        if (Objects.equals(chatSession.getDevice().getMcpList(), mcpList)) {
            return;
        }
        try {
            deviceRepository.findById(deviceId).ifPresent(device -> {
                device.updateMcpList(mcpList);
                deviceRepository.save(device);
            });
            chatSession.getDevice().setMcpList(mcpList);
            log.info("DeviceId: {}, mcp_list updated: {}", deviceId, mcpList);
        } catch (Exception e) {
            log.warn("DeviceId: {}, failed to persist mcp_list", deviceId, e);
        }
    }

    /**
     * Normaliza o nome da ferramenta MCP do dispositivo para um nome compatível com o OpenAI Function Calling.
     * <p>
     * Mantém letras, números, sublinhado, hífen e caracteres chineses; os demais caracteres (incluindo '.') são substituídos por '_'.
     */
    static String sanitizeToolName(String rawName) {
        return rawName.replaceAll("[^a-zA-Z0-9_\\-\\u4e00-\\u9fff]", "_");
    }

    /**
     * Substitui, em description, os nomes originais de ferramentas referenciados pelos nomes sanitized,
     * evitando que o LLM produza o nome original mencionado na descrição (como {@code self.get_device_status}), o que causaria falha no resolve.
     * <p>
     * O mapping informado deve estar em ordem decrescente do comprimento do nome original, evitando erros de posicionamento quando um nome curto é substring de um nome longo.
     */
    static String sanitizeDescription(String description, Map<String, String> nameMapping) {
        if (description == null || description.isEmpty() || nameMapping == null || nameMapping.isEmpty()) {
            return description;
        }
        String result = description;
        for (Map.Entry<String, String> entry : nameMapping.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public DeviceMcpMessage sendMcpRequest(ChatSession chatSession, DeviceMcpMessage mcpMessage) {
        Long id = mcpMessage.getPayload().getId();
        CompletableFuture<DeviceMcpMessage> future = new CompletableFuture<>();
        chatSession.sendTextMessage(JsonUtil.toJson(mcpMessage));
        chatSession.getDeviceMcpHolder().getMcpPendingRequests().put(id, future);

        DeviceMcpMessage response = null;
        try {
            response = future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("SessionId: {}, Error sending MCP request：{}", chatSession.getSessionId(), e);
            chatSession.getDeviceMcpHolder().getMcpPendingRequests().remove(id);
        }
        return response;
    }
}
