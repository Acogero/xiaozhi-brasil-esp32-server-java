package com.xiaozhi.ai.tts.providers;

import com.k2fsa.sherpa.onnx.*;
import com.xiaozhi.ai.tts.TtsService;
import com.xiaozhi.ai.tts.XiaozhiTtsOptions;
import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.utils.AudioUtils;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
/**
 * Serviço de síntese de voz local baseado no sherpa-onnx
 * Suporta diversos modelos TTS locais, como VITS, Kokoro, Matcha, entre outros
 *
 * Formato do voiceName: modelDir:modelType:speakerId
 *   Exemplo: vits-melo-tts-zh_en:vits:0
 *         kokoro-multi-lang:kokoro:3
 *         matcha-zh-baker:matcha:0
 */
@Slf4j
public class SherpaOnnxTtsService implements TtsService {
    private static final String PROVIDER_NAME = "sherpa-onnx";

    // Cache de instâncias OfflineTts, evitando o recarregamento do modelo (key = modelPath)
    private static final Map<String, OfflineTts> ttsCache = new ConcurrentHashMap<>();

    // Garante que ensureNativeSearchPath() só rode uma vez por JVM
    private static volatile boolean nativeSearchPathConfigured = false;

    /**
     * No Windows, ajusta a ordem de busca de DLLs nativas via SetDllDirectory, apontando para o
     * mesmo diretório de "lib" nativas do java.library.path (ex.: lib/), ANTES de qualquer classe
     * do sherpa-onnx ser carregada.
     * <p>
     * Isso evita que uma onnxruntime.dll de outra origem já presente no sistema (ex.:
     * C:\Windows\System32\onnxruntime.dll, instalada por outro programa) seja encontrada
     * primeiro pelo carregador de DLLs do Windows e usada no lugar da versão empacotada junto
     * com o sherpa-onnx-jni.dll, o que causa erro de incompatibilidade de versão da API do ONNX
     * Runtime (e pode até derrubar a JVM com EXCEPTION_ACCESS_VIOLATION).
     * <p>
     * Com SetDllDirectory, a ordem de busca passa a ser: (1) diretório do executável,
     * (2) diretório informado aqui, (3) System32, (4) diretório do Windows, (5) PATH — ou seja,
     * nosso diretório de libs nativas passa a ser buscado antes do System32.
     */
    private static void ensureNativeSearchPath() {
        if (nativeSearchPathConfigured) {
            return;
        }
        synchronized (SherpaOnnxTtsService.class) {
            if (nativeSearchPathConfigured) {
                return;
            }
            nativeSearchPathConfigured = true;

            String osName = System.getProperty("os.name", "");
            if (!osName.toLowerCase().contains("win")) {
                return;
            }

            try {
                String libraryPath = System.getProperty("java.library.path", "");
                String firstDir = libraryPath.split(File.pathSeparator)[0];
                if (firstDir == null || firstDir.isBlank()) {
                    return;
                }
                String absoluteDir = Path.of(firstDir).toAbsolutePath().normalize().toString();
                boolean ok = Kernel32Native.INSTANCE.SetDllDirectoryW(new com.sun.jna.WString(absoluteDir));
                if (ok) {
                    log.info("Diretório de busca de DLLs nativas ajustado (SetDllDirectory): {}", absoluteDir);
                } else {
                    log.warn("SetDllDirectory retornou falha para o diretório: {}", absoluteDir);
                }
            } catch (Throwable t) {
                log.warn("Não foi possível ajustar o diretório de busca de DLLs nativas via SetDllDirectory: {}", t.getMessage());
            }
        }
    }

    private final XiaozhiTtsOptions options;
    private final String outputPath;

    // Caminho do diretório do modelo
    private final String modelPath;
    // Tipo do modelo: kokoro, vits, matcha
    private final String modelType;
    // Speaker ID
    private final int speakerId;

    public SherpaOnnxTtsService(
            ConfigBO config,
            String voiceName,
            Double pitch,
            Double speed,
            String outputPath,
            String ttsModelsDir) {
        this.options = XiaozhiTtsOptions.builder().voiceName(voiceName).pitch(pitch).speed(speed).build();
        this.outputPath = outputPath;

        // Interpreta o voiceName, formato: modelDir:modelType:speakerId
        // Ex.: vits-melo-tts-zh_en:vits:0, kokoro-multi-lang:kokoro:3
        String[] parts = voiceName != null ? voiceName.split(":") : new String[]{};
        if (parts.length != 3) {
            throw new IllegalArgumentException("Formato de voiceName inválido; esperado modelDir:modelType:speakerId, recebido: " + voiceName);
        }
        this.modelPath = Path.of(ttsModelsDir).toAbsolutePath().normalize().resolve(parts[0]).toString();
        this.modelType = parts[1].toLowerCase();
        this.speakerId = parseSpeakerId(parts[2]);
    }

