package com.xiaozhi.communication.common;

import com.xiaozhi.communication.domain.iot.IotDescriptor;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.MessageBO;
import com.xiaozhi.ai.tool.ToolsSessionHolder;
import com.xiaozhi.dialogue.llm.tool.mcp.device.DeviceMcpHolder;
import com.xiaozhi.dialogue.runtime.DialogueContext;
import com.xiaozhi.dialogue.playback.Player;
import com.xiaozhi.enums.DeviceState;
import com.xiaozhi.enums.ListenMode;
import com.xiaozhi.utils.AudioUtils;
import com.xiaozhi.dialogue.runtime.Persona;
import lombok.Data;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Sinks;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public abstract class ChatSession {
    /**
     * sessionId da sessão atual
     */
    protected String sessionId;
    /**
     * Informações do dispositivo
     */
    protected DeviceBO device;

    /**
     * Contexto do diálogo, carrega o estado diretamente relacionado à lógica do diálogo (Persona, Player, callbacks de ferramentas etc.).
     * Detalhe de implementação interna; o acesso externo é feito por meio dos métodos de passthrough desta classe.
     */
    private DialogueContext dialogueContext;

    /**
     * Informações de IoT do dispositivo
     */
    protected Map<String, IotDescriptor> iotDescriptors = new ConcurrentHashMap<>();

    /**
     * Máquina de estados do dispositivo no lado do servidor.
     * Substitui os antigos campos booleanos dispersos playing / musicPlaying / streamingState / inWakeupResponse.
     * Somente o estado IDLE permite disparar o timeout de inatividade.
     */
    private volatile DeviceState deviceState = DeviceState.IDLE;

    /**
     * Método de transição de estado
     * Inclui validação da transição de estado e log
     */
    public void transitionTo(DeviceState newState) {
        if (newState == null) {
            return;
        }
        DeviceState oldState = this.deviceState;
        if (oldState == newState) {
            return;
        }
        this.deviceState = newState;
        log.debug("Transição de estado: {} -> {} (SessionId: {})", oldState, newState, sessionId);
    }

    /**
     * Estado do dispositivo (auto, realTime)
     */
    protected ListenMode mode;
    /**
     * Fluxo de dados de áudio da sessão.
     * Mantido em ChatSession e não em Persona: audioSinks é o buffer de entrada de áudio orientado pelo VAD,
     * seu ciclo de vida está vinculado ao "início e fim da fala do usuário" (o produtor é DialogueService/VAD, o consumidor é o STT),
     * é uma preocupação da camada de transporte, com ciclo de vida diferente do Persona (runtime das capacidades de IA).
     * Movê-lo para Persona aumentaria a complexidade das verificações de null sem trazer benefício.
     */
    protected volatile Sinks.Many<byte[]> audioSinks;
    /**
     * Horário da última atividade válida da sessão
     */
    protected volatile Instant lastActivityTime;

    // ========== Métodos de passthrough da camada de diálogo (delegados internamente a dialogueContext; transparentes para quem os chama de fora) ==========

    public Persona getPersona()                 { return dialogueContext.getPersona(); }
    public void setPersona(Persona persona)     { dialogueContext.setPersona(persona); }

    public Player getPlayer()                   { return dialogueContext.getPlayer(); }
    public void setPlayer(Player player)        { dialogueContext.setPlayer(player); }

    public Path getUserAudioPath()              { return dialogueContext.getUserAudioPath(); }
    public void setUserAudioPath(Path path)     { dialogueContext.setUserAudioPath(path); }

    public ToolsSessionHolder getToolsSessionHolder()                          { return dialogueContext.getToolsSessionHolder(); }
    public void setToolsSessionHolder(ToolsSessionHolder h)                    { dialogueContext.setToolsSessionHolder(h); }
    public List<ToolCallback> getToolCallbacks()                               { return dialogueContext.getToolCallbacks(); }
    public void addToolCallDetail(String name, String args, String result)     { dialogueContext.addToolCallDetail(name, args, result); }
    public List<DialogueContext.ToolCallInfo> drainToolCallDetails()           { return dialogueContext.drainToolCallDetails(); }
    public boolean isFunctionCalled()                                          { return dialogueContext.isFunctionCalled(); }

    // ========== Marcador de desconexão por timeout ==========
    private volatile boolean timeoutDisconnect;

    // --------------------MCP do dispositivo-------------------------
    private DeviceMcpHolder deviceMcpHolder = new DeviceMcpHolder();

    public ChatSession(String sessionId) {
        this.sessionId = sessionId;
        this.lastActivityTime = Instant.now();
        this.dialogueContext = new DialogueContext();
    }

    public void clearAudioSinks(){
        // Limpa o fluxo de áudio
        Sinks.Many<byte[]> sink = getAudioSinks();
        if (sink != null) {
            sink.tryEmitComplete();
        }
        // Reseta o estado da sessão
        deviceState = DeviceState.IDLE;
        setAudioSinks(null);
    }

    // ========== Métodos de gerenciamento do fluxo de áudio (migrados de SessionManager) ==========

    /**
     * Cria um novo fluxo de dados de áudio
     */
    public void createAudioStream() {
        this.audioSinks = Sinks.many().multicast().onBackpressureBuffer();
    }

    /**
     * Envia dados de áudio para o fluxo
     */
    public void sendAudioData(byte[] data) {
        Sinks.Many<byte[]> sink = audioSinks; // variável local para evitar TOCTOU
        if (sink != null) {
            sink.tryEmitNext(data);
        }
    }

    /**
     * Finaliza o fluxo de áudio (notifica os consumidores de que o envio de dados terminou)
     */
    public void completeAudioStream() {
        if (audioSinks != null) {
            audioSinks.tryEmitComplete();
        }
    }

    /**
     * Fecha o fluxo de áudio (libera a referência)
     */
    public void closeAudioStream() {
        this.audioSinks = null;
    }

    /**
     * Convenção de caminho dos arquivos de áudio: audio/{date}/{device-id}/{role-id}/{timestamp}-{who}.wav|ogg
     * Organizado em diretórios por data, facilitando a limpeza em lote de dados expirados (basta excluir o diretório da data inteira)
     *
     * @param who
     * @param instant
     * @return
     */
    public Path getAudioPath(String who, Instant instant) {

        instant = instant.truncatedTo(ChronoUnit.SECONDS);

        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        String date = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String datetime = localDateTime.format(DateTimeFormatter.ISO_DATE_TIME).replace(":", "");
        DeviceBO device = this.getDevice();
        // Verifica se o ID do dispositivo contém caracteres especiais impróprios para um caminho; provavelmente é um endereço MAC que precisa ser convertido.
        String deviceId = device.getDeviceId().replace(":", "-");
        String roleId = device.getRoleId().toString();
        String extension = MessageBO.SENDER_USER.equals(who) ? "wav" : "ogg";
        String filename = "%s-%s.%s".formatted(datetime, who, extension);
        return Path.of(AudioUtils.AUDIO_PATH, date, deviceId, roleId, filename);
    }

    /**
     * Se a conexão da sessão está aberta
     *
     * @return
     */
    public abstract boolean isOpen();

    /**
     * Se o canal de áudio está aberto e disponível
     *
     * @return
     */
    public abstract boolean isAudioChannelOpen();

    public abstract void close();

    public abstract void sendTextMessage(String message);

    public abstract void sendBinaryMessage(byte[] message);

    public boolean isTimeoutDisconnect()            { return timeoutDisconnect; }
    public void setTimeoutDisconnect(boolean flag)  { this.timeoutDisconnect = flag; }

    /**
     * Plataforma envia helloMessage de forma proativa
     * Geralmente usado para ativação da sessão
     */
    public void sendHelloMessage() {}
}
