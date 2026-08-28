package com.xiaozhi.ai.llm.memory;

import com.xiaozhi.ai.llm.factory.ChatModelFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import com.xiaozhi.common.model.bo.RoleBO;

import lombok.extern.slf4j.Slf4j;
/**
 * A desconexão atual de 10 segundos serve apenas para economizar energia; no cotidiano, uma conversa natural entre pessoas normalmente não é percebida como "uma conversa" com base em um intervalo tão curto quanto 10 segundos.
 * SessionID é um termo técnico, não um conceito que interessa ao usuário, assim como ligar e desligar o dispositivo também não são.
 * Já que o usuário não se importa de fato com o session id, é possível agrupar em lotes (batch) para exibir aos pais/responsáveis pelo usuário.
 * Considerando que cada vez mais APIs na nuvem suportam Cache, tornar esse Batch maior traz melhores benefícios.
 *
 * - Cenários que podem ocorrer
 *
 * 1. A Conversation tem rodadas de diálogo longas demais, sendo necessário dividi-la em vários Batches para fazer o Summarize.
 * 2. A Conversation tem rodadas de diálogo curtas demais, não valendo sequer a pena fazer um Summarize.
 * 3. Após realizar um summarize, pouco tempo depois, ainda com poucas rodadas de diálogo, a Conversation é encerrada.
 * 4. Pouco tempo depois de a Conversation ser encerrada, o usuário faz login novamente e continua a conversa.
 *
 * A introdução de CONVERSATION_INTERVAL_HOURS visa suportar o conceito de "uma conversa" do ponto de vista de negócio (independente do session_id de conexão em sentido técnico).
 */
@Slf4j
@Service
public class SummaryConversationFactory implements ConversationFactory{
    private final ChatMemory chatMemory;
    private final SystemPromptTemplate SUMMARIZER_SYSTEM_PROMPT_TEMPLATE;
    private final PromptTemplate initSummarizerPromptTemplate ;
    private final PromptTemplate againSummarizerPromptTemplate ;

    // No cenário de Summary é usado apenas chatModel.call(String), sem passar ToolCallbacks; nenhuma chamada de ferramenta é acionada.
    @Autowired
    private  ChatModelFactory chatModelFactory;

    @Value("${conversation.max-messages:8}")
    private int maxMessages;
    @Value("${conversation.batch-size:4}")
    private int batchSize;

    @Autowired
    public SummaryConversationFactory(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
        SUMMARIZER_SYSTEM_PROMPT_TEMPLATE = new SystemPromptTemplate(new ClassPathResource("/prompts/system_prompt_with_summary.md", getClass()));
        this.initSummarizerPromptTemplate = PromptTemplate.builder()
                .renderer(StTemplateRenderer.builder().startDelimiterToken('$').endDelimiterToken('$').build())
                .resource(new ClassPathResource("/prompts/init_summarizer.md", getClass()))
                .build();
        this.againSummarizerPromptTemplate = PromptTemplate.builder()
                .renderer(StTemplateRenderer.builder().startDelimiterToken('$').endDelimiterToken('$').build())
                .resource(new ClassPathResource("/prompts/again_summarizer.md", getClass()))
                .build();
    }

    @Override
    public Conversation initConversation(String ownerId, Integer userId, RoleBO role, String sessionId) {
        log.debug("Inicializando os parâmetros básicos de configuração do SummaryConversation, maxMessages: {}, batchSize: {}",maxMessages,batchSize);

        ChatModel chatModel = chatModelFactory.getChatModel(role);
        // Durante testes, batchSeconds pode ser reduzido. Para produção, considerar inicialmente os valores padrão: 20, 16, 60.
        return SummaryConversation.builder()
                .ownerId(ownerId)
                .roleId(role.getRoleId())
                .roleDesc(role.getRoleDesc())
                .userId(userId)
                .sessionId(sessionId)
                .maxMessages(maxMessages)
                .batchSize(batchSize)
                .chatMemory(chatMemory)
                .chatModel(chatModel)
                .initSummarizerPromptTemplate(initSummarizerPromptTemplate)
                .againSummarizerPromptTemplate(againSummarizerPromptTemplate)
                .build();
    }
}
