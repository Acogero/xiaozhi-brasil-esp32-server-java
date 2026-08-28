<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { useTable } from '@/composables/useTable'
import { useInlineEdit } from '@/composables/useInlineEdit'
import { useLoadingStore } from '@/store/loading'
import { useMemoryView } from '@/composables/useMemoryView'
import { queryDevices, addDevice, updateDevice, deleteDevice, clearDeviceMemory } from '@/services/device'
import { queryRoles } from '@/services/role'
import DeviceEditDialog from '@/components/DeviceEditDialog.vue'
import TableActionButtons from '@/components/TableActionButtons.vue'
import type { Device, DeviceQueryParams, Role } from '@/types/device'
import type { TablePaginationConfig } from 'ant-design-vue'

const { t } = useI18n()
const loadingStore = useLoadingStore()
const { navigateToMemory } = useMemoryView()

// Tabela e paginação
const {
  loading,
  data,
  pagination,
  handleTableChange,
  loadData,
  createDebouncedSearch
} = useTable<Device>()


// Formulário de consulta
const queryForm = reactive({
  deviceId: '',
  deviceName: '',
  roleName: '',
  state: '',
})

// Configuração dos filtros de consulta
const queryFilters = [
  { label: t('device.deviceId'), key: 'deviceId' as const, placeholder: t('device.deviceId') },
  { label: t('device.deviceName'), key: 'deviceName' as const, placeholder: t('device.deviceName') },
  { label: t('role.roleName'), key: 'roleName' as const, placeholder: t('role.roleName') },
]

// Opções de status do dispositivo
const stateOptions = [
  { label: t('common.all'), value: '' },
  { label: t('device.onlineStatus'), value: '1' },
  { label: t('device.standbyStatus'), value: '2' },
  { label: t('device.offlineStatus'), value: '0' },
]

// Lista de personagens
const roleItems = ref<Role[]>([])

// Usa o composable de edição inline
const {
  editingKey,
  startEdit,
  cancelEdit: cancelEditInline,
  saveEdit,
  updateField
} = useInlineEdit(data, {
  getKey: (item) => item.deviceId,
  onSave: async (item) => {
    loading.value = true
    try {
      const res = await updateDevice(item)
      if (res.code === 200) {
        message.success(t('common.updateSuccess'))
        await fetchData()
        return true
      } else {
        message.error(res.message || t('common.updateFailed'))
        return false
      }
    } catch (error) {
      console.error('Falha ao atualizar dispositivo:', error)
      message.error(t('common.serverMaintenance'))
      return false
    } finally {
      loading.value = false
    }
  }
})

// Relacionado ao modal
const editVisible = ref(false)
const currentDevice = ref<Device | null>(null)
const clearMemoryLoading = ref(false)

// Campo de entrada para adicionar dispositivo
const addDeviceCode = ref('')
const addDeviceLoading = ref(false)

// Configuração das colunas da tabela
const columns = computed(() => [
  {
    title: t('device.deviceId'),
    dataIndex: 'deviceId',
    width: 160,
    fixed: 'left',
    align: 'center'
  },
  {
    title: t('device.deviceName'),
    dataIndex: 'deviceName',
    width: 100,
    align: 'center'
  },
  {
    title: t('role.roleName'),
    dataIndex: 'roleName',
    width: 100,
    align: 'center'
  },
  {
    title: t('device.wifiName'),
    dataIndex: 'wifiName',
    width: 100,
    align: 'center'
  },
  {
    title: t('device.location'),
    dataIndex: 'location',
    width: 180,
    align: 'center'
  },
  {
    title: t('common.status'),
    dataIndex: 'state',
    width: 100,
    align: 'center',
  },
  {
    title: t('device.productType'),
    dataIndex: 'chipModelName',
    width: 100,
    align: 'center'
  },
  {
    title: t('device.deviceType'),
    dataIndex: 'type',
    width: 150,
    align: 'center'
  },
  {
    title: t('device.version'),
    dataIndex: 'version',
    width: 100,
    align: 'center',
  },
  {
    title: t('device.activeTime'),
    dataIndex: 'updateTime',
    width: 180,
    align: 'center',
  },
  {
    title: t('common.createTime'),
    dataIndex: 'createTime',
    width: 180,
    align: 'center',
  },
  {
    title: t('table.action'),
    dataIndex: 'operation',
    width: 250,
    align: 'center',
    fixed: 'right',
  },
])

