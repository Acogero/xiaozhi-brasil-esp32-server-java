package com.xiaozhi.dialogue.llm.tool.function;

import com.xiaozhi.common.config.RuntimePathConfig;
import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.communication.common.SessionManager;
import com.xiaozhi.ai.tool.ToolsGlobalRegistry;
import com.xiaozhi.ai.tool.session.ToolSession;
import com.xiaozhi.dialogue.runtime.Persona;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
// @Component
public class LocalMusicPlayer implements ToolsGlobalRegistry.GlobalFunction {
    public static final String TOOL_NAME = "play_music";

    // Usa um executor de virtual threads para tratar tarefas agendadas
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            Thread.ofVirtual().name("music-scheduler-", 0).factory());

    @Resource
    private SessionManager sessionManager;

    @Resource
    private RuntimePathConfig runtimePathConfig;

    @Tool(name = TOOL_NAME, description = "Player de música, reproduz a música com o nome especificado", returnDirect = true)
    public String playMusic(@ToolParam(description = "Nome da música a ser reproduzida") String songName, ToolContext toolContext) {
        String sessionId = (String) toolContext.getContext().get(Persona.TOOL_CONTEXT_SESSION_ID_KEY);
        ChatSession chatSession = sessionManager.getSession(sessionId);

        try {
            if (songName == null || songName.isEmpty()) {
                return "Você não me disse o nome específico da música, não consigo reproduzir!";
            } else {
                scheduler.schedule(() -> {
                    // Deve ser tratado de forma assíncrona: primeiro retorna uma string de resposta ao usuário, depois inicia a reprodução.
                    chatSession.getPlayer().play(songName, Path.of(runtimePathConfig.getMusicDir(), songName + ".mp3"));
                }, 60, TimeUnit.MILLISECONDS);
                return "Tentando reproduzir a música \"" + songName + "\"";
            }

        } catch (Exception e) {
            log.error("Exceção na reprodução de música do device, song name: {}", songName, e);
            return "Falha na reprodução da música";
        }
    }

    @Override
    public ToolCallback getFunctionCallTool(ToolSession toolSession) {
        ToolCallback[] tools = ToolCallbacks.from(this);
        return tools[0];
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
