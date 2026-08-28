/**
 * Composable de gerenciamento de personagens
 * Trata de forma unificada a lógica de seleção de modelo e de voz
 */

import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { queryConfigs } from '@/services/config'
import { queryAgents } from '@/services/agent'
import { querySherpaVoices } from '@/services/role'
import type { ModelOption, VoiceOption, SttOption, VoiceProvider } from '@/types/role'
import type { Config } from '@/types/config'
import type { Agent } from '@/types/agent'

export function useRoleManager() {
  // Estado de carregamento
  const modelLoading = ref(false)
  const voiceLoading = ref(false)
  const sttLoading = ref(false)

  // Relacionado a modelos
  const allModels = ref<ModelOption[]>([])
  const selectedModelId = ref<number>()

  // Relacionado a voz - lista de todas as vozes (proveniente de vários arquivos JSON)
  const allVoices = ref<VoiceOption[]>([])
  const selectedVoiceName = ref<string>()

  // Reconhecimento de voz
  const sttOptions = ref<SttOption[]>([])

  // Armazenamento de dados brutos
  const llmConfigs = ref<Config[]>([])
  const agentConfigs = ref<Agent[]>([])
  const ttsConfigs = ref<Config[]>([])

  /**
   * Carrega todos os modelos (LLM + Agent)
   */
  async function loadAllModels() {
    modelLoading.value = true
    try {
      // Carrega LLM e Agent em paralelo
      const [llmRes, cozeRes, difyRes, xingchenRes] = await Promise.all([
        queryConfigs({ configType: 'llm', pageNo: 1, pageSize: 1000 }),
        queryAgents({ provider: 'coze', pageNo: 1, pageSize: 1000 }),
        queryAgents({ provider: 'dify', pageNo: 1, pageSize: 1000 }),
        queryAgents({ provider: 'xingchen', pageNo: 1, pageSize: 1000 })
      ])

      const models: ModelOption[] = []

      // Processa a configuração de LLM (carrega apenas modelos de conversa)
      if (llmRes.code === 200 && llmRes.data?.list) {
        llmConfigs.value = llmRes.data.list
        llmRes.data.list.forEach((config: Config) => {
          // Adiciona apenas modelos de conversa (tipo chat)
          if (config.modelType === 'chat') {
            models.push({
              label: config.configName,
              value: Number(config.configId),
              desc: config.configDesc,
              type: 'llm',
              provider: config.provider || '',
              configName: config.configName,
              configDesc: config.configDesc
            })
          }
        })
      }

      // Processa o Coze Agent
      if (cozeRes.code === 200 && cozeRes.data?.list) {
        cozeRes.data.list.forEach((agent: Agent) => {
          agentConfigs.value.push(agent)
          models.push({
            label: `${agent.agentName} (Agente Coze)`,
            value: agent.configId,
            desc: agent.agentDesc,
            type: 'agent',
            provider: 'coze',
            agentName: agent.agentName,
            agentDesc: agent.agentDesc
          })
        })
      }

      // Processa o Dify Agent
      if (difyRes.code === 200 && difyRes.data?.list) {
        difyRes.data.list.forEach((agent: Agent) => {
          agentConfigs.value.push(agent)
          models.push({
            label: `${agent.agentName} (Agente Dify)`,
            value: agent.configId,
            desc: agent.agentDesc,
            type: 'agent',
            provider: 'dify',
            agentName: agent.agentName,
            agentDesc: agent.agentDesc
          })
        })
      }

      // Processa o XingChen Agent
      if (xingchenRes.code === 200 && xingchenRes.data?.list) {
        xingchenRes.data.list.forEach((agent: Agent) => {
          agentConfigs.value.push(agent)
          models.push({
            label: `${agent.agentName} (Agente XingChen)`,
            value: agent.configId,
            desc: agent.agentDesc,
            type: 'agent',
            provider: 'xingchen',
            agentName: agent.agentName,
            agentDesc: agent.agentDesc
          })
        })
      }

      allModels.value = models
    } catch (error) {
      console.error('Falha ao carregar lista de modelos:', error)
      message.error('Falha ao carregar lista de modelos')
    } finally {
      modelLoading.value = false
    }
  }

  /**
   * Carrega todas as opções de voz (a partir da configuração de TTS e arquivos JSON)
   */
  async function loadAllVoices() {
    voiceLoading.value = true
    try {
      // 1. Carrega a configuração de TTS
      const ttsRes = await queryConfigs({ configType: 'tts', pageNo: 1, pageSize: 1000 })
      if (ttsRes.code === 200 && ttsRes.data?.list) {
        ttsConfigs.value = ttsRes.data.list
      }

      // 2. Carrega em paralelo todos os arquivos JSON de voz e as vozes dinâmicas do sherpa-onnx
      const sherpaConfig = ttsConfigs.value.find(c => c.provider === 'sherpa-onnx')
      const [edgeVoices, aliyunVoices, aliyunNlsVoices, volcengineVoices, xfyunVoices, minimaxVoices, tencentVoices, sherpaRes] = await Promise.all([
        loadVoiceJson('/static/assets/edgeVoicesList.json', 'edge'),
        loadVoiceJson('/static/assets/aliyunVoicesList.json', 'aliyun'),
        loadVoiceJson('/static/assets/aliyunNlsVoicesList.json', 'aliyun-nls'),
        loadVoiceJson('/static/assets/volcengineVoicesList.json', 'volcengine'),
        loadVoiceJson('/static/assets/xfyunVoicesList.json', 'xfyun'),
        loadVoiceJson('/static/assets/minimaxVoicesList.json', 'minimax'),
        loadVoiceJson('/static/assets/tencentVoicesList.json', 'tencent'),
        sherpaConfig ? querySherpaVoices().catch(() => ({ data: [] })) : Promise.resolve({ data: [] })
      ])

      // 3. Combina todas as vozes e as associa à configuração de TTS
      const voices: VoiceOption[] = []

      // Voz do Edge (não precisa de configuração de TTS)
      voices.push(...edgeVoices.map(v => ({
        ...v,
        ttsId: -1
      })))

      // Voz de provedores de nuvem (requer associação à configuração de TTS)
      const providerVoicesMap: Record<string, VoiceOption[]> = {
        aliyun: aliyunVoices,
        'aliyun-nls': aliyunNlsVoices,
        volcengine: volcengineVoices,
        xfyun: xfyunVoices,
        minimax: minimaxVoices,
        tencent: tencentVoices,
      }

      Object.entries(providerVoicesMap).forEach(([provider, providerVoices]) => {
        const ttsConfig = ttsConfigs.value.find(c => c.provider === provider)
        if (ttsConfig) {
          providerVoices.forEach((v: VoiceOption) => {
            voices.push({ ...v, ttsId: ttsConfig.configId })
          })
        }
      })

      // Vozes dinâmicas do sherpa-onnx
      if (sherpaConfig) {
        const items = (sherpaRes as { data?: Record<string, string>[] }).data ?? []
        items.forEach((item: Record<string, string>) => {
          voices.push({
            label: item.label,
            value: item.value,
            gender: (item.gender as 'male' | 'female' | '') || '',
            provider: 'sherpa-onnx',
            model: item.model,
            ttsId: sherpaConfig.configId
          })
        })
      }

      allVoices.value = voices
    } catch (error) {
      console.error('Falha ao carregar lista de vozes:', error)
      message.error('Falha ao carregar lista de vozes')
    } finally {
      voiceLoading.value = false
    }
  }

  /**
   * Carrega um único arquivo JSON de voz
   */
  async function loadVoiceJson(url: string, provider: VoiceProvider): Promise<VoiceOption[]> {
    try {
      const response = await fetch(url)
      if (!response.ok) {
        throw new Error(`Falha ao carregar lista de vozes de ${provider}`)
      }
      const data = await response.json()

      // Processa o formato especial do Edge
      if (provider === 'edge') {
        interface EdgeVoice {
          Locale: string
          ShortName: string
          Gender: string
        }
        return (data as EdgeVoice[])
          .filter((voice) => voice.Locale && voice.Locale.includes('zh'))
          .sort((a, b) => a.Locale.localeCompare(b.Locale))
          .map((voice) => {
            const nameParts = voice.ShortName.split('-')
            let name = nameParts[2] || ''
            if (name.endsWith('Neural')) {
              name = name.substring(0, name.length - 6)
            }
            return {
              label: `${name} (${voice.Locale})`,
              value: voice.ShortName,
              gender: voice.Gender.toLowerCase() as 'male' | 'female' | '',
              provider: 'edge'
            }
          })
      }

      // Outros provedores retornam o label original diretamente, sem adicionar identificação do provedor
      return (data as Omit<VoiceOption, 'provider'>[]).map((voice) => ({
        ...voice,
        provider
      }))
    } catch (error) {
      console.warn(`Falha ao carregar lista de vozes de ${provider}:`, error)
      return []
    }
  }

  /**
   * Carrega as opções de reconhecimento de voz
   */
  async function loadSttOptions() {
    sttLoading.value = true
    try {
      const res = await queryConfigs({ configType: 'stt', pageNo: 1, pageSize: 1000 })
      const options: SttOption[] = [
        {
          label: 'Reconhecimento local Vosk',
          value: -1,
          desc: 'Modelo padrão de reconhecimento de voz local Vosk'
        }
      ]

      if (res.code === 200 && res.data?.list) {
        res.data.list.forEach((config: Config) => {
          options.push({
            label: config.configName,
            value: Number(config.configId),
            desc: config.configDesc
          })
        })
      }

      sttOptions.value = options
    } catch (error) {
      console.error('Falha ao carregar configuração de reconhecimento de voz:', error)
      message.error('Falha ao carregar configuração de reconhecimento de voz')
    } finally {
      sttLoading.value = false
    }
  }

  /**
   * Obtém informações do modelo a partir do ID
   */
  function getModelInfo(modelId?: number) {
    if (!modelId) return null
    return allModels.value.find(m => m.value === modelId)
  }

  /**
   * Obtém informações da voz a partir do nome
   * @param voiceName Nome/ID da voz
   */
  function getVoiceInfo(voiceName?: string) {
    if (!voiceName) return null
    return allVoices.value.find(v => v.value === voiceName) || null
  }

  /**
   * Formata o nome do provedor
   */
  function formatProviderName(provider: string): string {
    const names: Record<string, string> = {
      edge: 'Microsoft Edge',
      aliyun: 'Alibaba Cloud',
      'aliyun-nls': 'Alibaba Cloud NLS',
      volcengine: 'Volcano Engine',
      xfyun: 'iFlytek Cloud',
      minimax: 'Minimax',
      tencent: 'Tencent Cloud',
      'sherpa-onnx': 'Sherpa-ONNX',
      coze: 'Coze',
      dify: 'Dify',
      xingchen: 'XingChen'
    }
    return names[provider] || provider.charAt(0).toUpperCase() + provider.slice(1)
  }

  /**
   * Obtém a cor da tag de voz
   */
  function getVoiceTagColor(provider?: string): string {
    const colors: Record<string, string> = {
      edge: 'green',
      aliyun: 'orange',
      'aliyun-nls': 'orange',
      volcengine: 'blue',
      xfyun: 'cyan',
      minimax: 'red',
      'sherpa-onnx': 'purple'
    }
    return colors[provider || 'edge'] || 'green'
  }

  return {
    // Estado
    modelLoading,
    voiceLoading,
    sttLoading,
    // Dados
    allModels,
    allVoices,
    sttOptions,
    llmConfigs,
    agentConfigs,
    ttsConfigs,
    // Seleção
    selectedModelId,
    selectedVoiceName,
    // Métodos
    loadAllModels,
    loadAllVoices,
    loadSttOptions,
    getModelInfo,
    getVoiceInfo,
    formatProviderName,
    getVoiceTagColor
  }
}
