package com.xiaozhi.dialogue.runtime;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Ao dizer adeus, são necessárias informações de contexto?
 * Por exemplo: nome do usuário, ID do usuário, IP do usuário, informações do dispositivo do usuário, localização geográfica, trajetória de comportamento, histórico de diálogos anteriores.
 */
@Component
public class GoodbyeMessageSupplier implements Supplier<String> {

    private static final Random random = new Random();

    // Lista de mensagens de despedida
    private static final List<String> goodbyeMessages = Arrays.asList(
            "Certo, tchau~ Se precisar, é só me chamar!",
            "Beleza, então já vou indo, tchau~",
            "Combinado! Vou me retirar por agora, me chame se precisar~",
            "Entendido! Vou deixar você em paz, tchau~",
            "Certo, se precisar de algo é só chamar, tchau~",
            "Beleza, vou descansar um pouco, me chame quando precisar~",
            "Combinado! Vou me despedir por agora, tchau~",
            "Certo, vou sair agora, qualquer dúvida é só me procurar~",
            "Entendido! Vou ficar offline para descansar, me ative quando precisar~",
            "Tá bom, tá bom, então já vou indo, até mais~");

    @Override
    public String get() {
        return goodbyeMessages.get(random.nextInt(goodbyeMessages.size()));
    }
}
