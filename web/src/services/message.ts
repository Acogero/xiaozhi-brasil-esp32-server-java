import { http } from './request'
import api from './api'
import type {
  Message,
  Conversation,
  MessageQueryParams,
  ConversationQueryParams,
} from '@/types/message'

export type { Message, Conversation, MessageQueryParams, ConversationQueryParams }

/**
 * Consulta a lista de mensagens
 */
export function queryMessages(params: MessageQueryParams) {
  return http.getPage<Message>(api.message.query, params)
}

/**
 * Exclui uma mensagem
 */
export function deleteMessage(messageId: number) {
  return http.delete(`${api.message.delete}/${messageId}`)
}

/**
 * Exclui mensagens do dispositivo em lote
 */
export function batchDeleteMessages(deviceId: string) {
  return http.delete(api.message.delete, { deviceId })
}

/**
 * Exporta mensagens
 */
export function exportMessages(params: Omit<MessageQueryParams, 'pageNo' | 'pageSize'>) {
  return http.get(api.message.export, params)
}

/**
 * Consulta a lista de sessões
 */
export function queryConversations(params: ConversationQueryParams) {
  return http.getPage<Conversation>(api.message.conversations, params)
}
