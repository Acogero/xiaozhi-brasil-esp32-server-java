<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { PlayCircleOutlined, PauseCircleOutlined } from '@ant-design/icons-vue'
import WaveSurfer from 'wavesurfer.js'
import { getResourceUrl } from '@/utils/resource'
import { useEventBus } from '@vueuse/core'

interface Props {
  audioUrl: string
  autoPlay?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  autoPlay: false,
})

// Estado
const wavesurfer = ref<WaveSurfer | null>(null)
const isPlaying = ref(false)
const loading = ref(true)
const loadError = ref(false)
const playerId = ref('')
const waveformRef = ref<HTMLDivElement>()

// Usa o barramento de eventos do VueUse
const audioPlayBus = useEventBus<string>('audio-play')
const stopAllAudioBus = useEventBus<void>('stop-all-audio')

/**
 * Inicializa o WaveSurfer
 */
function initWaveSurfer() {
  if (!waveformRef.value) {
    console.error('Contêiner do WaveSurfer não encontrado')
    return
  }

  try {
    // Cria a instância do wavesurfer
    wavesurfer.value = WaveSurfer.create({
      container: waveformRef.value,
      waveColor: 'var(--ant-color-border)',
      progressColor: 'var(--ant-color-primary)',
      cursorColor: 'transparent',
      barWidth: 2,
      barRadius: 2,
      barGap: 1,
      height: 40,
      normalize: true,
    })
  } catch (error) {
    console.error('Falha ao inicializar o WaveSurfer:', error)
    loading.value = false
    loadError.value = true
    return
  }

  // Escuta de eventos
  wavesurfer.value.on('ready', () => {
    loading.value = false
    if (props.autoPlay && wavesurfer.value) {
      wavesurfer.value.play()
    }
  })

  wavesurfer.value.on('play', () => {
    isPlaying.value = true
    // Notifica outros players para pausar
    audioPlayBus.emit(playerId.value)
  })

  wavesurfer.value.on('pause', () => {
    isPlaying.value = false
  })

  wavesurfer.value.on('finish', () => {
    isPlaying.value = false
    // Reposiciona o cursor para o início após o término da reprodução
    if (wavesurfer.value) {
      wavesurfer.value.seekTo(0)
    }
  })

  wavesurfer.value.on('error', (_err: unknown) => {
    loading.value = false
    loadError.value = true
  })

  // Carrega o áudio
  if (props.audioUrl) {
    loadAudio(props.audioUrl)
  }
}

/**
 * Carrega o áudio
 */
function loadAudio(url: string) {
  if (!url) {
    return
  }
  
  if (!wavesurfer.value) {
    loadError.value = true
    return
  }

  loading.value = true
  loadError.value = false

  try {
    // Verifica se é uma Blob URL ou Data URL
    if (url.startsWith('blob:') || url.startsWith('data:')) {
      // Para blob URL, carrega diretamente
      wavesurfer.value.load(url)
    } else {
      // Usa a função unificada de tratamento de URL de recurso
      const audioUrl = getResourceUrl(url)
      if (audioUrl) {
        wavesurfer.value.load(audioUrl)
      } else {
        loading.value = false
        loadError.value = true
      }
    }
  } catch (error) {
    loading.value = false
    loadError.value = true
  }
}

/**
 * Alterna reprodução/pausa
 */
function togglePlay() {
  if (loading.value || !wavesurfer.value) return
  wavesurfer.value.playPause()
}

// Escuta o evento de reprodução de outros players
audioPlayBus.on((id) => {
  if (id !== playerId.value && isPlaying.value && wavesurfer.value) {
    wavesurfer.value.pause()
  }
})

// Escuta o evento global de parada
stopAllAudioBus.on(() => {
  if (isPlaying.value && wavesurfer.value) {
    wavesurfer.value.pause()
  }
})

// Observa a mudança de audioUrl
watch(
  () => props.audioUrl,
  (newUrl) => {
    if (wavesurfer.value && newUrl) {
      loading.value = true
      loadError.value = false
      loadAudio(newUrl)
    } else if (!wavesurfer.value && newUrl) {
      loadError.value = true
    }
  },
)

onMounted(() => {
  // Gera um ID único
  playerId.value = `player_${Date.now()}_${Math.floor(Math.random() * 1000)}`
  initWaveSurfer()
})

onBeforeUnmount(() => {
  if (wavesurfer.value) {
    if (isPlaying.value) {
      wavesurfer.value.pause()
    }
    wavesurfer.value.destroy()
  }
})
</script>

<template>
  <div v-if="loadError" class="audio-error">
    <span style="color: var(--ant-color-text-tertiary)">Falha ao carregar o áudio</span>
  </div>
  <div v-else class="audio-player-container">
    <div class="player-controls">
      <a-button
        type="primary"
        shape="circle"
        size="small"
        :loading="loading"
        :disabled="loadError"
        @click="togglePlay"
      >
        <template #icon>
          <PauseCircleOutlined v-if="isPlaying" />
          <PlayCircleOutlined v-else />
        </template>
      </a-button>
    </div>
    <div ref="waveformRef" class="waveform-container"></div>
  </div>
</template>

<style scoped lang="scss">
.audio-player-container {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 5px;
  min-height: 50px;
}

.player-controls {
  margin-right: 10px;
}

.waveform-container {
  flex: 1;
  height: 40px;
}

.audio-error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 5px;
  min-height: 50px;
}
</style>

