package com.xiaozhi.dialogue.playback;

import com.xiaozhi.common.Speech;
import com.xiaozhi.utils.EmojiUtils;

import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.communication.message.MessageSender;
import com.xiaozhi.utils.AudioUtils;
import io.jsonwebtoken.lang.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.slf4j.Slf4j;
/**
 * Player de fluxo de áudio baseado em virtual threads.
 *
 * Características principais:
 * 1. Virtual threads: cada player tem sua própria virtual thread, suportando concorrência ilimitada
 * 2. Modo Burst: pré-buffer dos 2 primeiros frames (-120ms), evitando distorção/perda de palavras no primeiro frame
 * 3. Agendamento preciso: controle de tempo em nanossegundos, garantindo um intervalo exato de 60ms
 * 4. Tempo absoluto: agendamento baseado no tempo absoluto a partir de startTimestamp, evitando erro acumulado
 *
 * Princípio do modo Burst:
 * - playPosition inicia em -120ms (2 frames)
 * - Os 2 primeiros frames são enviados imediatamente (targetSendTime < currentTime, passa direto)
 * - A partir do 3º frame, o agendamento segue o tempo exato
 * - Efeito: o dispositivo recebe os 2 primeiros frames e começa a reproduzir imediatamente, sem distorção por espera de dados
 */
@Slf4j
public class ScheduledPlayer extends Player {
    // Intervalo de envio do frame Opus: 60ms = 60.000.000 nanossegundos
    private static final long OPUS_FRAME_SEND_INTERVAL_NS = AudioUtils.OPUS_FRAME_DURATION_MS * 1_000_000L;

    // Modo Burst: pré-buffer dos 2 primeiros frames, evitando distorção no primeiro frame
    private static final long BURST_PREBUFFER_NS = -OPUS_FRAME_SEND_INTERVAL_NS * 2; // -120ms

    // Aguarda a conclusão da reprodução de todo o áudio no dispositivo terminal antes de enviar a mensagem de fim do TTS
    private static final long WAIT_TIME_MS_TO_SEND_STOP = 120;

    // Intervalo entre frases: compensa pré-buffer (2 frames) + primeiro frame após o pré-buffer (1 frame) + último frame (1 frame) + intervalo entre frases (1 frame) = 5 frames = 300ms
    // Isso evita que as frases se sobreponham, dando ao dispositivo tempo de buffer suficiente
    private static final long SENTENCE_GAP_NS = OPUS_FRAME_SEND_INTERVAL_NS * 5;

    // Marcador de intervalo entre frases (frame vazio); ao encontrá-lo, a thread de envio pula o envio e aumenta o intervalo de playPosition
    private static final Speech SENTENCE_GAP_MARKER = new Speech(new byte[0]);

    // Estado do modo Burst
    private long startTimestamp = 0;  // Timestamp absoluto do início da reprodução (nanossegundos)
    private long playPosition = BURST_PREBUFFER_NS;  // Posição de reprodução atual (nanossegundos), inicia em -120ms para implementar o pré-buffer

    // Fila de frames de áudio
    private Queue<Speech> allOpusFrames = new ConcurrentLinkedQueue<>();

    // Fila de Flux (usada para enfileirar múltiplas tarefas de TTS)
    private Queue<Flux<Speech>> fluxQueue = new ConcurrentLinkedQueue<>();

    // Flux atualmente assinado
    private AtomicReference<Disposable> fluxDisposable = new AtomicReference<>(null);

    // Controle da virtual thread
    private volatile boolean running = false;
    private Thread senderThread;

    public ScheduledPlayer(ChatSession session, MessageSender messageService) {
        super(session, messageService);
    }

    /**
     * Reproduz o fluxo de áudio
     * @param speechFlux fluxo de áudio gerado pelo TTS
     */
    public void play(Flux<Speech> speechFlux) {
        Assert.notNull(speechFlux, "speechFlux não pode ser nulo");

        synchronized (fluxDisposable) {
            // Se não houver TTS em andamento no momento, assina diretamente
            if (fluxDisposable.get() == null) {
                subscribe(speechFlux);

                // Inicia a thread de envio (apenas uma vez)
                if (!running) {
                    running = true;
                    sendStart();

                    // Usa virtual threads, leves, permitindo criar milhares delas
                    senderThread = Thread.startVirtualThread(this::sendFramesLoop);
                }
            } else {
                // Já há um TTS em andamento; entra na fila de espera
                fluxQueue.offer(speechFlux);
            }
        }
    }

