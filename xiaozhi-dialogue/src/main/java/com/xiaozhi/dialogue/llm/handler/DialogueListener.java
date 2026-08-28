package com.xiaozhi.dialogue.llm.handler;

import com.xiaozhi.dialogue.runtime.DialogueTurn;
import com.xiaozhi.dialogue.runtime.PersonaListener;
import com.xiaozhi.dialogue.runtime.convert.DialogueTurnConverter;
import com.xiaozhi.message.service.MessageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
/**
 * Implementação gerenciada pelo Spring de PersonaListener (camada de infraestrutura).
 * Responsável pela persistência das mensagens do diálogo.
 */
@Slf4j
@Component
public class DialogueListener implements PersonaListener {

    @Resource
    private MessageService messageService;

    @Resource
    private DialogueTurnConverter dialogueTurnConverter;

    @Override
    public void onDialogueTurn(DialogueTurn turn) {
        try {
            messageService.saveAll(dialogueTurnConverter.toMessages(turn));
        } catch (Exception e) {
            log.error("Falha ao persistir o diálogo", e);
        }
    }

    @Override
    public void onError(Throwable error) {
        log.error("Falha na chamada ao LLM", error);
    }
}
