package com.xiaozhi.ai.tts;

import com.xiaozhi.common.config.RuntimePathConfig;
import com.xiaozhi.common.port.TokenResolver;
import com.xiaozhi.utils.AudioUtils;
import com.xiaozhi.ai.tts.providers.*;
import com.xiaozhi.common.model.bo.ConfigBO;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TtsServiceFactory {

    // Cache dos serviços já inicializados: a chave segue o formato "provider:configId:voiceName", garantindo que uma nova instância seja criada quando o timbre mudar
    private final Map<String, TtsService> serviceCache = new ConcurrentHashMap<>();

    @Resource
    private TokenResolver tokenResolver;

    @Resource
    private RuntimePathConfig runtimePathConfig;

    // Nome do provedor de serviço padrão
    private static final String DEFAULT_PROVIDER = "edge";

    // Nome de voz padrão do serviço EDGE TTS padrão
    private static final String DEFAULT_VOICE = "zh-CN-XiaoyiNeural";

    /**
     * Obtém o serviço TTS padrão
     */
    public TtsService getDefaultTtsService() {
        var config = new ConfigBO().setProvider(DEFAULT_PROVIDER);
        return getTtsService(config, TtsServiceFactory.DEFAULT_VOICE, 1.0, 1.0);
    }

    // Cria a chave de cache (incluindo pitch e speed)
    private String createCacheKey(ConfigBO config, String provider, String voiceName, Double pitch, Double speed) {
        Integer configId = -1;
        if (config != null && config.getConfigId() != null) {
            configId = config.getConfigId();
        }
        return provider + ":" + configId + ":" + voiceName + ":" + pitch + ":" + speed;
    }

    /**
     * Obtém o serviço TTS com base na configuração (com os parâmetros pitch e speed)
     */
    public TtsService getTtsService(ConfigBO config, String voiceName, Double pitch, Double speed) {
        final ConfigBO finalConfig = !ObjectUtils.isEmpty(config) ? config : new ConfigBO().setProvider(DEFAULT_PROVIDER);
        String provider = finalConfig.getProvider();
        String cacheKey = createCacheKey(finalConfig, provider, voiceName, pitch, speed);

        // Usa computeIfAbsent para garantir uma operação atômica, evitando a criação concorrente de múltiplas instâncias
        return serviceCache.computeIfAbsent(cacheKey, k -> createApiService(finalConfig, voiceName, pitch, speed));
    }

    /**
     * Cria um serviço TTS do tipo API com base na configuração (com os parâmetros pitch e speed)
     */
    private TtsService createApiService(ConfigBO config, String voiceName, Double pitch, Double speed) {
        // Make sure output dir exists
        String outputPath = AudioUtils.AUDIO_PATH;
        ensureOutputPath(outputPath);

        return switch (config.getProvider()) {
            case "aliyun" -> new AliyunTtsService(config, voiceName, pitch, speed, outputPath);
            case "aliyun-nls" -> {
                yield new AliyunNlsTtsService(config, voiceName, pitch, speed, outputPath, tokenResolver);
            }
            case "volcengine" -> new VolcengineTtsService(config, voiceName, pitch, speed, outputPath);
            case "xfyun" -> new XfyunTtsService(config, voiceName, pitch, speed, outputPath);
            case "minimax" -> new MiniMaxTtsService(config, voiceName, pitch, speed, outputPath);
            case "tencent" -> new TencentTtsService(config, voiceName, pitch, speed, outputPath);
            case "sherpa-onnx" -> new SherpaOnnxTtsService(
                    config,
                    voiceName,
                    pitch,
                    speed,
                    outputPath,
                    runtimePathConfig.resolveTtsModelsDir().toString()
            );
            default -> new EdgeTtsService(voiceName, pitch, speed, outputPath);
        };
    }

    private void ensureOutputPath(String outputPath) {
        File dir = new File(outputPath);
        if (!dir.exists()) dir.mkdirs();
    }

    public void removeCache(ConfigBO config) {
        if (config == null) {
            return;
        }

        String provider = config.getProvider();
        Integer configId = config.getConfigId();

        // Se for o Alibaba Cloud NLS, é necessário limpar também o cache do NlsClient
        if ("aliyun-nls".equals(provider)) {
            AliyunNlsTtsService.clearClientCache(configId);
        }

        // Se for o sherpa-onnx, é necessário limpar também o cache do modelo
        if ("sherpa-onnx".equals(provider) && config.getApiUrl() != null) {
            SherpaOnnxTtsService.clearModelCache(config.getApiUrl());
        }

        // Percorre todas as chaves do cache, encontra a chave correspondente e a remove
        serviceCache.keySet().removeIf(key -> {
            String[] parts = key.split(":");
            if (parts.length < 5) {  // O novo formato é provider:configId:voiceName:pitch:speed
                return false;
            }
            String keyProvider = parts[0];
            String keyConfigId = parts[1];

            // Verifica se o provider e o configId correspondem
            return keyProvider.equals(provider) && keyConfigId.equals(String.valueOf(configId));
        });

    }
}
