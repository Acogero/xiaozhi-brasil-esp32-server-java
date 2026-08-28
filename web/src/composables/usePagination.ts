import { reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { TablePaginationConfig } from 'ant-design-vue'

/**
 * Composable de aprimoramento de paginação
 * Usado para gerenciar de forma unificada a lógica de paginação
 */

export interface UsePaginationOptions {
  /**
   * Quantidade padrão de itens por página
   */
  defaultPageSize?: number
  
  /**
   * Opções de itens por página
   */
  pageSizeOptions?: string[]
  
  /**
   * Indica se exibe o seletor de itens por página
   */
  showSizeChanger?: boolean
  
  /**
   * Indica se exibe o salto rápido de página
   */
  showQuickJumper?: boolean
  
  /**
   * Indica se exibe o total
   */
  showTotal?: boolean
  
  /**
   * Função de formatação do texto do total
   */
  totalFormat?: (total: number) => string
  
  /**
   * Callback de mudança de paginação
   */
  onChange?: (page: number, pageSize: number) => void
  
  /**
   * Callback de mudança de itens por página
   */
  onShowSizeChange?: (page: number, pageSize: number) => void
}

export function usePagination(options: UsePaginationOptions = {}) {
  const { t } = useI18n()
  
  // Configuração de paginação
  const pagination = reactive<TablePaginationConfig>({
    current: 1,
    pageSize: options.defaultPageSize || 10,
    total: 0,
    showSizeChanger: options.showSizeChanger !== false,
    showQuickJumper: options.showQuickJumper !== false,
    pageSizeOptions: options.pageSizeOptions || ['10', '20', '50', '100'],
    showTotal: options.showTotal !== false 
      ? (total: number) => options.totalFormat?.(total) || t('table.total', { total })
      : undefined,
    onChange: (page: number, pageSize: number) => {
      pagination.current = page
      pagination.pageSize = pageSize
      options.onChange?.(page, pageSize)
    },
    onShowSizeChange: (page: number, pageSize: number) => {
      pagination.current = 1 // Ao alterar itens por página, reinicia para a primeira página
      pagination.pageSize = pageSize
      options.onShowSizeChange?.(page, pageSize)
    }
  })
  
  // Página atual
  const currentPage = computed({
    get: () => pagination.current || 1,
    set: (val: number) => {
      pagination.current = val
    }
  })
  
  // Itens por página
  const pageSize = computed({
    get: () => pagination.pageSize || 10,
    set: (val: number) => {
      pagination.pageSize = val
    }
  })
  
  // Total de itens
  const total = computed({
    get: () => pagination.total || 0,
    set: (val: number) => {
      pagination.total = val
    }
  })
  
  // Total de páginas
  const totalPages = computed(() => {
    return Math.ceil(total.value / pageSize.value) || 1
  })
  
  // Indica se é a primeira página
  const isFirstPage = computed(() => currentPage.value === 1)
  
  // Indica se é a última página
  const isLastPage = computed(() => currentPage.value >= totalPages.value)
  
  // Indica se há página anterior
  const hasPrev = computed(() => !isFirstPage.value)
  
  // Indica se há próxima página
  const hasNext = computed(() => !isLastPage.value)
  
  // Intervalo de dados da página atual
  const dataRange = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value + 1
    const end = Math.min(currentPage.value * pageSize.value, total.value)
    return { start, end }
  })
  
  /**
   * Reinicia para a primeira página
   */
  const reset = () => {
    pagination.current = 1
  }
  
  /**
   * Navega para a página especificada
   */
  const goToPage = (page: number) => {
    if (page < 1) page = 1
    if (page > totalPages.value) page = totalPages.value
    pagination.current = page
  }
  
  /**
   * Página anterior
   */
  const prevPage = () => {
    if (hasPrev.value) {
      pagination.current = (pagination.current || 1) - 1
    }
  }
  
  /**
   * Próxima página
   */
  const nextPage = () => {
    if (hasNext.value) {
      pagination.current = (pagination.current || 1) + 1
    }
  }
  
  /**
   * Primeira página
   */
  const firstPage = () => {
    pagination.current = 1
  }
  
  /**
   * Última página
   */
  const lastPage = () => {
    pagination.current = totalPages.value
  }
  
  /**
   * Define o total de itens
   */
  const setTotal = (val: number) => {
    pagination.total = val
    
    // Se a página atual estiver fora do intervalo, navega automaticamente para a última página
    if (currentPage.value > totalPages.value) {
      pagination.current = totalPages.value || 1
    }
  }
  
  /**
   * Define os itens por página
   */
  const setPageSize = (val: number) => {
    pagination.pageSize = val
    pagination.current = 1 // Reinicia para a primeira página
  }
  
  /**
   * Obtém os parâmetros da requisição (usados nas chamadas de API)
   */
  const getRequestParams = () => {
    return {
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      // Compatível com cenários de paginação por offset
      offset: (currentPage.value - 1) * pageSize.value,
    }
  }
  
  /**
   * Processa a mudança da tabela (repassado diretamente ao componente Table do ant-design-vue)
   */
  const handleTableChange = (pag: TablePaginationConfig) => {
    if (pag.current !== undefined) {
      pagination.current = pag.current
    }
    if (pag.pageSize !== undefined) {
      pagination.pageSize = pag.pageSize
    }
  }
  
  return {
    // Objeto de configuração de paginação (usado pelo componente Table)
    pagination,
    
    // Propriedades computadas
    currentPage,
    pageSize,
    total,
    totalPages,
    isFirstPage,
    isLastPage,
    hasPrev,
    hasNext,
    dataRange,
    
    // Métodos
    reset,
    goToPage,
    prevPage,
    nextPage,
    firstPage,
    lastPage,
    setTotal,
    setPageSize,
    getRequestParams,
    handleTableChange
  }
}
