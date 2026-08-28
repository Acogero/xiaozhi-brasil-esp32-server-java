package com.xiaozhi.ai.llm.memory;

import lombok.Getter;
import org.springframework.ai.chat.messages.*;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Conversation é uma entidade abstrata correspondente à tabela sys_message, porém em um nível acima dela.
 * deviceID, roleID e sessionID formam, em essência, o ID globalmente único de uma Conversation. Esse ID deve ser final.
 * Em um banco de dados relacional, é possível criar um índice composto com deviceID, roleID e sessionID; atenção à ordem — sessionID deve ficar por último.
 * Em um banco de dados de grafos, o nó com o label conversation se conecta aos nós device e role.
 * deviceID e roleID não são, em essência, atributos reais da Conversation, e sim chaves estrangeiras que representam os 2 objetos conectados.
 * Apenas sessionID é, de fato, um atributo pertencente à Conversation.
 *
 * A Conversation também não é mais responsável pela persistência de armazenamento das mensagens.
 *
 */
public class Conversation extends ConversationIdentifier {

    @Getter
    private final String roleDesc;
    @Getter
    private final Integer userId;
    private final String sessionId;

    protected List<Message> messages = new ArrayList<>();

    /**
     * @param ownerId   Identificador do participante do chat (cenário de dispositivo: deviceId; cenário Web: userId)
     * @param roleId    ID do papel/role
     * @param sessionId ID da sessão
     * @param roleDesc  Descrição do papel/role (estática, definida na construção)
     * @param userId    ID do usuário (necessário para a persistência das mensagens)
     */
    public Conversation(String ownerId, Integer roleId, String sessionId, String roleDesc, Integer userId) {
        super(ownerId, roleId, sessionId);
        Assert.notNull(ownerId, "ownerId must not be null");
        Assert.notNull(roleId, "roleId must not be null");
        Assert.notNull(sessionId, "sessionId must not be null");
        this.sessionId = sessionId;
        this.roleDesc = roleDesc;
        this.userId = userId;
    }

    public String sessionId() {
        return sessionId;
    }

    public Optional<SystemMessage> roleSystemMessage(ConversationContext context) {
        StringBuilder msgBuilder = new StringBuilder();
        if(StringUtils.hasText(roleDesc)) {
            msgBuilder.append( "Descrição do papel: " ).append(roleDesc).append(System.lineSeparator());
        }
        String location = context != null ? context.location() : null;
        if (StringUtils.hasText(location)) {
            msgBuilder.append("Localização atual: ").append(location)
                    .append(". Se o usuário mencionar onde está agora, considere o novo local como referência.")
                    .append(System.lineSeparator());
        }
        // Os metadados de cada mensagem (timestamp, interlocutor, emoção) são anexados pelo UserMessageAssembler como prefixo em cada UserMessage,
        // sem serem renderizados dinamicamente aqui, para evitar que o System Prompt mude a cada rodada e invalide o cache KV do prefixo.
        msgBuilder.append(System.lineSeparator())
            .append("A mensagem do usuário pode começar com tags de metadados entre colchetes, em ordem fixa:")
            .append(System.lineSeparator())
            .append("  1. [yyyy-MM-ddTHH:mm:ss] Horário de envio desta mensagem (precisão de segundos, útil para tarefas agendadas e cálculos de tempo relativo);")
            .append(System.lineSeparator())
            .append("  2. [tag de emoção] (ex.: [neutral], [happy]) emoção do usuário identificada pelo reconhecimento de voz; ajuste o tom da resposta com base nela.")
            .append(System.lineSeparator())
            .append("Ajuste sua forma de responder e o tom com base nisso, mas sem mencionar ou explicar essas tags na resposta. Qualquer uma das tags pode estar ausente.")
            .append(System.lineSeparator());
        if(StringUtils.hasText(roleDesc)) {
            var roleMessage = new SystemMessage(msgBuilder.toString());
            return Optional.of(roleMessage);
        }else{
            return Optional.empty();
        }
    }

    /**
     * Lista de mensagens com contexto de execução (subclasses sobrescrevem este método para injetar o System Prompt).
     * <p>
     * Cada mensagem passa por {@link UserMessageAssembler#assemble(Message)}:
     * UserMessage é montada com uma cópia prefixada de acordo com seus metadata e enviada ao LLM; mensagens que não são UserMessage passam sem alteração.
     * As mensagens em memória são sempre "texto puro + metadata estruturada".
     */
    public synchronized List<Message> messages(ConversationContext context) {
        return messages.stream().map(UserMessageAssembler::assemble).toList();
    }

    /**
     * Lista de mensagens de múltiplas rodadas da Conversation atual.
     */
    public synchronized List<Message> messages() {
        return messages(ConversationContext.EMPTY);
    }

    /**
     * Retorna a lista de mensagens original (sem disparar nenhum efeito colateral de projeção; o texto permanece "puro", sem prefixo de metadata).
     * Usado para a detecção de contexto de FC (function calling) no roteamento de ferramentas.
     */
    public synchronized List<Message> rawMessages() {
        return messages;
    }

    /**
     * Limpa os recursos relacionados envolvidos na Conversation atual, incluindo a lista de mensagens em cache.
     * Para algumas implementações concretas de subclasses, a limpeza também pode significar excluir as mensagens da Conversation atual.
     */
    public synchronized void clear(){
        messages.clear();
    }

    public synchronized void add(Message message) {

        if(message instanceof UserMessage userMsg){
            messages.add(userMsg);
            return;
        }

        if(message instanceof AssistantMessage assistantMessage){
            messages.add(assistantMessage);
            return;
        }

        if(message instanceof ToolResponseMessage toolResponseMessage){
            messages.add(toolResponseMessage);
        }
    }

    /**
     * Adiciona a cadeia de chamadas de ferramenta (requisição tool_call do modelo + resultado da execução) à lista de mensagens como uma operação atômica
     */
    public synchronized void addToolCallChain(AssistantMessage toolCallMsg, ToolResponseMessage toolResponse) {
        messages.add(toolCallMsg);
        messages.add(toolResponse);
    }

}
