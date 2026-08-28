package com.xiaozhi.utils;

import org.springframework.util.Assert;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe utilitária de processamento de emojis
 * Usada para extrair emojis do texto, filtrar emojis e mapeá-los para palavras de emoção
 *
 * @author yuchen
 * @date 2025/4/14
 */
@Slf4j
public class EmojiUtils {

    // Define os intervalos Unicode dos emojis
    private static final int[][] EMOJI_RANGES = {
            { 0x1F600, 0x1F64F }, // Emojis (rostos)
            { 0x1F300, 0x1F5FF }, // Símbolos e pictogramas
            { 0x1F680, 0x1F6FF }, // Transportes e símbolos de mapa
            { 0x1F900, 0x1F9FF }, // Símbolos suplementares
            { 0x1FA70, 0x1FAFF }, // Mais símbolos suplementares
            { 0x2600, 0x26FF }, // Símbolos diversos
            { 0x2700, 0x27BF }, // Símbolos decorativos
            { 0x1F1E6, 0x1F1FF }, // Emojis de bandeiras
            { 0x1F700, 0x1F77F }, // Emojis adicionais
            { 0x20000, 0x2A6DF }, // Símbolos suplementares (mais emojis)
            { 0x1F3FB, 0x1F3FF }, // Modificadores de emoji
            { 0x200D, 0x200D }, // Zero-width joiner
            { 0xFE0F, 0xFE0F }, // Seletor de variação
    };

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[@#№$%&*]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    
    // Padrão de kaomoji - corresponde às combinações de kaomoji comuns
    private static final Pattern KAOMOJI_PATTERN = Pattern.compile(
        "[(（][^)）]{1,10}[)）]|" +  // Ex: (^_^) (・ω・) (≧▽≦)
        "[<＜][^>＞]{1,10}[>＞]|" +  // Ex: <(￣︶￣)>
        "[\\\\¯\\\\*][_-]{1,2}[\\\\¯\\\\*]|" +  // Ex: \_/ \*_*\
        "\\\\o/|" +                 // \o/
        ":-?[)D(]|" +               // :-) :D :-(
        ";-?[)]|" +                 // ;-)
        "=\\\\?[_/]"                // =_= =/=
    );

    // Mapeamento de emoji para palavra de emoção
    private static final Map<String, String> emojiToEmotionMap = new HashMap<>();

    static {
        // Inicializa o mapeamento de emoji para emoção
        initEmojiToEmotionMap();
    }

    /**
     * Inicializa o mapeamento de emoji para emoção
     */
    private static void initEmojiToEmotionMap() {
        Map<String, String[]> emotionToEmojis = new HashMap<>();
        // Neutro
        emotionToEmojis.put("neutral", new String[] { "😐", "😶" });
        // Feliz
        emotionToEmojis.put("happy", new String[] { "🌈", "😊", "🎈", "🐱" });
        // Rindo
        emotionToEmojis.put("laughing", new String[] { "😀", "😃", "😁", "😏", "😄", "🤪" });
        // Engraçado
        emotionToEmojis.put("funny", new String[] { "😂", "🤣", "😆" });
        // Triste
        emotionToEmojis.put("sad", new String[] { "😢", "😔", "😞", "😑" });
        // Bravo
        emotionToEmojis.put("angry", new String[] { "😠", "😡", "😒", "😤", "🤬" });
        // Chorando
        emotionToEmojis.put("crying", new String[] { "😭" });
        // Amando
        emotionToEmojis.put("loving", new String[] { "❤️", "💕", "😍", "🥰", "💖" });
        // Envergonhado
        emotionToEmojis.put("embarrassed", new String[] { "😳", "😓", "😅" });
        // Surpreso
        emotionToEmojis.put("surprised", new String[] { "😮", "😲", "😯" });
        // Chocado
        emotionToEmojis.put("shocked", new String[] { "😱", "😨", "😬" });
        // Pensando
        emotionToEmojis.put("thinking", new String[] { "🤔", "💭", "💬", "🧐" });
        // Piscando
        emotionToEmojis.put("winking", new String[] { "😉", "🤗", "👋", "🌟", "🐶" });
        // Descolado
        emotionToEmojis.put("cool", new String[] { "😎" });
        // Relaxado
        emotionToEmojis.put("relaxed", new String[] { "😌" });
        // Delicioso
        emotionToEmojis.put("delicious", new String[] { "😋", "🤤", "🍽️" });
        // Beijo
        emotionToEmojis.put("kissy", new String[] { "😘", "💋", "😚", "😗", "😙" });
        // Confiante
        emotionToEmojis.put("confident", new String[] { "💪" });
        // Sonolento
        emotionToEmojis.put("sleepy", new String[] { "😴" });
        // Bobo
        emotionToEmojis.put("silly", new String[] { "😛", "😜", "😝" });
        // Confuso
        emotionToEmojis.put("confused", new String[] { "😕", "🙄" });

        // Preenche o mapeamento de emoji para palavra de emoção
        for (Map.Entry<String, String[]> entry : emotionToEmojis.entrySet()) {
            String emotion = entry.getKey();
            for (String emoji : entry.getValue()) {
                // Mapeia cada caractere de emoji para a palavra de emoção
                emojiToEmotionMap.put(emoji, emotion);
            }
        }
    }

    /**
     * Limpa o texto, removendo tags HTML, caracteres especiais e caracteres de controle
     *
     * @param text texto de entrada
     * @return texto limpo
     */
    public static String cleanText(String text) {
        // Remove caracteres de controle
        text = text.replaceAll("[\\t\\n\\r\b\\f]", "");

        // Remove tags HTML
        text = HTML_TAG_PATTERN.matcher(text).replaceAll("");

        // Remove símbolos especiais
        text = SPECIAL_CHARS_PATTERN.matcher(text).replaceAll("");

        // Substitui sequências de espaços em branco por um único espaço
        text = WHITESPACE_PATTERN.matcher(text).replaceAll(" ");

        // Remove espaços no início e no fim
        return text.trim();
    }

    /**
     * Verifica se o caractere é um emoji
     *
     * @param codePoint código de ponto Unicode do caractere de entrada
     * @return true se for um emoji, false caso contrário
     */
    public static boolean isEmoji(int codePoint) {
        for (int[] range : EMOJI_RANGES) {
            if (codePoint >= range[0] && codePoint <= range[1]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica se o texto contém kaomoji
     *
     * @param text texto a ser verificado
     * @return true se contiver kaomoji, false caso contrário
     */
    public static boolean containsKaomoji(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        Matcher matcher = KAOMOJI_PATTERN.matcher(text);
        return matcher.find();
    }

    /**
     * Filtra os kaomojis do texto
     *
     * @param text texto a ser filtrado
     * @return texto filtrado
     */
    public static String filterKaomoji(String text) {
        if (text == null) {
            return null;
        }
        // Substitui os kaomojis por string vazia
        return KAOMOJI_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Extrai os emojis presentes na frase
     *
     * @param text frase de entrada
     * @return lista contendo todos os emojis
     */
    public static List<String> extractEmojis(String text) {
        List<String> emojis = new ArrayList<>();
        for (int i = 0; i < text.length();) {
            int codePoint = text.codePointAt(i);
            if (Character.isValidCodePoint(codePoint)) {
                String emoji = new String(Character.toChars(codePoint));
                if (isEmoji(codePoint)) {
                    emojis.add(emoji);
                }
            }
            i += Character.charCount(codePoint);
        }
        return emojis;
    }

    /**
     * Obtém a palavra de emoção a partir do emoji
     *
     * @param emoji o emoji
     * @return palavra de emoção; retorna "happy" se não houver correspondência
     */
    public static String getEmotionByEmoji(String emoji) {
        return emojiToEmotionMap.getOrDefault(emoji, "happy");
    }

    /**
     * Lista de todas as palavras de emoção disponíveis (usada para seleção aleatória)
     */
    private static final String[] EMOTIONS = {
            "neutral", "happy", "laughing", "funny", "sad", "angry", "crying",
            "loving", "embarrassed", "surprised", "shocked", "thinking", "winking",
            "cool", "relaxed", "delicious", "kissy", "confident", "sleepy", "silly", "confused"
    };

    /**
     * Retorna uma palavra de emoção aleatória (usada quando a frase não tem emoji)
     */
    public static String getRandomEmotion() {
        return EMOTIONS[ThreadLocalRandom.current().nextInt(EMOTIONS.length)];
    }

    /**
     * Processa a frase, removendo os emojis e mapeando-os para palavras de humor
     *
     * @param text frase de entrada
     * @return retorna a frase processada e a lista de emojis
     */
    public static String processSentence(String text, List<String> moods) {
        Assert.notNull(moods, "moods cannot be null");
        Assert.hasText(text, "text cannot be empty");
        text = cleanText(text);
        StringBuilder cleanedText = new StringBuilder();

        int length = text.length();
        for (int i = 0; i < length;) {
            int codePoint = text.codePointAt(i);
            if (isEmoji(codePoint)) {
                // Converte para string de emoji e busca a palavra de emoção correspondente
                String emoji = new String(Character.toChars(codePoint));
                String mood = getEmotionByEmoji(emoji);
                if (mood != null && !mood.isEmpty()) {
                    moods.add(mood);
                }
                // Pula o emoji atual
                i += Character.charCount(codePoint);
            } else {
                // Mantém o caractere que não é emoji
                cleanedText.appendCodePoint(codePoint);
                i++;
            }
        }
        
        // Filtra os kaomojis
        String filteredText = filterKaomoji(cleanedText.toString().trim());
        
        return  filteredText;
    }

}