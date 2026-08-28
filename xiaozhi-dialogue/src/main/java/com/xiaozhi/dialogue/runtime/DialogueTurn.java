package com.xiaozhi.dialogue.runtime;

import com.xiaozhi.ai.llm.memory.Conversation;
import com.xiaozhi.ai.llm.memory.MessageTimeMetadata;
import lombok.Builder;
import lombok.Value;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.util.Assert;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Representa uma rodada de interação já concluída em um Conversation:
 * um UserMessage, correspondendo a um AssistantMessage final, além das informações de sequência temporal e de chamadas de ferramentas geradas nessa rodada.
 * <p>
 * Uma rodada pode ter múltiplas cadeias de chamadas de ferramentas (em ordem), representadas por {@code toolChains}, por exemplo:
 * <ol>
 *   <li>Ferramentas reais chamadas proativamente pelo modelo durante a geração (MCP/Function interna)</li>
 * </ol>
 * Ao persistir, são gravadas em sys_message na ordem; ao reproduzir, são restauradas na mesma ordem.
 * <p>
 * Serve apenas como o objeto de resultado de uma única rodada dentro do Conversation; a conversão para persistência é responsabilidade de
 * {@link com.xiaozhi.dialogue.runtime.convert.DialogueTurnConverter}.
 */
@Value
public class DialogueTurn {

    private UserMessage userMessage;
    private ChatResponse chatResponse;
    private Conversation conversation;
    private Instant userMessageCreatedAt;
    private Instant assistantMessageCreatedAt;
    private List<DialogueContext.ToolCallInfo> toolCallDetails;
    private Path userSpeechPath;

    /**
     * Cadeia de chamadas de ferramentas em ordem cronológica dentro da rodada (pode estar vazia)
     */
    private List<ToolChainPair> toolChains;

    private final AssistantMessage assistantMessage;
    private final Duration timeToFirstToken;

    @Builder
    public DialogueTurn(
            UserMessage userMessage,
            ChatResponse chatResponse,
            Conversation conversation,
            Path userSpeechPath,
            Instant userMessageCreatedAt,
            Instant assistantMessageCreatedAt,
            List<DialogueContext.ToolCallInfo> toolCallDetails,
            List<ToolChainPair> toolChains) {
        Assert.notNull(userMessage, "O objeto de mensagem do usuário não deve ser NULL!");
        Assert.notNull(chatResponse, "O objeto de resposta do modelo de linguagem não deve ser NULL!");
        Assert.notNull(conversation, "O objeto de sessão não deve ser NULL!");
        Assert.notNull(userMessageCreatedAt, "O objeto de horário de criação da mensagem do usuário não deve ser NULL!");
        Assert.notNull(assistantMessageCreatedAt, "O objeto de horário de criação da resposta do modelo não deve ser NULL!");
        this.userMessage = userMessage;
        this.chatResponse = chatResponse;
        this.conversation = conversation;
        this.userSpeechPath = userSpeechPath;
        this.timeToFirstToken = Duration.between(userMessageCreatedAt, assistantMessageCreatedAt);
        this.userMessageCreatedAt = userMessageCreatedAt.truncatedTo(ChronoUnit.SECONDS);
        this.assistantMessageCreatedAt = assistantMessageCreatedAt.truncatedTo(ChronoUnit.SECONDS);
        this.toolCallDetails = toolCallDetails != null ? toolCallDetails : List.of();
        this.toolChains = toolChains != null ? toolChains : List.of();
        this.assistantMessage = chatResponse.getResult().getOutput();
    }

    public void injectInstants() {
        MessageTimeMetadata.setTimeMillis(userMessage, userMessageCreatedAt);
        MessageTimeMetadata.setTimeMillis(assistantMessage, assistantMessageCreatedAt);
    }
}
