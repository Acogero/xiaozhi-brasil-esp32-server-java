package com.xiaozhi.utils;

import io.github.jaredmdobson.concentus.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
/**
 * Processador de áudio Opus
 * Codificação e decodificação normalmente são dois processos distintos, mas podem compartilhar configurações básicas, como taxa de amostragem, número de canais e tamanho do frame.
 * Futuramente, se necessário otimizar, pode-se considerar dividir em três classes utilitárias.
 * Não há necessidade de gerenciar via Spring Context, nem de ser um @Component. Como ferramenta de processo, é descartada após o uso.
 * Classes utilitárias comuns (ou instâncias delas) não precisam ser objetos de vida longa.
 */
@Slf4j
public class OpusProcessor {
    // Cache
    private OpusDecoder decoders = initDecoder();
    private final OpusEncoder encoders = initEncoder();

    // Cache de estado de dados residuais
    private final LeftoverState leftoverStates = new LeftoverState();

    // Constantes
    private static final int FRAME_SIZE = AudioUtils.FRAME_SIZE;
    private static final int SAMPLE_RATE = AudioUtils.SAMPLE_RATE;
    private static final int CHANNELS = AudioUtils.CHANNELS;
    public static final int OPUS_FRAME_DURATION_MS = AudioUtils.OPUS_FRAME_DURATION_MS;
    private static final int MAX_SIZE = 1275;

    /**
     * Classe de estado de dados residuais
     */
    public static class LeftoverState {
        public short[] leftoverBuffer;
        public int leftoverCount;
        public boolean isFirst = true;

        public LeftoverState() {
            leftoverBuffer = new short[FRAME_SIZE]; // Pré-aloca um buffer do tamanho de um frame
            leftoverCount = 0;
        }

        public void clear() {
            leftoverCount = 0;
            Arrays.fill(leftoverBuffer, (short) 0);
        }
    }

    /**
     * Descarrega os dados residuais, gerando o último frame
     */
    public List<byte[]> flushLeftover() {
        LeftoverState state = leftoverStates;
        List<byte[]> frames = new ArrayList<>();

        if (state.leftoverCount <= 0) {
            return frames;
        }

        // Obtém o codificador
        OpusEncoder encoder = encoders;

        // Prepara o buffer
        short[] shortBuf = new short[FRAME_SIZE];
        byte[] opusBuf = new byte[MAX_SIZE];

        // Copia os dados residuais e preenche com silêncio
        System.arraycopy(state.leftoverBuffer, 0, shortBuf, 0, state.leftoverCount);
        Arrays.fill(shortBuf, state.leftoverCount, FRAME_SIZE, (short) 0);

        try {
            // Codifica o último frame
            int opusLen = encoder.encode(shortBuf, 0, FRAME_SIZE, opusBuf, 0, opusBuf.length);
            if (opusLen > 0) {
                byte[] frame = new byte[opusLen];
                System.arraycopy(opusBuf, 0, frame, 0, opusLen);
                frames.add(frame);
            }
        } catch (OpusException e) {
            log.warn("Falha ao codificar dados residuais: {}", e.getMessage());
        }

        // Limpa o cache
        state.clear();
        return frames;
    }

    /**
     * Converte Opus em array de bytes PCM
     */
    public byte[] opusToPcm(byte[] data) throws OpusException {
        if (data == null || data.length == 0) {
            return new byte[0];
        }

        try {
            OpusDecoder decoder = decoders;
            short[] buf = new short[FRAME_SIZE * 12];
            int samples = decoder.decode(data, 0, data.length, buf, 0, buf.length, false);

            byte[] pcm = new byte[samples * 2];
            for (int i = 0; i < samples; i++) {
                pcm[i * 2] = (byte) (buf[i] & 0xFF);
                pcm[i * 2 + 1] = (byte) ((buf[i] >> 8) & 0xFF);
            }

            return pcm;
        } catch (OpusException e) {
            log.warn("Falha na decodificação: {}", e.getMessage());
            // Reinicia o decodificador
            decoders = initDecoder();
            throw e;
        }
    }

