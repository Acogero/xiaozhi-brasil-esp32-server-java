package com.xiaozhi.ai.stt;

import com.xiaozhi.common.config.RuntimePathConfig;
import com.xiaozhi.ai.stt.SttService;
import com.xiaozhi.ai.stt.providers.*;
import com.xiaozhi.common.port.TokenResolver;
import com.xiaozhi.common.model.bo.ConfigBO;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SttServiceFactory {

    @Resource
    private TokenResolver tokenResolver;

    @Resource
    private RuntimePathConfig runtimePathConfig;

    // Cache dos serviços já inicializados: key format: "provider:configId"
    private final Map<String, SttService> serviceCache = new ConcurrentHashMap<>();

    // Nome do provedor de serviço padrão
    private static final String DEFAULT_PROVIDER = "vosk";

    // Marca se o Vosk foi inicializado com sucesso
    private boolean voskInitialized = false;

    // Provedor padrão alternativo (usado quando a inicialização do Vosk falha)
    private String fallbackProvider = null;

    /**
     * Inicializa automaticamente o serviço Vosk na inicialização da aplicação
     */
    @PostConstruct
    public void initializeDefaultSttService() {
        log.info("Inicializando o serviço de reconhecimento de voz padrão (Vosk)...");
        initializeVosk();
        if (voskInitialized) {
            log.info("Serviço de reconhecimento de voz padrão (Vosk) inicializado com sucesso; pronto para uso");
        } else {
            log.warn("Falha ao inicializar o serviço de reconhecimento de voz padrão (Vosk); um serviço alternativo será tentado quando necessário");
        }
    }

    /**
     * Inicializa o serviço Vosk
     */
    private synchronized SttService initializeVosk() {
        if (serviceCache.containsKey(DEFAULT_PROVIDER)) {
            return serviceCache.get(DEFAULT_PROVIDER);
        }

        try {
            var voskService = new VoskSttService(
                    runtimePathConfig.resolveNativeLibDir().toString(),
                    runtimePathConfig.resolveVoskModelDir().toString()
            );
            voskService.initialize();
            
            // Verifica se o modelo foi realmente carregado com sucesso
            if (voskService instanceof VoskSttService && !((VoskSttService)voskService).isModelLoaded()) {
                throw new Exception("Vosk model was not properly loaded");
            }
            
            serviceCache.put(DEFAULT_PROVIDER, voskService);
            voskInitialized = true;
            log.info("Serviço STT do Vosk inicializado com sucesso");
            return voskService;
        } catch (Throwable e) {
            voskInitialized = false;
            log.warn("Falha ao inicializar o serviço STT do Vosk: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Obtém o serviço STT padrão
     */
    public SttService getDefaultSttService() {
        return getSttService(null);
    }

    /**
     * Obtém o serviço STT com base na configuração
     */
    public SttService getSttService(ConfigBO config) {
        if (config == null) {
            config = new ConfigBO().setProvider(DEFAULT_PROVIDER).setConfigId(-1);
        }

        // Para serviços de API, usa "provider:configId" como chave de cache, garantindo que cada configuração use uma instância de serviço independente
        var cacheKey = config.getProvider() + ":" + config.getConfigId();

        // Verifica se já existe uma instância de serviço para esta configuração
        if (serviceCache.containsKey(cacheKey)) {
            return serviceCache.get(cacheKey);
        }

        // Cria uma nova instância de serviço de API
        var service = createApiService(config);
        serviceCache.put(cacheKey, service);

        // Se não houver serviço padrão alternativo, define este serviço como alternativo
        if (fallbackProvider == null) {
            fallbackProvider = cacheKey;
        }

        return service;
    }

    /**
     * Cria um serviço STT do tipo API com base na configuração
     */
    private SttService createApiService(@Nonnull ConfigBO config) {
        return switch (config.getProvider()) {
            case "tencent" -> new TencentSttService(config);
            case "aliyun" -> new AliyunSttService(config);
            case "aliyun-nls" -> {
                // Cria o serviço de Token da Alibaba Cloud para o NLS
                yield new AliyunNlsSttService(config, tokenResolver);
            }
            case "funasr" -> new FunASRSttService(config);
            case "xfyun" -> new XfyunSttService(config);
            case "volcengine" -> new VolcengineSttService(config);
            default -> {
                var service = initializeVosk();
                if (service == null) {
                    // If vosk create failed, return fallback stt service
                    if (fallbackProvider != null && serviceCache.containsKey(fallbackProvider)) {
                        yield serviceCache.get(fallbackProvider);
                    }
                    throw new RuntimeException("Create vosk service failed");
                }
                yield service;
            }
        };
    }

    public void removeCache(ConfigBO config) {
        // Para serviços de API, usa "provider:configId" como chave de cache, garantindo que cada configuração use uma instância de serviço independente
        Integer configId = config.getConfigId();
        String provider = config.getProvider();
        String cacheKey = provider + ":" + (configId != null ? configId : "default");

        if ("aliyun-nls".equals(provider)) {
            AliyunNlsSttService.clearClientCache(configId);
        }

        serviceCache.remove(cacheKey);
    }
}
