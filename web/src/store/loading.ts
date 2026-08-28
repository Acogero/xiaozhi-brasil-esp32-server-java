import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useLoadingStore = defineStore('loading', () => {
  // Estado global de loading
  const isLoading = ref(false)
  const loadingText = ref('Carregando...')

  // Contador de requisições (lida com múltiplas requisições concorrentes)
  const requestCount = ref(0)
  
  // Tempo mínimo de exibição (milissegundos) - evita flicker em operações rápidas
  const MIN_DISPLAY_TIME = 1000
  
  // Registra o horário de exibição
  let showTime = 0
  let hideTimer: ReturnType<typeof setTimeout> | null = null

  // Exibe o loading
  const showLoading = (text = 'Carregando...') => {
    requestCount.value++
    loadingText.value = text
    
    // Se já estiver exibindo, apenas atualiza o texto
    if (isLoading.value) {
      return
    }
    
    // Limpa um possível timer de ocultação existente
    if (hideTimer) {
      clearTimeout(hideTimer)
      hideTimer = null
    }
    
    // Registra o horário de exibição
    showTime = Date.now()
    isLoading.value = true
  }

  // Aguarda o tempo mínimo de exibição
  const awaitMinDisplay = (): Promise<void> => {
    return new Promise((resolve) => {
      const displayedTime = Date.now() - showTime
      const remainingTime = MIN_DISPLAY_TIME - displayedTime
      
      if (remainingTime > 0) {
        setTimeout(resolve, remainingTime)
      } else {
        resolve()
      }
    })
  }

  // Oculta o loading
  const hideLoading = () => {
    requestCount.value--
    if (requestCount.value > 0) {
      return
    }
    
    requestCount.value = 0
    
    // Calcula o tempo já exibido
    const displayedTime = Date.now() - showTime
    const remainingTime = MIN_DISPLAY_TIME - displayedTime
    
    // Se o tempo de exibição for menor que o tempo mínimo, atrasa a ocultação
    if (remainingTime > 0) {
      hideTimer = setTimeout(() => {
        isLoading.value = false
        hideTimer = null
      }, remainingTime)
    } else {
      // Tempo mínimo já atingido, oculta imediatamente
      isLoading.value = false
    }
  }
  
  // Executa uma operação assíncrona com loading (versão otimizada)
  // Aguarda o tempo mínimo antes de executar a operação de dados e fechar o loading
  const withLoading = async <T>(
    apiCall: () => Promise<T>,
    options?: {
      loadingText?: string
      onSuccess?: (data: T) => void | Promise<void>
      onError?: (error: any) => void
    }
  ): Promise<T | null> => {
    showLoading(options?.loadingText)
    
    try {
      // Execução em paralelo: requisição da API e espera do tempo mínimo
      const [data] = await Promise.all([
        apiCall(),
        awaitMinDisplay()
      ])
      
      // Neste ponto o tempo mínimo já foi atingido, executa a operação de dados imediatamente
      if (options?.onSuccess) {
        await options.onSuccess(data)
      }
      
      // Fecha o loading
      requestCount.value = 0
      isLoading.value = false
      if (hideTimer) {
        clearTimeout(hideTimer)
        hideTimer = null
      }
      
      return data
    } catch (error) {
      // Em caso de erro, também aguarda o tempo mínimo, para evitar flicker
      await awaitMinDisplay()
      
      if (options?.onError) {
        options.onError(error)
      }
      
      // Fecha o loading
      requestCount.value = 0
      isLoading.value = false
      if (hideTimer) {
        clearTimeout(hideTimer)
        hideTimer = null
      }
      
      return null
    }
  }

  // Força a ocultação (usado em situações de erro)
  const forceHideLoading = () => {
    requestCount.value = 0
    if (hideTimer) {
      clearTimeout(hideTimer)
      hideTimer = null
    }
    isLoading.value = false
  }

  return {
    isLoading,
    loadingText,
    showLoading,
    hideLoading,
    forceHideLoading,
    withLoading,
    awaitMinDisplay,
  }
})
