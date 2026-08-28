package com.xiaozhi.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Configuração de cache Redis
 * <p>
 * Estratégia anti-avalanche: o TTL de cada nome de cache = duração base + desvio aleatório (10% da duração base, no máximo 1 hora).
 * O valor aleatório é gerado de forma independente na inicialização de cada instância JVM; em implantações com múltiplas instâncias, o TTL de chaves do mesmo tipo fica naturalmente escalonado.
 *
 * @author Joey
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /** Limite máximo do desvio aleatório (segundos) */
    private static final int MAX_JITTER_SECONDS = 3600;

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        GenericJackson2JsonRedisSerializer serializer = createSerializer();

        // Configuração padrão: 1 dia + desvio aleatório
        RedisCacheConfiguration defaultConfig = buildConfig(serializer, Duration.ofDays(1));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("XiaoZhi:Device",        buildConfig(serializer, Duration.ofDays(1)));
        cacheConfigurations.put("XiaoZhi:Permission",    buildConfig(serializer, Duration.ofDays(7)));
        cacheConfigurations.put("XiaoZhi:User",          buildConfig(serializer, Duration.ofDays(1)));
        cacheConfigurations.put("XiaoZhi:SysConfig",     buildConfig(serializer, Duration.ofDays(7)));
        cacheConfigurations.put("XiaoZhi:McpToolExclude",buildConfig(serializer, Duration.ofDays(7)));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()
            .build();
    }

    /**
     * Constrói a configuração de cache, TTL = baseTtl + desvio aleatório.
     * Desvio = número aleatório de segundos dentro do intervalo min(10% de baseTtl, MAX_JITTER_SECONDS).
     */
    private RedisCacheConfiguration buildConfig(GenericJackson2JsonRedisSerializer serializer, Duration baseTtl) {
        long jitterBound = Math.min(baseTtl.toSeconds() / 10, MAX_JITTER_SECONDS);
        long jitterSeconds = jitterBound > 0 ? ThreadLocalRandom.current().nextLong(jitterBound) : 0;

        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(baseTtl.plusSeconds(jitterSeconds))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            .disableCachingNullValues();
    }

    private GenericJackson2JsonRedisSerializer createSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
