package com.xiaozhi.common.config;

import com.xiaozhi.utils.AudioUtils;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * Configuração de diretórios de modelos e bibliotecas nativas em tempo de execução.
 * Por padrão mantém a convenção de caminho relativo atual; em produção pode ser alterado para caminho absoluto para reduzir a dependência do diretório de trabalho.
 */
@Configuration
@ConfigurationProperties(prefix = "xiaozhi.runtime")
@Data
public class RuntimePathConfig {

    /** Diretório local de bibliotecas nativas */
    private String nativeLibDir = "lib";

    /** Diretório do modelo Vosk */
    private String voskModelDir = "models/vosk-model";

    /** Diretório raiz dos modelos TTS Sherpa-ONNX */
    private String ttsModelsDir = "models/tts";

    /** Diretório de saída de áudio (gravações de diálogo, saída TTS), deve terminar com / */
    private String audioDir = "audio/";

    /** Diretório de arquivos de música */
    private String musicDir = "uploads/music";

    /** Diretório de avatares */
    private String avatarDir = "avatar";

    @PostConstruct
    void initStaticPaths() {
        AudioUtils.AUDIO_PATH = audioDir;
    }

    public Path resolveNativeLibDir() {
        return Path.of(nativeLibDir).toAbsolutePath().normalize();
    }

    public Path resolveVoskModelDir() {
        return Path.of(voskModelDir).toAbsolutePath().normalize();
    }

    public Path resolveTtsModelsDir() {
        return Path.of(ttsModelsDir).toAbsolutePath().normalize();
    }

    public Path resolveAudioDir() {
        return Path.of(audioDir).toAbsolutePath().normalize();
    }

    public Path resolveMusicDir() {
        return Path.of(musicDir).toAbsolutePath().normalize();
    }

    public Path resolveAvatarDir() {
        return Path.of(avatarDir).toAbsolutePath().normalize();
    }
}
