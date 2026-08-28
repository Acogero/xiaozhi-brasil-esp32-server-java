package com.xiaozhi.communication.server.websocket;

import com.xiaozhi.communication.common.ChatSession;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSocketSession extends ChatSession {
    /**
     * Sessão de conexão da sessão atual
     */
    protected org.springframework.web.socket.WebSocketSession session;

    public WebSocketSession(String sessionId) {
        super(sessionId);
    }

    public WebSocketSession(org.springframework.web.socket.WebSocketSession session) {
        super(session.getId());
        this.session = session;
    }

    @Override
    public String getSessionId() {
        return session.getId();
    }

    public org.springframework.web.socket.WebSocketSession getSession() {
        return this.session;
    }

    @Override
    public void close() {
        if(session != null){
            try {
                session.close();
            } catch (IOException e) {
                log.error("Erro ao fechar a sessão WebSocket - SessionId: {}", getSessionId(), e);
            }
        }
    }

    @Override
    public boolean isOpen() {
        return session.isOpen();
    }

    @Override
    public boolean isAudioChannelOpen() {
        return session.isOpen();
    }

    @Override
    public void sendTextMessage(String message) {
        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            log.error("Falha ao enviar a mensagem Text, message: {}", message, e);
        }
    }

    @Override
    public void sendBinaryMessage(byte[] message) {
        try {
            session.sendMessage(new BinaryMessage(message));
        } catch (IOException e) {
            log.error("Falha ao enviar a mensagem Binary", e);
        }
    }
}
