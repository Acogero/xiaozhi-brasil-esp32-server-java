/**
 * Definições de tipos relacionados ao agente inteligente
 */

import type { PageQueryParams } from './api'

/**
 * Parâmetros de consulta do agente inteligente
 */
export interface AgentQueryParams extends PageQueryParams {
  provider: string
  agentName?: string
}

/**
 * Dados do agente inteligente
 */
export interface Agent {
  configId: number
  deviceId?: string
  roleId?: string
  configName?: string
  configDesc?: string
  configType?: string
  modelType?: string
  provider: string
  appId?: string
  apiKey?: string
  apiSecret?: string
  ak?: string
  sk?: string
  apiUrl?: string
  state?: string
  isDefault?: string
  agentId?: string
  agentName?: string
  botId?: string
  agentDesc?: string
  iconUrl?: string
  publishTime?: string
  createTime?: string
  updateTime?: string
}

/**
 * Formulário de configuração da plataforma
 */
export interface PlatformConfig {
  configId?: number
  deviceId?: string
  roleId?: string
  configName?: string
  configDesc?: string
  configType?: string
  modelType?: string
  provider: string
  appId?: string
  apiKey?: string
  apiSecret?: string
  ak?: string
  sk?: string
  apiUrl?: string
  state?: string
  isDefault?: string
  agentId?: string
  agentName?: string
  botId?: string
  agentDesc?: string
  iconUrl?: string
  publishTime?: string
  createTime?: string
  updateTime?: string
}

/**
 * Opções da plataforma
 */
export interface ProviderOption {
  label: string
  value: string
}

/**
 * Configuração do item de formulário
 */
export interface FormItem {
  field: string
  label: string
  placeholder: string
  suffix?: string
  type?: 'input' | 'textarea'
}

/**
 * Mapeamento de itens de formulário por plataforma
 */
export type PlatformFormItems = {
  [key: string]: FormItem[]
}