// Obtém os dados do dispositivo
async function fetchData() {
  // Reseta o estado de edição
  editingKey.value = ''
  
  await loadData((params) => {
    const queryParams: DeviceQueryParams = {
      pageNo: params.pageNo,
      pageSize: params.pageSize,
    }

    if (queryForm.deviceId) queryParams.deviceId = queryForm.deviceId
    if (queryForm.deviceName) queryParams.deviceName = queryForm.deviceName
    if (queryForm.roleName) queryParams.roleName = queryForm.roleName
    if (queryForm.state !== '') queryParams.state = queryForm.state

    return queryDevices(queryParams)
  })
}

// Busca com debounce
const debouncedSearch = createDebouncedSearch(fetchData, 500)

// Obtém a lista de personagens
async function getRoles() {
  try {
    const res = await queryRoles({})
    if (res.code === 200 && res.data) {
      roleItems.value = res.data.list
    }
  } catch (error) {
    console.error('Falha ao obter a lista de personagens:', error)
  }
}

/**
 * Adiciona dispositivo (mantém o loading global)
 */
async function handleAddDevice(code: string) {
  if (!code) {
    message.info(t('device.enterDeviceCode'))
    return
  }

  if (roleItems.value.length === 0) {
    message.warning(t('device.configureDefaultRole'))
    return
  }

  // Debounce: se já está adicionando, retorna diretamente
  if (addDeviceLoading.value) {
    return
  }

  addDeviceLoading.value = true
  loadingStore.showLoading(t('common.adding'))
  try {
    const res = await addDevice(code)
    if (res.code === 200) {
      message.success(t('common.addSuccess'))
      addDeviceCode.value = ''
      await fetchData()
    } else {
      message.error(res.message || t('common.addFailed'))
    }
  } catch (error) {
    console.error('Falha ao adicionar dispositivo:', error)
    message.error(t('common.serverMaintenance'))
  } finally {
    addDeviceLoading.value = false
    loadingStore.hideLoading()
  }
}

/**
 * Remove dispositivo (ação rápida, usa apenas o loading da tabela)
 */
async function handleDeleteDevice(record: Device) {
  loading.value = true
  try {
    const res = await deleteDevice(record.deviceId)
    if (res.code === 200) {
      message.success(t('common.deleteSuccess'))
      await fetchData()
    } else {
      message.error(res.message || t('common.deleteFailed'))
    }
  } catch (error) {
    console.error('Falha ao remover dispositivo:', error)
    message.error(t('common.serverMaintenance'))
  } finally {
    loading.value = false
  }
}

/**
 * Atualiza dispositivo (após edição no modal, usa apenas o loading da tabela)
 */
async function handleUpdate(device: Device) {
  loading.value = true
  try {
    const res = await updateDevice(device)
    if (res.code === 200) {
      message.success(t('common.updateSuccess'))
      editVisible.value = false
      await fetchData()
    } else {
      message.error(res.message || t('common.updateFailed'))
    }
  } catch (error) {
    console.error('Falha ao atualizar dispositivo:', error)
    message.error(t('common.serverMaintenance'))
  } finally {
    loading.value = false
  }
}

/**
 * Limpa a memória do dispositivo (mantém o loading global)
 */
async function handleClearMemory(device: Device) {
  clearMemoryLoading.value = true
  loadingStore.showLoading(t('device.clearingMemory'))
  try {
    const res = await clearDeviceMemory(device.deviceId)
    if (res.code === 200) {
      message.success(t('common.deleteSuccess'))
      editVisible.value = false
      await fetchData()
    } else {
      message.error(res.message || t('common.deleteFailed'))
    }
  } catch (error) {
    console.error('Falha ao limpar memória:', error)
    message.error(t('common.serverMaintenance'))
  } finally {
    clearMemoryLoading.value = false
    loadingStore.hideLoading()
  }
}

/**
 * Edita no modal
 */
function handleEditWithDialog(record: Device) {
  currentDevice.value = { ...record }
  editVisible.value = true
}

