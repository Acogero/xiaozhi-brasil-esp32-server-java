<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, onBeforeRouteLeave } from 'vue-router'
import { message as antMessage, type TablePaginationConfig } from 'ant-design-vue'
import { useTable } from '@/composables/useTable'
import { useExport } from '@/composables/useExport'
import { useSelectLoadMore } from '@/composables/useSelectLoadMore'
import { useLoadingStore } from '@/store/loading'
import { queryRoles } from '@/services/role'
import { queryDevices } from '@/services/device'
import { deleteMessage } from '@/services/message'
import {
  querySummaryMemory,
  queryChatMemory,
  deleteSummaryMemory,
} from '@/services/memory'
import AudioPlayer from '@/components/AudioPlayer.vue'
import TableActionButtons from '@/components/TableActionButtons.vue'
import type { Role } from '@/types/role'
import type { Device } from '@/types/device'
import type { SummaryMemory, ChatMemory } from '@/types/memory'
import dayjs, { Dayjs } from 'dayjs'
import { useEventBus } from '@vueuse/core'

const { t } = useI18n()
const route = useRoute()
const loadingStore = useLoadingStore()

// Deriva o tipo de memória a partir do caminho da rota
const memoryType = computed<'chat' | 'summary'>(() => {
  if (route.path.endsWith('/summary')) return 'summary'
  return 'chat'
})

// Obtém os parâmetros da rota
const roleId = computed(() => parseInt(route.query.roleId as string) || 0)
const routeDeviceId = computed(() => route.query.deviceId as string || '')

// Tabela e paginação
const {
  loading,
  data,
  pagination,
  handleTableChange,
  loadData,
} = useTable<SummaryMemory | ChatMemory>()

// Usa o composable de exportação
const { exporting, exportToExcel } = useExport()

// Barramento de eventos
const stopAllAudioBus = useEventBus<void>('stop-all-audio')

// Dropdown de perfis (carregamento por rolagem)
const {
  list: roles,
  loading: rolesLoading,
  load: loadRoles,
  onPopupScroll: onRolePopupScroll,
} = useSelectLoadMore<Role>(queryRoles)
const selectedRoleId = ref<number>(0)

// Dropdown de dispositivos (carregamento por rolagem)
const {
  list: devices,
  loading: devicesLoading,
  load: loadDevices,
  onPopupScroll: onDevicePopupScroll,
} = useSelectLoadMore<Device>(queryDevices)
const selectedDeviceId = ref<string>('')

// Intervalo de tempo
const timeRange = ref<[Dayjs, Dayjs]>([dayjs().startOf('month'), dayjs().endOf('month')])

// Opções rápidas de data
const rangePresets = computed(() => [
  { label: t('message.today'), value: [dayjs().startOf('day'), dayjs().endOf('day')] },
  { label: t('message.thisMonth'), value: [dayjs().startOf('month'), dayjs().endOf('month')] },
])

// Nome do dispositivo selecionado atualmente (o backend não retorna deviceName para o tipo long, então o frontend obtém diretamente)
const selectedDeviceName = computed(() => {
  if (!selectedDeviceId.value) return ''
  return devices.value.find((d: Device) => d.deviceId === selectedDeviceId.value)?.deviceName || selectedDeviceId.value
})

// Configuração das colunas da tabela
const columns = computed(() => {

  const baseColumns = [
    {
      title: t('message.conversationTime'),
      dataIndex: 'createTime',
      width: 180,
      align: 'center' as const,
    },
    {
      title: t('device.deviceName'),
      dataIndex: 'deviceName',
      width: 120,
      align: 'center' as const,
    },
  ]

  if (memoryType.value === 'summary') {
    return [
      ...baseColumns,
      {
        title: t('memory.summary'),
        dataIndex: 'summary',
        width: 300,
        align: 'center' as const,
      },
      {
        title: t('table.action'),
        dataIndex: 'operation',
        width: 110,
        fixed: 'right' as const,
        align: 'center' as const,
      },
    ]
  } else {
    // chat tab
    return [
      ...baseColumns,
      {
        title: t('message.messageSender'),
        dataIndex: 'sender',
        width: 100,
        align: 'center' as const,
      },
      {
        title: t('message.toolCalls'),
        dataIndex: 'messageType',
        width: 250,
        align: 'center' as const,
      },
      {
        title: t('message.messageContent'),
        dataIndex: 'message',
        width: 300,
        align: 'center' as const,
      },
      {
        title: t('message.voice'),
        dataIndex: 'audioPath',
        width: 400,
        align: 'center' as const,
      },
      {
        title: t('table.action'),
        dataIndex: 'operation',
        width: 160,
        fixed: 'right' as const,
        align: 'center' as const,
      },
    ]
  }
})

