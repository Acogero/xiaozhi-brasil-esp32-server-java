import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { testVoice } from '@/services/role'
import { getResourceUrl } from '@/utils/resource'

/**
 * Composable de reprodução de áudio
 * Encapsula de forma unificada a lógica de reprodução de áudio, com suporte a dois modos:
 * 1. Reprodução direta a partir do caminho do áudio
 * 2. Obtenção do áudio via API antes de reproduzir
 */
export function useAudioPlayer() {
  const { t } = useI18n()

  // Estado de reprodução
  const playingAudioId = ref<string>('')
  const loadingAudioId = ref<string>('') // estado de carregamento (durante a requisição da API)
  const audioCache = new Map<string, HTMLAudioElement>()

  /**
   * Reproduz diretamente um arquivo de áudio (usado na reprodução de roteiros)
   * @param audioPath Caminho do áudio
   * @param audioId Identificador único do áudio
   */
  const playAudioDirect = async (audioPath: string, audioId: string): Promise<boolean> => {
    try {
      // Se o mesmo áudio já estiver tocando, interrompe
      if (playingAudioId.value === audioId) {
        const audio = audioCache.get(audioId)
        if (audio) {
          audio.pause()
          audio.currentTime = 0
        }
        playingAudioId.value = ''
        return true
      }

      // Interrompe outros áudios em reprodução
      stopAllAudio()

      // Cria ou obtém o elemento de áudio
      let audio = audioCache.get(audioId)
      if (!audio) {
        audio = new Audio()
        audioCache.set(audioId, audio)

        // Define o callback de término do áudio
        audio.onended = () => {
          if (playingAudioId.value === audioId) {
            playingAudioId.value = ''
          }
        }

        // Define o callback de erro
        audio.onerror = () => {
          message.error(t('common.audioPlayFailed'))
          if (playingAudioId.value === audioId) {
            playingAudioId.value = ''
          }
        }
      }

      // Define a fonte do áudio e inicia a reprodução
      const audioUrl = getResourceUrl(audioPath)
      if (!audioUrl) {
        message.error(t('common.audioPathInvalid'))
        return false
      }
      
      audio.src = audioUrl
      
      await audio.play()
      playingAudioId.value = audioId

      return true
    } catch (error) {
      // Falha na reprodução, limpa o estado de reprodução
      // Observação: não exibe mensagem de erro aqui, pois audio.onerror já trata isso
      playingAudioId.value = ''
      return false
    }
  }

  /**
   * Testa a voz via API e reproduz o resultado (usado no teste de voz)
   * @param voiceName Nome da voz
   * @param ttsId ID da configuração de TTS
   * @param provider Provedor de TTS
   * @param audioId Identificador único do áudio
   * @param testMessage Texto de teste
   */
  const playAudioFromApi = async (
    voiceName: string,
    ttsId: number,
    provider: string,
    audioId: string,
    testMessage?: string
  ): Promise<boolean> => {
    try {
      // Se o mesmo áudio já estiver tocando, interrompe
      if (playingAudioId.value === audioId) {
        const audio = audioCache.get(audioId)
        if (audio) {
          audio.pause()
          audio.currentTime = 0
        }
        playingAudioId.value = ''
        return true
      }

      // Interrompe outros áudios em reprodução
      stopAllAudio()

      // Define o estado de carregamento (durante a requisição da API)
      loadingAudioId.value = audioId
      
      const res = await testVoice({
        voiceName,
        ttsId,
        provider,
        message: testMessage || t('role.voiceTestMessage')
      })

      // Limpa o estado de carregamento
      loadingAudioId.value = ''

      if (res.code !== 200 || !res.data) {
        message.error(res.message || t('common.audioGenerateFailed'))
        return false
      }

      // Cria ou obtém o elemento de áudio
      let audio = audioCache.get(audioId)
      if (!audio) {
        audio = new Audio()
        audioCache.set(audioId, audio)

        // Define o callback de término do áudio
        audio.onended = () => {
          if (playingAudioId.value === audioId) {
            playingAudioId.value = ''
          }
        }

        // Define o callback de erro
        audio.onerror = () => {
          message.error(t('common.audioPlayFailed'))
          if (playingAudioId.value === audioId) {
            playingAudioId.value = ''
          }
        }
      }

      // Define a fonte do áudio e inicia a reprodução
      const audioUrl = getResourceUrl(res.data)
      if (!audioUrl) {
        message.error(t('common.audioPathInvalid'))
        return false
      }
      
      audio.src = audioUrl
      
      await audio.play()
      
      // Define o estado de reprodução após o sucesso
      playingAudioId.value = audioId

      return true
    } catch (error) {
      console.error('Falha ao testar a voz:', error)
      message.error(t('common.audioTestFailed'))
      loadingAudioId.value = ''
      if (playingAudioId.value === audioId) {
        playingAudioId.value = ''
      }
      return false
    }
  }

  /**
   * Interrompe toda a reprodução de áudio
   */
  const stopAllAudio = () => {
    audioCache.forEach((audio) => {
      audio.pause()
      audio.currentTime = 0
    })
    playingAudioId.value = ''
  }

  /**
   * Interrompe a reprodução de um áudio específico
   */
  const stopAudio = (audioId: string) => {
    const audio = audioCache.get(audioId)
    if (audio) {
      audio.pause()
      audio.currentTime = 0
    }
    if (playingAudioId.value === audioId) {
      playingAudioId.value = ''
    }
  }

  /**
   * Verifica se um áudio específico está em reprodução
   */
  const isPlaying = (audioId: string): boolean => {
    return playingAudioId.value === audioId
  }

  /**
   * Limpa o cache de áudio
   */
  const clearAudioCache = () => {
    stopAllAudio()
    audioCache.forEach((audio) => {
      audio.src = ''
    })
    audioCache.clear()
  }

  return {
    playingAudioId,
    loadingAudioId,
    playAudioDirect,
    playAudioFromApi,
    stopAllAudio,
    stopAudio,
    isPlaying,
    clearAudioCache
  }
}