    /**
     * Assina o fluxo de áudio
     */
    private void subscribe(Flux<Speech> speechFlux) {
        Assert.notNull(speechFlux, "speechFlux não pode ser nulo");

        // Quando o primeiro bloco PCM de uma frase é pequeno demais, menor que um frame Opus, o texto fica temporariamente armazenado aqui e é anexado quando o próximo frame for gerado.
        // Usa variável local em vez de campo de classe: cada subscribe() é independente e é resetada automaticamente em subscribeNext(), evitando contaminação entre frases.
        AtomicReference<String> pendingText = new AtomicReference<>(null);

        // Usa boundedElastic em vez de single()
        // single() é uma thread única global; com múltiplos Players em concorrência, eles se bloqueiam serialmente entre si
        // boundedElastic fornece uma thread elástica independente para cada assinatura, adequada para cenários com bloqueio de I/O como o TTS
        Disposable disposable = speechFlux.subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                    speech -> {
                        // Atualiza o horário de atividade
                        session.setLastActivityTime(Instant.now());

                        // Frame Opus pré-codificado (lido diretamente do cache), entra na fila diretamente sem conversão
                        if (speech.isOpusEncoded()) {
                            allOpusFrames.add(speech);
                            return;
                        }

                        // Converte os dados PCM para o formato Opus
                        byte[] pcmData = speech.getOutput();
                        String text = speech.getText();

                        // O frame atual não tem texto; tenta recuperar o texto que não pôde ser anexado anteriormente por o PCM ser menor que um frame
                        if (!StringUtils.hasText(text)) {
                            text = pendingText.getAndSet(null);
                        }

                        List<byte[]> opusFrames = opusProcessor.pcmToOpus(pcmData, true);

                        if (!CollectionUtils.isEmpty(opusFrames)) {
                            // Cria a lista de Speech, com o texto anexado ao primeiro frame
                            List<Speech> speechList = opusFrames.stream()
                                    .map(Speech::new)
                                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

                            if (StringUtils.hasText(text)) {
                                // Substitui o primeiro frame por um Speech com texto
                                Speech firstSpeech = speechList.remove(0);
                                speechList.add(0, new Speech(firstSpeech.getOutput(), text));
                                pendingText.set(null);
                            }

                            allOpusFrames.addAll(speechList);
                        } else if (StringUtils.hasText(text)) {
                            // O PCM é menor que um frame Opus (já está no buffer interno do encoder); armazena o texto temporariamente aguardando o próximo frame
                            pendingText.set(text);
                        }
                    },
                    throwable -> {
                        log.error("Erro ao gerar o conteúdo de saída do modelo de TTS: {}", throwable.getMessage());
                        // O TTS atual lançou uma exceção; tenta assinar o próximo Flux
                        subscribeNext();
                    },
                    () -> {
                        // O Flux atual foi concluído; faz flush dos dados restantes
                        List<byte[]> opusFrames = opusProcessor.flushLeftover();
                        if (!CollectionUtils.isEmpty(opusFrames)) {
                            List<Speech> speechList = opusFrames.stream()
                                    .map(Speech::new)
                                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

                            // Se houver texto pendente (o primeiro frame da última frase era pequeno demais), anexa-o ao primeiro frame gerado pelo flush
                            String pt = pendingText.getAndSet(null);
                            if (pt != null) {
                                Speech firstSpeech = speechList.remove(0);
                                speechList.add(0, new Speech(firstSpeech.getOutput(), pt));
                            }

                            allOpusFrames.addAll(speechList);
                        }

                        // Adiciona o marcador de intervalo entre frases, evitando sobreposição
                        allOpusFrames.add(SENTENCE_GAP_MARKER);

                        // Tenta assinar o próximo Flux
                        subscribeNext();
                    }
                );

