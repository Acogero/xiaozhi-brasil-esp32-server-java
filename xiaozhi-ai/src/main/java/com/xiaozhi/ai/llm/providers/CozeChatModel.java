package com.xiaozhi.ai.llm.providers;

import cn.hutool.core.bean.BeanUtil;
import com.coze.openapi.client.chat.*;
import com.coze.openapi.client.chat.model.*;
import com.coze.openapi.client.connversations.message.model.Message;
import com.coze.openapi.client.connversations.message.model.MessageType;
import com.coze.openapi.service.auth.TokenAuth;
import com.coze.openapi.service.service.CozeAPI;

import io.reactivex.Flowable;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementação do serviço LLM Coze
 */
@Slf4j
public class CozeChatModel implements ChatModel {

    private final CozeAPI coze;
    private final String botId;

    public static final String PROVIDER_NAME = "coze";


    /**
     * Construtor
     * @param apiSecret Chave de API do Coze
     * @param model     Nome do modelo (não utilizado no Coze)
     */
    public CozeChatModel(String apiSecret, String model) {

        // Usa apiSecret como access_token
        TokenAuth authCli = new TokenAuth(apiSecret);

        // Usa o endpoint ou o endereço padrão da API Coze
        String baseUrl = "https://api.coze.cn";

        // Inicializa o cliente da API Coze
        this.coze = new CozeAPI.Builder()
                .baseURL(baseUrl)
                .auth(authCli)
                .readTimeout(60000) // timeout de 60 segundos
                .build();

        // Linha de configuração do coze no banco de dados: o campo appId já é usado como token, e o campo configName atua, na prática, como o botId do coze — equivalente ao parâmetro model.
        this.botId = model;

        log.info("Inicializando o serviço Coze, botId: {}, baseUrl: {}", botId, baseUrl);
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        var messages = prompt.getInstructions();
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("A lista de mensagens não pode ser vazia");
        }

        // Converte o formato das mensagens para o formato exigido pela API Coze
        List<Message> cozeMessages = convertToCozeMessages(messages);

        String userId = resolveUserId(prompt);

        // Cria a requisição de chat
        CreateChatReq req = CreateChatReq.builder()
                .botID(botId)
                .userID(userId)
                .messages(cozeMessages)
                .build();

        CreateChatResp chatResp = coze.chat().create(req);
        // Chat chat = chatResp.getChat();
        // get chat id and conversationID
        // String chatID = chat.getID();
        // String conversationID = chat.getConversationID();
        /*
         * Step two, poll the result of chat
         * Assume the development allows at most one chat to run for 10 seconds. If it
         * exceeds 10 seconds,
         * the chat will be cancelled.
         * And when the chat status is not completed, poll the status of the chat once
         * every second.
         * After the chat is completed, retrieve all messages in the chat.
         */
        long timeout = 10L;
        long start = System.currentTimeMillis();

        // the developer can also set the timeout.
        try {
            ChatPoll chatPoll = coze.chat().createAndPoll(req, timeout);
            log.debug(chatPoll.toString());
            var message = chatPoll.getMessages().getLast();
            Map<String, Object> messageMetadata = Optional.ofNullable(message.getMetaData())
                    .map(metaData -> new HashMap<String, Object>(metaData))
                    .orElse(new HashMap<>());
            var assistantMessage = AssistantMessage.builder().content(message.getContent()).properties(messageMetadata).build();
            var generation = new Generation(assistantMessage,
                    ChatGenerationMetadata.builder().metadata(BeanUtil.beanToMap(chatPoll.getChat())).build());
            log.info("Tempo decorrido: {}ms", System.currentTimeMillis() - start);
            return ChatResponse.builder().generations(List.of(generation)).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        var messages = prompt.getInstructions();
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("A lista de mensagens não pode ser vazia");
        }

        // Converte o formato das mensagens para o formato exigido pela API Coze
        List<Message> cozeMessages = convertToCozeMessages(messages);

        String userId = resolveUserId(prompt);
        // Cria a requisição de chat
        CreateChatReq req = CreateChatReq.builder()
                .botID(botId)
                .userID(userId)
                .messages(cozeMessages)
                .build();

