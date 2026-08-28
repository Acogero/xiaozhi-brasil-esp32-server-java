package com.xiaozhi.ai.llm.providers;

import com.xiaozhi.ai.llm.providers.xingchen.*;
import com.xiaozhi.utils.JsonUtil;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XingChenChatModel implements ChatModel {

    private XingChenClient chatClient;

    /**
     * Construtor
     */
    public XingChenChatModel(String endpoint, String apiKey, String secret) {
        chatClient = new XingChenClient(endpoint, apiKey, secret);
    }

    public String getProviderName() {
        return "xingchen";
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        ToolCallingChatOptions chatOptions = (ToolCallingChatOptions) prompt.getOptions();
        Map<String, Object> input = null;
        if (chatOptions != null) {
            input = Map.of(
                    "AGENT_USER_INPUT", prompt.getUserMessage().getText(),
                    "func_call", chatOptions.getToolCallbacks()
            );
            log.info("Ferramentas suportadas: {}", JsonUtil.toJson(chatOptions.getToolCallbacks()));
        } else {
            input = Map.of(
                    "AGENT_USER_INPUT", prompt.getUserMessage().getText(),
                    "func_call", new ArrayList<>()
            );
        }
        // Cria a mensagem de chat
        XingChenRequest message = XingChenRequest.builder()
                .flowId(chatClient.getFlowId())
                .uid("1")
                .parameters(
                        input
                )
                .ext(XingChenRequest.Ext.builder().botId("1").caller("workflow").build())
                .stream(false)
                .history(new ArrayList<>())
                .chatId("1")
                .build();
        try {
            // Envia a mensagem e obtém a resposta
            XingChenResponse response = chatClient.sendChatMessage(message);
            return new ChatResponse(List.of(new Generation(
                    AssistantMessage.builder()
                            .content(response.getChoices().get(0).getDelta().getContent())
                            .properties(Map.of("messageId", response.getId()))
                            .build()
            )));

        } catch (IOException e) {
            log.error("Erro: ", e);
            return ChatResponse.builder().generations(Collections.emptyList()).build();
        }

    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        Flux<ChatResponse> responseFlux = Flux.create(sink -> {

            ToolCallingChatOptions chatOptions = (ToolCallingChatOptions) prompt.getOptions();
            // Cria a mensagem de chat
            XingChenRequest message = XingChenRequest.builder()
                    .flowId(chatClient.getFlowId())
                    .uid("1")
                    .parameters(
                            Map.of(
                                    "AGENT_USER_INPUT", prompt.getUserMessage().getText(),
                                    "func_call", chatOptions.getToolCallbacks()
                            )
                    )
                    .ext(XingChenRequest.Ext.builder().botId("1").caller("workflow").build())
                    .stream(true)
                    .history(new ArrayList<>())
                    .chatId("1")
                    .build();

            // Usa um array para armazenar a flag (necessário por ser modificada dentro de uma classe anônima interna)
            final boolean[] hasToolCall = {false};
            
            // Envia a mensagem em streaming
            try {
                chatClient.sendChatMessageStream(message, new XingChenChatStreamCallback() {
                    @Override
                    public void onMessage(XingChenResponse event) {
                        // Verificação de segurança: garante que choices não seja vazio
                        if (event.getChoices() == null || event.getChoices().isEmpty()) {
                            log.warn("Choices vazio recebido, pulando esta mensagem");
                            return;
                        }
                        
                        XingChenResponse.Choices choice = event.getChoices().get(0);
                        if (choice.getDelta() == null) {
                            log.warn("Delta vazio recebido, pulando esta mensagem");
                            return;
                        }
                        
                        String content = choice.getDelta().getContent();
                        if (content != null && !content.isEmpty()) {
                            sink.next(ChatResponse.builder()
                                    .generations(
                                            List.of(new Generation(AssistantMessage.builder()
                                                    .content(content)
                                                    .properties(Map.of("messageId", event.getId()))
                                                    .build())))
                                    .build());
                        }
                    }

                    @Override
                    public void onMessageEnd(XingChenResponse event) {
                        // Se nenhuma chamada de ferramenta foi disparada, este é de fato o ponto final
                        if (!hasToolCall[0]) {
                            log.debug("Stream inicial encerrado sem chamada de ferramenta; processo concluído");
                            sink.complete();
                        } else {
                            log.debug("Stream inicial encerrado, mas com chamada de ferramenta; aguardando conclusão do resume");
                        }
                    }

                    @Override
                    public void onFunctionCall(XingChenResponse event) {
                        // Marca que há chamada de ferramenta
                        hasToolCall[0] = true;
                        log.debug("Chamada de ferramenta disparada");
                        
                        // Verificação de segurança
                        if (event.getEventData() == null || event.getEventData().getValue() == null) {
                            log.error("EventData ou Value vazio; não é possível executar a chamada de ferramenta");
                            sink.error(new IllegalStateException("Dados de chamada de ferramenta inválidos"));
                            return;
                        }
                        
                        XingChenResponse.EventData eventData = event.getEventData();
                        String content = eventData.getValue().getContent();
                        if (content == null || content.isEmpty()) {
                            log.error("Conteúdo da chamada de ferramenta vazio");
                            sink.error(new IllegalStateException("Conteúdo da chamada de ferramenta vazio"));
                            return;
                        }
                        
                        content = content.replace("```json", "").replace("```", "").trim();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = JsonUtil.fromJson(content, Map.class);
                        
                        if (map == null || !map.containsKey("name")) {
                            log.error("Falha ao interpretar a chamada de ferramenta; não foi possível obter o nome da ferramenta: {}", content);
                            sink.error(new IllegalStateException("Formato de chamada de ferramenta inválido"));
                            return;
                        }
                        
                        List<AssistantMessage.ToolCall> toolCalls = List.of(
                                new AssistantMessage.ToolCall(
                                        "1",
                                        "function",
                                        (String) map.get("name"),
                                        JsonUtil.toJson(map.get("arguments")))
                        );
                        
                        // Obtém o conteúdo da mensagem (pode ser vazio)
                        String messageContent = "";
                        if (event.getChoices() != null && !event.getChoices().isEmpty() 
                                && event.getChoices().get(0).getDelta() != null) {
                            messageContent = event.getChoices().get(0).getDelta().getContent();
                            if (messageContent == null) {
                                messageContent = "";
                            }
                        }

                        AssistantMessage assistantMessage = AssistantMessage.builder()
                                .content(messageContent)
                                .properties(Map.of("messageId", event.getId()))
                                .toolCalls(toolCalls)
                                .build();

                        Generation generation = new Generation(assistantMessage);
                        ChatResponse chatResponse = ChatResponse.builder()
                                .generations(List.of(generation))
                                .build();

                        var toolExecutionResult = ToolCallingManager.builder().build()
                                .executeToolCalls(prompt, chatResponse);
                        
                        if (toolExecutionResult.returnDirect()) {
                            // Return tool execution result directly to the client.
                            sink.next(ChatResponse.builder().from(chatResponse)
                                    .generations(ToolExecutionResult.buildGenerations(toolExecutionResult))
                                    .build());
                            // Se retornar diretamente, é preciso concluir o stream
                            sink.complete();
                        } else {
                            // Send the tool execution result back to the model.
                            XingChenResume resume = XingChenResume.builder()
                                    .eventId(eventData.getEventId())
                                    .eventType("resume")
                                    .content("Operação bem-sucedida")
                                    .build();
                            // Repassa o sink ao método resume, para que a resposta do resume também seja enviada ao cliente
                            resume(resume, sink);
                        }
                    }

                    @Override
                    public void onError(XingChenResponse event) {
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

    public void resume(XingChenResume resume, reactor.core.publisher.FluxSink<ChatResponse> sink) {
        try {
            log.debug("Mensagem de resume do XingChen: {}", JsonUtil.toJson(resume));
            chatClient.resume(resume, new XingChenChatStreamCallback() {
                @Override
                public void onMessage(XingChenResponse event) {
                    log.info("Resume onMessage: {}", JsonUtil.toJson(event));
                    
                    // Verificação de segurança: garante que choices não seja vazio
                    if (event.getChoices() == null || event.getChoices().isEmpty()) {
                        log.warn("Resume recebeu choices vazio, pulando esta mensagem");
                        return;
                    }
                    
                    XingChenResponse.Choices choice = event.getChoices().get(0);
                    if (choice.getDelta() == null) {
                        log.warn("Resume recebeu delta vazio, pulando esta mensagem");
                        return;
                    }
                    
                    String content = choice.getDelta().getContent();
                    if (content != null && !content.isEmpty()) {
                        // Envia também a resposta do resume ao cliente
                        sink.next(ChatResponse.builder().generations(
                                        List.of(new Generation(AssistantMessage.builder()
                                                .content(content)
                                                .properties(Map.of("messageId", event.getId()))
                                                .build())))
                                .build());
                    }
                }

                @Override
                public void onMessageEnd(XingChenResponse event) {
                    log.info("Resume onMessageEnd, processo concluído: {}", JsonUtil.toJson(event));
                    // Processo de resume encerrado, notificando conclusão
                    sink.complete();
                }

                @Override
                public void onFunctionCall(XingChenResponse event) {
                    log.warn("Um novo FunctionCall foi disparado durante o resume; isso pode não ser o comportamento esperado: {}", JsonUtil.toJson(event));
                    // Se uma nova chamada de ferramenta for disparada após o resume, é necessário tratamento recursivo
                    // Mas esse caso é particular; por ora, apenas o aviso é registrado
                }

                @Override
                public void onError(XingChenResponse event) {
                    log.error("Erro no Resume: code={}, message={}", event.getCode(), event.getMessage());
                    sink.error(new IOException("Erro no Resume: " + event.getMessage()));
                }

                @Override
                public void onException(Throwable throwable) {
                    log.error("Exceção no Resume: {}", throwable.getMessage());
                    sink.error(throwable);
                }
            });
        } catch (IOException e) {
            log.error("Falha ao enviar a requisição de resume", e);
            sink.error(e);
        } catch (Exception e) {
            log.error("Exceção inesperada durante o processo de resume", e);
            sink.error(e);
        }
    }
}