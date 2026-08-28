package com.xiaozhi.ai.tts;

import reactor.core.publisher.Flux;

@FunctionalInterface
public interface ChatConverter {

    /**
     * Converte a string de tokens em um SentenceResult (texto puro + palavra de emoção).
     * @return
     */
    Flux<SentenceHelper.SentenceResult> convert(Flux<String> stringFlux);
}