/**
 * Inicializa os dados dos dropdowns e carrega a tabela
 */
async function initSelects() {
  await Promise.all([loadRoles(), loadDevices()])

  const needDefault = memoryType.value === 'summary'

  // Prioriza o parâmetro da rota; caso contrário, seleciona automaticamente o primeiro summary/long
  if (roleId.value) {
    selectedRoleId.value = roleId.value
  } else if (needDefault && roles.value.length > 0) {
    selectedRoleId.value = roles.value[0]!.roleId
  }

  if (routeDeviceId.value && devices.value.find((d: Device) => d.deviceId === routeDeviceId.value)) {
    selectedDeviceId.value = routeDeviceId.value
  } else if (needDefault && devices.value.length > 0) {
    selectedDeviceId.value = devices.value[0]!.deviceId
  }

  await fetchMemoryData()
}

/**
 * Função de filtro de perfil
 */
function filterRoleOption(input: string, option: any) {
  return option.children?.[0]?.children?.toLowerCase().includes(input.toLowerCase())
}

/**
 * Trata a mudança de perfil
 */
async function handleRoleChange(roleIdValue: number) {
  selectedRoleId.value = roleIdValue
  data.value = []
  await fetchMemoryData()
}

/**
 * Obtém os dados de memória
 */
async function fetchMemoryData() {
  const params: any = {
    pageNo: pagination.current || 1,
    pageSize: pagination.pageSize || 10,
  }

  // Só adiciona roleId se um perfil foi selecionado
  if (selectedRoleId.value) {
    params.roleId = selectedRoleId.value
  }

  // Só adiciona deviceId se um dispositivo foi selecionado
  if (selectedDeviceId.value) {
    params.deviceId = selectedDeviceId.value
  }

  try {
    if (memoryType.value === 'chat') {
      await loadData(() => queryChatMemory({
        ...params,
        startTime: timeRange.value[0].format('YYYY-MM-DD HH:mm:ss'),
        endTime: timeRange.value[1].format('YYYY-MM-DD HH:mm:ss'),
      }))
    } else if (memoryType.value === 'summary') {
      await loadData(() => querySummaryMemory(params))
    }
  } catch (error) {
    console.error('Falha ao carregar dados de memória:', error)
    antMessage.error(t('common.loadFailed'))
  }
}

/**
 * Trata a exclusão de memória
 */
async function handleDeleteMemory(record: any) {
  loading.value = true
  try {
    let res
    if (memoryType.value === 'summary') {
      // Para summary, usa o id (milissegundos do createTime) para excluir o registro específico
      res = await deleteSummaryMemory(selectedRoleId.value, selectedDeviceId.value, record.id)
    }

    if (res?.code === 200) {
      antMessage.success(t('common.deleteSuccess'))
      await fetchMemoryData()
    } else {
      antMessage.error(res?.message || t('common.deleteFailed'))
    }
  } catch (error) {
    console.error('Falha ao excluir memória:', error)
    antMessage.error(t('common.deleteFailed'))
  } finally {
    loading.value = false
  }
}

/**
 * Trata a mudança de dispositivo
 */
async function handleDeviceChange(deviceId: string) {
  selectedDeviceId.value = deviceId
  await fetchMemoryData()
}

/**
 * Trata a mudança de paginação
 */
const onTableChange = (pag: TablePaginationConfig) => {
  handleTableChange(pag)
  fetchMemoryData()
}

/**
 * Obtém o texto de exibição do remetente
 */
function getSenderText(sender: string) {
  return sender === 'user' ? t('message.user') : t('message.assistant')
}

/**
 * Converte a string JSON de toolCalls em um array
 */
function parseToolCalls(toolCalls: string | undefined | null): { name: string; arguments: string; result: string }[] {
  if (!toolCalls) return []
  try {
    const parsed = JSON.parse(toolCalls)
    return Array.isArray(parsed) ? parsed : [parsed]
  } catch {
    return []
  }
}

/**
 * Verifica se o caminho de áudio é válido
 */
function hasValidAudio(audioPath: string | undefined | null): boolean {
  if (!audioPath || !audioPath.trim()) return false
  return true
}

/**
 * Exclui mensagem de chat
 */
