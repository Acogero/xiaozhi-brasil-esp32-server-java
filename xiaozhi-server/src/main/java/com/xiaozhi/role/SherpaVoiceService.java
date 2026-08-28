package com.xiaozhi.role;

import java.util.List;
import java.util.Map;

/**
 * Serviço de varredura de vozes locais do Sherpa-ONNX.
 * <p>
 * Varre o diretório local dos modelos de TTS configurados, identificando automaticamente o tipo de modelo (Kokoro / Matcha / VITS) e o speaker.
 */
public interface SherpaVoiceService {

    /**
     * Varre o diretório local dos modelos de TTS e retorna a lista de vozes sherpa-onnx disponíveis.
     */
    List<Map<String, Object>> listVoices();
}
