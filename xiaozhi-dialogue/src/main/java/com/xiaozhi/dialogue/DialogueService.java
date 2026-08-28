package com.xiaozhi.dialogue;

import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.communication.common.SessionManager;
import com.xiaozhi.communication.message.MessageSender;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.MessageBO;
import com.xiaozhi.dialogue.audio.VadService;
import com.xiaozhi.dialogue.llm.factory.PersonaFactory;
import com.xiaozhi.ai.llm.memory.MessageTimeMetadata;
import com.xiaozhi.ai.llm.service.IntentService;
import com.xiaozhi.ai.stt.SttResult;
import com.xiaozhi.common.model.bo.MessageMetadataBO;
import org.springframework.ai.chat.messages.UserMessage;
import com.xiaozhi.dialogue.audio.VadService.VadStatus;
import com.xiaozhi.dialogue.playback.Player;
import com.xiaozhi.dialogue.runtime.Persona;
import com.xiaozhi.enums.DeviceState;
import com.xiaozhi.event.ChatAbortedEvent;
import com.xiaozhi.event.SpeechRecognizedEvent;

import com.xiaozhi.storage.service.StorageServiceFactory;
import com.xiaozhi.utils.AudioUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
/**
 * Serviço de processamento de diálogo
 * Responsável pela lógica de negócio do reconhecimento de voz e da geração do diálogo
 * A lógica principal do diálogo foi delegada ao Persona; o DialogueService é responsável principalmente por:
 * 1. Recepção de dados de áudio e processamento de VAD
 * 2. Início do reconhecimento em streaming (STT) e gerenciamento do fluxo de áudio
 * 3. Tratamento da palavra de ativação
 * 4. Interrupção do diálogo (abort)
 * 5. Registro de dados de monitoramento
 */
@Slf4j
@Service
public class DialogueService{
    private static final String ABORT_REASON_VAD = "vad detectado";

    @Resource
    private PersonaFactory personaFactory;

    @Resource
    private MessageSender messageService;

    @Resource
    private VadService vadService;

    @Resource
    private SessionManager sessionManager;

    @Resource
    private IntentService intentService;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Resource
    private StorageServiceFactory storageServiceFactory;

    @org.springframework.context.event.EventListener
    public void onApplicationEvent(ChatAbortedEvent event) {
        ChatSession chatSession = sessionManager.getSession(event.getSessionId());
        if (chatSession == null) return;
        abortDialogue(chatSession, event.getReason());
    }