    /**
     * Converte PCM em Opus
     */
    public List<byte[]> pcmToOpus(byte[] pcm, boolean isStream) {
        if (pcm == null || pcm.length == 0) {
            return new ArrayList<>();
        }

        // Garante que o comprimento do PCM seja par
        int pcmLen = pcm.length;
        if (pcmLen % 2 != 0) {
            pcmLen--;
        }

        // Número de amostras por frame
        int frameSize = FRAME_SIZE;

        // Obtém o codificador
        OpusEncoder encoder = encoders;

        // Processa o PCM
        List<byte[]> frames = new ArrayList<>();

        // Obtém o estado de dados residuais
        LeftoverState state = leftoverStates;

        // Tratamento de byte order
        ByteBuffer pcmBuf = ByteBuffer.wrap(pcm, 0, pcmLen).order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer inputShorts = pcmBuf.asShortBuffer();
        int totalInputSamples = inputShorts.remaining();

        // Combina os dados residuais com a entrada atual
        short[] combined;
        // Buffer
        short[] shortBuf = new short[frameSize];
        byte[] opusBuf = new byte[MAX_SIZE];

        if (isStream) {
            if (state.leftoverCount > 0 || !state.isFirst) {
                combined = new short[state.leftoverCount + totalInputSamples];
                System.arraycopy(state.leftoverBuffer, 0, combined, 0, state.leftoverCount);
                inputShorts.get(combined, state.leftoverCount, totalInputSamples);
            } else {
                combined = new short[totalInputSamples];
                inputShorts.get(combined);
                state.isFirst = false;
            }
        } else {
            combined = new short[totalInputSamples];
            inputShorts.get(combined);
        }

        int availableSamples = combined.length;
        int frameCount = availableSamples / frameSize;
        int remainingSamples = availableSamples % frameSize;

        // Processa o primeiro frame - se for um novo segmento de áudio, aplica efeito de fade-in
        if (frameCount > 0 && state.isFirst) {
            System.arraycopy(combined, 0, shortBuf, 0, frameSize);

            // Aplica efeito de fade-in - primeiros 20 milissegundos (aproximadamente 320 amostras)
            int fadeInSamples = Math.min(320, frameSize);
            for (int i = 0; i < fadeInSamples; i++) {
                // Fade-in linear
                float gain = (float) i / fadeInSamples;
                shortBuf[i] = (short) (shortBuf[i] * gain);
            }

            try {
                int opusLen = encoder.encode(shortBuf, 0, frameSize, opusBuf, 0, opusBuf.length);
                if (opusLen > 0) {
                    frames.add(Arrays.copyOf(opusBuf, opusLen));
                }
            } catch (Exception | AssertionError e) {
                log.warn("Falha ao codificar o frame de fade-in: {}", e.getMessage());
            }

            // Processa os frames completos restantes
            for (int i = 1; i < frameCount; i++) {
                int start = i * frameSize;
                System.arraycopy(combined, start, shortBuf, 0, frameSize);
                try {
                    int opusLen = encoder.encode(shortBuf, 0, frameSize, opusBuf, 0, opusBuf.length);
                    if (opusLen > 0) {
                        frames.add(Arrays.copyOf(opusBuf, opusLen));
                    }
                } catch (Exception | AssertionError e) {
                    log.warn("Falha ao codificar o frame #{}: {}", i, e.getMessage());
                }
            }
        } else {
            // Processa todos os frames completos
            for (int i = 0; i < frameCount; i++) {
                int start = i * frameSize;
                System.arraycopy(combined, start, shortBuf, 0, frameSize);
                try {
                    int opusLen = encoder.encode(shortBuf, 0, frameSize, opusBuf, 0, opusBuf.length);
                    if (opusLen > 0) {
                        frames.add(Arrays.copyOf(opusBuf, opusLen));
                    }
                } catch (Exception | AssertionError e) {
                    log.warn("Falha ao codificar o frame #{}: {}", i, e.getMessage());
                }
            }
        }

        if (isStream) {
            // Armazena as amostras restantes em cache
            state.leftoverCount = remainingSamples;
            if (remainingSamples > 0) {
                if (state.leftoverBuffer.length < remainingSamples) {
                    state.leftoverBuffer = new short[frameSize]; // Garante que o buffer seja grande o suficiente
                }
                System.arraycopy(combined, frameCount * frameSize, state.leftoverBuffer, 0, remainingSamples);
            } else {
                Arrays.fill(state.leftoverBuffer, (short) 0); // Limpa
            }
        }
        return frames;
    }
    
    /**
     * Obtém o decodificador
     */
    public OpusDecoder initDecoder() {
        try {
            OpusDecoder decoder = new OpusDecoder(SAMPLE_RATE, CHANNELS);
            decoder.setGain(0);
            return decoder;
        } catch (OpusException e) {
            log.error("Falha ao criar o decodificador", e);
            throw new RuntimeException("Falha ao criar o decodificador", e);
        }
    }

    /**
     * Obtém o codificador
     */
    private OpusEncoder initEncoder() {
        try {
            // Usa a aplicação AUDIO para obter maior fidelidade (TTS mais próximo de conteúdo com voz)
            OpusEncoder encoder = new OpusEncoder(SAMPLE_RATE, CHANNELS, OpusApplication.OPUS_APPLICATION_AUDIO);

            // Configurações de otimização
            encoder.setBitrate(AudioUtils.BITRATE);
            // Mantém o tipo de sinal como voz, para que as otimizações relacionadas à fala continuem válidas
            encoder.setSignalType(OpusSignal.OPUS_SIGNAL_VOICE);
            // Aumenta a complexidade para melhorar a qualidade da codificação
            encoder.setComplexity(10);
            // Habilita VBR quando a rede permitir, para melhorar a qualidade percebida
            encoder.setUseVBR(true);
            // Se necessário, defina o limite máximo de VBR desejado: encoder.setMaxBandwidth(OpusBandwidth.OPUS_BANDWIDTH_NARROWBAND);
            // A compensação de perda de pacotes é configurada conforme o cenário; aqui mantida em 0
            encoder.setPacketLossPercent(0);
            encoder.setForceChannels(CHANNELS);
            // Mantém o DTX desabilitado para preservar a saída contínua, evitando cortes abruptos durante o silêncio
            encoder.setUseDTX(false);

            return encoder;
        } catch (OpusException e) {
            log.error("Falha ao criar o codificador: taxa de amostragem={}, canais={}", SAMPLE_RATE, CHANNELS, e);
            throw new RuntimeException("Falha ao criar o codificador", e);
        }
    }

}