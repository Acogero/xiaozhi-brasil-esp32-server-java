package com.xiaozhi.server.web.chat;

import com.xiaozhi.ai.llm.factory.ChatModelFactory;
import com.xiaozhi.ai.llm.memory.ChatMemory;
import com.xiaozhi.ai.llm.memory.Conversation;
import com.xiaozhi.ai.llm.memory.ConversationContext;
import com.xiaozhi.ai.llm.memory.MessageTimeMetadata;
import com.xiaozhi.ai.llm.memory.MessageWindowConversation;
import com.xiaozhi.common.model.ChatToken;
import com.xiaozhi.common.model.bo.MessageBO;
import com.xiaozhi.common.model.bo.RoleBO;
import com.xiaozhi.message.service.MessageService;
import com.xiaozhi.role.service.RoleService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
/**
 * Serviço de chat Web: fornece diálogo com IA em streaming para clientes Web somente texto.
 * Implementação leve, sem envolver componentes de áudio como STT/TTS/Player
 */
@Slf4j
@Service
public class WebChatService {
    @Resource
    private ChatModelFactory chatModelFactory;
    @Resource
    private RoleService roleService;
    @Resource
    private ChatMemory chatMemory;
    @Resource
    private MessageService messageService;

    @Value("${conversation.max-messages:16}")
    private int maxMessages;

    /**
     * Mapeamento sessionId → Conversation
     */
    private final ConcurrentHashMap<String, Conversation> conversations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ChatModel> chatModels = new ConcurrentHashMap<>();

    /**
     * Abre uma sessão de chat Web.
     * Quando {@code resumeSessionId} é nulo, cria uma nova sessão; quando informado, tenta retomar uma sessão existente.
     * Ao retomar, verifica a posse (mesmo userId e source='web'), evitando uso indevido de sessões de dispositivo ou acesso entre usuários.
     *
     * @param userId           ID do usuário logado
     * @param roleId           ID do papel
     * @param resumeSessionId  ID da sessão a retomar, pode ser null
     * @return sessionId
     */
    public String openSession(Integer userId, Integer roleId, String resumeSessionId) {
        String ownerId = "web:" + userId;

        RoleBO role = roleService.getBO(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Papel não encontrado: " + roleId);
        }

        String sessionId;
        if (StringUtils.hasText(resumeSessionId)) {
            assertSessionOwnedByUser(resumeSessionId, userId);
            sessionId = resumeSessionId;
        } else {
            sessionId = UUID.randomUUID().toString();
        }

        // Inicializa a Conversation: no cenário Web, sempre carrega por sessionId (nova sessão fica vazia, retomada traz o histórico).
        Conversation conversation = MessageWindowConversation.builder()
                .chatMemory(chatMemory)
                .maxMessages(maxMessages)
                .ownerId(ownerId)
                .roleId(role.getRoleId())
                .roleDesc(role.getRoleDesc())
                .userId(userId)
                .sessionId(sessionId)
                .sessionScoped(true)
                .build();
        conversations.put(sessionId, conversation);

        // Inicializa o ChatModel
        ChatModel chatModel = chatModelFactory.getChatModel(role);
        chatModels.put(sessionId, chatModel);

        log.info("Sessão de chat Web criada: sessionId={}, userId={}, roleId={}, resume={}",
                sessionId, userId, roleId, StringUtils.hasText(resumeSessionId));
        return sessionId;
    }

    /**
     * Sobrecarga de conveniência para criar uma nova sessão.
     */
    public String openSession(Integer userId, Integer roleId) {
        return openSession(userId, roleId, null);
    }

    /**
     * Verifica se o sessionId a ser retomado pertence a uma sessão Web do usuário atual.
     * Lança IllegalArgumentException em caso de divergência.
     */
    private void assertSessionOwnedByUser(String sessionId, Integer userId) {
        List<MessageBO> recent = messageService.listHistory(sessionId, 1);
        if (recent.isEmpty()) {
            throw new IllegalArgumentException("Sessão não encontrada ou já foi limpa: " + sessionId);
        }
        MessageBO first = recent.get(0);
        if (!MessageBO.SOURCE_WEB.equals(first.getSource())) {
            throw new IllegalArgumentException("Só é possível retomar sessões originadas na Web: " + sessionId);
        }
        if (!userId.equals(first.getUserId())) {
            throw new IllegalArgumentException("A sessão não pertence ao usuário atual: " + sessionId);
        }
    }

