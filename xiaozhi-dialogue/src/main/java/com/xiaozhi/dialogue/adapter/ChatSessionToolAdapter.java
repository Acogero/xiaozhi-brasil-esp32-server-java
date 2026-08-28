package com.xiaozhi.dialogue.adapter;

import com.xiaozhi.ai.tool.ToolsSessionHolder;
import com.xiaozhi.ai.tool.session.ToolSession;
import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.dialogue.playback.Player;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;

/**
 * Adaptador de ChatSession para ToolSession.
 * Isola a camada de ai da camada de comunicação; a camada de ai só pode acessar métodos relacionados à chamada de ferramentas,
 * sem visibilidade de detalhes da camada de comunicação como audioSinks, DeviceState, IotDescriptor etc.
 */
public class ChatSessionToolAdapter implements ToolSession {

    private final ChatSession chatSession;

    public ChatSessionToolAdapter(ChatSession chatSession) {
        this.chatSession = chatSession;
    }

    @Override
    public String getSessionId() {
        return chatSession.getSessionId();
    }

    @Override
    public Integer getRoleId() {
        return chatSession.getDevice() != null ? chatSession.getDevice().getRoleId() : null;
    }

    @Override
    public String getDeviceId() {
        return chatSession.getDevice() != null ? chatSession.getDevice().getDeviceId() : null;
    }

    @Override
    public ToolsSessionHolder getToolsSessionHolder() {
        return chatSession.getToolsSessionHolder();
    }

    @Override
    public boolean isDeviceMcpInitialized() {
        return chatSession.getDeviceMcpHolder() != null && chatSession.getDeviceMcpHolder().isMcpInitialized();
    }

    @Override
    public void addToolCallDetail(String name, String args, String result) {
        chatSession.addToolCallDetail(name, args, result);
    }

    @Override
    public void addToolCallMessages(AssistantMessage toolCallAssistantMessage,
                                    ToolResponseMessage toolResponseMessage) {
        chatSession.getDialogueContext().setToolCallMessages(toolCallAssistantMessage, toolResponseMessage);
    }

    @Override
    public void sendTextMessage(String message) {
        chatSession.sendTextMessage(message);
    }

    @Override
    public boolean isOpen() {
        return chatSession.isOpen();
    }

    @Override
    public void setToolCalling(boolean calling) {
        Player player = chatSession.getPlayer();
        if (player != null) {
            player.setToolCalling(calling);
        }
    }
}
