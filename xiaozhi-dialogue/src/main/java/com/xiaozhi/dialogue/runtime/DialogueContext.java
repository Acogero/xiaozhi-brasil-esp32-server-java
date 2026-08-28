package com.xiaozhi.dialogue.runtime;

import com.xiaozhi.ai.tool.ToolsSessionHolder;
import com.xiaozhi.dialogue.playback.Player;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.tool.ToolCallback;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contexto do diálogo, carrega o estado diretamente relacionado à lógica do diálogo em uma sessão de conversa.
 * Extraído do ChatSession (camada de comunicação), para que a camada de comunicação não carregue mais a lógica de negócio do diálogo.
 */
@Getter
@Setter
public class DialogueContext {

    private Persona persona;

    private final AtomicReference<Player> playerRef = new AtomicReference<>();

    private ToolsSessionHolder toolsSessionHolder;

    /**
     * Caminho de salvamento do áudio do usuário da rodada atual do diálogo, reutilizável pelas Functions
     */
    private volatile Path userAudioPath;

    /**
     * Lista de detalhes das chamadas de ferramentas na rodada atual do diálogo (incluindo Functions internas e ferramentas MCP)
     * Anexada pelo XiaoZhiToolCallingManager ao executar ferramentas, e limpa pelo Persona após o salvamento da mensagem.
     */
    private final List<ToolCallInfo> toolCallDetails = new CopyOnWriteArrayList<>();

    /**
     * Mensagem intermediária da chamada de ferramenta: o AssistantMessage em que o modelo solicita a chamada da ferramenta (contém toolCalls)
     */
    private volatile AssistantMessage toolCallAssistantMessage;

    /**
     * Mensagem intermediária da chamada de ferramenta: resultado da execução da ferramenta
     */
    private volatile ToolResponseMessage toolResponseMessage;

    /**
     * Detalhes da chamada de ferramenta
     */
    public record ToolCallInfo(String name, String arguments, String result) {}

    public Player getPlayer() {
        return playerRef.get();
    }

    public void setPlayer(Player player) {
        playerRef.set(player);
    }

    public void addToolCallDetail(String name, String arguments, String result) {
        toolCallDetails.add(new ToolCallInfo(name, arguments, result));
    }

    public synchronized List<ToolCallInfo> drainToolCallDetails() {
        List<ToolCallInfo> details = new ArrayList<>(toolCallDetails);
        toolCallDetails.clear();
        return details;
    }

    public void setToolCallMessages(AssistantMessage assistantMessage, ToolResponseMessage responseMessage) {
        this.toolCallAssistantMessage = assistantMessage;
        this.toolResponseMessage = responseMessage;
    }

    public synchronized AssistantMessage drainToolCallAssistantMessage() {
        AssistantMessage msg = this.toolCallAssistantMessage;
        this.toolCallAssistantMessage = null;
        return msg;
    }

    public synchronized ToolResponseMessage drainToolResponseMessage() {
        ToolResponseMessage msg = this.toolResponseMessage;
        this.toolResponseMessage = null;
        return msg;
    }

    public boolean isFunctionCalled() {
        return !toolCallDetails.isEmpty();
    }

    public List<ToolCallback> getToolCallbacks() {
        return toolsSessionHolder.getAllFunction();
    }
}