    private int parseSpeakerId(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public XiaozhiTtsOptions getOptions() {
        return options;
    }

    @Override
    public Path textToSpeech(String text) throws Exception {
        try {
            OfflineTts tts = getOrCreateTts();
            float ttsSpeed = (getSpeed() != null) ? getSpeed().floatValue() : 1.0f;

            long start = System.currentTimeMillis();
            GeneratedAudio audio = tts.generate(text, speakerId, ttsSpeed);
            long elapsed = System.currentTimeMillis() - start;

            if (audio == null || audio.getSamples() == null || audio.getSamples().length == 0) {
                log.error("A síntese de voz do sherpa-onnx retornou áudio vazio, caminho do modelo: {}", modelPath);
                return null;
            }

            float audioDuration = audio.getSamples().length / (float) audio.getSampleRate();
            float rtf = (elapsed / 1000.0f) / audioDuration;
            log.info("Síntese de voz do sherpa-onnx concluída - tempo decorrido: {}ms, duração do áudio: {}s, RTF: {}",
                    elapsed, String.format("%.2f", audioDuration), String.format("%.3f", rtf));

            // Converte float[] samples para 16-bit PCM byte[]
            byte[] pcmData = AudioUtils.floatToPcm16(audio.getSamples());

            // Se a taxa de amostragem não for 16000, é necessário reamostrar
            int sampleRate = audio.getSampleRate();
            if (sampleRate != AudioUtils.SAMPLE_RATE) {
                pcmData = AudioUtils.resamplePcm(pcmData, sampleRate, AudioUtils.SAMPLE_RATE);
            }

            // Salva como arquivo WAV
            Path outPath = Path.of(outputPath, getAudioFileName());
            AudioUtils.saveAsWav(outPath, pcmData);

            return outPath;
        } catch (Exception e) {
            log.error("Falha na síntese de voz do sherpa-onnx - caminho do modelo: {}, erro: {}", modelPath, e.getMessage(), e);
            throw new Exception("Falha na síntese de voz local: " + e.getMessage());
        }
    }

    /**
     * Obtém ou cria uma instância de OfflineTts (com cache)
     */
    private OfflineTts getOrCreateTts() {
        String cacheKey = modelPath + ":" + modelType;
        return ttsCache.computeIfAbsent(cacheKey, k -> createTts());
    }

    /**
     * Cria uma instância de OfflineTts com base no tipo de modelo
     */
    private OfflineTts createTts() {
        ensureNativeSearchPath();
        log.info("Inicializando o modelo TTS do sherpa-onnx - tipo: {}, caminho: {}", modelType, modelPath);

        OfflineTtsModelConfig.Builder modelConfigBuilder = OfflineTtsModelConfig.builder()
                .setNumThreads(2)
                .setDebug(false)
                .setProvider("cpu");

        OfflineTtsConfig.Builder ttsConfigBuilder = OfflineTtsConfig.builder();
        File dir = new File(modelPath);

        switch (modelType) {
            case "kokoro" -> {
                OfflineTtsKokoroModelConfig kokoroConfig = OfflineTtsKokoroModelConfig.builder()
                        .setModel(findOnnxModelFile(dir))
                        .setVoices(findFile(dir, "voices.bin"))
                        .setTokens(findFile(dir, "tokens.txt"))
                        .setDataDir(findDir(dir, "espeak-ng-data"))
                        .setLexicon(findLexicons(dir))
                        .build();
                modelConfigBuilder.setKokoro(kokoroConfig);
            }
            case "vits" -> {
                OfflineTtsVitsModelConfig vitsConfig = OfflineTtsVitsModelConfig.builder()
                        .setModel(findOnnxModelFile(dir))
                        .setTokens(findFile(dir, "tokens.txt"))
                        .setLexicon(findFileOptional(dir, "lexicon.txt"))
                        .setDataDir(findDirOptional(dir, "espeak-ng-data"))
                        .setDictDir(findDirOptional(dir, "dict"))
                        .build();
                modelConfigBuilder.setVits(vitsConfig);
                // Define rule fsts
                String ruleFsts = findRuleFsts(dir);
                if (!ruleFsts.isEmpty()) {
                    ttsConfigBuilder.setRuleFsts(ruleFsts);
                }
            }
            case "matcha" -> {
                OfflineTtsMatchaModelConfig matchaConfig = OfflineTtsMatchaModelConfig.builder()
                        .setAcousticModel(findFileByPattern(dir, "model-steps"))
                        .setVocoder(findFileByPattern(dir, "vocoder", "vocos"))
                        .setTokens(findFile(dir, "tokens.txt"))
                        .setLexicon(findFileOptional(dir, "lexicon.txt"))
                        .setDataDir(findDirOptional(dir, "espeak-ng-data"))
                        .setDictDir(findDirOptional(dir, "dict"))
                        .build();
                modelConfigBuilder.setMatcha(matchaConfig);
                String ruleFsts = findRuleFsts(dir);
                if (!ruleFsts.isEmpty()) {
                    ttsConfigBuilder.setRuleFsts(ruleFsts);
                }
            }
            default -> throw new RuntimeException("Tipo de modelo TTS do sherpa-onnx não suportado: " + modelType);
        }

        OfflineTtsConfig config = ttsConfigBuilder
                .setModel(modelConfigBuilder.build())
                .build();

        return new OfflineTts(config);
    }

    // ========== Métodos auxiliares de busca de arquivos ==========

    private String findFile(File dir, String name) {
        File f = new File(dir, name);
        if (!f.exists()) {
            throw new RuntimeException("Arquivo do modelo não existe: " + f.getAbsolutePath());
        }
        return f.getAbsolutePath();
    }

    /**
     * Localiza o arquivo .onnx do modelo principal dentro do diretório.
     * Prioriza o nome padrão "model.onnx"; se não existir, procura automaticamente
     * por qualquer arquivo .onnx no diretório (comum em modelos Piper, cujo arquivo
     * é nomeado como o próprio modelo, ex.: pt_BR-faber-medium.onnx).
     */
    private String findOnnxModelFile(File dir) {
        File preferred = new File(dir, "model.onnx");
        if (preferred.exists()) {
            return preferred.getAbsolutePath();
        }

        File[] onnxFiles = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".onnx"));
        if (onnxFiles == null || onnxFiles.length == 0) {
            throw new RuntimeException("Nenhum arquivo .onnx encontrado no diretório do modelo: " + dir.getAbsolutePath());
        }

