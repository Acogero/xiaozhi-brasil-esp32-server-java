<script setup lang="ts">
import { ref, reactive, nextTick, computed } from 'vue'
import { message, type FormInstance, type UploadProps } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import {
  UserOutlined,
  LoadingOutlined,
  CameraOutlined,
  SnippetsOutlined,
  SoundOutlined,
  PauseCircleOutlined
} from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { useTable } from '@/composables/useTable'
import { useRoleManager } from '@/composables/useRoleManager'
import { useMemoryView } from '@/composables/useMemoryView'
import { useClipboard } from '@/composables/useClipboard'
import { ROUTES } from '@/router/routes'
import { queryRoles, addRole, updateRole, deleteRole, testVoice, getSystemGlobalTools, getDisabledTools, updateToolsStatus } from '@/services/role'
import { queryTemplates } from '@/services/template'
import { getResourceUrl } from '@/utils/resource'
import { useAvatar } from '@/composables/useAvatar'
import { uploadFile } from '@/services/upload'
import type { PromptTemplate, Role, RoleFormData } from '@/types/role'
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue'
import type { McpToolItem } from '@/types/mcpTool'
import TableActionButtons from '@/components/TableActionButtons.vue'

const { t } = useI18n()
const { getAvatarUrl } = useAvatar()
const { copy } = useClipboard()

const router = useRouter()
const { navigateToMemory } = useMemoryView()

// Tabela e paginação
const { loading, data: roleList, pagination, handleTableChange, loadData, createDebouncedSearch } = useTable<Role>()


// Gerenciador de personagens
const {
  modelLoading,
  voiceLoading,
  sttLoading,
  allModels,
  allVoices,
  sttOptions,
  loadAllModels,
  loadAllVoices,
  loadSttOptions,
  getModelInfo,
  formatProviderName,
} = useRoleManager()

// Formulário de busca
const searchForm = reactive({
  roleName: ''
})

// Relacionado às abas
const activeTabKey = ref('1')

// Relacionado ao formulário
const formRef = ref<FormInstance>()
const formData = reactive<RoleFormData>({
  roleName: '',
  roleDesc: '',
  avatar: '',
  isDefault: false,
  modelType: 'llm',
  modelId: undefined,
  temperature: 0.7,
  topP: 0.9,
  sttId: -1,
  vadSpeechTh: 0.5,
  vadSilenceTh: 0.3,
  vadEnergyTh: 0.01,
  vadSilenceMs: 1200,
  voiceName: undefined,
  ttsId: undefined,
  gender: '',
  ttsPitch: 1.0,
  ttsSpeed: 1.0,
  memoryType: 'window'
})

// Estado de edição
const editingRoleId = ref<number>()
const submitLoading = ref(false)

// Upload de avatar
const avatarUrl = ref('')
const avatarLoading = ref(false)

// Estado de reprodução da voz
const playingVoiceId = ref<string>('')
const loadingVoiceId = ref<string>('') // estado de carregamento (durante a requisição à API)
const voiceAudioCache = new Map<string, HTMLAudioElement>()

// Modelo de prompt
const promptEditorMode = ref<'custom' | 'template'>('custom')
const selectedTemplateId = ref<number>()
const promptTemplates = ref<PromptTemplate[]>([])
const templatesLoading = ref(false)

const selectedProvider = ref<string>('')

// Estado de expansão do painel recolhível
const modelAdvancedVisible = ref<string[]>([])
const vadAdvancedVisible = ref<string[]>([])
const ttsAdvancedVisible = ref<string[]>([])

// Valor do painel recolhível pendente de definição
const pendingVadValues = ref<Record<string, number> | null>(null)
const pendingModelValues = ref<Record<string, number> | null>(null)
const pendingTtsValues = ref<Record<string, number> | null>(null)

// Relacionado às ferramentas MCP
const mcpToolsLoading = ref(false)
const allMcpTools = ref<McpToolItem[]>([])
const selectedToolNames = ref<string[]>([])
const globalDisabledTools = ref<string[]>([])

// Definição das colunas da tabela
const columns = computed<TableColumnsType>(() => [
  {
    title: t('common.avatar'),
    dataIndex: 'avatar',
    width: 80,
    align: 'center'
  },
  {
    title: t('role.roleName'),
    dataIndex: 'roleName',
    width: 120,
    align: 'center'
  },
  {
    title: t('role.roleDesc'),
    dataIndex: 'roleDesc',
    width: 200,
    align: 'center'
  },
  {
    title: t('role.voiceName'),
    dataIndex: 'voiceName',
    width: 200,
    align: 'center'
  },
  {
    title: t('role.modelName'),
    dataIndex: 'modelName',
    width: 200,
    align: 'center'
  },
  {
    title: t('role.sttName'),
    dataIndex: 'sttName',
    width: 150,
    align: 'center'
  },
  {
    title: t('role.memoryTypeLabel'),
    dataIndex: 'memoryType',
    width: 120,
    align: 'center'
  },
  {
    title: t('role.totalDevice'),
    dataIndex: 'totalDevice',
    width: 100,
    align: 'center'
  },
  {
    title: t('common.isDefault'),
    dataIndex: 'isDefault',
    width: 100,
    align: 'center'
  },
  {
    title: t('table.action'),
    dataIndex: 'operation',
    width: 250,
    align: 'center',
    fixed: 'right'
  }
])


// Carrega a lista de personagens
const fetchData = async () => {
  await loadData((params) => queryRoles({
    ...params,
    roleName: searchForm.roleName || undefined
  }))
}

// Busca com debounce
const debouncedSearch = createDebouncedSearch(fetchData, 500)

// Trata a mudança de paginação da tabela
const onTableChange = (pag: TablePaginationConfig) => {
  handleTableChange(pag)
  fetchData()
}

// Troca de aba
const handleTabChange = (key: string) => {
  activeTabKey.value = key
  if (key === '1') {
    fetchData()
  } else if (key === '2') {
    resetForm()
    // Ao mudar para a criação de personagem, carrega a lista de ferramentas MCP
    loadAllMcpTools()
  }
}

