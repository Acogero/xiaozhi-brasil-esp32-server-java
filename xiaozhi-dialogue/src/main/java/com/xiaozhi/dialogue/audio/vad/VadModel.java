package com.xiaozhi.dialogue.audio.vad;

/**
 * Interface do modelo de VAD - define as funcionalidades básicas do modelo de VAD
 */
public interface VadModel {
    /**
     * Inicializa o modelo de VAD
     */
    void initialize();

    /**
     * Inferência sem estado: o chamador é responsável por gerenciar e passar/receber o estado oculto do modelo
     * @param samples 512 pontos de amostra, float normalizado a 16kHz
     * @param prevState estado oculto do momento anterior, formato [2][1][128]; pode ser null, representando estado zero
     * @return resultado da inferência, contendo a probabilidade e o novo estado oculto
     */
    InferenceResult infer(float[] samples, float[][][] prevState);

    /**
     * Libera os recursos do modelo
     */
    void close();

    /**
     * Resultado da inferência
     */
    class InferenceResult {
        public final float probability;
        public final float[][][] state;

        public InferenceResult(float probability, float[][][] state) {
            this.probability = probability;
            this.state = state;
        }
    }
}
