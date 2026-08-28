import { reactive, computed } from 'vue'

/**
 * Composable de gerenciamento de estado de loading
 * Usado para gerenciar múltiplos estados de carregamento, evitando confusão entre eles
 */

export interface UseLoadingStateOptions {
  /**
   * Estado de carregamento inicial
   */
  initialStates?: Record<string, boolean>
  
  /**
   * Callback de carregamento global
   */
  onLoadingChange?: (key: string, loading: boolean) => void
}

export function useLoadingState(options: UseLoadingStateOptions = {}) {
  // Conjunto de estados de carregamento
  const loadingStates = reactive<Record<string, boolean>>(
    options.initialStates || {}
  )
  
  // Indica se há algum estado em carregamento
  const isAnyLoading = computed(() => {
    return Object.values(loadingStates).some(loading => loading)
  })
  
  // Indica se todos estão em carregamento
  const isAllLoading = computed(() => {
    const keys = Object.keys(loadingStates)
    if (keys.length === 0) return false
    return keys.every(key => loadingStates[key])
  })
  
  // Quantidade de itens em carregamento
  const loadingCount = computed(() => {
    return Object.values(loadingStates).filter(loading => loading).length
  })
  
  // Lista de keys em carregamento
  const loadingKeys = computed(() => {
    return Object.keys(loadingStates).filter(key => loadingStates[key])
  })
  
  /**
   * Define o estado de carregamento
   */
  const setLoading = (key: string, loading: boolean) => {
    loadingStates[key] = loading
    options.onLoadingChange?.(key, loading)
  }
  
  /**
   * Inicia o carregamento
   */
  const startLoading = (key: string) => {
    setLoading(key, true)
  }
  
  /**
   * Finaliza o carregamento
   */
  const stopLoading = (key: string) => {
    setLoading(key, false)
  }
  
  /**
   * Alterna o estado de carregamento
   */
  const toggleLoading = (key: string) => {
    setLoading(key, !loadingStates[key])
  }
  
  /**
   * Verifica se está carregando
   */
  const isLoading = (key: string): boolean => {
    return loadingStates[key] || false
  }
  
  /**
   * Encapsula uma função assíncrona, gerenciando automaticamente o estado de carregamento
   */
  const withLoading = async <T>(
    key: string,
    fn: () => Promise<T>
  ): Promise<T> => {
    startLoading(key)
    try {
      return await fn()
    } finally {
      stopLoading(key)
    }
  }
  
  /**
   * Define estados de carregamento em lote
   */
  const setLoadingBatch = (states: Record<string, boolean>) => {
    Object.entries(states).forEach(([key, loading]) => {
      setLoading(key, loading)
    })
  }
  
  /**
   * Redefine todos os estados de carregamento
   */
  const resetAll = () => {
    Object.keys(loadingStates).forEach(key => {
      loadingStates[key] = false
    })
  }
  
  /**
   * Remove o estado de carregamento especificado
   */
  const clear = (key: string) => {
    delete loadingStates[key]
  }
  
  /**
   * Remove todos os estados de carregamento
   */
  const clearAll = () => {
    Object.keys(loadingStates).forEach(key => {
      delete loadingStates[key]
    })
  }
  
  /**
   * Cria um gerenciador de carregamento com namespace (para múltiplos estados dentro do componente)
   */
  const createNamespace = (namespace: string) => {
    const getKey = (key: string) => `${namespace}:${key}`
    
    return {
      isLoading: (key: string) => isLoading(getKey(key)),
      setLoading: (key: string, loading: boolean) => setLoading(getKey(key), loading),
      startLoading: (key: string) => startLoading(getKey(key)),
      stopLoading: (key: string) => stopLoading(getKey(key)),
      toggleLoading: (key: string) => toggleLoading(getKey(key)),
      withLoading: <T>(key: string, fn: () => Promise<T>) => withLoading(getKey(key), fn),
      resetAll: () => {
        Object.keys(loadingStates)
          .filter(k => k.startsWith(`${namespace}:`))
          .forEach(k => setLoading(k, false))
      },
      clearAll: () => {
        Object.keys(loadingStates)
          .filter(k => k.startsWith(`${namespace}:`))
          .forEach(k => clear(k))
      }
    }
  }
  
  /**
   * Cria um computed de estado de carregamento (para uso combinado)
   */
  const createLoadingComputed = (...keys: string[]) => {
    return computed(() => keys.some(key => isLoading(key)))
  }
  
  /**
   * Aguarda a conclusão de todos os carregamentos especificados
   */
  const waitForAll = async (...keys: string[]): Promise<void> => {
    return new Promise((resolve) => {
      const checkInterval = setInterval(() => {
        if (!keys.some(key => isLoading(key))) {
          clearInterval(checkInterval)
          resolve()
        }
      }, 100)
    })
  }
  
  return {
    // Estado
    loadingStates,
    isAnyLoading,
    isAllLoading,
    loadingCount,
    loadingKeys,
    
    // Métodos
    setLoading,
    startLoading,
    stopLoading,
    toggleLoading,
    isLoading,
    withLoading,
    setLoadingBatch,
    resetAll,
    clear,
    clearAll,
    createNamespace,
    createLoadingComputed,
    waitForAll
  }
}