// Editar personagem
const handleEdit = (record: Role) => {
  editingRoleId.value = record.roleId
  avatarUrl.value = record.avatar || ''
  activeTabKey.value = '2'
  
  // Ao editar, usa o modo personalizado por padrão
  promptEditorMode.value = 'custom'

  nextTick(() => {
    // Obtém informações do modelo
    const modelInfo = getModelInfo(record.modelId || undefined)

    // Obtém informações de voz
    const voiceInfo = allVoices.value.find(v => v.value === (record.voiceName || ''))

    // Limpa os valores pendentes (o mecanismo pendente não é usado durante a edição)
    pendingVadValues.value = null
    pendingModelValues.value = null
    pendingTtsValues.value = null

    // Define todos os valores do formulário (incluindo os das configurações avançadas)
    Object.assign(formData, {
      roleName: record.roleName,
      roleDesc: record.roleDesc || '',
      avatar: record.avatar || '',
      isDefault: record.isDefault === '1',
      modelType: modelInfo?.type || 'llm',
      modelId: record.modelId,
      temperature: record.temperature ?? 0.7,
      topP: record.topP ?? 0.9,
      sttId: record.sttId ?? -1,
      vadSpeechTh: record.vadSpeechTh ?? 0.5,
      vadSilenceTh: record.vadSilenceTh ?? 0.3,
      vadEnergyTh: record.vadEnergyTh ?? 0.01,
      vadSilenceMs: record.vadSilenceMs ?? 1200,
      voiceName: record.voiceName || '',
      ttsId: voiceInfo?.ttsId,
      gender: voiceInfo?.gender || '',
      ttsPitch: record.ttsPitch ?? 1.0,
      ttsSpeed: record.ttsSpeed ?? 1.0,
      memoryType: record.memoryType || 'window'
    })

    // Carrega a lista de ferramentas MCP
    loadAllMcpTools()
  })
}

// Excluir personagem
const handleDelete = async (roleId: number) => {
  loading.value = true
  try {
    const res = await deleteRole(roleId)
    if (res.code === 200) {
      message.success(t('role.deleteRoleSuccess'))
      await fetchData()
    } else {
      message.error(res.message || t('role.deleteRoleFailed'))
    }
  } catch (error) {
    console.error('Falha ao excluir a personagem:', error)
    message.error(t('role.deleteRoleFailed'))
  } finally {
    loading.value = false
  }
}

// Definir como personagem padrão
const handleSetDefault = async (roleId: number) => {
  loading.value = true
  try {
    const res = await updateRole({
      roleId,
      isDefault: '1',
    })
    if (res.code === 200) {
      message.success(t('role.setAsDefaultSuccess'))
      await fetchData()
    } else {
      message.error(res.message || t('role.setAsDefaultFailed'))
    }
  } catch (error) {
    console.error('Falha ao definir a personagem padrão:', error)
    message.error(t('role.setAsDefaultFailed'))
  } finally {
    loading.value = false
  }
}

// Enviar formulário
const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitLoading.value = true

    // Tratamento unificado: busca entre todas as vozes disponíveis
    const voiceInfo = allVoices.value.find(v => v.value === formData.voiceName)
    const ttsId = voiceInfo?.ttsId || -1
    
    const submitData: Partial<RoleFormData> & { avatar?: string } = {
      ...formData,
      avatar: avatarUrl.value || '',
      // Converte o valor booleano isDefault em string '1' ou '0'
      isDefault: formData.isDefault ? '1' : '0',
      ttsId: ttsId,
    }

    if (editingRoleId.value) {
      submitData.roleId = editingRoleId.value
    }

    // 1. Salvar informações da personagem
    const res = editingRoleId.value 
      ? await updateRole(submitData)
      : await addRole(submitData)

    if (res.code === 200) {
      const savedRoleId = editingRoleId.value ?? res.data?.roleId

      // 2. Salvar seleção de ferramentas (usando abordagem de exclusão)
      if (savedRoleId && allMcpTools.value.length > 0) {
        try {
          const excludeTools = allMcpTools.value
            .filter(tool => !selectedToolNames.value.includes(tool.name))
            .map(tool => tool.name)
          
          await updateToolsStatus(savedRoleId, excludeTools)
        } catch (error) {
          console.error('Falha ao salvar a seleção de ferramentas:', error)
          message.warning(t('role.mcpSaveFailed'))
        }
      }
      
      message.success(editingRoleId.value ? t('role.updateRoleSuccess') : t('role.createRoleSuccess'))
      resetForm()
      activeTabKey.value = '1'
      fetchData()
    } else {
      message.error(res.message || t('common.operationFailed'))
    }
  } catch (error: unknown) {
    console.error('Falha ao enviar o formulário:', error)
    if (error && typeof error === 'object' && 'errorFields' in error) {
      message.error(t('role.checkForm'))
    }
  } finally {
    submitLoading.value = false
  }
}

// Cancelar edição
const handleCancel = () => {
  resetForm()
  activeTabKey.value = '1'
}

// Redefinir formulário
const resetForm = () => {
  formRef.value?.resetFields()
  editingRoleId.value = undefined
  avatarUrl.value = ''

  // Limpa o valor pendente do painel recolhível
  pendingVadValues.value = null
  pendingModelValues.value = null
  pendingTtsValues.value = null

  // Para toda a reprodução de áudio
  playingVoiceId.value = ''
  voiceAudioCache.forEach(audio => {
    audio.pause()
    audio.currentTime = 0
  })

  // Ao criar, usa o modo de modelo e aplica o modelo padrão
  promptEditorMode.value = 'template'
  const defaultTemplate = promptTemplates.value.find(t => t.isDefault == 1)
  if (defaultTemplate) {
    selectedTemplateId.value = defaultTemplate.templateId
    formData.roleDesc = defaultTemplate.templateContent
  } else {
    selectedTemplateId.value = undefined
    formData.roleDesc = ''
  }

  // Redefine para os valores padrão
  Object.assign(formData, {
    roleName: '',
    avatar: '',
    isDefault: false,
    modelType: 'llm',
    modelId: undefined,
    temperature: 0.7,
    topP: 0.9,
    sttId: -1,
    vadSpeechTh: 0.5,
    vadSilenceTh: 0.3,
    vadEnergyTh: 0.01,
    vadSilenceMs: 1200,
    voiceName: undefined,
    ttsId: undefined,
    gender: '',
    ttsPitch: 1.0,
    ttsSpeed: 1.0,
    memoryType: 'window'
  })
}

