package com.xiaozhi.dialogue.llm.factory;

import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.communication.common.SessionManager;
import com.xiaozhi.communication.message.MessageSender;
import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.ai.llm.memory.Conversation;
import com.xiaozhi.ai.llm.memory.ConversationFactory;
import com.xiaozhi.dialogue.audio.AecService;
import com.xiaozhi.dialogue.playback.OpusRecorder;
import com.xiaozhi.dialogue.playback.Player;
import com.xiaozhi.dialogue.playback.ScheduledPlayer;
import com.xiaozhi.dialogue.playback.Synthesizer;
import com.xiaozhi.dialogue.playback.SynthesizerFactory;
import com.xiaozhi.dialogue.runtime.GoodbyeMessageSupplier;
import com.xiaozhi.dialogue.runtime.Persona;
import com.xiaozhi.ai.llm.factory.ChatModelFactory;
import com.xiaozhi.ai.stt.SttService;
import com.xiaozhi.ai.stt.SttServiceFactory;
import com.xiaozhi.ai.tts.TtsService;
import com.xiaozhi.ai.tts.TtsServiceFactory;
import com.xiaozhi.common.model.bo.ConfigBO;
import com.xiaozhi.common.model.bo.RoleBO;
import com.xiaozhi.role.service.RoleService;
import com.xiaozhi.ai.tool.ToolRegistrationService;
import com.xiaozhi.dialogue.adapter.ChatSessionToolAdapter;
import com.xiaozhi.config.service.ConfigService;
import com.xiaozhi.dialogue.llm.handler.DialogueListener;
import com.xiaozhi.message.service.MessageService;
import com.xiaozhi.storage.service.StorageServiceFactory;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.util.Assert;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
/**
 * Classe factory de Persona, responsável por construir a instância completa do Persona (incluindo componentes como STT/TTS/LLM/Player).
 */
@Slf4j
@Component
public class PersonaFactory {
    @Resource
    private MessageService chatMessageService;
    @Resource
    private ConfigService configService;
    @Resource
    private ChatModelFactory chatModelFactory;
    @Resource
    private ToolRegistrationService toolRegistrationService;
    @Resource
    private TtsServiceFactory ttsFactory;
    @Resource
    private SttServiceFactory sttFactory;
    @Resource
    private ConversationFactory conversationFactory;
    @Resource
    private RoleService roleService;
    @Resource
    private MessageSender sessionMessageService;
    @Resource
    private SessionManager sessionManager;
    @Resource
    private AecService aecService;

    @Resource
    private GoodbyeMessageSupplier goodbyeMessages;
    @Resource
    private DialogueListener dialogueListener;
    @Resource
    private StorageServiceFactory storageServiceFactory;

    /**
     * Constrói a instância completa do Persona.
     * ToolCallbacks é obtido dinamicamente via session.getToolCallbacks(), suportando o registro de ferramentas MCP/IoT em tempo de execução.
     * Player não pertence totalmente ao Persona; quando o papel não existe, o Player deve ser construído antes do Persona, para permitir a reprodução de mensagens de erro.
     *
     * @param session sessão atual
     * @param device informações do dispositivo
     * @param role configuração do papel; um novo Persona só precisa ser construído quando diferente do Persona atual
     * @return a instância de Persona construída
     */
    public Persona buildPersona(ChatSession session, DeviceBO device, RoleBO role) {
        Assert.notNull(device, "device cannot be null");
        Assert.notNull(role, "role cannot be null");

        // Proteção de idempotência: pula a reconstrução se o Persona já existir
        if (session.getPersona() != null) {
            return session.getPersona();
        }

        // O Player deve poder existir independentemente do Persona; também pode ser visto como a boca/as cordas vocais do papel.
        Player player = session.getPlayer();
        if(player == null){
            player = new ScheduledPlayer(session, sessionMessageService);
            player.setOpusRecorder(new OpusRecorder(session, chatMessageService, aecService, storageServiceFactory));
            session.setPlayer(player);
        }
        // Inicializa o Conversation (equivalente à memória do papel)
        String ownerId = device.getDeviceId();
        Integer userId = device.getUserId();
        Conversation conversation = conversationFactory.initConversation(ownerId, userId, role, session.getSessionId());

        // Obtém o serviço de STT
        SttService sttService = initSttService(role);

        // Inicializa o sintetizador de voz
        Synthesizer synthesizer = initSynthesizer(session,player,role);

        //Trata o registro de ferramentas (ferramentas do sistema + MCP do dispositivo)
        toolRegistrationService.register(new ChatSessionToolAdapter(session));

        // Obtém o ChatModel
        ChatModel chatModel = chatModelFactory.getChatModel(role);

        // Ferramentas MCP/IoT já registradas; obtém a lista completa de ferramentas para passar ao Persona
        var toolCallbacks = session.getToolCallbacks();

        Persona persona = Persona.builder()
                .sessionManager(sessionManager)
                .sessionId(session.getSessionId())
                .conversation(conversation)
                .sttService(sttService)
                .chatModel(chatModel)
                .synthesizer(synthesizer)
                .player(session.getPlayer())
                .toolCallbacks(toolCallbacks)
                .listener(dialogueListener)
                .goodbyeMessages(goodbyeMessages)
                .build();
        session.setPersona(persona);
        return persona;
    }

    /**
     * Sobrecarga: passa apenas session; obtém device automaticamente da session e role do DB/cache.
     */
    public Persona buildPersona(ChatSession session) {
        if (session.getPersona() != null) {
            return session.getPersona();
        }
        DeviceBO device = session.getDevice();
        RoleBO role = roleService.getBO(device.getRoleId());
        return buildPersona(session, device, role);
    }

    /**
     * Inicializa o serviço de STT, registrando as informações importantes em log
     * @param role
     * @return
     */
    private SttService initSttService(RoleBO role){
        Assert.notNull(role, "role cannot be null");
        var sttId = role.getSttId();
        if (sttId == null || sttId <= 0) {
            log.warn("O papel não tem serviço de STT configurado - Role: {}, usando vosk como padrão", role.getRoleName());
            return sttFactory.getSttService(null);
        }
        var sttConfig = configService.getBO(sttId);
        if(sttConfig == null){
            log.error("Não foi possível obter a configuração do serviço de STT - Id: {}", sttId);
            return null;
        }
        SttService sttService = sttFactory.getSttService(sttConfig);
        if (sttService == null) {
            log.error("Não foi possível obter o serviço de STT - Provider: {}", sttConfig != null ? sttConfig.getProvider() : "null");
        }
        return sttService;
    }

    /**
     * Inicializa o estado do diálogo
     */
    public Synthesizer initSynthesizer(ChatSession session, Player player, RoleBO role) {
        // É provável que um dispositivo recém-adicionado não tenha o TTS configurado; para usar o Edge padrão é necessário passar null
        ConfigBO ttsConfig = null;
        if (role.getTtsId() != null && role.getTtsId() > 0) {
            ttsConfig = configService.getBO(role.getTtsId());
        }
        String voiceName = role.getVoiceName();
        TtsService ttsService = ttsFactory.getTtsService(ttsConfig, voiceName, role.getTtsPitch(), role.getTtsSpeed());

        return SynthesizerFactory.create(session, ttsService, player);

    }

}
