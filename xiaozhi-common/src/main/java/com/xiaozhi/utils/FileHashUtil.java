package com.xiaozhi.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Classe utilitária de cálculo de hash de arquivo
 * <p>
 * Esta classe é do tipo final e possui um construtor privado, para impedir que seja herdada ou instanciada.
 * Todos os métodos são estáticos, chamados diretamente pelo nome da classe.
 */
public final class FileHashUtil {

    private static final int BUFFER_SIZE = 8192; // Tamanho do buffer de 8KB

    /**
     * Construtor privado, impede que esta classe utilitária seja instanciada.
     */
    private FileHashUtil() {
        // Lançar uma exceção é uma implementação mais rigorosa do padrão singleton, garantindo que ninguém consiga criar instâncias via reflexão ou outros meios
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Calcula o hash de um MultipartFile (recomenda-se usar SHA-256).
     *
     * @param file arquivo do qual se deseja calcular o hash
     * @return hash SHA-256 do arquivo, representado como string hexadecimal
     */
    public static String calculateSha256(MultipartFile file) {
        return calculateHash(file, "SHA-256");
    }

    /**
     * Calcula o hash MD5 de um MultipartFile.
     *
     * @param file arquivo do qual se deseja calcular o hash
     * @return hash MD5 do arquivo, representado como string hexadecimal
     */
    public static String calculateMd5(MultipartFile file) {
        return calculateHash(file, "MD5");
    }

    /**
     * Método central genérico para calcular o hash de um MultipartFile.
     *
     * @param file      arquivo do qual se deseja calcular o hash
     * @param algorithm algoritmo de hash, por exemplo "MD5", "SHA-1", "SHA-256"
     * @return hash do arquivo, representado como string hexadecimal
     * @throws RuntimeException se o arquivo estiver vazio, o algoritmo não for suportado ou ocorrer um erro de I/O
     */
    public static String calculateHash(MultipartFile file, String algorithm) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo não pode ser vazio.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            try (InputStream is = file.getInputStream()) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hashBytes = digest.digest();
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo de hash não suportado: " + algorithm, e);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao calcular o hash do arquivo: " + file.getOriginalFilename(), e);
        }
    }

    /**
     * Método auxiliar para converter um array de bytes em uma string hexadecimal.
     *
     * @param hash array de bytes resultante do cálculo do hash
     * @return string em representação hexadecimal
     */
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
