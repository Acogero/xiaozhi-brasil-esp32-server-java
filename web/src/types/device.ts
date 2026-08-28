/**
 * Interface de informações do dispositivo
 */
export interface Device {
  createTime?: string
  updateTime?: string
  lastLogin?: string
  roleId?: number
  avatar?: string
  roleName?: string
  roleDesc?: string
  voiceName?: string
  state?: string
  ttsId?: number
  modelId?: number
  modelName?: string
  sttId?: number
  temperature?: number
  topP?: number
  vadEnergyTh?: number
  vadSpeechTh?: number
  vadSilenceTh?: number
  vadSilenceMs?: number
  modelProvider?: string
  ttsProvider?: string
  isDefault?: string
  totalDevice?: number
  deviceId: string
  sessionId?: string
  deviceName?: string
  totalMessage?: number
  audioPath?: string
  wifiName?: string
  ip?: string
  chipModelName?: string
  type?: string
  version?: string
  functionNames?: string
  location?: string
  editable?: boolean // Estado de edição na tabela
}

import type { PageQueryParams } from './api'

/**
 * Parâmetros de consulta de dispositivo
 */
export interface DeviceQueryParams extends PageQueryParams {
  deviceId?: string
  deviceName?: string
  roleName?: string
  state?: string | number
}

// Removida a definição de tipo de resposta duplicada, usa PageResponse<Device> unificado

/**
 * Interface de informações de função (role)
 */
export interface Role {
  roleId: number
  roleName: string
  roleDesc?: string
  prompt?: string
  model?: string
  ttsVoice?: string
  createTime?: string
  updateTime?: string
}


