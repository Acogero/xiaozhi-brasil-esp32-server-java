package com.xiaozhi.dialogue.audio;

import com.xiaozhi.event.TtsPlaybackCompletedEvent;
import com.xiaozhi.utils.OpusProcessor;

import dev.onvoid.webrtc.media.audio.AudioProcessing;
import dev.onvoid.webrtc.media.audio.AudioProcessingConfig;
import dev.onvoid.webrtc.media.audio.AudioProcessingStreamConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
/**
 * Serviço de AEC (cancelamento de eco) do lado do servidor.
 * Usa o WebRTC AEC3 no servidor para eliminar o eco do alto-falante captado pelo microfone,
 * permitindo que dispositivos sem AEC de hardware também consigam interromper e dialogar normalmente.
 *
 * Design principal:
 * - Após feedReference() decodificar o frame Opus de referência, chama processReverseStream imediatamente, subframe a subframe,
 *   conduzindo o canal de referência do AEC3 no ritmo em tempo real do envio do TTS, sem acumular fila de buffer.
 * - process() chama processStream subframe a subframe, no ritmo em tempo real de chegada do microfone.
 * - Ambos conduzem o AEC3 diretamente, mantendo suas próprias linhas do tempo em tempo real; o estimador de atraso embutido no AEC3
 *   encontra automaticamente o atraso entre o sinal de referência e o eco, sem necessidade de alinhamento manual.
 * - setStreamDelayMs serve apenas como dica inicial para acelerar a convergência.
 */
@Slf4j
@Service
public class AecService {
    @Value("${aec.enabled:true}")
    private boolean enabled;

    @Value("${aec.stream.delay.ms:120}")
    private int streamDelayMs;

    @Value("${aec.noise.suppression.level:MODERATE}")
    private String noiseSuppressionLevel;

    // Estado de AEC por sessão
    private final ConcurrentHashMap<String, AecState> states = new ConcurrentHashMap<>();

    // Parâmetros de frame de 10ms (16kHz mono, 16 bits)
    private static final int FRAME_BYTES_10MS = 320;      // bytes

    /**
     * Garante que o estado de AEC da sessão já esteja inicializado.
     * Se já existir, reutiliza (preservando o estado do filtro já convergido); só cria um novo se não existir.
     */
    public void initSession(String sessionId) {
        if (!enabled) return;
        if (states.containsKey(sessionId)) return;
        try {
            AecState existing = states.putIfAbsent(sessionId, new AecState());
            if (existing == null) {
            }
        } catch (Exception e) {
            log.error("Falha ao inicializar a sessão de AEC: {}", sessionId, e);
        }
    }

    /**
     * Reseta (destrói) o estado de AEC da sessão
     */
    public void resetSession(String sessionId) {
        AecState state = states.remove(sessionId);
        if (state != null) {
            // Faz o dispose dentro do apmLock, garantindo que aguarde a conclusão de processStream/processReverseStream em andamento
            synchronized (state.apmLock) {
                state.dispose();
            }
        }
    }

