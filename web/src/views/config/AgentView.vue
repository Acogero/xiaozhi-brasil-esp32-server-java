<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import type { TableColumnsType, FormInstance, TablePaginationConfig } from 'ant-design-vue'
import { SettingOutlined } from '@ant-design/icons-vue'
import { useTable } from '@/composables/useTable'
import { useModal } from '@/composables/useModal'
import { useConfigManager } from '@/composables/useConfigManager'
import { useLoadingStore } from '@/store/loading'
import TableActionButtons from '@/components/TableActionButtons.vue'
import { updatePlatformConfig, queryPlatformConfig, addPlatformConfig } from '@/services/config'
import type { Agent, PlatformConfig, ProviderOption, PlatformFormItems } from '@/types/agent'
import { queryAgents } from '@/services/agent'

const { t } = useI18n()
const loadingStore = useLoadingStore()

// ==================== Gerenciamento de configuração ====================
const { setAsDefault } = useConfigManager('llm')

// ==================== Formulário de busca ====================
const searchForm = ref({
  provider: 'coze' as string,
  agentName: ''
})

// Opções de plataforma
const providerOptions = computed<ProviderOption[]>(() => [
  { label: t('agent.coze'), value: 'coze' },
  { label: t('agent.dify'), value: 'dify' },
  { label: t('agent.xingchen'), value: 'xingchen' }
])

// ==================== Tabela ====================
const { loading, data: agentList, pagination, handleTableChange, loadData, createDebouncedSearch } = useTable<Agent>()

// Colunas básicas da tabela
const baseColumns = computed<TableColumnsType>(() => [
  {
    title: t('common.avatar'),
    dataIndex: 'iconUrl',
    width: 80,
    align: 'center',
    fixed: 'left'
  },
  {
    title: t('agent.agentName'),
    dataIndex: 'agentName',
    width: 150,
    align: 'center',
    fixed: 'left'
  },
  {
    title: t('common.platform'),
    dataIndex: 'provider',
    width: 80,
    align: 'center'
  },
  {
    title: t('agent.agentDesc'),
    dataIndex: 'agentDesc',
    align: 'center'
  },
  {
    title: t('common.isDefault'),
    dataIndex: 'isDefault',
    width: 120,
    align: 'center'
  },
  {
    title: t('agent.publishTime'),
    dataIndex: 'publishTime',
    width: 180,
    align: 'center'
  },
  {
    title: t('table.action'),
    key: 'operation',
    width: 150,
    align: 'center',
    fixed: 'right'
  }
])

// Colunas dinâmicas da tabela (adiciona a coluna de ID do agente conforme a plataforma)
const tableColumns = computed(() => {
  if (searchForm.value.provider === 'coze') {
    const cols = [...baseColumns.value]
    const botIdColumn = {
      title: t('agent.botId'),
      dataIndex: 'botId',
      width: 180,
      align: 'center' as const
    }
    // Insere a coluna de ID do agente na terceira posição
    cols.splice(2, 0, botIdColumn)
    return cols
  }
  return baseColumns.value
})

// Carrega dados
const fetchData = async () => {
  await loadData((params) => queryAgents({
    provider: searchForm.value.provider,
    agentName: searchForm.value.agentName,
    pageNo: params.pageNo,
    pageSize: params.pageSize
  }))
}

// Busca com debounce
const debouncedSearch = createDebouncedSearch(fetchData, 500)

// Trata a mudança de paginação da tabela
const onTableChange = (pag: TablePaginationConfig) => {
  handleTableChange(pag)
  fetchData()
}

// ==================== Configuração da plataforma ====================
const currentConfigId = ref<number | null>(null)
const platformFormRef = ref<FormInstance>()

// Dados do formulário de plataforma
const platformForm = reactive<PlatformConfig>({
  configType: 'agent',
  provider: 'coze',
  configName: '',
  configDesc: '',
  appId: '',
  apiKey: '',
  apiSecret: '',
  apiUrl: '',
  ak: '',
  sk: ''
})