    /**
     * Chat em streaming: recebe o texto do usuário e retorna o fluxo de ChatToken da resposta da IA (incluindo o processo de raciocínio e a resposta final),
     * persistindo as mensagens de user e assistant ao final.
     *
     * @param sessionId ID da sessão
     * @param text      texto informado pelo usuário
     * @return fluxo de ChatToken; o frontend pode diferenciar thinking/content pelo campo type
     */
    public Flux<ChatToken> chatStream(String sessionId, String text) {
        Conversation conversation = conversations.get(sessionId);
        ChatModel chatModel = chatModels.get(sessionId);
        if (conversation == null || chatModel == null) {
            return Flux.error(new IllegalStateException("Sessão não encontrada ou expirada: " + sessionId));
        }

        // Cenário Web: UserMessage em texto puro + metadata com timestamp;
        // A camada de projeção da Conversation monta o prefixo [timestamp] texto antes de enviar ao LLM.
        // Sem speaker/emotion, portanto não anexa MessageMetadataBO.
        LocalDateTime userCreatedAt = LocalDateTime.now();
        Instant userInstant = userCreatedAt.atZone(ZoneId.systemDefault()).toInstant();
        UserMessage userMessage = new UserMessage(text);
        MessageTimeMetadata.setTimeMillis(userMessage, userInstant);
        conversation.add(userMessage);

        // Cenário Web não possui posição
        List<Message> messages = conversation.messages(ConversationContext.EMPTY);

        Prompt prompt = new Prompt(messages);

        StringBuilder fullResponse = new StringBuilder();

        return chatModel.stream(prompt)
                .mapNotNull(ChatResponse::getResult)
                .mapNotNull(Generation::getOutput)
                .flatMap(message -> {
                    List<ChatToken> tokens = new ArrayList<>();
                    Object reasoning = message.getMetadata().get("reasoningContent");
                    if (reasoning instanceof String r && !r.isEmpty()) {
                        tokens.add(ChatToken.thinking(r));
                    }
                    String content = message.getText();
                    if (content != null && !content.isEmpty()) {
                        tokens.add(ChatToken.content(content));
                    }
                    return Flux.fromIterable(tokens);
                })
                .doOnNext(token -> {
                    // Acumula apenas o conteúdo da resposta final; o processo de raciocínio não é persistido
                    if (token.isContent()) {
                        fullResponse.append(token.text());
                    }
                })
                .doOnComplete(() -> {
                    if (fullResponse.isEmpty()) {
                        return;
                    }
                    String reply = fullResponse.toString();
                    conversation.add(new AssistantMessage(reply));
                    // Persiste o texto puro (o prefixo é montado pela camada de projeção da Conversation quando necessário, mantendo o banco limpo)
                    persistTurn(conversation, text, userCreatedAt, reply, LocalDateTime.now());
                })
                .doOnError(e -> log.error("Falha na resposta em streaming do chat Web: sessionId={}", sessionId, e));
    }

    /**
     * Grava no banco as mensagens de user + assistant de uma rodada de diálogo Web (source='web').
     * Extraído em método separado para que um erro aqui não afete a conclusão do streaming.
     */
    private void persistTurn(Conversation conversation, String userText, LocalDateTime userCreatedAt,
                             String assistantText, LocalDateTime assistantCreatedAt) {
        try {
            MessageBO userBO = buildMessageBO(conversation, MessageBO.SENDER_USER, userText, userCreatedAt);
            MessageBO assistantBO = buildMessageBO(conversation, MessageBO.SENDER_ASSISTANT, assistantText, assistantCreatedAt);
            messageService.saveAll(List.of(userBO, assistantBO));
        } catch (Exception e) {
            log.error("Falha ao persistir mensagem do chat Web: sessionId={}", conversation.sessionId(), e);
        }
    }

    private MessageBO buildMessageBO(Conversation conversation, String sender, String content, LocalDateTime createTime) {
        MessageBO bo = new MessageBO();
        bo.setUserId(conversation.getUserId());
        bo.setDeviceId(conversation.getOwnerId());
        bo.setSessionId(conversation.sessionId());
        bo.setSource(MessageBO.SOURCE_WEB);
        bo.setSender(sender);
        bo.setMessage(content);
        bo.setRoleId(conversation.getRoleId());
        bo.setMessageType(MessageBO.MESSAGE_TYPE_NORMAL);
        bo.setCreateTime(createTime);
        return bo;
    }

    /**
     * Fecha a sessão de chat Web, liberando recursos
     */
    public void closeSession(String sessionId) {
        conversations.remove(sessionId);
        chatModels.remove(sessionId);
        log.info("Sessão de chat Web encerrada: sessionId={}", sessionId);
    }

    /**
     * Verifica se a sessão existe
     */
    public boolean hasSession(String sessionId) {
        return conversations.containsKey(sessionId);
    }
}
