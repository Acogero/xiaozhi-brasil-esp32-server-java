package com.xiaozhi.common;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
/**
 * Classe auxiliar de cache
 * Fornece consulta de cache com lock distribuído, evitando o esgotamento de cache (cache breakdown)
 *
 * @author Joey
 */
@Slf4j
@Component
public class CacheHelper {

    @Resource
    private RedissonClient redissonClient;

    /**
     * Consulta de cache com lock distribuído
     * Evita o esgotamento de cache - quando o cache expira, apenas uma requisição consulta o banco de dados
     *
     * @param lockKey chave do lock
     * @param cacheGetter função que obtém os dados do cache
     * @param dbGetter função que obtém os dados do banco de dados
     * @param <T> tipo de dado
     * @return dados
     */
    public <T> T getWithLock(String lockKey, Supplier<T> cacheGetter, Supplier<T> dbGetter) {
        // 1. Tenta obter do cache primeiro
        T cached = cacheGetter.get();
        if (cached != null) {
            return cached;
        }

        // 2. Cache não encontrado, usa lock distribuído
        RLock lock = redissonClient.getLock("lock:" + lockKey);

        try {
            // Tenta obter o lock, aguardando no máximo 3 segundos; o lock é liberado automaticamente após 10 segundos
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                try {
                    // 3. Verificação dupla, evita consultar o banco de dados novamente
                    cached = cacheGetter.get();
                    if (cached != null) {
                        log.debug("Cache encontrado após obter o lock: {}", lockKey);
                        return cached;
                    }

                    // 4. Consulta o banco de dados
                    log.debug("Consultando o banco de dados: {}", lockKey);
                    T result = dbGetter.get();

                    // 5. O resultado será gravado no cache automaticamente via @Cacheable
                    return result;

                } finally {
                    lock.unlock();
                }
            } else {
                // Falha ao obter o lock, consulta o banco de dados diretamente (estratégia de fallback)
                log.warn("Timeout ao obter o lock, consultando o banco de dados diretamente: {}", lockKey);
                return dbGetter.get();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Obtenção do lock interrompida: {}", lockKey, e);
            // Fallback: consulta o banco de dados diretamente
            return dbGetter.get();
        } catch (Exception e) {
            log.error("Exceção no lock distribuído: {}", lockKey, e);
            // Fallback: consulta o banco de dados diretamente
            return dbGetter.get();
        }
    }

    /**
     * Versão simplificada - operação com lock distribuído
     *
     * @param lockKey chave do lock
     * @param supplier operação a ser executada
     * @param <T> tipo de retorno
     * @return resultado da operação
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock("lock:" + lockKey);

        try {
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                try {
                    return supplier.get();
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("Timeout ao obter o lock: {}", lockKey);
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Obtenção do lock interrompida: {}", lockKey, e);
            return null;
        } catch (Exception e) {
            log.error("Exceção ao executar operação com lock: {}", lockKey, e);
            return null;
        }
    }
}
