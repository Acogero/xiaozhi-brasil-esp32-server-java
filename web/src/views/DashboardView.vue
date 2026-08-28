<template>
  <div class="dashboard-view">
    <!-- Informações do usuário e estatísticas básicas -->
    <a-card :bordered="false" class="user-info-card" :loading="loading">
      <div class="user-info-content">
        <a-avatar :src="userAvatar" :size="72" class="user-avatar" />
        <div class="user-greeting">
          <h2>{{ timeFix }}，{{ userInfo?.name || userInfo?.username || 'Usuário' }}，{{ welcomeText }}</h2>
          <a-tooltip :title="t('dashboard.clickToTranslate')" placement="bottomLeft">
            <p class="daily-sentence" @click="sentenceShow = !sentenceShow">
              {{ sentenceShow ? sentence.content : sentence.note }}
            </p>
          </a-tooltip>
        </div>
        <div class="user-statistics">
          <a-statistic
            :title="t('dashboard.stat.messages')"
            :value="stats.messages"
            class="statistic-item"
          />
          <a-statistic
            :title="t('dashboard.stat.devices')"
            :value="stats.devices"
            class="statistic-item"
          />
          <a-statistic
            :title="t('dashboard.stat.roles')"
            :value="stats.roles"
            class="statistic-item"
          />
        </div>
      </div>
    </a-card>

    <!-- Área de conteúdo -->
    <a-row :gutter="[20, 20]" class="content-row">
      <!-- Histórico de conversas -->
      <a-col :xl="14" :lg="12" :xs="24">
        <a-card :title="t('menu.message')" :bordered="false" :loading="loading">
          <div class="chat-messages">
            <div v-if="formattedChatMessages.length === 0" class="empty-messages">
              <a-empty :description="t('dashboard.noData')" />
            </div>
            <div v-else class="message-list">
              <div
                v-for="msg in formattedChatMessages"
                :key="msg.id"
                class="message-item"
                :class="{ 'user-message': msg.isUser }"
              >
                <div class="message-content">
                  <div class="message-text">{{ msg.content }}</div>
                  <div class="message-time">
                    {{ dayjs(msg.timestamp).format('YYYY-MM-DD HH:mm:ss') }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>

      <!-- Lista de dispositivos e guia da comunidade -->
      <a-col :xl="10" :lg="12" :xs="24">
        <a-card :title="t('menu.device')" :bordered="false" :loading="loading" style="margin-bottom: 20px;">
          <a-table
            :columns="columns"
            :data-source="devices"
            :pagination="{ pageSize: 5, showSizeChanger: false }"
            :scroll="{ x: 500 }"
            size="small"
            row-key="deviceId"
          />
        </a-card>
      </a-col>
    </a-row>

    <a-back-top />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/user'
import { useAvatar } from '@/composables/useAvatar'
import { queryDevices } from '@/services/device'
import { queryMessages } from '@/services/message'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
// @ts-ignore
import jsonp from 'jsonp'
import type { Device } from '@/types/device'
import type { Message } from '@/types/message'
import {
  RobotOutlined,
  UserOutlined,
  MessageOutlined,
  ApiOutlined
} from '@ant-design/icons-vue'
import request from '@/services/request'
import api from '@/services/api'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const { getAvatarUrl } = useAvatar()

// Definições de tipo
interface DailySentence {
  content: string
  note: string
}

interface FormattedMessage {
  id: string
  content: string
  type: 'text'
  isUser: boolean
  timestamp: Date
}

// Estado
const loading = ref(true)
const sentenceShow = ref(true)
const sentence = ref<DailySentence>({
  content: 'Cada dia é um novo começo',
  note: 'Every day is a new beginning'
})

const stats = ref({
  devices: 0,
  roles: 0,
  messages: 0,
  users: 0
})

// Lista de dispositivos
const devices = ref<Device[]>([])

// Lista de mensagens
const messages = ref<Message[]>([])

// Definição das colunas da tabela
const columns = computed(() => [
  {
    title: t('device.deviceName'),
    dataIndex: 'deviceName',
    key: 'deviceName',
    width: 120
  },
  {
    title: t('role.roleName'),
    dataIndex: 'roleName',
    key: 'roleName',
    align: 'center' as const,
    width: 120
  },
  {
    title: t('device.onlineStatus'),
    dataIndex: 'state',
    key: 'state',
    align: 'center' as const,
    width: 100
  },
  {
    title: t('device.lastOnlineTime'),
    dataIndex: 'lastLogin',
    key: 'lastLogin',
    align: 'center' as const,
    width: 180,
    sorter: (a: Device, b: Device) => {
      if (!a.lastLogin || !b.lastLogin) return 0
      return dayjs(a.lastLogin).unix() - dayjs(b.lastLogin).unix()
    }
  }
])

// Propriedades computadas
const timeFix = computed(() => {
  const hour = dayjs().hour()
  if (hour < 9) return t('dashboard.greeting.morning')
  if (hour < 12) return t('dashboard.greeting.forenoon')
  if (hour < 14) return t('dashboard.greeting.noon')
  if (hour < 18) return t('dashboard.greeting.afternoon')
  if (hour < 22) return t('dashboard.greeting.evening')
  return t('dashboard.greeting.night')
})

const welcomeText = computed(() => {
  const arr = [
    t('dashboard.welcome.rest'),
    t('dashboard.welcome.eat'),
    t('dashboard.welcome.game'),
    t('dashboard.welcome.tired')
  ]
  const index = Math.floor(Math.random() * arr.length)
  return arr[index]
})

const userInfo = computed(() => userStore.userInfo)

const userAvatar = computed(() => {
  if (userInfo.value?.avatar) {
    return getAvatarUrl(userInfo.value.avatar)
  }
  return '/user-avatar.png'
})

// Formata mensagens de chat
const formattedChatMessages = computed<FormattedMessage[]>(() => {
  return messages.value.map(item => {
    const content = item.sender === 'user' 
      ? `${item.deviceName || 'Usuário'} em ${item.createTime} enviou: ${item.message}`
      : `${item.roleName || 'AI'} em ${item.createTime} respondeu: ${item.message}`
    
    return {
      id: String(item.messageId),
      content,
      type: 'text' as const,
      isUser: item.sender === 'user',
      timestamp: new Date(item.createTime || new Date().toISOString())
    }
  })
})

// Obtém a frase do dia
const getSentence = () => {
  const day = dayjs().format('YYYY-MM-DD')
  jsonp(`https://sentence.iciba.com/index.php?c=dailysentence&m=getdetail&title=${day}`, {
    param: 'callback'
  }, (err: Error | null, data: unknown) => {
    if (err) {
      console.log(t('dashboard.getSentenceFailed'))
    } else {
      const result = data as { content?: string; note?: string } | null
      sentence.value = {
        content: result?.content || sentence.value.content,
        note: result?.note || sentence.value.note
      }
    }
  })
}

// Obtém a lista de dispositivos
const fetchDevices = async () => {
  try {
    const res = await queryDevices({ start: 1, limit: 10 })
    if (res.code === 200) {
      devices.value = res.data.list || []
    } else {
      message.error(res.message || 'Falha ao obter a lista de dispositivos')
    }
  } catch (error) {
    message.error('Falha ao obter a lista de dispositivos')
  }
}

// Obtém a lista de mensagens
const fetchMessages = async () => {
  try {
    const res = await queryMessages({ start: 1, limit: 20 })
    if (res.code === 200) {
      messages.value = res.data.list || []
    } else {
      message.error(res.message || 'Falha ao obter a lista de mensagens')
    }
  } catch (error) {
    message.error('Falha ao obter a lista de mensagens')
  }
}

// Estatísticas totais
async function fetchStats() {
  try {
    const params = { pageNum: 1, pageSize: 1 }
    const [devicesRes, rolesRes, messagesRes] = await Promise.all([
      request.get(api.device.query, { params }),
      request.get(api.role.query, { params }),
      request.get(api.message.query, { params })
    ])
    
    stats.value.devices = devicesRes.data?.total ?? 0
    stats.value.roles = rolesRes.data?.total ?? 0
    stats.value.messages = messagesRes.data?.total ?? 0
  } catch (error) {
    console.error('Failed to fetch dashboard stats', error)
  }
}

onMounted(async () => {
  loading.value = true
  await Promise.all([
    getSentence(),
    fetchDevices(),
    fetchMessages(),
    fetchStats()
  ])
  loading.value = false
})
</script>

<style scoped lang="scss">
.dashboard-view {
  padding: 24px;
  max-width: 1600px;
  margin: 0 auto;
}

// Cartão de informações do usuário
.user-info-card {
  margin-bottom: 20px;
  border-radius: 12px;

  :deep(.ant-card-body) {
    padding: 32px;
  }
}

.user-info-content {
  display: flex;
  gap: 24px;
  align-items: center;
}

.user-avatar {
  flex-shrink: 0;
  border: 3px solid var(--primary-color);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.user-greeting {
  flex: 1;
  min-width: 0;

  h2 {
    font-size: 20px;
    font-weight: 600;
    margin: 0 0 12px 0;
    color: var(--text-color);
    line-height: 1.4;
  }

  .daily-sentence {
    font-size: 14px;
    color: var(--text-secondary);
    margin: 0;
    cursor: pointer;
    transition: color 0.3s;
    line-height: 1.6;

    &:hover {
      color: var(--primary-color);
    }
  }
}

.user-statistics {
  display: flex;
  gap: 32px;
  flex-shrink: 0;

  .statistic-item {
    text-align: right;

    :deep(.ant-statistic-title) {
      font-size: 14px;
      color: var(--text-secondary);
    }

    :deep(.ant-statistic-content) {
      font-size: 24px;
      font-weight: 600;
      color: var(--primary-color);
    }
  }
}

// Área de conteúdo
.content-row {
  margin-bottom: 20px;
}

// Mensagens de chat
.chat-messages {
  min-height: 400px;
  max-height: 500px;
  overflow-y: auto;
  overflow-x: hidden;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.1);
    border-radius: 3px;

    &:hover {
      background: rgba(0, 0, 0, 0.2);
    }
  }
}

