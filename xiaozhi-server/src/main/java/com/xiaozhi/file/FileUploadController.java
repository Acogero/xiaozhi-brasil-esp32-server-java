package com.xiaozhi.file;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.communication.ServerAddressProvider;
import com.xiaozhi.storage.service.StorageService;
import com.xiaozhi.storage.service.StorageServiceFactory;
import com.xiaozhi.utils.FileHashUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
/**
 * Controller de upload de arquivos
 * 
 * @author Joey
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@Tag(name = "Controller de upload de arquivos", description = "Operações relacionadas a upload de arquivos")
public class FileUploadController {
    /** Categorias de tipo de arquivo permitidas (previne path traversal) */
    private static final Set<String> ALLOWED_TYPES = Set.of("common", "image", "audio", "video", "document", "avatar");

    /** Lista de extensões de arquivo permitidas */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg",
            ".mp3", ".wav", ".ogg", ".opus", ".flac", ".aac", ".m4a",
            ".mp4", ".avi", ".mov", ".mkv", ".webm",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".csv",
            ".zip", ".rar", ".7z", ".tar", ".gz",
            ".bin"
    );

    @Resource
    private StorageServiceFactory storageServiceFactory;

    @Autowired
    private ServerAddressProvider serverAddressProvider;

    /**
     * Método genérico de upload de arquivo
     * 
     * @param file arquivo enviado
     * @param type tipo de arquivo (opcional, usado para organizar o armazenamento)
     * @return URL de acesso ao arquivo
     */
    @PostMapping("/upload")
    @ResponseBody
    @SaCheckPermission("system:file:api:upload")
    @Operation(summary = "Upload de arquivo", description = "Se o armazenamento de objetos da Tencent Cloud estiver configurado, o arquivo será armazenado nele por padrão")
    public ApiResponse<?> uploadFile(
            @Parameter(description = "Arquivo enviado") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Tipo de arquivo") @RequestParam(value = "type", required = false, defaultValue = "common") String type) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo enviado não pode estar vazio");
        }

        // Verifica o tamanho do arquivo
        StorageService.assertAllowed(file);

        // Previne path traversal: type precisa estar na lista permitida
        if (!ALLOWED_TYPES.contains(type)) {
            throw new IllegalArgumentException("Categoria de tipo de arquivo não suportada: " + type);
        }

        // Valida o nome do arquivo e a extensão
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("Nome de arquivo inválido ou sem extensão");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Extensão de arquivo não suportada: " + extension);
        }

        // Valida se o tipo MIME é consistente com a extensão
        String contentType = file.getContentType();
        if (contentType != null && !isContentTypeMatchExtension(contentType, extension)) {
            throw new IllegalArgumentException("O tipo MIME do arquivo não corresponde à extensão");
        }

        // Monta o caminho de armazenamento do arquivo, organizado por data e tipo
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = type + "/" + datePath;

        // Gera um nome de arquivo único
        String fileName = UUID.randomUUID().toString().replaceAll("-", "") + extension;

        StorageService storageService = storageServiceFactory.getStorageService();
        String filePathOrUrl;
        try {
            filePathOrUrl = storageService.upload(file, relativePath, fileName);
        } catch (IOException e) {
            log.error("Falha no upload do arquivo: {}", e.getMessage(), e);
            throw new OperationFailedException("Falha no upload do arquivo, tente novamente mais tarde", e);
        }

        log.info("Upload do arquivo concluído ({}): {}", storageService.getProvider(), filePathOrUrl);

        // Calcula o hash do arquivo
        String fileHash = FileHashUtil.calculateSha256(file);

        Map<String, Object> data = new HashMap<>();
        data.put("fileName", originalFilename);
        data.put("newFileName", fileName);
        data.put("hash", fileHash);

        // Verifica se é uma URL completa (armazenamento em nuvem retorna URL https, local retorna caminho relativo)
        if (filePathOrUrl.startsWith("http://") || filePathOrUrl.startsWith("https://")) {
            data.put("url", filePathOrUrl);
        } else {
            String fullUrl = serverAddressProvider.getServerAddress() + "/" + filePathOrUrl;
            data.put("url", fullUrl);
            data.put("relativePath", filePathOrUrl);
        }

        return ApiResponse.success("Upload realizado com sucesso", data);
    }

    /**
     * Verifica se o tipo MIME corresponde à extensão do arquivo
     */
    private boolean isContentTypeMatchExtension(String contentType, String extension) {
        String ct = contentType.toLowerCase();
        return switch (extension) {
            case ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg" -> ct.startsWith("image/");
            case ".mp3", ".wav", ".ogg", ".opus", ".flac", ".aac", ".m4a" -> ct.startsWith("audio/") || ct.equals("application/ogg");
            case ".mp4", ".avi", ".mov", ".mkv", ".webm" -> ct.startsWith("video/");
            case ".pdf" -> ct.equals("application/pdf");
            case ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx" -> ct.startsWith("application/");
            case ".txt", ".csv" -> ct.startsWith("text/");
            case ".zip", ".rar", ".7z", ".tar", ".gz" -> ct.startsWith("application/");
            case ".bin" -> ct.equals("application/octet-stream") || ct.startsWith("application/");
            default -> true;
        };
    }
}
