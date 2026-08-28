import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { useStorage } from '@vueuse/core'
import type { AuthRole } from '@/types/authRole'

export type { AuthRole } from '@/types/authRole'

export interface UserInfo {
  userId?: string
  username?: string
  email?: string
  name?: string
  tel?: string
  avatar?: string
  state?: string // 1-normal 0-desabilitado
  isAdmin?: string // 1-administrador 0-usuário comum
  totalDevice?: number
  aliveNumber?: number
  totalMessage?: number
  loginTime?: string
  loginIp?: string
  authRoleId?: number
}

// Informações de permissão
export interface Permission {
  permissionId: number
  parentId?: number
  name: string
  permissionKey: string
  permissionType: 'menu' | 'button' | 'api'
  path?: string
  component?: string
  icon?: string
  sort?: number
  visible?: string // '1'-visível '0'-oculto
  status?: string // '1'-habilitado '0'-desabilitado
  children?: Permission[]
}

// Dados de resposta do login
export interface LoginResponse {
  user: UserInfo
  authRole: AuthRole
  permissions: Permission[]
  token: string
  refreshToken: string
  sessionId: string
}

export interface WebSocketConfig {
  url: string
  deviceName?: string
}

export const useUserStore = defineStore('user', () => {
  const userInfo = useStorage<UserInfo | null>('userInfo', null, localStorage, {
    serializer: {
      read: (v: string) => {
        try {
          return v ? JSON.parse(v) as UserInfo : null
        } catch (e) {
          console.error('Failed to parse user info:', e)
          return null
        }
      },
      write: (v: UserInfo | null) => JSON.stringify(v),
    },
  })

  // Informações de permissão
  const permissions = useStorage<Permission[]>('permissions', [], localStorage, {
    serializer: {
      read: (v: any) => {
        try {
          return v ? JSON.parse(v) : []
        } catch (e) {
          console.error('Failed to parse permissions:', e)
          return []
        }
      },
      write: (v: any) => JSON.stringify(v),
    },
  })

  // Informações da função de permissão do backend
  const authRole = useStorage<AuthRole | null>('authRole', null, localStorage, {
    serializer: {
      read: (v: any) => {
        try {
          return v ? JSON.parse(v) : null
        } catch (e) {
          console.error('Failed to parse auth role:', e)
          return null
        }
      },
      write: (v: any) => JSON.stringify(v),
    },
  })

  // Gerenciamento do Token
  const token = useStorage<string>('token', '', localStorage)
  const refreshToken = useStorage<string>('refreshToken', '', localStorage)

  // Gerenciamento de configuração do WebSocket
  const defaultWsConfig: WebSocketConfig = {
    url: import.meta.env.VITE_WS_URL || 'ws://localhost:8091/ws/xiaozhi/v1',
  }
  
  const wsConfig = useStorage<WebSocketConfig>(
    'wsConfig',
    defaultWsConfig,
    localStorage,
    {
      serializer: {
        read: (v: string) => {
          try {
            return v ? JSON.parse(v) as WebSocketConfig : defaultWsConfig
          } catch (e) {
            return defaultWsConfig
          }
        },
        write: (v: WebSocketConfig) => JSON.stringify(v),
      },
    }
  )

  const navigationStyle = useStorage<'tabs' | 'sidebar'>('navigationStyle', 'tabs', localStorage)
  const isMobile = ref(false)

  const setUserInfo = (info: UserInfo) => {
    userInfo.value = info
  }

  const setPermissions = (perms: Permission[]) => {
    permissions.value = perms
  }

  const setAuthRole = (roleInfo: AuthRole) => {
    authRole.value = roleInfo
  }

  const setMobileType = (mobile: boolean) => {
    isMobile.value = mobile
  }

  const setNavigationStyle = (style: 'tabs' | 'sidebar') => {
    navigationStyle.value = style
  }

  const clearUserInfo = () => {
    userInfo.value = null
    permissions.value = []
    authRole.value = null
  }

  const updateUserInfo = (info: Partial<UserInfo>) => {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...info }
    }
  }

  const setToken = (newToken: string) => {
    token.value = newToken
  }

  const setRefreshToken = (newRefreshToken: string) => {
    refreshToken.value = newRefreshToken
  }

  const clearToken = () => {
    token.value = ''
    refreshToken.value = ''
  }

  // Propriedade computada - se é administrador
  const isAdmin = computed(() => userInfo.value?.isAdmin == '1')

  // Método de verificação de permissão
  const hasPermission = (permissionKey: string): boolean => {
    // Administrador possui todas as permissões
    if (isAdmin.value) {
      return true
    }
    return permissions.value.some(perm => perm.permissionKey === permissionKey)
  }

  const hasAnyPermission = (permissionKeys: string[]): boolean => {
    if (isAdmin.value) {
      return true
    }
    return permissionKeys.some(key => hasPermission(key))
  }

  const hasAllPermissions = (permissionKeys: string[]): boolean => {
    if (isAdmin.value) {
      return true
    }
    return permissionKeys.every(key => hasPermission(key))
  }

  const updateWsConfig = (config: Partial<WebSocketConfig>) => {
    wsConfig.value = { ...wsConfig.value, ...config }
  }

  return {
    userInfo,
    permissions,
    authRole,
    token,
    refreshToken,
    wsConfig,
    isMobile,
    navigationStyle,
    isAdmin,
    setUserInfo,
    setPermissions,
    setAuthRole,
    setMobileType,
    setNavigationStyle,
    clearUserInfo,
    updateUserInfo,
    setToken,
    setRefreshToken,
    clearToken,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    updateWsConfig,
  }
})
