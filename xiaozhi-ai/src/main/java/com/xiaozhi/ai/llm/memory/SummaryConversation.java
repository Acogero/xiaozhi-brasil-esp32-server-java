package com.xiaozhi.ai.llm.memory;

import com.xiaozhi.common.model.bo.SummaryBO;

import lombok.Builder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
/**
 * Implementa o resumo (summary) da conversa
 * @see org.springframework.ai.chat.memory.MessageWindowChatMemory
 * @see # org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor
 *
 * Premissas básicas do design:
 * 1. O peso dado ao fator custo do histórico de chat é muito maior do que o peso dado à confiabilidade;
 * 2. Desde que o requisito de custo acima seja atendido, procura-se ao máximo garantir a qualidade do chat.
 *
 * No futuro, pode-se considerar a criação de um Advisor customizado para injetar um SystemMessage no Prompt.
 * Aqui fica definido que a SystemMessage não é armazenada em messages; seu template é registrado como uma variável separada.
 *
 * De onde vem a Conversation?
 * 1. Uma nova conversa entre o usuário e o modelo cria uma conversa ainda não persistida no banco. Esta é a Conversation inicial.
 * 2. Inicializada a partir do banco de dados. Esta é uma Conversation já persistida.
 *
 * Em termos de custo: poder computacional de GPU do modelo >> IO de armazenamento > uso de memória > espaço de armazenamento > CPU.
 * Para manter a coerência de sentido da conversa, todas as mensagens que não estão no Prompt devem passar por resumo. Como o resumo exige uma chamada ao modelo, não é viável resumir mensagem por mensagem — é necessário resumir em lote.
 * @author Able
 */

@Slf4j
public class SummaryConversation extends Conversation {
    private static final int CONVERSATION_INTERVAL_HOURS = 1;
    private final PromptTemplate initSummarizerPromptTemplate ;
    private final PromptTemplate againSummarizerPromptTemplate ;
    private final ChatMemory chatMemory;
    private final ChatClient chatClient;
    private final Object summaryLock = new Object();
    // Não deve mudar em tempo de execução, para evitar erros de cálculo

    private final int maxMessages ;
    // Não deve mudar em tempo de execução, para evitar erros de cálculo
    private final int batchSize;

    // Resumo das mensagens
    private SummaryBO lastSummary = null;
    private boolean summarizing = false;

    @Builder
    public SummaryConversation(String ownerId, Integer roleId, String sessionId, String roleDesc, Integer userId,
                               PromptTemplate initSummarizerPromptTemplate, PromptTemplate againSummarizerPromptTemplate,
                               ChatMemory chatMemory, ChatModel chatModel, int maxMessages, int batchSize){
        super(ownerId, roleId, sessionId, roleDesc, userId);
        Assert.notNull(initSummarizerPromptTemplate, "initSummarizerPromptTemplate must not be null");
        this.initSummarizerPromptTemplate = initSummarizerPromptTemplate;

        Assert.notNull(againSummarizerPromptTemplate, "againSummarizerPromptTemplate must not be null");
        this.againSummarizerPromptTemplate = againSummarizerPromptTemplate;

        Assert.state(maxMessages>0, "maxMessages must be greater than 0");
        this.maxMessages = maxMessages;

        Assert.state(batchSize>0, "batchSize must be greater than 0");
        this.batchSize = batchSize;

        Assert.notNull(chatMemory, "chatMemory must not be null");
        this.chatMemory = chatMemory;

        Assert.notNull(chatModel, "chatModel must not be null");
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors()
                .build();

        // Ao criar uma nova Conversation, é possível carregar um Summary já existente.
        this.lastSummary = chatMemory.findLastSummary(getOwnerId(), getRoleId());
        if(lastSummary == null){
            List<Message> history = chatMemory.find(getOwnerId(), getRoleId(), maxMessages);
            log.info("{} ainda não possui summary no histórico; carregando {} mensagens comuns no contexto da conversa", getOwnerId(), history.size());
            synchronized (summaryLock) {
                super.messages.addAll(history);
            }
            // Se a última mensagem tiver mais de 1 hora e houver mensagens suficientes, gera um summary para compactar o contexto
            if (history.size() >= 2) {
                Instant lastMessageTime = MessageTimeMetadata.getTimeMillis(history.getLast());
                if (Duration.between(lastMessageTime, Instant.now()).toHours() >= CONVERSATION_INTERVAL_HOURS) {
                    log.info("A última mensagem de {} já passou de {} horas; gerando summary para compactar o contexto", getOwnerId(), CONVERSATION_INTERVAL_HOURS);
                    summarize(true);
                }
            }
        }else {
            List<Message> history = chatMemory.find(getOwnerId(), getRoleId(), lastSummary.getLastMessageTimestamp());
            log.info("Carregando as mensagens não resumidas de {} ({} mensagens) como histórico da conversa", getOwnerId(), history.size());
            synchronized (summaryLock) {
                super.messages.addAll(history);
            }
            if (Duration.between(lastSummary.getLastMessageTimestamp(), Instant.now()).toHours() >= CONVERSATION_INTERVAL_HOURS
                    && history.size() >= 2) {
                log.info("O último summary de {} já passou de 1 hora, mas ainda há mensagens restantes não resumidas; gerando um novo summary", getOwnerId());
                summarize(true);
            }
        }
    }

