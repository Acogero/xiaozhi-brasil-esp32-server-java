package com.xiaozhi.ai.llm.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
/**
 * Implementação do modelo de grande porte XingHuo (iFLYTEK)
 * Versões de modelo suportadas:
 * - X1 (x1): modelo de raciocínio profundo, comparável ao OpenAI o1 e ao DeepSeek R1, com suporte a tarefas de raciocínio, matemática, código, entre outras
 * - 4.0Ultra (generalv4): o modelo XingHuo mais potente, 32K de entrada/32K de saída
 * - Max (generalv3.5): modelo de linguagem de grande porte topo de linha, 8K de entrada/8K de saída
 * - Max-32K (generalv3.5-32k): versão 32K do Max
 * - Pro (generalv3): modelo de linguagem de grande porte de nível profissional, 8K de entrada/8K de saída
 * - Pro-128K (generalv3-128k): versão 128K do Pro, 128K de entrada/4K de saída
 * - Lite (general): modelo de linguagem leve, 8K de entrada/4K de saída, uso gratuito
 * 
 * Documentação da API: 
 * - Modelos V1: https://www.xfyun.cn/doc/spark/HTTP%E8%B0%83%E7%94%A8%E6%96%87%E6%A1%A3.html
 * - Modelo X1: https://www.xfyun.cn/doc/spark/X1http.html
 */
@Slf4j
public class XingHuoChatModel implements ChatModel {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    // Endereço da API XingHuo (iFLYTEK)
    private static final String SPARK_V1_API_URL = "https://spark-api-open.xf-yun.com/v1/chat/completions";
    private static final String SPARK_X1_API_URL = "https://spark-api-open.xf-yun.com/v2/chat/completions";
    
    private final OkHttpClient httpClient;
    private final String apiPassword;
    private final String model;
    private final String baseUrl;

    /**
     * Construtor
     * 
     * @param apiPassword Senha de API, obtida no console
     * @param model Nome do modelo, ex.: generalv4, generalv3.5, generalv3, general
     */
    public XingHuoChatModel(String apiPassword, String model) {
        this.apiPassword = apiPassword;
        this.model = model;
        // Seleciona o endereço da API de acordo com o modelo: o modelo X1 usa a interface v2, os demais usam a v1
        this.baseUrl = "x1".equalsIgnoreCase(model) ? SPARK_X1_API_URL : SPARK_V1_API_URL;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
    }

