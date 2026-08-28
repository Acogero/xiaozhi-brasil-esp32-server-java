package com.xiaozhi.communication.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.event.TtsPlaybackCompletedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MessageSender {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ApplicationEventPublisher eventPublisher;

    public MessageSender(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void sendTtsMessage(ChatSession session, String text, String state) {
        if (session == null || !session.isOpen()) {
            log.error("ChatSession é null ou já está fechado, verifique! {}", Arrays.toString(Thread.currentThread().getStackTrace()));
            return;
        }
        ObjectNode messageJson = objectMapper.createObjectNode();
        messageJson.put("type", "tts");
        messageJson.put("state", state);
        if (text != null) {
            messageJson.put("text", text);
        }

        String jsonMessage = messageJson.toString();
        log.info("sendTtsMessage enviando mensagem - SessionId: {}, Message: {}", session.getSessionId(), jsonMessage);
        sendTextMessage(session, jsonMessage);

        if ("stop".equals(state)) {
            eventPublisher.publishEvent(new TtsPlaybackCompletedEvent(this, session.getSessionId()));
        }
    }

    public void sendSttMessage(ChatSession session, String text) {
        if (session == null || !session.isOpen()) {
            log.warn("sendSttMessage não conseguiu enviar a mensagem - sessão fechada ou nula");
            return;
        }
        ObjectNode messageJson = objectMapper.createObjectNode();
        messageJson.put("type", "stt");
        messageJson.put("text", text);

        String jsonMessage = messageJson.toString();
        log.info("sendSttMessage enviando mensagem - SessionId: {}, Message: {}", session.getSessionId(), jsonMessage);
        sendTextMessage(session, jsonMessage);
    }

    public void sendIotCommandMessage(ChatSession session, List<Map<String, Object>> commands) {
        if (session == null || !session.isOpen()) {
            log.warn("sendIotCommandMessage não conseguiu enviar a mensagem - sessão fechada ou nula");
            return;
        }
        ObjectNode messageJson = objectMapper.createObjectNode();
        messageJson.put("session_id", session.getSessionId());
        messageJson.put("type", "iot");
        messageJson.set("commands", objectMapper.valueToTree(commands));

        String jsonMessage = messageJson.toString();
        log.debug("sendIotCommandMessage enviando mensagem iot - SessionId: {}, Message: {}", session.getSessionId(), messageJson);
        sendTextMessage(session, jsonMessage);
    }

    public void sendEmotion(ChatSession session, String emotion) {
        if (session == null || !session.isOpen()) {
            log.warn("sendEmotion não conseguiu enviar a mensagem - sessão fechada ou nula");
            return;
        }
        ObjectNode messageJson = objectMapper.createObjectNode();
        messageJson.put("session_id", session.getSessionId());
        messageJson.put("type", "llm");
        messageJson.put("emotion", emotion);
        messageJson.put("text", emotion);
        String jsonMessage = messageJson.toString();
        log.info("sendEmotion enviando mensagem Emotion - SessionId: {}, Message: {}", session.getSessionId(), jsonMessage);
        sendTextMessage(session, jsonMessage);
    }

    public void sendTextMessage(ChatSession chatSession, String message) {
        try {
            if (chatSession == null || !chatSession.isOpen()) {
                return;
            }
            chatSession.sendTextMessage(message);
        } catch (Exception e) {
            log.error("Exceção ao enviar a mensagem - SessionId: {}, Error: {}", chatSession.getSessionId(), e.getMessage());
            throw new RuntimeException("Falha ao enviar a mensagem, conteúdo da mensagem: " + message, e);
        }
    }

    public void sendBinaryMessage(ChatSession chatSession, byte[] opusFrame) {
        try {
            if (chatSession == null || !chatSession.isOpen()) {
                return;
            }
            chatSession.sendBinaryMessage(opusFrame);
        } catch (Exception e) {
            log.error("Exceção ao enviar a mensagem - SessionId: {}, Error: {}", chatSession.getSessionId(), e.getMessage());
            throw new RuntimeException("Falha ao enviar a mensagem de áudio, conteúdo da mensagem", e);
        }
    }
}
