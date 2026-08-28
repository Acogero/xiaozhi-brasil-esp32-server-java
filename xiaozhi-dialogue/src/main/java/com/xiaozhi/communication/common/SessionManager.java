package com.xiaozhi.communication.common;

import com.xiaozhi.communication.server.websocket.WebSocketSession;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.ai.llm.memory.Conversation;
import com.xiaozhi.device.domain.repository.DeviceRepository;
import com.xiaozhi.event.ChatAudioOpenedEvent;
import com.xiaozhi.event.ChatSessionClosedEvent;
import com.xiaozhi.event.DeviceOnlineEvent;
import com.xiaozhi.event.DeviceUpdatedEvent;
import com.xiaozhi.event.ChatSessionOpenedEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
/**
 * Registro de sessões, responsável por gerenciar o estado de todas as sessões conectadas.
 * Responsabilidades principais: register / get / remove / close, além do registro de dispositivos e do gerenciamento do estado do código de verificação.
 * <p>
 * A verificação de sessões inativas foi extraída para {@link InactiveSessionChecker}.
 * O gerenciamento do fluxo de áudio foi migrado para métodos de instância de {@link ChatSession}.
 */
@Slf4j
@Service
public class SessionManager {
    private final ConcurrentHashMap<String, ChatSession> sessions = new ConcurrentHashMap<>();

    /** Índice reverso deviceId → sessionId, busca O(1) da sessão do dispositivo */
    private final ConcurrentHashMap<String, String> deviceIdToSessionId = new ConcurrentHashMap<>();

    // Armazena o estado de geração do código de verificação
    private final ConcurrentHashMap<String, Boolean> captchaState = new ConcurrentHashMap<>();

    // Usado para adiar o reset do estado do dispositivo na inicialização
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Sinalizador de encerramento do serviço; durante o desligamento, pula a gravação do estado do dispositivo no banco (na inicialização é feito bulk reset, então não é necessário regravar)
    private volatile boolean shuttingDown = false;

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    @Lazy
    private DeviceRepository deviceRepository;

    @Resource
    private DeviceRegistry deviceRegistry;

    @Resource
    private InstanceIdHolder instanceIdHolder;

    @Value("${xiaozhi.check.inactive.session:true}")
    private boolean checkInactiveSession;

