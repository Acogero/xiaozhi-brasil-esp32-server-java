package com.xiaozhi.dialogue.runtime;

import com.xiaozhi.common.model.ChatToken;
import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.communication.common.SessionManager;
import com.xiaozhi.ai.llm.memory.Conversation;
import com.xiaozhi.ai.llm.memory.ConversationContext;
import com.xiaozhi.dialogue.playback.Player;
import com.xiaozhi.dialogue.playback.Synthesizer;
import com.xiaozhi.ai.stt.SttService;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.slf4j.Slf4j;
/**
 * Personagem, avatar virtual, descreve os atributos e comportamentos do papel. Entidade de domínio: CharacterRole (papel de chat, Persona); gerencia o histórico do diálogo, as chamadas de ferramentas do diálogo etc.
 * Agrega o ChatModel, o TTS (Synthesizer) e o Player
 *
 * Persona tem principalmente duas relações com o ChatSession:
 * Primeiro, ao receber uma mensagem, ela precisa ser transmitida do ChatSession para o Persona, que então a repassa ao ChatModel.
 * Segundo, ao enviar uma mensagem, ela precisa ser transmitida do Persona para o ChatSession.
 *
 * O Path do arquivo de áudio do usuário é associado ao DialogueTurn via ChatSession.getUserAudioPath(),
 * e o DialogueTurn é construído como variável local dentro do método chatStream() (já implementado).
 *
 * Diversos eventos em pontos distintos do ciclo de vida:
 * 1. Ao receber o UserSpeech, quando a fala completa é obtida;
 * 2. Quando o ASR reconhece o UserText, após o reconhecimento de voz (STT), obtendo o texto.
 * 3. Quando o LLM responde com o AssistantText, após a geração da mensagem pelo LLM, obtendo os PromptTokens.
 * 4. TTS sintetiza o AssistantSpeech,
 * 5. O Player termina de reproduzir a fala.
 *
 * Persona e Conversation pertencem ao Domain, não à Infrastructure, e não tratam da persistência.
 * A persistência é tratada pelo callback PersonaListener.onDialogueTurn(DialogueTurn).
 * ConversationIdentifier = deviceId + sessionId + roleId
 */
@Slf4j
@Builder(toBuilder = true)
public class Persona {

    /**
     * O ToolContext transmite o sessionId em vez do ChatSession inteiro, evitando problemas de serialização.
     * O XiaoZhiToolCallingManager é responsável por restaurá-lo para ChatSession via SessionManager antes de repassá-lo à Function.
     */
    public static final String TOOL_CONTEXT_SESSION_ID_KEY = "sessionId";

    private final SessionManager sessionManager;
    
    @Setter
    private String sessionId;

    private PersonaListener listener;

    @Getter
    private SttService sttService;

    /**
     * Classe de implementação concreta que se comunica com o Provider de LLM
     */
    private ChatModel chatModel;
    private GoodbyeMessageSupplier goodbyeMessages;

    @Getter
    private Synthesizer synthesizer;

    @Getter
    private Player player;

    /**
     * Em um determinado momento, uma Session tem apenas um Conversation ativo.
     * Ao trocar de papel, o Conversation deve ser liberado e recriado. A troca de papel geralmente não é frequente.
     */
    @Getter
    private Conversation conversation;

    /**
     * Lista de callbacks de ferramentas, passada pelo DialogueContext no momento da construção pelo PersonaFactory.
     * chatStream() obtém a lista de ferramentas a partir deste campo, fazendo com que o Persona não dependa mais de session.getToolCallbacks().
     */
    @Builder.Default
    private List<ToolCallback> toolCallbacks = new ArrayList<>();


    // O callback PersonaListener implementa a separação entre o núcleo e o auxiliar: o Persona apenas notifica "o que aconteceu"; a persistência e o monitoramento são implementados externamente.

    /**
     * Obtém o ChatSession
     */
    private ChatSession getSession() {
        return sessionManager.getSession(sessionId);
    }

