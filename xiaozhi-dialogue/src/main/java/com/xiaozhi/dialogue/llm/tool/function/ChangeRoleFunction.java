package com.xiaozhi.dialogue.llm.tool.function;

import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.communication.common.SessionManager;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.RoleBO;
import com.xiaozhi.device.domain.repository.DeviceRepository;
import com.xiaozhi.dialogue.llm.factory.PersonaFactory;
import com.xiaozhi.ai.llm.tool.ToolCallStringResultConverter;
import com.xiaozhi.ai.tool.ToolsGlobalRegistry;
import com.xiaozhi.ai.tool.session.ToolSession;
import com.xiaozhi.ai.llm.tool.XiaozhiToolMetadata;
import com.xiaozhi.dialogue.runtime.Persona;
import com.xiaozhi.role.service.RoleService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
/**
 * Função para trocar de papel por voz
 */
@Slf4j
// @Component
public class ChangeRoleFunction implements ToolsGlobalRegistry.GlobalFunction {
    private static final String TOOL_NAME = "change_role";
    @Resource
    private RoleService roleService;
    @Resource
    private DeviceRepository deviceRepository;
    @Resource
    @Lazy
    private PersonaFactory personaFactory;
    @Resource
    private SessionManager sessionManager;

    @Override
    public ToolCallback getFunctionCallTool(ToolSession toolSession) {
        // Obtém o ChatSession via sessionManager (ToolSession é um Adapter, não pode ser convertido diretamente)
        ChatSession chatSession = sessionManager.getSession(toolSession.getSessionId());
        if (chatSession == null) {
            return null;
        }
        DeviceBO device = chatSession.getDevice();
        List<RoleBO> roleList = roleService.listBO(device.getUserId(), 5);
        if(!roleList.isEmpty() && roleList.size() > 1) {
            return FunctionToolCallback
                    .builder(TOOL_NAME, (Map<String, String> params, ToolContext toolContext) -> {
                        String roleName = params.get("roleName");
                        try{
                            // Obtém os parâmetros
                            Optional<RoleBO> changedRole = roleList.stream()
                                    .filter(role -> role.getRoleName().equals(roleName))
                                    .findFirst();

                            if(changedRole.isPresent()){
                                RoleBO role = changedRole.get();
                                deviceRepository.findById(device.getDeviceId()).ifPresent(d -> {
                                    d.bindRole(role.getRoleId());
                                    deviceRepository.save(d);
                                });
                                device.setRoleId(role.getRoleId());
                                device.setRoleName(role.getRoleName());
                                // Papel alterado, é necessário trocar o Conversation
                                if(chatSession.getPersona().getConversation()!=null){
                                    chatSession.getPersona().getConversation().clear();
                                }

                                Persona persona = personaFactory.buildPersona(chatSession, device, role);
                                chatSession.setPersona(persona);
                                return "Papel alterado para " + roleName;
                            }else{
                                return "Falha ao trocar de papel, não há um papel correspondente";
                            }
                        }catch (Exception e){
                            log.error("Exceção ao trocar de papel, role name: {}", roleName, e);
                            return "Exceção ao trocar de papel";
                        }
                    })
                    .toolMetadata(new XiaozhiToolMetadata(true))
                    .description("Chamado quando o usuário deseja trocar de papel/nome do assistente. Lista de papéis disponíveis: " + getRoleList(roleList)
                            + ". Antes de chamar, informe ao usuário todos os nomes de papéis disponíveis; o usuário informará o nome do papel para a troca.")
                    .inputSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "roleName": {
                                    "type": "string",
                                    "description": "Nome do papel para o qual trocar"
                                }
                            },
                            "required": ["roleName"]
                        }
                    """)
                    .inputType(Map.class)
                    .toolCallResultConverter(ToolCallStringResultConverter.INSTANCE)
                    .build();
        }
        return null;
    }

    public String getRoleList(List<RoleBO> roleList){
        return roleList.stream().map(RoleBO::getRoleName).collect(Collectors.joining(", "));
    }

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public String getToolDescription() {
        return "Trocar de papel";
    }
}