.empty-messages {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 8px 0;
}

.message-item {
  display: flex;
  padding: 12px 16px;
  background: var(--bg-secondary);
  border-radius: 8px;
  border-left: 3px solid var(--primary-color);
  transition: all 0.3s;

  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    transform: translateX(4px);
  }

  &.user-message {
    border-left-color: #52c41a;
  }
}

.message-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.message-text {
  font-size: 14px;
  color: var(--text-color);
  line-height: 1.6;
}

.message-time {
  font-size: 12px;
  color: var(--text-secondary);
}

// Responsivo
@media (max-width: 768px) {
  .dashboard-view {
    padding: 16px;
  }

  .user-info-card {
    :deep(.ant-card-body) {
      padding: 20px;
    }
  }

  .user-info-content {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .user-greeting {
    h2 {
      font-size: 18px;
    }
  }

  .user-statistics {
    flex-wrap: wrap;
    justify-content: center;
    gap: 24px;
  }

  .chat-messages {
    min-height: 300px;
    max-height: 400px;
  }
}

// Estilo unificado dos cartões
:deep(.ant-card) {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  .ant-card-head {
    border-bottom: 1px solid var(--border-color);
    
    .ant-card-head-title {
      font-size: 16px;
      font-weight: 600;
    }
  }
}
</style>
