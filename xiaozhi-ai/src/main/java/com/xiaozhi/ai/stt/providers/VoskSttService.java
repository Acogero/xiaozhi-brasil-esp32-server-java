package com.xiaozhi.ai.stt.providers;

import com.xiaozhi.ai.stt.SttResult;
import com.xiaozhi.ai.stt.SttService;
import com.xiaozhi.utils.AudioUtils;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import reactor.core.publisher.Flux;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;
/**
 * Implementação do serviço STT Vosk
 * Implementa o processamento assíncrono usando virtual threads do JDK 21
 */
@Slf4j
public class VoskSttService implements SttService {

    private static final String PROVIDER_NAME = "vosk";

    // Usa um pool de platform threads para executar as tarefas de reconhecimento JNI native, evitando conflitos de vinculação de memória nativa com virtual threads
    private static final ExecutorService recognizerExecutor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    static {
        // Registra um shutdown hook da JVM, garantindo que o pool de threads seja encerrado corretamente
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            recognizerExecutor.shutdown();
            try {
                if (!recognizerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    recognizerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                recognizerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }, "vosk-stt-shutdown"));
    }

    // Objetos relacionados ao modelo Vosk
    private Model model;
    private String voskModelPath;
    private boolean modelLoaded = false;
    private final String nativeLibDir;

    public VoskSttService(String nativeLibDir, String voskModelDir) {
        this.nativeLibDir = nativeLibDir;
        this.voskModelPath = voskModelDir;
    }

    /**
     * Inicializa o modelo Vosk
     *
     * @throws Exception Se o carregamento do modelo falhar
     */
    @PostConstruct
    public void initialize() throws Exception {
        try {
            // Verifica se é o sistema operacional macOS
            String osName = System.getProperty("os.name").toLowerCase();
            // Verifica se é arquitetura ARM (para chips da série M)
            String osArch = System.getProperty("os.arch").toLowerCase();

            if (osName.contains("mac") && osArch.contains("aarch64")) {
                // Se for macOS e arquitetura ARM (chips da série M)
                Path libPath = Path.of(nativeLibDir).toAbsolutePath().normalize().resolve("libvosk.dylib");
                System.load(libPath.toString());
                log.info("Vosk library loaded for macOS M-series chip.");
            } else {
                log.info("Not macOS M-series chip, skipping Vosk library load.");
            }
            // Desativa a saída de log do Vosk
            LibVosk.setLogLevel(LogLevel.WARNINGS);

            // Carrega o modelo, usando o diretório configurado
            voskModelPath = Path.of(voskModelPath).toAbsolutePath().normalize().toString();
            if (!Files.isDirectory(Path.of(voskModelPath))) {
                throw new Exception("Vosk model directory not found: " + voskModelPath);
            }
            model = new Model(voskModelPath);
            modelLoaded = true;
            log.info("Modelo Vosk carregado com sucesso! Caminho: {}", voskModelPath);
        } catch (Exception e) {
            modelLoaded = false;
            log.warn("Falha ao carregar o modelo Vosk! Outro serviço STT será usado: {}", e.getMessage());
            throw new Exception("Vosk model loading failed: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica se o modelo foi carregado com sucesso
     *
     * @return true se o modelo foi carregado com sucesso; caso contrário, false
     */
    public boolean isModelLoaded() {
        return modelLoaded && model != null;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public SttResult stream(Flux<byte[]> audioSink) {
        if (!isModelLoaded()) {
            log.error("O modelo Vosk não foi carregado; não é possível realizar o reconhecimento em streaming!");
            return null;
        }

        // Usa uma fila bloqueante para armazenar os dados de áudio
        BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
        AtomicBoolean isCompleted = new AtomicBoolean(false);
        List<String> recognizedText = new ArrayList<>();
        StringBuilder finalResult = new StringBuilder();

        // Assina o Sink e coloca os dados na fila
        audioSink.subscribe(
                data -> audioQueue.offer(data),
                error -> {
                    log.error("Erro no processamento do fluxo de áudio", error);
                    isCompleted.set(true);
                },
                () -> isCompleted.set(true)
        );

        // Usa o pool de platform threads para executar a tarefa de reconhecimento, evitando conflitos de vinculação de memória nativa com virtual threads
        Future<?> future = recognizerExecutor.submit(() -> {
            try (Recognizer recognizer = new Recognizer(model, AudioUtils.SAMPLE_RATE)) {
                while (!isCompleted.get() || !audioQueue.isEmpty()) {
                    try {
                        byte[] audioChunk = audioQueue.poll(100, TimeUnit.MILLISECONDS);
                        if (audioChunk != null) {
                            boolean hasResult = recognizer.acceptWaveForm(audioChunk, audioChunk.length);
                            if (hasResult) {
                                // Extrai o texto do resultado parcial de reconhecimento
                                String result = recognizer.getResult();
                                JSONObject jsonResult = new JSONObject(result);
                                if (jsonResult.has("text") && !jsonResult.getString("text").isEmpty()) {
                                    String text = jsonResult.getString("text").replaceAll("\\s+", "");
                                    recognizedText.add(text);
                                    log.debug("Resultado intermediário do reconhecimento Vosk: {}", text);
                                }
                            }
                        }

                        // Se já concluído e a fila estiver vazia, obtém o resultado final
                        if (isCompleted.get() && audioQueue.isEmpty()) {
                            String finalText = recognizer.getFinalResult();
                            JSONObject jsonFinal = new JSONObject(finalText);
                            if (jsonFinal.has("text")) {
                                String text = jsonFinal.getString("text").replaceAll("\\s+", "");
                                if (!text.isEmpty()) {
                                    recognizedText.add(text);
                                    log.debug("Resultado final do reconhecimento Vosk: {}", text);
                                }
                            }
                            break;
                        }
                    } catch (InterruptedException e) {
                        log.warn("Espera na fila de dados de áudio interrompida", e);
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                // Mescla todos os resultados de reconhecimento
                for (String text : recognizedText) {
                    finalResult.append(text);
                }

            } catch (Exception e) {
                log.error("Erro durante o reconhecimento em streaming do Vosk", e);
            }
        });

        try {
            future.get(90, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Interrompido ao aguardar a conclusão do reconhecimento do Vosk", e);
            Thread.currentThread().interrupt();
            future.cancel(true);
        } catch (Exception e) {
            log.error("Falha na execução da tarefa de reconhecimento do Vosk", e);
            future.cancel(true);
        }

        return SttResult.textOnly(finalResult.toString());
    }
}
