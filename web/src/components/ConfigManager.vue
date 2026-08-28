<script setup lang="ts">
import { ref, computed } from 'vue'
import { message as antMessage, Modal } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance, TableColumnsType } from 'ant-design-vue'
import { useConfigManager } from '@/composables/useConfigManager'
import TableActionButtons from '@/components/TableActionButtons.vue'
import type { ConfigType, Config, ConfigField } from '@/types/config'
import { addConfig, updateConfig } from '@/services/config'

const { t } = useI18n()

interface Props {
  configType: ConfigType
}

const props = defineProps<Props>()

// Usa o composable
const {
  loading,
  configItems,
  currentType,
  editingConfigId,
  activeTabKey,
  modelOptions,
  pagination,
  queryForm,
  configTypeInfo,
  typeOptions,
  currentTypeFields,
  fetchData,
  deleteConfig,
  setAsDefault,
  updateModelOptions,
  getModelsByProviderAndType,
} = useConfigManager(props.configType)

// Opções predefinidas de configName do provider atual (usado no AutoComplete)
const currentConfigNameOptions = computed(() => {
  const option = typeOptions.value.find((item) => item.value === currentType.value)
  return option?.configNameOptions?.map((v) => ({ value: v })) ?? []
})

// Formulário
const formRef = ref<FormInstance>()
const formData = ref<Partial<Config>>({
  provider: undefined,
  configName: undefined,
  configDesc: undefined,
  modelType: 'chat',
  isDefault: false,
  apiKey: undefined,
  apiUrl: undefined,
  appId: undefined,
  apiSecret: undefined,
  ak: undefined,
  sk: undefined,
  enableThinking: false,
})

// Configuração das colunas da tabela
const columns = computed(() => {
  const baseColumns: TableColumnsType = [
    {
      title: t('config.category'),
      dataIndex: 'provider',
      width: 150,
      align: 'center',
      customRender: ({ text }: { text: string }) => {
        const provider = typeOptions.value.find((item) => item.value === text)
        return provider ? provider.label : text
      },
    },
    {
      title: t('common.name'),
      dataIndex: 'configName',
      width: 200,
      align: 'center',
    },
  ]

  // LLM adiciona coluna de tipo de modelo
  if (props.configType === 'llm') {
    baseColumns.push({
      title: t('config.modelType'),
      dataIndex: 'modelType',
      width: 120,
      align: 'center' as const,
    })
  }

  baseColumns.push(
    {
      title: t('common.description'),
      dataIndex: 'configDesc',
      width: 200,
      align: 'center' as const
    },
    {
      title: t('common.isDefault'),
      dataIndex: 'isDefault',
      width: 120,
      align: 'center' as const,
    },
    {
      title: t('common.createTime'),
      dataIndex: 'createTime',
      width: 200,
      align: 'center' as const,
    },
    {
      title: t('table.action'),
      dataIndex: 'operation',
      width: 180,
      align: 'center' as const,
      fixed: 'right' as const,
    }
  )

  // TTS filtra a coluna de padrão
  if (props.configType === 'tts') {
    return baseColumns.filter((col) => {
      return 'dataIndex' in col && col.dataIndex !== 'isDefault'
    })
  }

  return baseColumns
})

/**
 * Trata a mudança de tipo
 */
function handleTypeChange(value: string) {
  currentType.value = value
  
  // Limpa o nome do modelo
  formData.value.configName = undefined
  
  // Se for LLM, atualiza as opções de modelo
  if (props.configType === 'llm' && formData.value.modelType) {
    updateModelOptions(value, formData.value.modelType)
  }
  
  // Preenche a URL padrão
  const typeField = currentTypeFields.value.find((f: ConfigField) => f.name === 'apiUrl')
  if (typeField?.placeholder) {
    formData.value.apiUrl = typeField.placeholder
  }
}

/**
 * Trata a mudança de tipo de modelo
 */
function handleModelTypeChange(value: string) {
  if (currentType.value) {
    updateModelOptions(currentType.value, value)
    formData.value.configName = undefined
  }
}

/**
 * Trata a troca de aba
 */
function handleTabChange(key: string) {
  activeTabKey.value = key
  if (key === '1') {
    fetchData()
  } else if (key === '2') {
    resetForm()
  }
}

/**
 * Editar configuração
 */
