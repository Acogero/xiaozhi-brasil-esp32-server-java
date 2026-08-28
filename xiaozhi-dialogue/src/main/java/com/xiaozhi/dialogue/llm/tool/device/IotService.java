package com.xiaozhi.dialogue.llm.tool.device;

import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.communication.common.SessionManager;
import com.xiaozhi.communication.message.MessageSender;
import com.xiaozhi.communication.domain.iot.IotDescriptor;
import com.xiaozhi.communication.domain.iot.IotProperty;
import com.xiaozhi.communication.domain.iot.IotState;
import com.xiaozhi.ai.llm.tool.ToolCallStringResultConverter;
import com.xiaozhi.ai.tool.ToolsSessionHolder;
import com.xiaozhi.utils.JsonUtil;
import jakarta.annotation.Resource;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
/**
 * Serviço de IoT - responsável pelo processamento de IoT e pelo envio via WebSocket
 */
@Slf4j
@Service
public class IotService {
    private static final String TAG = "IotService";

    @Resource
    private SessionManager sessionManager;

    @Resource
    private MessageSender messageService;

    /**
     * Trata as informações de descrição do dispositivo IoT, gera o function_call e registra no sessionManager, para uso posterior em chamadas do LLM e do dispositivo
     *
     * @param sessionId   ID da sessão
     * @param descriptors conteúdo da mensagem de descrição do dispositivo IoT
     */
    public void handleDeviceDescriptors(String sessionId, List<IotDescriptor> descriptors) {
        ChatSession chatSession = sessionManager.getSession(sessionId);
        for (var descriptor : descriptors) {
            chatSession.getIotDescriptors().put(descriptor.getName(), descriptor);
            registerFunctionTools(sessionId, descriptor);
        }
    }

    /**
     * Trata as informações de mudança de estado do dispositivo IoT; pode ser usado para atualizar o estado do dispositivo ou realizar outras operações
     *
     * @param sessionId ID da sessão
     * @param states    conteúdo da mensagem de estado do IoT
     */
    public void handleDeviceStates(String sessionId, List<IotState> states) {
        ChatSession chatSession = sessionManager.getSession(sessionId);
        for (var state : states) {
            var iotDescriptor = chatSession.getIotDescriptors().get(state.getName());
            if (iotDescriptor == null) {
                log.error("[{}] - SessionId: {} informações de descrição do dispositivo {} não encontradas", TAG, sessionId, state.getName());
                continue;
            }
            for (var stateProp : state.getState().entrySet()) {
                var propName = stateProp.getKey();
                var propValue = stateProp.getValue();
                var property = iotDescriptor.getProperties().get(propName);
                if (property != null) {
                    property.setValue(propValue);
                    log.info("[{}] - SessionId: {} handleDeviceStates atualização de estado IoT: {} , {} = {}", TAG, sessionId, state.getName(), propName, propValue);
                } else {
                    log.error("[{}] - SessionId: {} handleDeviceStates propriedade {} do dispositivo {} não encontrada", TAG, sessionId, state.getName(), propName);
                }
            }
        }
    }

    /**
     * Obtém o estado do IoT
     *
     * @param sessionId    ID da sessão
     * @param iotName      nome do dispositivo IoT
     * @param propertyName nome da propriedade
     * @return o valor da propriedade; retorna null se não for encontrado
     */
    public Object getIotStatus(String sessionId, String iotName, String propertyName) {
        ChatSession chatSession = sessionManager.getSession(sessionId);
        var iotDescriptor = chatSession.getIotDescriptors().get(iotName);
        if (iotDescriptor != null) {
            IotProperty property = iotDescriptor.getProperties().get(propertyName);
            if (property != null) {
                return property.getValue();
            } else {
                log.error("[{}] - SessionId: {} getIotStatus propriedade {} do dispositivo {} não encontrada", TAG, sessionId, iotName, propertyName);
            }
        } else {
            log.error("[{}] - SessionId: {} getIotStatus dispositivo {} não encontrado", TAG, sessionId, iotName);
        }
        return null;
    }

