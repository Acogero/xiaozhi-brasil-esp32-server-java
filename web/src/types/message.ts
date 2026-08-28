import type { PageQueryParams } from './api'

/**
 * Remetente da mensagem
 */
export type MessageSender = 'user' | 'assistant' | 'system'

/**
 * Interface de informações da mensagem (alinhada ao MessageResp do backend)
 */
export interface Message {
  messageId: number
  deviceId: string
  deviceName?: string
  sender: MessageSender
  message: string
  audioPath?: string
  state?: string
  messageType?: string
  toolCalls?: string
  sessionId?: string
  /** Origem da mensagem: 'web' | 'device' */
  source?: string
  roleId?: number
  roleName?: string
  createTime?: string
  updateTime?: string
  // Campos de extensão do frontend (não retornados pelo backend)
  audioLoadError?: boolean
}

/**
 * Interface de informações da conversa (alinhada ao ConversationResp do backend)
 */
export interface Conversation {
  sessionId: string
  roleId: number
  roleName: string
  title: string
  updateTime: string
}

/**
 * Parâmetros de consulta de mensagem (alinhados ao MessagePageReq do backend)
 */
export interface MessageQueryParams extends PageQueryParams {
  deviceId?: string
  deviceName?: string
  sender?: string
  messageType?: string
  roleId?: number
  startTime?: string
  endTime?: string
  sessionId?: string
  /** Filtro de origem da mensagem: 'web' | 'device' */
  source?: string
}

/**
 * Parâmetros de consulta de conversa (alinhados ao ConversationPageReq do backend)
 */
export interface ConversationQueryParams extends PageQueryParams {
  roleId?: number
  /** Filtro de origem da mensagem: 'web' | 'device' */
  source?: string
}
