package com.xiaozhi.storage.service;

import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.config.service.ConfigService;
import com.xiaozhi.storage.service.impl.AliyunOssStorageService;
import com.xiaozhi.storage.service.impl.LocalStorageService;
import com.xiaozhi.storage.service.impl.TencentCosStorageService;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
/**
 * Fábrica de serviços de armazenamento.
 * Lê a configuração padrão de OSS a partir de sys_config (configType="oss") e cria a implementação correspondente por provider.
 * Se não houver configuração ou ela for inválida, usa fallback para armazenamento local.
 * <p>
 * O cliente da nuvem é armazenado em cache e reutilizado (os clientes SDK do COS/OSS são thread-safe); reconstruído automaticamente quando a configuração do provider muda.
 */
@Slf4j
@Component
public class StorageServiceFactory {

    @Resource
    private ConfigService configService;

    @Resource
    private LocalStorageService localStorageService;

    private volatile StorageService cachedCloudService;
    private volatile String cachedProvider;

    /**
     * Obtém o serviço de armazenamento atualmente ativo
     */
    public StorageService getStorageService() {
        try {
            ConfigBO ossConfig = getDefaultOssConfig();

            if (ossConfig == null || "local".equals(ossConfig.getProvider())) {
                return localStorageService;
            }

            String provider = ossConfig.getProvider();
            if (provider.equals(cachedProvider) && cachedCloudService != null) {
                return cachedCloudService;
            }

            synchronized (this) {
                if (provider.equals(cachedProvider) && cachedCloudService != null) {
                    return cachedCloudService;
                }
                shutdownCached();
                cachedCloudService = createStorageService(ossConfig);
                cachedProvider = provider;
                log.info("Serviço de armazenamento alterado para: {}", provider);
                return cachedCloudService;
            }
        } catch (Exception e) {
            log.warn("Falha ao obter configuração do OSS, usando armazenamento local: {}", e.getMessage());
            return localStorageService;
        }
    }

    /**
     * Cria o serviço de armazenamento correspondente com base na configuração
     */
    public StorageService createStorageService(ConfigBO config) {
        return switch (config.getProvider()) {
            case "tencent" -> new TencentCosStorageService(config);
            case "aliyun" -> new AliyunOssStorageService(config);
            default -> {
                log.warn("Provider de armazenamento desconhecido: {}, usando armazenamento local", config.getProvider());
                yield localStorageService;
            }
        };
    }

    private ConfigBO getDefaultOssConfig() {
        return configService.getDefaultBO("oss");
    }

    private void shutdownCached() {
        if (cachedCloudService instanceof TencentCosStorageService cos) {
            cos.shutdown();
        } else if (cachedCloudService instanceof AliyunOssStorageService oss) {
            oss.shutdown();
        }
    }

    @PreDestroy
    public void destroy() {
        shutdownCached();
    }
}