function handleEdit(record: Config) {
  editingConfigId.value = record.configId
  currentType.value = record.provider || ''
  activeTabKey.value = '2'

  // Define os valores do formulário, convertendo o valor do backend ('0'/'1') em boolean
  formData.value = {
    ...record,
    isDefault: props.configType != 'tts' ? record.isDefault === '1' : false,
    enableThinking: !!record.enableThinking,
  }

  // LLM atualiza as opções de modelo
  if (props.configType === 'llm') {
    updateModelOptions(record.provider, record.modelType || 'chat')
  }
}

/**
 * Envia o formulário
 */
async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    
    // Prepara os dados a enviar
    const submitData: Partial<Config> = {
      ...formData.value,
      configId: editingConfigId.value,
      configType: props.configType,
    }

    // Trata isDefault: converte boolean no string enum ('0'/'1') esperado pelo backend
    submitData.isDefault = formData.value.isDefault == '1' ? '1' : '0'

    // Validação especial de LLM
    if (props.configType === 'llm') {
      // Valida o nome do modelo
      if (submitData.configName && /[\u4e00-\u9fa5]/.test(submitData.configName)) {
        antMessage.error(t('config.modelNameNoChinese'))
        return
      }

      const validModels = getModelsByProviderAndType(
        submitData.provider || '',
        submitData.modelType || 'chat'
      )
      const isValid = validModels.some((m: any) => m.llm_name === submitData.configName)
      
      // Se o nome do modelo não existir, pergunta ao usuário se deseja continuar
      if (!isValid && validModels.length > 0) {
        try {
          await Modal.confirm({
            title: t('common.confirmSubmit'),
            content: t('config.modelNameInvalid', { name: submitData.configName }),
            okText: t('common.confirm'),
            cancelText: t('common.cancel'),
          })
          // Usuário confirmou, continua a execução
        } catch {
          // Usuário cancelou, interrompe o fluxo
          return
        }
      }
    }

    loading.value = true

    const res = editingConfigId.value
      ? await updateConfig(submitData)
      : await addConfig(submitData)

    if (res.code === 200) {
      antMessage.success(editingConfigId.value ? t('config.updateSuccess') : t('config.createSuccess'))
      resetForm()
      fetchData()
      activeTabKey.value = '1'
    } else {
      antMessage.error(res.message || t('common.operationFailed'))
    }
  } catch (error: unknown) {
    if (error && typeof error === 'object' && 'errorFields' in error) {
      antMessage.error(t('config.fillRequiredFields'))
    } else {
      console.error('Falha ao enviar configuração:', error)
      antMessage.error(t('common.operationFailed'))
    }
  } finally {
    loading.value = false
  }
}

/**
 * Redefinir formulário
 */
function resetForm() {
  formRef.value?.resetFields()
  currentType.value = ''
  editingConfigId.value = undefined
  modelOptions.value = []
  formData.value = {
    provider: undefined,
    configName: undefined,
    configDesc: undefined,
    modelType: 'chat',
    isDefault: false,
    apiKey: undefined,
    apiUrl: undefined,
    appId: undefined,
    apiSecret: undefined,
    ak: undefined,
    sk: undefined,
    enableThinking: false,
  }
}

/**
 * Cancelar
 */
function handleCancel() {
  resetForm()
  activeTabKey.value = '1'
}

/**
 * Obtém a cor padrão da tag
 */
function getDefaultTagColor(record: Config) {
  if (props.configType === 'llm') {
    const colors: Record<string, string> = {
      chat: 'blue',
      vision: 'purple',
      intent: 'orange',
      embedding: 'green',
    }
    return colors[record.modelType || ''] || 'green'
  }
  return props.configType === 'stt' ? 'cyan' : 'green'
}

/**
 * Obtém o texto padrão da tag
 */
function getDefaultTagText(record: Config) {
  if (props.configType === 'llm') {
    const texts: Record<string, string> = {
      chat: t('config.defaultChat'),
      vision: t('config.defaultVision'),
      intent: t('config.defaultIntent'),
      embedding: t('config.defaultEmbedding'),
    }
    return texts[record.modelType || ''] || t('common.default')
  }
  return props.configType === 'stt' ? t('config.defaultStt') : t('common.default')
}

/**
 * Tag de tipo de modelo
 */
