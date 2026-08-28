#!/usr/bin/env bash
# =============================================================================
# Download de dependências básicas: modelo VAD + bibliotecas nativas (sherpa-onnx JNI + Vosk)
# Pode ser executado de forma independente ou chamado pelo script principal download_models.sh
#
# Uso:
#   ./scripts/download_base.sh              # Baixa todas as dependências básicas
#   ./scripts/download_base.sh vad          # Baixa somente o modelo VAD
#   ./scripts/download_base.sh jni          # Baixa somente a biblioteca nativa sherpa-onnx JNI
#   ./scripts/download_base.sh vosk-lib     # Baixa somente a biblioteca nativa Vosk
#   ./scripts/download_base.sh libs         # Baixa todas as bibliotecas nativas
#   ./scripts/download_base.sh clean        # Limpa
#   ./scripts/download_base.sh status       # Verifica o status
# =============================================================================

set -e
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_common.sh"

# ---- Configuração do modelo VAD ----
VAD_MODEL_NAME="silero_vad.onnx"
VAD_MODEL_URL="https://github.com/snakers4/silero-vad/raw/master/src/silero_vad/data/silero_vad.onnx"
VAD_MODEL_DIR="${MODELS_DIR}"

# ---- Endereços de download das bibliotecas nativas ----
SHERPA_GITHUB="https://github.com/k2-fsa/sherpa-onnx/releases/download/v${SHERPA_VERSION}"
VOSK_GITHUB="https://github.com/alphacep/vosk-api/releases/download/v${VOSK_VERSION}"

# ============================================================
# Download do modelo VAD (Silero VAD)
# ============================================================
download_vad() {
    info "========== Download do modelo VAD de detecção de voz =========="
    info "Modelo: ${VAD_MODEL_NAME} (~2.2MB)"

    mkdir -p "$VAD_MODEL_DIR"

    if [ -f "${VAD_MODEL_DIR}/${VAD_MODEL_NAME}" ]; then
        info "Modelo VAD já existe: ${VAD_MODEL_DIR}/${VAD_MODEL_NAME}"
        info "Para baixar novamente, execute primeiro: $0 clean"
        return 0
    fi

    info "Baixando..."
    download_file "$VAD_MODEL_URL" "${VAD_MODEL_DIR}/${VAD_MODEL_NAME}"

    info "Download do modelo VAD concluído!"
    echo ""
}

# ============================================================
# Download da biblioteca nativa sherpa-onnx JNI (todas as plataformas, extraída do tarball JNI)
# ============================================================
download_jni_lib() {
    detect_platform
    info "========== Download da biblioteca nativa sherpa-onnx JNI =========="
    info "Versão: v${SHERPA_VERSION} (${PLATFORM})"

    mkdir -p "$LIB_DIR"

    if [ -f "${LIB_DIR}/${SHERPA_JNI_LIB}" ] && [ -f "${LIB_DIR}/${ONNXRT_LIB}" ]; then
        info "Biblioteca nativa JNI já existe: ${LIB_DIR}/"
        info "Para baixar novamente, execute primeiro: $0 clean"
        return 0
    fi

    local tar_name="sherpa-onnx-v${SHERPA_VERSION}-${PLATFORM}-jni.tar.bz2"
    local tar_url="${SHERPA_GITHUB}/${tar_name}"

    local WORK_DIR=$(mktemp -d)
    trap "rm -rf '$WORK_DIR'" EXIT

    info "Baixando ${tar_name} ..."
    download_file "$tar_url" "${WORK_DIR}/${tar_name}"

    info "Extraindo bibliotecas nativas..."
    tar xf "${WORK_DIR}/${tar_name}" -C "$WORK_DIR"

    local found=0
    while IFS= read -r -d '' lib_file; do
        cp "$lib_file" "$LIB_DIR/"
        info "  Extraindo: $(basename "$lib_file")"
        found=$((found + 1))
    done < <(find "$WORK_DIR" -type f \( -name "*.${LIB_EXT}" -o -name "*.${LIB_EXT}.*" \) -print0)

    # macOS: cria link simbólico libonnxruntime.dylib
    if [[ "$PLATFORM" == osx-* ]] && [ ! -e "${LIB_DIR}/libonnxruntime.dylib" ]; then
        local versioned_ort
        versioned_ort=$(ls "${LIB_DIR}"/libonnxruntime.*.dylib 2>/dev/null | head -1)
        if [ -n "$versioned_ort" ]; then
            ln -sf "$(basename "$versioned_ort")" "${LIB_DIR}/libonnxruntime.dylib"
            info "  Criando link simbólico: libonnxruntime.dylib -> $(basename "$versioned_ort")"
        fi
    fi

    rm -rf "$WORK_DIR"
    trap - EXIT

    if [ "$found" -eq 0 ]; then
        error "Falha na extração, nenhum arquivo .${LIB_EXT} encontrado"
        return 1
    fi

    info "Download da biblioteca nativa sherpa-onnx JNI concluído! (${found} arquivo(s) no total)"
    echo ""
}

