package com.xiaozhi.music;

import com.xiaozhi.common.config.RuntimePathConfig;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/file")
@Tag(name = "Controller de música", description = "Operações relacionadas a música")
@Slf4j
public class MusicController {

    @Resource
    private RuntimePathConfig runtimePathConfig;

    @PostMapping("/music")
    @ResponseBody
    public String uploadMusic(@Parameter(description = "Arquivo de música enviado") @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "Falha no upload";
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            return "Falha no upload";
        }
        if (!originalFilename.equals("playlist.txt") && !originalFilename.endsWith(".mp3")) {
            return "Falha no upload";
        }
        try {
            Path musicPath = Path.of(runtimePathConfig.getMusicDir());
            Files.createDirectories(musicPath);
            file.transferTo(musicPath.resolve(originalFilename));
            return originalFilename + ", upload realizado com sucesso";
        } catch (IOException e) {
            log.error("Falha no upload", e);
            return "Falha no upload";
        }
    }
}
