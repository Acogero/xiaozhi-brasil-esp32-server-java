#!/usr/bin/env bash
# =============================================================================
# Download do modelo de reconhecimento de voz (STT) (Vosk)
# Pode ser executado de forma independente ou chamado pelo script principal download_models.sh
#
# Uso:
#   ./scripts/download_stt.sh              # Baixa o modelo pequeno (padrão)
#   ./scripts/download_stt.sh small        # Baixa o modelo pequeno (~50MB)
#   ./scripts/download_stt.sh standard     # Baixa o modelo padrão (~1.3GB)
#   ./scripts/download_stt.sh clean        # Limpa
#   ./scripts/download_stt.sh status       # Verifica o status
# =============================================================================

set -e
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/_common.sh"

# ---- Configuração do modelo Vosk STT ----
# small: vosk-model-small-cn-0.22 (~50MB, mais rápido, indicado para dispositivos com poucos recursos)
# standard: vosk-model-cn-0.22 (~1.3GB, alta precisão, recomendado para produção)
VOSK_MODEL_SMALL="vosk-model-small-cn-0.22"
VOSK_MODEL_STANDARD="vosk-model-cn-0.22"
VOSK_BASE_URL="https://alphacephei.com/vosk/models"
STT_MODEL_DIR="${MODELS_DIR}"

# ============================================================
# Download do modelo Vosk STT
# ============================================================
download_stt() {
    local size="${1:-small}"
    local model_name

    if [ "$size" = "small" ]; then
        model_name="$VOSK_MODEL_SMALL"
    else
        model_name="$VOSK_MODEL_STANDARD"
    fi

    info "========== Download do modelo de reconhecimento de voz (STT) =========="
    info "Modelo: ${model_name}"
    if [ "$size" = "small" ]; then
        info "Descrição: modelo Vosk chinês pequeno (~50MB, mais rápido)"
    else
        info "Descrição: modelo Vosk chinês padrão (~1.3GB, alta precisão)"
    fi

    mkdir -p "$STT_MODEL_DIR"

    if [ -d "${STT_MODEL_DIR}/vosk-model" ]; then
        info "Modelo Vosk já existe: ${STT_MODEL_DIR}/vosk-model/"
        info "Para baixar novamente, execute primeiro: $0 clean"
        return 0
    fi

    cd "$STT_MODEL_DIR"
    info "Baixando ${model_name}.zip ..."
    download_file "${VOSK_BASE_URL}/${model_name}.zip" "${model_name}.zip"

    info "Extraindo..."
    unzip -q "${model_name}.zip"

    # Renomeia para o diretório padrão vosk-model (consistente com o caminho em VoskSttService.java)
    mv "${model_name}" vosk-model
    rm -f "${model_name}.zip"

    info "Download do modelo STT concluído!"
    info "Caminho: ${STT_MODEL_DIR}/vosk-model/"
    echo ""
}

# ============================================================
# Limpeza
# ============================================================
clean_stt() {
    warn "========== Limpando modelo STT =========="
    if [ -d "${STT_MODEL_DIR}/vosk-model" ]; then
        rm -rf "${STT_MODEL_DIR}/vosk-model"
        info "Modelo Vosk STT removido"
    else
        info "Nada a limpar"
    fi
    info "Limpeza concluída!"
}

# ============================================================
# Status
# ============================================================
show_stt_status() {
    if [ -d "${STT_MODEL_DIR}/vosk-model" ]; then
        echo -e "  STT (reconhecimento de voz): ${GREEN}✓ Baixado${NC} - vosk-model"
    else
        echo -e "  STT (reconhecimento de voz): ${RED}✗ Não baixado${NC} - vosk-model"
    fi
}

# ============================================================
# Ponto de entrada para execução independente
# ============================================================
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    case "${1:-small}" in
        small|standard)
            download_stt "$1"
            ;;
        clean)
            clean_stt
            ;;
        status)
            echo ""; info "========== Status do modelo STT =========="; show_stt_status; echo ""
            ;;
        *)
            echo "Uso: $0 [small|standard|clean|status]"
            echo ""
            echo "  small    - Baixa o modelo Vosk chinês pequeno (~50MB, padrão)"
            echo "  standard - Baixa o modelo Vosk chinês padrão (~1.3GB)"
            echo "  clean    - Limpa o modelo STT"
            echo "  status   - Verifica o status"
            exit 1
            ;;
    esac
fi
