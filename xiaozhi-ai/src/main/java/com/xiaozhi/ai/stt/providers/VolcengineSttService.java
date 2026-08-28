package com.xiaozhi.ai.stt.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xiaozhi.ai.stt.SttResult;
import com.xiaozhi.ai.stt.SttService;
import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.ai.utils.HttpUtil;

import okhttp3.*;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayInputStream;

import lombok.extern.slf4j.Slf4j;
/**
 * Serviço de reconhecimento de voz em streaming de grande modelo da Volcengine
 * Implementado com base no protocolo binário WebSocket
 * 
 * @see <a href="https://www.volcengine.com/docs/6561/1354869">API de reconhecimento de voz em streaming de grande modelo</a>
 */
@Slf4j
public class VolcengineSttService implements SttService {
    private static final String PROVIDER_NAME = "volcengine";

    // Endereço da API WebSocket
    private static final String WS_API_URL = "wss://openspeech.bytedance.com/api/v3/sauc/bigmodel_async";

    // Tempo limite de reconhecimento (90 segundos)
    private static final long RECOGNITION_TIMEOUT_MS = 90000;
    // Tempo limite de espera da fila
    private static final int QUEUE_TIMEOUT_MS = 100;

    // Constantes do protocolo
    private static final byte PROTOCOL_VERSION = 0b0001;
    private static final byte HEADER_SIZE = 0b0001;
    private static final byte FULL_CLIENT_REQUEST = 0b0001;
    private static final byte AUDIO_ONLY_REQUEST = 0b0010;
    private static final byte FULL_SERVER_RESPONSE = (byte) 0b1001;
    private static final byte SERVER_ERROR_RESPONSE = (byte) 0b1111;
    private static final byte JSON_SERIALIZATION = 0b0001;
    private static final byte GZIP_COMPRESSION = 0b0001;
    private static final byte NO_SEQUENCE = 0b0000;
    private static final byte LAST_PACKET = 0b0010;

