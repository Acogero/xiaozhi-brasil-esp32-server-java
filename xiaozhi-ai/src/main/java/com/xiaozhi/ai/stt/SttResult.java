package com.xiaozhi.ai.stt;

/**
 * Resultado do reconhecimento STT.
 * O campo de emoção só tem valor em modelos que suportam reconhecimento de emoção; nos demais, é null.
 *
 * <p>Descrição dos campos:
 * <ul>
 *   <li>text - texto reconhecido</li>
 *   <li>emotion - tag de emoção, como happy / neutral / angry / sad, etc.</li>
 *   <li>emotionScore - confiança da emoção (0~1)</li>
 *   <li>emotionDegree - tag de intensidade da emoção, como weak / moderate / strong (Volcengine)</li>
 *   <li>emotionDegreeScore - confiança da intensidade da emoção (0~1) (Volcengine)</li>
 * </ul>
 */
public record SttResult(
        String text,
        String emotion,
        Double emotionScore,
        String emotionDegree,
        Double emotionDegreeScore
) {

    /**
     * Contém apenas o texto, sem informações de emoção.
     */
    public static SttResult textOnly(String text) {
        return new SttResult(text, null, null, null, null);
    }

    /**
     * Contém texto e informações de emoção (usado pelo Alibaba Cloud paraformer).
     */
    public static SttResult withEmotion(String text, String emotion, Double emotionScore) {
        return new SttResult(text, emotion, emotionScore, null, null);
    }

    /**
     * Contém texto e informações completas de emoção (usado pelo Volcengine).
     */
    public static SttResult withFullEmotion(String text, String emotion, Double emotionScore,
                                            String emotionDegree, Double emotionDegreeScore) {
        return new SttResult(text, emotion, emotionScore, emotionDegree, emotionDegreeScore);
    }

    public boolean hasEmotion() {
        return emotion != null && !emotion.isEmpty();
    }
}
