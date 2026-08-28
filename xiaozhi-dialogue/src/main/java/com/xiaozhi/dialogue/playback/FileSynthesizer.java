package com.xiaozhi.dialogue.playback;

import com.xiaozhi.common.Speech;

import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.ai.tts.SentenceHelper;
import com.xiaozhi.ai.tts.TtsService;
import com.xiaozhi.utils.AudioUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
/**
 * Sintetizador de voz para TTS não streaming (gera o arquivo de áudio completo antes de reproduzir).
 * Adequado para Providers de TTS que não suportam saída em streaming (como o SherpaOnnx).
 *
 * Fluxo de dados: fluxo de tokens do LLM → segmentação em frases pelo SentenceHelper → chamada ao TTS frase a frase gerando o arquivo de áudio completo → leitura do PCM → entrega ao player para reprodução
 */
@Slf4j
public class FileSynthesizer extends Synthesizer {

    // Mantém a referência da assinatura do fluxo de saída do LLM, para poder cancelar a assinatura upstream em cancel
    private volatile Disposable llmDisposable;

    public FileSynthesizer(ChatSession session, TtsService ttsService, Player player) {
        super(session, ttsService, player);
    }

    @Override
    public void cancel() {
        if (llmDisposable != null && !llmDisposable.isDisposed()) {
            llmDisposable.dispose();
        }
    }

    @Override
    public boolean isActive() {
        return llmDisposable != null && !llmDisposable.isDisposed();
    }

    /**
     * Converte o fluxo de tokens gerado pelo LLM em voz e o envia ao player.
     * Usa o SentenceHelper para segmentar por pontuação e, frase a frase, chama o TTS para gerar o arquivo de áudio completo antes de entregá-lo ao player.
     *
     * @param stringFlux fluxo de tokens gerado pelo LLM
     */
    @Override
    public void synthesize(Flux<String> stringFlux) {
        llmDisposable = new SentenceHelper().convert(stringFlux).subscribe(result -> {
            String text = result.text();
            String mood = result.mood();
            Flux<Speech> lazyTtsFlux = Flux.create(sink -> {
                try {
                    Path audioPath = ttsService.textToSpeech(text);
                    if (audioPath != null) {
                        List<byte[]> chunks = AudioUtils.readAsPcmChunks(audioPath.toString());
                        boolean first = true;
                        for (byte[] chunk : chunks) {
                            sink.next(first ? new Speech(chunk, text).withMood(mood) : new Speech(chunk));
                            first = false;
                        }
                    } else {
                        log.error("O serviço de TTS retornou um arquivo de áudio vazio - SessionId: {}", chatSession.getSessionId());
                    }
                } catch (Exception e) {
                    log.error("Erro na síntese de TTS: {} - SessionId: {}", e.getMessage(), chatSession.getSessionId());
                }
                sink.complete();
            });
            player.play(lazyTtsFlux);
        });
    }

    /**
     * Sintetiza diretamente um único texto
     * @param text texto a ser sintetizado
     */
    @Override
    public void synthesize(String text) {
        // Delega para synthesize(Flux), onde as métricas de cache são registradas de forma unificada
        synthesize(Flux.just(text));
    }

}