    /**
     * Adiciona mensagem
     * Consideração futura: herdar e encapsular UserMessage e AssistantMessage como UserMessageWithTime, AssistantMessageWithTime
     * @param message
     */
    @Override
    public void add(Message message) {
        synchronized (summaryLock) {
            super.add(message);
        }
        // Ao atingir o limite, aciona o modelo para gerar o resumo. Só é acionado ao adicionar uma AssistantMessage (para evitar disparos duplicados)
        if (message instanceof AssistantMessage) {
            summarize();
        }
    }

    private void summarize() {
        summarize(false);
    }

    private void summarize(boolean force) {
        List<Message> needSummaryMessages;
        int size;
        int actualBatchSize;
        synchronized (summaryLock) {
            if (summarizing) {
                return;
            }
            size = messages.size();
            if (size == 0 || (!force && size < maxMessages)) {
                return;
            }
            actualBatchSize = Math.min(batchSize, size);
            if (actualBatchSize <= 0) {
                return;
            }
            needSummaryMessages = new ArrayList<>(messages.subList(0, actualBatchSize));
            summarizing = true;
        }
        log.info("current conversation message size:{}, batch size to summary:{}", size, actualBatchSize);
        Thread.startVirtualThread(() -> summaryMessages(needSummaryMessages));
    }

    protected void summaryMessages(List<Message> needSummaryMessages) {
        // 1. Process memory messages as a string.
        String memory = MessageHistoryFormatter.format(needSummaryMessages);

        // 2. Monta o prompt
        String lastSummaryText;
        synchronized (summaryLock) {
            lastSummaryText = lastSummary == null ? null : lastSummary.getSummary();
        }
        String factExtractPrompt;
        if (StringUtils.hasText(lastSummaryText)) {
            factExtractPrompt = againSummarizerPromptTemplate.render(Map.of(
                    "last_summary", lastSummaryText,
                    "conversation", memory
            ));
        } else {
            factExtractPrompt = initSummarizerPromptTemplate.render(Map.of(
                    "datetime", LocalDate.now().toString(),
                    "conversation", memory
            ));
        }

        try {
            // 3. Call the model.
            log.info("Chamando o modelo para gerar o resumo: {}", factExtractPrompt);

            String factExtract = chatClient.prompt()
                    .user(factExtractPrompt)
                    .call()
                    .content();
            log.info("O modelo extraiu da conversa anotações importantes do usuário: {}", factExtract);

            // 4. Persiste no banco de dados.
            SummaryBO newSummary = new SummaryBO()
                    .setDeviceId(getOwnerId())
                    .setRoleId(getRoleId())
                    .setLastMessageTimestamp(MessageTimeMetadata.getTimeMillis(needSummaryMessages.getLast()).truncatedTo(ChronoUnit.SECONDS))
                    .setSummary(factExtract)
                    .setCreateTime(Instant.now());
            chatMemory.save(newSummary);

            synchronized (summaryLock) {
                // 5. Remove as mensagens já processadas
                messages.removeAll(needSummaryMessages);
                this.lastSummary = newSummary;
                summarizing = false;
            }
            summarize();
        } catch (Exception e) {
            log.error("Falha ao resumir a conversa de {}", getOwnerId(), e);
            synchronized (summaryLock) {
                summarizing = false;
            }
        }
    }

    public List<Message> messages(ConversationContext context) {
        List<Message> messageSnapshot;
        SummaryBO summarySnapshot;
        synchronized (summaryLock) {
            messageSnapshot = new ArrayList<>(messages);
            summarySnapshot = lastSummary;
        }
        // Novo objeto de lista de mensagens, para evitar poluir o objeto de lista original durante o uso
        List<Message> historyMessages = new ArrayList<>();
        var roleSystemMessage = roleSystemMessage(context);
        if(roleSystemMessage.isPresent()){
            historyMessages.add(roleSystemMessage.get());
        }
        if(summarySnapshot != null && StringUtils.hasText(summarySnapshot.getSummary())){
            // Múltiplas SystemMessage já foram validadas como funcionais nos principais modelos (OpenAI, Qwen, DeepSeek)
            historyMessages.add(new SystemMessage("A seguir está um resumo do conteúdo recente da sua conversa com o usuário:\n" + summarySnapshot.getSummary()));
        }
        historyMessages.addAll(messageSnapshot);
        // UserMessage é montada com uma cópia prefixada, de acordo com a metadata, para uso pelo LLM
        return historyMessages.stream().map(UserMessageAssembler::assemble).toList();
    }

    @Override
    public List<Message> messages() {
        return messages(ConversationContext.EMPTY);
    }
}