// Usa o composable modal
const platformModal = useModal<PlatformConfig>({
  formRef: platformFormRef,
  onSubmit: async (data, isEdit) => {
    // O Modal já possui submitLoading, não precisa de loading global
    try {
      // Se for a plataforma Dify, garante que a apiUrl tenha o formato correto
      if (data.provider === 'dify' && data.apiUrl) {
        let baseUrl = data.apiUrl
        if (baseUrl.endsWith('/')) {
          baseUrl = baseUrl.slice(0, -1)
        }
        data.apiUrl = baseUrl
      }
      
      // Se for modo de edição, adiciona o configId
      if (isEdit && currentConfigId.value) {
        data.configId = currentConfigId.value

        // No modo de edição, remove os campos sensíveis vazios (vazio significa manter o valor original)
        const sensitiveFields: (keyof PlatformConfig)[] = ['apiKey', 'apiSecret', 'ak', 'sk']
        sensitiveFields.forEach(field => {
          if (!data[field]) {
            delete data[field]
          }
        })
      }

      // Chama a API
      const apiFunc = isEdit ? updatePlatformConfig : addPlatformConfig
      const res = await apiFunc(data)
      
      if (res.code === 200) {
        message.success(isEdit ? t('common.updatePlatformConfigSuccess') : t('common.addPlatformConfigSuccess'))
        await fetchData()
        return true
      } else {
        message.error(res.message || (isEdit ? t('common.updatePlatformConfigFailed') : t('common.addPlatformConfigFailed')))
        return false
      }
    } catch (error) {
      console.error('Error with platform config:', error)
      message.error(isEdit ? t('common.updatePlatformConfigFailed') : t('common.addPlatformConfigFailed'))
      return false
    }
  },
  onOpen: async (item) => {
    if (item) {
      // Modo de edição: preenche a configuração existente
      currentConfigId.value = item.configId ?? null
      Object.assign(platformForm, {
        configType: item.configType || 'agent',
        provider: item.provider,
        configName: item.configName || '',
        configDesc: item.configDesc || '',
        appId: item.appId || '',
        apiSecret: item.apiSecret || '',
        apiKey: item.apiKey || '',
        apiUrl: item.apiUrl || '',
        ak: item.ak || '',
        sk: item.sk || ''
      })
    } else {
      // Modo de criação: usa os valores padrão
      // configName usa o nome do provider como valor padrão (cada usuário tem apenas uma credencial de plataforma por provider, sem risco de duplicidade),
      currentConfigId.value = null
      Object.assign(platformForm, {
        configType: 'agent',
        provider: searchForm.value.provider,
        configName: searchForm.value.provider,
        configDesc: '',
        appId: '',
        apiKey: '',
        apiSecret: '',
        apiUrl: searchForm.value.provider === 'dify' ? 'https://api.dify.ai/v1' : '',
        ak: '',
        sk: ''
      })
    }
  }
})

// Configuração dos itens do formulário
const formItems = computed<PlatformFormItems>(() => ({
  coze: [
    {
      field: 'appId',
      label: t('agent.appId'),
      placeholder: t('agent.enterAppId')
    },
    {
      field: 'apiSecret',
      label: t('agent.spaceId'),
      placeholder: t('agent.enterSpaceId')
    },
    {
      field: 'ak',
      label: t('agent.publicKey'),
      placeholder: t('agent.enterPublicKey')
    },
    {
      field: 'sk',
      label: t('agent.privateKey'),
      placeholder: t('agent.enterPrivateKey'),
      type: 'textarea'
    }
  ],
  dify: [
    {
      field: 'apiUrl',
      label: t('agent.apiUrl'),
      placeholder: t('agent.enterApiUrl'),
      suffix: '/chat_message'
    },
    {
      field: 'apiKey',
      label: t('agent.apiKey'),
      placeholder: t('agent.enterApiKey')
    }
  ],
  xingchen: [
    {
      field: 'apiUrl',
      label: t('agent.apiUrl'),
      placeholder: t('agent.enterApiUrl'),
      suffix: '/chat/completions'
    },
    {
      field: 'apiKey',
      label: t('agent.authCode'),
      placeholder: t('agent.enterAuthCode')
    },
    {
      field: 'apiSecret',
      label: t('agent.flowId'),
      placeholder: t('agent.enterFlowId')
    }
  ]
}))

