package com.xiaozhi.communication.common;

import com.xiaozhi.communication.domain.*;
import com.xiaozhi.communication.server.websocket.WebSocketSession;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.RoleBO;
import com.xiaozhi.common.model.bo.VerifyCodeBO;
import com.xiaozhi.device.domain.Device;
import com.xiaozhi.device.domain.repository.DeviceRepository;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.communication.message.MessageSender;
import com.xiaozhi.dialogue.DialogueService;
import com.xiaozhi.dialogue.audio.AecService;
import com.xiaozhi.ai.stt.SttResult;
import com.xiaozhi.dialogue.llm.factory.PersonaFactory;
import com.xiaozhi.ai.llm.factory.ChatModelFactory;
import com.xiaozhi.ai.tool.ToolsGlobalRegistry;
import com.xiaozhi.ai.tool.ToolsSessionHolder;
import com.xiaozhi.dialogue.llm.tool.device.IotService;
import com.xiaozhi.dialogue.audio.VadService;
import com.xiaozhi.dialogue.playback.Player;
import com.xiaozhi.dialogue.playback.ScheduledPlayer;
import com.xiaozhi.ai.tts.TtsServiceFactory;
import com.xiaozhi.enums.DeviceState;
import com.xiaozhi.enums.ListenState;
import com.xiaozhi.event.ChatAbortedEvent;
import com.xiaozhi.role.service.RoleService;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MessageHandler {
    @Resource
    private DeviceService deviceService;

    @Resource
    private DeviceRepository deviceRepository;

    @Resource
    private VadService vadService;

    @Resource
    private SessionManager sessionManager;

    @Resource
    private DialogueService dialogueService;

    @Resource
    private IotService iotService;

    @Resource
    private TtsServiceFactory ttsFactory;

    @Resource
    private PersonaFactory personaFactory;

    @Resource
    private ChatModelFactory chatModelFactory;

    @Resource
    private ToolsGlobalRegistry toolsGlobalRegistry;

    @Resource
    private RoleService roleService;

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private MessageSender messageService;

    @Resource
    private AecService aecService;

    @Resource
    private DeviceRegistry deviceRegistry;

    @Resource
    private InstanceIdHolder instanceIdHolder;

    @Resource
    private RedisBroadcast redisBroadcast;

    // Mapa para armazenar o ID do dispositivo e o estado de geração do código de verificação
    private final Map<String, Boolean> captchaGenerationInProgress = new ConcurrentHashMap<>();

    /**
     * Trata o evento de estabelecimento de conexão.
     *
     * @param chatSession
     * @param deviceIdAuth
     */
    public void afterConnection(ChatSession chatSession, String deviceIdAuth) {
        String deviceId = deviceIdAuth;
        String sessionId = chatSession.getSessionId();
        // Registra a sessão
        sessionManager.registerSession(sessionId, chatSession);

        // Limpeza de sessão fantasma entre instâncias: se o dispositivo estava vinculado anteriormente a outra instância, notifica a instância antiga para fechar a sessão
        // Neste ponto, registerDevice ainda não foi chamado; o deviceIdToSessionId desta instância não contém este dispositivo,
        // então, quando o broadcast chega a esta instância, getSessionByDeviceId retorna null e não há risco de fechar a própria sessão por engano
        String previousInstance = deviceRegistry.getInstance(deviceId);
        if (previousInstance != null && !previousInstance.equals(instanceIdHolder.getInstanceId())) {
            log.info("O dispositivo {} estava anteriormente na instância {}; notificando a instância antiga para limpar a sessão fantasma", deviceId, previousInstance);
            redisBroadcast.closeDeviceSession(deviceId);
        }

        log.info("Iniciando consulta das informações do dispositivo - DeviceId: {}", deviceId);
        DeviceBO device = Optional.ofNullable(deviceService.getBO(deviceId)).orElse(new DeviceBO());
        device.setDeviceId(deviceId);
        device.setSessionId(sessionId);
        sessionManager.registerDevice(sessionId, device);
        // Se já estiver vinculado, inicializa o restante
        if (!ObjectUtils.isEmpty(device) && device.getRoleId() != null) {
            initializeBoundDevice(chatSession, device);
        }
    }

    /**
     * Inicializa um dispositivo já vinculado
     *
     * @param chatSession sessão de chat
     * @param device informações do dispositivo
     */
    private void initializeBoundDevice(ChatSession chatSession, DeviceBO device) {
        String deviceId = device.getDeviceId();
        String sessionId = chatSession.getSessionId();
        
        //Isso precisa ficar fora da virtual thread
        ToolsSessionHolder toolsSessionHolder = new ToolsSessionHolder(chatSession.getSessionId(),
                device, toolsGlobalRegistry);
        chatSession.setToolsSessionHolder(toolsSessionHolder);
        // Obtém a descrição do papel a partir do cache/banco de dados. device
        RoleBO role = roleService.getBO(device.getRoleId());
        if (role == null) {
            throw new IllegalStateException("O papel não existe");
        }

        personaFactory.buildPersona(chatSession, device, role);

        // Inicializa o AEC já no estabelecimento da conexão, garantindo que os frames de referência de qualquer reprodução TTS futura (incluindo a resposta de wake word) não sejam descartados
        if (aecService != null) aecService.initSession(sessionId);

        // Após o processamento síncrono acima, atualiza de forma assíncrona o status online do dispositivo
        String newState = DeviceBO.DEVICE_STATE_ONLINE;
        Thread.startVirtualThread(() -> {
            try {
                deviceRepository.updateState(deviceId, newState);
            } catch (Exception e) {
                // Apenas registra um alerta, sem fechar a sessão: falha ao gravar o estado no banco não afeta a comunicação normal do dispositivo
                log.warn("Falha ao atualizar o status online do dispositivo - DeviceId: {}, State: {}", deviceId, newState, e);
            }
        });

    }

    /**
     * Trata o evento de fechamento de conexão.
     *
     * @param sessionId
     */
    public void afterConnectionClosed(String sessionId) {
        ChatSession chatSession = sessionManager.getSession(sessionId);
        if (chatSession == null) {
            return;
        }
        // Limpa os recursos ao fechar a conexão
        DeviceBO device = chatSession.getDevice();
        if (device != null) {
            String deviceId = device.getDeviceId();

            // Durante o desligamento do serviço, pula a gravação do estado no banco: na inicialização é feito um bulk reset de todos os dispositivos para offline, então não é necessário gravar um por um no desligamento
            if (!sessionManager.isShuttingDown()) {
                Thread.startVirtualThread(() -> {
                    try {
                        String newState = DeviceBO.DEVICE_STATE_OFFLINE;

                        // Proteção de timing: verifica se o dispositivo já reconectou
                        ChatSession currentSession = sessionManager.getSessionByDeviceId(deviceId);
                        if (currentSession != null && !sessionId.equals(currentSession.getSessionId())) {
                            return;
                        }

                        deviceRepository.updateState(deviceId, newState);
                        log.info("Conexão fechada - SessionId: {}, DeviceId: {}, novo estado: {}",
                                sessionId, deviceId, newState);
                    } catch (Exception e) {
                        log.error("Falha ao atualizar o estado do dispositivo", e);
                    }
                });
            }
        }
        // Limpa a sessão
        sessionManager.closeSession(sessionId);
        // Limpa a sessão de VAD
        vadService.resetSession(sessionId);
        // Limpa a sessão de AEC
        if (aecService != null) aecService.resetSession(sessionId);

    }

    /**
     * Trata os dados de áudio
     *
     * @param sessionId
     * @param opusData
     */
    public void handleBinaryMessage(String sessionId, byte[] opusData) {
        ChatSession chatSession = sessionManager.getSession(sessionId);
        if ((chatSession == null || !chatSession.isOpen()) && !vadService.isSessionInitialized(sessionId)) {
            return;
        }
        // Delega o processamento dos dados de áudio ao DialogueService
        dialogueService.processAudioData(chatSession, opusData);

    }

    /**
     * Trata dispositivo não vinculado
     * @return true se o dispositivo foi vinculado automaticamente com sucesso, false se for necessário gerar um código de verificação
     */
    public boolean handleUnboundDevice(String sessionId, DeviceBO device) {
        String deviceId;
        if (device == null || device.getDeviceId() == null) {
            return false;
        }
        deviceId = device.getDeviceId();
        
        // Verifica se é um dispositivo virtual com prefixo user_chat_; se for, vincula automaticamente
        if (deviceId.startsWith("user_chat_")) {
            try {
                log.info("Dispositivo virtual {} detectado, tentando vinculação automática", deviceId);
                
                // Extrai o ID do usuário
                String userIdStr = deviceId.substring("user_chat_".length());
                Integer userId = Integer.parseInt(userIdStr);
                
                RoleBO defaultRole = roleService.getDefaultOrFirstBO(userId);
                Integer defaultRoleId = defaultRole != null ? defaultRole.getRoleId() : null;
                
                if (defaultRoleId != null) {
                    // Cria o dispositivo virtual e vincula ao papel padrão
                    Device createdDevice = Device.newDevice(
                            deviceId, "Assistente", "web", userId, defaultRoleId);
                    deviceRepository.save(createdDevice);
                    if (createdDevice.getDeviceId() != null) {
                        log.info("Dispositivo virtual {} vinculado automaticamente com sucesso, ID do papel: {}", deviceId, defaultRoleId);
                        
                        // Consulta novamente as informações do dispositivo
                        DeviceBO boundDevice = deviceService.getBO(deviceId);
                        if (boundDevice != null) {
                            // Atualiza as informações do dispositivo na sessão
                            boundDevice.setSessionId(sessionId);
                            sessionManager.registerDevice(sessionId, boundDevice);
                            
                            // Obtém o objeto da sessão
                            ChatSession chatSession = sessionManager.getSession(sessionId);
                            if (chatSession != null && chatSession.isOpen()) {
                                // Inicializa a sessão do dispositivo (mesma lógica de afterConnection)
                                initializeBoundDevice(chatSession, boundDevice);
                                log.info("Dispositivo virtual {} inicializado, o diálogo pode começar", deviceId);
                            }
                            
                            // Dispositivo vinculado e inicializado; retorna true indicando que o processamento da mensagem pode continuar
                            return true;
                        }
                    } else {
                        log.warn("Falha ao vincular automaticamente o dispositivo virtual {}", deviceId);
                    }
                } else {
                    log.warn("O usuário {} não possui papéis disponíveis; não é possível vincular o dispositivo virtual automaticamente", userId);
                }
            } catch (NumberFormatException e) {
                log.error("Falha ao analisar o ID do dispositivo virtual: {}", deviceId, e);
            } catch (Exception e) {
                log.error("Falha ao vincular automaticamente o dispositivo virtual: {}", deviceId, e);
            }
        }
        
        ChatSession chatSession = sessionManager.getSession(sessionId);
        if (chatSession == null || !chatSession.isOpen()) {
            return false;
        }
        // Verifica se já está em processamento, usando operação CAS para garantir segurança em concorrência
        Boolean previous = captchaGenerationInProgress.putIfAbsent(deviceId, true);
        if (previous != null && previous) {
            return false; // já está em processamento
        }

        Thread.startVirtualThread(() -> {
            try {
                // Para dispositivos não vinculados, o player é de uso único e não precisa ser vinculado ao ChatSession.
                Player player = new ScheduledPlayer(chatSession, messageService);
                // Dispositivo registrado, mas sem modelo configurado
                if (device.getDeviceName() != null && device.getRoleId() == null) {
                    String message = "Dispositivo sem papel configurado; acesse a página de configuração de papéis para concluir a configuração antes de iniciar o diálogo";

                    Path audioFilePath = ttsFactory.getDefaultTtsService().textToSpeech(message);

                    player.play(message, audioFilePath);

                    // Remove a marcação após um período de atraso
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    captchaGenerationInProgress.remove(deviceId);
                    return;
                }

                // Dispositivo sem nome, gerando código de verificação
                // Gera um novo código de verificação
                VerifyCodeBO codeResult = deviceService.generateCode(deviceId, sessionId, device.getType());
                Path audioPath;
                if (!StringUtils.hasText(codeResult.getAudioPath())) {
                    String codeMessage = "Acesse a página de gerenciamento de dispositivos para adicioná-lo, informando o código de verificação " + codeResult.getCode();
                    audioPath = ttsFactory.getDefaultTtsService().textToSpeech(codeMessage);
                    deviceService.updateCodeAudioPath(deviceId, sessionId, codeResult.getCode(), audioPath.toString());
                } else {
                    audioPath = Path.of(codeResult.getAudioPath());
                }

                player.play(codeResult.getCode(), audioPath);
                // Remove a marcação após um período de atraso
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                captchaGenerationInProgress.remove(deviceId);

            } catch (Exception e) {
                log.error("Falha ao tratar dispositivo não vinculado", e);
                captchaGenerationInProgress.remove(deviceId);
            }
        });
        
        // Retorna false indicando que é necessário o fluxo de código de verificação; não continua processando a mensagem atual
        return false;
    }

    private void handleListenMessage(ChatSession chatSession, ListenMessage message) {
        String sessionId = chatSession.getSessionId();
        log.info("Mensagem listen recebida - SessionId: {}, State: {}, Mode: {}", sessionId, message.getState(), message.getMode());

        // Se a sessão estiver marcada para fechamento iminente, ignora a mensagem listen
        if (chatSession.getPlayer().getFunctionAfterChat()!= null) {
            return;
        }

        chatSession.setMode(message.getMode());

        // Trata os diferentes estados de escuta de acordo com o state
        switch (message.getState()) {
            case ListenState.Start:
                // O dispositivo começou a gravar, entrando em estado de escuta
                log.info("Iniciando escuta - Mode: {}", message.getMode());

                chatSession.transitionTo(DeviceState.LISTENING);

                // Inicializa a sessão de VAD
                vadService.initSession(sessionId);
                // Inicializa a sessão de AEC
                if (aecService != null) aecService.initSession(sessionId);
                break;

            case ListenState.Stop:
                // Interrompe a escuta
                log.info("Escuta interrompida");

                // Fecha o fluxo de áudio, retornando ao estado IDLE
                chatSession.completeAudioStream();
                chatSession.closeAudioStream();
                chatSession.transitionTo(DeviceState.IDLE);
                // Reseta a sessão de VAD
                vadService.resetSession(sessionId);
                // Atenção: não reseta a sessão de AEC, mantendo o estado do filtro já convergido para reutilização em diálogos futuros
                break;

            case ListenState.Text:
                // Detecta entrada de texto de chat — garante que o AEC esteja inicializado antes do início do TTS
                if (aecService != null) aecService.initSession(sessionId);
                Player player = chatSession.getPlayer();
                if (player != null ) {
                    String modeValue = message.getMode() != null ? message.getMode().getValue() : null;
                    String abortDeviceId = chatSession.getDevice() != null ? chatSession.getDevice().getDeviceId() : null;
                    applicationContext.publishEvent(new ChatAbortedEvent(this, chatSession.getSessionId(), abortDeviceId, modeValue));
                }
                // Garante que o Persona exista, notifica o dispositivo e atualiza o horário de atividade
                sessionManager.updateLastActivity(sessionId);
                personaFactory.buildPersona(chatSession);
                messageService.sendSttMessage(chatSession, message.getText());
                log.info("Processando entrada de texto de chat: \"{}\"", message.getText());
                dialogueService.handleText(chatSession, SttResult.textOnly(message.getText()));
                break;

            case ListenState.Detect:
                // Palavra de ativação detectada — garante que o AEC esteja inicializado antes do início do TTS
                if (aecService != null) aecService.initSession(sessionId);
                dialogueService.handleWakeWord(chatSession, message.getText());
                break;

            default:
                log.warn("Estado de listen desconhecido: {}", message.getState());
        }
    }

    private void handleAbortMessage(ChatSession session, AbortMessage message) {
        String deviceId = session.getDevice() != null ? session.getDevice().getDeviceId() : null;
        applicationContext.publishEvent(new ChatAbortedEvent(this, session.getSessionId(), deviceId, message.getReason()));
    }

    private void handleIotMessage(ChatSession chatSession, IotMessage message) {
        String sessionId = chatSession.getSessionId();
        // Trata as informações de descrição do dispositivo
        if (message.getDescriptors() != null) {
            log.info("Informações de descrição do dispositivo IoT recebidas - SessionId: {}: {}", sessionId, message.getDescriptors());
            // Lógica de tratamento das informações de descrição do dispositivo
            iotService.handleDeviceDescriptors(sessionId, message.getDescriptors());
        }

        // Trata a atualização de estado do dispositivo
        if (message.getStates() != null) {
            log.info("Atualização de estado do dispositivo IoT recebida - SessionId: {}: {}", sessionId, message.getStates());
            // Lógica de tratamento da atualização de estado do dispositivo
            iotService.handleDeviceStates(sessionId, message.getStates());
        }
    }

    private void handleGoodbyeMessage(ChatSession session, GoodbyeMessage message) {
        // Verifica se a sessão já foi fechada, evitando processamento duplicado
        if (!session.isAudioChannelOpen()) {
            return;
        }

        // Limpa primeiro as sessões de VAD e AEC, evitando que mensagens listen subsequentes as reinicializem
        String sessionId = session.getSessionId();
        vadService.resetSession(sessionId);
        if (aecService != null) aecService.resetSession(sessionId);

        // Interrompe o diálogo em andamento, parando o TTS e o envio de áudio
        String goodbyeDeviceId = session.getDevice() != null ? session.getDevice().getDeviceId() : null;
        applicationContext.publishEvent(new ChatAbortedEvent(this, session.getSessionId(), goodbyeDeviceId, "Dispositivo saiu voluntariamente"));

        sessionManager.closeSession(session);
    }

    private void handleDeviceMcpMessage(ChatSession chatSession, DeviceMcpMessage message) {
        Long mcpRequestId = message.getPayload().getId();
        CompletableFuture<DeviceMcpMessage> future = chatSession.getDeviceMcpHolder().getMcpPendingRequests().get(mcpRequestId);
        if(future != null){
            future.complete(message);
            chatSession.getDeviceMcpHolder().getMcpPendingRequests().remove(mcpRequestId);
        }
    }

    public void handleMessage(Message msg, String sessionId) {
        var chatSession = sessionManager.getSession(sessionId);
        switch (msg) {
            case ListenMessage m -> handleListenMessage(chatSession, m);
            case IotMessage m -> handleIotMessage(chatSession, m);
            case AbortMessage m -> handleAbortMessage(chatSession, m);
            case GoodbyeMessage m -> handleGoodbyeMessage(chatSession, m);
            case DeviceMcpMessage m -> handleDeviceMcpMessage(chatSession, m);
            default -> {
            }
        }
    }
}
