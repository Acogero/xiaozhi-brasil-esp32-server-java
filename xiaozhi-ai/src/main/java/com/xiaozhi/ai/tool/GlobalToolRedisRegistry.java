package com.xiaozhi.ai.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xiaozhi.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Registro Redis dos metadados de ferramentas globais do sistema.
 * <p>
 * Resolve o problema de visibilidade entre processos: o Bean de {@link ToolsGlobalRegistry.GlobalFunction} só é registrado no processo dialogue,
 * enquanto o processo server precisa exibir essas ferramentas na interface de "excluir ferramentas" do frontend. Este registro é gravado no Redis pelo dialogue na inicialização,
 * e lido pelo processo server ao consultar.
 * <p>
 * Redis Key: {@value #REDIS_KEY}
 * Value: array JSON {@code [{"name":"...","description":"..."}]}
 */
@Slf4j
@Component
public class GlobalToolRedisRegistry {

    private static final String REDIS_KEY = "xiaozhi:system-global-tools";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Publica no Redis os metadados de GlobalFunction visíveis no processo atual, para leitura por outros processos.
     */
    public void publish(List<ToolSummary> tools) {
        if (tools == null || tools.isEmpty()) {
            return;
        }
        try {
            String json = JsonUtil.toJson(tools);
            stringRedisTemplate.opsForValue().set(REDIS_KEY, json);
            log.info("Metadados de ferramentas globais do sistema publicados no Redis, quantidade: {}", tools.size());
        } catch (Exception e) {
            log.warn("Falha ao publicar os metadados de ferramentas globais do sistema no Redis: {}", e.getMessage());
        }
    }

    /**
     * Lê todos os metadados de ferramentas globais do sistema a partir do Redis; retorna uma lista vazia se não existirem.
     */
    public List<ToolSummary> getAll() {
        try {
            String json = stringRedisTemplate.opsForValue().get(REDIS_KEY);
            if (json == null || json.isEmpty()) {
                return List.of();
            }
            List<ToolSummary> list = JsonUtil.fromJson(json, new TypeReference<List<ToolSummary>>() {});
            return list == null ? List.of() : list;
        } catch (Exception e) {
            log.warn("Falha ao ler os metadados de ferramentas globais do sistema a partir do Redis: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Resumo da ferramenta: contém apenas o nome e a descrição necessários para a interface de "excluir ferramentas" do frontend.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ToolSummary {
        private String name;
        private String description;
    }
}
