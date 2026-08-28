#!/usr/bin/env bash
# =============================================================================
# Download do modelo de síntese de voz (TTS) (sherpa-onnx)
# Pode ser executado de forma independente ou chamado pelo script principal download_models.sh
#
# Uso:
#   ./scripts/download_tts.sh              # Baixa o modelo padrão (vits-melo)
#   ./scripts/download_tts.sh vits-melo    # Baixa o modelo VITS MeloTTS chinês-inglês (~163MB)
#   ./scripts/download_tts.sh matcha       # Baixa o modelo Matcha-Icefall chinês-inglês
#   ./scripts/download_tts.sh clean        # Limpa
#   ./scripts/download_tts.sh status       # Verifica o status
# =============================================================================

set -e
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_common.sh"

# ---- Configuração do modelo TTS ----
TTS_MODEL_DIR="${MODELS_DIR}/tts"
SHERPA_TTS_BASE_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"
# Modelos TTS suportados: vits-melo (padrão), matcha
TTS_VITS_MELO="vits-melo-tts-zh_en"
TTS_MATCHA="matcha-icefall-zh-en"

# ============================================================
# Download do modelo TTS
# ============================================================
download_tts() {
    local tts_type="${1:-vits-melo}"
    local model_name

    case "$tts_type" in
        vits-melo|vits|melo)
            model_name="$TTS_VITS_MELO"
            info "========== Download do modelo de síntese de voz (TTS) =========="
            info "Modelo: ${model_name} (VITS MeloTTS chinês-inglês, ~163MB)"
            ;;
        matcha|matcha-icefall)
            model_name="$TTS_MATCHA"
            info "========== Download do modelo de síntese de voz (TTS) =========="
            info "Modelo: ${model_name} (Matcha-Icefall chinês-inglês)"
            ;;
        *)
            error "Tipo de modelo TTS desconhecido: $tts_type (suportados: vits-melo, matcha)"
            return 1
            ;;
    esac

    mkdir -p "$TTS_MODEL_DIR"

    if [ -f "${TTS_MODEL_DIR}/${model_name}/model.onnx" ]; then
        info "Modelo TTS já existe: ${TTS_MODEL_DIR}/${model_name}/"
        info "Para baixar novamente, execute primeiro: $0 clean"
        return 0
    fi

    local tar_url="${SHERPA_TTS_BASE_URL}/${model_name}.tar.bz2"

    cd "$TTS_MODEL_DIR"
    info "Baixando..."
    download_file "$tar_url" "${model_name}.tar.bz2"

    info "Extraindo..."
    tar xf "${model_name}.tar.bz2"
    rm -f "${model_name}.tar.bz2"

    info "Download do modelo TTS concluído!"
    info "Caminho: ${TTS_MODEL_DIR}/${model_name}/"
    echo ""
}

# ============================================================
# Limpeza
# ============================================================
clean_tts() {
    warn "========== Limpando modelo TTS =========="
    for tts_model in "$TTS_VITS_MELO" "$TTS_MATCHA"; do
        if [ -d "${TTS_MODEL_DIR}/${tts_model}" ]; then
            rm -rf "${TTS_MODEL_DIR}/${tts_model}"
            info "Modelo TTS removido: ${tts_model}"
        fi
        rm -f "${TTS_MODEL_DIR}/${tts_model}.tar.bz2" 2>/dev/null
    done
    info "Limpeza concluída!"
}

# ============================================================
# Status
# ============================================================
show_tts_status() {
    if [ -f "${TTS_MODEL_DIR}/${TTS_VITS_MELO}/model.onnx" ]; then
        echo -e "  TTS (vits-melo):   ${GREEN}✓ Baixado${NC} - ${TTS_VITS_MELO}"
    else
        echo -e "  TTS (vits-melo):   ${RED}✗ Não baixado${NC} - ${TTS_VITS_MELO}"
    fi

    if [ -f "${TTS_MODEL_DIR}/${TTS_MATCHA}/model.onnx" ]; then
        echo -e "  TTS (matcha):      ${GREEN}✓ Baixado${NC} - ${TTS_MATCHA}"
    else
        echo -e "  TTS (matcha):      ${YELLOW}○ Não baixado${NC} - ${TTS_MATCHA} (opcional)"
    fi
}

# ============================================================
# Ponto de entrada para execução independente
# ============================================================
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    case "${1:-vits-melo}" in
        vits-melo|vits|melo|matcha|matcha-icefall)
            download_tts "$1"
            ;;
        clean)
            clean_tts
            ;;
        status)
            echo ""; info "========== Status do modelo TTS =========="; show_tts_status; echo ""
            ;;
        *)
            echo "Uso: $0 [vits-melo|matcha|clean|status]"
            echo ""
            echo "  vits-melo - Baixa o modelo VITS MeloTTS chinês-inglês (~163MB, padrão)"
            echo "  matcha    - Baixa o modelo Matcha-Icefall chinês-inglês"
            echo "  clean     - Limpa todos os modelos TTS"
            echo "  status    - Verifica o status"
            exit 1
            ;;
    esac
fi