// Exporta diretamente os métodos do composable, sem wrapper adicional
const handleEdit = startEdit
const handleCancel = cancelEditInline
const handleSave = saveEdit

/**
 * Edição de entrada - usa composable
 */
function handleInputEdit(value: string, key: string, field: 'deviceName') {
  updateField(key, field, value)
}

/**
 * Alteração da seleção de personagem - usa composable
 */
function handleRoleChange(value: number, key: string) {
  const role = roleItems.value.find((item) => item.roleId === value)
  
  if (role) {
    updateField(key, 'roleId', value)
    updateField(key, 'roleName', role.roleName)
  }
}

/**
 * Obtém o nome do personagem
 */
function getRoleName(roleId?: number) {
  if (!roleId) return ''
  const role = roleItems.value.find((r) => r.roleId === roleId)
  return role ? role.roleName : `ID do Personagem:${roleId}`
}

// Lida com a mudança de paginação
const onTableChange = (pag: TablePaginationConfig) => {
  handleTableChange(pag)
  fetchData()
}

// Inicialização (carregamento não bloqueante)
getRoles()
fetchData()
</script>

<template>
  <div class="device-view">
    <!-- Formulário de consulta -->
    <a-card :bordered="false" style="margin-bottom: 16px" class="search-card">
      <a-form layout="horizontal" :colon="false">
        <a-row :gutter="16">
          <a-col
            v-for="filter in queryFilters"
            :key="filter.key"
            :xl="6"
            :lg="8"
            :md="12"
            :xs="24"
          >
            <a-form-item :label="filter.label">
              <a-input
                v-model:value="queryForm[filter.key]"
                :placeholder="filter.placeholder"
                allow-clear
                @input="debouncedSearch"
              />
            </a-form-item>
          </a-col>

          <a-col :xl="6" :lg="8" :md="12" :xs="24">
            <a-form-item :label="t('common.status')">
              <a-select
                v-model:value="queryForm.state"
                :placeholder="t('common.pleaseSelect')"
                @change="debouncedSearch"
              >
                <a-select-option
                  v-for="item in stateOptions"
                  :key="item.value"
                  :value="item.value"
                >
                  {{ item.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- Tabela de dados -->
    <a-card :bordered="false">
      <template #title>
        <span>{{ t('menu.device') }}</span>
      </template>

      <template #extra>
        <a-input-search
          v-model:value="addDeviceCode"
          v-permission="'system:device:create'"
          :loading="addDeviceLoading"
          :enter-button="t('device.addDevice')"
          :placeholder="t('device.enterDeviceCode')"
          style="width: 300px"
          @search="handleAddDevice"
        />
      </template>

      <a-table
        row-key="deviceId"
        :columns="columns"
        :data-source="data"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1200 }"
        size="middle"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- Coluna de ID do dispositivo -->
          <template v-if="column.dataIndex === 'deviceId'">
            <a-tooltip :title="record.deviceId" placement="topLeft">
              <span class="ellipsis-text">{{ record.deviceId }}</span>
            </a-tooltip>
          </template>

          <!-- Coluna de nome do dispositivo -->
          <template v-else-if="column.dataIndex === 'deviceName'">
            <div>
              <a-input
                v-if="record.editable"
                :value="record.deviceName"
                style="margin: -5px 0; text-align: center"
                @update:value="(val: string) => handleInputEdit(val, record.deviceId, 'deviceName')"
                @press-enter="() => handleUpdate(record)"
                @keyup.esc="() => handleCancel(record.deviceId)"
              />
              <span
                v-else-if="editingKey === ''"
              >
                <a-tooltip :title="record.deviceName || 'Sem nome'" :mouse-enter-delay="0.5">
                  <span v-if="record.deviceName" class="ellipsis-text">{{ record.deviceName }}</span>
                  <span v-else>-</span>
                </a-tooltip>
              </span>
              <span v-else class="ellipsis-text">{{ record.deviceName }}</span>
            </div>
          </template>

          <!-- Coluna de personagem -->
          <template v-else-if="column.dataIndex === 'roleName'">
            <a-select
              v-if="record.editable"
              :value="record.roleId"
              style="margin: -5px 0; width: 100%"
              @change="(val: number) => handleRoleChange(val, record.deviceId)"
            >
              <a-select-option
                v-for="role in roleItems"
                :key="role.roleId"
                :value="role.roleId"
              >
                {{ role.roleName }}
              </a-select-option>
            </a-select>
            <span
              v-else-if="editingKey === ''"
            >
              <a-tooltip
                :title="record.roleDesc || getRoleName(record.roleId)"
                :mouse-enter-delay="0.5"
                placement="top"
              >
                <span v-if="record.roleId" class="ellipsis-text">{{ getRoleName(record.roleId) }}</span>
                <span v-else>-</span>
              </a-tooltip>
            </span>
            <span v-else class="ellipsis-text">{{ record.roleName }}</span>
          </template>

          <!-- Coluna de nome do WiFi -->
          <template v-else-if="column.dataIndex === 'wifiName'">
            <a-tooltip :title="record.wifiName" placement="top">
              <span class="ellipsis-text">{{ record.wifiName || '-' }}</span>
            </a-tooltip>
          </template>

          <!-- Coluna de localização -->
          <template v-else-if="column.dataIndex === 'location'">
            <a-tooltip :title="record.location" placement="top">
              <span class="ellipsis-text">{{ record.location || '-' }}</span>
            </a-tooltip>
          </template>

          <!-- Coluna de tipo de produto -->
          <template v-else-if="column.dataIndex === 'chipModelName'">
            <a-tooltip :title="record.chipModelName" placement="top">
              <span class="ellipsis-text">{{ record.chipModelName || '-' }}</span>
            </a-tooltip>
          </template>

          <!-- Coluna de tipo de dispositivo -->
          <template v-else-if="column.dataIndex === 'type'">
            <a-tooltip :title="record.type" placement="top">
              <span class="ellipsis-text">{{ record.type || '-' }}</span>
            </a-tooltip>
          </template>

          <!-- Coluna de status -->
          <template v-else-if="column.dataIndex === 'state'">
            <a-tag :color="record.state == 1 ? 'green' : record.state == 2 ? 'blue' : 'red'">
              {{ record.state == 1 ? t('device.onlineStatus') : record.state == 2 ? t('device.standbyStatus') : t('device.offlineStatus') }}
            </a-tag>
          </template>

          <!-- Coluna de data/hora -->
          <template v-else-if="column.dataIndex === 'updateTime' || column.dataIndex === 'createTime'">
            {{ record[column.dataIndex] || '-' }}
          </template>

          <!-- Coluna de ações -->
          <template v-else-if="column.dataIndex === 'operation'">
            <a-space v-if="record.editable">
              <a-popconfirm :title="t('common.confirmSave')" @confirm="() => handleUpdate(record)">
                <a>{{ t('common.save') }}</a>
              </a-popconfirm>
              <a @click="() => handleCancel(record.deviceId)">{{ t('common.cancel') }}</a>
            </a-space>
            <TableActionButtons
              v-else
              :record="record"
              permission-prefix="system:device"
              show-edit
              show-view
              show-delete
              :delete-title="t('device.confirmDelete')"
              @edit="() => handleEdit(record.deviceId)"
              @view="() => handleEditWithDialog(record)"
              @delete="() => handleDeleteDevice(record)"
            >
              <template #actions>
                <a
                  v-permission="'system:device:memory'"
                  @click="() => navigateToMemory({ roleId: record.roleId, deviceId: record.deviceId })"
                >
                  {{ t('role.memory') }}
                </a>
              </template>
            </TableActionButtons>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Modal de detalhes do dispositivo -->
    <DeviceEditDialog
      :visible="editVisible"
      :current="currentDevice"
      :role-items="roleItems"
      :clear-memory-loading="clearMemoryLoading"
      @close="editVisible = false"
      @submit="handleUpdate"
      @clear-memory="handleClearMemory"
    />

    <!-- Voltar ao topo -->
    <a-back-top />
  </div>
</template>

<style scoped lang="scss">
.device-view {
  padding: 24px;
}

.search-card :deep(.ant-form-item) {
  margin-bottom: 0;
}

// Centraliza o dropdown na tabela
:deep(.ant-select-selection-item) {
  text-align: center;
}

// Estilo de reticências para texto na tabela
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
</style>