// Mudança do tipo de modelo
const handleModelTypeChange = () => {
  formData.modelId = undefined
  if (formData.modelType === 'agent') {
    formData.roleDesc = ''
  }
}

// Mudança na seleção do modelo
const handleModelChange = (modelId: number | undefined) => {
  if (!modelId) return
  const modelInfo = getModelInfo(modelId)
  if (modelInfo && modelInfo.type === 'agent') {
    formData.roleDesc = modelInfo.agentDesc || ''
  }
}

// Reproduzir exemplo de voz
const handlePlayVoice = async (voiceName?: string) => {
  if (!voiceName) return
  try {
    // Se a mesma voz já estiver sendo reproduzida, para a reprodução
    if (playingVoiceId.value === voiceName) {
      const audio = voiceAudioCache.get(voiceName)
      if (audio) {
        audio.pause()
        audio.currentTime = 0
      }
      playingVoiceId.value = ''
      return
    }

    // Para a reprodução anterior
    if (playingVoiceId.value) {
      const prevAudio = voiceAudioCache.get(playingVoiceId.value)
      if (prevAudio) {
        prevAudio.pause()
        prevAudio.currentTime = 0
      }
    }

    // Define o estado de carregamento (durante a requisição à API)
    loadingVoiceId.value = voiceName

    // Verifica o cache
    let audio = voiceAudioCache.get(voiceName)
    
    if (!audio) {
      // Tratamento unificado: busca entre todas as vozes disponíveis
      const voiceInfo = allVoices.value.find(v => v.value === voiceName)
      if (!voiceInfo) {
        message.error(t('role.voiceNotFound'))
        loadingVoiceId.value = ''
        return
      }

      const testParams = {
        message: t('role.voiceTestMessage'),
        voiceName: voiceName,
        ttsId: voiceInfo.ttsId || -1,
        provider: voiceInfo.provider,
        ttsPitch: formData.ttsPitch || 1.0,
        ttsSpeed: formData.ttsSpeed || 1.0
      }

      // Chama a API de teste para obter a URL do áudio
      const result: any = await testVoice(testParams)
      
      // Limpa o estado de carregamento
      loadingVoiceId.value = ''

      if (result.code === 200 && result.data) {
        // Usa getResourceUrl para tratar o caminho do áudio
        const audioUrl = getResourceUrl(result.data)
        if (audioUrl) {
          // Cria o objeto de áudio
          audio = new Audio(audioUrl)
          voiceAudioCache.set(voiceName, audio)
        } else {
          message.error(t('common.audioUrlInvalid'))
          return
        }
        
        // Escuta o término da reprodução
        audio.onended = () => {
          if (playingVoiceId.value === voiceName) {
            playingVoiceId.value = ''
          }
        }
        
        // Escuta erros
        audio.onerror = () => {
          message.error(t('common.audioPlayFailed'))
          playingVoiceId.value = ''
          voiceAudioCache.delete(voiceName)
        }
      } else {
        message.error(t('role.getTestAudioFailed'))
        return
      }
    } else {
      // Limpa o estado de carregamento
      loadingVoiceId.value = ''
    }

    // Reproduz o áudio
    if (audio) {
      await audio.play()
      // Define o estado playing após o início da reprodução
      playingVoiceId.value = voiceName
    }
  } catch (error: unknown) {
    console.error('Falha ao reproduzir a voz:', error)
    const errorMessage = error instanceof Error ? error.message : t('role.playVoiceFailed')
    message.error(errorMessage)
    loadingVoiceId.value = ''
    playingVoiceId.value = ''
  }
}

// Mudança no modo de prompt
const handlePromptModeChange = () => {
  if (promptEditorMode.value === 'template') {
    // Ao mudar para o modo de modelo, se nenhum modelo estiver selecionado, seleciona o modelo padrão
    if (!selectedTemplateId.value) {
      const defaultTemplate = promptTemplates.value.find(t => t.isDefault == 1)
      if (defaultTemplate) {
        selectedTemplateId.value = defaultTemplate.templateId
        formData.roleDesc = defaultTemplate.templateContent
      }
    } else {
      // Se um modelo já estiver selecionado, aplica esse modelo
      const template = promptTemplates.value.find(t => t.templateId === selectedTemplateId.value)
      if (template) {
        formData.roleDesc = template.templateContent
      }
    }
  }
}

// Mudança na seleção do modelo
const handleTemplateChange = (templateId: number) => {
  const template = promptTemplates.value.find(t => t.templateId === templateId)
  if (template) {
    formData.roleDesc = template.templateContent
  }
}

// Ir para o gerenciamento de modelos
const goToTemplateManager = () => {
  router.push(ROUTES.TEMPLATE)
}

// Trata a mudança do painel recolhível de VAD
const handleVadCollapseChange = (activeKeys: string | string[]) => {
  const keys = Array.isArray(activeKeys) ? activeKeys : [activeKeys]
  if (keys.includes('vad') && pendingVadValues.value) {
    nextTick(() => {
      Object.assign(formData, pendingVadValues.value)
      pendingVadValues.value = null
    })
  }
}

// Trata a mudança do painel recolhível de modelo
const handleModelCollapseChange = (activeKeys: string | string[]) => {
  const keys = Array.isArray(activeKeys) ? activeKeys : [activeKeys]
  if (keys.includes('advanced') && pendingModelValues.value) {
    nextTick(() => {
      Object.assign(formData, pendingModelValues.value)
      pendingModelValues.value = null
    })
  }
}

// Trata a mudança do painel recolhível de TTS
const handleTtsCollapseChange = (activeKeys: string | string[]) => {
  const keys = Array.isArray(activeKeys) ? activeKeys : [activeKeys]
  if (keys.includes('tts') && pendingTtsValues.value) {
    nextTick(() => {
      Object.assign(formData, pendingTtsValues.value)
      pendingTtsValues.value = null
    })
  }
}

