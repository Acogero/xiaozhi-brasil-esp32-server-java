package com.xiaozhi.dialogue.llm.tool.media;

import com.xiaozhi.common.Speech;

import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.utils.AudioUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import reactor.core.publisher.Flux;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HuiBenPlayer  {

    private static final String API_BASE_URL = "https://www.limaogushi.com/huiben/";

    // Usa OkHttp3 em vez do HttpClient do JDK
    private static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final Integer bookId;
    private final ChatSession session;

    public HuiBenPlayer(ChatSession session, Integer bookId) {
        this.session = session;
        this.bookId = bookId;
    }

    /**
     * Reproduz o livro ilustrado
     * Implementa o processamento assíncrono usando virtual threads do JDK 21 e concorrência estruturada
     */
    public void play() {

        // 1. Obtém as informações do livro ilustrado
        Map<String, String> huiBenInfo = getHuiBenInfo(bookId);
        if (huiBenInfo == null) {
            throw new RuntimeException("Não foi possível encontrar o livro ilustrado: " + bookId);
        }

        // 2. Baixa o arquivo de áudio para o diretório temporário local, usando um nome de arquivo aleatório para evitar conflitos
        String audioUrl = huiBenInfo.get("audioUrl");
        String randomName = "huiBen_" + session.getSessionId() + "_" + UUID.randomUUID() + ".mp3";
        // Lê o arquivo de áudio
        Path audioFilePath = downloadFile(audioUrl, randomName);

        if (audioFilePath == null || !Files.exists(audioFilePath)) {
            log.warn("Arquivo de áudio não encontrado: {}", audioFilePath);
            return;
        }

        // Envia a mensagem de início do livro ilustrado

        // Envia o áudio e o texto sincronizado
        try {
            // Converte o arquivo de áudio para o formato PCM
            byte[] audioData = AudioUtils.readAsPcm(audioFilePath.toAbsolutePath().toString());
            if (audioData == null || audioData.length == 0) {
                log.warn("Dados de áudio vazios");
                return;
            }
            Flux<Speech> speechFlux = Flux.just(new Speech(audioData))
                    .doFinally(signalType -> AudioUtils.deleteFile(audioFilePath.toString()));
            session.getPlayer().play(speechFlux);

        } catch (Exception e) {
            log.error("Erro ao processar o áudio ", e);
        }
    }

    /**
     * Obtém as informações do livro ilustrado (URL do áudio)
     */
    private Map<String, String> getHuiBenInfo(Integer bookId) {
        try {
            // Monta a URL
            String url = API_BASE_URL + bookId + ".html";

            // Envia a requisição usando OkHttp3
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Falha ao obter as informações do livro ilustrado, código de resposta: {}", response.code());
                    return null;
                }

                // Analisa a resposta
                String responseBody = response.body() != null ? response.body().string() : null;
                if (responseBody == null) {
                    log.error("Falha ao obter as informações do livro ilustrado, corpo da resposta vazio");
                    return null;
                }

                String audioUrl = extractAudioSrcByRegex(responseBody);
                Map<String, String> result = new HashMap<>();
                result.put("audioUrl", audioUrl);
                return result;
            }
        } catch (Exception e) {
            log.error("Erro ao obter as informações do livro ilustrado", e);
            return null;
        }
    }

    /**
     * Extrai a URL de origem do áudio a partir do HTML
     */
    public static String extractAudioSrcByRegex(String html) {
        // Localiza o atributo src na tag source
        Pattern pattern = Pattern.compile("<source\\s+[^>]*src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>");
        Matcher matcher = pattern.matcher(html);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Baixa o arquivo para o diretório temporário
     */
    private Path downloadFile(String fileUrl, String fileName) {
        try {
            // Garante que o diretório de áudio exista
            Path audioDir = Path.of(AudioUtils.AUDIO_PATH);
            Files.createDirectories(audioDir);

            // Salva o arquivo no diretório de áudio
            Path outputPath = audioDir.resolve(fileName);

            // Baixa o arquivo usando OkHttp3
            Request request = new Request.Builder()
                    .url(fileUrl)
                    .get()
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.error("Falha ao baixar o arquivo, código de resposta: {}", response.code());
                    return null;
                }

                // Grava o corpo da resposta no arquivo
                Files.write(outputPath, response.body().bytes());

                //return outputPath.toAbsolutePath().toString();
                return outputPath;
            }
        } catch (Exception e) {
            log.error("Erro ao baixar o arquivo", e);
            return null;
        }
    }
}