async function handleDeleteMessage(record: any) {
  loading.value = true
  try {
    const res = await deleteMessage(record.messageId)
    if (res.code === 200) {
      antMessage.success(t('common.deleteSuccess'))
      await fetchMemoryData()
    }
  } catch (error) {
    console.error('Falha ao excluir mensagem:', error)
    antMessage.error(t('common.deleteFailed'))
  } finally {
    loading.value = false
  }
}

/**
 * Exporta os dados atuais
 */
async function handleExport() {
  if (!data.value || data.value.length === 0) {
    antMessage.warning(t('export.noData'))
    return
  }

  loadingStore.showLoading(t('common.exporting'))
  try {
    let columns: any[] = []
    let filename = ''

    if (memoryType.value === 'chat') {
      filename = `chat_memory_${dayjs().format('YYYY-MM-DD_HH-mm-ss')}`
      columns = [
        { key: 'deviceName', title: t('device.deviceName') },
        {
          key: 'sender',
          title: t('message.messageSender'),
          format: (val: string) => val === 'user' ? t('message.user') : t('message.assistant')
        },
        { key: 'message', title: t('message.messageContent') },
        { key: 'createTime', title: t('message.conversationTime') }
      ]
    } else if (memoryType.value === 'summary') {
      filename = `summary_memory_${dayjs().format('YYYY-MM-DD_HH-mm-ss')}`
      columns = [
        { key: 'deviceName', title: t('device.deviceName') },
        { key: 'summary', title: t('memory.summary') },
        { key: 'createTime', title: t('message.conversationTime') }
      ]
    }

    await exportToExcel(data.value, {
      filename,
      showLoading: false,
      columns
    })
    antMessage.success(t('common.exportSuccess'))
  } catch (error) {
    console.error('Falha ao exportar:', error)
    antMessage.error(t('common.exportFailed'))
  } finally {
    loadingStore.hideLoading()
  }
}

// Para todos os áudios antes de sair da rota
onBeforeRouteLeave(() => {
  stopAllAudioBus.emit()
})

// Para todos os áudios antes de destruir o componente
onBeforeUnmount(() => {
  stopAllAudioBus.emit()
})

// Inicialização
onMounted(async () => {
  await initSelects()
})
</script>

