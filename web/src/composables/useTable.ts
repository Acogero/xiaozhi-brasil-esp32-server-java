import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import type { TablePaginationConfig } from 'ant-design-vue'
import { useDebounceFn } from '@vueuse/core'
import type { PageResponse } from '@/types/api'
import { shouldIgnoreRequestError } from '@/services/request'

/**
 * Composable de gerenciamento de paginação de tabela (versão aprimorada)
 */
export function useTable<T = any>() {
  const { t } = useI18n()
  const loading = ref<boolean>(false)
  const data = ref<T[]>([])

  // Configuração de paginação
  const pagination = reactive<TablePaginationConfig>({
    current: 1,
    pageSize: 10,
    total: 0,
    showTotal: (total: number) => t('table.total', { total }),
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: ['10', '30', '50', '100', '1000'],
  })

  /**
   * Processa a mudança de paginação
   */
  const handleTableChange = (pag: TablePaginationConfig) => {
    pagination.current = pag.current
    pagination.pageSize = pag.pageSize
  }

  /**
   * Reinicia para a primeira página
   */
  const resetPagination = () => {
    pagination.current = 1
  }

  /**
   * Carrega dados (com tratamento de erros)
   */
  const loadData = async (
    fetchFn: (params: { pageNo: number; pageSize: number }) => Promise<PageResponse<T>>,
    options?: {
      showError?: boolean
      onSuccess?: () => void
      onError?: (error: unknown) => void
    }
  ) => {
    const { showError = true, onSuccess, onError } = options || {}

    try {
      loading.value = true
      const res = await fetchFn({
        pageNo: pagination.current || 1,
        pageSize: pagination.pageSize || 10,
      })

      if (res.code === 200) {
        data.value = res.data?.list || []
        pagination.total = res.data?.total || 0
        onSuccess?.()
      } else {
        if (showError) {
          message.error(res.message || t('common.loadDataFailed'))
        }
        onError?.(res)
      }
    } catch (error: unknown) {
      if (shouldIgnoreRequestError(error)) {
        onError?.(error)
        return
      }
      console.error('Error loading data:', error)
      if (showError) {
        const errorMessage = error instanceof Error 
          ? error.message 
          : t('common.loadDataFailed')
        message.error(errorMessage)
      }
      onError?.(error)
    } finally {
      loading.value = false
    }
  }

  /**
   * Cria uma função de busca com debounce
   */
  const createDebouncedSearch = (searchFn: () => void, delay = 500) => {
    return useDebounceFn(() => {
      resetPagination()
      searchFn()
    }, delay)
  }

  return {
    loading,
    data,
    pagination,
    handleTableChange,
    resetPagination,
    loadData,
    createDebouncedSearch,
  }
}
