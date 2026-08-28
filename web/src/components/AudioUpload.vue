<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { 
  UploadOutlined, 
  DeleteOutlined, 
  InfoCircleOutlined,
  AudioOutlined,
  PauseOutlined
} from '@ant-design/icons-vue'
import AudioPlayer from '@/components/AudioPlayer.vue'
import type { UploadFile } from 'ant-design-vue'

interface Props {
  previewUrl?: string
  fileList: UploadFile[]
  isRecording: boolean
  recordingTime: number
  recordError: string
  isRetraining: boolean
  useOriginalAudio: boolean
  originalAudioPath?: string
}

interface Emits {
  (e: 'file-upload', file: File): void
  (e: 'file-remove'): void
  (e: 'start-recording'): void
  (e: 'stop-recording'): void
  (e: 'clear-audio'): void
  (e: 'restore-original'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const { t } = useI18n()

const audioTabKey = ref('upload')

// Propriedade computada
const hasAudio = computed(() => {
  return props.previewUrl && (
    props.fileList.length > 0 || 
    (props.isRetraining && props.useOriginalAudio && props.originalAudioPath)
  )
})

const recordingStatusText = computed(() => {
  if (props.recordingTime > 0) {
    return `${t('common.recordedTime')} ${formatTime(props.recordingTime)}`
  }
  return t('common.clickToRecord')
})

const isOverRecommendedTime = computed(() => {
  return props.recordingTime > 15
})

// Métodos
const formatTime = (seconds: number) => {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes}:${remainingSeconds < 10 ? '0' : ''}${remainingSeconds}`
}

const beforeUpload = (file: File) => {
  const isAudio = file.type === 'audio/wav' || file.type === 'audio/mp3' || file.type === 'audio/mpeg' || file.name.endsWith('.wav') || file.name.endsWith('.mp3')
  if (!isAudio) {
    message.error(t('common.audioFormatError'))
    return false
  }
  
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    message.error(t('common.audioSizeError'))
    return false
  }

  // Dispara o evento de upload diretamente aqui
  emit('file-upload', file)
  return false // Impede o upload automático
}

const toggleRecording = () => {
  if (props.isRecording) {
    emit('stop-recording')
  } else {
    emit('start-recording')
  }
}

const handleClearAudio = () => {
  emit('clear-audio')
}

const handleRestoreOriginal = () => {
  emit('restore-original')
}
</script>

<template>
  <div class="audio-upload-container">
    <a-alert 
      :message="t('common.audioQualityTip')"
      :description="t('common.audioQualityDesc')" 
      type="info"
      show-icon 
      style="margin-bottom: 20px" 
    />

    <!-- Área de upload/gravação de áudio -->
    <div>
      <a-tabs v-model:active-key="audioTabKey">
        <a-tab-pane key="upload" :tab="t('common.uploadAudio')">
          <div class="upload-area">
            <p>{{ t('common.audioFormatTip') }}</p>
            <a-upload 
              name="file" 
              :multiple="false" 
              :before-upload="beforeUpload" 
              :file-list="fileList"
              accept=".wav,.mp3"
            >
              <a-button>
                <template #icon><UploadOutlined /></template>
                {{ t('common.selectFile') }}
              </a-button>
            </a-upload>
          </div>
        </a-tab-pane>
        
        <a-tab-pane key="record" :tab="t('common.recordAudio')">
          <div class="record-area">
            <p>{{ t('common.recordTip') }}</p>
            <div class="record-controls">
              <a-button 
                type="primary" 
                shape="circle" 
                :icon="isRecording ? 'pause' : 'audio'"
                @click="toggleRecording" 
                style="margin-right: 16px" 
              />
              <span>{{ recordingStatusText }}</span>
              <span v-if="isOverRecommendedTime" style="color: var(--ant-color-error); margin-left: 8px">
                {{ t('common.overRecommendedTime') }}
              </span>
            </div>
            <p v-if="recordError" class="record-error">{{ recordError }}</p>
          </div>
        </a-tab-pane>
      </a-tabs>

      <!-- Pré-visualização do áudio -->
      <div v-if="hasAudio" class="audio-preview">
        <a-divider>{{ t('common.audioPreview') }}</a-divider>
        <AudioPlayer :audio-url="previewUrl || ''" />

        <!-- Exibe o botão de limpar somente quando não estiver usando o áudio original -->
        <a-button 
          v-if="!(isRetraining && useOriginalAudio)" 
          type="link" 
          @click="handleClearAudio"
        >
          <template #icon><DeleteOutlined /></template>
          {{ t('common.clearAudio') }}
        </a-button>

        <!-- Exibe informações da origem do áudio apenas no modo de retreinamento -->
        <div v-if="isRetraining" style="margin-top: 8px; color: var(--ant-color-text-tertiary); font-size: 12px;">
          <InfoCircleOutlined /> 
          {{ useOriginalAudio ? t('common.useOriginalAudio') : t('common.useNewAudio') }}

          <!-- Se um novo áudio foi enviado, oferece a opção de restaurar o áudio original -->
          <a 
            v-if="!useOriginalAudio" 
            style="margin-left: 8px;"
            @click="handleRestoreOriginal"
          >
            {{ t('common.restoreOriginalAudio') }}
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.audio-upload-container {
  background-color: var(--ant-color-fill-quaternary);
  padding: 20px;
  border-radius: 4px;
  border: 1px dashed var(--ant-color-border);
}

.upload-area,
.record-area {
  text-align: center;
  margin-bottom: 16px;
}

.record-controls {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.audio-preview {
  margin-top: 16px;
  padding: 16px;
  background-color: var(--ant-color-fill-tertiary);
  border-radius: 4px;
}

.record-error {
  color: var(--ant-color-error);
  margin-top: 8px;
}
</style>