# ============================================================
# Download da biblioteca nativa Vosk (extraída do JAR do Maven)
# ============================================================
download_vosk_lib() {
    detect_platform
    info "========== Download da biblioteca nativa Vosk =========="
    info "Versão: v${VOSK_VERSION} (${PLATFORM})"

    mkdir -p "$LIB_DIR"

    if [ -f "${LIB_DIR}/${VOSK_LIB}" ]; then
        info "Biblioteca nativa Vosk já existe: ${LIB_DIR}/${VOSK_LIB}"
        info "Para baixar novamente, execute primeiro: $0 clean"
        return 0
    fi

    local jar_subdir
    case "$PLATFORM" in
        linux-x64)      jar_subdir="linux-x86-64" ;;
        linux-aarch64)
            warn "O JAR Maven do Vosk não contém a biblioteca nativa linux-aarch64"
            warn "Baixe manualmente em ${VOSK_GITHUB}/vosk-linux-aarch64-${VOSK_VERSION}.zip"
            return 0
            ;;
        osx-*)          jar_subdir="darwin" ;;
        win-x64)        jar_subdir="win32-x86-64" ;;
    esac

    local vosk_jar=""
    local m2_jar="${HOME}/.m2/repository/com/alphacephei/vosk/${VOSK_VERSION}/vosk-${VOSK_VERSION}.jar"
    if [ -f "$m2_jar" ]; then
        info "Extraindo do cache local do Maven..."
        vosk_jar="$m2_jar"
    else
        local jar_url="https://repo1.maven.org/maven2/com/alphacephei/vosk/${VOSK_VERSION}/vosk-${VOSK_VERSION}.jar"
        local WORK_DIR=$(mktemp -d)
        vosk_jar="${WORK_DIR}/vosk-${VOSK_VERSION}.jar"
        info "Baixando vosk-${VOSK_VERSION}.jar do Maven Central ..."
        download_file "$jar_url" "$vosk_jar"
    fi

    local EXTRACT_DIR=$(mktemp -d)
    unzip -q -o "$vosk_jar" "${jar_subdir}/${VOSK_LIB}" -d "$EXTRACT_DIR" 2>/dev/null || true

    if [ -f "${EXTRACT_DIR}/${jar_subdir}/${VOSK_LIB}" ]; then
        cp "${EXTRACT_DIR}/${jar_subdir}/${VOSK_LIB}" "$LIB_DIR/"
        info "Extração da biblioteca nativa Vosk concluída: ${VOSK_LIB}"
    else
        error "${jar_subdir}/${VOSK_LIB} não encontrado no JAR"
        rm -rf "$EXTRACT_DIR" "${WORK_DIR:-}"
        return 1
    fi

    rm -rf "$EXTRACT_DIR" "${WORK_DIR:-}"
    echo ""
}

# ============================================================
# Download de todas as bibliotecas nativas
# ============================================================
download_libs() {
    download_jni_lib
    download_vosk_lib
}

# ============================================================
# Download de todas as dependências básicas
# ============================================================
download_base_all() {
    download_vad
    download_libs
}

