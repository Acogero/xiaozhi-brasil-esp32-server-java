/**
 * Tipo de configuração
 */
export type ConfigType = 'llm' | 'stt' | 'tts' | 'agent' | 'oss'
export type ModelType = 'chat' | 'vision' | 'intent' | 'embedding'

/**
 * Interface de informações de configuração
 */
export interface Config {
  configId?: number
  configType: ConfigType
  provider: string
  configName: string
  configDesc?: string
  modelType?: ModelType
  isDefault?: string | boolean // 1-padrão 0-não padrão, o formulário usa boolean
  state?: string
  createTime?: string
  // Campos relacionados à API
  appId?: string
  apiKey?: string
  apiSecret?: string
  ak?: string
  sk?: string
  apiUrl?: string
  enableThinking?: boolean
  // Suporte a campos dinâmicos
  [key: string]: any
}

import type { PageQueryParams } from './api'

/**
 * Parâmetros de consulta de configuração
 */
export interface ConfigQueryParams extends PageQueryParams {
  configType: ConfigType
  provider?: string
  configName?: string
  modelType?: string,
  state?: string
}

/**
 * Definição de campo de configuração
 */
export interface ConfigField {
  name: string
  label: string
  required: boolean
  inputType?: string  // 'text' | 'password' | 'select'
  placeholder?: string
  span?: number
  help?: string
  suffix?: string
  defaultUrl?: string
  options?: Array<{ label: string; value: string }>  // Opções do select (usado quando inputType é 'select')
}

/**
 * Informações do tipo de configuração
 */
export interface ConfigTypeInfo {
  label: string
  permissionPrefix?: string
  typeOptions?: Array<{ value: string; label: string; key?: string; configNameOptions?: string[] }>
  typeFields?: Record<string, ConfigField[]>
}

/**
 * Opção de modelo
 */
export interface ModelOption {
  value: string
  label: string
}

/**
 * Informações do modelo da fábrica LLM
 */
export interface LLMModel {
  llm_name: string
  model_type: string
  max_tokens?: number
  is_tools?: boolean
  tags?: string
}

/**
 * Informações da fábrica LLM
 */
export interface LLMFactory {
  name: string
  llm: LLMModel[]
  url?: string
  rank?: string
  status?: string
}
