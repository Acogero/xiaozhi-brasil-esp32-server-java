/**
 * Definição de tipos unificada da API
 */

/**
 * Interface base de resposta da API
 */
export interface ApiResponse<T = unknown> {
  code: number
  data: T
  message: string
  timestamp?: number
  success?: boolean
}

/**
 * Interface de dados paginados
 */
export interface PageData<T = unknown> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  size: number
  startRow: number
  endRow: number
  pages: number
  prePage: number
  nextPage: number
  isFirstPage: boolean
  isLastPage: boolean
  hasPreviousPage: boolean
  hasNextPage: boolean
  navigatePages: number
  navigatepageNums: number[]
  navigateFirstPage: number
  navigateLastPage: number
}

/**
 * Interface de resposta paginada
 */
export interface PageResponse<T = unknown> extends ApiResponse<PageData<T>> {
  data: PageData<T>
}

/**
 * Interface de resposta em lista (sem paginação)
 */
export interface ListResponse<T = unknown> extends ApiResponse<T[]> {
  data: T[]
}

/**
 * Interface de resposta genérica (sem dados)
 */
export interface EmptyResponse extends ApiResponse<null> {
  data: null
}

/**
 * Interface de resposta genérica (dados quaisquer)
 */
export interface DataResponse<T = unknown> extends ApiResponse<T> {
  data: T
}

/**
 * Interface base de parâmetros de consulta
 */
export interface BaseQueryParams {
  [key: string]: unknown
}

/**
 * Parâmetros de consulta paginada
 */
export interface PageQueryParams extends BaseQueryParams {
  pageNo?: number
  pageSize?: number
}
