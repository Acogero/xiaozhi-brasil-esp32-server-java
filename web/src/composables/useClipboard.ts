import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'

/**
 * Composable de operações da área de transferência
 * Usado para operações de copiar, colar etc. na área de transferência
 */

export interface UseClipboardOptions {
  /**
   * Mensagem de cópia bem-sucedida
   */
  successMessage?: string
  
  /**
   * Mensagem de falha na cópia
   */
  errorMessage?: string
  
  /**
   * Se deve exibir a mensagem de notificação
   */
  showMessage?: boolean
  
  /**
   * Callback de cópia bem-sucedida
   */
  onSuccess?: (text: string) => void
  
  /**
   * Callback de falha na cópia
   */
  onError?: (error: any) => void
}

export function useClipboard(options: UseClipboardOptions = {}) {
  const { t } = useI18n()
  
  // Estado de cópia
  const copying = ref(false)
  
  // Texto copiado
  const copiedText = ref<string>('')
  
  // Se a API de área de transferência é suportada
  const isSupported = ref(
    typeof navigator !== 'undefined' && 'clipboard' in navigator
  )
  
  /**
   * Copia o texto para a área de transferência
   */
  const copy = async (
    text: string,
    customOptions?: Partial<UseClipboardOptions>
  ): Promise<boolean> => {
    const mergedOptions = { ...options, ...customOptions }
    const showMessage = mergedOptions.showMessage !== false
    
    if (!isSupported.value) {
      // Fallback: usa document.execCommand
      return copyFallback(text, mergedOptions, showMessage)
    }
    
    copying.value = true
    
    try {
      await navigator.clipboard.writeText(text)
      copiedText.value = text
      
      if (showMessage) {
        const successMsg = mergedOptions.successMessage || t('clipboard.copySuccess')
        message.success(successMsg)
      }
      
      mergedOptions.onSuccess?.(text)
      return true
    } catch (error: any) {
      console.error('Falha ao copiar:', error)
      
      if (showMessage) {
        const errorMsg = mergedOptions.errorMessage || t('clipboard.copyFailed')
        message.error(errorMsg)
      }
      
      mergedOptions.onError?.(error)
      return false
    } finally {
      copying.value = false
    }
  }
  
  /**
   * Solução de fallback: usa execCommand
   */
  const copyFallback = (
    text: string,
    mergedOptions: Partial<UseClipboardOptions>,
    showMessage: boolean
  ): boolean => {
    copying.value = true
    
    try {
      // Cria um textarea temporário
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.top = '0'
      textarea.style.left = '-9999px'
      textarea.style.opacity = '0'
      
      document.body.appendChild(textarea)
      textarea.select()
      
      const successful = document.execCommand('copy')
      document.body.removeChild(textarea)
      
      if (successful) {
        copiedText.value = text
        
        if (showMessage) {
          const successMsg = mergedOptions.successMessage || t('clipboard.copySuccess')
          message.success(successMsg)
        }
        
        mergedOptions.onSuccess?.(text)
        return true
      } else {
        throw new Error('execCommand failed')
      }
    } catch (error: any) {
      console.error('Falha ao copiar (solução de fallback):', error)
      
      if (showMessage) {
        const errorMsg = mergedOptions.errorMessage || t('clipboard.copyFailed')
        message.error(errorMsg)
      }
      
      mergedOptions.onError?.(error)
      return false
    } finally {
      copying.value = false
    }
  }
  
  /**
   * Lê o conteúdo da área de transferência
   */
  const paste = async (): Promise<string | null> => {
    if (!isSupported.value) {
      message.warning(t('clipboard.pasteNotSupported'))
      return null
    }
    
    try {
      const text = await navigator.clipboard.readText()
      return text
    } catch (error: any) {
      console.error('Falha ao ler a área de transferência:', error)
      message.error(t('clipboard.pasteFailed'))
      return null
    }
  }
  
  /**
   * Limpa a área de transferência
   */
  const clear = async (): Promise<boolean> => {
    if (!isSupported.value) {
      return false
    }
    
    try {
      await navigator.clipboard.writeText('')
      copiedText.value = ''
      return true
    } catch (error: any) {
      console.error('Falha ao limpar a área de transferência:', error)
      return false
    }
  }
  
  /**
   * Copia um objeto como string JSON
   */
  const copyJSON = async (
    obj: any,
    pretty = true,
    customOptions?: Partial<UseClipboardOptions>
  ): Promise<boolean> => {
    try {
      const json = pretty ? JSON.stringify(obj, null, 2) : JSON.stringify(obj)
      return await copy(json, customOptions)
    } catch (error: any) {
      console.error('Falha na serialização JSON:', error)
      message.error(t('clipboard.jsonError'))
      return false
    }
  }
  
  /**
   * Copia HTML
   */
  const copyHTML = async (
    html: string,
    plainText?: string
  ): Promise<boolean> => {
    if (!isSupported.value) {
      // Fallback: copia texto simples
      return copy(plainText || html.replace(/<[^>]*>/g, ''))
    }
    
    copying.value = true
    
    try {
      const blob = new Blob([html], { type: 'text/html' })
      const textBlob = new Blob([plainText || html], { type: 'text/plain' })
      
      const clipboardItem = new ClipboardItem({
        'text/html': blob,
        'text/plain': textBlob
      })
      
      await navigator.clipboard.write([clipboardItem])
      
      if (options.showMessage !== false) {
        message.success(options.successMessage || t('clipboard.copySuccess'))
      }
      
      return true
    } catch (error: any) {
      console.error('Falha ao copiar HTML:', error)
      
      // Fallback: copia texto simples
      return copy(plainText || html.replace(/<[^>]*>/g, ''))
    } finally {
      copying.value = false
    }
  }
  
  /**
   * Copia imagem
   */
  const copyImage = async (blob: Blob): Promise<boolean> => {
    if (!isSupported.value) {
      message.warning(t('clipboard.imageNotSupported'))
      return false
    }
    
    copying.value = true
    
    try {
      const clipboardItem = new ClipboardItem({
        [blob.type]: blob
      })
      
      await navigator.clipboard.write([clipboardItem])
      
      if (options.showMessage !== false) {
        message.success(options.successMessage || t('clipboard.copySuccess'))
      }
      
      return true
    } catch (error: any) {
      console.error('Falha ao copiar imagem:', error)
      
      if (options.showMessage !== false) {
        message.error(options.errorMessage || t('clipboard.copyFailed'))
      }
      
      return false
    } finally {
      copying.value = false
    }
  }
  
  return {
    // Estado
    copying,
    copiedText,
    isSupported,
    
    // Métodos
    copy,
    paste,
    clear,
    copyJSON,
    copyHTML,
    copyImage
  }
}