    /**
     * Trata os dados de áudio
     */
    public void processAudioData(ChatSession session, byte[] opusData) {
        if (session == null || opusData == null || opusData.length == 0) {
            return;
        }
        String sessionId = session.getSessionId();

        try {
            // Se o player estiver executando um callback subsequente (como a reprodução da mensagem de despedida), ignora os dados de áudio
            Player player = session.getPlayer();
            if (player != null && player.getFunctionAfterChat() != null) {
                return;
            }

            DeviceBO device = session.getDevice();
            // Se o dispositivo não estiver registrado ou vinculado, ignora os dados de áudio
            if (device == null || ObjectUtils.isEmpty(device.getRoleId())) {
                return;
            }

            // Processa o VAD
            VadService.VadResult vadResult = vadService.processAudio(sessionId, opusData);
            if (vadResult == null || vadResult.getStatus() == VadStatus.ERROR
                    || vadResult.getProcessedData() == null) {
                return;
            }

            // Atividade de voz detectada, atualizando o horário da última atividade
            sessionManager.updateLastActivity(sessionId);
            // Trata de acordo com o estado do VAD
            switch (vadResult.getStatus()) {
                case SPEECH_START:
                    // Inicia o STT primeiro (cria o fluxo de áudio de forma síncrona), garantindo que o fluxo já esteja pronto
                    startStt(session, sessionId, vadResult.getProcessedData());
                    // Em seguida, dispara o abort para parar o TTS em reprodução
                    // Usa Persona.isActive() para avaliar de forma abrangente se todo o pipeline está ativo (qualquer uma das camadas LLM/TTS/Player)
                    Persona persona = session.getPersona();
                    if (persona != null && persona.isActive()) {
                        abortDialogue(session, ABORT_REASON_VAD);
                    }
                    break;

                case SPEECH_CONTINUE:
                    // Voz continua, enviando dados para o reconhecimento em streaming
                    if (session.getDeviceState() == DeviceState.LISTENING) {
                        session.sendAudioData(vadResult.getProcessedData());
                    }
                    break;

                case SPEECH_END:
                    // Voz encerrada, finalizando o reconhecimento em streaming; estado alterado para THINKING aguardando a resposta do LLM
                    if (session.getDeviceState() == DeviceState.LISTENING) {
                        session.completeAudioStream();
                        session.transitionTo(DeviceState.THINKING);
                    }
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            log.error("Falha ao processar os dados de áudio: {}", e.getMessage(), e);
        }
    }

    /**
     * Inicia o reconhecimento de voz
     * Cria o fluxo de áudio de forma síncrona (evitando condição de corrida) e então executa o STT e o processamento subsequente em uma virtual thread
     */
    private void startStt(
            ChatSession session,
            String sessionId,
            byte[] initialAudio) {
        Assert.notNull(session, "session não pode ser nulo");

        // Parte síncrona: cria o fluxo de áudio e define o estado primeiro, evitando condição de corrida
        // Isso garante que o SPEECH_CONTINUE subsequente consiga enviar os dados corretamente
        session.closeAudioStream();
        session.createAudioStream();
        session.transitionTo(DeviceState.LISTENING);

        Thread.startVirtualThread(() -> {
            try {
                // Envia os dados de áudio iniciais
                if (initialAudio != null && initialAudio.length > 0) {
                    session.sendAudioData(initialAudio);
                }

                if (session.getAudioSinks() == null) {
                    return;
                }

                Persona persona = session.getPersona();
                if (persona == null || persona.getSttService() == null) {
                    return;
                }

                var sttResult = persona.getSttService().stream(session.getAudioSinks().asFlux());

                if (sttResult == null || !StringUtils.hasText(sttResult.text())) {
                    return;
                }

                // Envia o resultado do reconhecimento STT ao dispositivo
                persona.getPlayer().sendStt(sttResult.text());

                // Publica o evento de conclusão do reconhecimento de voz
                eventPublisher.publishEvent(new SpeechRecognizedEvent(this, sessionId, sttResult.text(),
                        sttResult.hasEmotion() ? sttResult.emotion() : null));

                // Salvamento de áudio
                Instant userInstant = Instant.now();
                Path userAudioPath = session.getAudioPath(MessageBO.SENDER_USER, userInstant);
                session.setUserAudioPath(userAudioPath);
                saveUserAudio(session, userAudioPath);

                handleText(session, sttResult);

            } catch (Exception e) {
                log.error("Erro no reconhecimento em streaming: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Trata a ativação por voz
     */
    public void handleWakeWord(ChatSession session, String text) {
        log.info("Palavra de ativação detectada: {}", text);
        try {
            // Define o estado como SPEAKING, ignorando a detecção de VAD durante a resposta de ativação
            session.transitionTo(DeviceState.SPEAKING);

            DeviceBO device = session.getDevice();
            if (device == null) {
                return;
            }

            personaFactory.buildPersona(session).chat(text, false);
        } catch (Exception e) {
            log.error("Falha ao tratar a palavra de ativação: {}", e.getMessage(), e);
        }
    }

    /**
     * Ponto de entrada unificado para o processamento de texto: rótulo de emoção → detecção de intenção → LLM+TTS
     *
     * @param session sessão atual
     * @param sttResult resultado do STT (texto puro é encapsulado com SttResult.textOnly())
     */
    public void handleText(ChatSession session, SttResult sttResult) {
        try {
            Persona persona = session.getPersona();

            String text = sttResult.text();

            UserMessage userMessage = buildUserMessage(text, sttResult);

            // Detecção de intenção
            if (intentService.detect(text) == IntentService.Intent.EXIT) {
                sendGoodbyeMessage(session);
                return;
            }

            // LLM+TTS
            try {
                persona.chat(userMessage, true);
            } catch (Exception e) {
                log.error("Falha no processamento do diálogo com o LLM: {}", e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("Falha ao processar o texto: {}", e.getMessage(), e);
        }
    }

    /**
     * Constrói um UserMessage com metadados estruturados e timestamp.
     * Os metadados não são concatenados como prefixo no text; em vez disso, passam pelo Map UserMessage.metadata,
     * que é montado de forma unificada por {@code UserMessageAssembler#assemble(Message)} antes de ser enviado ao LLM.
     *
     * @param text     texto puro do usuário
     * @param sttResult resultado do STT, pode conter informações de emoção
     */
    private static UserMessage buildUserMessage(String text, SttResult sttResult) {
        MessageMetadataBO metadataBO = MessageMetadataBO.builder()
                .emotion(sttResult.hasEmotion() ? sttResult.emotion() : null)
                .emotionScore(sttResult.hasEmotion() ? sttResult.emotionScore() : null)
                .emotionDegree(sttResult.hasEmotion() ? sttResult.emotionDegree() : null)
                .build();
        Map<String, Object> msgMeta = new HashMap<>();
        // Anexa se qualquer campo tiver valor; se todos estiverem vazios, não anexa, mantendo o UserMessage.metadata limpo
        if (StringUtils.hasText(metadataBO.getEmotion())) {
            msgMeta.put(MessageMetadataBO.METADATA_KEY, metadataBO);
        }
        UserMessage userMessage = UserMessage.builder().text(text).metadata(msgMeta).build();
        // Timestamp da mensagem (usado pela camada de projeção para montar o prefixo [yyyy-MM-ddTHH:mm:ss])
        MessageTimeMetadata.setTimeMillis(userMessage, Instant.now());
        return userMessage;
    }

    /**
     * Envia a mensagem de despedida e fecha a sessão após a reprodução terminar
     * Delega o fluxo de despedida ao Persona
     *
     * @param session sessão WebSocket
     */
    public void sendGoodbyeMessage(ChatSession session) {
        if (session == null || !session.isAudioChannelOpen()) {
            return;
        }
        Persona persona = session.getPersona();
        if (persona != null) {
            persona.sendGoodbyeMessage();
        } else {
            session.close();
        }
    }

    /**
     * Interrompe o diálogo atual
     * Cancela primeiro a assinatura do Flux upstream do Synthesizer, depois para o Player.
     * Se o Synthesizer não for cancelado primeiro, o SentenceHelper continuará segmentando frases e chamando player.play(newFlux),
     * causando sobreposição de áudio ou a chegada de novo áudio depois que a reprodução já foi limpa.
     */
    public void abortDialogue(ChatSession session, String reason) {
        try {
            String sessionId = session.getSessionId();
            log.info("Interrompendo o diálogo - SessionId: {}, Reason: {}", sessionId, reason);

            // Fecha o fluxo de áudio
            // Atenção: quando o reason é "vad detectado", não fecha o fluxo de áudio nem reseta o estado
            // porque isso significa que o usuário interrompeu o TTS para continuar falando; o startStt já criou um novo fluxo de áudio e definiu o estado como LISTENING
            if (!ABORT_REASON_VAD.equals(reason)) {
                session.closeAudioStream();
                // Após o abort, o servidor envia tts stop, o dispositivo volta a escutar e o servidor sincroniza o estado para LISTENING
                session.transitionTo(DeviceState.LISTENING);
            }

            // Cancela primeiro a assinatura do Flux upstream do sintetizador de voz, interrompendo a geração de novos dados de áudio
            Persona persona = session.getPersona();
            if (persona != null && persona.getSynthesizer() != null) {
                persona.getSynthesizer().cancel();
            }

            // Em seguida, encerra a reprodução de áudio, limpando a fila de reprodução
            Player player = session.getPlayer();
            if(player!=null){
                player.stop();
            }

            // Independentemente de o player existir ou não, é necessário enviar a mensagem stop para notificar o dispositivo a entrar em estado de escuta
            // Isso ocorre porque o dispositivo pode ter enviado a mensagem abort antes mesmo de o player ser criado
            messageService.sendTtsMessage(session, null, "stop");

            // Se for interrompido durante o fluxo de goodbye (functionAfterChat já definido),
            // é necessário executar o callback de limpeza (fechar a session etc.) e removê-lo para evitar execução duplicada
            if (player != null) {
                Runnable afterChat = player.getFunctionAfterChat();
                if (afterChat != null) {
                    player.setFunctionAfterChat(null);
                    afterChat.run();
                }
            }
        } catch (Exception e) {
            log.error("Falha ao interromper o diálogo: {}", e.getMessage(), e);
        }
    }

    /**
     * Salva os dados de áudio do usuário como um arquivo WAV
     */
    private void saveUserAudio(ChatSession session, Path path) {
        List<byte[]> pcmFrames = vadService.getPcmData(session.getSessionId());
        byte[] fullPcmData = AudioUtils.joinPcmFrames(pcmFrames);
        if (fullPcmData.length == 0) {
            return;
        }
        AudioUtils.saveAsWav(path, fullPcmData);
        log.debug("Áudio do usuário salvo: {}", path);

        try {
            String storedPath = storageServiceFactory.getStorageService().upload(path, path.toString());
            session.setUserAudioPath(Path.of(storedPath));
        } catch (Exception e) {
            log.warn("Falha ao enviar o áudio do usuário, mantendo o caminho local: {}", path, e);
        }
    }

}
