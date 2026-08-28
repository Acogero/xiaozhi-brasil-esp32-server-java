package com.xiaozhi.dialogue.llm.tool.function;

import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.dialogue.runtime.Persona;
import com.xiaozhi.ai.tool.ToolsGlobalRegistry;
import com.xiaozhi.ai.tool.session.ToolSession;
import com.xiaozhi.ai.llm.tool.ToolCallStringResultConverter;
import com.xiaozhi.communication.message.MessageSender;
import com.xiaozhi.dialogue.llm.tool.media.MusicPlayer;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Component
public class PlayMusicFunction implements ToolsGlobalRegistry.GlobalFunction {
    private static final String TOOL_NAME = "play_music";
    // Usa um executor de virtual threads para tratar tarefas agendadas
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            Thread.ofVirtual().name("music-scheduler-", 0).factory());
    @Autowired
    private MessageSender messageService;

    ToolCallback toolCallback = FunctionToolCallback
            .builder(TOOL_NAME, (Map<String, String> params, ToolContext toolContext) -> {
                ChatSession chatSession = (ChatSession)toolContext.getContext().get(Persona.TOOL_CONTEXT_SESSION_ID_KEY);
                String songName = params.get("songName");
                try{
                    if (songName == null || songName.isEmpty()) {
                        return "Falha na reprodução da música";
                    }else{
                        scheduler.schedule(() -> {
                            // Deve ser tratado de forma assíncrona: primeiro retorna uma string de resposta ao usuário, depois inicia a reprodução.
                            new MusicPlayer(chatSession,songName, null).play();
                        },60, TimeUnit.MILLISECONDS);

                        return "Tentando reproduzir a música \""+songName+"\"";
                    }
                }catch (Exception e){
                    log.error("Exceção na reprodução de música do device, song name: {}", songName, e);
                    return "Falha na reprodução da música";
                }
            })
            .toolMetadata(ToolMetadata.builder().returnDirect(true).build())
            .description("Assistente de reprodução de música; requer que o usuário informe o nome da música")
            .inputSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "songName": {
                                    "type": "string",
                                    "description": "Nome da música a ser reproduzida"
                                }
                            },
                            "required": ["songName"]
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
        return "Reproduzir música";
    }
}