        fluxDisposable.set(disposable);
    }

    /**
     * Assina o próximo Flux da fila
     */
    private void subscribeNext() {
        synchronized (fluxDisposable) {
            Flux<Speech> nextFlux = fluxQueue.poll();
            if (nextFlux != null) {
                subscribe(nextFlux);
            } else {
                fluxDisposable.set(null);
            }
        }
    }

    /**
     * Loop de envio de frames de áudio (virtual thread)
     *
     * Utiliza o modo Burst + agendamento por tempo absoluto:
     * 1. Define startTimestamp no primeiro frame
     * 2. Calcula o tempo de envio alvo com base em playPosition
     * 3. playPosition inicia em -120ms; os 2 primeiros frames são enviados imediatamente (pré-buffer)
     * 4. Os frames seguintes são enviados com intervalo exato de 60ms
     */
    private void sendFramesLoop() {
        while (running) {
            Speech speech = allOpusFrames.poll();

            if (speech != null) {
                if (speech == SENTENCE_GAP_MARKER) {
                    // Intervalo entre frases: avança playPosition sem enviar áudio
                    playPosition += SENTENCE_GAP_NS;
                    continue;
                }
                // Há dados, envia o frame de áudio
                sendSpeechWithBurstMode(speech);
            } else {
                // Fila vazia, verifica se a reprodução terminou
                if (fluxDisposable.get() == null && !isToolCalling()) {
                    // Nenhum novo Flux gerando dados, preparando para encerrar
                    try {
                        Thread.sleep(WAIT_TIME_MS_TO_SEND_STOP);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    // Verifica novamente, garantindo que não há novos dados
                    if (allOpusFrames.isEmpty() && fluxDisposable.get() == null && !isToolCalling()) {
                        running = false;
                        // Reseta o estado do modo Burst, evitando que na próxima chamada de play() o startTimestamp antigo faça todos os frames serem enviados com atraso zero
                        startTimestamp = 0;
                        playPosition = BURST_PREBUFFER_NS;
                        sendStop();
                        break;
                    }
                } else {
                    // Ainda há Flux gerando dados, aguarda com uma breve pausa
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    /**
     * Envia um único Speech usando o modo Burst
     *
     * Sequência de tempo do modo Burst:
     * - 1º frame: playPosition = -120ms → envio imediato (pré-buffer)
     * - 2º frame: playPosition = -60ms  → envio imediato (pré-buffer)
     * - 3º frame: playPosition = 0ms    → envia após aguardar até startTimestamp
     * - 4º frame: playPosition = 60ms   → envia após aguardar até startTimestamp+60ms
     * - ...
     */
    private void sendSpeechWithBurstMode(Speech speech) {
        byte[] frame = speech.getOutput();

        // Atualiza o horário de atividade
        session.setLastActivityTime(Instant.now());

        // Envia o texto e a emoção (se houver)
        String text = speech.getText();
        if (StringUtils.hasText(text)) {
            String mood = speech.getMood();
            sendEmotion(StringUtils.hasText(mood) ? mood : EmojiUtils.getRandomEmotion());
            sendSentenceStart(text);
        }

        // Verifica o estado de reprodução
        if (!isPlaying()) {
            log.error("Estado do player inconsistente: envio de frame de áudio fora do estado Playing - SessionId: {}", session.getSessionId());
            sendStart();
        }

        // Define o timestamp de início (apenas no primeiro frame)
        if (startTimestamp == 0) {
            startTimestamp = System.nanoTime();
        }

        // Calcula o tempo de envio alvo (timestamp absoluto)
        // playPosition inicia em -120ms; os 2 primeiros frames passam imediatamente (targetSendTime < currentTime)
        long targetSendTime = startTimestamp + playPosition;

        // Aguarda até o tempo alvo
        long currentTime = System.nanoTime();
        long delay = targetSendTime - currentTime;

        if (delay > 0) {
            // É necessário aguardar
            try {
                long delayMs = delay / 1_000_000L;
                int delayNs = (int) (delay % 1_000_000L);
                Thread.sleep(delayMs, delayNs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
                return;
            }
        }
        // else: delay <= 0, envia imediatamente (fase de pré-buffer)

        // Envia o frame de áudio
        sendOpusFrame(frame);

        // Atualiza a posição de reprodução (aumenta 60ms a cada frame)
        playPosition += OPUS_FRAME_SEND_INTERVAL_NS;
    }

    /**
     * Para a reprodução
     */
    @Override
    public void stop() {
        super.stop();
        running = false;

        // Interrompe a thread de envio
        if (senderThread != null) {
            senderThread.interrupt();
        }

        // Limpa a fila
        fluxQueue.clear();
        allOpusFrames.clear();

        // Cancela a assinatura do Flux
        Disposable disposable = fluxDisposable.getAndSet(null);
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }

        // Reseta o estado do modo Burst
        startTimestamp = 0;
        playPosition = BURST_PREBUFFER_NS;

        // Fecha o arquivo ativamente ao interromper, evitando gerar um arquivo Opus corrompido
        if (getOpusRecorder() != null) {
            getOpusRecorder().closeOpusFile();
        }
    }

    /**
     * Verifica se o player está reproduzindo ou tem conteúdo aguardando reprodução
     * Usado na decisão de interrupção, evitando perder a interrupção durante a troca de frases
     *
     * @return true se estiver reproduzindo, houver dados na fila, houver um Flux gerando dados, ou houver um Flux aguardando reprodução
     */
    public boolean hasContent() {
        return isPlaying() || !fluxQueue.isEmpty() || !allOpusFrames.isEmpty() || fluxDisposable.get() != null;
    }
}