    /**
     * Trata a consulta do usuário (modo streaming)
     * @param userMessage         mensagem do usuário
     * @param useFunctionCall se deve usar chamada de função
     */
    private Flux<ChatResponse> chatStream(Instant now, UserMessage userMessage, boolean useFunctionCall) {
        // userSpeechPath é obtido da session, evitando a propagação do parâmetro por várias camadas
        Path userSpeechPath = getSession().getUserAudioPath();

        // time to first token, que também deve ser, na prática, o timestamp createdAt do AssistantMessage.
        // Quando o ChatModel termina a geração, o sintetizador de voz e o player já estão em funcionamento. Mas antes da geração do primeiro Token, o sintetizador e o player ainda não começaram a trabalhar.
        // Ao gerar o arquivo, o player também precisa de um ID associado ao AssistantMessage; o arquivo de áudio em disco não pode ser criado em sendStart.
        AtomicReference<Instant> ttft = new AtomicReference<>(null);

        String ownerId = conversation.getOwnerId();

        // Obtém a lista de ferramentas em tempo real a partir do ToolsSessionHolder (incluindo ferramentas MCP do dispositivo registradas posteriormente)
        List<ToolCallback> liveTools = getSession().getToolsSessionHolder().getAllFunction();

        // Camada 3: pré-seleção do subconjunto de ferramentas via Embedding
        List<ToolCallback> effectiveTools = useFunctionCall ? liveTools : new ArrayList<>();

        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(effectiveTools)
                .toolContext(TOOL_CONTEXT_SESSION_ID_KEY, sessionId)
                .toolContext("deviceId", ownerId)
                .toolContext("conversationTimestamp", now.toEpochMilli())
                .build();

        conversation.add(userMessage);

        // Constrói o contexto de execução
        ChatSession currentSession = getSession();
        String location = currentSession.getDevice() != null ? currentSession.getDevice().getLocation() : null;
        ConversationContext ctx = new ConversationContext(location);
        List<Message> messages = conversation.messages(ctx);
        Prompt prompt = new Prompt(messages, chatOptions);

        Flux<ChatResponse> chatFlux = chatModel.stream(prompt)
            .doOnError(error -> {
                listener.onError(error);
            });
        chatFlux = chatFlux.doOnNext(chatResponse -> {
            Instant assistantMessageCreatedAt = Instant.now();
            boolean isFirst = ttft.compareAndSet(null, assistantMessageCreatedAt);
            if (isFirst) {
                if (player.getOpusRecorder() != null) {
                    player.getOpusRecorder().setAssistantMessageCreatedAt(assistantMessageCreatedAt);
                }
            }
        });
        return new MessageAggregator().aggregate(chatFlux, chatResponse -> {
            var toolCallDetails = getSession().drainToolCallDetails();
            // Obtém do DialogueContext a mensagem intermediária da cadeia real de chamadas de ferramentas feitas pelo modelo
            AssistantMessage toolCallAssistantMsg = getSession().getDialogueContext().drainToolCallAssistantMessage();
            ToolResponseMessage toolResponseMsg = getSession().getDialogueContext().drainToolResponseMessage();

            // Mescla todas as tool chains desta rodada: a cadeia real de chamadas do modelo (a ordem é a ordem de persistência)
            List<ToolChainPair> allChains = new ArrayList<>();
            if (toolCallAssistantMsg != null && toolResponseMsg != null) {
                allChains.add(new ToolChainPair(toolCallAssistantMsg, toolResponseMsg));
            }

            DialogueTurn dialogueTurn = DialogueTurn.builder()
                    .userMessage(userMessage)
                    .chatResponse(chatResponse)
                    .conversation(conversation)
                    .userMessageCreatedAt(now)
                    .userSpeechPath(userSpeechPath)
                    .assistantMessageCreatedAt(ttft.get())
                    .toolCallDetails(toolCallDetails)
                    .toolChains(allChains)
                    .build();
            // O timestamp do UserMessage deve ser injetado no DialogueTurn; é o mesmo UserMessage mantido pelo Conversation.
            dialogueTurn.injectInstants();
            listener.onDialogueTurn(dialogueTurn);

            // Injeta no Conversation a cadeia de ferramentas realmente chamada pelo modelo
            if (toolCallAssistantMsg != null && toolResponseMsg != null) {
                conversation.addToolCallChain(toolCallAssistantMsg, toolResponseMsg);
            }
            // Não é mais possível obter o AssistantMessage a partir do ChatResponse, pois o timestamp já foi injetado
            conversation.add(dialogueTurn.getAssistantMessage());
        });
    }

