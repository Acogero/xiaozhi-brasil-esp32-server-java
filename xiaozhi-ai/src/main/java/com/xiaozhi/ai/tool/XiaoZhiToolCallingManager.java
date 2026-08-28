package com.xiaozhi.ai.tool;

import com.xiaozhi.ai.tool.session.ToolSession;
import com.xiaozhi.ai.tool.session.ToolSessionProvider;
import com.xiaozhi.event.ToolCallCompletedEvent;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.tool.observation.DefaultToolCallingObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationDocumentation;
import org.springframework.ai.tool.resolution.DelegatingToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

import lombok.extern.slf4j.Slf4j;
/**
 * Gerenciador customizado de chamadas de ferramenta, usado para processar e executar chamadas de ferramenta.
 * Baseado no DefaultToolCallingManager do Spring AI, com funcionalidades customizadas de monitoramento e processamento de metadados adicionadas.
 * <p>
 * Inclui a correção para a mesclagem de fragmentos de chamadas de ferramenta em streaming (Spring AI issue #4629, #4790).
 * Esse problema ainda não foi corrigido no Spring AI 1.1.4; o método mergeToolCalls é mantido como uma correção necessária.
 * <p>
 * TODO: [Acompanhamento de atualização do Spring AI] Acompanhar se versões futuras corrigem o problema de fragmentação; quando isso ocorrer, o método mergeToolCalls poderá ser removido.
 */
@Slf4j
public class XiaoZhiToolCallingManager implements ToolCallingManager, ApplicationContextAware {

    private static ApplicationContext applicationContext;

    // @formatter:off

    private static final ObservationRegistry DEFAULT_OBSERVATION_REGISTRY
            = ObservationRegistry.NOOP;

    private static final ToolCallingObservationConvention DEFAULT_OBSERVATION_CONVENTION
            = new DefaultToolCallingObservationConvention();

    private static final ToolCallbackResolver DEFAULT_TOOL_CALLBACK_RESOLVER
            = new DelegatingToolCallbackResolver(List.of());

    private static final ToolExecutionExceptionProcessor DEFAULT_TOOL_EXECUTION_EXCEPTION_PROCESSOR
            = DefaultToolExecutionExceptionProcessor.builder().build();

    // @formatter:on
    private final ObservationRegistry observationRegistry;

    private final ToolCallbackResolver toolCallbackResolver;

    private final ToolExecutionExceptionProcessor toolExecutionExceptionProcessor;

    private ToolCallingObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

    public XiaoZhiToolCallingManager(ObservationRegistry observationRegistry, ToolCallbackResolver toolCallbackResolver,
                                     ToolExecutionExceptionProcessor toolExecutionExceptionProcessor) {
        Assert.notNull(observationRegistry, "observationRegistry cannot be null");
        Assert.notNull(toolCallbackResolver, "toolCallbackResolver cannot be null");
        Assert.notNull(toolExecutionExceptionProcessor, "toolCallExceptionConverter cannot be null");

        this.observationRegistry = observationRegistry;
        this.toolCallbackResolver = toolCallbackResolver;
        this.toolExecutionExceptionProcessor = toolExecutionExceptionProcessor;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        XiaoZhiToolCallingManager.applicationContext = applicationContext;
    }

    /**
     * Obtém o ToolSessionProvider
     */
    private static ToolSessionProvider sessionProvider() {
        if (applicationContext != null) {
            try {
                return applicationContext.getBean(ToolSessionProvider.class);
            } catch (Exception e) {
                log.debug("Não foi possível obter o ToolSessionProvider: {}", e.getMessage());
            }
        }
        return null;
    }