        // Envia a requisição
        try {
            Flowable<ChatEvent> resp = coze.chat().stream(req);
            // Converte para Reactor Flux
            Flux<ChatEvent> flux = Flux.from(resp);

            Flux<ChatResponse> chatResponse = flux
                    .filter(event -> event != null) // Filtra eventos null
                    .map(event -> {
                        List<AssistantMessage.ToolCall> toolCalls = List.of();
                        String content = "";

                        if (ChatEventType.CONVERSATION_MESSAGE_DELTA.equals(event.getEvent())) {
                            Message message = event.getMessage();
                            content = Optional.ofNullable(message)
                                    .map(Message::getContent)
                                    .orElse("");
                        }

                        if (ChatEventType.CONVERSATION_CHAT_REQUIRES_ACTION.equals(event.getEvent())) {
                            List<ChatToolCall> toolCallList = event.getChat().getRequiredAction()
                                    .getSubmitToolOutputs().getToolCalls();

                            toolCalls = toolCallList
                                    .stream()
                                    .map(toolCall -> new AssistantMessage.ToolCall(
                                            toolCall.getID(),
                                            "function",
                                            toolCall.getFunction().getName(),
                                            toolCall.getFunction().getArguments()))
                                    .toList();
                        }

                        if (ChatEventType.CONVERSATION_CHAT_COMPLETED.equals(event.getEvent())) {
                            Message message = event.getMessage();
                            if (message != null && MessageType.FOLLOW_UP.equals(message.getType())) {
                                log.debug(message.getContent());
                            } else if (event.getChat() != null && event.getChat().getUsage() != null) {
                                log.debug("Token usage:{}", event.getChat().getUsage().getTokenCount());
                            }
                        }

                        if (ChatEventType.DONE.equals(event.getEvent())) {
                            coze.shutdownExecutor();
                        }

                        var message = event.getMessage();

                        Map<String, Object> messageMetadata = Optional.ofNullable(message)
                                .map(Message::getMetaData)
                                .map(metaData -> {
                                    Map<String, Object> result = new HashMap<>();
                                    if (metaData != null) {
                                        result.putAll(metaData);
                                    }
                                    return result;
                                })
                                .orElse(new HashMap<>());

                        var assistantMessage = AssistantMessage.builder().content(content).properties(messageMetadata).toolCalls(toolCalls).build();

                        Map<String, Object> chatMetadata = Optional.ofNullable(event.getChat())
                                .map(chat -> {
                                    Map<String, Object> beanMap = BeanUtil.beanToMap(chat);
                                    // Filtra valores null
                                    return beanMap.entrySet().stream()
                                            .filter(entry -> entry.getValue() != null)
                                            .collect(Collectors.toMap(
                                                    Map.Entry::getKey,
                                                    Map.Entry::getValue));
                                })
                                .orElse(new HashMap<>());

                        ChatGenerationMetadata generationMetadata = ChatGenerationMetadata.builder()
                                .metadata(chatMetadata)
                                .build();

                        var generation = new Generation(assistantMessage, generationMetadata);
                        return new ChatResponse(List.of(generation));
                    });

            final ChatModelObservationContext observationContext = ChatModelObservationContext.builder()
                    .prompt(prompt)
                    .provider(PROVIDER_NAME)
                    .build();

            return new MessageAggregator().aggregate(chatResponse, observationContext::setResponse);
        } catch (Exception e) {
            log.error("Erro ao criar a requisição em streaming: {}", e.getMessage(), e);
            return Flux.error(e);
        }
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
     * Converte o formato de mensagem genérico para o formato exigido pela API Coze
     *
     * @param messages Lista de mensagens no formato genérico
     * @return Lista de mensagens no formato Coze
     */
    private List<Message> convertToCozeMessages(List<org.springframework.ai.chat.messages.Message> messages) {
        List<Message> cozeMessages = new ArrayList<>();

        for (org.springframework.ai.chat.messages.Message msg : messages) {
            Map<String, String> metadata = msg.getMetadata().entrySet()
                    .stream()
                    .filter(e -> e.getValue() != null)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().toString()));
            switch (msg.getMessageType()) {
                case USER:
                    cozeMessages.add(Message.buildUserQuestionText(msg.getText(), metadata));
                    break;
                case ASSISTANT:
                    cozeMessages.add(Message.buildAssistantAnswer(msg.getText(), metadata));
                    break;
                default:
                    // O prompt de sistema do coze não é definido aqui por padrão; deve ser configurado no próprio coze
            }
        }

        return cozeMessages;
    }

}