    @PostConstruct
    public void init() {
        if (checkInactiveSession) {
            // Na inicialização do projeto, reseta para offline somente os dispositivos pertencentes a esta instância (execução adiada para evitar dependência circular)
            scheduler.schedule(() -> {
                try {
                    Set<String> ownDeviceIds = deviceRegistry.getOwnDeviceIds();
                    if (!ownDeviceIds.isEmpty()) {
                        int updated = deviceRepository.batchUpdateState(ownDeviceIds, DeviceBO.DEVICE_STATE_OFFLINE);
                        log.info("Projeto iniciado; {} dispositivo(s) desta instância resetado(s) para offline", updated);
                        // Limpa os mapeamentos antigos no Redis desta instância
                        for (String deviceId : ownDeviceIds) {
                            deviceRegistry.unbind(deviceId);
                        }
                    }
                    log.info("Projeto iniciado, instanceId: {}", instanceIdHolder.getInstanceId());
                } catch (Exception e) {
                    log.error("Falha ao resetar o estado dos dispositivos na inicialização do projeto", e);
                }
            }, 1, TimeUnit.SECONDS);
        }
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * O ContextClosedEvent é disparado antes de todos os @PreDestroy,
     * garantindo que o sinalizador shuttingDown já esteja definido antes que os callbacks de desconexão ocorram.
     */
    @EventListener(ContextClosedEvent.class)
    public void onContextClosed() {
        shuttingDown = true;
        scheduler.shutdown();
    }

    /**
     * Abre o canal de áudio e publica o evento (para uso do Handler)
     */
    public void openAudioChannel(String sessionId, String deviceId) {
        applicationContext.publishEvent(new ChatAudioOpenedEvent(this, sessionId, deviceId));
    }

    /**
     * Sincroniza com a sessão correspondente quando as informações do dispositivo mudam
     */
    @EventListener
    public void onDeviceUpdated(DeviceUpdatedEvent event) {
        DeviceBO device = event.getDevice();
        if (device == null || device.getDeviceId() == null) {
            return;
        }
        ChatSession session = getSessionByDeviceId(device.getDeviceId());
        if (session != null) {
            DeviceBO currentDevice = session.getDevice();
            if (currentDevice != null) {
                if (!StringUtils.hasText(device.getSessionId())) {
                    device.setSessionId(currentDevice.getSessionId());
                }
                if (!StringUtils.hasText(device.getRoleName())) {
                    device.setRoleName(currentDevice.getRoleName());
                }
            }
            session.setDevice(device);
        }
    }

    // ========== Registro e obtenção de sessão ==========

    public void registerSession(String sessionId, ChatSession chatSession) {
        sessions.put(sessionId, chatSession);
        log.info("Sessão registrada - SessionId: {}  SessionType: {}", sessionId, chatSession.getClass().getSimpleName());
        String deviceId = chatSession.getDevice() != null ? chatSession.getDevice().getDeviceId() : null;
        applicationContext.publishEvent(new ChatSessionOpenedEvent(this, sessionId, deviceId));
    }

    public void removeSession(String sessionId) {
        ChatSession removed = sessions.remove(sessionId);
        if (removed != null && removed.getDevice() != null) {
            deviceIdToSessionId.remove(removed.getDevice().getDeviceId());
        }
    }

    public ChatSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public ChatSession getSessionByDeviceId(String deviceId) {
        String sessionId = deviceIdToSessionId.get(deviceId);
        if (sessionId != null) {
            ChatSession session = sessions.get(sessionId);
            if (session != null) {
                return session;
            }
            // Mapeamento residual, limpando
            deviceIdToSessionId.remove(deviceId);
        }
        return null;
    }

    /**
     * Obtém todas as sessões (para uso em iterações por InactiveSessionChecker e outros)
     */
    public Collection<ChatSession> getAllSessions() {
        return sessions.values();
    }

    // ========== Fechamento de sessão ==========

    public void closeSession(String sessionId) {
        ChatSession chatSession = sessions.get(sessionId);
        if (chatSession != null) {
            closeSession(chatSession);
        }
    }

    public void closeSession(ChatSession chatSession) {
        if (chatSession == null) {
            return;
        }
        try {
            if (chatSession instanceof WebSocketSession) {
                removeSession(chatSession.getSessionId());
            }
            // Remove o vínculo dispositivo-instância
            if (chatSession.getDevice() != null) {
                deviceRegistry.unbind(chatSession.getDevice().getDeviceId());
            }
            if (chatSession.isAudioChannelOpen()) {
                chatSession.close();
                String closeDeviceId = chatSession.getDevice() != null ? chatSession.getDevice().getDeviceId() : null;
                applicationContext.publishEvent(new ChatSessionClosedEvent(this, chatSession.getSessionId(), closeDeviceId));
                log.info("Sessão fechada - SessionId: {} SessionType: {}", chatSession.getSessionId(), chatSession.getClass().getSimpleName());
            }
            chatSession.clearAudioSinks();
        } catch (Exception e) {
            log.error("Erro ao limpar os recursos da sessão - SessionId: {}",
                    chatSession.getSessionId(), e);
        }
    }

    // ========== Registro de dispositivo ==========

    public void registerDevice(String sessionId, DeviceBO device) {
        if (device == null || device.getDeviceId() == null) {
            log.warn("Falha ao registrar o dispositivo: device ou deviceId é null, sessionId={}", sessionId);
            return;
        }
        ChatSession chatSession = sessions.get(sessionId);
        if (chatSession != null) {
            chatSession.setDevice(device);
            deviceIdToSessionId.put(device.getDeviceId(), sessionId);
            updateLastActivity(sessionId);
            deviceRegistry.bind(device.getDeviceId());
            log.debug("Configuração do dispositivo registrada - SessionId: {}, DeviceId: {}", sessionId, device.getDeviceId());
            applicationContext.publishEvent(new DeviceOnlineEvent(this, device.getDeviceId()));
        }
    }

    public void updateLastActivity(String sessionId) {
        ChatSession session = sessions.get(sessionId);
        if (session != null) {
            session.setLastActivityTime(java.time.Instant.now());
        }
    }

    // ========== Estado do código de verificação ==========

    public boolean markCaptchaGeneration(String deviceId) {
        return captchaState.putIfAbsent(deviceId, Boolean.TRUE) == null;
    }

    public void unmarkCaptchaGeneration(String deviceId) {
        captchaState.remove(deviceId);
    }

    // ========== Consulta entre sessões ==========

    public Optional<Conversation> findConversation(String deviceId) {
        ChatSession session = getSessionByDeviceId(deviceId);
        if (session != null && session.getPersona() != null) {
            return Optional.ofNullable(session.getPersona().getConversation());
        }
        return Optional.empty();
    }
}