    /**
     * Publica o evento de chamada de ferramenta
     */
    private static void publishToolEvent(String sessionId, String toolName, String arguments,
                                          String result, boolean success, long startTimeMs) {
        if (applicationContext == null) {
            return;
        }
        try {
            long durationMs = startTimeMs > 0 ? System.currentTimeMillis() - startTimeMs : 0;
            applicationContext.publishEvent(new ToolCallCompletedEvent(
                    XiaoZhiToolCallingManager.class, sessionId, toolName, arguments, result, success, durationMs));
        } catch (Exception e) {
            log.debug("Falha ao publicar o evento de chamada de ferramenta: {}", e.getMessage());
        }
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        Assert.notNull(chatOptions, "chatOptions cannot be null");

        List<ToolCallback> toolCallbacks = new ArrayList<>(chatOptions.getToolCallbacks());
        for (String toolName : chatOptions.getToolNames()) {
            // Skip the tool if it is already present in the request toolCallbacks.
            // That might happen if a tool is defined in the options
            // both as a ToolCallback and as a tool name.
            if (chatOptions.getToolCallbacks()
                    .stream()
                    .anyMatch(tool -> tool.getToolDefinition().name().equals(toolName))) {
                continue;
            }
            ToolCallback toolCallback = this.toolCallbackResolver.resolve(toolName);
            if (toolCallback == null) {
                throw new IllegalStateException("No ToolCallback found for tool name: " + toolName);
            }
            toolCallbacks.add(toolCallback);
        }

        return toolCallbacks.stream().map(ToolCallback::getToolDefinition).toList();
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        Assert.notNull(prompt, "prompt cannot be null");
        Assert.notNull(chatResponse, "chatResponse cannot be null");

        Optional<Generation> toolCallGeneration = chatResponse.getResults()
                .stream()
                .filter(g -> !CollectionUtils.isEmpty(g.getOutput().getToolCalls()))
                .findFirst();

        if (toolCallGeneration.isEmpty()) {
            throw new IllegalStateException("No tool call requested by the chat model");
        }

        AssistantMessage originalAssistantMessage = toolCallGeneration.get().getOutput();

        // Corrige o problema de ToolCall dividido causado pela fragmentação em streaming
        List<AssistantMessage.ToolCall> mergedToolCalls = mergeToolCalls(originalAssistantMessage.getToolCalls());
        AssistantMessage assistantMessage = (mergedToolCalls == originalAssistantMessage.getToolCalls())
                ? originalAssistantMessage
                : AssistantMessage.builder()
                    .content(originalAssistantMessage.getText())
                    .properties(originalAssistantMessage.getMetadata())
                    .toolCalls(mergedToolCalls)
                    .build();

        ToolContext toolContext = buildToolContext(prompt, assistantMessage);

        XiaoZhiToolCallingManager.ToolExecResult toolExecResult = executeToolCall(prompt, assistantMessage,
                toolContext);

        // Armazena a mensagem intermediária (requisição tool_call do modelo + resultado da execução da ferramenta) no ToolSession, para que a Persona a injete na Conversation
        String sessionId = toolContext.getContext().get("sessionId") instanceof String s ? s : null;
        if (sessionId != null) {
            ToolSessionProvider provider = sessionProvider();
            if (provider != null) {
                ToolSession toolSession = provider.getSession(sessionId);
                if (toolSession != null) {
                    toolSession.addToolCallMessages(assistantMessage, toolExecResult.toolResponseMessage());
                }
            }
        }

        List<Message> conversationHistory = buildPostToolHistory(prompt.getInstructions(),
                assistantMessage, toolExecResult.toolResponseMessage());

        return ToolExecutionResult.builder()
                .conversationHistory(conversationHistory)
                .returnDirect(toolExecResult.returnDirect())
                .build();
    }

    private static ToolContext buildToolContext(Prompt prompt, AssistantMessage assistantMessage) {
        Map<String, Object> toolContextMap = Map.of();

        if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions
                && !CollectionUtils.isEmpty(toolCallingChatOptions.getToolContext())) {
            toolContextMap = new HashMap<>(toolCallingChatOptions.getToolContext());

            toolContextMap.put(ToolContext.TOOL_CALL_HISTORY,
                    buildPreToolHistory(prompt, assistantMessage));
        }

