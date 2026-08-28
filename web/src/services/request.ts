import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { message } from 'ant-design-vue'
import qs from 'qs'
import { useUserStore } from '@/store/user'
import { ROUTES } from '@/router/routes'
import type {
  ApiResponse,
  PageResponse,
  ListResponse,
  EmptyResponse,
  DataResponse,
  PageQueryParams,
  BaseQueryParams
} from '@/types/api'

export interface RequestError extends Error {
  code?: string
  isSilent?: boolean
  isAuthExpired?: boolean
  isRequestCanceled?: boolean
  isForbidden?: boolean
}

// Cria a instância do axios
const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 30000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
})

// Controlador global de cancelamento de requisições: cancela automaticamente todas as requisições em andamento na troca de rota
let globalController = new AbortController()
let authExpiredHandling = false

/**
 * Cancela todas as requisições em andamento (chamado automaticamente pelo guarda de rota)
 */
export function cancelPendingRequests() {
  globalController.abort()
  globalController = new AbortController()
}

function createRequestError(messageText: string, extras: Partial<RequestError> = {}): RequestError {
  const error = new Error(messageText) as RequestError
  Object.assign(error, extras)
  return error
}

function toRequestError(error: unknown, fallbackMessage: string): RequestError {
  if (error instanceof Error) {
    return error as RequestError
  }
  return createRequestError(fallbackMessage)
}

export function isRequestCanceledError(error: unknown): boolean {
  if (!(error instanceof Error)) {
    return false
  }
  const requestError = error as RequestError
  return requestError.isRequestCanceled === true ||
    requestError.code === 'ERR_CANCELED' ||
    requestError.message.includes('canceled') ||
    requestError.message.includes('aborted')
}

export function isAuthExpiredError(error: unknown): boolean {
  return error instanceof Error && (error as RequestError).isAuthExpired === true
}

export function isForbiddenError(error: unknown): boolean {
  return error instanceof Error && (error as RequestError).isForbidden === true
}

export function shouldIgnoreRequestError(error: unknown): boolean {
  return isRequestCanceledError(error) || isAuthExpiredError(error) ||
    (error instanceof Error && (error as RequestError).isSilent === true)
}

function handleAuthExpired(authMessage = 'Sessão expirada, faça login novamente!') {
  const userStore = useUserStore()
  userStore.clearUserInfo()
  userStore.clearToken()
  cancelPendingRequests()

  if (authExpiredHandling) {
    return
  }

  authExpiredHandling = true
  message.destroy('auth-error')
  message.error({
    content: authMessage,
    key: 'auth-error',
    duration: 2,
    onClose: () => {
      authExpiredHandling = false
      if (window.location.pathname !== ROUTES.LOGIN) {
        window.location.replace(ROUTES.LOGIN)
      }
    },
  })
}

