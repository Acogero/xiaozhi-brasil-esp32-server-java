package com.xiaozhi.dialogue.playback;

import com.xiaozhi.common.Speech;

import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.ai.tts.TtsService;
import lombok.Data;
import reactor.core.publisher.Flux;


/**
 * Classe base do sintetizador de voz, orquestrador de alto nível do TtsService.
 *
 * No pipeline do diálogo VAD → STT → LLM → TTS → Player, o Synthesizer pertence à etapa de TTS,
 * responsável por orquestrar o fluxo de texto (ou um único texto) gerado pelo LLM em um fluxo de áudio Speech consumível pelo Player.
 * Diferentes Providers de TTS retornam formatos de dados diferentes,
 * e o Synthesizer oculta essas diferenças, fornecendo de forma unificada um Flux<Speech> ao Player.
 *
 * Camadas da arquitetura:
 * - TtsService: interface de baixo nível do Provider de TTS
 * - Synthesizer (esta classe): orquestrador de alto nível, responsável pela segmentação de frases, consulta de cache e construção do fluxo de áudio
 * - Player: reprodução de áudio no terminal, recebe o Flux<Speech> e converte para o formato do protocolo do dispositivo (Opus) para envio
 *
 * Modo de síntese:
 * text → Path（FileSynthesizer + TtsService）
 *
 * Ciclo de vida: o Synthesizer pode ser liberado assim que a síntese da resposta da IA de uma rodada de diálogo terminar; o Player pode ainda estar reproduzindo.
 * O mesmo Synthesizer pode ser combinado com Players diferentes; por isso, o Player não é criado internamente pelo Synthesizer.
 */
@Data
public abstract class Synthesizer {

    protected final ChatSession chatSession;
    protected final TtsService ttsService;
    protected final Player player;

    private int firstChatDurationMillis = 0;

    /**
     * @param chatSession  sessão atual
     * @param ttsService   Provider de TTS de baixo nível (criado por TtsServiceFactory)
     * @param player       player usado para reproduzir o áudio
     */
    public Synthesizer(ChatSession chatSession, TtsService ttsService, Player player) {
        this.chatSession = chatSession;
        this.ttsService = ttsService;
        this.player = player;
    }

    /**
     * Síntese de voz.
     * @param stringFlux fluxo de texto; cada elemento do fluxo vem da saída do LLM, principalmente tokens.
     *                   Em geral, não pode ser submetido diretamente para síntese de TTS; a implementação específica do Provider precisa reorganizar em frases antes da síntese de voz.
     */
    abstract public void synthesize(Flux<String> stringFlux);

    /**
     * Cancela a síntese de voz, interrompendo a assinatura do Flux upstream.
     * Chamado externamente quando o usuário interrompe (abort), garantindo que nenhum novo dado de áudio seja gerado.
     */
    abstract public void cancel();

    /**
     * Verifica se o pipeline de síntese de voz ainda está ativo (LLM gerando, TTS sintetizando etc.).
     * Usado na decisão de interrupção: mesmo que o Player já tenha parado de reproduzir, se o pipeline upstream ainda estiver em funcionamento, a interrupção deve ocorrer.
     */
    abstract public boolean isActive();

    /**
     * Síntese de voz.
     * @param text geralmente é uma frase completa ou um texto completo que pode ser submetido de uma vez para a síntese de voz.
     */
    abstract public void synthesize(String text);
}
