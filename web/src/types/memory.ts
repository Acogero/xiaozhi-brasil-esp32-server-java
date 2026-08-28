import type { PageQueryParams } from './api'

/**
 * Definições de tipos de memória
 */

/**
 * Memória resumida
 */
export interface SummaryMemory {
  id: number
  deviceId: string
  roleId: number
  lastMessageTimestamp: string
  summary: string
  promptTokens: number
  completionTokens: number
  createTime: string
}

/**
 * Mensagem de chat (memória de curto prazo/janela)
 */
export interface ChatMemory {
  messageId: string
  deviceId: string
  roleId: number
  message: string
  sender: 'user' | 'assistant'
  createTime: string
  audioPath?: string
  messageType?: string
}

/**
 * Parâmetros de consulta de memória
 */
export interface MemoryQueryParams extends PageQueryParams {
  roleId: number
  deviceId: string
}

/**
 * Estado da view de gerenciamento de memória
 */
export interface MemoryManagementState {
  roleId: number
  roleName: string
  memoryType: 'window' | 'summary'
  selectedDeviceId: string
  devices: Array<{
    deviceId: string
    deviceName: string
  }>
}
