import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'

/**
 * Validador de arquivo
 */
export interface FileValidator {
  /**
   * Verifica se o arquivo é válido
   * @param file Arquivo a ser validado
   * @returns Resultado da validação: retorna true se for válido, ou a mensagem de erro caso contrário
   */
  validate: (file: File) => true | string
}

/**
 * Configuração de upload por arrastar e soltar
 */
export interface DragUploadOptions {
  /**
   * Validador de arquivo
   */
  validator: FileValidator

  /**
   * Callback de processamento do arquivo
   * @param file Arquivo enviado
   */
  onDrop: (file: File) => void | Promise<void>

  /**
   * Se o upload por arrastar e soltar está habilitado
   * @returns Retorna true para habilitado, false para desabilitado
   */
  enabled?: () => boolean

  /**
   * Configuração dos textos de dica para arrastar e soltar
   */
  messages?: {
    /** Chave i18n do texto de dica principal */
    dragText?: string
    /** Chave i18n do texto de dica auxiliar */
    dragHint?: string
    /** Chave i18n do texto de dica de sucesso */
    successMessage?: string
  }

  /**
   * Se deve exibir a mensagem de sucesso
   */
  showSuccessMessage?: boolean
}

/**
 * Composable genérico de upload por arrastar e soltar
 *
 * Fornece funcionalidade global de upload por arrastar e soltar, com suporte a validação e processamento de arquivo personalizados
 *
 * @example
 * ```ts
 * // Upload de arquivo de áudio
 * const audioValidator: FileValidator = {
 *   validate: (file) => {
 *     const isAudio = file.type.startsWith('audio/') ||
 *                     file.name.match(/\.(wav|mp3|m4a)$/i)
 *     if (!isAudio) return 'common.audioFormatError'
 *
 *     const isLt10M = file.size / 1024 / 1024 < 10
 *     if (!isLt10M) return 'common.audioSizeError'
 *
 *     return true
 *   }
 * }
 *
 * const { isDragging } = useDragUpload({
 *   validator: audioValidator,
 *   onDrop: (file) => {
 *     handleFileUpload(file)
 *   },
 *   enabled: () => activeTab.value === 'upload',
 *   messages: {
 *     dragText: 'firmware.dragDropText',
 *     dragHint: 'firmware.dragDropHint',
 *     successMessage: 'firmware.fileUploadSuccess'
 *   }
 * })
 * ```
 */
