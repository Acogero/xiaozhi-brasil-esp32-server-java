<template>
  <a-space :size="size">
    <!-- Slot de ação personalizada -->
    <slot name="actions" :record="record" />
    
    <!-- Botão de editar -->
    <a v-if="showEdit && hasPermission('edit')" @click="handleEdit">
      {{ editText || t('common.edit') }}
    </a>
    
    <!-- Botão de visualizar -->
    <a v-if="showView && hasPermission('view')" @click="handleView">
      {{ viewText || t('common.view') }}
    </a>
    
    <!-- Botão de download -->
    <a v-if="showDownload && hasPermission('download')" @click="handleDownload">
      {{ downloadText || t('common.download') }}
    </a>
    
    <!-- Botão de copiar -->
    <a v-if="showCopy && hasPermission('copy')" @click="handleCopy">
      {{ copyText || t('common.copy') }}
    </a>
    
    <!-- Botão de definir como padrão -->
    <a 
      v-if="showSetDefault && hasPermission('setDefault') && !isDefault" 
      @click="handleSetDefault"
    >
      {{ setDefaultText || t('common.setAsDefault') }}
    </a>
    
    <!-- Divisor (caso haja botão de excluir) -->
    <template v-if="showDelete && hasPermission('delete') && hasAnyVisibleButton">
      <a-divider v-if="showDivider" type="vertical" />
    </template>
    
    <!-- Botão de excluir (com confirmação) -->
    <a-popconfirm
      v-if="showDelete && hasPermission('delete')"
      :title="deleteTitle || t('common.confirmDelete')"
      :ok-text="t('common.confirm')"
      :cancel-text="t('common.cancel')"
      :ok-type="deleteOkType"
      :placement="deletePopconfirmPlacement"
      @confirm="handleDelete"
    >
      <a :class="deleteClass">
        {{ deleteText || t('common.delete') }}
      </a>
    </a-popconfirm>
    
    <!-- Menu suspenso de mais ações -->
    <a-dropdown v-if="moreActions && moreActions.length > 0" :trigger="['click']">
      <a @click.prevent>
        {{ moreText || t('common.more') }}
        <DownOutlined />
      </a>
      <template #overlay>
        <a-menu @click="handleMoreAction">
          <a-menu-item
            v-for="action in visibleMoreActions"
            :key="action.key"
            :disabled="action.disabled"
            :danger="action.danger"
          >
            <component :is="action.icon" v-if="action.icon" style="margin-right: 8px" />
            {{ action.label }}
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>
    
    <!-- Slot de ação extra (por último) -->
    <slot name="extra" :record="record" />
  </a-space>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { DownOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/store/user'
import type { Component } from 'vue'

/**
 * Item de mais ações
 */
export interface MoreAction {
  /** Chave única da ação */
  key: string
  /** Rótulo da ação */
  label: string
  /** Ícone da ação */
  icon?: Component
  /** Se está desabilitado */
  disabled?: boolean
  /** Se é uma ação perigosa */
  danger?: boolean
  /** Se deve exibir */
  visible?: boolean
  /** Identificador de permissão */
  permission?: string | string[]
}

export interface PermissionConfig {
  edit?: boolean | string | string[]
  view?: boolean | string | string[]
  delete?: boolean | string | string[]
  download?: boolean | string | string[]
  copy?: boolean | string | string[]
  setDefault?: boolean | string | string[]
  [key: string]: boolean | string | string[] | undefined
}

export interface Props {
  /** Dados da linha atual */
  record?: any
  /** Prefixo padrão de permissão do botão, por exemplo system:role, system:config:firmware */
  permissionPrefix?: string
  /** Se deve exibir o botão de editar */
  showEdit?: boolean
  /** Se deve exibir o botão de visualizar */
  showView?: boolean
  /** Se deve exibir o botão de excluir */
  showDelete?: boolean
  /** Se deve exibir o botão de download */
  showDownload?: boolean
  /** Se deve exibir o botão de copiar */
  showCopy?: boolean
  /** Se deve exibir o botão de definir como padrão */
  showSetDefault?: boolean
  
  /** Se é o item padrão (usado no botão de definir como padrão) */
  isDefault?: boolean
  
  /** Texto personalizado do botão de editar */
  editText?: string
  /** Texto personalizado do botão de visualizar */
  viewText?: string
  /** Texto personalizado do botão de excluir */
  deleteText?: string
  /** Texto personalizado do botão de download */
  downloadText?: string
  /** Texto personalizado do botão de copiar */
  copyText?: string
  /** Texto personalizado do botão de definir como padrão */
  setDefaultText?: string
  /** Texto personalizado do botão de mais */
  moreText?: string
  
  /** Título de confirmação de exclusão */
  deleteTitle?: string
  /** Classe do botão de excluir */
  deleteClass?: string
  /** Posição da caixa de confirmação de exclusão */
  deletePopconfirmPlacement?: 'top' | 'left' | 'right' | 'bottom' | 'topLeft' | 'topRight' | 'bottomLeft' | 'bottomRight' | 'leftTop' | 'leftBottom' | 'rightTop' | 'rightBottom'
  /** Tipo de confirmação do botão de excluir */
  deleteOkType?: 'default' | 'primary' | 'dashed' | 'link' | 'text' | 'danger'
  
  /** Se deve exibir o divisor */
  showDivider?: boolean
  
  /** Espaçamento entre botões */
  size?: 'small' | 'middle' | 'large' | number
  
  /** Mais ações */
  moreActions?: MoreAction[]
  /** Sobrescrita de permissão especial, tem prioridade sobre a dedução automática de permissionPrefix */
  permissions?: PermissionConfig
}

const props = withDefaults(defineProps<Props>(), {
  showEdit: false,
  showView: false,
  showDelete: false,
  showDownload: false,
  showCopy: false,
  showSetDefault: false,
  isDefault: false,
  deleteClass: 'delete-link',
  deletePopconfirmPlacement: 'topRight',
  deleteOkType: 'danger',
  showDivider: true,
  size: 'small',
  permissions: () => ({})
})

export interface Emits {
  (e: 'edit', record: any): void
  (e: 'view', record: any): void
  (e: 'delete', record: any): void
  (e: 'download', record: any): void
  (e: 'copy', record: any): void
  (e: 'setDefault', record: any): void
  (e: 'more', action: string, record: any): void
}

const emit = defineEmits<Emits>()

const { t } = useI18n()
const userStore = useUserStore()

/**
 * Ações padrão usam a permissão de botão genérica; `view` por padrão é controlado apenas pelo controle de acesso à página;
 * Apenas em poucos cenários especiais a sobrescrita explícita é feita via `permissions`.
 */
const actionMap: Record<string, string> = {
  edit: 'update',
  delete: 'delete',
  download: 'download',
  copy: 'copy',
  setDefault: 'update',
}

const matchPermission = (permission?: boolean | string | string[]): boolean | null => {
  if (typeof permission === 'boolean') {
    return permission
  }
  if (typeof permission === 'string') {
    return userStore.hasPermission(permission)
  }
  if (Array.isArray(permission)) {
    return userStore.hasAnyPermission(permission)
  }
  return null
}

const hasPermission = (action: string): boolean => {
  const configured = matchPermission(props.permissions?.[action])
  if (configured !== null) {
    return configured
  }

  if (action === 'view') {
    return true
  }

  if (props.permissionPrefix) {
    const mappedAction = actionMap[action]
    if (!mappedAction) {
      return true
    }

    return userStore.hasPermission(`${props.permissionPrefix}:${mappedAction}`)
  }

  return true
}

/**
 * Se já existe alguma outra ação padrão visível antes do botão de excluir, usado para controlar o divisor.
 */
const hasAnyVisibleButton = computed(() => {
  return (
    (props.showEdit && hasPermission('edit')) ||
    (props.showView && hasPermission('view')) ||
    (props.showDownload && hasPermission('download')) ||
    (props.showCopy && hasPermission('copy')) ||
    (props.showSetDefault && hasPermission('setDefault') && !props.isDefault)
  )
})

const visibleMoreActions = computed(() => {
  if (!props.moreActions) return []

  return props.moreActions.filter(action => {
    // Verifica a propriedade visible
    if (action.visible === false) return false
    const allowed = matchPermission(action.permission)
    return allowed !== false
  })
})

const handleEdit = () => emit('edit', props.record)
const handleView = () => emit('view', props.record)
const handleDelete = () => emit('delete', props.record)
const handleDownload = () => emit('download', props.record)
const handleCopy = () => emit('copy', props.record)
const handleSetDefault = () => emit('setDefault', props.record)
const handleMoreAction = ({ key }: { key: string }) => emit('more', key, props.record)
</script>

<style scoped lang="scss">
.delete-link {
  color: var(--ant-color-error);

  &:hover {
    color: var(--ant-color-error-hover);
  }
}
</style>
