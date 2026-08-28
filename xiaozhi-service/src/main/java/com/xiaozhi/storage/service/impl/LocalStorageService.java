package com.xiaozhi.storage.service.impl;

import com.xiaozhi.storage.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import lombok.extern.slf4j.Slf4j;
/**
 * Implementação de armazenamento de arquivos local
 */
@Slf4j
@Component
public class LocalStorageService implements StorageService {

    @Value("${xiaozhi.upload-path:uploads}")
    private String baseDir;

    @Override

    public String upload(MultipartFile file, String relativePath, String fileName) throws IOException {
        String fullPath = baseDir;
        if (!relativePath.isEmpty()) {
            fullPath = fullPath + File.separator + relativePath;
        }

        File directory = new File(fullPath);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                throw new IOException("Não foi possível criar o diretório: " + fullPath);
            }
        }

        File destFile = new File(directory, fileName);
        try (FileOutputStream fos = new FileOutputStream(destFile);
            InputStream inputStream = file.getInputStream()) {
            inputStream.transferTo(fos);
        }

        // Retorna o caminho relativo (usando sempre barra normal, para facilitar o acesso via URL)
        String relativeFilePath = baseDir + File.separator + relativePath + File.separator + fileName;
        return relativeFilePath.replace(File.separator, "/");
    }

    @Override

    public String upload(Path localFile, String objectKey) throws IOException {
        Path target = Path.of(objectKey);
        if (!localFile.equals(target)) {
            Files.createDirectories(target.getParent());
            Files.move(localFile, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target.toString();
    }

    @Override
    public byte[] download(String storedPath) {
        try {
            Path path = Path.of(storedPath);
            return Files.exists(path) ? Files.readAllBytes(path) : null;
        } catch (Exception e) {
            log.warn("Falha ao ler arquivo local: {}", storedPath, e);
            return null;
        }
    }

    @Override
    public void remove(String storedPath) {
        if (storedPath == null) return;
        try {
            Files.deleteIfExists(Path.of(storedPath));
        } catch (Exception e) {
            log.warn("Falha ao excluir arquivo local: {}", storedPath, e);
        }
    }

    @Override
    public boolean exists(String storedPath) {
        return storedPath != null && Files.exists(Path.of(storedPath));
    }

    @Override
    public String getProvider() {
        return "local";
    }
}
