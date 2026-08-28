package com.xiaozhi.utils;

/**
 * Classe utilitária de comparação de números de versão
 */
public class VersionUtils {

    /**
     * Compara dois números de versão
     * @param version1 versão 1
     * @param version2 versão 2
     * @return retorna número negativo se version1 < version2, 0 se forem iguais, número positivo se version1 > version2
     */
    public static int compareVersion(String version1, String version2) {
        if (version1 == null || version2 == null) {
            throw new IllegalArgumentException("O número de versão não pode ser vazio");
        }

        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");

        int maxLength = Math.max(v1Parts.length, v2Parts.length);

        for (int i = 0; i < maxLength; i++) {
            int v1Part = i < v1Parts.length ? parseVersionPart(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ? parseVersionPart(v2Parts[i]) : 0;

            if (v1Part < v2Part) {
                return -1;
            } else if (v1Part > v2Part) {
                return 1;
            }
        }

        return 0;
    }

    /**
     * Analisa cada parte do número de versão (suporta formatos como 1.2.0-beta)
     */
    private static int parseVersionPart(String part) {
        try {
            // Se contiver caracteres não numéricos (como em 1.2.0-beta), pega apenas a parte numérica
            String numericPart = part.replaceAll("[^0-9].*", "");
            return numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
