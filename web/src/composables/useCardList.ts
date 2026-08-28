import { shallowRef, computed } from 'vue'
import { useDebounceFn } from '@vueuse/core'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'

/**
 * Composable de gerenciamento de listas em cartões
 * Usado para gerenciar busca, carregamento, skeleton screen etc. de listas em formato de cartão
 */

export interface UseCardListOptions<T extends object> {
  /**
   * Função para obter os dados
   */
  fetchData: () => Promise<T[]>
  
  /**
   * Campos de busca (suporta busca em múltiplos campos)
   */
  searchFields: (keyof T)[]
  
  /**
   * Quantidade padrão de skeletons
   */
  defaultSkeletonCount?: number
  
  /**
   * Atraso do debounce (em milissegundos)
   */
  debounceDelay?: number
  
  /**
   * Se deve carregar os dados automaticamente na inicialização
   */
  immediate?: boolean
  
  /**
   * Tratamento de erros
   */
  onError?: (error: Error) => void
}

export function useCardList<T extends object>(options: UseCardListOptions<T>) {
  const { t } = useI18n()
  
  // Estado de carregamento
  const loading = shallowRef(false)
  
  // Palavra-chave de busca
  const searchQuery = shallowRef('')
  
  // Todos os dados (usa shallowRef para evitar reatividade profunda)
  const allItems = shallowRef<T[]>([])
  
  // Dados filtrados
  const filteredItems = computed(() => {
    if (!searchQuery.value.trim()) {
      return allItems.value
    }
    
    const query = searchQuery.value.toLowerCase()
    return allItems.value.filter(item => {
      return options.searchFields.some(field => {
        const value = item[field as keyof T]
        if (value === null || value === undefined) {
          return false
        }
        return String(value).toLowerCase().includes(query)
      })
    })
  })
  
  // Quantidade de skeletons (calculada dinamicamente)
  const skeletonCount = computed(() => {
    // Se estiver carregando e não houver dados, exibe a quantidade padrão
    if (loading.value && allItems.value.length === 0) {
      return options.defaultSkeletonCount || 6
    }
    // Se houver dados, exibe conforme a quantidade real (no máximo 6)
    return Math.max(1, Math.min(allItems.value.length, 6))
  })
  
  // Se está vazio
  const isEmpty = computed(() => {
    return !loading.value && filteredItems.value.length === 0
  })
  
  // Se há dados
  const hasData = computed(() => {
    return filteredItems.value.length > 0
  })
  
  /**
   * Carrega os dados
   */
  const loadData = async () => {
    loading.value = true
    try {
      const data = await options.fetchData()
      allItems.value = data
    } catch (error) {
      console.error('Falha ao carregar dados:', error)
      const errorMessage = error instanceof Error 
        ? error.message 
        : t('common.loadDataFailed')
      message.error(errorMessage)
      
      // Dispara o callback de erro
      if (error instanceof Error) {
        options.onError?.(error)
      }
    } finally {
      loading.value = false
    }
  }
  
  /**
   * Atualiza os dados
   */
  const refresh = async () => {
    await loadData()
  }
  
  /**
   * Busca com debounce
   */
  const debouncedSearch = useDebounceFn(() => {
    // filteredItems é atualizado automaticamente, não é necessária lógica adicional aqui
    // É possível adicionar análise de busca etc. aqui
  }, options.debounceDelay || 300)
  
  /**
   * Reseta a busca
   */
  const resetSearch = () => {
    searchQuery.value = ''
  }
  
  /**
   * Adiciona um item à lista
   */
  const addItem = (item: T) => {
    allItems.value = [...allItems.value, item]
  }
  
  /**
   * Atualiza um item
   */
  const updateItem = (predicate: (item: T) => boolean, newItem: Partial<T>) => {
    const items = allItems.value
    const index = items.findIndex(item => predicate(item))
    if (index !== -1) {
      const updatedItems = [...items]
      updatedItems[index] = { ...items[index], ...newItem } as T
      allItems.value = updatedItems
    }
  }
  
  /**
   * Remove um item
   */
  const removeItem = (predicate: (item: T) => boolean) => {
    const items = allItems.value
    allItems.value = items.filter(item => !predicate(item))
  }
  
  /**
   * Limpa os dados
   */
  const clear = () => {
    allItems.value = []
    searchQuery.value = ''
  }
  
  // Se o carregamento imediato estiver habilitado, carrega os dados automaticamente
  if (options.immediate !== false) {
    loadData()
  }
  
  return {
    // Estado
    loading,
    searchQuery,
    allItems,
    filteredItems,
    skeletonCount,
    isEmpty,
    hasData,
    
    // Métodos
    loadData,
    refresh,
    debouncedSearch,
    resetSearch,
    addItem,
    updateItem,
    removeItem,
    clear
  }
}
