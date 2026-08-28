package com.xiaozhi.dialogue.runtime;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Fornecedor de mensagens de timeout
 * Usado para fornecer a mensagem de aviso quando a sessão expira por timeout
 */
@Component
public class TimeoutMessageSupplier implements Supplier<String> {

    private static final Random random = new Random();

    // Lista de mensagens de aviso de timeout
    private static final List<String> timeoutMessages = Arrays.asList(
            "Parece que você está ocupado com outra coisa, vou me retirar por agora~",
            "Parece que você não precisa de mim no momento, vou descansar um pouco~",
            "Faz um tempo que você não fala nada, vou recarregar as baterias~",
            "Parece que você está ocupado, não vou incomodar~",
            "Parece que você tem outra coisa para fazer, vou sair por agora~",
            "Faz um tempo que você não fala nada, vou descansar~");

    @Override
    public String get() {
        return timeoutMessages.get(random.nextInt(timeoutMessages.size()));
    }
}
