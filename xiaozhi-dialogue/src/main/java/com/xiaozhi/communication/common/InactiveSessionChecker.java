package com.xiaozhi.communication.common;

import com.xiaozhi.communication.server.websocket.WebSocketSession;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.xiaozhi.enums.DeviceState;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
/**
 * Verificador de sessões inativas, checa periodicamente e fecha as sessões que ultrapassaram o timeout de inatividade.
 * Extraído de SessionManager para ter responsabilidade única.
 */
@Slf4j
@Component
public class InactiveSessionChecker {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Resource
    private SessionManager sessionManager;

    @Resource
    private DeviceRegistry deviceRegistry;

    @Value("${xiaozhi.check.inactive.session:true}")
    private boolean checkInactiveSession;

    @Value("${inactive.timeout.seconds:60}")
    private int inactiveTimeOutSeconds;

    @PostConstruct
    public void init() {
        if (checkInactiveSession) {
            scheduler.scheduleAtFixedRate(this::checkInactiveSessions, 10, 10, TimeUnit.SECONDS);
            log.info("Tarefa de verificação de sessões inativas iniciada, timeout: {} segundos", inactiveTimeOutSeconds);
        }
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Tarefa de verificação de sessões inativas encerrada");
    }

    private void checkInactiveSessions() {
        Instant now = Instant.now();
        sessionManager.getAllSessions().forEach(session -> {
            // Atualiza o heartbeat de dispositivo-instância
            if (session.getDevice() != null) {
                deviceRegistry.refresh(session.getDevice().getDeviceId());
            }
            if (session instanceof WebSocketSession || session.isAudioChannelOpen()) {
                Instant lastActivity = session.getLastActivityTime();
                if (lastActivity != null) {
                    Duration inactiveDuration = Duration.between(lastActivity, now);
                    if (inactiveDuration.getSeconds() > inactiveTimeOutSeconds) {
                        // Não dispara o timeout enquanto está falando ou pensando (SPEAKING/THINKING têm processamento ativo)
                        // IDLE e LISTENING podem disparar o timeout (dispositivo conectado, mas o usuário ficou sem falar por muito tempo)
                        if (session.getDeviceState() != DeviceState.SPEAKING
                                && session.getDeviceState() != DeviceState.THINKING) {
                            log.info("A sessão {} está há {} segundos sem atividade válida; enviando aviso de timeout e encerrando automaticamente",
                                    session.getSessionId(), inactiveDuration.getSeconds());
                            session.clearAudioSinks();
                            if (session.getPersona() != null) {
                                session.getPersona().sendGoodbyeMessage();
                            }
                            if (session instanceof WebSocketSession) {
                                sessionManager.closeSession(session);
                            }
                        }
                    }
                }
            }
        });
    }
}
