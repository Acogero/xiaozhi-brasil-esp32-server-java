package com.xiaozhi.ai.stt;

import reactor.core.publisher.Flux;

/**
 * Interface de serviço STT
 */
public interface SttService {

  /**
   * Obtém o nome do provedor do serviço
   */
  String getProviderName();

  /**
   * Processa dados de áudio em streaming
   *
   * @param audioSink Fluxo de dados de áudio
   * @return Resultado do reconhecimento, contendo o texto e informações opcionais de emoção
   */
  SttResult stream(Flux<byte[]> audioSink);

}