        return new ToolContext(toolContextMap);
    }

    private static List<Message> buildPreToolHistory(Prompt prompt,
                                                                             AssistantMessage assistantMessage) {
        List<Message> messageHistory = new ArrayList<>(prompt.copy().getInstructions());

        // Garante que a mensagem de chamada de ferramenta contenha a metadata correta
        if (!CollectionUtils.isEmpty(assistantMessage.getToolCalls())) {
            Map<String, Object> metadata = new HashMap<>(assistantMessage.getMetadata());
            String toolName = assistantMessage.getToolCalls().get(0).name();
            metadata.put("toolName", toolName);
            AssistantMessage updatedAssistantMessage = AssistantMessage.builder()
                    .content(assistantMessage.getText())
                    .properties(metadata)
                    .toolCalls(assistantMessage.getToolCalls())
                    .build();
            messageHistory.add(updatedAssistantMessage);
        } else {
            messageHistory.add(AssistantMessage.builder()
                    .content(assistantMessage.getText())
                    .properties(assistantMessage.getMetadata())
                    .toolCalls(assistantMessage.getToolCalls())
                    .build());
        }

        return messageHistory;
    }

    /**
     * Mescla as chamadas de ferramenta fragmentadas na resposta em streaming (Spring AI issue #4629, #4790; ainda não corrigido na 1.1.4).
     * <p>
     * Algumas APIs compatíveis com OpenAI (Qwen, Alibaba Cloud, etc.) retornam, no streaming de tool call, o id do chunk de continuação como uma string vazia "" em vez de null,
     * o que faz com que OpenAiStreamFunctionCallingHelper.merge() divida o name e os arguments de um mesmo tool call em múltiplos registros.
     * <p>
     * Padrão de fragmentação observado (o mesmo id dividido em duas partes):
     * <pre>
     *   Fragmento[0]: id='call_xxx', name='get_device_status', arguments=''
     *   Fragmento[1]: id='call_xxx', name='',                  arguments='{}'
     * </pre>
     * <p>
     * Estratégia de mesclagem:
     * - Entradas com o mesmo id pertencem ao mesmo tool call; mescla name e arguments
     * - Entradas com id vazio e name vazio são tratadas como fragmentos de continuação, mescladas ao tool call imediatamente anterior
     * - Entradas que, após a mesclagem, ainda não tiverem name recebem um warn e são ignoradas (arguments pode ser vazio; algumas ferramentas não requerem parâmetros)
     */
    private static List<AssistantMessage.ToolCall> mergeToolCalls(List<AssistantMessage.ToolCall> toolCalls) {
        if (toolCalls == null || toolCalls.size() <= 1) {
            return toolCalls;
        }

        // Verificação rápida: se todas as entradas têm name, não há problema de fragmentação; retorna diretamente
        boolean hasFragment = toolCalls.stream()
                .anyMatch(tc -> !StringUtils.hasText(tc.name()));
        if (!hasFragment) {
            return toolCalls;
        }

        List<AssistantMessage.ToolCall> merged = new ArrayList<>();
        String currentId = null;
        String currentType = null;
        String currentName = null;
        StringBuilder currentArgs = null;

        for (AssistantMessage.ToolCall tc : toolCalls) {
            // Verifica se é continuação: mesmo id, ou um fragmento isolado sem id e sem name
            boolean isContinuation = currentId != null
                    && ((!StringUtils.hasText(tc.id()) && !StringUtils.hasText(tc.name()))
                        || (StringUtils.hasText(tc.id()) && tc.id().equals(currentId)));

            if (isContinuation) {
                // Fragmento de continuação: mescla ao tool call atual
                if (StringUtils.hasText(tc.name()) && !StringUtils.hasText(currentName)) {
                    currentName = tc.name();
                }
                if (tc.arguments() != null && !tc.arguments().isEmpty()) {
                    currentArgs.append(tc.arguments());
                }
            } else {
                // Novo tool call: emite primeiro o anterior
                if (currentName != null) {
                    merged.add(new AssistantMessage.ToolCall(currentId, currentType, currentName, currentArgs.toString()));
                }
                currentId = StringUtils.hasText(tc.id()) ? tc.id() : "";
                currentType = StringUtils.hasText(tc.type()) ? tc.type() : "function";
                currentName = StringUtils.hasText(tc.name()) ? tc.name() : null;
                currentArgs = new StringBuilder(tc.arguments() != null ? tc.arguments() : "");
            }
        }
        // Emite o último
        if (currentName != null) {
            merged.add(new AssistantMessage.ToolCall(currentId, currentType, currentName, currentArgs.toString()));
        }

        // Validação: intercepta apenas as que faltam name; arguments pode ser vazio (algumas ferramentas não requerem parâmetros)
        List<AssistantMessage.ToolCall> valid = new ArrayList<>();
        for (AssistantMessage.ToolCall tc : merged) {
            if (!StringUtils.hasText(tc.name())) {
                log.warn("Chamada de ferramenta ainda sem name após a mesclagem, ignorando: id={}, arguments={}", tc.id(), tc.arguments());
            } else {
                valid.add(tc);
            }
        }

        if (valid.size() != toolCalls.size()) {
            log.warn("Mesclagem de fragmentos de chamada de ferramenta acionada: {} registros → {} registros",
                    toolCalls.size(), valid.size());
        }
        return valid;
    }

    /**
     * Execute the tool call and return the response message.
     */
    private XiaoZhiToolCallingManager.ToolExecResult executeToolCall(Prompt prompt, AssistantMessage assistantMessage,
                                                                                  ToolContext toolContext) {
        List<ToolCallback> toolCallbacks = List.of();
        if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions) {
            toolCallbacks = toolCallingChatOptions.getToolCallbacks();
        }

        List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

        Boolean returnDirect = null;

        for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {

            String toolName = toolCall.name();
            String toolInputArguments = toolCall.arguments();

            ToolCallback toolCallback = toolCallbacks.stream()
                    .filter(tool -> toolName.equals(tool.getToolDefinition().name()))
                    .findFirst()
                    .orElseGet(() -> this.toolCallbackResolver.resolve(toolName));

            if (toolCallback == null) {
                // O modelo alucinou e chamou uma ferramenta não registrada; retorna um resultado de erro para que o modelo resuma a resposta por conta própria, em vez de quebrar todo o fluxo
                log.error("O modelo chamou uma ferramenta não registrada: {}", toolName);
                toolResponses.add(new ToolResponseMessage.ToolResponse(
                        toolCall.id(), toolName,
                        "A ferramenta '" + toolName + "' não existe ou não foi registrada; informe ao usuário que esta funcionalidade não está disponível no momento."));
                continue;
            }

            if (returnDirect == null) {
                returnDirect = toolCallback.getToolMetadata().returnDirect();
            }
            else {
                returnDirect = returnDirect && toolCallback.getToolMetadata().returnDirect();
            }

            ToolCallingObservationContext observationContext = ToolCallingObservationContext.builder()
                    .toolDefinition(toolCallback.getToolDefinition())
                    .toolMetadata(toolCallback.getToolMetadata())
                    .toolCallArguments(toolInputArguments)
                    .build();
            // Obtém o ToolSession através do sessionId (a Persona passa apenas o sessionId para evitar problemas de serialização)
            String sessionId = toolContext.getContext().get("sessionId") instanceof String s ? s : null;
            ToolSession toolSession = null;
            if (sessionId != null) {
                toolSession = sessionProvider() != null ? sessionProvider().getSession(sessionId) : null;
                observationContext.put("sessionId", sessionId);
            }

            // Registra o horário de início da chamada de ferramenta
            final long[] startTimeRef = new long[]{System.currentTimeMillis()};
            final boolean[] successRef = new boolean[]{true};

            String toolCallResult = ToolCallingObservationDocumentation.TOOL_CALL
                    .observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
                            this.observationRegistry)
                    .observe(() -> {
                        String toolResult;
                        try {
                            toolResult = toolCallback.call(toolInputArguments, toolContext);
                        }
                        catch (ToolExecutionException ex) {
                            log.error("Tool execution exception: ", ex);
                            toolResult = this.toolExecutionExceptionProcessor.process(ex);
                            log.debug("Processed tool execution exception result: {}", toolResult);
                            successRef[0] = false;
                        }
                        catch (Exception ex) {
                            log.error("Unexpected exception during tool execution: ", ex);
                            toolResult = "Error executing tool: " + ex.getMessage();
                            successRef[0] = false;
                        }
                        observationContext.setToolCallResult(toolResult);

                        return toolResult;
                    });

            // Registra os detalhes da chamada de ferramenta na session
            if (toolSession != null) {
                toolSession.addToolCallDetail(toolName, toolInputArguments, toolCallResult);
            }

            // Publica o evento de chamada de ferramenta
            publishToolEvent(sessionId, toolName, toolInputArguments, toolCallResult,
                    successRef[0], startTimeRef[0]);

            toolResponses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolName,
                    toolCallResult != null ? toolCallResult : ""));
        }

        return new XiaoZhiToolCallingManager.ToolExecResult(ToolResponseMessage.builder().responses(toolResponses).build(),
                returnDirect != null && returnDirect);
    }

    private List<Message> buildPostToolHistory(List<Message> previousMessages,
                                                                     AssistantMessage assistantMessage, ToolResponseMessage toolResponseMessage) {
        List<Message> messages = new ArrayList<>(previousMessages);

        // Garante que a mensagem de chamada de ferramenta contenha a metadata correta
        if (!CollectionUtils.isEmpty(assistantMessage.getToolCalls())) {
            Map<String, Object> metadata = new HashMap<>(assistantMessage.getMetadata());
            String toolName = assistantMessage.getToolCalls().get(0).name();
            metadata.put("toolName", toolName);
            AssistantMessage updatedAssistantMessage = AssistantMessage.builder()
                    .content(assistantMessage.getText())
                    .properties(metadata)
                    .toolCalls(assistantMessage.getToolCalls())
                    .build();
            messages.add(updatedAssistantMessage);
        } else {
            messages.add(assistantMessage);
        }

        messages.add(toolResponseMessage);
        return messages;
    }

    public void setObservationConvention(ToolCallingObservationConvention observationConvention) {
        this.observationConvention = observationConvention;
    }

    public static XiaoZhiToolCallingManager.Builder builder() {
        return new XiaoZhiToolCallingManager.Builder();
    }

    private record ToolExecResult(ToolResponseMessage toolResponseMessage, boolean returnDirect) {
    }

    public final static class Builder {

        private ObservationRegistry observationRegistry = DEFAULT_OBSERVATION_REGISTRY;

        private ToolCallbackResolver toolCallbackResolver = DEFAULT_TOOL_CALLBACK_RESOLVER;

        private ToolExecutionExceptionProcessor toolExecutionExceptionProcessor = DEFAULT_TOOL_EXECUTION_EXCEPTION_PROCESSOR;

        private Builder() {
        }

        public XiaoZhiToolCallingManager.Builder observationRegistry(ObservationRegistry observationRegistry) {
            this.observationRegistry = observationRegistry;
            return this;
        }

        public XiaoZhiToolCallingManager.Builder toolCallbackResolver(ToolCallbackResolver toolCallbackResolver) {
            this.toolCallbackResolver = toolCallbackResolver;
            return this;
        }

        public XiaoZhiToolCallingManager.Builder toolExecutionExceptionProcessor(
                ToolExecutionExceptionProcessor toolExecutionExceptionProcessor) {
            this.toolExecutionExceptionProcessor = toolExecutionExceptionProcessor;
            return this;
        }

        public XiaoZhiToolCallingManager build() {
            return new XiaoZhiToolCallingManager(this.observationRegistry, this.toolCallbackResolver,
                    this.toolExecutionExceptionProcessor);
        }

    }
}
