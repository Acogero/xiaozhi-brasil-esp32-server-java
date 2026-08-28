package com.xiaozhi.ai.stt.providers;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xiaozhi.ai.stt.SttResult;
import com.xiaozhi.ai.stt.SttService;
import com.xiaozhi.common.model.bo.ConfigBO;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.slf4j.Slf4j;
/**
 * Implementação do serviço STT FunASR
 * <br/>
 * <a href="https://github.com/modelscope/FunASR/blob/main/runtime/docs/SDK_tutorial_online_zh.md">Tutorial de implantação rápida da transcrição de voz em tempo real do FunASR</a>
 *  <br/>
 * <a href="https://github.com/modelscope/FunASR/blob/main/runtime/docs/SDK_advanced_guide_online_zh.md">Guia de desenvolvimento do serviço de transcrição de voz em tempo real do FunASR</a>
 *  <br/>
 * <a href="https://www.funasr.com/static/offline/index.html">Endereço de demonstração</a>
 */
@Slf4j
public class FunASRSttService implements SttService {

    private static final String PROVIDER_NAME = "funasr";

    private static final String SPEAKING_START = "{\"mode\":\"2pass\",\"wav_name\":\"voice.wav\",\"is_speaking\":true,\"wav_format\":\"pcm\",\"chunk_size\":[5,10,5],\"itn\":true}";
    private static final String SPEAKING_END = "{\"is_speaking\": false}";
    private static final int QUEUE_TIMEOUT_MS = 100; // Tempo limite de espera da fila
    private static final long RECOGNITION_TIMEOUT_MS = 90000; // Tempo limite de reconhecimento (90 segundos)

    private final String apiUrl;

    public FunASRSttService(ConfigBO config) {
        this.apiUrl = config.getApiUrl();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public SttResult stream(Flux<byte[]> audioSink) {
        // Usa uma fila bloqueante para armazenar os dados de áudio
        BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
        AtomicBoolean isCompleted = new AtomicBoolean(false);
        // Concatena todos os resultados de correção offline do modo 2pass-offline
        StringBuilder offlineResult = new StringBuilder();
        AtomicReference<String> finalResult = new AtomicReference<>("");
        CountDownLatch recognitionLatch = new CountDownLatch(1);
        
        // Assina o Sink e coloca os dados na fila
        audioSink.subscribe(
            data -> audioQueue.offer(data),
            error -> {
                log.error("Erro no processamento do fluxo de áudio", error);
                isCompleted.set(true);
            },
            () -> isCompleted.set(true)
        );
        
        // Cria o cliente WebSocket
        WebSocketClient webSocketClient = new WebSocketClient(URI.create(apiUrl)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                log.debug("Conexão WebSocket do FunASR aberta");
                send(SPEAKING_START);
                
                // Inicia uma virtual thread para enviar os dados de áudio
                Thread.startVirtualThread(() -> {
                    try {
                        while (!isCompleted.get() || !audioQueue.isEmpty()) {
                            byte[] audioChunk = null;
                            try {
                                audioChunk = audioQueue.poll(QUEUE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                            } catch (InterruptedException e) {
                                log.warn("Espera na fila de dados de áudio interrompida", e);
                                Thread.currentThread().interrupt(); // Restaura o flag de interrupção
                                break;
                            }
                            
                            if (audioChunk != null && isOpen()) {
                                send(audioChunk);
                            }
                        }
                        
                        // Envia o sinal de encerramento
                        if (isOpen()) {
                            send(SPEAKING_END);
                        }
                    } catch (Exception e) {
                        log.error("Erro ao enviar os dados de áudio", e);
                    }
                });
            }

            @Override
            public void onMessage(String message) {
                try {
                    JSONObject jsonObject = JSON.parseObject(message);
                    boolean isFinal = Boolean.TRUE.equals(jsonObject.getBoolean("is_final"));
                    String mode = jsonObject.getString("mode");
                    String text = jsonObject.getString("text");
                    // Modo 2pass: concatena cada trecho de correção offline (o VAD pode dividir uma frase em vários trechos)
                    if (isFinal && "2pass-offline".equals(mode)) {
                        if (text != null && !text.isEmpty()) {
                            offlineResult.append(text);
                        }
                        log.debug("Trecho de correção offline do FunASR: {}", text);
                    }
                } catch (Exception e) {
                    log.error("Falha ao interpretar a resposta do FunASR", e);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                log.info("WebSocket do FunASR encerrado, motivo: {}", reason);
                // Ao encerrar a conexão, todos os resultados de correção offline já foram recebidos; define o resultado final
                finalResult.set(offlineResult.toString());
                recognitionLatch.countDown();
            }

            @Override
            public void onError(Exception ex) {
                log.error("Erro no WebSocket do FunASR", ex);
                // Define primeiro o resultado já obtido, e só então libera o lock, evitando que a thread principal leia um resultado vazio
                finalResult.set(offlineResult.toString());
                recognitionLatch.countDown();
            }
        };

        try {
            // Conecta o WebSocket
            webSocketClient.connect();
            
            // Aguarda a conclusão do reconhecimento ou o timeout
            boolean recognized = recognitionLatch.await(RECOGNITION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            if (!recognized) {
                log.warn("Timeout no reconhecimento do FunASR");
            }
        } catch (Exception e) {
            log.error("Erro durante o reconhecimento do FunASR", e);
        } finally {
            // Fecha a conexão WebSocket
            if (webSocketClient.isOpen()) {
                webSocketClient.close();
            }
        }
        
        return SttResult.textOnly(finalResult.get());
    }
}