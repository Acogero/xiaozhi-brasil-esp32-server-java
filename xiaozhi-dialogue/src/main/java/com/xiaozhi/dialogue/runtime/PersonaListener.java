package com.xiaozhi.dialogue.runtime;

/**
 * Interface de callback do ciclo de vida do Persona (camada de domínio, sem dependência de framework).
 * <p>
 * Desacopla o Persona da infraestrutura (persistência de mensagens, estatísticas de monitoramento):
 * O Persona apenas notifica externamente "o que aconteceu" por meio desta interface; a implementação gerenciada pelo Spring decide "o que fazer".
 * <p>
 * Amigável a ambientes distribuídos: quando for necessário fazer broadcast em cluster no futuro, basta a implementação fazer a ponte internamente com Redis/Kafka, sem nenhuma alteração no Persona.
 * <p>
 * Nota: o reconhecimento de voz STT e as chamadas de ferramentas ocorrem fora do Persona (respectivamente no DialogueService e no framework Spring AI),
 * por isso são notificados via Spring Event (SpeechRecognizedEvent, ToolCallCompletedEvent) e não por esta interface.
 *
 * @see com.xiaozhi.dialogue.llm.handler.DialogueListener
 */
public interface PersonaListener {

    /**
     * Callback chamado após a conclusão de uma rodada de Conversation.
     * A implementação é responsável por persistir a mensagem, registrar o sucesso da chamada ao LLM etc.
     *
     * @param turn informações completas desta rodada do diálogo
     */
    void onDialogueTurn(DialogueTurn turn);

    /**
     * Callback chamado quando a chamada ao LLM falha.
     * A implementação é responsável por registrar a falha da chamada ao LLM, logs etc.
     *
     * @param error informações do erro
     */
    void onError(Throwable error);
}