// Carrega todas as ferramentas MCP
const loadAllMcpTools = async () => {
  try {
    mcpToolsLoading.value = true
    
    const isEditMode = !!editingRoleId.value
    
    const [systemRes, disabledRes] = await Promise.all([
      getSystemGlobalTools(),
      getDisabledTools(isEditMode ? editingRoleId.value! : 0)
    ])

    const tools: McpToolItem[] = []

    // Trata as ferramentas globais do sistema
    if (systemRes.code === 200 && systemRes.data && Array.isArray(systemRes.data)) {
      systemRes.data.forEach((tool: { name: string; description: string }) => {
        tools.push({
          name: tool.name,
          description: tool.description || '',
          inputSchema: '',
          inputSchemaData: [],
          enabled: true,
          source: 'system'
        })
      })
    }

    allMcpTools.value = tools

    // Trata o estado desabilitado
    if (disabledRes.code === 200 && disabledRes.data) {
      const data = disabledRes.data as { globalDisabled?: string[]; roleDisabled?: string[] }
      globalDisabledTools.value = data.globalDisabled || []
      const roleDisabled = isEditMode ? (data.roleDisabled || []) : []
      
      selectedToolNames.value = tools
        .filter(tool => !roleDisabled.includes(tool.name) && !globalDisabledTools.value.includes(tool.name))
        .map(tool => tool.name)
    } else {
      selectedToolNames.value = tools.map(tool => tool.name)
    }

  } catch (error) {
    console.error('Falha ao carregar as ferramentas MCP:', error)
    message.error(t('role.mcpLoadToolsFailed'))
  } finally {
    mcpToolsLoading.value = false
  }
}

// Obtém todas as ferramentas disponíveis
const availableTools = computed(() => {
  return allMcpTools.value.filter(tool => !globalDisabledTools.value.includes(tool.name))
})

// Filtro de busca de ferramentas
const filterToolOption = (input: string, option: any) => {
  const toolName = option.value || ''
  return toolName.toLowerCase().includes(input.toLowerCase())
}

// Formata a exibição do nome da ferramenta (remove o prefixo)
const formatToolName = (toolName: string): string => {
  return toolName
    .replace(/^func_/, '')  // remove o prefixo "func_"
    .replace(/^XiaoZhi_MCP_Client_/, '')  // remove o prefixo "XiaoZhi_MCP_Client_"
}

// Verificação antes do upload do avatar
const beforeAvatarUpload: UploadProps['beforeUpload'] = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    message.error(t('common.onlyImageFiles'))
    return false
  }
  if (!isLt2M) {
    message.error(t('common.imageSizeLimit'))
    return false
  }

  avatarLoading.value = true
  uploadAvatarFile(file)
    .then(url => {
      avatarUrl.value = url
      avatarLoading.value = false
    })
    .catch(error => {
      message.error(t('common.avatarUploadFailed') + error)
      avatarLoading.value = false
    })

  return false
}

// Upload do arquivo de avatar
const uploadAvatarFile = async (file: File): Promise<string> => {
  return await uploadFile(file, 'avatar') as string
}

// Remover avatar
const removeAvatar = () => {
  avatarUrl.value = ''
}

// Obtém a URL do avatar
const getAvatar = (avatar?: string) => {
  return getAvatarUrl(avatar)
}


// Obtém o nome de exibição da voz
const getVoiceDisplayName = (record: any) => {
  if (!record.voiceName) return ''

  // Tratamento unificado: busca entre todas as vozes disponíveis
  const voiceInfo = allVoices.value.find(v => v.value === record.voiceName)
  return voiceInfo?.label || record.voiceName
}

// Obtém as informações de exibição do tipo de memória
const getMemoryTypeInfo = (memoryType?: string) => {
  switch (memoryType) {
    case 'window':
      return { label: t('device.windowMemory'), color: 'orange' }
    case 'summary':
      return { label: t('device.summaryMemory'), color: 'blue' }
    default:
      return { label: t('device.windowMemory'), color: 'orange' }
  }
}

// Carrega os modelos de prompt
const loadTemplates = async () => {
  try {
    templatesLoading.value = true
    const res = await queryTemplates({})
    if (res.code === 200 && res.data) {
      promptTemplates.value = (res.data.list || []) as PromptTemplate[]
    }
  } catch (error) {
    console.error('Falha ao carregar a lista de modelos:', error)
    message.error(t('role.loadTemplateFailed'))
  } finally {
    templatesLoading.value = false
  }
}

// Opções de provedor
const providerOptions = computed(() => {
  const providers = new Set<string>()
  allVoices.value.forEach(v => {
    if (v.provider) providers.add(v.provider)
  })
  const items = Array.from(providers).map(p => ({ label: formatProviderName(p), value: p }))
  // prepend All option (empty value means all)
  return [{ label: t('common.all'), value: '' }, ...items]
})

// Filtra as opções de voz de acordo com o provedor
const filteredVoices = computed(() => {
  const list = allVoices.value
  return selectedProvider.value ? list.filter(v => v.provider === selectedProvider.value) : list
})

// Troca de provedor: limpa a voz selecionada após a troca
const handleProviderChange = () => {
  formData.voiceName = undefined
}

// Inicialização: carrega todos os dados em paralelo (modo não bloqueante)
Promise.all([
  loadAllModels(),
  loadAllVoices(),
  loadSttOptions(),
  loadTemplates(),
  fetchData()
])

// Após carregar os modelos, aplica o modelo padrão (ao criar)
if (!editingRoleId.value) {
  const defaultTemplate = promptTemplates.value.find(t => t.isDefault == 1)
  if (defaultTemplate) {
    selectedTemplateId.value = defaultTemplate.templateId
    formData.roleDesc = defaultTemplate.templateContent
  }
}

</script>

