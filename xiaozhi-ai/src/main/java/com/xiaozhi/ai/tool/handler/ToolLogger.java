package com.xiaozhi.ai.tool.handler;

import com.xiaozhi.event.ToolCallCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
/**
 * Handler de log dos eventos de chamada de ferramenta.
 * Escuta o ToolCallCompletedEvent, registrando o nome da ferramenta, o tempo decorrido e o status de sucesso/falha da chamada.
 */
@Slf4j
@Component
public class ToolLogger {

    @EventListener
    public void onToolCallCompletedEvent(ToolCallCompletedEvent event) {
        if (event.isSuccess()) {
            log.info("Chamada de ferramenta bem-sucedida - session: {}, tool: {}, tempo decorrido: {}ms",
                    event.getSessionId(), event.getToolName(), event.getDurationMs());
        } else {
            log.warn("Falha na chamada de ferramenta - session: {}, tool: {}, tempo decorrido: {}ms, resultado: {}",
                    event.getSessionId(), event.getToolName(), event.getDurationMs(), event.getResult());
        }
    }
}