// Interceptador de requisição
request.interceptors.request.use(
  (config) => {
    // Adiciona o Token ao cabeçalho da requisição
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }

    // Anexa automaticamente o sinal de cancelamento global (caso a requisição não especifique um signal próprio)
    if (!config.signal) {
      config.signal = globalController.signal
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// Interceptador de resposta
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // Se for uma resposta do tipo blob, retorna diretamente, sem processamento de negócio
    if (response.config.responseType === 'blob') {
      return response
    }

    const { data } = response

    // Trata o código de erro de negócio
    if (data.code === 401) {
      handleAuthExpired()
      return Promise.reject(
        createRequestError(data.message || 'Não autorizado', {
          code: 'ERR_AUTH_EXPIRED',
          isSilent: true,
          isAuthExpired: true,
        })
      )
    }

    if (data.code === 403) {
      return Promise.reject(
        createRequestError(data.message || 'Permissão insuficiente', {
          code: 'ERR_FORBIDDEN',
          isForbidden: true,
        })
      )
    }

    // Retorna apenas a parte dos dados, e não o response inteiro
    return data as unknown as AxiosResponse<ApiResponse>
  },
  (error) => {
    // Verifica se é um erro de cancelamento de requisição (causado por troca rápida de página)
    if (error.code === 'ERR_CANCELED' || error.message?.includes('canceled') || error.message?.includes('aborted')) {
      // O cancelamento da requisição é um comportamento normal, não exibe mensagem de erro
      console.debug('Requisição cancelada:', error.config?.url)
      const requestError = toRequestError(error, 'Requisição cancelada')
      requestError.code = 'ERR_CANCELED'
      requestError.isSilent = true
      requestError.isRequestCanceled = true
      return Promise.reject(requestError)
    }

    if (error.code === 'ECONNABORTED' || error.message?.toLowerCase?.().includes('timeout')) {
      message.error({
        content: 'Tempo de requisição esgotado',
        key: 'timeout-error',
      })
      return Promise.reject(error)
    }

    // Tratamento de erro HTTP
    if (error.response) {
      const { status } = error.response
      if (status === 401) {
        handleAuthExpired()
        const requestError = toRequestError(error, 'Sessão expirada, faça login novamente!')
        requestError.code = 'ERR_AUTH_EXPIRED'
        requestError.isSilent = true
        requestError.isAuthExpired = true
        return Promise.reject(requestError)
      } else if (status === 403) {
        const requestError = toRequestError(error, error.response.data?.message || 'Permissão insuficiente')
        requestError.code = 'ERR_FORBIDDEN'
        requestError.isForbidden = true
        return Promise.reject(requestError)
      } else {
        message.error({
          content: error.response.data?.message || `Falha na requisição (${status})`,
          key: 'request-error',
        })
      }
    } else if (error.request) {
      message.error({
        content: 'Erro de rede, verifique sua conexão com a internet',
        key: 'network-error',
      })
    } else {
      // Outros erros (como erro de configuração da requisição, etc.)
      message.error({
        content: error.message || 'Falha na requisição',
        key: 'unknown-error',
      })
    }
    return Promise.reject(error)
  },
)

// Exporta os métodos de requisição
export default request

/**
 * Métodos de conveniência para requisições HTTP
 * Por padrão, envia os dados no formato JSON; para o formato de formulário, utilize postForm
 * Cancela automaticamente todas as requisições em andamento na troca de rota, sem necessidade de tratamento manual
 */
export const http = {
  /**
   * Requisição GET
   */
  get<T = unknown>(url: string, params?: Record<string, unknown>): Promise<DataResponse<T>> {
    return request.get(url, { params })
  },

  /**
   * Requisição POST (formato JSON)
   */
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<DataResponse<T>> {
    return request.post(url, data, config)
  },

  /**
   * Requisição POST (multipart/form-data)
   */
  postMultipart<T = unknown>(url: string, data: FormData, config?: AxiosRequestConfig): Promise<DataResponse<T>> {
    return request.post(url, data, {
      ...config,
      headers: {
        ...config?.headers,
        'Content-Type': 'multipart/form-data',
      },
    })
  },

  /**
   * Requisição POST (formato form-urlencoded)
   */
  postForm<T = unknown>(url: string, data?: Record<string, unknown>): Promise<DataResponse<T>> {
    return request.post(url, qs.stringify(data), {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
      },
    })
  },

  /**
   * Requisição PUT (formato JSON)
   */
  put<T = unknown>(url: string, data?: unknown): Promise<DataResponse<T>> {
    return request.put(url, data)
  },

  /**
   * Requisição PATCH (formato JSON)
   */
  patch<T = unknown>(url: string, data?: unknown): Promise<DataResponse<T>> {
    return request.patch(url, data)
  },

  /**
   * Requisição DELETE (via parâmetros de consulta)
   */
  delete<T = unknown>(url: string, params?: Record<string, unknown>): Promise<DataResponse<T>> {
    return request.delete(url, { params })
  },

  /**
   * Requisição DELETE (com corpo JSON)
   */
  deleteBody<T = unknown>(url: string, data?: Record<string, unknown> | unknown[]): Promise<DataResponse<T>> {
    return request.delete(url, { data })
  },

  /**
   * Consulta paginada (GET)
   */
  getPage<T = unknown>(
    url: string,
    params?: PageQueryParams
  ): Promise<PageResponse<T>> {
    return request.get(url, { params })
  },

  /**
   * Consulta em lista (GET, sem paginação)
   */
  getList<T = unknown>(url: string, params?: BaseQueryParams): Promise<ListResponse<T>> {
    return request.get(url, { params })
  },
}
