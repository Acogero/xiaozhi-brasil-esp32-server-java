package com.xiaozhi.dialogue.llm.tool.media;

import com.xiaozhi.common.Speech;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.utils.AudioUtils;
import io.jsonwebtoken.lang.Assert;
import lombok.Getter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MusicPlayer {
    private static final String API_BASE_URL = "";//"https://api.xiaozhi.com/api/v1/music/search";
    // Usa OkHttp3 em vez do HttpClient do JDK
    private static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final ChatSession session;

    @Getter
    private final String song;
    @Getter
    private final String artist;

    // Armazena as informações da letra atual de cada sessão
    @Getter
    private List<LyricLine> lyrics = new ArrayList<>();

    // Armazena o caminho do arquivo de áudio, usado para exclusão após a reprodução
    @Getter
    private Path audioFile;

    public MusicPlayer(ChatSession session, String song, String artist) {
        Assert.notNull(session.getPlayer(), "O player não foi inicializado para a sessão atual");
        this.session = session;
        this.song = song;
        this.artist = artist;

    }

    /**
     * Busca e reproduz música
     * O chamador processa de forma assíncrona usando virtual threads
     */
    public void play() {
        String sessionId = session.getSessionId();
        try {

            // Limpa o arquivo de áudio anterior (se houver)
            AudioUtils.deleteFile(audioFile.toString());

            // 1. Obtém as informações da música
            Map<String, String> musicInfo = getMusicInfo(song, artist);
            if (musicInfo == null) {
                throw new RuntimeException("Não foi possível encontrar a música: " + song + (artist != null ? " - " + artist : ""));
            }

            // 2. Baixa o arquivo de áudio para o diretório temporário local, usando um nome de arquivo aleatório para evitar conflitos
            String audioUrl = musicInfo.get("audioUrl");
            String randomName = "music_" + sessionId + "_" + UUID.randomUUID() + ".mp3";
            audioFile = downloadFile(audioUrl, randomName);

            if (!Files.exists(audioFile)) {
                throw new RuntimeException("Falha ao baixar o arquivo de áudio");
            }

            // 3. Analisa a letra
            String lyricUrl = musicInfo.get("lyricUrl");
            lyrics = parseLyrics(lyricUrl);

            // Envia o áudio e a letra sincronizada
            sendAudioWithLyrics(audioFile);

        } catch (Exception e) {
            log.error("Erro ao reproduzir a música", e);
            session.getPersona().getSynthesizer().synthesize("Erro ao reproduzir a música");
        }

    }

    /**
     * Envia o áudio e a letra sincronizada
     */
    private void sendAudioWithLyrics(Path audioPath) {

        if (!Files.exists(audioPath)) {
            log.error("Arquivo de áudio não encontrado: {}", audioPath);
            return;
        }
        if (CollectionUtils.isEmpty(lyrics) || lyrics.size() < 2) {
            session.getPlayer().play(song, audioPath);
            return;
        }
        try {
            // Lê o arquivo de áudio
            // Converte o arquivo de áudio para o formato PCM
            byte[] audioData = AudioUtils.readAsPcm(audioPath.toAbsolutePath().toString());
            if (audioData == null || audioData.length == 0) {
                log.warn("Dados de áudio vazios");
                return;
            }

            // Pré-processa os marcadores de tempo da letra, convertendo o tempo em milissegundos para índice de frame
            // Um frame Opus tem aproximadamente algumas centenas de bytes, 60ms, e a quantidade de bytes do PCM também é diretamente proporcional à duração. É possível estimar aproximadamente o tamanho em bytes correspondente pelos milissegundos.
            LyricLine[] lines=lyrics.toArray(new LyricLine[0]);

            // Obtém a duração real da música a partir dos metadados do áudio
            double durationSec = AudioUtils.getAudioDuration(audioPath);
            long sumMs = durationSec > 0 ? (long)(durationSec * 1000) : lines[1].timeMs() + lines[lines.length - 1].timeMs();
            // Média de bytes por milissegundo
            int avg = (int) ( audioData.length / sumMs);
            int startIndex=0;
            int endIndex=0;
            List<Speech> speeches = new ArrayList<>();
            for (int i=0;i<lines.length-1;i++) {
                // Calcula o array de bytes de áudio correspondente à letra
                int ms = lines[i+1].timeMs()-lines[i].timeMs();
                endIndex = ms * avg;
                if(endIndex>audioData.length){

                    log.warn("A letra excede a duração do áudio; foi truncada");
                    break;
                }
                byte[] frameData = Arrays.copyOfRange(audioData, startIndex, endIndex);
                String text = lines[i].text();
                Speech speech = new Speech(frameData, text);
                speeches.add(speech);
                startIndex = endIndex;
            }
            if(endIndex<audioData.length){
                byte[] frameData = Arrays.copyOfRange(audioData, startIndex, audioData.length);
                Speech speech = new Speech(frameData, lines[lines.length-1].text());
                speeches.add(speech);
            }
            session.getPlayer().play(Flux.fromIterable( speeches));

        } catch (Exception e) {
            String sessionId = session.getSessionId();
            log.error("Erro ao processar o áudio - SessionId: {}", sessionId, e);

        }
    }

    /**
     * Obtém as informações da música (URL do áudio e URL da letra)
     */
    private Map<String, String> getMusicInfo(String song, String artist) {
        try {
            // Monta a URL
            StringBuilder urlBuilder = new StringBuilder(API_BASE_URL + "/stream_pcm?song=" +
                    URLEncoder.encode(song, StandardCharsets.UTF_8));

            if (artist != null && !artist.isEmpty()) {
                urlBuilder.append("&artist=").append(URLEncoder.encode(artist, StandardCharsets.UTF_8));
            }

            // Envia a requisição usando OkHttp3
            Request request = new Request.Builder()
                    .url(urlBuilder.toString())
                    .get()
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Falha ao obter as informações da música, código de resposta: {}", response.code());
                    return null;
                }

                // Analisa a resposta JSON
                String responseBody = response.body() != null ? response.body().string() : null;
                if (responseBody == null) {
                    log.error("Falha ao obter as informações da música, corpo da resposta vazio");
                    return null;
                }

                Map<String, Object> responseMap = new ObjectMapper().readValue(responseBody, Map.class);

                Map<String, String> result = new HashMap<>();

                // Verifica o formato da resposta da API, com suporte a dois possíveis nomes de campo
                String audioPath = (String) responseMap.get("audioPath");
                String audioUrl = (String) responseMap.get("audio_url");

                String lyricPath = (String) responseMap.get("lyricPath");
                String lyricUrl = (String) responseMap.get("lyric_url");

                // Usa a URL direta se disponível; caso contrário, monta a URL
                if (audioUrl != null && !audioUrl.isEmpty()) {
                    if (!audioUrl.startsWith("http")) {
                        audioUrl = API_BASE_URL + audioUrl;
                    }
                    result.put("audioUrl", audioUrl);
                } else if (audioPath != null && !audioPath.isEmpty()) {
                    result.put("audioUrl", API_BASE_URL + "/get_file?path=" +
                            URLEncoder.encode(audioPath, StandardCharsets.UTF_8) +
                            "&name=" + URLEncoder.encode(song + ".mp3", StandardCharsets.UTF_8));
                } else {
                    log.error("A resposta da API não contém a URL do áudio");
                    return null;
                }

                if (lyricUrl != null && !lyricUrl.isEmpty()) {
                    if (!lyricUrl.startsWith("http")) {
                        lyricUrl = API_BASE_URL + lyricUrl;
                    }
                    result.put("lyricUrl", lyricUrl);
                } else if (lyricPath != null && !lyricPath.isEmpty()) {
                    result.put("lyricUrl", API_BASE_URL + "/get_file?path=" +
                            URLEncoder.encode(lyricPath, StandardCharsets.UTF_8) +
                            "&name=" + URLEncoder.encode(song + ".lrc", StandardCharsets.UTF_8));
                } else {
                    // A letra é opcional; a reprodução funciona mesmo sem ela
                    log.warn("A resposta da API não contém a URL da letra");
                }

                return result;
            }
        } catch (Exception e) {
            log.error("Erro ao obter as informações da música", e);
            return null;
        }
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
                log.info("Arquivo salvo em: {}", outputPath);

                return outputPath;
            }
        } catch (Exception e) {
            log.error("Erro ao baixar o arquivo", e);
            return null;
        }
    }

    /**
     * Analisa a letra no formato LRC
     */
    private List<LyricLine> parseLyrics(String lyricUrl) {
        List<LyricLine> result = new ArrayList<>();

        if (lyricUrl == null || lyricUrl.isEmpty()) {
            log.warn("URL da letra vazia, não é possível analisar a letra");
            return result;
        }

        try {

            // Envia a requisição usando OkHttp3
            Request request = new Request.Builder()
                    .url(lyricUrl)
                    .get()
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.error("Falha ao obter a letra, código de resposta: {}", response.code());
                    return result;
                }

                String responseBody = response.body().string();

                // Expressão regular do marcador de tempo LRC: [mm:ss.xx]
                Pattern pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.*)");

                // Processa cada linha usando a Stream API
                return responseBody.lines()
                        .map(pattern::matcher)
                        .filter(Matcher::find)
                        .map(matcher -> {
                            int minutes = Integer.parseInt(matcher.group(1));
                            int seconds = Integer.parseInt(matcher.group(2));
                            int hundredths = Integer.parseInt(matcher.group(3));

                            // Calcula o tempo em milissegundos
                            int timeMs = (minutes * 60 * 1000) + (seconds * 1000) + (hundredths * 10);
                            String text = matcher.group(4).trim();

                            return new LyricLine(timeMs, text);
                        })
                        .sorted(Comparator.comparingLong(LyricLine::timeMs))
                        .toList();
            }

        } catch (Exception e) {
            log.error("Erro ao analisar a letra", e);
        }

        return result;
    }

}

/**
 * Estrutura de dados de linha de letra - usa o tipo Record do JDK 16+
 */
record LyricLine(int timeMs, String text) {
}
