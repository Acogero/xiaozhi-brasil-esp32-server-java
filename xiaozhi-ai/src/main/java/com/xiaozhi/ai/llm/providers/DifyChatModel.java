package com.xiaozhi.ai.llm.providers;

import io.github.imfangs.dify.client.DifyChatClient;
import io.github.imfangs.dify.client.DifyClientFactory;
import io.github.imfangs.dify.client.callback.ChatStreamCallback;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.event.*;
import io.github.imfangs.dify.client.model.chat.ChatMessage;
import io.github.imfangs.dify.client.model.chat.ChatMessageResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DifyChatModel implements ChatModel {
    /**
     * Chave sessionId que a Persona coloca no ToolContext, consistente com
     * {@code com.xiaozhi.dialogue.runtime.Persona.TOOL_CONTEXT_SESSION_ID_KEY}.
     * Aqui é usado um literal porque o módulo xiaozhi-ai não depende de xiaozhi-dialogue.
     */
    private static final String TOOL_CONTEXT_SESSION_ID_KEY = "sessionId";

    private DifyChatClient chatClient;

    /**
     * Armazena em cache o conversation_id retornado pelo Dify, indexado por sessionId, para que conversas de múltiplas rodadas mantenham a memória de sessão do agente Dify.
     */
    private final Map<String, String> conversationIds = new ConcurrentHashMap<>();

    /**
     * Construtor
     *
     * @param endpoint  Endpoint da API
     * @param apiKey    Chave de API
     */
    public DifyChatModel(String endpoint, String apiKey) {
        chatClient = DifyClientFactory.createChatClient(endpoint, apiKey);
    }

    public String getProviderName() {
        return "dify";
    }

    @Override
    public ChatResponse call(Prompt prompt) {

        // Cria a mensagem de chat
        // inputs deve ser não nulo (mesmo sem variáveis de App, é preciso enviar um objeto vazio), caso contrário o servidor Dify rejeitará a requisição.
        // conversationId usa o ID de sessão retornado pelo Dify na rodada anterior, permitindo que o agente mantenha a memória de contexto.
        ChatMessage message = ChatMessage.builder()
                .query(prompt.getContents())
                .inputs(Map.of())
                .user(resolveUserId(prompt))
                .conversationId(getCurrentConversationId(prompt))
                .responseMode(ResponseMode.BLOCKING)
                .build();
        try {
            // Envia a mensagem e obtém a resposta
            ChatMessageResponse response = chatClient.sendChatMessage(message);
            log.debug("Resposta: {}", response.getAnswer());
            log.debug("ID da sessão: {}", response.getConversationId());
            log.debug("ID da mensagem: {}", response.getMessageId());
            saveCurrentConversationId(prompt, response.getConversationId());
            return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                    .content(response.getAnswer())
                    .properties(Map.of("messageId", response.getMessageId(), "conversationId", response.getConversationId()))
                    .build())));

        } catch (IOException e) {
            log.error("Erro: ", e);
            return ChatResponse.builder().generations(Collections.emptyList()).build();
        }

    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        Flux<ChatResponse> responseFlux = Flux.create(sink -> {

            // inputs deve ser não nulo (mesmo sem variáveis de App, é preciso enviar um objeto vazio), caso contrário o servidor Dify rejeitará a requisição.
            // conversationId usa o ID de sessão retornado pelo Dify na rodada anterior, permitindo que o agente mantenha a memória de contexto.
            ChatMessage message = ChatMessage.builder()
                    .user(resolveUserId(prompt))
                    .query(prompt.getUserMessage().getText())
                    .inputs(Map.of())
                    .conversationId(getCurrentConversationId(prompt))
                    .responseMode(ResponseMode.STREAMING)
                    .build();

            // Envia a mensagem em streaming
            try {
                chatClient.sendChatMessageStream(message, new ChatStreamCallback() {
                    @Override
                    public void onMessage(MessageEvent event) {
                        sink.next(ChatResponse.builder()
                                .generations(
                                        List.of(new Generation(AssistantMessage.builder()
                                                .content(event.getAnswer())
                                                .properties(Map.of("messageId", event.getMessageId(),
                                                        "conversationId", event.getConversationId()))
                                                .build())))
                                .build());
                    }

                    @Override
                    public void onAgentMessage(AgentMessageEvent event) {
                        sink.next(ChatResponse.builder()
                                .generations(
                                        List.of(new Generation(AssistantMessage.builder()
                                                .content(event.getAnswer())
                                                .properties(Map.of("messageId", event.getMessageId(),
                                                        "conversationId", event.getConversationId()))
                                                .build())))
                                .build());
                    }

                    @Override
                    public void onMessageEnd(MessageEndEvent event) {
                        // É necessário persistir o conversationId antes de chamar complete, para evitar que a próxima chamada de chatStream leia o valor antigo antes do save.
                        saveCurrentConversationId(prompt, event.getConversationId());
                        sink.complete();
                    }

                    @Override
                    public void onError(ErrorEvent event) {
                        sink.error(new IOException(event.toString()));
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        log.error("Exceção: {}", throwable.getMessage());
                        sink.error(throwable);
                    }

                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return responseFlux;
    }

    /**
     * Extrai o deviceId do ChatOptions do Prompt e gera um userId determinístico.
     * Caso não seja possível extrair o deviceId, recorre a um userId baseado em UUID.
     */
    private String resolveUserId(Prompt prompt) {
        if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions) {
            Map<String, Object> toolContext = toolCallingChatOptions.getToolContext();
            if (toolContext != null) {
                Object deviceIdObj = toolContext.get("deviceId");
                if (deviceIdObj instanceof String deviceId && !deviceId.isBlank()) {
                    return "user_xz_" + deviceId.replace(":", "");
                }
            }
        }
        return "user_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Obtém o sessionId a partir do ToolContext e consulta o conversation_id retornado pelo Dify na rodada anterior.
     * Quando não encontrado, retorna null: a API do Dify interpreta isso como o início de uma nova sessão.
     */
    private String getCurrentConversationId(Prompt prompt) {
        String sessionId = extractSessionId(prompt);
        if (sessionId == null) {
            return null;
        }
        return conversationIds.get(sessionId);
    }

    /**
     * Persiste o conversation_id retornado pelo Dify. Só grava quando há sessionId e conversationId válidos.
     */
    private void saveCurrentConversationId(Prompt prompt, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        String sessionId = extractSessionId(prompt);
        if (sessionId == null) {
            return;
        }
        conversationIds.put(sessionId, conversationId);
    }

    private String extractSessionId(Prompt prompt) {
        if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions) {
            Map<String, Object> toolContext = toolCallingChatOptions.getToolContext();
            if (toolContext != null) {
                Object value = toolContext.get(TOOL_CONTEXT_SESSION_ID_KEY);
                if (value instanceof String sessionId && !sessionId.isBlank()) {
                    return sessionId;
                }
            }
        }
        return null;
    }
}