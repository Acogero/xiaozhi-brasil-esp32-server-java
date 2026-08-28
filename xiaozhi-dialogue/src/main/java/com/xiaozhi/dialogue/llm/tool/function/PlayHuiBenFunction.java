package com.xiaozhi.dialogue.llm.tool.function;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.RandomUtil;
import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.dialogue.runtime.Persona;
import com.xiaozhi.ai.llm.tool.ToolCallStringResultConverter;
import com.xiaozhi.ai.tool.ToolsGlobalRegistry;
import com.xiaozhi.ai.tool.session.ToolSession;
import com.xiaozhi.dialogue.llm.tool.media.HuiBenPlayer;
import com.xiaozhi.utils.AudioUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
// @Component
public class PlayHuiBenFunction implements ToolsGlobalRegistry.GlobalFunction {
    private static final String TOOL_NAME = "play_huiben";
    // Usa um executor de virtual threads para tratar tarefas agendadas
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            Thread.ofVirtual().name("huiBen-scheduler-", 0).factory());

    ToolCallback toolCallback = FunctionToolCallback
            .builder(TOOL_NAME, (Map<String, String> params, ToolContext toolContext) -> {
                ChatSession chatSession = (ChatSession) toolContext.getContext().get(Persona.TOOL_CONTEXT_SESSION_ID_KEY);
                Integer num = MapUtil.getInt(params, "num");
                try {
                    if (num == null || num < 5 || num > 1100) {
                        num = RandomUtil.randomInt(5, 1100);
                    }
                    var huiBenPlayer = new HuiBenPlayer(chatSession,num);
                    scheduler.schedule(() -> {
                        huiBenPlayer.play();
                    }, AudioUtils.OPUS_FRAME_DURATION_MS, TimeUnit.MILLISECONDS);

                    return "Tentando reproduzir o livro ilustrado \"" + num + "\"";

                } catch (Exception e) {
                    log.error("Exceção ao reproduzir o livro ilustrado, número: {}", num, e);
                    return "Falha ao reproduzir o livro ilustrado";
                }
            })
            .toolMetadata(ToolMetadata.builder().returnDirect(true).build())
            .description("Assistente de reprodução de livros ilustrados; requer que o usuário informe o número do livro")
            .inputSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "num": {
                                    "type": "integer",
                                    "description": "Número do livro ilustrado a ser reproduzido"
                                }
                            },
                            "required": ["num"]
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
        return "Reproduzir livro ilustrado";
    }
}