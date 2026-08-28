<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, type TablePaginationConfig } from 'ant-design-vue'
import { useTable } from '@/composables/useTable'
import { useExport } from '@/composables/useExport'
import { queryAuthRoles } from '@/services/authRole'
import { useLoadingStore } from '@/store/loading'
import { queryUsers } from '@/services/user'
import { useAvatar } from '@/composables/useAvatar'
import type { AuthRole } from '@/types/authRole'
import type { User, UserQueryParams } from '@/types/user'
import dayjs from 'dayjs'

const { t } = useI18n()
const { getAvatarUrl } = useAvatar()
const { exporting, exportToCSV } = useExport()

// Tabela e paginação
const {
  loading,
  data,
  pagination,
  handleTableChange,
  loadData,
  createDebouncedSearch
} = useTable<User>()

// Loading global
const loadingStore = useLoadingStore()
const authRoleOptions = ref<AuthRole[]>([])

// Formulário de consulta
const queryForm = reactive({
  name: '',
  email: '',
  tel: '',
  authRoleId: undefined as number | undefined,
})

// Configuração dos filtros de consulta
const queryFilters = [
  { label: t('common.name'), key: 'name' as const, placeholder: t('common.name') },
  { label: t('user.email'), key: 'email' as const, placeholder: t('user.email') },
  { label: t('user.phone'), key: 'tel' as const, placeholder: t('user.phone') },
]

// Configuração das colunas da tabela
const columns = computed(() => [
  {
    title: t('common.name'),
    dataIndex: 'name',
    width: 100,
    fixed: 'left',
    align: 'center'
  },
  {
    title: t('common.avatar'),
    dataIndex: 'avatar',
    width: 80,
    fixed: 'left',
    align: 'center',
  },
  {
    title: t('user.email'),
    dataIndex: 'email',
    width: 180,
    align: 'center'
  },
  {
    title: t('user.phone'),
    dataIndex: 'tel',
    width: 150,
    align: 'center'
  },
  {
    title: t('user.deviceCount'),
    dataIndex: 'totalDevice',
    width: 100,
    align: 'center',
  },
  {
    title: t('user.onlineDeviceCount'),
    dataIndex: 'aliveNumber',
    width: 120,
    align: 'center',
  },
  {
    title: t('user.messageCount'),
    dataIndex: 'totalMessage',
    width: 120,
    align: 'center',
  },
  {
    title: t('common.status'),
    dataIndex: 'state',
    width: 80,
    align: 'center',
  },
  {
    title: t('user.accountType'),
    dataIndex: 'isAdmin',
    width: 100,
    align: 'center',
  },
  {
    title: t('user.authRole'),
    dataIndex: 'authRoleName',
    width: 140,
    align: 'center',
  },
  {
    title: t('user.lastLoginTime'),
    dataIndex: 'loginTime',
    width: 150,
    align: 'center',
  },
  {
    title: t('user.lastLoginIp'),
    dataIndex: 'loginIp',
    width: 150,
    align: 'center'
  },
])

// Buscar dados dos usuários
async function fetchData() {
  await loadData((params) => {
    const queryParams: UserQueryParams = {
      pageNo: params.pageNo,
      pageSize: params.pageSize,
    }
    
    if (queryForm.name) queryParams.name = queryForm.name
    if (queryForm.email) queryParams.email = queryForm.email
    if (queryForm.tel) queryParams.tel = queryForm.tel
    if (queryForm.authRoleId !== undefined && queryForm.authRoleId !== null) {
      queryParams.authRoleId = queryForm.authRoleId
    }
    
    return queryUsers(queryParams)
  })
}

async function loadAuthRoleOptions() {
  const res = await queryAuthRoles({ pageNo: 1, pageSize: 100 })
  if (res.code === 200 && res.data?.list) {
    authRoleOptions.value = res.data.list
  }
}

// Busca com debounce
const debouncedSearch = createDebouncedSearch(fetchData, 500)

