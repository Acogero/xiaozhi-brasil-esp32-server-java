import type { PageQueryParams } from './api'

// Tipo de modelo
export type ModelType = 'llm' | 'agent'

// Tipo de provedor de voz
export type VoiceProvider = 'edge' | 'aliyun' | 'aliyun-nls' | 'volcengine' | 'xfyun' | 'minimax' | 'tencent' | 'sherpa-onnx'

// Gênero da voz
export type VoiceGender = '' | 'male' | 'female'

// Tipo de memória
export type MemoryType = 'window' | 'summary'

// Dados da função (role)
export interface Role {
  createTime?: string
  updateTime?: string
  userId?: number
  startTime?: string
  endTime?: string
  roleId: number
  avatar?: string
  roleName: string
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
  isDefault?: string | number // O servidor retorna string '1' ou '0', o frontend pode converter para número
  totalDevice?: number
  ttsPitch?: number // Tom de voz (0.5-2.0)
  ttsSpeed?: number // Velocidade de voz (0.5-2.0)
  memoryType?: MemoryType // Tipo de memória
}

export interface RoleQueryParams extends PageQueryParams {
  roleName?: string
  isDefault?: number
}

export interface VoiceOption {
  label?: string
  value?: string
  gender: VoiceGender
  provider: VoiceProvider
  ttsId?: number
  model?: string
}

export interface ModelOption {
  label: string
  value: number
  desc?: string
  type: ModelType
  provider: string
  configName?: string
  configDesc?: string
  agentName?: string
  agentDesc?: string
}

export interface SttOption {
  label: string
  value: number
  desc?: string
}

export interface PromptTemplate {
  templateId: number
  templateName: string
  templateContent: string
  isDefault?: boolean | number
}

export interface RoleFormData {
  roleId?: number
  roleName: string
  roleDesc?: string
  avatar?: string
  isDefault: boolean | number | string // Suporta booleano, número e string (convertido para '1' ou '0' ao enviar)
  state?: string
  // Relacionado ao modelo
  modelType: ModelType
  modelId?: number
  temperature?: number
  topP?: number
  // Relacionado ao reconhecimento de voz
  sttId: number
  vadSpeechTh?: number
  vadSilenceTh?: number
  vadEnergyTh?: number
  vadSilenceMs?: number
  // Relacionado à síntese de voz
  voiceName?: string
  ttsId?: number
  gender?: VoiceGender
  ttsPitch?: number
  ttsSpeed?: number
  // Tipo de memória
  memoryType?: MemoryType
}

// Parâmetros de teste de voz
export interface TestVoiceParams {
  voiceName: string
  ttsId: number
  message: string
  provider: string
  ttsPitch?: number
  ttsSpeed?: number
}