<template>
  <div class="memory-management-view">
    <!-- Barra de filtros -->
    <a-card :bordered="false" style="margin-bottom: 16px" class="search-card">
      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item :label="t('role.roleName')">
            <a-select
              v-model:value="selectedRoleId"
              show-search
              :filter-option="filterRoleOption"
              :loading="rolesLoading"
              @change="handleRoleChange"
              @popup-scroll="onRolePopupScroll"
            >
              <a-select-option v-if="memoryType === 'chat'" :value="0">
                {{ t('common.all') }}
              </a-select-option>
              <a-select-option
                v-for="role in roles"
                :key="role.roleId"
                :value="role.roleId"
              >
                {{ role.roleName }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item :label="t('device.deviceName')">
            <a-select
              v-model:value="selectedDeviceId"
              :loading="devicesLoading"
              @change="handleDeviceChange"
              @popup-scroll="onDevicePopupScroll"
            >
              <a-select-option v-if="memoryType === 'chat'" value="">
                {{ t('common.all') }}
              </a-select-option>
              <a-select-option
                v-for="device in devices"
                :key="device.deviceId"
                :value="device.deviceId"
              >
                {{ device.deviceName }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>

        <a-col v-if="memoryType === 'chat'" :span="8">
          <a-form-item :label="t('message.conversationDate')">
            <a-range-picker
              v-model:value="timeRange"
              :presets="rangePresets"
              :allow-clear="false"
              format="MM-DD"
              @change="fetchMemoryData"
            />
          </a-form-item>
        </a-col>
      </a-row>
    </a-card>

    <!-- Tabela de dados de memória -->
    <a-card :bordered="false">
      <template #title>
        <a-space>
          <span>{{ t(`router.title.${memoryType === 'chat' ? 'shortTermMemory' : 'summaryMemory'}`) }}</span>
        </a-space>
      </template>
      <template #extra>
        <a-button v-permission="'system:role:memory:export'" type="primary" @click="handleExport" :loading="exporting">
          {{ t('common.export') }}
        </a-button>
      </template>

      <!-- Tabela de memória de curto prazo -->
      <a-table
        v-if="memoryType === 'chat'"
        row-key="messageId"
        :columns="columns"
        :data-source="data"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 800 }"
        size="middle"
        :expandable="{
          rowExpandable: (record: any) => !!record.toolCalls,
        }"
        @change="onTableChange"
      >
        <template #expandedRowRender="{ record }">
          <a-table
            :columns="[
              { title: t('message.toolName'), dataIndex: 'name', width: 300 },
              { title: t('message.toolArguments'), dataIndex: 'arguments', width: 300 },
              { title: t('message.toolResult'), dataIndex: 'result' },
            ]"
            :data-source="parseToolCalls(record.toolCalls).map((t, i) => ({ ...t, _key: i }))"
            :pagination="false"
            size="small"
            :row-key="(r: any) => r._key"
          >
            <template #bodyCell="{ column, record: tool }">
              <template v-if="column.dataIndex === 'arguments'">
                <pre class="tool-json">{{ tool.arguments }}</pre>
              </template>
              <template v-else-if="column.dataIndex === 'result'">
                <pre class="tool-json">{{ tool.result }}</pre>
              </template>
            </template>
          </a-table>
        </template>

        <template #bodyCell="{ column, record }">
          <!-- Coluna do remetente -->
          <template v-if="column.dataIndex === 'sender'">
            {{ getSenderText(record.sender) }}
          </template>

          <!-- Coluna do tipo de mensagem -->
          <template v-else-if="column.dataIndex === 'messageType'">
            <template v-if="!!record.toolCalls">
              <a-tooltip placement="topLeft" :mouse-enter-delay="0.5" :overlay-style="{ maxWidth: '400px' }">
                <template #title>
                  <div v-for="(tool, index) in parseToolCalls(record.toolCalls)" :key="index">{{ tool.name }}</div>
                </template>
                <div v-for="(tool, index) in parseToolCalls(record.toolCalls)" :key="index" class="ellipsis-text">{{ tool.name }}</div>
              </a-tooltip>
            </template>
            <span v-else>-</span>
          </template>

          <!-- Coluna de conteúdo da mensagem -->
          <template v-else-if="column.dataIndex === 'message'">
            <a-tooltip :title="record.message" :mouse-enter-delay="0.5" placement="topLeft">
              <span v-if="record.message" class="ellipsis-text">{{ record.message }}</span>
              <span v-else>-</span>
            </a-tooltip>
          </template>

          <!-- Coluna de áudio -->
          <template v-else-if="column.dataIndex === 'audioPath'">
            <div v-if="hasValidAudio(record.audioPath)" class="audio-player-container">
              <AudioPlayer :audio-url="record.audioPath" />
            </div>
            <span v-else>{{ t('message.noAudio') }}</span>
          </template>

          <!-- Coluna de ações -->
          <template v-else-if="column.dataIndex === 'operation'">
            <a-space>
              <TableActionButtons
                :record="record"
                :permissions="{ delete: 'system:role:memory:chat:delete' }"
                :show-delete="record.state !== '0'"
                :delete-title="t('message.confirmDeleteMessage')"
                @delete="() => handleDeleteMessage(record)"
              />
            </a-space>
          </template>
        </template>
      </a-table>

      <!-- Tabela de memória resumida -->
      <a-table
        v-else-if="memoryType === 'summary'"
        row-key="createTime"
        :columns="columns"
        :data-source="data"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 800 }"
        size="middle"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">

          <!-- Coluna do nome do dispositivo (backend não retorna, usa o dispositivo selecionado atualmente) -->
          <template v-if="column.dataIndex === 'deviceName'">
            {{ selectedDeviceName }}
          </template>
          <!-- Coluna de conteúdo do resumo -->
          <template v-if="column.dataIndex === 'summary'">
            <a-tooltip :title="record.summary" :mouse-enter-delay="0.5" placement="topLeft">
              <span v-if="record.summary" class="ellipsis-text">{{ record.summary }}</span>
              <span v-else>-</span>
            </a-tooltip>
          </template>

          <!-- Coluna de ações -->
          <template v-else-if="column.dataIndex === 'operation'">
            <TableActionButtons
              :record="record"
              :permissions="{ delete: 'system:role:memory:summary:delete' }"
              show-delete
              :delete-title="t('common.confirmDelete')"
              @delete="() => handleDeleteMemory(record)"
            />
          </template>
        </template>
      </a-table>

    </a-card>

    <!-- Voltar ao topo -->
    <a-back-top />
  </div>
</template>

<style scoped lang="scss">
.memory-management-view {
  padding: 24px;
}

.search-card :deep(.ant-form-item) {
  margin-bottom: 0;
}

.audio-player-container {
  position: relative;
  width: 100%;
  overflow: hidden;
  z-index: 1;
}

// Estilo de reticências no texto da tabela
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

// Exibição do JSON de chamadas de ferramenta
.tool-json {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  line-height: 1.5;
  max-height: 200px;
  overflow-y: auto;
}
</style>
