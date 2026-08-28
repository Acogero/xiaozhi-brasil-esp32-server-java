package com.xiaozhi.dialogue.llm.tool.function;

import com.xiaozhi.ai.llm.tool.ToolCallStringResultConverter;
import com.xiaozhi.ai.tool.ToolsGlobalRegistry;
import com.xiaozhi.ai.tool.session.ToolSession;
import okhttp3.OkHttpClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
/**
 * Atualização do Quote0
 */
@Slf4j
// @Component
public class Quote0Function implements ToolsGlobalRegistry.GlobalFunction {
    private static final String TOOL_NAME = "update_eink";
    private static final String API_BASE_URL = "https://dot.mindreset.tech/api/open/text";

    // Usa OkHttp3 em vez do HttpClient do JDK
    private static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    public ToolCallback getFunctionCallTool(ToolSession toolSession) {
        return FunctionToolCallback
                .builder(TOOL_NAME, (Map<String, String> params, ToolContext toolContext) -> {
                    String title = params.get("title");
                    String message = params.get("message");
                    String signature = params.get("signature");
                    try {
                        // Monta o corpo da requisição JSON
                        String jsonBody = String.format("""
                                {
                                    "refreshNow": true,
                                    "deviceId": "xxx",
                                    "title": "%s",
                                    "message": "%s",
                                    "signature": "%s",
                                    "icon": "",
                                    "link": ""
                                }
                                """, title, message, signature);

                        // Monta a requisição HTTP
                        okhttp3.Request request = new okhttp3.Request.Builder()
                                .url(API_BASE_URL)
                                .post(okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json")))
                                .addHeader("Authorization", "Bearer xxx") // Deve ser substituído pela chave de API real
                                .addHeader("Content-Type", "application/json")
                                .build();

                        // Executa a requisição
                        try (okhttp3.Response response = okHttpClient.newCall(request).execute()) {
                            if (response.isSuccessful()) {
                                return "Informações da tela de tinta eletrônica atualizadas com sucesso";
                            } else {
                                log.error("Falha ao atualizar a tela de tinta eletrônica, código HTTP: {}", response.code());
                                return "Falha ao atualizar a tela de tinta eletrônica, código: " + response.code();
                            }
                        }
                    } catch (Exception e) {
                        log.error("Exceção ao atualizar a tela de tinta eletrônica", e);
                        return "Exceção ao atualizar a tela de tinta eletrônica: " + e.getMessage();
                    }
                })
                .toolMetadata(ToolMetadata.builder().returnDirect(true).build())
                .description("Atualiza as informações da tela de tinta eletrônica; o horário atual é: " + LocalDateTime.now())
                .inputSchema("""
                            {
                                "type": "object",
                                "properties": {
                                    "title": {
                                        "type": "string",
                                        "description": "Título da atualização, geralmente um lembrete de agenda"
                                    },
                                    "message": {
                                        "type": "string",
                                        "description": "Informações a serem exibidas"
                                    },
                                    "signature": {
                                        "type": "string",
                                        "description": "Horário de atenção, formato: yyyy-MM-dd HH:mm:ss"
                                    }
                                },
                                "required": ["title","message","signature"]
                            }
                        """)
                .inputType(Map.class)
                .toolCallResultConverter(ToolCallStringResultConverter.INSTANCE)
                .build();
    }

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public String getToolDescription() {
        return "Atualizar tela de tinta eletrônica";
    }
}
