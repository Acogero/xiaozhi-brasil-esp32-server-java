import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import type { ConfigType, Config, ConfigField, ModelOption, LLMModel, LLMFactory } from '@/types/config'
import { queryConfigs, addConfig, updateConfig, deleteConfig as deleteConfigRequest } from '@/services/config'
import { configTypeMap } from '@/config/providerConfig'
import llmFactoriesData from '@/config/llm_factories.json'
import { useTable } from './useTable'
import { useLoadingStore } from '@/store/loading'

export function useConfigManager(configType: ConfigType) {
  const { t } = useI18n()
  const loadingStore = useLoadingStore()
  
  // Usa o gerenciamento unificado de tabelas
  const {
    loading,
    data: configItems,
    pagination,
    loadData,
  } = useTable<Config>()

  // Estado
  const currentType = ref('')
  const editingConfigId = ref<number>()
  const activeTabKey = ref('1')
  const modelOptions = ref<ModelOption[]>([])
  
  // Dados das fábricas de LLM
  interface LLMFactoryModelInfo {
    chat?: LLMModel[]
    vision?: LLMModel[]
    intent?: LLMModel[]
    embedding?: LLMModel[]
  }
  const llmFactoryData = ref<Record<string, LLMFactoryModelInfo>>({})
  const llmFactoryUrls = ref<Record<string, string>>({})
  const availableProviders = ref<Array<{ value: string; label: string; configNameOptions?: string[] }>>([])

  // Formulário de consulta
  const queryForm = ref({
    provider: '',
    configName: '',
    modelType: '',
  })

  // Informações do tipo de configuração
  const configTypeInfo = computed(() => {
    return configTypeMap[configType] || { label: '' }
  })

  // Opções de tipo
  const typeOptions = computed(() => {
    if (configType === 'llm') {
      return availableProviders.value
    }
    return configTypeInfo.value.typeOptions || []
  })

  // Campos do tipo atual
  const currentTypeFields = computed((): ConfigField[] => {
    if (!currentType.value) return []

    const typeFieldsMap = configTypeInfo.value.typeFields || {}

    if (configType === 'llm') {
      // Se providerConfig estiver explicitamente definido, usa-o
      if (typeFieldsMap[currentType.value]) {
        const fields = [...(typeFieldsMap[currentType.value] || [])]
        // Se não houver o campo apiUrl mas a fábrica tiver URL, adiciona automaticamente
        const factoryUrl = llmFactoryUrls.value[currentType.value]
        if (factoryUrl && !fields.some(f => f.name === 'apiUrl')) {
          fields.push({
            name: 'apiUrl',
            label: 'API URL',
            required: true,
            inputType: 'text',
            placeholder: factoryUrl,
            span: 12,
            suffix: '/chat/completions',
          })
        }
        return fields
      }

      // Não definido explicitamente: gera campos padrão automaticamente a partir dos dados da fábrica
      const factoryUrl = llmFactoryUrls.value[currentType.value] || ''
      return [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: factoryUrl,
          span: 12,
          suffix: '/chat/completions',
        }
      ]
    }

    return typeFieldsMap[currentType.value] || []
  })

  /**
   * Inicializa os dados das fábricas de LLM
   */
  function initLlmFactoriesData() {
    if (!llmFactoriesData || !llmFactoriesData.factory_llm_infos) {
      console.warn('Formato de dados de llm_factories.json inválido')
      return
    }

    const factoryData: Record<string, LLMFactoryModelInfo> = {}
    const providers: Array<{ value: string; label: string }> = []
    const urls: Record<string, string> = {}
    const ranks: Record<string, number> = {}

    llmFactoriesData.factory_llm_infos.forEach((factory: LLMFactory) => {
      const providerName = factory.name
      providers.push({
        value: providerName,
        label: providerName,
      })

      // Armazena a URL da fábrica
      if (factory.url) {
        urls[providerName] = factory.url
      }

      // Armazena o peso de ordenação
      if (factory.rank) {
        ranks[providerName] = parseInt(factory.rank) || 0
      }

      // Armazena os modelos agrupados por tipo
      const modelsByType: LLMFactoryModelInfo = {
        chat: [],
        embedding: [],
        vision: [],
        intent: []
      }

      if (factory.llm && Array.isArray(factory.llm)) {
        factory.llm.forEach((llm: LLMModel) => {
          let mappedModelType = llm.model_type

          // Mapeia o tipo de modelo
          if (mappedModelType === 'speech2text' || mappedModelType === 'image2text') {
            mappedModelType = 'vision'
          }

          // Mantém apenas os tipos de modelo necessários
          if (['chat', 'embedding', 'vision'].includes(mappedModelType as keyof LLMFactoryModelInfo)) {
            (modelsByType[mappedModelType as keyof LLMFactoryModelInfo] as LLMModel[]).push({
              llm_name: llm.llm_name,
              model_type: mappedModelType,
              max_tokens: llm.max_tokens,
              is_tools: llm.is_tools || false,
              tags: llm.tags || '',
            })
          }
        })
      }

      factoryData[providerName] = modelsByType
    })

    llmFactoryData.value = factoryData
    llmFactoryUrls.value = urls

    // Ordena pelo rank da fábrica (decrescente, rank maior aparece primeiro); ranks iguais são ordenados alfabeticamente
    const sortedProviders = providers.sort((a, b) => {
      const rankA = ranks[a.value] || 0
      const rankB = ranks[b.value] || 0
      if (rankA !== rankB) return rankB - rankA
      return a.label.localeCompare(b.label)
    })

    availableProviders.value = sortedProviders
  }

  /**
   * Obtém a lista de modelos com base em provider e modelType
   */
  function getModelsByProviderAndType(provider: string, modelType: string): LLMModel[] {
    if (!llmFactoryData.value[provider]) {
      return []
    }
    const providerData = llmFactoryData.value[provider]
    return (providerData[modelType as keyof LLMFactoryModelInfo] || []) as LLMModel[]
  }

  /**
   * Atualiza a lista de opções de modelo
   */
  function updateModelOptions(provider: string, modelType: string) {
    if (configType !== 'llm') {
      return
    }

    const models = getModelsByProviderAndType(provider, modelType)
    modelOptions.value = models.map((model: LLMModel) => ({
      value: model.llm_name,
      label: model.llm_name,
    }))
  }

  /**
   * Obtém a lista de configurações
   */
  async function fetchData() {
    await loadData(async ({ pageNo, pageSize }) => {
      return queryConfigs({
        pageNo,
        pageSize,
        configType,
        ...queryForm.value,
      })
    })
  }

  /**
   * Exclui a configuração (ação rápida, usa apenas o loading da tabela)
   */
  async function deleteConfig(configId: number) {
    loading.value = true
    try {
      const res = await deleteConfigRequest(configId)

      if (res.code === 200) {
        message.success(t('common.delete'))
        await fetchData()
      } else {
        message.error(res.message)
      }
    } catch (error) {
      console.error('Falha ao excluir a configuração:', error)
      message.error(t('common.serverMaintenance'))
    } finally {
      loading.value = false
    }
  }

  /**
   * Define como configuração padrão (ação rápida, usa apenas o loading da tabela)
   */
  async function setAsDefault(record: Config) {
    if (configType === 'tts') return

    loading.value = true
    try {
      const res = await updateConfig({
        configId: record.configId,
        configType,
        modelType: configType === 'llm' ? record.modelType : undefined,
        isDefault: '1',
      })

      if (res.code === 200) {
        message.success(t('common.setDefaultSuccess', { name: record.configName }))
        await fetchData()
      } else {
        message.error(res.message || t('common.setDefaultFailed'))
      }
    } catch (error) {
      console.error('Falha ao definir a configuração padrão:', error)
      message.error(t('common.serverMaintenance'))
    } finally {
      loading.value = false
    }
  }

  // Inicialização
  if (configType === 'llm') {
    initLlmFactoriesData()
  }

  return {
    // Estado
    loading,
    configItems,
    currentType,
    editingConfigId,
    activeTabKey,
    modelOptions,
    pagination,
    queryForm,
    
    // Propriedades computadas
    configTypeInfo,
    typeOptions,
    currentTypeFields,
    
    // Métodos
    fetchData,
    deleteConfig,
    setAsDefault,
    updateModelOptions,
    getModelsByProviderAndType,
  }
}
