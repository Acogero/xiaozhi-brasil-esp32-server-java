package com.xiaozhi.ai.tts;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Interface de serviço TTS.
 * Relação de alinhamento com o {@link org.springframework.ai.audio.tts.TextToSpeechModel} do Spring AI:
 * <ul>
 *   <li>TtsService → TextToSpeechModel (modo call) — em ponte via {@link TtsServiceAdapter}</li>
 *   <li>{@link XiaozhiTtsOptions} → TextToSpeechOptions — implementação direta</li>
 * </ul>
 *
 * @see TtsServiceAdapter Adaptador que adapta o TtsService para o TextToSpeechModel do Spring AI
 */
public interface TtsService {

  /**
   * Obtém o nome do provedor do serviço
   */
  String getProviderName();

  /**
   * Obtém a configuração de parâmetros do TTS
   */
  XiaozhiTtsOptions getOptions();

  /**
   * Obtém o nome do timbre de voz
   */
  default String getVoiceName() {
    return getOptions().getVoiceName();
  }

  /**
   * Obtém a velocidade da fala
   */
  default Double getSpeed() {
    return getOptions().getSpeed();
  }

  /**
   * Obtém o tom de voz
   */
  default Double getPitch() {
    return getOptions().getPitch();
  }

  /**
   * Formato de áudio
   */
  default String audioFormat() {
    return "wav";
  }

  /**
   * Gera o nome do arquivo
   * 
   * @return Nome do arquivo
   */
  default String getAudioFileName() {
    return UUID.randomUUID().toString().replace("-", "") + "." + audioFormat();
  }


  /**
   * Converte o texto em voz (com voz customizada)
   *
   * @param text Texto a ser convertido em voz
   * @return Caminho do arquivo de áudio gerado
   */
  Path textToSpeech(String text) throws Exception;


}
