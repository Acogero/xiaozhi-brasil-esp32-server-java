package com.xiaozhi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ponto de entrada de inicialização independente do Dialogue
 * <p>
 * Inclui: WebSocket/MQTT.
 * Pode escalar horizontalmente, colaborando com outras instâncias via Redis Pub/Sub.
 * <p>
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
@ComponentScan(
    basePackages = {
        // xiaozhi-common
        "com.xiaozhi.common",
        "com.xiaozhi.communication",
        "com.xiaozhi.utils",
        // xiaozhi-service (partes necessárias para o dialogue)
        "com.xiaozhi.config",
        "com.xiaozhi.storage",
        "com.xiaozhi.device",
        "com.xiaozhi.mcptoolexclude",
        "com.xiaozhi.message",
        "com.xiaozhi.monitoring",
        "com.xiaozhi.role",
        "com.xiaozhi.summary",
        "com.xiaozhi.token",
        "com.xiaozhi.task",
        // xiaozhi-ai
        "com.xiaozhi.ai",
        // xiaozhi-dialogue
        "com.xiaozhi.dialogue",
    },
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class)
    }
)
@MapperScan({
    "com.xiaozhi.config.dal.mysql.mapper",
    "com.xiaozhi.device.dal.mysql.mapper",
    "com.xiaozhi.mcptoolexclude.dal.mysql.mapper",
    "com.xiaozhi.message.dal.mysql.mapper",
    "com.xiaozhi.role.dal.mysql.mapper",
    "com.xiaozhi.summary.dal.mysql.mapper",
})
public class DialogueApplication {

    public static void main(String[] args) {
        SpringApplication.run(DialogueApplication.class, args);
    }
}
