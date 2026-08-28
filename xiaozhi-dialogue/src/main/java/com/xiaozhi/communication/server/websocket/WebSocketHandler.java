package com.xiaozhi.communication.server.websocket;

import com.xiaozhi.communication.common.*;
import com.xiaozhi.communication.domain.*;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.dialogue.llm.tool.mcp.device.DeviceMcpService;
import com.xiaozhi.utils.JsonUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WebSocketHandler extends AbstractWebSocketHandler {
    @Resource
    private SessionManager sessionManager;

    @Resource
    private MessageHandler messageHandler;

    @Resource
    private DeviceMcpService deviceMcpService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Map<String, String> headers = getHeadersFromSession(session);
        String deviceIdAuth = headers.get("device-id");
        String token = headers.get("Authorization");
        if (deviceIdAuth == null || deviceIdAuth.isEmpty()) {
            log.error("ID do dispositivo vazio");
            try {
                session.close(CloseStatus.BAD_DATA.withReason("ID do dispositivo vazio"));
            } catch (IOException e) {
                log.error("Falha ao fechar a conexão WebSocket", e);
            }
            return;
        }

        com.xiaozhi.communication.server.websocket.WebSocketSession xiaoZhiSession
                = new com.xiaozhi.communication.server.websocket.WebSocketSession(session);
        messageHandler.afterConnection(xiaoZhiSession, deviceIdAuth);
        sessionManager.openAudioChannel(xiaoZhiSession.getSessionId(), deviceIdAuth);

        log.info("Conexão WebSocket estabelecida com sucesso - SessionId: {}, DeviceId: {}", session.getId(), deviceIdAuth);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String sessionId = session.getId();
        ChatSession chatSession = sessionManager.getSession(sessionId);
        DeviceBO device = chatSession != null ? chatSession.getDevice() : null;
        String payload = message.getPayload();

        try {
            var msg = JsonUtil.fromJson(payload, Message.class);
            if (Objects.requireNonNull(msg) instanceof HelloMessage m) {
                handleHelloMessage(session, m);
            } else {
                if (device == null || device.getRoleId() == null) {
                    // Dispositivo não vinculado, tentando vinculação automática
                    boolean autoBound = messageHandler.handleUnboundDevice(sessionId, device);
                    if (!autoBound) {
                        // Falha na vinculação automática ou é necessário código de verificação; não continua processando a mensagem
                        return;
                    }
                    // Vinculação automática bem-sucedida, obtendo novamente as informações do dispositivo
                    device = chatSession != null ? chatSession.getDevice() : null;
                    if (device == null || device.getRoleId() == null) {
                        log.warn("Informações do dispositivo inconsistentes após a vinculação automática - SessionId: {}", sessionId);
                        return;
                    }
                    log.info("Vinculação automática bem-sucedida, continuando o processamento da mensagem - SessionId: {}, DeviceId: {}", sessionId, device.getDeviceId());
                }
                messageHandler.handleMessage(msg, sessionId);
            }
        } catch (Exception e) {
            log.error("Falha no processamento de handleTextMessage", e);
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String sessionId = session.getId();
        ChatSession chatSession = sessionManager.getSession(sessionId);
        if (chatSession == null || chatSession.getDevice() == null) {
            return;
        }
        messageHandler.handleBinaryMessage(sessionId, message.getPayload().array());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        messageHandler.afterConnectionClosed(sessionId);

        log.info("Conexão WebSocket fechada - SessionId: {}, status: {}", sessionId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String sessionId = session.getId();
        // Verifica se a exceção foi causada pelo fechamento normal da conexão pelo cliente
        if (isClientCloseRequest(exception)) {
            // Fechamento iniciado pelo cliente; registrado como log de nível informativo, não como erro
            log.info("Conexão WebSocket fechada pelo cliente - SessionId: {}", sessionId);
            messageHandler.afterConnectionClosed(sessionId);
        } else {
            // Erro de transporte real
            log.error("Erro de transporte do WebSocket - SessionId: {}", sessionId, exception);
        }
    }

    /**
     * Verifica se a exceção foi causada pelo fechamento da conexão pelo cliente
     */
    private boolean isClientCloseRequest(Throwable exception) {
        // Verifica os tipos comuns de exceção causados pelo fechamento da conexão pelo cliente
        if (exception instanceof IOException) {
            String message = exception.getMessage();
            if (message != null) {
                return message.contains("Connection reset by peer") ||
                    message.contains("Broken pipe") ||
                    message.contains("Connection closed") ||
                    message.contains("Uma conexão existente foi forçosamente fechada pelo host remoto");
            }
            // Trata EOFException, que geralmente é causada pelo fechamento da conexão pelo cliente
            return exception instanceof java.io.EOFException;
        }
        return false;
    }

    private void handleHelloMessage(WebSocketSession session, HelloMessage message) {
        var sessionId = session.getId();
        log.info("Mensagem hello recebida - SessionId: {}, JsonNode: {}", sessionId, message);

        if (message.getAudioParams() != null) {
            log.info("Parâmetros de áudio do cliente - formato: {}, taxa de amostragem: {}, canais: {}, duração do frame: {}ms",
                    message.getAudioParams().getFormat(),
                    message.getAudioParams().getSampleRate(),
                    message.getAudioParams().getChannels(),
                    message.getAudioParams().getFrameDuration());
        }

        // Responde à mensagem hello
        var resp = new HelloMessageResp()
                .setTransport("websocket")
                .setSessionId(sessionId)
                .setAudioParams(AudioParams.Opus);

        try {
            session.sendMessage(new TextMessage(JsonUtil.toJson(resp)));
            if(message.getFeatures() != null && message.getFeatures().getMcp()) {
                //Se o cliente tiver o protocolo MCP habilitado, inicializa as ferramentas MCP de forma assíncrona
                ChatSession chatSession = sessionManager.getSession(sessionId);
                Thread.startVirtualThread(() -> {
                    DeviceBO device = chatSession != null ? chatSession.getDevice() : null;
                    if (device != null && device.getRoleId() != null) {
                        deviceMcpService.initialize(chatSession);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Falha ao enviar a resposta hello", e);
        }
    }

    private Map<String, String> getHeadersFromSession(WebSocketSession session) {
        // Tenta obter o ID do dispositivo a partir do cabeçalho da requisição
        String[] deviceKeys = { "device-id", "mac_address", "uuid", "Authorization" };

        Map<String, String> headers = new HashMap<>();

        for (String key : deviceKeys) {
            String value = session.getHandshakeHeaders().getFirst(key);
            if (value != null) {
                headers.put(key, value);
            }
        }
        // Tenta obter a partir dos parâmetros da URI
        URI uri = session.getUri();
        if (uri != null) {
            String query = uri.getQuery();
            if (query != null) {
                for (String key : deviceKeys) {
                    String paramPattern = key + "=";
                    int startIdx = query.indexOf(paramPattern);
                    if (startIdx >= 0) {
                        startIdx += paramPattern.length();
                        int endIdx = query.indexOf('&', startIdx);
                        headers.put(key, endIdx >= 0 ? query.substring(startIdx, endIdx) : query.substring(startIdx));
                    }
                }
            }
        }
        return headers;
    }
}
