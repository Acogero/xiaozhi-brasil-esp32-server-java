/**
 * Composable de tratamento de requisições
 * Integra loading global, tratamento de erros, debounce e outras funcionalidades
 */
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useLoadingStore } from '@/store/loading'
import { useDebounceFn } from '@vueuse/core'
import { shouldIgnoreRequestError } from '@/services/request'

interface RequestOptions<T = unknown> {
  showLoading?: boolean // Indica se exibe o loading global
  loadingText?: string // Texto do loading
  showError?: boolean // Indica se exibe o aviso de erro
  showSuccess?: boolean // Indica se exibe o aviso de sucesso
  successText?: string // Texto do aviso de sucesso
  onSuccess?: (data: T) => void // Callback de sucesso
  onError?: (error: Error) => void // Callback de erro
}

// Guarda de tipo de erro
function isErrorWithMessage(error: unknown): error is { message: string } {
  return typeof error === 'object' && error !== null && 'message' in error
}

function getErrorMessage(error: unknown): string {
  if (isErrorWithMessage(error)) {
    return error.message
  }
  if (typeof error === 'string') {
    return error
  }
  return 'Falha na operação'
}

/**
 * Hook de tratamento de requisições
 */
export function useRequest() {
  const loadingStore = useLoadingStore()
  const loading = ref(false)

  /**
   * Executa a requisição
   */
  const execute = async <T = unknown>(
    requestFn: () => Promise<T>,
    options: RequestOptions<T> = {}
  ): Promise<T | undefined> => {
    const {
      showLoading = false,
      loadingText = 'Carregando...',
      showError = true,
      showSuccess = false,
      successText = 'Operação realizada com sucesso',
      onSuccess,
      onError,
    } = options

    try {
      loading.value = true
      if (showLoading) {
        loadingStore.showLoading(loadingText)
      }

      const result = await requestFn()

      if (showSuccess) {
        message.success(successText)
      }

      if (onSuccess) {
        onSuccess(result)
      }

      return result
    } catch (error: unknown) {
      if (shouldIgnoreRequestError(error)) {
        console.debug('Requisição tratada silenciosamente:', getErrorMessage(error))
        return undefined
      }

      console.error('Request error:', error)

      if (showError) {
        const errorMessage = getErrorMessage(error)
        message.error(errorMessage)
      }

      if (onError && error instanceof Error) {
        onError(error)
      }

      return undefined
    } finally {
      loading.value = false
      if (showLoading) {
        loadingStore.hideLoading()
      }
    }
  }

  /**
   * Cria uma função de requisição com debounce
   */
  const createDebouncedRequest = <T = unknown>(
    requestFn: () => Promise<T>,
    delay = 500,
    options: RequestOptions<T> = {}
  ) => {
    return useDebounceFn(() => execute(requestFn, options), delay)
  }

  return {
    loading,
    execute,
    createDebouncedRequest,
  }
}

/**
 * Executor de requisição simplificado (uso direto, sem valor de retorno)
 */
export async function withLoading<T = unknown>(
  requestFn: () => Promise<T>,
  loadingText = 'Carregando...'
): Promise<T | undefined> {
  const loadingStore = useLoadingStore()

  try {
    loadingStore.showLoading(loadingText)
    return await requestFn()
  } catch (error: unknown) {
    if (shouldIgnoreRequestError(error)) {
      console.debug('Requisição tratada silenciosamente:', getErrorMessage(error))
      return undefined
    }
    
    console.error('Request error:', error)
    const errorMessage = getErrorMessage(error)
    message.error(errorMessage)
    return undefined
  } finally {
    loadingStore.hideLoading()
  }
}

/**
 * Wrapper de tratamento de erros
 */
export async function withErrorHandler<T = unknown>(
  requestFn: () => Promise<T>,
  errorMessage = 'Falha na operação'
): Promise<T | undefined> {
  try {
    return await requestFn()
  } catch (error: unknown) {
    if (shouldIgnoreRequestError(error)) {
      console.debug('Requisição tratada silenciosamente:', getErrorMessage(error))
      return undefined
    }
    
    console.error('Error:', error)
    const msg = getErrorMessage(error) || errorMessage
    message.error(msg)
    return undefined
  }
}
