package com.xiaozhi.dialogue.playback;

import com.xiaozhi.common.Speech;

import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.communication.message.MessageSender;
import com.xiaozhi.enums.DeviceState;
import com.xiaozhi.utils.AudioUtils;
import com.xiaozhi.utils.OpusProcessor;
import io.jsonwebtoken.lang.Assert;
import lombok.*;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;
/**
 *
 * Player, responsável por processar a reprodução de áudio (envio para o dispositivo terminal).
 * Seu ciclo de vida é aproximadamente equivalente ao do ChatSession; quando não há música ou livro ilustrado em reprodução, o player não precisa ser trocado.
 * O único cenário em que é necessário parar a reprodução ativamente é ao receber o evento Abort. Em todos os outros casos, a parada deve ser natural.
 * Quando é necessário interromper, este player é obtido a partir do ChatSession para interromper a reprodução e limpar os recursos na fila.
 * Também é necessário inserir uma reprodução ao dizer adeus ou quando uma chamada de ferramenta envia um aviso amigável.
 * No futuro, pode-se considerar o uso do padrão Composite para suportar mais tipos de formato de áudio a serem reproduzidos. O player deve seguir o formato combinado com o dispositivo terminal.
 * TODO Portanto, a direção de refatoração futura deve transformar este player em players específicos para diferentes formatos.
 *
 * setCloseAfterChat vem apenas de dois lugares,
 * @see com.xiaozhi.dialogue.llm.tool.function.SessionExitFunction
 * @see Persona#sendGoodbyeMessage()
 * Quando o SessionExitFunction está em execução, essa ferramenta não consegue encontrar o Player; mesmo dentro do ChatSession, a instância de Player pode ainda não ter sido inicializada.
 * O SessionExitFunction normalmente retorna um GoodbyeMessage ao DialogueService, que então trata a síntese de voz e a reprodução.
 * O método sendGoodbyeMessage é utilizado por checkInactiveSessions.
 *
 * @see com.xiaozhi.event.ChatAbortedEvent
 * O que o usuário realmente se importa é o intervalo de tempo entre terminar de falar e o início da reprodução, não o tempo de geração do TTS. Por isso o Player precisa ter um Instant.
 *
 * Pergunta: é necessário implementar a interface Runnable?
 * Resposta: nem todas as classes que implementam Player precisam implementar Runnable; também é possível usar ExecutorService / ScheduledExecutorService, ou agregar múltiplos Players (padrão Composite).
 *
 */
@Slf4j
@Data
public abstract class Player {
    // Por padrão, deve ser false. O estado muda conforme as mensagens enviadas ao dispositivo.
    private volatile boolean isPlaying = false;
    /**
     * Indica se uma chamada de ferramenta está em andamento no momento.
     * Durante a chamada de ferramenta (como tirar uma foto),
     * é necessário aguardar o retorno da ferramenta para que o LLM continue a gerar saída.
     */
    private volatile boolean toolCalling = false;
    /**
     * Callback executado após o envio da fala atual ser concluído (como fechar a session)
     */
    private Runnable functionAfterChat = null;
    protected final ChatSession session;
    protected final OpusProcessor opusProcessor = new OpusProcessor();
    private final MessageSender messageService;
    /**
     * Componente opcional de gravação Opus: grava simultaneamente em um arquivo OGG os frames Opus enviados pelo player.
     * Substitui, via composição, a herança original de PlayerWithOpusFile.
     */
    @Setter
    @Getter
    private OpusRecorder opusRecorder;

    /**
     * Construtor do player de áudio
     * @param session
     * @param messageService
     */
    protected Player(ChatSession session, MessageSender messageService) {
        Assert.notNull(session, "session não pode ser nulo");
        Assert.notNull(messageService, "messageService não pode ser nulo");
        this.session = session;
        this.messageService = messageService;
    }

    public void sendStt(String userText){
        messageService.sendSttMessage(session, userText);
    }

    /**
     * Envia a mensagem de início do TTS
     */
    protected void sendStart() {
        if (opusRecorder != null) {
            opusRecorder.onSendStart();
        }
        messageService.sendTtsMessage(session, null, "start");
        isPlaying = true;
        session.transitionTo(DeviceState.SPEAKING);
    }

    /**
     * Envia a mensagem de início de frase do TTS
     */
    protected void sendSentenceStart( String text) {
        messageService.sendTtsMessage(session, text, "sentence_start");
    }

    /**
     * Envia os dados do frame Opus
     */
    protected void sendOpusFrame( byte[] opusFrame)  {
        messageService.sendBinaryMessage(session, opusFrame);
        // log.info("Envia os dados do frame Opus: {}", opusFrame.length);
        if (opusRecorder != null) {
            opusRecorder.onSendOpusFrame(opusFrame);
        }
    }

    /**
     * Envia as informações de emoção. Se nenhuma emoção for identificada na frase, retorna happy por padrão
     */
    protected void sendEmotion( String emotion) {
        messageService.sendEmotion(session, emotion);
    }

    /**
     * Envia a mensagem de parada
     * Este método não é exposto externamente; somente o player pode iniciar a mensagem de parada. Externamente, a parada deve ocorrer via stop ou outra forma indireta.
     */
    protected void sendStop() {
        try {
            if (opusRecorder != null) {
                opusRecorder.onSendStop();
            }
            messageService.sendTtsMessage(session, null, "stop");
            isPlaying = false;
            // Após o envio de tts stop, o dispositivo muda para o estado de escuta e o servidor sincroniza para LISTENING
            session.transitionTo(DeviceState.LISTENING);
            // Verifica se é necessário executar uma ação subsequente (como fechar a sessão)
            if (functionAfterChat != null) {
                functionAfterChat.run();
            }
        } catch (Exception e) {
            // sendStop pode ser disparado devido à queda da conexão, então apenas registra a exceção sem relançá-la.
            log.error("Falha ao enviar a mensagem de parada", e);
        }
    }

    abstract public void play(Flux<Speech> speechFlux);

    public void play(Path audioPath) {
        play("",audioPath);
    }

    public void play(String text, Path audioPath) {

        File audioFile = audioPath.toFile();
        if (!audioFile.exists()) {
            log.error("Arquivo de áudio não encontrado: {}", audioPath);
            return;
        }
        // Lê o PCM em blocos, evitando carregar tudo na memória
        try {
            List<byte[]> chunks = AudioUtils.readAsPcmChunks(audioPath.toString());
            AtomicBoolean first = new AtomicBoolean(true);
            play(Flux.fromIterable(chunks)
                    .map(chunk -> first.compareAndSet(true, false) ? new Speech(chunk, text) : new Speech(chunk)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verifica se o player tem conteúdo em reprodução ou aguardando reprodução.
     * A implementação padrão da classe base equivale a isPlaying(); as subclasses podem sobrescrever para incluir verificações de estado como a fila.
     * Usado na decisão de interrupção; mais abrangente que isPlaying().
     */
    public boolean hasContent() {
        return isPlaying;
    }

    /**
     * Usado para limpar recursos em caso de interrupção ou quando o usuário interrompe.
     * Mas se este objeto precisa ser destruído depende de o player precisar ou não ser trocado.
     * Quando a fala termina naturalmente, o controle interno de sendStop é feito internamente, mas o método stop não pode ser chamado internamente.
     */
    public void stop() {
        isPlaying = false;
        // Subclasses (como ScheduledPlayer) sobrescrevem este método para uma limpeza mais detalhada
        log.info("Tarefa de envio de áudio cancelada - SessionId: {}", session.getSessionId());
    }

}
