import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'

/**
 * Composable de gerenciamento de upload de arquivos
 * Gerencia de forma unificada o upload, a validação, o progresso etc. de arquivos
 */

export interface UseFileUploadOptions {
  /**
   * Tipos de arquivo aceitos
   * Exemplo: '.pdf,.docx' ou 'image/*' ou 'application/pdf'
   */
  accept?: string
  
  /**
   * Tamanho máximo do arquivo (em MB)
   */
  maxSize?: number
  
  /**
   * Se o upload de múltiplos arquivos é suportado
   */
  multiple?: boolean
  
  /**
   * Upload automático (envia imediatamente após selecionar o arquivo)
   */
  autoUpload?: boolean
  
  /**
   * Função de callback do upload
   * @param files Lista de arquivos a serem enviados
   * @returns Se o upload foi bem-sucedido
   */
  onUpload?: (files: File[]) => Promise<boolean | void>
  
  /**
   * Callback de alteração de arquivo
   */
  onChange?: (files: File[]) => void
  
  /**
   * Callback de progresso do upload
   */
  onProgress?: (progress: number) => void
  
  /**
   * Callback de validação (lógica de validação personalizada)
   * @returns true indica que a validação passou; false ou uma mensagem de erro indica falha na validação
   */
  customValidate?: (file: File) => boolean | string
}

export interface FileItem {
  uid: string
  file: File
  name: string
  size: number
  type: string
  status: 'ready' | 'uploading' | 'success' | 'error'
  progress: number
  url?: string
  error?: string
  response?: unknown  // dados de resposta do upload
}

