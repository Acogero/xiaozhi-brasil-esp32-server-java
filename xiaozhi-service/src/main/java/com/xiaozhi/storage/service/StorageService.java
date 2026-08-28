package com.xiaozhi.storage.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface do serviço de armazenamento de objetos.
 * A implementação atualmente ativa é obtida via {@link StorageServiceFactory}.
 * <p>
 * Suporta armazenamento de arquivos local e múltiplos provedores de OSS na nuvem (Tencent Cloud COS, Alibaba Cloud OSS).
 * A configuração é gerenciada pela tabela sys_config (configType="oss"); o administrador pode escolher o modo de armazenamento na página de configurações do frontend.
 */
public interface StorageService {

    /** Limite padrão de tamanho de arquivo: 50MB */
    long DEFAULT_MAX_SIZE = 50 * 1024 * 1024;

    /**
     * Envia um arquivo (lado Web)
     *
     * @param file         Arquivo a ser enviado
     * @param relativePath Caminho relativo (ex.: "avatar/2026/02/28")
     * @param fileName     Nome do arquivo (ex.: "xxxx.png")
     * @return Caminho de acesso (localmente retorna o caminho relativo, na nuvem retorna a URL completa)
     * @throws IOException Falha no envio
     */
    String upload(MultipartFile file, String relativePath, String fileName) throws IOException;

    /**
     * Envia um arquivo local (uso interno, ex.: cache de áudio).
     * O método assume o ciclo de vida do localFile; o chamador não precisa mais se preocupar com o arquivo de origem.
     *
     * @param localFile Arquivo local
     * @param objectKey Chave de armazenamento (localmente como caminho relativo, na nuvem como chave de objeto)
     * @return Caminho de armazenamento (localmente retorna o caminho do arquivo, na nuvem retorna a URL completa)
     * @throws IOException Falha no envio
     */
    String upload(Path localFile, String objectKey) throws IOException;

    /**
     * Baixa o conteúdo do arquivo
     *
     * @param storedPath Caminho retornado por {@link #upload}
     * @return Bytes do arquivo; retorna {@code null} se não existir ou falhar
     */
    byte[] download(String storedPath);

    /**
     * Exclui o arquivo (trata silenciosamente o caso de não existir)
     */
    void remove(String storedPath);

    /**
     * Verifica se o arquivo existe
     */
    boolean exists(String storedPath);

    /**
     * Obtém o nome do provider, usado no roteamento da Factory
     */
    String getProvider();

    /**
     * Verifica o tamanho do arquivo
     */
    static void assertAllowed(MultipartFile file) {
        if (file.getSize() > DEFAULT_MAX_SIZE) {
            throw new IllegalArgumentException("O tamanho do arquivo excede o limite; máximo permitido: " + (DEFAULT_MAX_SIZE / 1024 / 1024) + "MB");
        }
    }
}
