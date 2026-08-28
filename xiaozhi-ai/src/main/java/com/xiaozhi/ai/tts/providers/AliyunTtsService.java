package com.xiaozhi.ai.tts.providers;

import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisAudioFormat;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.tts.SpeechSynthesizer;
import com.xiaozhi.ai.tts.TtsService;
import com.xiaozhi.ai.tts.XiaozhiTtsOptions;
import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.utils.AudioUtils;


import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AliyunTtsService implements TtsService {
    private static final String PROVIDER_NAME = "aliyun";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final long TTS_TIMEOUT_SECONDS = 5;

    private static final ExecutorService sharedExecutor = new ThreadPoolExecutor(
            0, 20, 60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            r -> {
                Thread t = new Thread(r, "aliyun-tts-worker");
                t.setDaemon(true);
                return t;
            });

    static {
        // Registra um shutdown hook da JVM, garantindo que o pool de threads seja encerrado corretamente
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            sharedExecutor.shutdown();
            try {
                if (!sharedExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    sharedExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                sharedExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }, "aliyun-tts-shutdown"));
    }
    
    // Tabela de mapeamento de timbres: mapeia o nome do timbre para o enum AudioParameters.Voice
    // Contém todos os timbres do Qwen
    private static final Map<String, AudioParameters.Voice> VOICE_MAP = new HashMap<>();

    static {
        VOICE_MAP.put("Cherry", AudioParameters.Voice.CHERRY);          // Qianyue - Moça alegre, positiva, gentil e natural
        VOICE_MAP.put("Ethan", AudioParameters.Voice.ETHAN);            // Chenxu - Mandarim padrão, caloroso e solar
        VOICE_MAP.put("Nofish", AudioParameters.Voice.NOFISH);          // Buchiyu - Designer que não pronuncia sons retroflexos
        VOICE_MAP.put("Jennifer", AudioParameters.Voice.JENNIFER);      // Jennifer - Voz feminina em inglês americano, nível premium, com textura cinematográfica
        VOICE_MAP.put("Ryan", AudioParameters.Voice.RYAN);              // Tianca - Ritmo intenso, dramaticidade explosiva
        VOICE_MAP.put("Katerina", AudioParameters.Voice.KATERINA);      // Katerina - Timbre de "onee-sama", com cadência marcante
        VOICE_MAP.put("Elias", AudioParameters.Voice.ELIAS);            // Professor Mo - Rigor acadêmico com técnica narrativa
        VOICE_MAP.put("Jada", AudioParameters.Voice.JADA);              // Xangai-Ah Zhen - Senhora enérgica e decidida de Xangai
        VOICE_MAP.put("Dylan", AudioParameters.Voice.DYLAN);            // Pequim-Xiaodong - Jovem criado nos hutongs de Pequim
        VOICE_MAP.put("Sunny", AudioParameters.Voice.SUNNY);            // Sichuan-Qing'er - Moça de Sichuan com um doce que chega ao coração
        VOICE_MAP.put("Li", AudioParameters.Voice.LI);                  // Nanjing-Velho Li - Professor de yoga paciente
        VOICE_MAP.put("Marcus", AudioParameters.Voice.MARCUS);          // Shaanxi-Qinchuan - Rosto largo, fala curta, coração sincero e voz grave
        VOICE_MAP.put("Roy", AudioParameters.Voice.ROY);                // Minnan-Ah Jie - Espirituoso, direto e cheio de vida popular
        VOICE_MAP.put("Peter", AudioParameters.Voice.PETER);            // Tianjin-Li Peter - Xiangsheng de Tianjin, especialista em fazer contracenas
        VOICE_MAP.put("Rocky", AudioParameters.Voice.ROCKY);            // Cantonês-Ah Keung - Ah Keung, bem-humorado e divertido
        VOICE_MAP.put("Kiki", AudioParameters.Voice.KIKI);              // Cantonês-Ah Ching - Melhor amiga doce de Hong Kong
        VOICE_MAP.put("Eric", AudioParameters.Voice.ERIC);              // Sichuan-Chengchuan - Homem de Chengdu, Sichuan, espontâneo e popular
        VOICE_MAP.put("Serena", AudioParameters.Voice.SERENA);              // Su Yao - Moça gentil e doce
        VOICE_MAP.put("Chelsie", AudioParameters.Voice.CHELSIE);            // Qianxue - Namorada virtual estilo anime
        VOICE_MAP.put("Momo", AudioParameters.Voice.MOMO);                  // Motu - Manhosa e brincalhona, sempre te alegrando
        VOICE_MAP.put("Moon", AudioParameters.Voice.MOON);                  // Yuebai - Descontraído e cheio de estilo
        VOICE_MAP.put("Maia", AudioParameters.Voice.MAIA);                  // Siyue - Encontro entre intelectualidade e doçura
        VOICE_MAP.put("Kai", AudioParameters.Voice.KAI);                    // Kai - Um spa para os ouvidos
        VOICE_MAP.put("Bella", AudioParameters.Voice.BELLA);                // Mengbao - Garotinha que bebe sem cambalear
        VOICE_MAP.put("Aiden", AudioParameters.Voice.AIDEN);                // Aiden - Rapaz de inglês americano que manja de culinária
        VOICE_MAP.put("EldricSage", AudioParameters.Voice.ELDRIC_SAGE);     // Cangmingzi - Ancião sereno e sábio
        VOICE_MAP.put("Mia", AudioParameters.Voice.MIA);                    // Guaixiaomei - Dócil como água de primavera, meiga como a primeira neve
        VOICE_MAP.put("Bellona", AudioParameters.Voice.BELLONA);            // Yan Zhengying - Voz forte e dicção clara
        VOICE_MAP.put("Vincent", AudioParameters.Voice.VINCENT);            // Tio Tian - Voz rouca e única de fumante
        VOICE_MAP.put("Bunny", AudioParameters.Voice.BUNNY);                // Mengxiaoji - Voz feminina fofa estilo "moe"
        VOICE_MAP.put("Arthur", AudioParameters.Voice.ARTHUR);              // Vovô Xu - Voz simples marcada pelo tempo e pelo cachimbo
        VOICE_MAP.put("Ebona", AudioParameters.Voice.EBONA);                // Vovó Misteriosa - Sussurro como uma chave enferrujada
        VOICE_MAP.put("Seren", AudioParameters.Voice.SEREN);                // Xiaowan - Voz suave e tranquila
        VOICE_MAP.put("Bodega", AudioParameters.Voice.BODEGA);              // Bodega - Senhor espanhol caloroso
        VOICE_MAP.put("Sonrisa", AudioParameters.Voice.SONRISA);            // Sonrisa - Mulher latina alegre e efusiva
        VOICE_MAP.put("Alek", AudioParameters.Voice.ALEK);                  // Alek - O frio e o calor do "povo guerreiro" (russo)
        VOICE_MAP.put("OnoAnna", AudioParameters.Voice.ONO_ANNA);           // Ono Anna - Amiga de infância travessa e cheia de vida
        VOICE_MAP.put("Lenn", AudioParameters.Voice.LENN);                  // Lenn - Racional por natureza, rebeldia nos detalhes
        VOICE_MAP.put("Emilien", AudioParameters.Voice.EMILIEN);            // Emilien - Rapaz francês romântico
        VOICE_MAP.put("Andre", AudioParameters.Voice.ANDRE);                // Andre - Voz magnética
    }

    // Configuração da Alibaba Cloud
    private final String apiKey;
    private final XiaozhiTtsOptions options;
    private final String outputPath;

    public AliyunTtsService(ConfigBO config,
            String voiceName, Double pitch, Double speed, String outputPath) {
        this.apiKey = config.getApiKey();
        this.options = XiaozhiTtsOptions.builder().voiceName(voiceName).pitch(pitch).speed(speed).build();
        this.outputPath = outputPath;
    }

    /**
     * Interpreta o parâmetro de timbre do Qwen, suportando os formatos:
     * 1. "qwen3-tts-flash-realtime:Cherry" - especifica o modelo e o timbre (separados por dois-pontos)
     * 2. "qwen3-tts-instruct-flash-realtime:Cherry" - especifica o modelo e o timbre (separados por dois-pontos)
     * 3. "qwen-tts-realtime:Cherry" - especifica o modelo e o timbre (separados por dois-pontos)
     * 4. "Cherry" - apenas o timbre, usa por padrão qwen3-tts-flash-realtime
     *
     * @param voiceParam Parâmetro de timbre
     * @return [nome do modelo, nome do timbre]
     */
    private String[] parseQwenVoiceParam(String voiceParam) {
        if (voiceParam == null || voiceParam.isEmpty()) {
            return new String[]{"qwen3-tts-flash-realtime", voiceParam};
        }

        // Verifica se contém o prefixo do modelo (formato separado por dois-pontos)
        if (voiceParam.contains(":")) {
            String[] parts = voiceParam.split(":", 2);
            String model = parts[0];
            String voice = parts.length > 1 ? parts[1] : "";

            // Valida se o nome do modelo é um modelo Qwen válido
            if (model.startsWith("qwen") && model.contains("tts")) {
                return new String[]{model, voice};
            }
            // Se o nome do modelo for inválido, trata a string inteira como nome do timbre
            log.warn("Nome de modelo Qwen inválido: {}, usando o modelo padrão qwen3-tts-flash-realtime", model);
            return new String[]{"qwen3-tts-flash-realtime", voiceParam};
        }

        // Sem prefixo de modelo, usa o modelo padrão
        return new String[]{"qwen3-tts-flash-realtime", voiceParam};
    }

    /**
     * Interpreta o parâmetro de timbre, suportando os formatos:
     * 1. "cosyvoice-v3-plus-voiceclone-xxx" - formato retornado pela clonagem de voz, com identificação automática do prefixo do modelo
     * 2. "cosyvoice-v3-flash-voiceclone-xxx" - formato retornado pela clonagem de voz, com identificação automática do prefixo do modelo
     * 3. "cosyvoice-v2-voiceclone-xxx" - formato retornado pela clonagem de voz, com identificação automática do prefixo do modelo
     * 4. "cosyvoice-v2:longanyang" - especifica o modelo e o timbre (separados por dois-pontos)
     * 5. "cosyvoice-v3-flash:longanyang" - especifica o modelo e o timbre (separados por dois-pontos)
     * 6. "cosyvoice-v3-plus:longanyang" - especifica o modelo e o timbre (separados por dois-pontos)
     * 7. "longanyang" - apenas o timbre, usa por padrão cosyvoice-v2
     *
     * @param voiceParam Parâmetro de timbre
     * @return [nome do modelo, nome do timbre]
     */
    private String[] parseCosyVoiceParam(String voiceParam) {
        if (voiceParam == null || voiceParam.isEmpty()) {
            return new String[]{"cosyvoice-v2", voiceParam};
        }

        // Verifica se é o formato retornado pela clonagem de voz (ex.: cosyvoice-v3-plus-voiceclone-xxx)
        if (voiceParam.startsWith("cosyvoice-v3-plus-")) {
            return new String[]{"cosyvoice-v3-plus", voiceParam};
        } else if (voiceParam.startsWith("cosyvoice-v3-flash-")) {
            return new String[]{"cosyvoice-v3-flash", voiceParam};
        } else if (voiceParam.startsWith("cosyvoice-v2-")) {
            return new String[]{"cosyvoice-v2", voiceParam};
        }

        // Verifica se contém o prefixo do modelo (formato separado por dois-pontos, ex.: cosyvoice-v3-plus:longanyang)
        if (voiceParam.contains(":")) {
            String[] parts = voiceParam.split(":", 2);
            String model = parts[0];
            String voice = parts.length > 1 ? parts[1] : "";

            // Valida se o nome do modelo é um modelo CosyVoice válido
            if ("cosyvoice-v2".equals(model) || "cosyvoice-v3-flash".equals(model) || "cosyvoice-v3-plus".equals(model)) {
                return new String[]{model, voice};
            }
            // Se o nome do modelo for inválido, trata a string inteira como nome do timbre
            log.warn("Nome de modelo CosyVoice inválido: {}, usando o modelo padrão cosyvoice-v2", model);
            return new String[]{"cosyvoice-v2", voiceParam};
        }

        // Sem prefixo de modelo, usa o modelo padrão
        return new String[]{"cosyvoice-v2", voiceParam};
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
            if (getVoiceName().contains("sambert")) {
                return ttsSambert(text);
            } else {
                // Interpreta o parâmetro de timbre do Qwen
                String[] parsed = parseQwenVoiceParam(getVoiceName());
                String actualVoiceName = parsed[1];

                if (VOICE_MAP.get(actualVoiceName) != null) {
                    return ttsQwen(text);
                } else {
                    return ttsCosyvoice(text);
                }
            }
        } catch (Exception e) {
            log.error("Síntese de voz aliyun - falha na síntese usando o modelo {}: ", getVoiceName(), e);
            throw new Exception("Falha na síntese de voz");
        }
    }

    private Path ttsQwen(String text) {
        int attempts = 0;
        // Interpreta o parâmetro de timbre
        String[] parsed = parseQwenVoiceParam(getVoiceName());
        String actualVoiceName = parsed[1];

        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                AudioParameters.Voice voice = VOICE_MAP.get(actualVoiceName);
                MultiModalConversationParam param = MultiModalConversationParam.builder()
                        // O SDK não realtime (MultiModalConversation) suporta apenas qwen3-tts-flash; não é possível usar nomes de modelo realtime
                        .model("qwen3-tts-flash")
                        .apiKey(apiKey)
                        .text(text)
                        .voice(voice)
                        .build();
                
                // Usa o pool de threads compartilhado em vez de criar um novo a cada vez
                Future<MultiModalConversationResult> future = sharedExecutor.submit(() -> {
                    MultiModalConversation conv = new MultiModalConversation();
                    return conv.call(param);
                });
                
                // Aguarda o resultado, com timeout definido
                MultiModalConversationResult result;
                try {
                    result = future.get(TTS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    log.warn("Síntese de voz aliyun - timeout usando o modelo {}, tentando novamente ({}/{})", getVoiceName(), attempts + 1, MAX_RETRY_ATTEMPTS);
                    attempts++;
                    if (attempts >= MAX_RETRY_ATTEMPTS) {
                        log.error("Síntese de voz aliyun - timeout repetido usando o modelo {}, desistindo das tentativas", getVoiceName());
                        return null;
                    }
                    // Aguarda um tempo antes de tentar novamente
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    continue;
                }

                // Verifica se o resultado é válido
                if (result == null || result.getOutput() == null ||
                    result.getOutput().getAudio() == null ||
                    result.getOutput().getAudio().getUrl() == null) {

                    log.warn("Síntese de voz aliyun - resultado inválido usando o modelo {}, tentando novamente ({}/{})", getVoiceName(), attempts + 1, MAX_RETRY_ATTEMPTS);
                    attempts++;
                    if (attempts >= MAX_RETRY_ATTEMPTS) {
                        log.error("Síntese de voz aliyun - resultado inválido repetido usando o modelo {}, desistindo das tentativas", getVoiceName());
                        return null;
                    }
                    // Aguarda um tempo antes de tentar novamente
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    continue;
                }

                String audioUrl = result.getOutput().getAudio().getUrl();
                Path outPath = Path.of(outputPath, getAudioFileName());

                // Baixa o WAV (24kHz), reamostra para 16kHz e salva
                Future<Boolean> downloadFuture = sharedExecutor.submit(() -> {
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                         InputStream in = URI.create(audioUrl).toURL().openStream()) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            baos.write(buffer, 0, bytesRead);
                        }
                        byte[] pcm24k = AudioUtils.wavToPcm(baos.toByteArray());
                        byte[] pcm16k = AudioUtils.resamplePcm(pcm24k, 24000, 16000);
                        AudioUtils.saveAsWav(outPath, pcm16k);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });

                try {
                    Boolean downloadSuccess = downloadFuture.get(TTS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    if (!downloadSuccess) {
                        throw new IOException("Falha ao baixar o arquivo de áudio");
                    }
                } catch (TimeoutException e) {
                    downloadFuture.cancel(true);
                    log.warn("Síntese de voz aliyun - timeout ao baixar áudio usando o modelo {}, tentando novamente ({}/{})", getVoiceName(), attempts + 1, MAX_RETRY_ATTEMPTS);
                    attempts++;
                    if (attempts >= MAX_RETRY_ATTEMPTS) {
                        log.error("Síntese de voz aliyun - timeout de download repetido usando o modelo {}, desistindo das tentativas", getVoiceName());
                        return null;
                    }
                    // Aguarda um tempo antes de tentar novamente
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    continue;
                }

                return outPath;
            } catch (Exception e) {
                attempts++;
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    log.warn("Síntese de voz aliyun - falha usando o modelo {}, tentando novamente ({}/{}): {}", getVoiceName(), attempts, MAX_RETRY_ATTEMPTS, e.getMessage());
                    try {
                        // Aguarda um tempo antes de tentar novamente
                        TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Espera de retry interrompida", ie);
                        return null;
                    }
                } else {
                    log.error("Síntese de voz aliyun - falha usando o modelo {}, número máximo de tentativas atingido: ", getVoiceName(), e);
                    return null;
                }
            }
        }
        return null;
    }

    // O CosyVoice tem concorrência padrão de apenas 3, portanto é necessário um mecanismo de retry adicional
    private Path ttsCosyvoice(String text) {
        int attempts = 0;
        // Interpreta o parâmetro de timbre, obtendo o nome do modelo e do timbre
        String[] parsed = parseCosyVoiceParam(getVoiceName());
        String modelName = parsed[0];
        String actualVoiceName = parsed[1];
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam param =
                com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam.builder()
                                .apiKey(apiKey)
                                .model(modelName)  // Usa o nome do modelo interpretado
                                .voice(actualVoiceName)  // Usa o nome do timbre interpretado
                                .speechRate(getSpeed().floatValue())
                                .pitchRate(getPitch().floatValue())
                                .format(com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisAudioFormat.WAV_16000HZ_MONO_16BIT)
                                .build();

                // Usa o pool de threads compartilhado
                Future<ByteBuffer> future = sharedExecutor.submit(() -> {
                    com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer synthesizer =
                        new com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer(param, null);
                    try {
                        return synthesizer.call(text);
                    } finally {
                        // Fecha proativamente a conexão WebSocket, evitando que conexões zumbis lotem o pool de conexões
                        try {
                            synthesizer.getDuplexApi().close(1000, "completed");
                        } catch (Exception e) {
                            log.debug("Erro ao fechar a conexão TTS do CosyVoice", e);
                        }
                    }
                });

                // Aguarda o resultado, com timeout definido
                ByteBuffer audio;
                try {
                    audio = future.get(TTS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    log.warn("Síntese de voz aliyun - timeout usando o modelo {}, tentando novamente ({}/{}) - timbre: {}", modelName, attempts + 1, MAX_RETRY_ATTEMPTS, actualVoiceName);
                    attempts++;
                    if (attempts >= MAX_RETRY_ATTEMPTS) {
                        log.error("Síntese de voz aliyun - timeout repetido usando o modelo {}, desistindo das tentativas - timbre: {}", modelName, actualVoiceName);
                        return null;
                    }
                    // Aguarda um tempo antes de tentar novamente
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    continue;
                }

                // Verifica se o ByteBuffer retornado é null
                if (audio == null) {
                    attempts++;
                    if (attempts < MAX_RETRY_ATTEMPTS) {
                        log.warn("Síntese de voz aliyun - retorno null usando o modelo {}, tentando novamente ({}/{}) - timbre: {}", modelName, attempts, MAX_RETRY_ATTEMPTS, actualVoiceName);
                        // Aguarda um tempo antes de tentar novamente
                        TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                        continue;
                    } else {
                        log.error("Síntese de voz aliyun - retorno null repetido usando o modelo {}, desistindo das tentativas - timbre: {}", modelName, actualVoiceName);
                        return null;
                    }
                }

                Path outPath = Path.of(outputPath, getAudioFileName());
                try (FileOutputStream fos = new FileOutputStream(outPath.toFile())) {
                    fos.write(audio.array());
                } catch (IOException e) {
                    log.error("Síntese de voz aliyun - falha na síntese usando o modelo {} - timbre: {}", modelName, actualVoiceName, e);
                    return null;
                }
                return outPath;
            } catch (Exception e) {
                attempts++;
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    log.warn("Síntese de voz aliyun - falha usando o modelo {}, tentando novamente ({}/{}) - timbre: {}: {}", modelName, attempts, MAX_RETRY_ATTEMPTS, actualVoiceName, e.getMessage());
                    try {
                        // Aguarda um tempo antes de tentar novamente
                        TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Espera de retry interrompida - modelo: {}", modelName, ie);
                        return null;
                    }
                } else {
                    log.error("Síntese de voz aliyun - falha na síntese usando o modelo {}, número máximo de tentativas atingido - timbre: {}", modelName, actualVoiceName, e);
                    return null;
                }
            }
        }
        return null;
    }

    public Path ttsSambert(String text) {
        int attempts = 0;
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                        .apiKey(apiKey)
                        .model(getVoiceName())
                        .text(text)
                        .rate(getSpeed().floatValue())
                        .pitch(getPitch().floatValue())
                        .sampleRate(AudioUtils.SAMPLE_RATE)
                        .format(SpeechSynthesisAudioFormat.WAV)
                        .build();
                
                // Usa o pool de threads compartilhado
                Future<ByteBuffer> future = sharedExecutor.submit(() -> {
                    SpeechSynthesizer synthesizer = new SpeechSynthesizer();
                    return synthesizer.call(param);
                });
                
                // Aguarda o resultado, com timeout definido
                ByteBuffer audio;
                try {
                    audio = future.get(TTS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    log.warn("Síntese de voz aliyun - timeout usando o modelo {}, tentando novamente ({}/{}), texto: {}", getVoiceName(), attempts + 1, MAX_RETRY_ATTEMPTS, text);
                    attempts++;
                    if (attempts >= MAX_RETRY_ATTEMPTS) {
                        log.error("Síntese de voz aliyun - timeout repetido usando o modelo {}, desistindo das tentativas, texto: {}", getVoiceName(), text);
                        return null;
                    }
                    // Aguarda um tempo antes de tentar novamente
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    continue;
                }
                
                // Verifica se o ByteBuffer retornado é null
                if (audio == null) {
                    attempts++;
                    if (attempts < MAX_RETRY_ATTEMPTS) {
                        log.warn("Síntese de voz aliyun - retorno null usando o modelo {}, tentando novamente ({}/{})", getVoiceName(), attempts, MAX_RETRY_ATTEMPTS);
                        // Aguarda um tempo antes de tentar novamente
                        TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                        continue;
                    } else {
                        log.error("Síntese de voz aliyun - retorno null repetido usando o modelo {}, desistindo das tentativas", getVoiceName());
                        return null;
                    }
                }
                
                Path outPath = Path.of(outputPath, getAudioFileName());
                try (FileOutputStream fos = new FileOutputStream(outPath.toFile())) {
                    fos.write(audio.array());
                } catch (IOException e) {
                    log.error("Síntese de voz aliyun - falha usando o modelo {}: ", getVoiceName(), e);
                    return null;
                }
                return outPath;
            } catch (Exception e) {
                attempts++;
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    log.warn("Síntese de voz aliyun - falha usando o modelo {}, tentando novamente ({}/{}): {}", getVoiceName(), attempts, MAX_RETRY_ATTEMPTS, e.getMessage());
                    try {
                        // Aguarda um tempo antes de tentar novamente
                        TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Espera de retry interrompida", ie);
                        return null;
                    }
                } else {
                    log.error("Síntese de voz aliyun - falha usando o modelo {}, número máximo de tentativas atingido: ", getVoiceName(), e);
                    return null;
                }
            }
        }
        return null;
    }
}