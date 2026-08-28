package com.xiaozhi.ai.llm.service;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import com.xiaozhi.ai.llm.factory.ChatModelFactory;

import lombok.extern.slf4j.Slf4j;
/**
 * Serviço de reconhecimento visual.
 * Encapsula a chamada ao modelo multimodal de visão, para uso por ferramentas MCP ou por interfaces REST.
 */
@Slf4j
@Service
public class VisionService {

    @Resource
    private ChatModelFactory chatModelFactory;

    /**
     * Reconhece o conteúdo da imagem.
     *
     * @param file     Arquivo de imagem
     * @param question Pergunta do usuário
     * @return Descrição textual retornada pelo modelo de visão
     * @throws IllegalStateException Nenhum modelo de visão disponível
     */
    public String recognize(MultipartFile file, String question) {
        ChatModel chatModel = chatModelFactory.getVisionModel();
        if (chatModel == null) {
            throw new IllegalStateException("Nenhum modelo de visão disponível");
        }

        MimeType mimeType = MimeType.valueOf(file.getContentType());
        Media media = Media.builder()
                .mimeType(mimeType)
                .data(file.getResource())
                .build();

        UserMessage userMessage = UserMessage.builder()
                .media(media)
                .text(question)
                .build();

        String result = ChatClient.builder(chatModel)
                .defaultAdvisors()
                .build()
                .prompt()
                .messages(userMessage)
                .call()
                .content();
        log.info("Reconhecimento visual concluído - pergunta: {}, resultado: {}", question, result);
        return result;
    }
}