export function useFileUpload(options: UseFileUploadOptions = {}) {
  const { t } = useI18n()
  
  // Estado do upload
  const uploading = ref(false)
  
  // Lista de arquivos
  const fileList = ref<FileItem[]>([])
  
  // Progresso total
  const totalProgress = ref(0)
  
  // Quantidade de arquivos
  const fileCount = computed(() => fileList.value.length)
  
  // Se há arquivos
  const hasFiles = computed(() => fileCount.value > 0)
  
  // Se todos foram enviados com sucesso
  const allSuccess = computed(() => {
    if (fileCount.value === 0) return false
    return fileList.value.every(item => item.status === 'success')
  })
  
  // Se há erros
  const hasError = computed(() => {
    return fileList.value.some(item => item.status === 'error')
  })
  
  /**
   * Valida o tipo do arquivo
   */
  const validateFileType = (file: File): boolean => {
    if (!options.accept) return true
    
    const acceptTypes = options.accept.split(',').map(t => t.trim())
    const fileExt = '.' + file.name.split('.').pop()?.toLowerCase()
    const fileType = file.type.toLowerCase()
    
    const isValid = acceptTypes.some(type => {
      // Correspondência por wildcard, como image/*
      if (type.includes('*')) {
        const [mainType] = type.split('/')
        if (!mainType) return false
        return fileType.startsWith(mainType)
      }
      // Correspondência por extensão
      if (type.startsWith('.')) {
        return type === fileExt
      }
      // Correspondência por tipo MIME
      return type === fileType
    })
    
    if (!isValid) {
      message.error(t('upload.invalidFileType', { types: options.accept }))
    }
    
    return isValid
  }
  
  /**
   * Valida o tamanho do arquivo
   */
  const validateFileSize = (file: File): boolean => {
    if (!options.maxSize) return true
    
    const maxBytes = options.maxSize * 1024 * 1024
    const isValid = file.size <= maxBytes
    
    if (!isValid) {
      message.error(t('upload.fileTooLarge', { size: options.maxSize }))
    }
    
    return isValid
  }
  
  /**
   * Valida o arquivo
   */
  const validateFile = (file: File): boolean => {
    // Validação básica
    if (!validateFileType(file)) return false
    if (!validateFileSize(file)) return false
    
    // Validação personalizada
    if (options.customValidate) {
      const result = options.customValidate(file)
      if (result === false) {
        message.error(t('upload.validationFailed'))
        return false
      }
      if (typeof result === 'string') {
        message.error(result)
        return false
      }
    }
    
    return true
  }
  
  /**
   * Cria o item de arquivo
   */
  const createFileItem = (file: File): FileItem => {
    return {
      uid: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      file,
      name: file.name,
      size: file.size,
      type: file.type,
      status: 'ready',
      progress: 0
    }
  }
  
  /**
   * Trata a seleção de arquivo
   */
  const handleFileChange = async (files: File[] | FileList) => {
    const fileArray = Array.from(files)

    // Valida o arquivo
    const validFiles = fileArray.filter(validateFile)
    if (validFiles.length === 0) return
    
    // Modo de arquivo único: substitui a lista de arquivos
    // Modo de múltiplos arquivos: adiciona o arquivo
    const newFileItems = validFiles.map(createFileItem)
    
    if (options.multiple) {
      fileList.value.push(...newFileItems)
    } else {
      fileList.value = newFileItems
    }
    
    // Dispara o callback de alteração
    options.onChange?.(validFiles)
    
    // Upload automático
    if (options.autoUpload && options.onUpload) {
      await upload(validFiles)
    }
  }
  
  /**
   * Envia o arquivo
   */
  const upload = async (files?: File[]): Promise<boolean> => {
    if (!options.onUpload) {
      console.warn('Função de upload não configurada')
      return false
    }
    
    // Se nenhum arquivo for especificado, envia todos os arquivos ainda não enviados
    const filesToUpload = files || fileList.value
      .filter(item => item.status === 'ready' || item.status === 'error')
      .map(item => item.file)
    
    if (filesToUpload.length === 0) {
      message.warning(t('upload.noFilesToUpload'))
      return false
    }
    
    uploading.value = true
    totalProgress.value = 0
    
    try {
      // Atualiza o estado do arquivo para "enviando"
      filesToUpload.forEach(file => {
        const item = fileList.value.find(f => f.file === file)
        if (item) {
          item.status = 'uploading'
          item.progress = 0
        }
      })
      
      // Executa o upload
      const result = await options.onUpload(filesToUpload)
      
      // Upload bem-sucedido
      if (result !== false) {
        filesToUpload.forEach(file => {
          const item = fileList.value.find(f => f.file === file)
          if (item) {
            item.status = 'success'
            item.progress = 100
          }
        })
        totalProgress.value = 100
        message.success(t('upload.success'))
        return true
      } else {
        // Falha no upload
        filesToUpload.forEach(file => {
          const item = fileList.value.find(f => f.file === file)
          if (item) {
            item.status = 'error'
            item.error = t('upload.failed')
          }
        })
        return false
      }
    } catch (error: any) {
      console.error('Falha no upload:', error)
      
      // Marca como falha
      filesToUpload.forEach(file => {
        const item = fileList.value.find(f => f.file === file)
        if (item) {
          item.status = 'error'
          item.error = error.message || t('upload.failed')
        }
      })
      
      message.error(error.message || t('upload.failed'))
      return false
    } finally {
      uploading.value = false
    }
  }
  
  /**
   * Atualiza o progresso do arquivo
   */
  const updateProgress = (fileUid: string, progress: number) => {
    const item = fileList.value.find(f => f.uid === fileUid)
    if (item) {
      item.progress = progress
      
      // Calcula o progresso total
      const total = fileList.value.reduce((sum, f) => sum + f.progress, 0)
      totalProgress.value = Math.round(total / fileList.value.length)
      
      options.onProgress?.(totalProgress.value)
    }
  }
  
  /**
   * Remove o arquivo
   */
  const removeFile = (fileUid: string) => {
    const index = fileList.value.findIndex(f => f.uid === fileUid)
    if (index !== -1) {
      fileList.value.splice(index, 1)
    }
  }
  
  /**
   * Limpa a lista de arquivos
   */
  const clearFiles = () => {
    fileList.value = []
    totalProgress.value = 0
  }
  
  /**
   * Tenta reenviar os arquivos com falha no upload
   */
  const retryFailed = async () => {
    const failedFiles = fileList.value
      .filter(item => item.status === 'error')
      .map(item => item.file)
    
    if (failedFiles.length > 0) {
      await upload(failedFiles)
    }
  }
  
  return {
    // Estado
    uploading,
    fileList,
    totalProgress,
    fileCount,
    hasFiles,
    allSuccess,
    hasError,
    
    // Métodos
    handleFileChange,
    upload,
    updateProgress,
    removeFile,
    clearFiles,
    retryFailed,
    validateFile
  }
}