        java.util.Arrays.sort(onnxFiles, java.util.Comparator.comparing(File::getName));
        if (onnxFiles.length == 1) {
            return onnxFiles[0].getAbsolutePath();
        }

        // Múltiplos .onnx: evita usar versões quantizadas (int8/quant) quando houver alternativa
        for (File f : onnxFiles) {
            String n = f.getName().toLowerCase();
            if (!n.contains("int8") && !n.contains("quant")) {
                log.warn("Múltiplos arquivos .onnx encontrados em {}, usando: {}", dir.getAbsolutePath(), f.getName());
                return f.getAbsolutePath();
            }
        }

        log.warn("Múltiplos arquivos .onnx encontrados em {} (todos parecem quantizados), usando: {}", dir.getAbsolutePath(), onnxFiles[0].getName());
        return onnxFiles[0].getAbsolutePath();
    }

    private String findFileOptional(File dir, String name) {
        File f = new File(dir, name);
        return f.exists() ? f.getAbsolutePath() : "";
    }

    private String findDir(File dir, String name) {
        File d = new File(dir, name);
        if (!d.exists() || !d.isDirectory()) {
            throw new RuntimeException("Diretório do modelo não existe: " + d.getAbsolutePath());
        }
        return d.getAbsolutePath();
    }

    private String findDirOptional(File dir, String name) {
        File d = new File(dir, name);
        return (d.exists() && d.isDirectory()) ? d.getAbsolutePath() : "";
    }

    /**
     * Busca um arquivo .onnx que corresponda a qualquer um dos padrões
     */
    private String findFileByPattern(File dir, String... patterns) {
        File[] files = dir.listFiles((d, n) -> {
            if (!n.endsWith(".onnx")) return false;
            for (String p : patterns) {
                if (n.contains(p)) return true;
            }
            return false;
        });
        if (files == null || files.length == 0) {
            throw new RuntimeException("Nenhum arquivo .onnx correspondente a " + java.util.Arrays.toString(patterns) + " foi encontrado, diretório: " + dir.getAbsolutePath());
        }
        return files[0].getAbsolutePath();
    }

    /**
     * Busca todos os arquivos lexicon e os concatena com vírgula
     */
    private String findLexicons(File dir) {
        File[] files = dir.listFiles((d, n) -> n.startsWith("lexicon") && n.endsWith(".txt"));
        if (files == null || files.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < files.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(files[i].getAbsolutePath());
        }
        return sb.toString();
    }

    /**
     * Busca todos os arquivos de regras .fst e os concatena com vírgula
     */
    private String findRuleFsts(File dir) {
        File[] files = dir.listFiles((d, n) -> n.endsWith(".fst"));
        if (files == null || files.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < files.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(files[i].getAbsolutePath());
        }
        return sb.toString();
    }

    /**
     * Limpa o cache do caminho de modelo especificado
     */
    public static void clearModelCache(String modelPath) {
        ttsCache.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(modelPath)) {
                try {
                    entry.getValue().release();
                } catch (Exception e) {
                    // ignore
                }
                return true;
            }
            return false;
        });
    }
}