<template>
  <div class="role-view">
    <!-- Formulário de busca -->
    <a-card :bordered="false" style="margin-bottom: 16px" class="search-card">
      <a-form layout="horizontal" :colon="false">
        <a-row :gutter="16">
          <a-col :xl="8" :lg="12" :xs="24">
            <a-form-item :label="t('role.roleName')">
              <a-input
                v-model:value="searchForm.roleName"
                :placeholder="t('role.enterRoleName')"
                allow-clear
                @input="debouncedSearch"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- Conteúdo principal -->
    <a-card :bordered="false" :body-style="{ padding: '0 24px 24px 24px' }">
      <a-tabs
        v-model:active-key="activeTabKey"
        @change="handleTabChange"
      >
        <!-- Lista de personagens -->
        <a-tab-pane key="1" :tab="t('role.roleList')">
          <a-table
            row-key="roleId"
            :columns="columns"
            :data-source="roleList"
            :loading="loading"
            :pagination="pagination"
            :scroll="{ x: 1000 }"
            size="middle"
            @change="onTableChange"
          >
            <!-- Avatar -->
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'avatar'">
                <a-avatar :src="getAvatar(record.avatar)" icon="user" :size="40" />
              </template>

              <!-- Nome da personagem -->
              <template v-else-if="column.dataIndex === 'roleName'">
                <a-tooltip :title="record.roleName" placement="top">
                  <span class="ellipsis-text">{{ record.roleName }}</span>
                </a-tooltip>
              </template>

              <!-- Descrição da personagem -->
              <template v-else-if="column.dataIndex === 'roleDesc'">
                <a-tooltip :title="record.roleDesc" :mouse-enter-delay="0.5" placement="topLeft">
                  <span v-if="record.roleDesc" class="ellipsis-text">{{ record.roleDesc }}</span>
                  <span v-else>-</span>
                </a-tooltip>
              </template>

              <!-- Voz -->
              <template v-else-if="column.dataIndex === 'voiceName'">
                <a-tooltip 
                  :title="getVoiceDisplayName(record)"
                  placement="top"
                >
                  <span v-if="record.voiceName" class="ellipsis-text">
                    {{ getVoiceDisplayName(record) }}
                  </span>
                  <span v-else>-</span>
                </a-tooltip>
              </template>

              <!-- Modelo -->
              <template v-else-if="column.dataIndex === 'modelName'">
                <a-tooltip 
                  :title="getModelInfo(record.modelId)?.desc || (getModelInfo(record.modelId)?.label || record.modelName || t('role.unknownModel'))"
                  :mouse-enter-delay="0.5"
                  placement="top"
                >
                  <span v-if="record.modelId" class="ellipsis-text">
                    {{ getModelInfo(record.modelId)?.label || record.modelName || t('role.unknownModel') }}
                  </span>
                  <span v-else>-</span>
                </a-tooltip>
              </template>

              <!-- Reconhecimento de voz -->
              <template v-else-if="column.dataIndex === 'sttName'">
                <a-tooltip
                  :title="record.sttId === -1 || record.sttId === null ? t('role.voskLocalRecognition') : (sttOptions.find(s => s.value === record.sttId)?.label || t('common.unknown'))"
                  placement="top"
                >
                  <span v-if="record.sttId === -1 || record.sttId === null" class="ellipsis-text">
                    {{ t('role.voskLocalRecognition') }}
                  </span>
                  <span v-else class="ellipsis-text">
                    {{ sttOptions.find(s => s.value === record.sttId)?.label || t('common.unknown') }}
                  </span>
                </a-tooltip>
              </template>

              <!-- Tipo de memória -->
              <template v-else-if="column.dataIndex === 'memoryType'">
                <a-tag :color="getMemoryTypeInfo(record.memoryType).color">
                  {{ getMemoryTypeInfo(record.memoryType).label }}
                </a-tag>
              </template>

              <!-- Estado padrão -->
              <template v-else-if="column.dataIndex === 'isDefault'">
                <a-tag v-if="record.isDefault == 1" color="green">{{ t('common.default') }}</a-tag>
                <span v-else>-</span>
              </template>

              <!-- Ações -->
              <template v-else-if="column.dataIndex === 'operation'">
                <TableActionButtons
                  :record="record"
                  permission-prefix="system:role"
                  show-edit
                  show-set-default
                  show-delete
                  :is-default="record.isDefault == 1"
                  :delete-title="t('role.confirmDeleteRole')"
                  @edit="() => handleEdit(record)"
                  @set-default="() => handleSetDefault(record.roleId)"
                  @delete="() => handleDelete(record.roleId)"
                >
                  <template #actions>
                    <a
                      v-permission="'system:role:memory'"
                      @click="() => navigateToMemory({ roleId: record.roleId })"
                    >
                      {{ t('role.memory') }}
                    </a>
                  </template>
                </TableActionButtons>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- Criar/Editar personagem -->
        <a-tab-pane
          key="2"
          :tab="editingRoleId ? t('role.updateRole') : t('role.createRole')"
          v-permission="editingRoleId ? 'system:role:update' : 'system:role:create'"
        >
          <a-form
            ref="formRef"
            :model="formData"
            layout="horizontal"
            :colon="false"
            @finish="handleSubmit"
            :hideRequiredMark="true"
          >
            <!-- Informações básicas -->
            <a-row :gutter="20">
              <a-col :xl="8" :lg="12" :xs="24">
                <a-form-item :label="t('common.avatar')">
                  <div class="avatar-uploader-wrapper">
                    <a-upload
                      name="file"
                      :show-upload-list="false"
                      :before-upload="beforeAvatarUpload"
                      accept=".jpg,.jpeg,.png,.gif"
                      class="avatar-uploader"
                    >
                      <div class="avatar-content">
                        <a-avatar
                          v-if="avatarUrl"
                          :size="128"
                          :src="getAvatar(avatarUrl)"
                          icon="user"
                        />
                        <div v-else class="avatar-placeholder">
                          <user-outlined />
                          <p>{{ t('common.clickToUpload') }}</p>
                        </div>

                        <div class="avatar-hover-mask">
                          <loading-outlined v-if="avatarLoading" />
                          <camera-outlined v-else />
                          <p>{{ avatarUrl ? t('common.changeAvatar') : t('common.uploadAvatar') }}</p>
                        </div>
                      </div>
                    </a-upload>

                    <a-button
                      v-if="avatarUrl"
                      type="primary"
                      danger
                      size="small"
                      @click.stop="removeAvatar"
                      class="avatar-remove-btn"
                    >
                      <delete-outlined /> {{ t('common.removeAvatar') }}
                    </a-button>

                    <div class="avatar-tip">
                      {{ t('common.avatarTip') }}
                    </div>
                  </div>
                </a-form-item>
              </a-col>
              <a-col :span="24"></a-col>

              <a-col :xl="8" :lg="12" :xs="24">
                <a-form-item
                  :label="t('role.roleName')"
                  name="roleName"
                  :rules="[{ required: true, message: t('role.enterRoleName') }]"
                >
                  <a-input
                    v-model:value="formData.roleName"
                    :placeholder="t('role.enterRoleName')"
                  />
                </a-form-item>
              </a-col>

              <a-col :span="24">
                <a-form-item v-permission="'system:role:update'" :label="t('role.setAsDefaultRole')">
                  <a-switch v-model:checked="formData.isDefault" />
                  <span style="margin-left: 8px; color: var(--ant-color-text-tertiary)">
                    {{ t('role.defaultRoleTip') }}
                  </span>
                </a-form-item>
              </a-col>
            </a-row>

            <!-- Configurações do modelo de conversa -->
            <a-divider orientation="left">{{ t('role.conversationModelSettings') }}</a-divider>

            <a-row :gutter="20">
              <a-col :span="24">
                <a-form-item :label="t('role.modelType')" name="modelType">
                  <a-radio-group
                    v-model:value="formData.modelType"
                    button-style="solid"
                    @change="handleModelTypeChange"
                  >
                    <a-radio-button value="llm">{{ t('role.llmModel') }}</a-radio-button>
                    <a-radio-button value="agent">{{ t('role.agent') }}</a-radio-button>
                  </a-radio-group>
                </a-form-item>
              </a-col>

              <a-col :xl="8" :lg="12" :xs="24">
                <a-form-item
                  :label="t('role.model')"
                  name="modelId"
                  :rules="[{ required: true, message: t('role.selectModel') }]"
                >
                  <a-select
                    v-model:value="formData.modelId"
                    :placeholder="t('role.selectModel')"
                    :loading="modelLoading"
                    show-search
                    :filter-option="(input: string, option: { label: string; value: number }) => 
                      option.label.toLowerCase().includes(input.toLowerCase())
                    "
                    @change="(value: number) => handleModelChange(value)"
                  >
                    <a-select-option
                      v-for="model in allModels.filter(m => m.type === formData.modelType)"
                      :key="model.value"
                      :value="model.value"
                      :label="model.label"
                    >
                      {{ model.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>

            <!-- Configurações avançadas do modelo de conversa -->
            <a-collapse
              v-model:active-key="modelAdvancedVisible"
              :bordered="false"
              style="background: transparent; margin-bottom: 24px"
              @change="handleModelCollapseChange"
            >
              <a-collapse-panel :header="t('role.conversationModelAdvanced')" key="advanced">
                <a-row :gutter="16">
                  <a-col :xl="8" :lg="12" :xs="24">
                    <a-form-item
                      :label="t('role.temperature')"
                      name="temperature"
                      :label-col="{ span: 10 }"
                      :wrapper-col="{ span: 14 }"
                    >
                      <a-tooltip placement="top">
                        <template #title>
                          <div v-html="t('role.temperatureTip').replace(/\n/g, '<br>')"></div>
                        </template>
                        <a-input-number
                          v-model:value="formData.temperature"
                          :min="0"
                          :max="2"
                          :step="0.1"
                          style="width: 100%"
                        />
                      </a-tooltip>
                    </a-form-item>
                  </a-col>

                  <a-col :xl="8" :lg="12" :xs="24">
                    <a-form-item
                      :label="t('role.topP')"
                      name="topP"
                      :label-col="{ span: 10 }"
                      :wrapper-col="{ span: 14 }"
                    >
                      <a-tooltip placement="top">
                        <template #title>
                          <div v-html="t('role.topPTip').replace(/\n/g, '<br>')"></div>
                        </template>
                        <a-input-number
                          v-model:value="formData.topP"
                          :min="0"
                          :max="1"
                          :step="0.05"
                          style="width: 100%"
                        />
                      </a-tooltip>
                    </a-form-item>
                  </a-col>
                </a-row>
              </a-collapse-panel>
            </a-collapse>

            <!-- Configurações de reconhecimento de voz -->
            <a-divider orientation="left">{{ t('role.speechRecognitionSettings') }}</a-divider>

            <a-row :gutter="20">
              <a-col :xl="8" :lg="12" :xs="24">
                <a-form-item
                  :label="t('role.speechRecognition')"
                  name="sttId"
                  :rules="[{ required: true, message: t('role.selectSpeechRecognition') }]"
                >
                  <a-select
                    v-model:value="formData.sttId"
                    :placeholder="t('role.selectSpeechRecognition')"
                    :loading="sttLoading"
                  >
                    <a-select-option
                      v-for="stt in sttOptions"
                      :key="stt.value"
                      :value="stt.value"
                    >
                      {{ stt.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>

            <!-- Configurações avançadas de VAD -->
            <a-collapse
              v-model:active-key="vadAdvancedVisible"
              :bordered="false"
              style="background: transparent; margin-bottom: 24px"
              @change="handleVadCollapseChange"
            >
              <a-collapse-panel :header="t('role.speechRecognitionAdvanced')" key="vad">
                <a-row :gutter="16">
                  <a-col :xl="6" :lg="12" :xs="24">
                    <a-form-item
                      :label="t('role.speechThreshold')"
                      name="vadSpeechTh"
                      :label-col="{ span: 10 }"
                      :wrapper-col="{ span: 14 }"
                    >
                      <a-input-number
                        v-model:value="formData.vadSpeechTh"
                        :min="0"
                        :max="1"
                        :step="0.1"
                        style="width: 100%"
                      />
                    </a-form-item>
                  </a-col>

                  <a-col :xl="6" :lg="12" :xs="24">
                    <a-form-item
                      :label="t('role.silenceThreshold')"
                      name="vadSilenceTh"
                      :label-col="{ span: 10 }"
                      :wrapper-col="{ span: 14 }"
                    >
                      <a-input-number
                        v-model:value="formData.vadSilenceTh"
                        :min="0"
                        :max="1"
                        :step="0.1"
                        style="width: 100%"
                      />
                    </a-form-item>
                  </a-col>

                  <a-col :xl="6" :lg="12" :xs="24">
                    <a-form-item
                      :label="t('role.energyThreshold')"
                      name="vadEnergyTh"
                      :label-col="{ span: 10 }"
                      :wrapper-col="{ span: 14 }"
                    >
                      <a-input-number
                        v-model:value="formData.vadEnergyTh"
                        :min="0"
                        :max="1"
                        :step="0.01"
                        style="width: 100%"
                      />
                    </a-form-item>
                  </a-col>

                  <a-col :xl="6" :lg="12" :xs="24">
                    <a-form-item
                      :label="t('role.silenceDuration')"
                      name="vadSilenceMs"
                      :label-col="{ span: 10 }"
                      :wrapper-col="{ span: 14 }"
                    >
                      <a-input-number
                        v-model:value="formData.vadSilenceMs"
                        :min="0"
                        :max="5000"
                        :step="100"
                        style="width: 100%"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>
              </a-collapse-panel>
            </a-collapse>

            <!-- Configurações de síntese de voz -->
            <a-divider orientation="left">{{ t('role.voiceSynthesisSettings') }}</a-divider>

            <!-- Seleção de voz -->
            <a-row :gutter="20">
              <a-col :xl="6" :lg="8" :xs="24">
                <a-form-item>
                  <a-select
                    v-model:value="selectedProvider"
                    :placeholder="t('role.selectProvider')"
                    :loading="voiceLoading"
                    @change="handleProviderChange"
                  >
                    <a-select-option
                      v-for="p in providerOptions"
                      :key="p.value"
                      :value="p.value"
                      :label="p.label"
                    >
                      {{ p.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :xl="8" :lg="12" :xs="24">
                <a-form-item
                  :label="t('role.voiceName')"
                  name="voiceName"
                  :rules="[{ required: true, message: t('role.selectVoice') }]"
                >
                  <a-select
                    v-model:value="formData.voiceName"
                    :placeholder="t('role.selectVoice')"
                    :loading="voiceLoading"
                    show-search
                    :filter-option="(input: string, option: { label: string; value: string }) => 
                      option.label.toLowerCase().includes(input.toLowerCase())
                    "
                  >
                    <a-select-option
                      v-for="voice in filteredVoices"
                      :key="voice.value"
                      :value="voice.value"
                      :label="voice.label"
                    >
                      <div style="display: flex; align-items: center; justify-content: space-between;">
                        <a-tag color="blue" v-if="voice.model">{{ voice.model }}</a-tag>
                        <span>{{ voice.label }}</span>
                        <a-button
                          v-permission="'system:role'"
                          type="text"
                          size="small"
                          :loading="loadingVoiceId === voice.value"
                          @click.stop="handlePlayVoice(voice.value)"
                          style="margin-left: 8px; padding: 0 4px;"
                        >
                          <template #icon>
                            <LoadingOutlined v-if="loadingVoiceId === voice.value" />
                            <PauseCircleOutlined v-else-if="playingVoiceId === voice.value" />
                            <SoundOutlined v-else />
                          </template>
                        </a-button>
                      </div>
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>

            <!-- Configurações avançadas de síntese de voz -->
            <a-collapse
              v-model:active-key="ttsAdvancedVisible"
              :bordered="false"
              style="background: transparent; margin-bottom: 24px"
              @change="handleTtsCollapseChange"
            >
              <a-collapse-panel :header="t('role.voiceSynthesisAdvanced')" key="tts">
                <a-row :gutter="16">
                  <a-col :xl="8" :lg="12" :xs="24">
                    <a-form-item
                      :label="t('role.voiceSpeed')"
                      name="ttsSpeed"
                      :label-col="{ span: 10 }"
                      :wrapper-col="{ span: 14 }"
                    >
                      <a-tooltip placement="top">
                        <template #title>
                          {{ t('role.voiceSpeedTip') }}
                        </template>
                        <a-slider
                          v-model:value="formData.ttsSpeed"
                          :min="0.5"
                          :max="2.0"
                          :step="0.1"
                          :marks="{ 0.5: '0.5', 1.0: '1.0', 2.0: '2.0' }"
                        />
                      </a-tooltip>
                    </a-form-item>
                  </a-col>

                  <a-col :xl="8" :lg="12" :xs="24">
                    <a-form-item
                      :label="t('role.voicePitch')"
                      name="ttsPitch"
                      :label-col="{ span: 10 }"
                      :wrapper-col="{ span: 14 }"
                    >
                      <a-tooltip placement="top">
                        <template #title>
                          {{ t('role.voicePitchTip') }}
                        </template>
                        <a-slider
                          v-model:value="formData.ttsPitch"
                          :min="0.5"
                          :max="2.0"
                          :step="0.1"
                          :marks="{ 0.5: '0.5', 1.0: '1.0', 2.0: '2.0' }"
                        />
                      </a-tooltip>
                    </a-form-item>
                  </a-col>
                </a-row>
              </a-collapse-panel>
            </a-collapse>

            <div v-permission="'system:role:mcp-tools'">
              <!-- Configurações de ferramentas MCP -->
              <a-divider orientation="left">{{ t('role.mcpTools') }}</a-divider>
              <a-row :gutter="20">
                <a-col :xl="8" :lg="12" :xs="24">
                  <a-form-item :label="t('role.mcpTools')">
                    <a-select
                      v-model:value="selectedToolNames"
                      mode="multiple"
                      :placeholder="t('role.mcpSelectTools')"
                      :loading="mcpToolsLoading"
                      :max-tag-count="10"
                      :maxTagTextLength="10"
                      show-search
                      :filter-option="filterToolOption"
                      allow-clear
                    >
                      <a-select-option
                        v-for="tool in availableTools"
                        :key="tool.name"
                        :value="tool.name"
                        :label="formatToolName(tool.name)"
                      >
                        <a-tooltip :title="tool.description" placement="right">
                          <span>{{ formatToolName(tool.name) }}</span>
                        </a-tooltip>
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
              </a-row>
            </div>
            <!-- Configuração do tipo de memória -->
            <a-divider orientation="left">{{ t('role.memoryTypeSettings') }}</a-divider>

            <a-row :gutter="20">
              <a-col :xl="8" :lg="12" :xs="24">
                <a-form-item :label="t('role.memoryTypeLabel')">
                  <a-select
                    v-model:value="formData.memoryType"
                    :placeholder="t('role.selectMemoryType')"
                  >
                    <a-select-option value="window">
                      {{ t('device.windowMemory') }}
                    </a-select-option>
                    <a-select-option value="summary">
                      {{ t('device.summaryMemory') }}
                    </a-select-option>
                  </a-select>
                  <div style="margin-top: 8px; color: var(--ant-color-text-tertiary); font-size: 12px">
                    {{ t('role.memoryTypeTip') }}
                  </div>
                </a-form-item>
              </a-col>
            </a-row>

            <!-- Prompt da personagem -->
            <a-divider orientation="left">{{ t('role.rolePrompt') }}</a-divider>

            <!-- Dica do agente -->
            <a-alert
              v-if="formData.modelType === 'agent'"
              :message="t('role.agentPrompt')"
              :description="t('role.agentPromptDesc')"
              type="info"
              show-icon
              style="margin-bottom: 16px"
            />

            <!-- Edição do prompt -->
            <template v-else>
              <div style="margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center">
                <a-space>
                  <a-radio-group
                    v-model:value="promptEditorMode"
                    button-style="solid"
                    @change="handlePromptModeChange"
                  >
                    <a-radio-button value="template">{{ t('role.useTemplate') }}</a-radio-button>
                    <a-radio-button value="custom">{{ t('role.custom') }}</a-radio-button>
                  </a-radio-group>

                  <template v-if="promptEditorMode === 'template'">
                    <a-select
                      v-model:value="selectedTemplateId"
                      style="width: 200px"
                      :placeholder="t('role.selectTemplate')"
                      :loading="templatesLoading"
                      @change="handleTemplateChange"
                    >
                      <a-select-option
                        v-for="template in promptTemplates"
                        :key="template.templateId"
                        :value="template.templateId"
                      >
                        {{ template.templateName }}
                        <a-tag v-if="template.isDefault == 1" color="green" size="small">
                          {{ t('common.default') }}
                        </a-tag>
                      </a-select-option>
                    </a-select>
                  </template>
                </a-space>

                <a-button type="primary" @click="goToTemplateManager">
                  <snippets-outlined /> {{ t('role.templateManagement') }}
                </a-button>
              </div>
            </template>

            <!-- Entrada do prompt -->
            <a-form-item name="roleDesc">
              <a-textarea
                v-model:value="formData.roleDesc"
                :disabled="formData.modelType === 'agent'"
                :rows="10"
                :placeholder="t('role.enterRolePrompt')"
              />
            </a-form-item>

            <!-- Botões de ação do formulário -->
            <a-form-item>
              <a-button
                v-permission="editingRoleId ? 'system:role:update' : 'system:role:create'"
                type="primary"
                html-type="submit"
                :loading="submitLoading"
              >
                {{ editingRoleId ? t('role.updateRole') : t('role.createRole') }}
              </a-button>
              <a-button style="margin-left: 8px" @click="handleCancel">
                {{ t('role.cancel') }}
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- Voltar ao topo -->
    <a-back-top />
  </div>
</template>

<style scoped lang="scss">
.role-view {
  padding: 24px;
}

.search-card :deep(.ant-form-item) {
  margin-bottom: 0;
}

// Estilo de upload do avatar
.avatar-uploader-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-uploader {
  cursor: pointer;
}

.avatar-content {
  position: relative;
  width: 128px;
  height: 128px;
  border-radius: 64px;
  background-color: var(--ant-color-fill-quaternary);
  border: 1px dashed var(--ant-color-border);
  overflow: hidden;
  transition: all 0.3s;
}

.avatar-content:hover {
  border-color: var(--ant-color-primary);
}

.avatar-placeholder {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: var(--ant-color-text-tertiary);

  .anticon {
    font-size: 32px;
    margin-bottom: 8px;
  }

  p {
    margin: 0;
  }
}

.avatar-hover-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: white;
  opacity: 0;
  transition: opacity 0.3s;

  .anticon {
    font-size: 24px;
    margin-bottom: 8px;
  }

  p {
    margin: 0;
  }
}

.avatar-content:hover .avatar-hover-mask {
  opacity: 1;
}

.avatar-remove-btn {
  margin-top: 8px;
}

.avatar-tip {
  margin-top: 8px;
  color: var(--ant-color-text-tertiary);
  font-size: 12px;
}

// Estilo do painel recolhível
:deep(.ant-collapse) {
  background: transparent;
}

:deep(.ant-collapse-borderless > .ant-collapse-item) {
  border-bottom: 1px dashed var(--ant-color-border);
}

:deep(.ant-collapse-borderless > .ant-collapse-item:last-child) {
  border-bottom: none;
}

// Cor do título do painel recolhível (adaptado ao modo escuro)
:deep(.ant-collapse-header) {
  color: var(--ant-color-text) !important;
}

// Usa variáveis do Ant Design, sem necessidade de tratamento especial

// Estilo de truncamento de texto da tabela
.ellipsis-text {
  display: inline-block;
  width: 100%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

// Estilo das células da tabela
:deep(.ant-table) {
  .ant-table-tbody > tr > td {
    max-width: 0;
  }
}

:deep(.ant-select-selection-item-content) {
  max-width: 100px; // Define a largura máxima
}

</style>
