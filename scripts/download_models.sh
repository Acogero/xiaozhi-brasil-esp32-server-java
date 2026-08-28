#!/usr/bin/env bash
# =============================================================================
# Ponto de entrada principal: baixa todos os modelos e bibliotecas nativas
# Agrega download_base.sh / download_stt.sh / download_tts.sh
#
# Plataformas suportadas: linux-x64, linux-aarch64, osx-x86_64, osx-arm64, win-x64
# Detecta automaticamente a plataforma atual, ou pode ser especificada via variável de ambiente TARGET_PLATFORM
#
# Uso:
#   ./scripts/download_models.sh              # Baixa todos os modelos e bibliotecas nativas
#   ./scripts/download_models.sh stt          # Baixa o modelo STT (Vosk)
#   ./scripts/download_models.sh tts          # Baixa o modelo TTS (vits-melo, padrão)
#   ./scripts/download_models.sh vad          # Baixa o modelo VAD (silero_vad)
#   ./scripts/download_models.sh models       # Baixa todos os modelos
#   ./scripts/download_models.sh jni          # Baixa somente a biblioteca nativa sherpa-onnx JNI
#   ./scripts/download_models.sh vosk-lib     # Baixa somente a biblioteca nativa Vosk
#   ./scripts/download_models.sh libs         # Baixa todas as bibliotecas nativas (JNI + Vosk)
#   ./scripts/download_models.sh clean        # Limpa todos os modelos e bibliotecas baixados
#   ./scripts/download_models.sh status       # Verifica o status dos modelos e bibliotecas
#
# Variáveis de ambiente:
#   TARGET_PLATFORM=osx-arm64 ./scripts/download_models.sh  # Especifica a plataforma de destino
# =============================================================================

set -e

_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Carrega todos os subscripts (apenas as funções, sem disparar o ponto de entrada independente)
source "${_SCRIPT_DIR}/_common.sh"
source "${_SCRIPT_DIR}/download_base.sh"
source "${_SCRIPT_DIR}/download_stt.sh"
source "${_SCRIPT_DIR}/download_tts.sh"

# ============================================================
# Operações agregadas
# ============================================================
download_all_models() {
    download_stt "${2:-small}"
    download_tts "vits-melo"
    download_vad
}

clean_all() {
    clean_stt
    clean_tts
    clean_base
}

show_status() {
    detect_platform
    echo ""
    info "========== Status dos modelos e bibliotecas nativas (${PLATFORM}) =========="

    echo -e "  ${BLUE}── Modelos ──${NC}"
    show_stt_status
    show_tts_status

    echo -e "  ${BLUE}── Dependências básicas ──${NC}"
    show_base_status

    echo ""
}

# ============================================================
# Lógica principal
# ============================================================
case "${1:-all}" in
    stt)        download_stt "$2" ;;
    tts)        download_tts "$2" ;;
    vad)        download_vad ;;
    models)     download_all_models ;;
    jni)        download_jni_lib ;;
    vosk-lib)   download_vosk_lib ;;
    libs)       download_libs ;;
    all)
        download_all_models
        download_libs
        show_status
        info "Download de todos os modelos e bibliotecas nativas concluído!"
        ;;
    clean)
        clean_all
        show_status
        ;;
    status)
        show_status
        ;;
    *)
        echo "Uso: $0 <command> [options]"
        echo ""
        echo "Download de modelos:"
        echo "  stt [small|standard]   - Baixa o modelo de reconhecimento de voz Vosk (padrão small)"
        echo "  tts [vits-melo|matcha] - Baixa o modelo de síntese de voz TTS (padrão vits-melo)"
        echo "  vad                    - Baixa o modelo VAD de detecção de voz (silero_vad)"
        echo "  models                 - Baixa todos os modelos"
        echo ""
        echo "Download de bibliotecas nativas:"
        echo "  jni                    - Baixa a biblioteca nativa sherpa-onnx JNI (inclui onnxruntime)"
        echo "  vosk-lib               - Baixa a biblioteca nativa Vosk"
        echo "  libs                   - Baixa todas as bibliotecas nativas (JNI + Vosk)"
        echo ""
        echo "Outros:"
        echo "  all                    - Baixa todos os modelos e bibliotecas nativas (padrão)"
        echo "  clean                  - Limpa todos os modelos e bibliotecas nativas baixados"
        echo "  status                 - Verifica o status do download"
        echo ""
        echo "Plataformas suportadas: linux-x64, linux-aarch64, osx-x86_64, osx-arm64, win-x64"
        echo "Detecta automaticamente a plataforma atual, ou especifique via variável de ambiente TARGET_PLATFORM"
        echo "Exemplo: TARGET_PLATFORM=linux-x64 $0 libs"
        echo ""
        echo "Cada subscript também pode ser executado de forma independente:"
        echo "  ./scripts/download_base.sh       # Dependências básicas (VAD + bibliotecas nativas)"
        echo "  ./scripts/download_stt.sh        # Modelo STT"
        echo "  ./scripts/download_tts.sh        # Modelo TTS"
        exit 1
        ;;
esac
