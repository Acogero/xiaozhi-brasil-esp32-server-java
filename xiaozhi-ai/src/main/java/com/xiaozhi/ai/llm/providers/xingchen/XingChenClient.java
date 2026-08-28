package com.xiaozhi.ai.llm.providers.xingchen;

import com.xiaozhi.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Implementação do cliente da API do Agent XingChen (iFLYTEK)
 * Baseado na documentação: https://www.xfyun.cn/doc/spark/Agent04-API%E6%8E%A5%E5%85%A5.html
 * 
 * Endereço da API: https://xingchen-api.xf-yun.com/workflow/v1/chat/completions
 * Forma de autenticação: Bearer token (usando o formato APIKey:APISecret)
 */
@Slf4j
public class XingChenClient {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    // Endpoint da API
    private static final String API_BASE_URL = "https://xingchen-api.xf-yun.com";
    private static final String CHAT_COMPLETIONS_PATH = "/workflow/v1/chat/completions";
    private static final String RESUME_PATH = "/workflow/v1/resume";
    
    // Identificador de resposta em streaming
    private static final String DATA_PREFIX = "data:";
    private static final String EVENT_PREFIX = "event:";
    
    private final String baseUrl;
    private final String flowId;
    private final String bearerToken; // APIKey:APISecret
    private final OkHttpClient httpClient;

    /**
     * Construtor
     * @param apiKey Chave de API
     * @param apiSecret Chave secreta de API
     * @param flowId ID do workflow
     */
    public XingChenClient(String baseUrl, String apiKey, String apiSecret, String flowId) {
        this.baseUrl = baseUrl != null && !baseUrl.isEmpty() ? baseUrl : API_BASE_URL;
        this.flowId = flowId;
        this.bearerToken = apiKey + ":" + apiSecret;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        log.info("XingChenClient inicializado: baseUrl={}, flowId={}", this.baseUrl, flowId);
    }

    /**
     * Compatibilidade com o construtor antigo
     */
    public XingChenClient(String endpoint, String apiKey, String apiSecret) {
        this(endpoint, apiKey, apiSecret, null);
    }

    public String getFlowId() {
        return flowId;
    }

    /**
     * Envia uma mensagem de chat síncrona
     */
    public XingChenResponse sendChatMessage(XingChenRequest request) throws IOException {
        log.debug("Enviando mensagem de conversa síncrona: flowId={}, uid={}", request.getFlowId(), request.getUid());
        
        // Garante que não seja em streaming
        request.setStream(false);
        
        String jsonBody = JsonUtil.toJson(request);
        log.debug("Corpo da requisição: {}", jsonBody);
        
        Request httpRequest = new Request.Builder()
                .url(baseUrl + CHAT_COMPLETIONS_PATH)
                .post(RequestBody.create(jsonBody, JSON))
                .addHeader("Authorization", "Bearer " + bearerToken)
                .addHeader("Content-Type", "application/json")
                .build();
        
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "sem corpo de resposta";
                log.error("Falha na requisição da API: code={}, body={}", response.code(), errorBody);
                throw new IOException("Falha na requisição da API: " + response.code() + ", " + errorBody);
            }
            
            String responseBody = response.body().string();
            log.debug("Resposta: {}", responseBody);
            