    /**
     * Por padrão, a chamada de ferramentas é habilitada.
     * @param userMessage mensagem de texto puro do usuário (método de conveniência, sem metadados estruturados)
     */
    public void chat(String userMessage){
        chat(userMessage, true);
    }

    /**
     * Recebe texto puro. Internamente encapsulado como um UserMessage sem metadata.
     */
    public void chat(String userMessage, boolean useFunctionCall){
        chat(new UserMessage(userMessage), useFunctionCall);
    }

    /**
     * Ponto de entrada principal: diálogo com metadados (time/speaker/emotion etc. em UserMessage.metadata).
     * @param userMessage UserMessage do Spring AI já construído, podendo conter metadata
     * @param useFunctionCall se a chamada de ferramentas deve ser habilitada
     */
    public void chat(UserMessage userMessage, boolean useFunctionCall){
        Instant now = Instant.now();
        Flux<ChatResponse> chatResponseFlux = chatStream(now, userMessage, useFunctionCall);
        Flux<ChatToken> tokenFlux = convert(chatResponseFlux);
        // Pipeline de diálogo do dispositivo: filtra o conteúdo de raciocínio, enviando apenas a resposta final para a síntese de voz
        synthesizer.synthesize(tokenFlux.filter(ChatToken::isContent).map(ChatToken::text));
    }

    /**
     * Verifica se o Persona atual está em estado ativo (LLM gerando, TTS sintetizando, áudio em reprodução etc.).
     * Usado na decisão de interrupção: enquanto qualquer camada do pipeline ainda estiver funcionando, a interrupção deve ocorrer.
     */
    public boolean isActive() {
        if (synthesizer != null && synthesizer.isActive()) {
            return true;
        }
        return player != null && player.hasContent();
    }

    /**
     * Envia a mensagem de despedida e fecha a sessão após a reprodução terminar
     *
     * @return se a mensagem de despedida foi enviada com sucesso
     */
    public void sendGoodbyeMessage() {
        ChatSession session = getSession();
        if (session == null || !session.isAudioChannelOpen()){
            return ;
        }
        // A mensagem de despedida não precisa salvar o arquivo de áudio opus; reseta o timestamp para evitar reutilizar o valor da rodada anterior
        if (player.getOpusRecorder() != null) {
            player.getOpusRecorder().setAssistantMessageCreatedAt(null);
        }
        player.setFunctionAfterChat(() -> {
            session.setPersona(null);
            session.setPlayer(null);
            conversation.clear();
            if (sessionManager != null) {
                sessionManager.closeSession(session);
            } else {
                session.close();
            }
        });
        if(goodbyeMessages!=null){
            // Seleciona aleatoriamente uma mensagem de despedida
            String goodbyeMessage = goodbyeMessages.get();

            // Trata a mensagem de despedida diretamente, sem passar pelo LLM
            synthesizer.synthesize(goodbyeMessage);
        }else{
            chat("Preciso resolver uma coisa agora, até mais!",false);
        }

    }

    /**
     * Converte o fluxo de ChatResponse em um fluxo de ChatToken, incluindo o conteúdo de raciocínio e a resposta final.
     * <p>
     * No Spring AI 1.1.0+, com reasoningEffort habilitado, o conteúdo de raciocínio é retornado via
     * {@code AssistantMessage.getProperties().get("reasoningContent")}.
     */
    private Flux<ChatToken> convert(Flux<ChatResponse> chatResponseFlux) {
        return chatResponseFlux.mapNotNull(ChatResponse::getResult)
                .mapNotNull(Generation::getOutput)
                .flatMap(message -> {
                    List<ChatToken> tokens = new ArrayList<>();
                    Object reasoning = message.getMetadata().get("reasoningContent");
                    if (reasoning instanceof String r && !r.isEmpty()) {
                        tokens.add(ChatToken.thinking(r));
                    }
                    String text = message.getText();
                    if (text != null && !text.isEmpty()) {
                        tokens.add(ChatToken.content(text));
                    }
                    return Flux.fromIterable(tokens);
                });
    }
}