function getModelTypeTag(modelType: string) {
  const tags: Record<string, { text: string; color: string }> = {
    chat: { text: t('config.chatModel'), color: 'blue' },
    vision: { text: t('config.visionModel'), color: 'purple' },
    intent: { text: t('config.intentModel'), color: 'orange' },
    embedding: { text: t('config.embeddingModel'), color: 'green' },
  }
  return tags[modelType] || { text: '-', color: 'default' }
}

// Trata a mudança da tabela
const handleTableChangeWrapper = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

// Inicialização
fetchData()
</script>

<template>
  <div class="config-manager">
    <!-- Formulário de consulta -->
    <a-card :bordered="false" style="margin-bottom: 16px" class="search-card">
      <a-form layout="horizontal" :colon="false">
        <a-row :gutter="16">
          <a-col :xxl="8" :xl="8" :lg="12" :xs="24">
            <a-form-item :label="t('config.category')">
              <a-select v-model:value="queryForm.provider" @change="fetchData">
                <a-select-option value="">{{ t('common.all') }}</a-select-option>
                <a-select-option
                  v-for="item in typeOptions"
                  :key="item.value"
                  :value="item.value"
                >
                  {{ item.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <a-col :xl="8" :lg="12" :xs="24">
            <a-form-item :label="t('common.name')">
              <a-input
                v-model:value="queryForm.configName"
                :placeholder="t('config.pleaseEnter')"
                allow-clear
                @press-enter="fetchData"
              />
            </a-form-item>
          </a-col>

          <a-col v-if="configType === 'llm'" :xxl="8" :xl="8" :lg="12" :xs="24">
            <a-form-item :label="t('config.modelType')">
              <a-select v-model:value="queryForm.modelType" @change="fetchData">
                <a-select-option value="">{{ t('common.all') }}</a-select-option>
                <a-select-option value="chat">{{ t('config.chatModel') }}</a-select-option>
                <a-select-option value="vision">{{ t('config.visionModel') }}</a-select-option>
                <a-select-option value="intent">{{ t('config.intentModel') }}</a-select-option>
                <a-select-option value="embedding">{{ t('config.embeddingModel') }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- Tabela e formulário -->
    <a-card :bordered="false" :body-style="{ padding: '0 24px 24px 24px' }">
      <a-tabs
        v-model:active-key="activeTabKey"
        @change="handleTabChange"
      >
        <!-- Aba de listagem -->
        <a-tab-pane key="1" :tab="`${t(configTypeInfo.label)} ${t('config.list')}`">
          <a-table
            :columns="columns"
            :data-source="configItems"
            :loading="loading"
            :pagination="pagination"
            @change="handleTableChangeWrapper"
            row-key="configId"
            :scroll="{ x: 800 }"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <!-- Coluna de tipo de modelo -->
              <template v-if="column.dataIndex === 'modelType' && configType === 'llm'">
                <a-tag :color="getModelTypeTag(record.modelType).color">
                  {{ getModelTypeTag(record.modelType).text }}
                </a-tag>
                <a-tag v-if="record.enableThinking" color="cyan">
                  {{ t('config.enableThinking') }}
                </a-tag>
              </template>

              <!-- Coluna de descrição -->
              <template v-else-if="column.dataIndex === 'configDesc'">
                <a-tooltip :title="record.configDesc" :mouse-enter-delay="0.5" placement="topLeft">
                  <span v-if="record.configDesc" class="ellipsis-text">{{ record.configDesc }}</span>
                  <span v-else>-</span>
                </a-tooltip>
              </template>

              <!-- Coluna de identificação padrão -->
              <template v-else-if="column.dataIndex === 'isDefault'">
                <a-tag v-if="record.isDefault === '1'" :color="getDefaultTagColor(record)">
                  {{ getDefaultTagText(record) }}
                </a-tag>
                <span v-else>-</span>
              </template>

              <!-- Coluna de ações -->
              <template v-else-if="column.dataIndex === 'operation'">
                <TableActionButtons
                  :record="record"
                  :permission-prefix="configTypeInfo.permissionPrefix"
                  show-edit
                  :show-set-default="configType !== 'tts'"
                  :show-delete="record.isDefault !== '1'"
                  :is-default="record.isDefault === '1'"
                  :delete-title="t('config.confirmDelete', { type: t(configTypeInfo.label) })"
                  @edit="handleEdit"
                  @set-default="setAsDefault"
                  @delete="() => deleteConfig(record.configId)"
                />
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- Aba de criação/edição -->
        <a-tab-pane
          v-permission="editingConfigId ? `${configTypeInfo.permissionPrefix}:update` : `${configTypeInfo.permissionPrefix}:create`"
          key="2"
          :tab="editingConfigId ? `${t('config.update', { type: t(configTypeInfo.label) })}` : `${t('config.create')} ${t(configTypeInfo.label)}`"
        >
          <a-form
            ref="formRef"
            :model="formData"
            layout="horizontal"
            :colon="false"
            style="padding: 10px 24px"
            :hideRequiredMark="true"
          >
            <a-row :gutter="20">
              <a-col :xl="8" :lg="12" :xs="24">
                <a-form-item
                  :label="`${t(configTypeInfo.label)} ${t('config.category')}`"
                  name="provider"
                  :rules="[{ required: true, message: t('config.selectCategory', { type: t(configTypeInfo.label) }) }]"
                >
                  <a-select
                    v-model:value="formData.provider"
                    :placeholder="t('config.selectCategory', { type: t(configTypeInfo.label) })"
                    @change="handleTypeChange"
                  >
                    <a-select-option
                      v-for="item in typeOptions"
                      :key="item.value"
                      :value="item.value"
                    >
                      {{ item.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>

              <!-- Tipo de modelo LLM -->
              <a-col v-if="configType === 'llm'" :xl="8" :lg="12" :xs="24">
                <a-form-item
                  :label="t('config.modelType')"
                  name="modelType"
                  :rules="[{ required: true, message: t('config.selectModelType') }]"
                >
                  <a-select
                    v-model:value="formData.modelType"
                    :placeholder="t('config.selectModelType')"
                    @change="handleModelTypeChange"
                  >
                    <a-select-option value="chat">{{ t('config.chatModel') }}</a-select-option>
                    <a-select-option value="vision">{{ t('config.visionModel') }}</a-select-option>
                    <a-select-option value="intent">{{ t('config.intentModel') }}</a-select-option>
                    <a-select-option value="embedding">{{ t('config.embeddingModel') }}</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>

              <a-col :xl="8" :lg="12" :xs="24">
                <a-form-item
                  :label="`${t(configTypeInfo.label)} ${t('common.name')}`"
                  name="configName"
                  :rules="[{ required: true, message: t('config.enterName', { type: t(configTypeInfo.label) }) }]"
                >
                  <!-- LLM usa AutoComplete (opcional e editável) -->
                  <a-auto-complete
                    v-if="configType === 'llm' && currentType"
                    v-model:value="formData.configName"
                    allow-clear
                    :placeholder="t('config.enterName', { type: t(configTypeInfo.label) })"
                    :options="modelOptions"
                    :filter-option="
                      (input: string, option: any) =>
                        option.label.toLowerCase().includes(input.toLowerCase())
                    "
                  />
                  <!-- Quando há opções de modelo predefinidas, usa AutoComplete (opcional e editável) -->
                  <a-auto-complete
                    v-else-if="currentConfigNameOptions.length > 0"
                    v-model:value="formData.configName"
                    :options="currentConfigNameOptions"
                    :placeholder="t('config.enterName', { type: t(configTypeInfo.label) })"
                    :filter-option="
                      (input: string, option: any) =>
                        option.value.toLowerCase().includes(input.toLowerCase())
                    "
                    allow-clear
                  />
                  <!-- Demais casos usam campo de texto -->
                  <a-input
                    v-else
                    v-model:value="formData.configName"
                    :placeholder="t('config.enterName', { type: t(configTypeInfo.label) })"
                  />
                </a-form-item>
              </a-col>
            </a-row>

            <a-form-item :label="`${t(configTypeInfo.label)} ${t('common.description')}`" name="configDesc">
              <a-textarea
                v-model:value="formData.configDesc"
                :placeholder="t('config.enterDescription', { type: t(configTypeInfo.label) })"
                :rows="4"
              />
            </a-form-item>

            <!-- Definir como padrão -->
            <a-form-item
              v-if="configType !== 'tts'"
              v-permission="`${configTypeInfo.permissionPrefix}:update`"
              :label="`${t('common.setAsDefault')}${t(configTypeInfo.label)}`"
              name="isDefault"
            >
              <a-switch v-model:checked="formData.isDefault" />
              <span style="margin-left: 8px; color: var(--ant-color-text-tertiary)">
                {{ t('config.defaultTip') }}
              </span>
            </a-form-item>

            <!-- Alternância de modo de raciocínio (apenas LLM) -->
            <a-form-item
              v-if="configType === 'llm'"
              :label="t('config.enableThinking')"
              name="enableThinking"
            >
              <a-switch v-model:checked="formData.enableThinking" />
              <span style="margin-left: 8px; color: var(--ant-color-text-tertiary)">
                {{ t('config.enableThinkingTip') }}
              </span>
            </a-form-item>

            <a-divider>{{ t('config.parameterConfig') }}</a-divider>

            <!-- Campos de parâmetros dinâmicos -->
            <a-card
              v-if="currentType"
              size="small"
              :bordered="false"
            >
              <!-- Instruções de uso do modelo local -->
              <a-alert
                v-if="currentType === 'sherpa-onnx' && configType === 'tts'"
                type="info"
                show-icon
                style="margin-bottom: 16px"
              >
                <template #message>Instruções de uso de síntese de voz local (Sherpa-ONNX)</template>
                <template #description>
                  <div style="font-size: 13px; line-height: 2">
                    <p style="margin: 0">Não é necessário preencher nenhum parâmetro; após salvar, a voz local já pode ser selecionada na configuração de personagem.</p>
                    <p style="margin: 0">Suporta as três arquiteturas de modelo <strong>VITS</strong>, <strong>Kokoro</strong> e <strong>Matcha</strong>, identificadas automaticamente pelo sistema.</p>
                    <p style="margin: 0">Diretório de armazenamento dos modelos: <code>models/tts/</code>, cada subdiretório corresponde a um modelo.</p>
                    <p style="margin: 0">O modelo Matcha requer o download adicional do arquivo vocoder (como <code>vocos-16khz-univ.onnx</code>) para colocar no diretório do modelo.</p>
                    <p style="margin: 0">Se o modelo em chinês não tiver o diretório <code>dict/</code>, é possível criar um link simbólico para o diretório dict de outro modelo e compartilhá-lo.</p>
                  </div>
                </template>
              </a-alert>
              <a-row :gutter="20">
                <a-col
                  v-for="field in currentTypeFields"
                  :key="field.name"
                  :xl="field.span || 12"
                  :lg="12"
                  :xs="24"
                >
                  <a-form-item
                    :label="field.label"
                    :name="field.name"
                    :rules="[{ required: editingConfigId && ['apiKey', 'apiSecret', 'ak', 'sk'].includes(field.name) ? false : field.required, message: t('config.enterField', { field: field.label }) }]"
                    style="margin-bottom: 24px"
                  >
                    <a-input
                      v-model:value="formData[field.name]"
                      :placeholder="(editingConfigId && ['apiKey', 'apiSecret', 'ak', 'sk'].includes(field.name) && currentType !== 'sherpa-onnx') ? 'Deixe em branco para não alterar' : (field.placeholder || t('config.enterField', { field: field.label }))"
                      :type="field.inputType || 'text'"
                    >
                      <template v-if="field.suffix" #suffix>
                        <span style="color: var(--ant-color-text-tertiary)">{{ field.suffix }}</span>
                      </template>
                    </a-input>
                    <div v-if="field.help" class="field-help">
                      {{ field.help }}
                    </div>
                  </a-form-item>
                </a-col>
              </a-row>
            </a-card>

            <a-card v-else :bordered="false">
              <a-empty :description="t('config.selectCategoryFirst', { type: t(configTypeInfo.label) })" />
            </a-card>

            <a-form-item style="margin-top: 24px">
              <a-space>
                <a-button
                  v-permission="editingConfigId ? `${configTypeInfo.permissionPrefix}:update` : `${configTypeInfo.permissionPrefix}:create`"
                  type="primary"
                  :loading="loading"
                  @click="handleSubmit"
                >
                  {{ editingConfigId ? t('config.update', { type: t(configTypeInfo.label) }) : t('config.create', { type: t(configTypeInfo.label) }) }}
                </a-button>
                <a-button @click="handleCancel">{{ t('common.cancel') }}</a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<style scoped lang="scss">
.config-manager {
  padding: 24px;
}

.search-card :deep(.ant-form-item) {
  margin-bottom: 0;
}

.field-help {
  margin-top: 4px;
  font-size: 12px;
  color: #999;
}

.ellipsis-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
