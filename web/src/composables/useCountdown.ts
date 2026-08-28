import { ref, computed, onBeforeUnmount } from 'vue'

/**
 * Composable de contagem regressiva
 * Usado em cenários como contagem regressiva de código de verificação, promoções por tempo limitado etc.
 */

export interface UseCountdownOptions {
  /**
   * Segundos iniciais da contagem regressiva
   */
  initialCount?: number
  
  /**
   * Callback de término da contagem regressiva
   */
  onFinish?: () => void
  
  /**
   * Callback a cada segundo
   */
  onTick?: (count: number) => void
  
  /**
   * Se deve iniciar automaticamente
   */
  autoStart?: boolean
  
  /**
   * Intervalo de tempo (em milissegundos)
   */
  interval?: number
}

export function useCountdown(options: UseCountdownOptions = {}) {
  const initialCount = options.initialCount || 60
  const interval = options.interval || 1000
  
  // Valor atual da contagem regressiva
  const count = ref(0)
  
  // Se a contagem regressiva está em andamento
  const counting = ref(false)
  
  // Temporizador
  let timer: ReturnType<typeof setInterval> | null = null
  
  // Se já foi concluída
  const isFinished = computed(() => count.value === 0 && !counting.value)
  
  // Exibição de tempo formatada (MM:SS)
  const formattedTime = computed(() => {
    const minutes = Math.floor(count.value / 60)
    const seconds = count.value % 60
    return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  })
  
  // Texto da contagem regressiva (comumente usado em botões)
  const countdownText = computed(() => {
    return counting.value ? `${count.value}s` : ''
  })
  
  /**
   * Inicia a contagem regressiva
   */
  const start = (seconds: number = initialCount): boolean => {
    // Se já estiver em contagem regressiva, não inicia novamente
    if (counting.value) {
      return false
    }
    
    count.value = seconds
    counting.value = true
    
    timer = setInterval(() => {
      count.value--
      
      // Dispara o callback a cada segundo
      options.onTick?.(count.value)
      
      // Contagem regressiva encerrada
      if (count.value <= 0) {
        stop()
        options.onFinish?.()
      }
    }, interval)
    
    return true
  }
  
  /**
   * Interrompe a contagem regressiva
   */
  const stop = () => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    counting.value = false
  }
  
  /**
   * Pausa a contagem regressiva
   */
  const pause = () => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    counting.value = false
  }
  
  /**
   * Retoma a contagem regressiva
   */
  const resume = () => {
    if (count.value > 0 && !counting.value) {
      counting.value = true
      
      timer = setInterval(() => {
        count.value--
        
        options.onTick?.(count.value)
        
        if (count.value <= 0) {
          stop()
          options.onFinish?.()
        }
      }, interval)
    }
  }
  
  /**
   * Reseta a contagem regressiva
   */
  const reset = (seconds: number = initialCount) => {
    stop()
    count.value = seconds
  }
  
  /**
   * Reinicia a contagem regressiva
   */
  const restart = (seconds: number = initialCount) => {
    stop()
    start(seconds)
  }
  
  /**
   * Define o valor da contagem regressiva
   */
  const setCount = (seconds: number) => {
    count.value = seconds
  }
  
  /**
   * Aumenta a contagem regressiva (prolonga o tempo)
   */
  const addTime = (seconds: number) => {
    count.value += seconds
  }
  
  /**
   * Reduz a contagem regressiva (diminui o tempo)
   */
  const reduceTime = (seconds: number) => {
    count.value = Math.max(0, count.value - seconds)
    
    if (count.value === 0 && counting.value) {
      stop()
      options.onFinish?.()
    }
  }
  
  // Início automático
  if (options.autoStart) {
    start()
  }
  
  // Limpa o temporizador ao desmontar o componente
  onBeforeUnmount(() => {
    stop()
  })
  
  return {
    // Estado
    count,
    counting,
    isFinished,
    formattedTime,
    countdownText,
    
    // Métodos
    start,
    stop,
    pause,
    resume,
    reset,
    restart,
    setCount,
    addTime,
    reduceTime
  }
}

