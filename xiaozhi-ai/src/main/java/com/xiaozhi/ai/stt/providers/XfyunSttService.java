package com.xiaozhi.ai.stt.providers;

import cn.xfyun.model.response.iat.IatResponse;
import cn.xfyun.model.response.iat.Text;
import com.google.gson.JsonObject;
import com.xiaozhi.ai.stt.SttResult;
import com.xiaozhi.ai.stt.SttService;
import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.ai.utils.HttpUtil;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static cn.xfyun.util.StringUtils.gson;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XfyunSttService implements SttService {
    public static final int StatusFirstFrame = 0;
    public static final int StatusContinueFrame = 1;
    public static final int StatusLastFrame = 2;

    private static final String PROVIDER_NAME = "xfyun";

    // Tempo limite de reconhecimento (90 segundos)
    private static final long RECOGNITION_TIMEOUT_MS = 90000;

    private static final String hostUrl = "https://iat-api.xfyun.cn/v2/iat";

    private String secretId;
    private String secretKey;
    private String appId;

    public XfyunSttService(ConfigBO config) {
        if (config != null) {
            this.secretId = config.getApiKey();
            this.secretKey = config.getApiSecret();
            this.appId = config.getAppId();
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    /**
     * Processa o resultado retornado (inclui retorno completo e retorno em streaming (correção de resultado))
     */
    private void handleResultText(Text textObject, List<Text> resultSegments) {
        // Processa o resultado de substituição do retorno em streaming
        if ("rpl".equals(textObject.getPgs()) && textObject.getRg() != null && textObject.getRg().length == 2) {
            // O valor mínimo do campo sn (número de sequência) do resultado retornado é 1
            int start = textObject.getRg()[0] - 1;
            int end = textObject.getRg()[1] - 1;

            // Marca os resultados do intervalo especificado como excluídos
            for (int i = start; i <= end && i < resultSegments.size(); i++) {
                resultSegments.get(i).setDeleted(true);
            }
            // log.info("Operação de substituição, resultado retornado pelo servidor: " + textObject);
        }

        // Lógica geral: adiciona o texto atual à lista de resultados
        resultSegments.add(textObject);
    }

    /**
     * Obtém o resultado final
     */
    private String getFinalResult(List<Text> resultSegments) {
        StringBuilder finalResult = new StringBuilder();
        for (Text text : resultSegments) {
            if (text != null && !text.isDeleted()) {
                finalResult.append(text.getText());
            }
        }
        return finalResult.toString();
    }

    private String getAuthUrl(String apiKey, String apiSecret) throws Exception {
        URL url = URI.create(XfyunSttService.hostUrl).toURL();
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());

        StringBuilder builder = new StringBuilder("host: ").append(url.getHost()).append("\n")
                .append("date: ").append(date).append("\n")
                .append("GET ").append(url.getPath()).append(" HTTP/1.1");

        Charset charset = StandardCharsets.UTF_8;
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "HmacSHA256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
        String sha = Base64.getEncoder().encodeToString(hexDigits);

        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                apiKey, "hmac-sha256", "host date request-line", sha);

        return Objects.requireNonNull(HttpUrl.parse("https://" + url.getHost() + url.getPath()))
                .newBuilder()
                .addQueryParameter("authorization",
                        Base64.getEncoder().encodeToString(authorization.getBytes(charset)))
                .addQueryParameter("date", date)
                .addQueryParameter("host", url.getHost())
                .build()
                .toString();
    }

    @Override
    public SttResult stream(Flux<byte[]> audioSink) {
        // Verifica se a configuração já foi definida
        if (secretId == null || secretKey == null || appId == null) {
            log.error("A configuração de reconhecimento de voz da iFLYTEK Cloud não foi definida; não é possível reconhecer");
            return null;
        }

        // Monta a URL de autenticação
        String authUrl;
        try {
            authUrl = getAuthUrl(secretId, secretKey);
        } catch (Exception e) {
            log.error("Erro ao montar a URL de autenticação!", e);
            return SttResult.textOnly("");
        }

        String wsUrl = authUrl.replace("http://", "ws://")
                .replace("https://", "wss://");
        Request request = new Request.Builder().url(wsUrl).build();
        AtomicInteger status = new AtomicInteger(StatusFirstFrame);
        AtomicReference<WebSocket> webSocketRef = new AtomicReference<>();
        BlockingQueue<JsonObject> frameQueue = new LinkedBlockingQueue<>();
        AtomicBoolean isClosed = new AtomicBoolean(false);
        AtomicBoolean latchReleased = new AtomicBoolean(false);
        CountDownLatch recognitionLatch = new CountDownLatch(1);
        List<Text> resultSegments = new ArrayList<>();

        HttpUtil.client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                webSocketRef.set(webSocket);
                isClosed.set(false);
                // Usa o Flux para assinar o fluxo de áudio
                audioSink.subscribeOn(Schedulers.single())  // Garante a execução em ordem
                        .subscribe(
                                chunk -> {
                                    if (isClosed.get()) return;
                                    try {
                                        if (chunk == null || chunk.length == 0) {
                                            log.debug("Dados do audioSink vazios, pulando este frame");
                                            return;
                                        }
                                        if ((status.compareAndSet(StatusFirstFrame, StatusContinueFrame))) {
                                            log.debug("xfyun começando a enviar o primeiro frame de áudio");
                                            frameQueue.offer(buildFirstFrame(chunk, chunk.length));
                                        } else {
                                            // log.debug("xfyun continuando a enviar frames de áudio");
                                            frameQueue.offer(buildContinueFrame(chunk, chunk.length));
                                        }
                                    } catch (Exception e) {
                                        log.error("Falha ao enviar o frame de áudio", e);
                                    }
                                },
                                error -> {
                                    log.error("Erro no fluxo de áudio", error);
                                },
                                () -> {
                                    if (isClosed.get()) return;
                                    // Fim do fluxo; envia o último frame pela fila, garantindo a ordem dos frames
                                    log.debug("Notificação de encerramento do audioSink enviada");
                                    JsonObject frame = buildLastFrame();
                                    frameQueue.offer(frame);
                                }
                        );
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                if (isClosed.get()) return;
                IatResponse response = gson.fromJson(text, IatResponse.class);
                if (response.getCode() != 0) {
                    log.warn("code:{}, error:{}, sid:{}",
                            response.getCode(), response.getMessage(), response.getSid());
                    return;
                }

                if (response.getData() != null && response.getData().getResult() != null) {
                    Text textObject = response.getData().getResult().getText();
                    handleResultText(textObject, resultSegments);
                }

                if (response.getData() != null && response.getData().getStatus() == 2) {
                    if (latchReleased.compareAndSet(false, true)) {
                        recognitionLatch.countDown();
                    }
                    // wsClose();
                    wsClose(webSocketRef, isClosed); // Fechamento explícito
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("Falha no reconhecimento em streaming", t);
                wsClose(webSocketRef, isClosed); // Fechamento explícito
                isClosed.set(true);
                webSocketRef.set(null);
                if (latchReleased.compareAndSet(false, true)) {
                    recognitionLatch.countDown();
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                wsClose(webSocketRef, isClosed); // Fechamento explícito
                isClosed.set(true);
                webSocketRef.set(null);
                super.onClosed(webSocket, code, reason);
            }
        });

        // Thread de envio de frames
        Thread.startVirtualThread(() -> {
            while (!isClosed.get()) {
                try {
                    JsonObject frame = frameQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (frame != null) {
                        WebSocket ws = webSocketRef.get();
                        if (ws != null) {
                            ws.send(frame.toString());
                        }
                    }
                } catch (Exception e) {
                    log.error("Falha ao enviar o frame de áudio", e);
                }
            }
        });

        try {
            // Aguarda a conclusão do reconhecimento ou o timeout
            boolean recognized = recognitionLatch.await(RECOGNITION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            String finalText = "";
            if (recognized) {
                finalText = getFinalResult(resultSegments);
            } else {
                String partialResult = getFinalResult(resultSegments);
                // Mesmo em caso de timeout, retorna o texto parcial já reconhecido
                if (StringUtils.hasText(partialResult)) {
                    finalText = partialResult;
                }
                wsClose(webSocketRef, isClosed);
            }
            return SttResult.textOnly(finalText);
        } catch (Exception e) {
            log.error("Erro ao criar a sessão de reconhecimento de voz", e);
            wsClose(webSocketRef, isClosed);
            // Fecha proativamente a sessão
            return SttResult.textOnly(getFinalResult(resultSegments));
        }
    }

    private void wsClose(AtomicReference<WebSocket> webSocketRef, AtomicBoolean isClosed) {
        if (isClosed.compareAndSet(false, true)) {
            WebSocket ws = webSocketRef.get();
            if (ws != null) {
                try {
                    ws.close(1000, "Encerramento do programa");
                } catch (Exception e) {
                    log.warn("Exceção ao fechar o WebSocket", e);
                }
            }
        }
    }

    private JsonObject buildFirstFrame(byte[] buffer, int len) {
        JsonObject common = new JsonObject();
        common.addProperty("app_id", appId);

        JsonObject business = new JsonObject();
        business.addProperty("language", "zh_cn");
        business.addProperty("domain", "iat");
        business.addProperty("accent", "mandarin");
        business.addProperty("dwa", "wpgs");

        JsonObject data = new JsonObject();
        data.addProperty("status", StatusFirstFrame);
        data.addProperty("format", "audio/L16;rate=16000");
        data.addProperty("encoding", "raw");
        data.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));

        JsonObject frame = new JsonObject();
        frame.add("common", common);
        frame.add("business", business);
        frame.add("data", data);

        return frame;
    }

    private JsonObject buildContinueFrame(byte[] buffer, int len) {
        JsonObject data = new JsonObject();
        data.addProperty("status", StatusContinueFrame);
        data.addProperty("format", "audio/L16;rate=16000");
        data.addProperty("encoding", "raw");
        data.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));

        JsonObject frame = new JsonObject();
        frame.add("data", data);

        return frame;
    }

    private JsonObject buildLastFrame() {
        JsonObject data = new JsonObject();
        data.addProperty("status", StatusLastFrame);
        data.addProperty("audio", "");
        data.addProperty("format", "audio/L16;rate=16000");
        data.addProperty("encoding", "raw");

        JsonObject frame = new JsonObject();
        frame.add("data", data);

        return frame;
    }
}