    /**
     * Reconstrói a instância de AEC quando a reprodução do TTS termina.
     * Após o TTS parar, o AEC3 ainda mantém o filtro de eco antigo, o que pode tratar a fala do usuário como eco e removê-la indevidamente (cancelamento excessivo).
     * Reconstruir a instância do APM limpa o filtro antigo, evitando o cancelamento indevido da voz do usuário.
     */
    @EventListener
    public void onTtsPlaybackEnd(TtsPlaybackCompletedEvent event) {
        if (!enabled) return;
        String sessionId = event.getSessionId();
        AecState old = states.get(sessionId);
        if (old == null) return;
        try {
            AecState fresh = new AecState();
            // Substituição atômica: substitui a instância antiga pela nova
            if (states.replace(sessionId, old, fresh)) {
                // Faz o dispose dentro do apmLock, garantindo que aguarde a conclusão de processStream/processReverseStream em andamento
                synchronized (old.apmLock) {
                    old.dispose();
                }
            } else {
                // Concorrência: a nova instância foi substituída antes por outra thread; libera a que acabou de ser criada
                synchronized (fresh.apmLock) {
                    fresh.dispose();
                }
            }
        } catch (Exception e) {
            log.warn("Falha ao reconstruir o AEC: {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Alimenta o sinal de referência (frame Opus enviado do TTS para o dispositivo).
     * Após a decodificação, chama processReverseStream imediatamente, subframe a subframe, conduzindo o AEC3 no ritmo em tempo real do envio do TTS.
     * Sem fila de buffer — o acúmulo em fila causaria desalinhamento entre a linha do tempo do frame de referência e do frame do microfone, impedindo o AEC3 de alinhar corretamente.
     */
    public void feedReference(String sessionId, byte[] opusFrame) {
        if (!enabled) return;
        AecState state = states.get(sessionId);
        if (state == null) return;

        try {
            // Decodifica o frame Opus de referência com um decodificador independente
            byte[] pcm = state.refDecoder.opusToPcm(opusFrame);
            if (pcm == null || pcm.length == 0) return;

            // Chama processReverseStream imediatamente, subframe a subframe, conduzindo o canal de referência no ritmo em tempo real do TTS
            synchronized (state.apmLock) {
                if (state.disposed) return;
                int offset = 0;
                while (offset + FRAME_BYTES_10MS <= pcm.length) {
                    byte[] subFrame = new byte[FRAME_BYTES_10MS];
                    System.arraycopy(pcm, offset, subFrame, 0, FRAME_BYTES_10MS);
                    byte[] refOutput = new byte[FRAME_BYTES_10MS];
                    state.apm.processReverseStream(subFrame, state.streamConfig, state.streamConfig, refOutput);
                    offset += FRAME_BYTES_10MS;
                }
            }

        } catch (Exception e) {
            log.warn("Falha no feedReference do AEC - SessionId: {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Processa os dados PCM do microfone, eliminando o eco.
     * Chama processStream subframe a subframe, no ritmo em tempo real de chegada do microfone.
     * O estimador de atraso interno do AEC3 alinha automaticamente o canal de referência com o canal do microfone.
     */
    public byte[] process(String sessionId, byte[] micPcm) {
        if (!enabled) return micPcm;
        AecState state = states.get(sessionId);
        if (state == null) return micPcm;

        try {
            int totalBytes = micPcm.length;
            byte[] aecOutput = new byte[totalBytes];
            int offset = 0;
            int outOffset = 0;

            synchronized (state.apmLock) {
                if (state.disposed) return micPcm;
                while (offset + FRAME_BYTES_10MS <= totalBytes) {
                    byte[] micSubFrame = new byte[FRAME_BYTES_10MS];
                    System.arraycopy(micPcm, offset, micSubFrame, 0, FRAME_BYTES_10MS);
                    byte[] outputFrame = new byte[FRAME_BYTES_10MS];
                    state.apm.processStream(micSubFrame, state.streamConfig, state.streamConfig, outputFrame);
                    System.arraycopy(outputFrame, 0, aecOutput, outOffset, FRAME_BYTES_10MS);
                    offset += FRAME_BYTES_10MS;
                    outOffset += FRAME_BYTES_10MS;
                }
            }

            // Trata os dados residuais com menos de 10ms
            if (offset < totalBytes) {
                System.arraycopy(micPcm, offset, aecOutput, outOffset, totalBytes - offset);
            }

            return aecOutput;
        } catch (Exception e) {
            log.warn("Falha no process do AEC - SessionId: {}: {}", sessionId, e.getMessage());
            return micPcm;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Estado de AEC por sessão.
     */
    private class AecState {
        final AudioProcessing apm;
        final OpusProcessor refDecoder;
        final AudioProcessingStreamConfig streamConfig;
        final Object apmLock = new Object();  // feedReference e process compartilham o mesmo lock, garantindo a segurança de thread nas chamadas ao APM
        volatile boolean disposed = false;     // sinalizador de dispose, definido e verificado dentro do apmLock

        AecState() {
            apm = new AudioProcessing();

            AudioProcessingConfig config = new AudioProcessingConfig();
            config.echoCanceller.enabled = true;
            config.echoCanceller.enforceHighPassFiltering = false;

            // Redução de ruído: nível configurável
            AudioProcessingConfig.NoiseSuppression.Level nsLevel;
            try {
                nsLevel = AudioProcessingConfig.NoiseSuppression.Level.valueOf(noiseSuppressionLevel.toUpperCase());
            } catch (Exception e) {
                nsLevel = AudioProcessingConfig.NoiseSuppression.Level.LOW;
            }
            config.noiseSuppression.enabled = true;
            config.noiseSuppression.level = nsLevel;

            config.highPassFilter.enabled = true;

            // Controle de ganho adaptativo (AGC): substitui o ganho fixo + compressão do AudioEnhancer,
            // atuando em conjunto com o AEC/redução de ruído na mesma cadeia de processamento, sem amplificar o eco residual
            config.gainControl.enabled = true;
            config.gainControl.adaptiveDigital.enabled = true;

            apm.applyConfig(config);

            // Define a dica de atraso inicial, ajudando o AEC3 a acelerar a convergência (o estimador de atraso embutido no AEC3 ajusta automaticamente)
            apm.setStreamDelayMs(streamDelayMs);

            refDecoder = new OpusProcessor();
            streamConfig = new AudioProcessingStreamConfig(16000, 1);
        }

        void dispose() {
            disposed = true;
            try {
                apm.dispose();
            } catch (Exception e) {
                log.warn("Falha no dispose do AEC: {}", e.getMessage());
            }
        }
    }
}
