package com.xiaozhi.utils;

import org.gagravarr.ogg.*;
import org.gagravarr.opus.*;
import javazoom.jl.decoder.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AudioUtils {
    /** Inicializado na inicialização por {@link com.xiaozhi.common.config.RuntimePathConfig} */
    public static String AUDIO_PATH;

    public static final int AUDIO_RETENTION_DAYS = 30;
    public static final int FRAME_SIZE = 960;
    public static final int SAMPLE_RATE = 16000; // Taxa de amostragem
    public static final int CHANNELS = 1; // Mono
    public static final int BITRATE = 48000; // Taxa de bits de 48kbps (alta qualidade, próxima da transparência)
    public static final int SAMPLE_FORMAT = 1; // AV_SAMPLE_FMT_S16, PCM de 16 bits
    public static final int BUFFER_SIZE = 512; // Tamanho da janela
    public static final int OPUS_FRAME_DURATION_MS = 60; // Duração do frame OPUS (milissegundos)

    /**
     * Exclui o arquivo (trata exceções silenciosamente)
     */
    public static void deleteFile(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(path));
        } catch (IOException e) {
            log.warn("Falha ao excluir o arquivo: {}", path, e);
        }
    }

    public static void deleteDirectory(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    log.warn("Falha ao excluir: {}", path, e);
                }
            });
        } catch (IOException e) {
            log.warn("Falha ao excluir o diretório: {}", dir, e);
        }
    }

    public static String saveAsWav(byte[] audio) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileName = uuid + ".wav";
        Path path = Path.of(AUDIO_PATH , fileName);
        saveAsWav(path, audio);
        return AUDIO_PATH + fileName;
    }
    /**
     * Salva os dados de áudio brutos como um arquivo WAV
     *
     * @param audioData dados de áudio
     * @return nome do arquivo
     */
    public static void saveAsWav(Path path, byte[] audioData) {

        // Parâmetros do arquivo WAV
        int bitsPerSample = 16; // Amostragem de 16 bits

        try {
            // Garante que o diretório de áudio exista
            Files.createDirectories(path.getParent());

            try (FileOutputStream fos = new FileOutputStream(path.toFile());
                 DataOutputStream dos = new DataOutputStream(fos)) {

                // Escreve o cabeçalho do arquivo WAV
                // Cabeçalho RIFF
                dos.writeBytes("RIFF");
                dos.writeInt(Integer.reverseBytes(36 + audioData.length)); // Tamanho do arquivo
                dos.writeBytes("WAVE");

                // Subchunk fmt
                dos.writeBytes("fmt ");
                dos.writeInt(Integer.reverseBytes(16)); // Tamanho do subchunk
                dos.writeShort(Short.reverseBytes((short) 1)); // Formato de áudio (1 = PCM)
                dos.writeShort(Short.reverseBytes((short) CHANNELS)); // Número de canais
                dos.writeInt(Integer.reverseBytes(SAMPLE_RATE)); // Taxa de amostragem
                dos.writeInt(Integer.reverseBytes(SAMPLE_RATE * CHANNELS * bitsPerSample / 8)); // Taxa de bytes
                dos.writeShort(Short.reverseBytes((short) (CHANNELS * bitsPerSample / 8))); // Alinhamento de bloco
                dos.writeShort(Short.reverseBytes((short) bitsPerSample)); // Bits por amostra

                // Subchunk data
                dos.writeBytes("data");
                dos.writeInt(Integer.reverseBytes(audioData.length)); // Tamanho dos dados

                // Escreve os dados de áudio
                dos.write(audioData);
            }
        } catch (IOException e) {
            log.error("Erro ao escrever o arquivo WAV", e);
        }
    }

    /**
     * Mescla múltiplos arquivos de áudio em um único arquivo WAV
     * Formatos suportados para mesclagem: wav, mp3, pcm
     *
     * @param path caminho do arquivo WAV de saída
     * @param audioPaths lista de caminhos dos arquivos de áudio a mesclar
     */
    public static void mergeAudioFiles(Path path, List<String> audioPaths) {
        if (audioPaths.size() == 1) {
            // Arquivo único é movido diretamente, evitando leitura e recodificação desnecessárias
            try {
                var sourcePath = Paths.get(audioPaths.getFirst());
                if (!sourcePath.isAbsolute()) {
                    sourcePath = Paths.get(AUDIO_PATH, audioPaths.getFirst());
                }
                Files.createDirectories(path.getParent());
                Files.move(sourcePath, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (Exception e) {
                log.warn("Falha ao mover o arquivo, retornando à lógica de mesclagem: {}", e.getMessage());
            }
        }
//        var uuid = UUID.randomUUID().toString().replace("-", "");
//        var outputFileName = uuid + ".wav";
//        var outputPath = Paths.get(AUDIO_PATH, outputFileName).toString();

        try {
            // Garante que o diretório de áudio exista
            Files.createDirectories(path.getParent());
            // Calcula o tamanho total de todos os dados PCM
            var totalPcmSize = 0L;
            var audioChunks = new ArrayList<byte[]>();
            for (var audioPath : audioPaths) {
                var fullPath = audioPath.startsWith(AUDIO_PATH) ? audioPath : AUDIO_PATH + audioPath;
                byte[] pcmData;

                pcmData = readAsPcm(fullPath);
                
                totalPcmSize += pcmData.length;
                audioChunks.add(pcmData);
            }

            // Cria o arquivo WAV de saída
            try (FileOutputStream fos = new FileOutputStream(path.toFile());
                 DataOutputStream dos = new DataOutputStream(fos)) {

                // Escreve o cabeçalho do arquivo WAV
                int bitsPerSample = 16; // Amostragem de 16 bits

                // Cabeçalho RIFF
                dos.writeBytes("RIFF");
                dos.writeInt(Integer.reverseBytes(36 + (int) totalPcmSize)); // Tamanho do arquivo
                dos.writeBytes("WAVE");

                // Subchunk fmt
                dos.writeBytes("fmt ");
                dos.writeInt(Integer.reverseBytes(16)); // Tamanho do subchunk
                dos.writeShort(Short.reverseBytes((short) 1)); // Formato de áudio (1 = PCM)
                dos.writeShort(Short.reverseBytes((short) CHANNELS)); // Número de canais
                dos.writeInt(Integer.reverseBytes(SAMPLE_RATE)); // Taxa de amostragem
                dos.writeInt(Integer.reverseBytes(SAMPLE_RATE * CHANNELS * bitsPerSample / 8)); // Taxa de bytes
                dos.writeShort(Short.reverseBytes((short) (CHANNELS * bitsPerSample / 8))); // Alinhamento de bloco
                dos.writeShort(Short.reverseBytes((short) bitsPerSample)); // Bits por amostra

                // Subchunk data
                dos.writeBytes("data");
                dos.writeInt(Integer.reverseBytes((int) totalPcmSize)); // Tamanho dos dados

                // Escreve os dados PCM de cada arquivo, um após o outro
                for (var pcmData : audioChunks) {
                    dos.write(pcmData);
                }
            }
            // Como o cache de áudio é utilizado, não é necessário excluir os arquivos já mesclados.
            // for (var audioPath : audioPaths) {
            //     var fullPath = audioPath.startsWith(AUDIO_PATH) ? audioPath : AUDIO_PATH + audioPath;
            //     Files.deleteIfExists(Paths.get(fullPath));
            // }

        } catch (Exception e) {
            log.error("Erro ao mesclar arquivos de áudio", e);
        }
    }

    /**
     * Extrai dados PCM de um array de bytes WAV
     *
     * @param wavData array de bytes bruto do arquivo WAV
     * @return array de bytes com os dados PCM
     */
    public static byte[] wavToPcm(byte[] wavData) throws IOException {
        if (wavData == null || wavData.length < 44) {
            throw new IOException("Dados WAV inválidos");
        }

        if (wavData[0] != 'R' || wavData[1] != 'I' || wavData[2] != 'F' || wavData[3] != 'F' ||
                wavData[8] != 'W' || wavData[9] != 'A' || wavData[10] != 'V' || wavData[11] != 'E') {
            throw new IOException("Não é um formato de arquivo WAV válido");
        }

        int dataOffset = -1;
        for (int i = 12; i < wavData.length - 4; i++) {
            if (wavData[i] == 'd' && wavData[i + 1] == 'a' && wavData[i + 2] == 't' && wavData[i + 3] == 'a') {
                dataOffset = i + 8;
                break;
            }
        }

        if (dataOffset == -1) {
            throw new IOException("Subchunk data não encontrado no arquivo WAV");
        }

        int dataSize = wavData.length - dataOffset;
        byte[] pcmData = new byte[dataSize];
        System.arraycopy(wavData, dataOffset, pcmData, 0, dataSize);
        return pcmData;
    }

    /**
     * Extrai dados PCM de um arquivo WAV
     *
     * @param wavPath caminho do arquivo WAV
     * @return array de bytes com os dados PCM
     */
    public static byte[] wavToPcm(String wavPath) throws IOException {

        byte[] wavData = Files.readAllBytes(Paths.get(wavPath));

        if (wavData == null || wavData.length < 44) { // O cabeçalho WAV tem pelo menos 44 bytes
            throw new IOException("Dados WAV inválidos");
        }

        // Verifica a assinatura do arquivo WAV
        if (wavData[0] != 'R' || wavData[1] != 'I' || wavData[2] != 'F' || wavData[3] != 'F' ||
                wavData[8] != 'W' || wavData[9] != 'A' || wavData[10] != 'V' || wavData[11] != 'E') {
            throw new IOException("Não é um formato de arquivo WAV válido");
        }

        // Localiza o subchunk data
        int dataOffset = -1;
        for (int i = 12; i < wavData.length - 4; i++) {
            if (wavData[i] == 'd' && wavData[i + 1] == 'a' && wavData[i + 2] == 't' && wavData[i + 3] == 'a') {
                dataOffset = i + 8; // Pula o campo "data" e o campo de tamanho dos dados
                break;
            }
        }

        if (dataOffset == -1) {
            throw new IOException("Subchunk data não encontrado no arquivo WAV");
        }

        // Calcula o tamanho dos dados PCM
        int dataSize = wavData.length - dataOffset;

        // Extrai os dados PCM
        byte[] pcmData = new byte[dataSize];
        System.arraycopy(wavData, dataOffset, pcmData, 0, dataSize);

        return pcmData;
    }

    /**
     * Verifica se o arquivo está no formato OGG Opus (extensão .ogg ou .opus)
     */
    public static boolean isOggOpus(String filePath) {
        String lower = filePath.toLowerCase();
        return lower.endsWith(".ogg") || lower.endsWith(".opus");
    }

    /**
     * Lê dados PCM de um arquivo, tratando automaticamente os formatos WAV e MP3
     *
     * @param filePath caminho do arquivo de áudio
     * @return array de bytes com os dados PCM
     */
    public static byte[] readAsPcm(String filePath) throws IOException {
        if (filePath.toLowerCase().endsWith(".wav")) {
            return wavToPcm(filePath);
        } else if (filePath.toLowerCase().endsWith(".mp3")) {
            return mp3ToPcm(filePath);
        } else if (filePath.toLowerCase().endsWith(".pcm")) {
            // Lê o arquivo PCM diretamente
            return Files.readAllBytes(Paths.get(filePath));
        } else if (isOggOpus(filePath)) {
            return opusToPcm(filePath);
        } else {
            throw new IOException("Formato de áudio não suportado: " + filePath);
        }
    }

    /**
     * Lê dados PCM de um arquivo e retorna divididos em blocos do tamanho de um frame Opus (3840 bytes = 60ms).
     * Evita manter o arquivo de áudio inteiro como um único byte[], reduzindo o pico de memória.
     *
     * @param filePath caminho do arquivo de áudio
     * @return lista de blocos de dados PCM, cada um com 3840 bytes (o último pode ser menor)
     */
    public static List<byte[]> readAsPcmChunks(String filePath) throws IOException {
        byte[] pcmData = readAsPcm(filePath);
        // Tamanho de PCM correspondente a cada frame Opus: 60ms × 16000Hz × 16bit / 8 = 3840 bytes
        int chunkSize = OPUS_FRAME_DURATION_MS * SAMPLE_RATE * 2 / 1000; // 3840
        List<byte[]> chunks = new ArrayList<>();
        for (int i = 0; i < pcmData.length; i += chunkSize) {
            int end = Math.min(i + chunkSize, pcmData.length);
            byte[] chunk = new byte[end - i];
            System.arraycopy(pcmData, i, chunk, 0, end - i);
            chunks.add(chunk);
        }
        return chunks;
    }

    /**
     * Lê dados de frames Opus de um arquivo, tratando automaticamente diversos formatos de áudio
     *
     * @param filePath caminho do arquivo de áudio
     * @return lista de frames Opus
     */
    public static List<byte[]> readAsOpus(String filePath) throws IOException {
        if (isOggOpus(filePath)) {
            // Lê o arquivo OGG Opus diretamente
            return readOpus(new File(filePath));
        } else {
            // Outros formatos são primeiro convertidos para PCM, depois codificados em Opus
            byte[] pcmData = readAsPcm(filePath);
            return new OpusProcessor().pcmToOpus(pcmData, false);
        }
    }

    /**
     * Reamostra os dados PCM de uma taxa de amostragem para outra (interpolação linear)
     * Adequado para cenários de streaming em tempo real (operação puramente em memória, sem latência de I/O)
     *
     * @param pcmData      dados PCM originais (16 bits, com sinal, little-endian)
     * @param fromRate     taxa de amostragem de origem (Hz), por exemplo 24000
     * @param toRate       taxa de amostragem de destino (Hz), por exemplo 16000
     * @return dados PCM reamostrados
     */
    public static byte[] resamplePcm(byte[] pcmData, int fromRate, int toRate) {
        if (fromRate == toRate || pcmData == null || pcmData.length == 0) {
            return pcmData;
        }

        // Cada amostra tem 2 bytes (16 bits)
        int inputSamples = pcmData.length / 2;
        int outputSamples = (int) Math.ceil((long) inputSamples * toRate / fromRate);
        byte[] output = new byte[outputSamples * 2];

        for (int i = 0; i < outputSamples; i++) {
            // Posição de amostra de origem (ponto flutuante)
            double srcPos = (double) i * fromRate / toRate;
            int srcIndex = (int) srcPos;
            double frac = srcPos - srcIndex;

            // Lê as duas amostras adjacentes (16 bits, com sinal, little-endian)
            short s0 = readShortLE(pcmData, srcIndex);
            short s1 = (srcIndex + 1 < inputSamples) ? readShortLE(pcmData, srcIndex + 1) : s0;

            // Interpolação linear
            short interpolated = (short) Math.round(s0 + frac * (s1 - s0));

            // Escreve a saída (little-endian)
            output[i * 2] = (byte) (interpolated & 0xFF);
            output[i * 2 + 1] = (byte) ((interpolated >> 8) & 0xFF);
        }

        return output;
    }

    /**
     * Converte amostras PCM float[] (intervalo -1.0 a 1.0) em byte[] PCM de 16 bits (little-endian)
     */
    public static byte[] floatToPcm16(float[] samples) {
        ByteBuffer buffer = ByteBuffer.allocate(samples.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (float sample : samples) {
            float clamped = Math.max(-1.0f, Math.min(1.0f, sample));
            buffer.putShort((short) (clamped * 32767));
        }
        return buffer.array();
    }

    private static short readShortLE(byte[] data, int index) {
        int byteIndex = index * 2;
        if (byteIndex + 1 >= data.length) return 0;
        return (short) ((data[byteIndex] & 0xFF) | (data[byteIndex + 1] << 8));
    }

    /**
     * Converte MP3 para o formato PCM
     *
     * @param mp3Path caminho do arquivo MP3
     * @return array de bytes com os dados PCM (16kHz, 16 bits, mono)
     */
    public static byte[] mp3ToPcm(String mp3Path) throws IOException {
        try (FileInputStream fis = new FileInputStream(mp3Path)) {
            Bitstream bitstream = new Bitstream(fis);
            Decoder decoder = new Decoder();
            ByteArrayOutputStream pcmOut = new ByteArrayOutputStream();
            int mp3SampleRate = -1;

            Header header;
            while ((header = bitstream.readFrame()) != null) {
                SampleBuffer output =
                        (SampleBuffer) decoder.decodeFrame(header, bitstream);
                if (mp3SampleRate < 0) {
                    mp3SampleRate = output.getSampleFrequency();
                }
                short[] samples = output.getBuffer();
                int len = output.getBufferLength();
                byte[] frameBytes = new byte[len * 2];
                for (int i = 0; i < len; i++) {
                    frameBytes[i * 2] = (byte) (samples[i] & 0xFF);
                    frameBytes[i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xFF);
                }
                pcmOut.write(frameBytes);
                bitstream.closeFrame();
            }
            bitstream.close();

            byte[] pcmData = pcmOut.toByteArray();
            // Se a taxa de amostragem do MP3 não for 16kHz, realiza a reamostragem
            if (mp3SampleRate > 0 && mp3SampleRate != SAMPLE_RATE) {
                pcmData = resamplePcm(pcmData, mp3SampleRate, SAMPLE_RATE);
            }
            return pcmData;
        } catch (BitstreamException | DecoderException e) {
            throw new IOException("Falha ao decodificar MP3 com o JLayer: " + e.getMessage(), e);
        }
    }

    /**
     * Mescla múltiplos frames PCM em um único array de bytes contínuo
     */
    public static byte[] joinPcmFrames(List<byte[]> pcmFrames) {
        if (pcmFrames == null || pcmFrames.isEmpty()) {
            return new byte[0];
        }
        int totalSize = pcmFrames.stream().mapToInt(frame -> frame.length).sum();
        byte[] fullPcmData = new byte[totalSize];
        int offset = 0;
        for (byte[] frame : pcmFrames) {
            System.arraycopy(frame, 0, fullPcmData, offset, frame.length);
            offset += frame.length;
        }
        return fullPcmData;
    }

    /**
     * Lê um arquivo Ogg Opus padrão e converte para dados PCM
     *
     * @param opusFilePath caminho do arquivo Ogg Opus
     * @return dados PCM
     * @throws IOException exceção de leitura do arquivo
     */
    public static byte[] opusToPcm(String opusFilePath) throws IOException {
        // Lê os frames Opus
        List<byte[]> opusFrames = readOpus(new File(opusFilePath));

        if (opusFrames.isEmpty()) {
            throw new IOException("Arquivo Opus vazio ou falha na leitura");
        }

        OpusProcessor opusProcessor = new OpusProcessor();

        // Decodifica todos os frames para PCM
        List<byte[]> pcmChunks = new ArrayList<>();
        for (byte[] opusFrame : opusFrames) {
            try {
                byte[] pcmData = opusProcessor.opusToPcm(opusFrame);
                if (pcmData != null && pcmData.length > 0) {
                    pcmChunks.add(pcmData);
                }
            } catch (Exception e) {
                // Ignora silenciosamente frames corrompidos
            }
        }

        if (pcmChunks.isEmpty()) {
            throw new IOException("Nenhum dado PCM válido");
        }

        // Calcula o tamanho total e mescla todos os dados PCM
        int totalSize = pcmChunks.stream().mapToInt(chunk -> chunk.length).sum();
        byte[] result = new byte[totalSize];

        int offset = 0;
        for (byte[] chunk : pcmChunks) {
            System.arraycopy(chunk, 0, result, offset, chunk.length);
            offset += chunk.length;
        }

        return result;
    }

    /**
     * Salva os dados dos frames Opus como um arquivo Ogg Opus padrão
     *
     * @param opusFrames lista de dados de frames Opus
     * @param filePath caminho do arquivo a salvar
     * @throws IOException exceção de operação de arquivo
     */
    public static void saveAsOpus(List<byte[]> opusFrames, String filePath) throws IOException {
        if (opusFrames == null || opusFrames.isEmpty()) {
            return;
        }

        // Cria o objeto OpusInfo, definindo os parâmetros básicos
        OpusInfo oi = new OpusInfo();
        oi.setSampleRate(SAMPLE_RATE);
        oi.setNumChannels(CHANNELS);
        oi.setPreSkip(0);

        // Cria o objeto OpusTags
        OpusTags ot = new OpusTags();
        ot.addComment("TITLE", "Xiaozhi TTS Audio");
        ot.addComment("ARTIST", "Xiaozhi ESP32 Server");

        // Usa try-with-resources para gerenciar todos os recursos
        try (FileOutputStream fos = new FileOutputStream(filePath);
             OpusFile opusFile = new OpusFile(fos, oi, ot)) {

            // Escreve cada frame Opus
            for (byte[] frame : opusFrames) {
                opusFile.writeAudioData(new OpusAudioData(frame));
            }
        }
    }

    /**
     * Obtém a duração do arquivo de áudio
     *
     * @param path caminho do arquivo de áudio
     * @return duração (segundos); retorna -1 em caso de falha
     */
    public static double getAudioDuration(Path path) {
        String pathStr = path.toString().toLowerCase();
        try {
            if (pathStr.endsWith(".wav")) {
                return getWavDuration(path);
            } else if (isOggOpus(pathStr)) {
                return getOpusDuration(path);
            } else if (pathStr.endsWith(".mp3")) {
                return getMp3Duration(path);
            } else if (pathStr.endsWith(".pcm")) {
                long fileSize = Files.size(path);
                return (double) fileSize / (SAMPLE_RATE * CHANNELS * 2);
            }
        } catch (Exception e) {
            log.debug("Falha ao obter a duração do áudio: {}", path, e);
        }
        return -1;
    }

    private static double getWavDuration(Path path) throws IOException {
        byte[] header = new byte[44];
        try (InputStream is = Files.newInputStream(path)) {
            if (is.read(header) < 44) return -1;
        }
        // Lê a taxa de amostragem (bytes 24-27, little-endian)
        int sampleRate = (header[24] & 0xFF) | ((header[25] & 0xFF) << 8)
                | ((header[26] & 0xFF) << 16) | ((header[27] & 0xFF) << 24);
        // Lê a taxa de bytes (bytes 28-31, little-endian)
        int byteRate = (header[28] & 0xFF) | ((header[29] & 0xFF) << 8)
                | ((header[30] & 0xFF) << 16) | ((header[31] & 0xFF) << 24);
        if (byteRate <= 0) return -1;
        long dataSize = Files.size(path) - 44;
        return (double) dataSize / byteRate;
    }

    private static double getOpusDuration(Path path) throws IOException {
        List<byte[]> frames = readOpus(path.toFile());
        if (frames.isEmpty()) return -1;
        // Cada frame tem 60ms (OPUS_FRAME_DURATION_MS)
        return frames.size() * OPUS_FRAME_DURATION_MS / 1000.0;
    }

    private static double getMp3Duration(Path path) throws IOException {
        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            Bitstream bitstream = new Bitstream(fis);
            double totalSeconds = 0;
            Header header;
            while ((header = bitstream.readFrame()) != null) {
                totalSeconds += header.ms_per_frame() / 1000.0;
                bitstream.closeFrame();
            }
            bitstream.close();
            return totalSeconds;
        } catch (BitstreamException e) {
            throw new IOException("Falha ao ler a duração do MP3 com o JLayer", e);
        }
    }

    /**
     * Lê um arquivo Ogg Opus padrão
     *
     * @param file arquivo Ogg Opus
     * @return lista de frames Opus
     * @throws IOException exceção de leitura do arquivo
     */
    public static List<byte[]> readOpus(File file) {
        List<byte[]> frames = new ArrayList<>();

        if (file.length() <= 0) {
            return frames;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            OggFile oggFile = new OggFile(fis);
            try (OpusFile opusFile = new OpusFile(oggFile)) {
                OpusAudioData audioData;
                while ((audioData = opusFile.getNextAudioPacket()) != null) {
                    byte[] frameData = audioData.getData();
                    if (frameData != null && frameData.length > 0) {
                        frames.add(frameData);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Falha ao ler o arquivo Ogg Opus: {}", file.getAbsolutePath(), e);
            return frames;
        }

        return frames;
    }

    /**
     * Lê frames de áudio no formato Ogg Opus a partir de um InputStream (usado para interpretar arrays de bytes vindos de armazenamento em nuvem).
     *
     * @param inputStream stream de dados Ogg Opus
     * @return lista de frames Opus
     */
    public static List<byte[]> readOpus(InputStream inputStream) {
        List<byte[]> frames = new ArrayList<>();
        try {
            OggFile oggFile = new OggFile(inputStream);
            try (OpusFile opusFile = new OpusFile(oggFile)) {
                OpusAudioData audioData;
                while ((audioData = opusFile.getNextAudioPacket()) != null) {
                    byte[] frameData = audioData.getData();
                    if (frameData != null && frameData.length > 0) {
                        frames.add(frameData);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Falha ao ler Ogg Opus a partir do stream de entrada", e);
        }
        return frames;
    }
}