    /**
     * Define o estado do IoT
     *
     * @param sessionId    ID da sessão
     * @param iotName      nome do dispositivo IoT
     * @param propertyName nome da propriedade
     * @param value        valor da propriedade
     * @return se a definição foi bem-sucedida
     */
    public boolean setIotStatus(String sessionId, String iotName, String propertyName, Object value) {
        ChatSession chatSession = sessionManager.getSession(sessionId);
        var iotDescriptor = chatSession.getIotDescriptors().get(iotName);
        if (iotDescriptor != null) {
            IotProperty property = iotDescriptor.getProperties().get(propertyName);
            if (property != null) {
                // Verificação de tipo
                boolean typeCheck = false;
                if (property.getType().equalsIgnoreCase(JsonNodeType.OBJECT.name())) {
                    typeCheck = true;
                } else if (value instanceof Number && property.getType().equalsIgnoreCase(JsonNodeType.NUMBER.name())) {
                    typeCheck = true;
                } else if (value instanceof String && property.getType().equalsIgnoreCase(JsonNodeType.STRING.name())) {
                    typeCheck = true;
                } else if (value instanceof Boolean && property.getType().equalsIgnoreCase(JsonNodeType.BOOLEAN.name())) {
                    typeCheck = true;
                }
                if (!typeCheck) {
                    log.error("[{}] - SessionId: {} setIotStatus tipo do valor da propriedade {} não corresponde, tipo registrado: {}, tipo informado: {}", TAG, sessionId, propertyName,
                            property.getType(), value.getClass().getSimpleName());
                    return false;
                }
                property.setValue(value);
                log.info("[{}] - SessionId: {} setIotStatus atualização de estado IoT: {} , {} = {}", TAG, sessionId, iotName, propertyName, value);
                sendIotMessage(sessionId, iotName, propertyName, Collections.singletonMap(propertyName, value));
                return true;
            }
        }
        log.error("[{}] - SessionId: {} setIotStatus propriedade {} do dispositivo {} não encontrada", TAG, sessionId, iotName, propertyName);
        return false;
    }

    /**
     * Envia mensagem IoT ao dispositivo
     *
     * @param sessionId  ID da sessão
     * @param iotName    nome do dispositivo IoT
     * @param methodName nome do método
     * @param parameters parâmetros do método
     */
    public boolean sendIotMessage(String sessionId, String iotName, String methodName, Map<String, Object> parameters) {
        try {
            log.info("[{}] - SessionId: {}, message send iotName: {}, methodName: {}, parameters: {}", TAG, sessionId,
                    iotName, methodName, JsonUtil.toJson(parameters));
            ChatSession chatSession = sessionManager.getSession(sessionId);
            IotDescriptor iotDescriptor = chatSession.getIotDescriptors().get(iotName);
            if (iotDescriptor != null && iotDescriptor.getMethods().containsKey(methodName)) {
                Map<String, Object> command = new HashMap<>();
                command.put("name", iotName);
                command.put("method", methodName);
                command.put("parameters", parameters);
                messageService.sendIotCommandMessage(chatSession, Collections.singletonList(command));
                return true;
            } else {
                log.error("[{}] - SessionId: {}, {} method not found: {}", TAG, sessionId, iotName, methodName);
            }
        } catch (Exception e) {
            log.error("[{}] - SessionId: {}, error sending Iot message", TAG, sessionId, e);
        }
        return false;
    }

    /**
     * Registra as funções do dispositivo IoT no FunctionHolder
     *
     * @param sessionId     ID da sessão
     * @param iotDescriptor FunctionHolder vinculado à session
     */
    private void registerFunctionTools(String sessionId, IotDescriptor iotDescriptor) {
        ChatSession chatSession = sessionManager.getSession(sessionId);
        ToolsSessionHolder toolsSessionHolder = chatSession != null ? chatSession.getToolsSessionHolder() : null;
        registerPropertiesFunctionTools(sessionId, toolsSessionHolder, iotDescriptor);
        registerMethodFunctionTools(sessionId, toolsSessionHolder, iotDescriptor);
    }