# ============================================================
# Limpeza
# ============================================================
clean_base() {
    warn "========== Limpando dependências básicas =========="

    if [ -f "${VAD_MODEL_DIR}/${VAD_MODEL_NAME}" ]; then
        rm -f "${VAD_MODEL_DIR}/${VAD_MODEL_NAME}"
        info "Modelo VAD removido: ${VAD_MODEL_NAME}"
    fi

    local cleaned=0
    for pattern in \
        "libsherpa-onnx-jni.*" "libsherpa-onnx-c-api.*" "libsherpa-onnx-cxx-api.*" \
        "sherpa-onnx-jni.*" "sherpa-onnx-c-api.*" \
        "libonnxruntime*" "onnxruntime.*" \
        "libvosk.*" "vosk.dll"; do
        for f in "${LIB_DIR}"/${pattern}; do
            if [ -f "$f" ] || [ -L "$f" ]; then
                rm -f "$f"
                info "Removido: $(basename "$f")"
                cleaned=$((cleaned + 1))
            fi
        done
    done

    [ "$cleaned" -eq 0 ] && info "Diretório lib/ não precisa de limpeza"
    info "Limpeza concluída!"
}

# ============================================================
# Status
# ============================================================
show_base_status() {
    detect_platform

    # VAD
    if [ -f "${VAD_MODEL_DIR}/${VAD_MODEL_NAME}" ]; then
        echo -e "  VAD (detecção de voz): ${GREEN}✓ Baixado${NC} - ${VAD_MODEL_NAME}"
    else
        echo -e "  VAD (detecção de voz): ${RED}✗ Não baixado${NC} - ${VAD_MODEL_NAME}"
    fi

    # sherpa-onnx JNI
    if [ -f "${LIB_DIR}/${SHERPA_JNI_LIB}" ]; then
        echo -e "  sherpa-onnx JNI:   ${GREEN}✓ Existe${NC} - ${SHERPA_JNI_LIB}"
    else
        echo -e "  sherpa-onnx JNI:   ${RED}✗ Não existe${NC} - ${SHERPA_JNI_LIB}"
    fi

    # onnxruntime
    if [ -f "${LIB_DIR}/${ONNXRT_LIB}" ] || ls "${LIB_DIR}"/libonnxruntime.*.${LIB_EXT} &>/dev/null 2>&1; then
        echo -e "  onnxruntime:       ${GREEN}✓ Existe${NC} - ${ONNXRT_LIB}"
    else
        echo -e "  onnxruntime:       ${RED}✗ Não existe${NC} - ${ONNXRT_LIB}"
    fi

    # Vosk
    if [ -f "${LIB_DIR}/${VOSK_LIB}" ]; then
        echo -e "  Vosk (biblioteca nativa): ${GREEN}✓ Existe${NC} - ${VOSK_LIB}"
    else
        echo -e "  Vosk (biblioteca nativa): ${RED}✗ Não existe${NC} - ${VOSK_LIB}"
    fi
}

# ============================================================
# Ponto de entrada para execução independente
# ============================================================
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    case "${1:-all}" in
        vad)        download_vad ;;
        jni)        download_jni_lib ;;
        vosk-lib)   download_vosk_lib ;;
        libs)       download_libs ;;
        all)        download_base_all ;;
        clean)      clean_base ;;
        status)     detect_platform; echo ""; info "========== Status das dependências básicas (${PLATFORM}) =========="; show_base_status; echo "" ;;
        *)
            echo "Uso: $0 [vad|jni|vosk-lib|libs|all|clean|status]"
            echo ""
            echo "  vad      - Baixa o modelo VAD de detecção de voz (silero_vad)"
            echo "  jni      - Baixa a biblioteca nativa sherpa-onnx JNI (inclui onnxruntime)"
            echo "  vosk-lib - Baixa a biblioteca nativa Vosk"
            echo "  libs     - Baixa todas as bibliotecas nativas (JNI + Vosk)"
            echo "  all      - Baixa todas as dependências básicas (padrão)"
            echo "  clean    - Limpa todas as dependências básicas"
            echo "  status   - Verifica o status"
            exit 1
            ;;
    esac
fi