export function useDragUpload(options: DragUploadOptions) {
  const { t } = useI18n()

  const {
    validator,
    onDrop,
    enabled = () => true,
    messages = {},
    showSuccessMessage = true
  } = options

  // Estado de arrastar
  const isDragging = ref(false)
  let dragCounter = 0

  // Texto de dica de arrastar
  const dragText = computed(() =>
    messages.dragText ? t(messages.dragText) : t('common.dragDropFile')
  )

  const dragHint = computed(() =>
    messages.dragHint ? t(messages.dragHint) : ''
  )

  /**
   * Trata a entrada do arrasto
   */
  const handleDragEnter = (e: DragEvent) => {
    e.preventDefault()
    e.stopPropagation()

    // Verifica se está habilitado
    if (!enabled()) {
      return
    }

    dragCounter++

    // Verifica se contém arquivos
    if (e.dataTransfer) {
      const types = e.dataTransfer.types
      const hasFiles = types.includes('Files') ||
                       types.includes('application/x-moz-file') ||
                       types.some(type =>
                         type.startsWith('application/') ||
                         type.startsWith('image/') ||
                         type.startsWith('audio/')
                       )

      if (hasFiles || e.dataTransfer.items?.length > 0) {
        isDragging.value = true
      }
    }
  }

  /**
   * Trata o arrasto sobre a área
   */
  const handleDragOver = (e: DragEvent) => {
    e.preventDefault()
    e.stopPropagation()

    // Verifica se está habilitado
    if (!enabled()) {
      return
    }

    if (e.dataTransfer) {
      e.dataTransfer.dropEffect = 'copy'
    }

    // Garante que o estado de arrastar seja mantido
    if (dragCounter > 0 && !isDragging.value) {
      isDragging.value = true
    }
  }

  /**
   * Trata a saída do arrasto
   */
  const handleDragLeave = (e: DragEvent) => {
    e.preventDefault()
    e.stopPropagation()

    // Verifica se está habilitado
    if (!enabled()) {
      return
    }

    dragCounter--

    if (dragCounter === 0) {
      isDragging.value = false
    }
  }

  /**
   * Trata a soltura do arquivo
   */
  const handleDrop = async (e: DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    dragCounter = 0
    isDragging.value = false

    // Verifica se está habilitado
    if (!enabled()) {
      return
    }

    const files = e.dataTransfer?.files
    if (files && files.length > 0) {
      const file = files[0] // pega apenas o primeiro arquivo

      // Garante que o arquivo exista
      if (!file) return

      // Valida o arquivo
      const validationResult = validator.validate(file)
      if (validationResult !== true) {
        message.error(t(validationResult))
        return
      }

      // Processa o arquivo
      try {
        await onDrop(file)

        // Exibe a mensagem de sucesso
        if (showSuccessMessage && messages.successMessage) {
          message.success(t(messages.successMessage))
        }
      } catch (error) {
        console.error('File upload error:', error)
        const errorMessage = error instanceof Error ? error.message : t('common.operationFailed')
        message.error(errorMessage)
      }
    }
  }

  /**
   * Instala os listeners de eventos globais
   */
  const install = () => {
    // Reseta o estado
    dragCounter = 0
    isDragging.value = false

    document.addEventListener('dragenter', handleDragEnter)
    document.addEventListener('dragover', handleDragOver)
    document.addEventListener('dragleave', handleDragLeave)
    document.addEventListener('drop', handleDrop)
  }

  /**
   * Remove os listeners de eventos globais
   */
  const uninstall = () => {
    document.removeEventListener('dragenter', handleDragEnter)
    document.removeEventListener('dragover', handleDragOver)
    document.removeEventListener('dragleave', handleDragLeave)
    document.removeEventListener('drop', handleDrop)

    // Limpa o estado
    dragCounter = 0
    isDragging.value = false
  }

  // Instalação e remoção automáticas
  onMounted(install)
  onBeforeUnmount(uninstall)

  return {
    isDragging,
    dragText,
    dragHint,
    install,
    uninstall
  }
}

/**
 * Validadores de arquivo predefinidos
 */
export const fileValidators = {
  /**
   * Validador de arquivo de áudio (limite de 10 MB)
   */
  audio: {
    validate: (file: File) => {
      const isAudio = file.type.startsWith('audio/') ||
                      file.name.toLowerCase().match(/\.(wav|mp3|m4a|flac|ogg)$/)

      if (!isAudio) {
        return 'common.audioFormatError'
      }

      const isLt10M = file.size / 1024 / 1024 < 10
      if (!isLt10M) {
        return 'common.audioSizeError'
      }

      return true
    }
  } as FileValidator,

  /**
   * Validador de arquivo de imagem (limite de 2 MB)
   */
  image: {
    validate: (file: File) => {
      const isImage = file.type.startsWith('image/')

      if (!isImage) {
        return 'common.onlyImageFiles'
      }

      const isLt2M = file.size / 1024 / 1024 < 2
      if (!isLt2M) {
        return 'common.imageSizeLimit'
      }

      return true
    }
  } as FileValidator,

  /**
   * Validador de arquivo de firmware (arquivos .bin/.hex, limite de 50 MB)
   */
  firmware: {
    validate: (file: File) => {
      const isFirmware = file.name.toLowerCase().endsWith('.bin') ||
                        file.name.toLowerCase().endsWith('.hex')

      if (!isFirmware) {
        return 'firmware.invalidFileType'
      }

      const isLt50M = file.size / 1024 / 1024 < 50
      if (!isLt50M) {
        return 'firmware.fileSizeLimit'
      }

      return true
    }
  } as FileValidator
}