// Exportar dados dos usuários
async function handleExport() {
  loadingStore.showLoading(t('common.exporting'))
  try {
    // Primeiro, obter todos os dados dos usuários
    const queryParams: UserQueryParams = {
      pageNo: 1,
      pageSize: 100000, // Obter todos os dados
    }
    
    if (queryForm.name) queryParams.name = queryForm.name
    if (queryForm.email) queryParams.email = queryForm.email
    if (queryForm.tel) queryParams.tel = queryForm.tel
    if (queryForm.authRoleId !== undefined && queryForm.authRoleId !== null) {
      queryParams.authRoleId = queryForm.authRoleId
    }
    
    const res = await queryUsers(queryParams)
    
    if (res.code !== 200 || !res.data?.list || res.data.list.length === 0) {
      message.warning(t('export.noData'))
      return
    }
    
    const allData = res.data.list
    
    // Exportar em formato CSV
    await exportToCSV(allData, {
      filename: `users_${dayjs().format('YYYY-MM-DD_HH-mm-ss')}`,
      showLoading: false,
      columns: [
        { key: 'name', title: t('common.name') },
        { key: 'email', title: t('user.email') },
        { key: 'tel', title: t('user.phone') },
        { key: 'totalDevice', title: t('user.deviceCount') },
        { key: 'aliveNumber', title: t('user.onlineDeviceCount') },
        { key: 'totalMessage', title: t('user.messageCount') },
        { 
          key: 'state', 
          title: t('common.status'),
          format: (val) => val == 1 ? t('user.normal') : t('user.disabled')
        },
        { 
          key: 'isAdmin', 
          title: t('user.accountType'),
          format: (val) => val == 1 ? t('user.admin') : t('user.normalUser')
        },
        { key: 'authRoleName', title: t('user.authRole') },
        { key: 'loginTime', title: t('user.lastLoginTime') },
        { key: 'loginIp', title: t('user.lastLoginIp') },
      ]
    })
    message.success(t('common.exportSuccess'))
  } catch (error) {
    console.error('Falha na exportação:', error)
    message.error(t('common.exportFailed'))
  } finally {
    loadingStore.hideLoading()
  }
}

// Obter URL do avatar
function getAvatar(avatar?: string) {
  return getAvatarUrl(avatar)
}

// Tratar mudança de paginação
const onTableChange = (pag: TablePaginationConfig) => {
  handleTableChange(pag)
  fetchData()
}


loadAuthRoleOptions()
fetchData()
</script>

<template>
  <div class="user-view">
    <!-- Formulário de consulta -->
    <a-card :bordered="false" style="margin-bottom: 16px" class="search-card">
      <a-form layout="horizontal" :colon="false">
        <a-row :gutter="16">
          <a-col
            v-for="filter in queryFilters"
            :key="filter.key"
            :xl="6"
            :lg="12"
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
          <a-col :xl="6" :lg="12" :xs="24">
            <a-form-item :label="t('user.authRole')">
              <a-select
                v-model:value="queryForm.authRoleId"
                :placeholder="t('user.authRole')"
                allow-clear
                @change="fetchData"
              >
                <a-select-option
                  v-for="authRole in authRoleOptions"
                  :key="authRole.authRoleId"
                  :value="authRole.authRoleId"
                >
                  {{ authRole.authRoleName }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- Tabela de dados -->
    <a-card :title="t('menu.user')" :bordered="false">
      <template #extra>
        <a-button v-permission="'system:user:export'" type="primary" @click="handleExport" :loading="exporting">
          {{ t('common.export') }}
        </a-button>
      </template>
      
      <a-table
        row-key="userId"
        :columns="columns"
        :data-source="data"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1200 }"
        size="middle"
        @change="onTableChange"
      >
        <!-- Coluna de avatar -->
        <template #bodyCell="{ column, record }">
          <!-- Coluna de nome -->
          <template v-if="column.dataIndex === 'name'">
            <a-tooltip :title="record.name" placement="top">
              <span class="ellipsis-text">{{ record.name }}</span>
            </a-tooltip>
          </template>

          <!-- Coluna de avatar -->
          <template v-else-if="column.dataIndex === 'avatar'">
            <a-avatar :src="getAvatar(record.avatar)" />
          </template>
          
          <!-- Coluna de e-mail -->
          <template v-else-if="column.dataIndex === 'email'">
            <a-tooltip :title="record.email" placement="top">
              <span class="ellipsis-text">{{ record.email || '-' }}</span>
            </a-tooltip>
          </template>
          
          <!-- Coluna de telefone -->
          <template v-else-if="column.dataIndex === 'tel'">
            <a-tooltip :title="record.tel" placement="top">
              <span class="ellipsis-text">{{ record.tel || '-' }}</span>
            </a-tooltip>
          </template>
          
          <!-- Coluna de IP de login -->
          <template v-else-if="column.dataIndex === 'loginIp'">
            <a-tooltip :title="record.loginIp" placement="topRight">
              <span class="ellipsis-text">{{ record.loginIp || '-' }}</span>
            </a-tooltip>
          </template>
          
          <!-- Coluna de status -->
          <template v-else-if="column.dataIndex === 'state'">
            <a-tag v-if="record.state == 1" color="green">{{ t('user.normal') }}</a-tag>
            <a-tag v-else color="red">{{ t('user.disabled') }}</a-tag>
          </template>
          
          <!-- Coluna de tipo de conta -->
          <template v-else-if="column.dataIndex === 'isAdmin'">
            <a-tag v-if="record.isAdmin == 1" color="blue">{{ t('user.admin') }}</a-tag>
            <a-tag v-else>{{ t('user.normalUser') }}</a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'authRoleName'">
            <span>{{ record.authRoleName || '-' }}</span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Voltar ao topo -->
    <a-back-top />
  </div>
</template>

<style scoped lang="scss">
.user-view {
  padding: 24px;
}

.search-card :deep(.ant-form-item) {
  margin-bottom: 0;
}

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
</style>
