package com.xiaozhi.task;

import com.xiaozhi.utils.AudioUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

/**
 * Tarefa agendada de limpeza de arquivos de áudio
 *
 * À 1h da manhã: limpa os diretórios de gravações de conversa com mais de retentionDays dias
 */
@Slf4j
@Component
public class AudioCleanupTask {

    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanupExpiredAudio() {
        Path audioDir = Path.of(AudioUtils.AUDIO_PATH);
        if (!Files.exists(audioDir)) {
            return;
        }

        int retentionDays = AudioUtils.AUDIO_RETENTION_DAYS;
        log.info("========== Iniciando tarefa de limpeza de arquivos de áudio (mantendo {} dias) ==========", retentionDays);
        LocalDate expireDate = LocalDate.now().minusDays(retentionDays);
        int deletedDirs = 0;

        try (Stream<Path> dirs = Files.list(audioDir).filter(Files::isDirectory)) {
            for (Path dir : dirs.toList()) {
                String dirName = dir.getFileName().toString();
                try {
                    LocalDate dirDate = LocalDate.parse(dirName, DateTimeFormatter.ISO_LOCAL_DATE);
                    if (!dirDate.isAfter(expireDate)) {
                        AudioUtils.deleteDirectory(dir);
                        deletedDirs++;
                    }
                } catch (DateTimeParseException ignored) {
                    // Ignora diretórios que não estão no formato de data
                }
            }
        } catch (IOException e) {
            log.error("Falha na execução da tarefa de limpeza de arquivos de áudio", e);
        }

        log.info("========== Limpeza de arquivos de áudio concluída, {} diretório(s) limpo(s) ==========", deletedDirs);
    }
}