    /**
     * Registra os métodos de consulta de propriedades do dispositivo IoT no FunctionHolder
     *
     * @param sessionId          ID da sessão
     * @param toolsSessionHolder FunctionHolder vinculado à session
     * @param iotDescriptor      informações do IoT
     */
    private void registerPropertiesFunctionTools(String sessionId, ToolsSessionHolder toolsSessionHolder, IotDescriptor iotDescriptor) {
        //Percorre properties, gerando FunctionCallTool
        var iotName = iotDescriptor.getName();
        for (var entry : iotDescriptor.getProperties().entrySet()) {
            var propName = entry.getKey();
            var propInfo = entry.getValue();
            // Cria o nome da função, formato: iot_get_{IoTName}_{PropName}
            var funcName = "iot_get_" + iotName.toLowerCase() + "_" + propName.toLowerCase();
            var toolCallback = FunctionToolCallback
                    .builder(funcName, (Map<String, String> params, ToolContext toolContext) -> {
                        Object value = getIotStatus(sessionId, iotName, propName);
                        if (value != null) {
                            // Obtém os parâmetros
                            String response_success = params.get("response_success");
                            //Se houver o parâmetro success e o placeholder {value}, substitui pelo parâmetro correspondente
                            if (response_success != null) {
                                if (response_success.contains("{value}")) {
                                    response_success = response_success.replace("{value}", String.valueOf(value));
                                }
                            } else {
                                response_success = "O valor atual é " + value;
                            }
                            return response_success;
                        } else {
                            return "Não foi possível obter a configuração";
                        }
                    })
                    .toolMetadata(ToolMetadata.builder().returnDirect(true).build())
                    .description("Consulta " + propInfo.getDescription() + " de " + iotName)
                    .inputSchema("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "response_success": {
                                            "type": "string",
                                            "description": "Resposta amigável para quando a consulta for bem-sucedida; deve usar {value} como placeholder para o valor consultado"
                                        }
                                    },
                                    "required": ["response_success"]
                                }
                            """)
                    .inputType(Map.class)
                    .toolCallResultConverter(ToolCallStringResultConverter.INSTANCE)
                    .build();
            // Registra no function holder da sessão atual
            toolsSessionHolder.registerFunction(funcName, toolCallback);
        }

    }

    /**
     * Registra os métodos chamáveis do dispositivo IoT no FunctionHolder
     *
     * @param sessionId          ID da sessão
     * @param toolsSessionHolder instância do FunctionHolder
     * @param iotDescriptor      informações do IoT
     */
    private void registerMethodFunctionTools(String sessionId, ToolsSessionHolder toolsSessionHolder, IotDescriptor iotDescriptor) {
        // Percorre methods, gerando FunctionCallTool
        var iotName = iotDescriptor.getName();

        for (var entry : iotDescriptor.getMethods().entrySet()) {
            var methodName = entry.getKey();
            var method = entry.getValue();
            // Cria o nome da função, formato: iot_{IoTName}_{MethodName}
            var funcName = "iot_" + iotName + "_" + methodName;

            Map<String, String> valueMap = new HashMap<>();
            //Obtém o parâmetro do método iotMethod e adiciona aos parâmetros da função. Os métodos IoT têm sempre um único parâmetro
            for (var paramEntry : method.getParameters().entrySet()) {
                var paramName = paramEntry.getKey();
                var paramInfo = paramEntry.getValue();
                valueMap.put("paramName", paramName);
                valueMap.put("paramType", paramInfo.getType());
                valueMap.put("paramDescription", paramInfo.getDescription());
            }
            String inputSchema = StringSubstitutor.replace("""
                        {
                            "type": "object",
                            "properties": {
                                "${paramName}": {
                                    "type": "${paramType}",
                                    "description": "${paramDescription}"
                                },
                                "response_success": {
                                    "type": "string",
                                    "description": "Resposta amigável para quando a operação for bem-sucedida, referente ao resultado da operação neste dispositivo; use o nome presente em description para o dispositivo, sem placeholders"
                                }
                            },
                            "required": ["${paramName}", "response_success"]
                        }
                    """, valueMap);

            var toolCallback = FunctionToolCallback
                    .builder(funcName, (Map<String, Object> params, ToolContext toolContext) -> {
                        String actFuncName = funcName.replace("iot_" + iotName + "_", ""); // Chamada do método original, removendo o prefixo iot_iotName_
                        String response_success = (String) params.get("response_success");
                        params.remove("response_success"); // Remove o parâmetro response_success, evitando repassá-lo ao dispositivo
                        boolean result = sendIotMessage(sessionId, iotName, actFuncName, params);
                        if (result) {
                            // Obtém os parâmetros
                            if (response_success == null || response_success.isEmpty()) {
                                response_success = "Operação bem-sucedida";
                            }
                            return response_success;
                        } else {
                            return "Falha na operação";
                        }
                    })
                    .toolMetadata(ToolMetadata.builder().returnDirect(true).build())
                    .description(iotDescriptor.getDescription() + " - " + method.getDescription())
                    .inputSchema(inputSchema)
                    .inputType(Map.class)
                    .toolCallResultConverter(ToolCallStringResultConverter.INSTANCE)
                    .build();
            // Registra no function holder da sessão atual
            toolsSessionHolder.registerFunction(funcName, toolCallback);
        }
    }

}