// Regras de validação do formulário
const platformRules = computed(() => {
  const isEditMode = currentConfigId.value !== null

  // Lista de campos sensíveis
  const sensitiveFields = ['apiKey', 'apiSecret', 'ak', 'sk']

  const rules: Record<string, any[]> = {}

  // appId e apiUrl são sempre obrigatórios
  rules.appId = [{ required: true, message: t('agent.enterAppId'), trigger: 'blur' }]
  rules.apiUrl = [{ required: true, message: t('agent.enterApiUrl'), trigger: 'blur' }]

  // Campos sensíveis: não obrigatórios no modo de edição (deixar em branco mantém o valor original)
  sensitiveFields.forEach(field => {
    if (isEditMode) {
      // No modo de edição não é obrigatório
      rules[field] = []
    } else {
      // No modo de criação é obrigatório
      const messageMap: Record<string, string> = {
        apiKey: t('agent.enterApiKey'),
        apiSecret: t('agent.enterSpaceId'),
        ak: t('agent.enterPublicKey'),
        sk: t('agent.enterPrivateKey')
      }
      rules[field] = [{ required: true, message: messageMap[field], trigger: 'blur' }]
    }
  })

  return rules
})

// Itens do formulário da plataforma atual
const currentFormItems = computed(() => {
  return formItems.value[searchForm.value.provider] || []
})

// Obtém o placeholder dos campos sensíveis
function getSensitivePlaceholder(field: string, defaultPlaceholder: string): string {
  const isEditMode = currentConfigId.value !== null
  const sensitiveFields = ['apiKey', 'apiSecret', 'ak', 'sk']

  if (isEditMode && sensitiveFields.includes(field)) {
    return 'Deixe em branco para manter o valor original'
  }

  return defaultPlaceholder
}

// Título da configuração da plataforma
const platformModalTitle = computed(() => {
  const platformMap: Record<string, string> = {
    'coze': t('agent.coze'),
    'dify': t('agent.dify'),
    'xingchen': t('agent.xingchen')
  }
  const platformName = platformMap[searchForm.value.provider] || searchForm.value.provider
  return `${t('common.platformConfig')} - ${platformName}`
})

// Abre o diálogo de configuração da plataforma
const handleConfigPlatform = async () => {
  loadingStore.showLoading(t('common.loading'))
  try {
    // Primeiro verifica se já existe uma configuração
    const [res] = await Promise.all([
      queryPlatformConfig('agent', searchForm.value.provider),
      loadingStore.awaitMinDisplay()
    ])

    if (res.code === 200) {
      const configs = (res.data as { list: any[] })?.list || []

      if (configs.length > 0) {
        // Configuração existente, abre em modo de edição
        await platformModal.openEdit(configs[0])
      } else {
        // Sem configuração, abre em modo de criação
        await platformModal.openCreate()
      }
    } else {
      message.error(res.message || t('common.getPlatformConfigFailed'))
    }
  } catch (error) {
    await loadingStore.awaitMinDisplay()
    console.error('Error fetching platform config:', error)
    message.error(t('common.getPlatformConfigFailed'))
  } finally {
    loadingStore.hideLoading()
  }
}

// Envio da configuração da plataforma
const handlePlatformModalOk = async () => {
  try {
    await platformFormRef.value?.validate()
    await platformModal.submit(platformForm)
  } catch (error) {
    // Falha na validação do formulário, não executa nenhuma ação
    console.error('Falha na validação do formulário:', error)
  }
}

// ==================== Definir como padrão ====================
const handleSetDefault = async (record: Agent) => {
  // Converte o Agent para o formato Config e então chama o setAsDefault unificado
  const configRecord = {
    configId: record.configId,
    configName: record.agentName || record.configName || '',
    modelType: 'chat' as const,
    configType: 'agent' as const,
    provider: 'coze' as const
  }
  
  await setAsDefault(configRecord)
  // Atualiza os dados
  await fetchData()
}

// ==================== Inicialização (carregamento não bloqueante) ====================
fetchData()
</script>