            return JsonUtil.fromJson(responseBody, XingChenResponse.class);
        }
    }

    /**
     * Envia uma mensagem de chat em streaming
     */
    public void sendChatMessageStream(XingChenRequest request, XingChenChatStreamCallback callback) throws IOException {
        log.debug("Enviando mensagem de conversa em streaming: flowId={}, uid={}", request.getFlowId(), request.getUid());
        
        // Garante o modo streaming
        request.setStream(true);
        
        String jsonBody = JsonUtil.toJson(request);
        log.debug("Corpo da requisição: {}", jsonBody);
        
        Request httpRequest = new Request.Builder()
                .url(baseUrl + CHAT_COMPLETIONS_PATH)
                .post(RequestBody.create(jsonBody, JSON))
                .addHeader("Authorization", "Bearer " + bearerToken)
                .addHeader("Content-Type", "application/json")
                .build();
        
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "sem corpo de resposta";
                log.error("Falha na requisição da API: code={}, body={}", response.code(), errorBody);
                callback.onException(new IOException("Falha na requisição da API: " + response.code() + ", " + errorBody));
                return;
            }
            
            ResponseBody body = response.body();
            if (body == null) {
                callback.onException(new IOException("Corpo da resposta vazio"));
                return;
            }
            
            // Processa o stream SSE
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    
                    // Processa o evento SSE
                    if (line.startsWith(EVENT_PREFIX)) {
                        String eventType = line.substring(EVENT_PREFIX.length()).trim();
                        log.debug("Tipo de evento recebido: {}", eventType);
                        continue;
                    }
                    
                    // Processa a linha de dados
                    if (line.startsWith(DATA_PREFIX)) {
                        String jsonData = line.substring(DATA_PREFIX.length()).trim();
                        if (jsonData.isEmpty() || "[DONE]".equals(jsonData)) {
                            log.debug("Stream encerrado");
                            callback.onMessageEnd(null);
                            break;
                        }
                        
                        try {
                            XingChenResponse event = JsonUtil.fromJson(jsonData, XingChenResponse.class);
                            
                            // Verifica se há erro
                            if (event.getCode() != null && event.getCode() != 0) {
                                log.error("A API retornou erro: code={}, message={}", event.getCode(), event.getMessage());
                                callback.onError(event);
                                continue;
                            }
                            
                            // Verifica se é um evento de chamada de ferramenta
                            if (event.getEventData() != null) {
                                log.debug("Evento de chamada de ferramenta recebido: {}", JsonUtil.toJson(event.getEventData()));
                                callback.onFunctionCall(event);
                            } else if (event.getChoices() != null && !event.getChoices().isEmpty()) {
                                // Evento de mensagem comum
                                XingChenResponse.Choices choice = event.getChoices().get(0);
                                if ("stop".equals(choice.getFinishReason())) {
                                    log.debug("Mensagem encerrada");
                                    callback.onMessageEnd(event);
                                    break;
                                } else {
                                    callback.onMessage(event);
                                }
                            }
                        } catch (Exception e) {
                            log.error("Falha ao interpretar os dados de resposta: {}", jsonData, e);
                            callback.onException(e);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Falha na requisição em streaming", e);
            callback.onException(e);
        }
    }

    /**
     * Envia a requisição de Resume (usada para continuar a conversa após a chamada de ferramenta)
     */
    public void resume(XingChenResume resume, XingChenChatStreamCallback callback) throws IOException {
        log.debug("Enviando a requisição de Resume: {}", JsonUtil.toJson(resume));
        
        String jsonBody = JsonUtil.toJson(resume);
        
        Request httpRequest = new Request.Builder()
                .url(baseUrl + RESUME_PATH)
                .post(RequestBody.create(jsonBody, JSON))
                .addHeader("Authorization", "Bearer " + bearerToken)
                .addHeader("Content-Type", "application/json")
                .build();
        
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "sem corpo de resposta";
                log.error("Falha na requisição de Resume: code={}, body={}", response.code(), errorBody);
                callback.onException(new IOException("Falha na requisição de Resume: " + response.code() + ", " + errorBody));
                return;
            }
            
            ResponseBody body = response.body();
            if (body == null) {
                callback.onException(new IOException("Corpo da resposta vazio"));
                return;
            }
            
            // Processa o stream SSE
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    
                    // Processa a linha de dados
                    if (line.startsWith(DATA_PREFIX)) {
                        String jsonData = line.substring(DATA_PREFIX.length()).trim();
                        if (jsonData.isEmpty() || "[DONE]".equals(jsonData)) {
                            log.debug("Stream do Resume encerrado");
                            callback.onMessageEnd(null);
                            break;
                        }
                        
                        try {
                            XingChenResponse event = JsonUtil.fromJson(jsonData, XingChenResponse.class);
                            
                            // Verifica se há erro
                            if (event.getCode() != null && event.getCode() != 0) {
                                log.error("O Resume retornou erro: code={}, message={}", event.getCode(), event.getMessage());
                                callback.onError(event);
                                continue;
                            }
                            
                            // Verifica a flag de encerramento
                            if (event.getChoices() != null && !event.getChoices().isEmpty()) {
                                XingChenResponse.Choices choice = event.getChoices().get(0);
                                if ("stop".equals(choice.getFinishReason())) {
                                    log.debug("Mensagem do Resume encerrada");
                                    callback.onMessageEnd(event);
                                    break;
                                }
                            }
                            
                            callback.onMessage(event);
                        } catch (Exception e) {
                            log.error("Falha ao interpretar a resposta do Resume: {}", jsonData, e);
                            callback.onException(e);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Falha na requisição de Resume", e);
            callback.onException(e);
        }
    }
}