    private final String appId;
    private final String accessToken;
    private final String resourceId;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public VolcengineSttService(ConfigBO config) {
        this.appId = config.getAppId();
        this.accessToken = config.getApiKey();
        // Usa fixamente o modelo de reconhecimento de voz em streaming Doubao versão 1.0 hora
        this.resourceId = "volc.bigasr.sauc.duration";
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public SttResult stream(Flux<byte[]> audioFlux) {
        // Verifica se a configuração já foi definida
        if (appId == null || accessToken == null) {
            log.error("A configuração de reconhecimento de voz da Volcengine não foi definida; não é possível reconhecer");
            return null;
        }

        String connectId = UUID.randomUUID().toString();
        AtomicReference<SttResult> finalResult = new AtomicReference<>(SttResult.textOnly(""));
        AtomicBoolean isCompleted = new AtomicBoolean(false);
        AtomicBoolean latchReleased = new AtomicBoolean(false);
        CountDownLatch recognitionLatch = new CountDownLatch(1);
        BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
        AtomicReference<WebSocket> webSocketRef = new AtomicReference<>();

        // Assina o fluxo de áudio
        audioFlux.subscribe(
                data -> audioQueue.offer(data),
                error -> {
                    log.error("Erro no processamento do fluxo de áudio", error);
                    isCompleted.set(true);
                },
                () -> isCompleted.set(true)
        );

        // Monta a requisição
        Request request = new Request.Builder()
                .url(WS_API_URL)
                .addHeader("X-Api-App-Key", appId)
                .addHeader("X-Api-Access-Key", accessToken)
                .addHeader("X-Api-Resource-Id", resourceId)
                .addHeader("X-Api-Connect-Id", connectId)
                .build();

        HttpUtil.client.newWebSocket(request, new WebSocketListener() {
            private final StringBuilder textBuilder = new StringBuilder();

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                webSocketRef.set(webSocket);

                // Envia o full client request
                try {
                    byte[] fullRequest = buildFullClientRequest();
                    webSocket.send(okio.ByteString.of(fullRequest));
                } catch (Exception e) {
                    log.error("Falha ao enviar o full client request", e);
                    webSocket.close(1000, "Falha ao enviar a requisição");
                }

                // Inicia uma virtual thread para enviar os dados de áudio
                Thread.startVirtualThread(() -> {
                    try {
                        while (!isCompleted.get() || !audioQueue.isEmpty()) {
                            byte[] audioChunk = audioQueue.poll(QUEUE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                            if (audioChunk != null && audioChunk.length > 0) {
                                try {
                                    byte[] audioRequest = buildAudioRequest(audioChunk, false);
                                    webSocket.send(okio.ByteString.of(audioRequest));
                                } catch (Exception e) {
                                    log.error("Erro ao enviar os dados de áudio", e);
                                    break;
                                }
                            }
                        }

                        // Envia o último pacote (áudio vazio, marcando o fim)
                        try {
                            byte[] lastRequest = buildAudioRequest(new byte[0], true);
                            webSocket.send(okio.ByteString.of(lastRequest));
                        } catch (Exception e) {
                            log.error("Erro ao enviar o último pacote", e);
                        }
                    } catch (Exception e) {
                        log.error("Erro ao processar o fluxo de áudio", e);
                    }
                });
            }

            @Override
            public void onMessage(WebSocket webSocket, okio.ByteString bytes) {
                try {
                    parseServerResponse(bytes.toByteArray(), textBuilder, finalResult, recognitionLatch, latchReleased, connectId);
                } catch (Exception e) {
                    log.error("Falha ao interpretar a resposta do servidor", e);
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("Falha no reconhecimento da Volcengine", t);
                if (latchReleased.compareAndSet(false, true)) {
                    recognitionLatch.countDown();
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                if (latchReleased.compareAndSet(false, true)) {
                    recognitionLatch.countDown();
                }
            }
        });

        try {
            // Aguarda a conclusão do reconhecimento ou o timeout
            boolean recognized = recognitionLatch.await(RECOGNITION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!recognized) {
                log.warn("Timeout no reconhecimento da Volcengine - ConnectId: {}", connectId);
            }
        } catch (InterruptedException e) {
            log.error("Interrompido ao aguardar o resultado do reconhecimento", e);
            Thread.currentThread().interrupt();
        } finally {
            // Garante o fechamento da conexão WebSocket
            WebSocket ws = webSocketRef.get();
            if (ws != null) {
                ws.close(1000, "Reconhecimento concluído");
            }
        }

        return finalResult.get();
    }

    /**
     * Monta a mensagem full client request
     */
    private byte[] buildFullClientRequest() throws Exception {
        // Monta o JSON da requisição
        ObjectNode requestJson = objectMapper.createObjectNode();

        // Configuração de user
        ObjectNode user = objectMapper.createObjectNode();
        user.put("uid", "xiaozhi-" + UUID.randomUUID().toString().substring(0, 8));
        requestJson.set("user", user);

        // Configuração de audio
        ObjectNode audio = objectMapper.createObjectNode();
        audio.put("format", "pcm");
        audio.put("codec", "raw");
        audio.put("rate", 16000);
        audio.put("bits", 16);
        audio.put("channel", 1);
        requestJson.set("audio", audio);

        // Configuração de request
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model_name", "bigmodel");
        request.put("enable_itn", true);
        request.put("enable_punc", true);
        request.put("enable_ddc", false);
        request.put("show_utterances", true);
        request.put("result_type", "full");
        request.put("enable_emotion_detection", true);
        requestJson.set("request", request);

        String jsonStr = objectMapper.writeValueAsString(requestJson);
        byte[] jsonBytes = jsonStr.getBytes("UTF-8");

        // Compressão Gzip
        byte[] compressedPayload = gzipCompress(jsonBytes);

        // Monta a mensagem binária
        return buildBinaryMessage(FULL_CLIENT_REQUEST, NO_SEQUENCE, JSON_SERIALIZATION, GZIP_COMPRESSION, compressedPayload);
    }

    /**
     * Monta a mensagem audio only request
     */
    private byte[] buildAudioRequest(byte[] audioData, boolean isLast) throws Exception {
        // Comprime os dados de áudio com Gzip
        byte[] compressedPayload = gzipCompress(audioData);

        byte flags = isLast ? LAST_PACKET : NO_SEQUENCE;

        // Monta a mensagem binária
        return buildBinaryMessage(AUDIO_ONLY_REQUEST, flags, (byte) 0b0000, GZIP_COMPRESSION, compressedPayload);
    }

    /**
     * Monta a mensagem binária
     */
    private byte[] buildBinaryMessage(byte messageType, byte flags, byte serialization, byte compression, byte[] payload) {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + payload.length);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Header (4 bytes)
        byte byte0 = (byte) ((PROTOCOL_VERSION << 4) | HEADER_SIZE);
        byte byte1 = (byte) ((messageType << 4) | flags);
        byte byte2 = (byte) ((serialization << 4) | compression);
        byte byte3 = 0x00; // Reserved

        buffer.put(byte0);
        buffer.put(byte1);
        buffer.put(byte2);
        buffer.put(byte3);

        // Payload size (4 bytes, big-endian)
        buffer.putInt(payload.length);

        // Payload
        buffer.put(payload);

        return buffer.array();
    }

    /**
     * Interpreta a resposta do servidor
     */
    private void parseServerResponse(byte[] data, StringBuilder textBuilder,
            AtomicReference<SttResult> finalResult, CountDownLatch latch, AtomicBoolean latchReleased,
            String connectId) throws Exception {
        if (data.length < 4) {
            log.warn("Dados de resposta muito curtos");
            return;
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Interpreta o header (4 bytes)
        buffer.get(); // byte0: protocol version & header size, ignorado
        byte byte1 = buffer.get();
        byte byte2 = buffer.get();
        buffer.get(); // Reserved byte, ignorado

        // Interpreta os campos (usa apenas os campos necessários)
        int messageType = (byte1 >> 4) & 0x0F;
        int flags = byte1 & 0x0F;
        int compression = byte2 & 0x0F;

        // Verifica se há sequence number (flags contém 0b0001 ou 0b0011)
        boolean hasSequence = (flags & 0b0001) != 0;
        if (hasSequence && buffer.remaining() >= 4) {
            buffer.getInt(); // Lê e ignora o sequence number
        }

        // Verifica o tipo de mensagem
        if (messageType == (SERVER_ERROR_RESPONSE & 0x0F)) {
            // Mensagem de erro
            if (buffer.remaining() >= 8) {
                int errorCode = buffer.getInt();
                int errorMsgSize = buffer.getInt();
                if (buffer.remaining() >= errorMsgSize) {
                    byte[] errorMsgBytes = new byte[errorMsgSize];
                    buffer.get(errorMsgBytes);
                    String errorMsg = new String(errorMsgBytes, "UTF-8");
                    log.error("Erro de reconhecimento da Volcengine - Code: {}, Message: {}", errorCode, errorMsg);
                }
            }
            if (latchReleased.compareAndSet(false, true)) {
                latch.countDown();
            }
            return;
        }

        if (messageType != (FULL_SERVER_RESPONSE & 0x0F)) {
            return;
        }

        // Lê o payload
        if (buffer.remaining() < 4) {
            return;
        }

        int payloadSize = buffer.getInt();
        if (buffer.remaining() < payloadSize) {
            log.warn("Dados do Payload incompletos");
            return;
        }

        byte[] payload = new byte[payloadSize];
        buffer.get(payload);

        // Descompacta
        byte[] decompressedPayload;
        if (compression == GZIP_COMPRESSION) {
            decompressedPayload = gzipDecompress(payload);
        } else {
            decompressedPayload = payload;
        }

        // Interpreta o JSON
        String jsonStr = new String(decompressedPayload, "UTF-8");
        JsonNode responseJson = objectMapper.readTree(jsonStr);

        // Extrai o resultado do reconhecimento
        if (responseJson.has("result")) {
            JsonNode result = responseJson.get("result");
            if (result.has("text")) {
                String text = result.get("text").asText();
                if (text != null && !text.isEmpty()) {
                    synchronized (textBuilder) {
                        textBuilder.setLength(0);
                        textBuilder.append(text);
                    }
                    // Extrai a emoção a partir de utterances (usa a emoção da frase com definite=true)
                    String topEmotion = null;
                    Double topEmotionScore = null;
                    String topEmotionDegree = null;
                    Double topEmotionDegreeScore = null;
                    if (result.has("utterances")) {
                        for (JsonNode utterance : result.get("utterances")) {
                            if (utterance.path("definite").asBoolean(false)
                                    && utterance.has("additions")) {
                                JsonNode additions = utterance.get("additions");
                                String emotion = additions.path("emotion").asText(null);
                                if (emotion != null && !emotion.isEmpty()) {
                                    topEmotion = emotion;
                                    topEmotionScore = additions.path("emotion_score").asDouble(0) > 0 ? additions.path("emotion_score").asDouble() : null;
                                    topEmotionDegree = additions.path("emotion_degree").asText(null);
                                    topEmotionDegreeScore = additions.path("emotion_degree_score").asDouble(0) > 0 ? additions.path("emotion_degree_score").asDouble() : null;
                                }
                            }
                        }
                    }
                    SttResult sttResult = topEmotion != null
                            ? SttResult.withFullEmotion(text, topEmotion, topEmotionScore, topEmotionDegree, topEmotionDegreeScore)
                            : SttResult.textOnly(text);
                    finalResult.set(sttResult);
                }
            }
        }

        // Verifica se é o último pacote de resposta (flags contém 0b0010 ou 0b0011)
        boolean isLast = (flags & 0b0010) != 0;
        if (isLast) {
            SttResult current = finalResult.get();
            log.info("Reconhecimento de voz concluído (volcengine): {} [emoção: {}, confiança: {}, intensidade: {}, confiança da intensidade: {}]",
                    current.text(), current.emotion(), current.emotionScore(),
                    current.emotionDegree(), current.emotionDegreeScore());
            if (latchReleased.compareAndSet(false, true)) {
                latch.countDown();
            }
        }
    }

    /**
     * Compressão Gzip
     */
    private byte[] gzipCompress(byte[] data) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
        }
        return bos.toByteArray();
    }

    /**
     * Descompressão Gzip
     */
    private byte[] gzipDecompress(byte[] data) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPInputStream gzip = new GZIPInputStream(bis)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzip.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
        }
        return bos.toByteArray();
    }
}