<template>
  <div class="agent-view">
    <!-- Caixa de busca -->
    <a-card :bordered="false" style="margin-bottom: 16px" class="search-card">
      <a-form layout="horizontal" :colon="false">
        <a-row :gutter="16">
          <a-col :xl="8" :lg="12" :xs="24">
            <a-form-item :label="t('common.platform')">
              <a-select v-model:value="searchForm.provider" @change="debouncedSearch">
                <a-select-option v-for="item in providerOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xl="8" :lg="12" :xs="24">
            <a-form-item :label="t('agent.agentName')">
              <a-input
                v-model:value="searchForm.agentName"
                :placeholder="t('agent.enterAgentName')"
                allow-clear
                @input="debouncedSearch"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- Dados da tabela -->
    <a-card :title="t('menu.agent')" :bordered="false">
      <template #extra>
        <a-button
          v-permission="['system:config:agent:create', 'system:config:agent:update']"
          type="primary"
          @click="handleConfigPlatform"
        >
          <template #icon>
            <SettingOutlined />
          </template>
          {{ t('common.platformConfig') }}
        </a-button>
      </template>

      <a-table
        row-key="configId"
        :columns="tableColumns"
        :data-source="agentList"
        :loading="loading"
        :pagination="pagination"
        @change="onTableChange"
        size="middle"
        :scroll="{ x: 1000 }"
      >
        <!-- Avatar -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'iconUrl'">
            <a-avatar :src="record.iconUrl" shape="square" :size="48" />
          </template>

          <!-- Nome do agente -->
          <template v-else-if="column.dataIndex === 'agentName'">
            <a-tooltip :title="record.agentName" :mouse-enter-delay="0.5" placement="topLeft">
              <span v-if="record.agentName" class="ellipsis-text">{{ record.agentName }}</span>
              <span v-else>-</span>
            </a-tooltip>
          </template>

          <!-- Plataforma -->
          <template v-else-if="column.dataIndex === 'provider'">
            <a-tag color="blue">{{ record.provider }}</a-tag>
          </template>

          <!-- Descrição do agente -->
          <template v-else-if="column.dataIndex === 'agentDesc'">
            <a-tooltip :title="record.agentDesc" :mouse-enter-delay="0.5" placement="topLeft">
              <span v-if="record.agentDesc" class="ellipsis-text">{{ record.agentDesc }}</span>
              <span v-else>-</span>
            </a-tooltip>
          </template>

          <!-- Status padrão -->
          <template v-else-if="column.dataIndex === 'isDefault'">
            <a-tag v-if="record.isDefault == 1" color="green">{{ t('common.default') }}</a-tag>
            <span v-else>-</span>
          </template>

          <!-- Ações -->
          <template v-else-if="column.key === 'operation'">
            <TableActionButtons
              :record="record"
              permission-prefix="system:config:agent"
              show-set-default
              :is-default="record.isDefault == 1"
              @set-default="handleSetDefault"
            />
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Diálogo de configuração da plataforma -->
    <a-modal
      :title="platformModalTitle"
      :open="platformModal.visible.value"
      :confirm-loading="platformModal.submitLoading.value"
      @ok="handlePlatformModalOk"
      @cancel="platformModal.close"
      :width="600"
    >
      <a-form
        ref="platformFormRef"
        :model="platformForm"
        :rules="platformRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          v-for="item in currentFormItems"
          :key="item.field"
          :label="item.label"
          :name="item.field"
        >
          <a-textarea
            v-if="item.type === 'textarea'"
            v-model:value="platformForm[item.field as keyof PlatformConfig] as string"
            :placeholder="getSensitivePlaceholder(item.field, item.placeholder)"
            :rows="4"
            :auto-size="{ minRows: 4, maxRows: 8 }"
          />
          <a-input
            v-else
            v-model:value="platformForm[item.field as keyof PlatformConfig] as string"
            :placeholder="getSensitivePlaceholder(item.field, item.placeholder)"
          >
            <template v-if="item.suffix" #suffix>
              <span style="color: var(--ant-color-text-tertiary)">{{ item.suffix }}</span>
            </template>
          </a-input>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.agent-view {
  padding: 24px;
}

.search-card :deep(.ant-form-item) {
  margin-bottom: 0;
}

.ellipsis-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
