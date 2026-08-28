package com.xiaozhi.role.impl;

import com.xiaozhi.common.config.RuntimePathConfig;
import com.xiaozhi.role.SherpaVoiceService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
/**
 * Implementação do serviço de varredura de vozes locais do Sherpa-ONNX.
 */
@Slf4j
@Service
public class SherpaVoiceServiceImpl implements SherpaVoiceService {

    @Resource
    private RuntimePathConfig runtimePathConfig;

    @Override
    public List<Map<String, Object>> listVoices() {
        List<Map<String, Object>> voices = new ArrayList<>();
        File ttsDir = runtimePathConfig.resolveTtsModelsDir().toFile();
        if (!ttsDir.exists() || !ttsDir.isDirectory()) {
            return voices;
        }
        File[] modelDirs = ttsDir.listFiles(File::isDirectory);
        if (modelDirs != null) {
            Arrays.sort(modelDirs, Comparator.comparing(File::getName));
            for (File modelDir : modelDirs) {
                voices.addAll(buildVoicesForModel(modelDir));
            }
        }
        return voices;
    }

    private List<Map<String, Object>> buildVoicesForModel(File modelDir) {
        List<Map<String, Object>> voices = new ArrayList<>();
        String dirName = modelDir.getName();

        // Detecta o tipo de modelo
        boolean isKokoro = new File(modelDir, "voices.bin").exists();
        boolean isMatcha = false;
        if (!isKokoro) {
            File[] files = modelDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if ((f.getName().contains("vocoder") || f.getName().contains("vocos")) && f.getName().endsWith(".onnx")) {
                        isMatcha = true;
                        break;
                    }
                }
            }
        }

        if (isKokoro) {
            List<String> speakerNames = readKokoroSpeakers(new File(modelDir, "voices.bin"));
            if (speakerNames.isEmpty()) {
                // Falha na leitura, define 8 por padrão
                for (int i = 0; i < 8; i++) {
                    voices.add(buildVoice(dirName, "kokoro", i, "Speaker-" + i));
                }
            } else {
                for (int i = 0; i < speakerNames.size(); i++) {
                    voices.add(buildVoice(dirName, "kokoro", i, speakerNames.get(i)));
                }
            }
        } else if (isMatcha) {
            voices.add(buildVoice(dirName, "matcha", 0, dirName));
        } else {
            // VITS: modelos com múltiplos speakers são identificados pelo nome do diretório
            boolean isMultiSpeaker = dirName.contains("aishell3") || dirName.contains("vctk");
            if (isMultiSpeaker) {
                // VITS com múltiplos speakers: lista os 10 primeiros por padrão; o usuário pode expandir
                for (int i = 0; i < 10; i++) {
                    voices.add(buildVoice(dirName, "vits", i, "Speaker-" + i));
                }
            } else {
                voices.add(buildVoice(dirName, "vits", 0, dirName));
            }
        }
        return voices;
    }

    private Map<String, Object> buildVoice(String modelDir, String modelType, int speakerId, String label) {
        Map<String, Object> voice = new LinkedHashMap<>();
        voice.put("label", label);
        voice.put("value", modelDir + ":" + modelType + ":" + speakerId);
        voice.put("provider", "sherpa-onnx");
        voice.put("model", modelDir);
        return voice;
    }

    /**
     * Lê a lista de nomes de speaker do arquivo voices.bin do Kokoro.
     * Formato do arquivo: cada nome é armazenado sequencialmente, terminado por \0.
     */
    private List<String> readKokoroSpeakers(File voicesBin) {
        List<String> names = new ArrayList<>();
        try {
            byte[] data = Files.readAllBytes(voicesBin.toPath());
            int start = 0;
            for (int i = 0; i < data.length; i++) {
                if (data[i] == 0) {
                    if (i > start) {
                        String name = new String(data, start, i - start, StandardCharsets.UTF_8).trim();
                        if (!name.isEmpty()) {
                            names.add(name);
                        }
                    }
                    start = i + 1;
                }
            }
            // Trata o caso em que não há \0 no final
            if (start < data.length) {
                String name = new String(data, start, data.length - start, StandardCharsets.UTF_8).trim();
                if (!name.isEmpty()) names.add(name);
            }
        } catch (IOException e) {
            log.warn("Falha ao ler voices.bin: {}", voicesBin.getAbsolutePath());
        }
        return names;
    }
}
