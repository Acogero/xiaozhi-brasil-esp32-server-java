package com.xiaozhi.dialogue.llm.tool.function;

import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.communication.common.SessionManager;
import com.xiaozhi.ai.tool.ToolsGlobalRegistry;
import com.xiaozhi.ai.tool.session.ToolSession;
import com.xiaozhi.ai.llm.tool.ToolCallStringResultConverter;
import com.xiaozhi.ai.llm.tool.XiaozhiToolMetadata;
import com.xiaozhi.dialogue.runtime.Persona;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SessionExitFunction implements ToolsGlobalRegistry.GlobalFunction {
    private static final String TOOL_NAME = "exit_session";

    @Resource
    private SessionManager sessionManager;

    ToolCallback toolCallback = FunctionToolCallback
            .builder(TOOL_NAME, (Map<String, String> params, ToolContext toolContext) -> {
                String sessionId = (String) toolContext.getContext().get(Persona.TOOL_CONTEXT_SESSION_ID_KEY);
                ChatSession chatSession = sessionManager.getSession(sessionId);
                chatSession.getPlayer().setFunctionAfterChat(()->sessionManager.closeSession(chatSession));
                String sayGoodbye = params.get("sayGoodbye");
                if(sayGoodbye == null || sayGoodbye.trim().isEmpty()){
                    sayGoodbye = "Certo, até logo! Aguardo nossa próxima conversa!";
                }
                return sayGoodbye;
            })
            .toolMetadata(new XiaozhiToolMetadata(true))
            .description("Chamado quando o usuário expressa claramente que deseja sair/encerrar o diálogo. Palavras-gatilho: 'tchau', 'até logo', 'já vou', 'encerrar conversa', 'sair', 'eu vou embora', 'goodbye', 'bye'. Importante: ao detectar essas palavras, esta função deve ser chamada para encerrar corretamente a sessão, não apenas responder normalmente.")
            .inputSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "sayGoodbye": {
                                    "type": "string",
                                    "description": "Mensagem de despedida"
                                }
                            },
                            "required": ["sayGoodbye"]
                        }
                    """)
            .inputType(Map.class)
            .toolCallResultConverter(ToolCallStringResultConverter.INSTANCE)
            .build();

    @Override
    public ToolCallback getFunctionCallTool(ToolSession toolSession) {
        return toolCallback;
    }

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public String getToolDescription() {
        return "Saída da sessão";
    }
}