    public String getProviderName() {
        return "xinghuo";
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        
        try {
            // Monta o corpo da requisição
            Map<String, Object> requestBody = buildRequestBody(prompt, false);
            
            // Envia a requisição
            Request request = buildRequest(requestBody);
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Falha na requisição da API XingHuo: code={}, message={}", response.code(), response.message());
                    return ChatResponse.builder().generations(Collections.emptyList()).build();
                }
                
                String responseBody = response.body().string();
                
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                
                // Interpreta a resposta
                
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) {
                        String content = (String) message.get("content");
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("model", model);
                        if (responseMap.containsKey("usage")) {
                            metadata.put("usage", responseMap.get("usage"));
                        }
                        
                        return new ChatResponse(
                                List.of(new Generation(AssistantMessage.builder().content(content).properties(metadata).build()))
                        );
                    }
                }
                
                return ChatResponse.builder().generations(Collections.emptyList()).build();
                
            }
        } catch (Exception e) {
            log.error("Falha ao chamar o modelo de grande porte XingHuo", e);
            return ChatResponse.builder().generations(Collections.emptyList()).build();
        }
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.create(sink -> {
            log.debug("Chamada em streaming do modelo XingHuo: model={}", model);
            
            // Usa um array para armazenar a flag (necessário por ser modificada dentro de uma classe anônima interna)
            final boolean[] hasToolCall = {false};
            
            // Acumula as informações da chamada de ferramenta — o retorno em streaming do XingHuo é enviado em múltiplas partes
            final Map<String, Map<String, Object>> toolCallsAccumulator = new HashMap<>();
            
            try {
                // Monta o corpo da requisição
                Map<String, Object> requestBody = buildRequestBody(prompt, true);
                
                // Envia a requisição em streaming
                Request request = buildRequest(requestBody);
                
                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        log.error("Falha na requisição em streaming da API XingHuo", e);
                        sink.error(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            String errorBody = response.body() != null ? response.body().string() : "sem corpo de resposta";
                            sink.error(new IOException("Falha na requisição da API XingHuo: " + response.code() + " " + response.message() + ", detalhes: " + errorBody));
                            return;
                        }
                        
                        try (ResponseBody responseBody = response.body()) {
                            if (responseBody == null) {
                                sink.error(new IOException("Corpo da resposta vazio"));
                                return;
                            }
                            
                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(responseBody.byteStream(), StandardCharsets.UTF_8))) {
                                
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (line.isEmpty() || line.equals("data: [DONE]")) {
                                        continue;
                                    }
                                    
                                    if (line.startsWith("data: ")) {
                                        String jsonData = line.substring(6);
                                        processStreamLine(jsonData, sink, prompt, hasToolCall, toolCallsAccumulator);
                                    }
                                }
                                
                                // Processamento após a leitura do stream ser concluída
                                if (!hasToolCall[0]) {
                                    // Sem chamada de ferramenta, encerramento normal
                                    sink.complete();
                                } else if (!toolCallsAccumulator.isEmpty()) {
                                    // Valida se a chamada de ferramenta acumulada está completa
                                    boolean hasValidToolCall = toolCallsAccumulator.values().stream().anyMatch(tool -> {
                                        
                                        Map<String, Object> func = (Map<String, Object>) tool.get("function");
                                        if (func == null) return false;
                                        String name = (String) func.get("name");
                                        return name != null && !name.isEmpty();
                                    });
                                    
                                    if (hasValidToolCall) {
                                        // Há chamada de ferramenta e o name não é vazio; executa a chamada
                                        List<Map<String, Object>> finalToolCalls = new ArrayList<>(toolCallsAccumulator.values());
                                        processToolCalls(finalToolCalls, sink, prompt);
                                        sink.complete();
                                    } else {
                                        // name vazio indica que a chamada de ferramenta está incompleta; envia mensagem de erro e encerra
                                        String errorMessage = "Desculpe, a chamada de ferramenta falhou. Por favor, descreva novamente o que você precisa.";
                                        AssistantMessage assistantMessage = new AssistantMessage(errorMessage);
                                        ChatResponse errorResponse = new ChatResponse(
                                                List.of(new Generation(assistantMessage))
                                        );
                                        sink.next(errorResponse);
                                        sink.complete();
                                    }
                                } else {
                                    // hasToolCall marcado, porém accumulator vazio — situação anômala
                                    String errorMessage = "Desculpe, a chamada de ferramenta falhou. Por favor, descreva novamente o que você precisa.";
                                    AssistantMessage assistantMessage = new AssistantMessage(errorMessage);
                                    ChatResponse errorResponse = new ChatResponse(
                                            List.of(new Generation(assistantMessage))
                                    );
                                    sink.next(errorResponse);
                                    sink.complete();
                                }
                            }
                        } catch (Exception e) {
                            log.error("Falha ao processar a resposta em streaming", e);
                            sink.error(e);
                        }
                    }
                });
                
            } catch (Exception e) {
                log.error("Falha na chamada em streaming do modelo XingHuo", e);
                sink.error(e);
            }
        });
    }

    /**
     * Processa cada linha da resposta em streaming
     */
    private void processStreamLine(String jsonData, reactor.core.publisher.FluxSink<ChatResponse> sink, 
                                   Prompt prompt, boolean[] hasToolCall,
                                   Map<String, Map<String, Object>> toolCallsAccumulator) {
        try {
            // Adiciona o log do JSON bruto
            
            
            Map<String, Object> data = objectMapper.readValue(jsonData, Map.class);
            
            List<Map<String, Object>> choices = (List<Map<String, Object>>) data.get("choices");
            
            if (choices == null || choices.isEmpty()) {
                return;
            }
            
            Map<String, Object> choice = choices.get(0);
            
            Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
            
            if (delta == null) {
                return;
            }
            
            // Processa o conteúdo comum
            if (delta.containsKey("content")) {
                String content = (String) delta.get("content");
                if (content != null && !content.isEmpty()) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("model", model);
                    
                    sink.next(ChatResponse.builder()
                            .generations(List.of(new Generation(
                                    AssistantMessage.builder().content(content).properties(metadata).build())))
                            .build());
                }
            }
            
            // Processa a chamada de ferramenta — acumula os parâmetros, sem executar imediatamente
            if (delta.containsKey("tool_calls")) {
                hasToolCall[0] = true;
                
                Object toolCallsObj = delta.get("tool_calls");
                String toolCallsJson = objectMapper.writeValueAsString(toolCallsObj);
                
                // Acumula as informações da chamada de ferramenta
                accumulateToolCalls(toolCallsObj, toolCallsAccumulator);
            }
            
            // Verifica se terminou — a chamada de ferramenta só é executada ao final do stream
            String finishReason = (String) choice.get("finish_reason");
            if (finishReason != null && !finishReason.isEmpty()) {
                
                // Quando há chamada de ferramenta, verifica se está completa
                if (hasToolCall[0] && !toolCallsAccumulator.isEmpty()) {
                    // Valida se a chamada de ferramenta está completa (tem name e arguments)
                    // Observação: arguments pode ser uma string vazia "", mas deve existir e não ser null
                    boolean allToolsComplete = toolCallsAccumulator.values().stream().allMatch(tool -> {
                        
                        Map<String, Object> func = (Map<String, Object>) tool.get("function");
                        if (func == null) return false;
                        String name = (String) func.get("name");
                        String args = (String) func.get("arguments");
                        // name não pode ser vazio; arguments deve existir (pode ser "")
                        boolean hasName = name != null && !name.isEmpty();
                        boolean hasArgs = args != null;
                        boolean complete = hasName && hasArgs;
                        return complete;
                    });
                    
                    if (allToolsComplete) {
                        List<Map<String, Object>> finalToolCalls = new ArrayList<>(toolCallsAccumulator.values());
                        processToolCalls(finalToolCalls, sink, prompt);
                        sink.complete();
                    }
                } else if (!hasToolCall[0]) {
                    // Sem chamada de ferramenta, encerramento normal
                    sink.complete();
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Falha ao interpretar a resposta em streaming: {}", jsonData, e);
        }
    }
    
    /**
     * Acumula as informações da chamada de ferramenta — o retorno em streaming do XingHuo envia tool_calls em múltiplas partes
     * Estratégia: usa index como chave (se houver); caso contrário, usa "tool_0" como chave padrão
     */
    private void accumulateToolCalls(Object toolCallsObj, Map<String, Map<String, Object>> accumulator) {
        try {
            List<Map<String, Object>> toolCallsList = new ArrayList<>();
            if (toolCallsObj instanceof List) {
                
                List<Map<String, Object>> list = (List<Map<String, Object>>) toolCallsObj;
                toolCallsList = list;
            } else if (toolCallsObj instanceof Map) {
                
                Map<String, Object> single = (Map<String, Object>) toolCallsObj;
                toolCallsList.add(single);
            }
            
            for (int i = 0; i < toolCallsList.size(); i++) {
                Map<String, Object> toolCall = toolCallsList.get(i);
                Object functionObj = toolCall.get("function");
                
                if (functionObj instanceof Map) {
                    
                    Map<String, Object> function = (Map<String, Object>) functionObj;
                    String name = (String) function.get("name");
                    
                    // Usa name como chave; se name estiver vazio, usa o índice
                    String key = (name != null && !name.isEmpty()) ? name : "tool_" + i;
                    
                    // Obtém ou cria o objeto acumulador
                    Map<String, Object> existing = accumulator.getOrDefault(key, new HashMap<>());
                    
                    // Mescla o type
                    String type = (String) toolCall.get("type");
                    if (type != null && !type.isEmpty()) {
                        existing.put("type", type);
                    }
                    
                    // Obtém ou cria o objeto function
                    
                    Map<String, Object> existingFunc = (Map<String, Object>) existing.getOrDefault("function", new HashMap<>());
                    
                    // Acumula o name — pode vir vazio na primeira vez e receber valor depois
                    if (name != null && !name.isEmpty()) {
                        existingFunc.put("name", name);
                    }
                    
                    // Acumula a description
                    String description = (String) function.get("description");
                    if (description != null && !description.isEmpty()) {
                        existingFunc.put("description", description);
                    }
                    
                    // Acumula os arguments — pode retornar em múltiplas partes
                    Object argsObj = function.get("arguments");
                    if (argsObj != null) {
                        String currentArgs = (argsObj instanceof String) ? (String) argsObj : "";
                        String existingArgs = (String) existingFunc.getOrDefault("arguments", "");
                        existingFunc.put("arguments", existingArgs + currentArgs);
                    }
                    
                    existing.put("function", existingFunc);
                    accumulator.put(key, existing);
                }
            }
        } catch (Exception e) {
            log.error("❌ Falha ao acumular a chamada de ferramenta", e);
        }
    }

    /**
     * Processa a chamada de ferramenta
     */
    private void processToolCalls(List<Map<String, Object>> toolCalls, 
                                  reactor.core.publisher.FluxSink<ChatResponse> sink,
                                  Prompt prompt) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return;
        }
        
        try {
            List<AssistantMessage.ToolCall> assistantToolCalls = new ArrayList<>();
            
            for (int i = 0; i < toolCalls.size(); i++) {
                Map<String, Object> toolCall = toolCalls.get(i);
                
                // Extrai os campos — reanalisa como JSON para garantir o tipo correto
                String type = (String) toolCall.get("type");
                Object functionObj = toolCall.get("function");
                
                if (functionObj != null) {
                    // Converte uniformemente para Map
                    
                    Map<String, Object> function = (functionObj instanceof Map) 
                        ? (Map<String, Object>) functionObj
                        : objectMapper.readValue(objectMapper.writeValueAsString(functionObj), Map.class);
                    
                    String name = (String) function.get("name");
                    Object argsObj = function.get("arguments");
                    // arguments pode ser uma string ou um objeto
                    String arguments = (argsObj instanceof String) 
                        ? (String) argsObj 
                        : objectMapper.writeValueAsString(argsObj);
                    
                    // Se arguments for uma string vazia, converte para um objeto vazio (indicando ausência de parâmetros)
                    if (arguments == null || arguments.trim().isEmpty()) {
                        arguments = "{}";
                    }
                    
                    // Gera um ID único
                    String id = (String) toolCall.getOrDefault("id", "tool_" + System.currentTimeMillis() + "_" + i);
                    
                    assistantToolCalls.add(new AssistantMessage.ToolCall(
                            id,
                            type != null ? type : "function",
                            name,
                            arguments
                    ));
                }
            }
            
            if (!assistantToolCalls.isEmpty()) {
                AssistantMessage assistantMessage = AssistantMessage.builder()
                        .content("")
                        .properties(Map.of("model", model))
                        .toolCalls(assistantToolCalls)
                        .build();
                        
                ChatResponse chatResponse = ChatResponse.builder()
                        .generations(List.of(new Generation(assistantMessage)))
                        .build();
                
                // Executa a chamada de ferramenta
                var toolExecutionResult = ToolCallingManager.builder().build()
                        .executeToolCalls(prompt, chatResponse);
                
                if (toolExecutionResult.returnDirect()) {
                    // Retorna diretamente o resultado da execução da ferramenta
                    sink.next(ChatResponse.builder().from(chatResponse)
                            .generations(ToolExecutionResult.buildGenerations(toolExecutionResult))
                            .build());
                    sink.complete();
                } else {
                    // O resultado da execução da ferramenta precisa ser enviado de volta ao modelo para continuar a conversa
                    // O modelo XingHuo ainda não suporta continuar a conversa após a chamada de ferramenta; o resultado é retornado diretamente
                    sink.next(ChatResponse.builder().from(chatResponse)
                            .generations(ToolExecutionResult.buildGenerations(toolExecutionResult))
                            .build());
                    sink.complete();
                }
            }
        } catch (Exception e) {
            log.error("❌ Falha ao processar a chamada de ferramenta", e);
            sink.error(e);
        }
    }

    /**
     * Monta o corpo da requisição
     */
    private Map<String, Object> buildRequestBody(Prompt prompt, boolean stream) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("stream", stream);
        
        // Converte o formato das mensagens — versão simplificada, processa apenas mensagens do usuário
        List<Map<String, String>> messages = new ArrayList<>();
        
        // Adiciona a mensagem de sistema (se houver)
        if (prompt.getInstructions().size() > 1) {
            // A primeira pode ser a mensagem de sistema
            Message firstMsg = prompt.getInstructions().get(0);
            if ("system".equals(firstMsg.getMessageType().getValue())) {
                Map<String, String> systemMsg = new HashMap<>();
                systemMsg.put("role", "system");
                systemMsg.put("content", firstMsg.getText());
                messages.add(systemMsg);
            }
        }
        
        // Adiciona a mensagem do usuário
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt.getUserMessage().getText());
        messages.add(userMsg);
        
        requestBody.put("messages", messages);
        
        // Adiciona a definição de ferramentas (se houver) — o XingHuo Max e Ultra suportam Function Call
        ToolCallingChatOptions chatOptions = (ToolCallingChatOptions) prompt.getOptions();
        if (chatOptions != null && chatOptions.getToolCallbacks() != null && !chatOptions.getToolCallbacks().isEmpty()) {
            List<Map<String, Object>> tools = new ArrayList<>();
            chatOptions.getToolCallbacks().forEach(toolCallback -> {
                try {
                    // Obtém a definição da ferramenta
                    var toolDefinition = toolCallback.getToolDefinition();
                    if (toolDefinition != null) {
                        // Converte o ToolDefinition para string JSON e depois interpreta como Map
                        String toolJson = com.xiaozhi.utils.JsonUtil.toJson(toolDefinition);
                        
                        Map<String, Object> toolMap = com.xiaozhi.utils.JsonUtil.fromJson(toolJson, Map.class);
                        
                        if (toolMap != null) {
                            // Monta a definição de ferramenta no formato exigido pelo XingHuo
                            // Formato: {"type":"function", "function":{"name": "...", "description": "...", "parameters": {...}}}
                            Map<String, Object> xinghuoTool = new HashMap<>();
                            xinghuoTool.put("type", "function");
                            
                            Map<String, Object> function = new HashMap<>();
                            function.put("name", toolMap.get("name"));
                            function.put("description", toolMap.get("description"));
                            
                            // Processa parameters: inputSchema pode ser string ou objeto — deve ser convertido para objeto!
                            Object inputSchema = toolMap.get("inputSchema");
                            Map<String, Object> parameters = null;
                            
                            if (inputSchema instanceof String) {
                                // Converte string para objeto
                                String schemaStr = (String) inputSchema;
                                if (!schemaStr.isEmpty()) {
                                    try {
                                        parameters = objectMapper.readValue(schemaStr, Map.class);
                                    } catch (Exception e) {
                                        log.warn("Falha ao interpretar inputSchema: {}", schemaStr, e);
                                    }
                                }
                            } else if (inputSchema instanceof Map) {
                                // Já é um objeto
                                
                                Map<String, Object> map = (Map<String, Object>) inputSchema;
                                parameters = map;
                            }
                            
                            // Garante que parameters seja um objeto, e não uma string
                            if (parameters != null) {
                                function.put("parameters", parameters);
                            } else {
                                // Usa um objeto vazio como valor padrão
                                function.put("parameters", Map.of("type", "object", "properties", Map.of()));
                            }
                            
                            xinghuoTool.put("function", function);
                            tools.add(xinghuoTool);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Falha ao adicionar a definição de ferramenta: {}", e.getMessage(), e);
                }
            });
            
            if (!tools.isEmpty()) {
                requestBody.put("tools", tools);
            }
        }
        
        return requestBody;
    }

    /**
     * Monta a requisição HTTP
     */
    private Request buildRequest(Map<String, Object> requestBody) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        
        return new Request.Builder()
                .url(baseUrl)
                .post(RequestBody.create(jsonBody, JSON))
                .addHeader("Authorization", "Bearer " + apiPassword)
                .addHeader("Content-Type", "application/json")
                .build();
    }
}

