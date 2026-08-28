package com.xiaozhi.ai.tool.observation;

import com.xiaozhi.ai.tool.session.ToolSession;
import com.xiaozhi.ai.tool.session.ToolSessionProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;

/**
 * Handler de observação de chamadas de ferramenta, usado para capturar informações antes e depois da chamada.
 * Inclui nome da função, parâmetros, callId, nome da ferramenta, parâmetros e resultado da execução.
 */
@Slf4j
@Component
public class ToolCallingObservationHandler implements ObservationHandler<ToolCallingObservationContext> {

    private static final String SESSION_ID_KEY = "sessionId";

    @Autowired(required = false)
    private ToolSessionProvider toolSessionProvider;

    @Override
    public void onStart(ToolCallingObservationContext context) {
        ToolDefinition toolDefinition = context.getToolDefinition();
        String toolName = toolDefinition.name();
        log.info("ToolCalling#onStart: {}", toolName);

        if (!context.containsKey(SESSION_ID_KEY)) {
            return;
        }

        // Obtém o sessionId
        Object sessionIdObj = context.get(SESSION_ID_KEY);
        if (!(sessionIdObj instanceof String sessionId)) {
            return;
        }

        // Obtém o ToolSession a partir do ToolSessionProvider
        if (toolSessionProvider == null) {
            return;
        }
        ToolSession session = toolSessionProvider.getSession(sessionId);
        if (session == null) {
            return;
        }

        // Marca o início da chamada de ferramenta, evitando que o player chame sendStop antecipadamente
        session.setToolCalling(true);
    }

    @Override
    public void onStop(ToolCallingObservationContext context) {
        if (!context.containsKey(SESSION_ID_KEY)) {
            return;
        }

        Object sessionIdObj = context.get(SESSION_ID_KEY);
        if (!(sessionIdObj instanceof String sessionId)) {
            return;
        }

        if (toolSessionProvider == null) {
            return;
        }
        ToolSession session = toolSessionProvider.getSession(sessionId);
        if (session == null) {
            return;
        }

        // Limpa o estado da chamada de ferramenta
        session.setToolCalling(false);
    }

    @Override
    public void onError(ToolCallingObservationContext context) {
        ToolDefinition toolDefinition = context.getToolDefinition();
        String toolName = toolDefinition.name();
        Throwable error = context.getError();
        log.error("ToolCalling#onError - toolName: {}, error: {}", toolName, error.getMessage(), error);

        // Em caso de erro na chamada de ferramenta, também é necessário limpar o estado toolCalling, evitando que o player nunca chame sendStop
        if (context.containsKey(SESSION_ID_KEY)) {
            Object sessionIdObj = context.get(SESSION_ID_KEY);
            if (sessionIdObj instanceof String sessionId && toolSessionProvider != null) {
                ToolSession session = toolSessionProvider.getSession(sessionId);
                if (session != null) {
                    session.setToolCalling(false);
                }
            }
        }
    }

    @Override
    public boolean supportsContext(@NonNull Observation.Context context) {
        return context instanceof ToolCallingObservationContext;
    }

}
