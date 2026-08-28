package com.xiaozhi.ai.llm.providers.xingchen;

/**
 * Interface de callback em streaming do Agent XingChen
 * Baseado na documentação: https://www.xfyun.cn/doc/spark/Agent04-API%E6%8E%A5%E5%85%A5.html
 */
public interface XingChenChatStreamCallback {

    /**
     * Mensagem comum recebida
     * @param event Evento de resposta
     */
    default void onMessage(XingChenResponse event) {
    }

    /**
     * Fim do stream de mensagens
     * @param event Evento de resposta (pode ser null)
     */
    default void onMessageEnd(XingChenResponse event) {
    }

    /**
     * Evento de chamada de ferramenta recebido
     * @param event Evento de resposta (contém event_data)
     */
    default void onFunctionCall(XingChenResponse event) {
    }

    /**
     * Ocorreu um erro
     * @param event Resposta de erro (code != 0)
     */
    default void onError(XingChenResponse event) {
    }

    /**
     * Ocorreu uma exceção
     * @param throwable Objeto da exceção
     */
    default void onException(Throwable throwable) {
    }
}
