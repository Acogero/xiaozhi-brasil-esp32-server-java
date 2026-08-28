package com.xiaozhi.dialogue.playback;

import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.ai.tts.TtsService;

/**
 * Factory de Synthesizer, cria a implementação correspondente de Synthesizer.
 */
public class SynthesizerFactory {

    public static Synthesizer create(ChatSession session, TtsService ttsService, Player player) {
        return new FileSynthesizer(session, ttsService, player);
    }
}
