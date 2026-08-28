import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '../user'
import type { UserInfo, Permission, AuthRole } from '../user'

describe('useUserStore', () => {
  let store: ReturnType<typeof useUserStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useUserStore()
    // Limpa o localStorage
    localStorage.clear()
  })

  describe('Estado inicial', () => {
    it('userInfo é null por padrão', () => {
      expect(store.userInfo).toBeNull()
    })

    it('permissions é um array vazio por padrão', () => {
      expect(store.permissions).toEqual([])
    })

    it('authRole é null por padrão', () => {
      expect(store.authRole).toBeNull()
    })

    it('token é uma string vazia por padrão', () => {
      expect(store.token).toBe('')
    })

    it('isAdmin é false por padrão', () => {
      expect(store.isAdmin).toBe(false)
    })
  })

  describe('setUserInfo / updateUserInfo / clearUserInfo', () => {
    const mockUser: UserInfo = {
      userId: '1',
      username: 'admin',
      name: 'Administrador',
      isAdmin: '1',
    }

    it('define as informações do usuário', () => {
      store.setUserInfo(mockUser)
      expect(store.userInfo).toEqual(mockUser)
    })

    it('atualiza parcialmente as informações do usuário', () => {
      store.setUserInfo(mockUser)
      store.updateUserInfo({ name: 'Novo nome', email: 'new@test.com' })
      expect(store.userInfo?.name).toBe('Novo nome')
      expect(store.userInfo?.email).toBe('new@test.com')
      expect(store.userInfo?.username).toBe('admin') // mantém o campo original
    })

    it('updateUserInfo não faz nada quando userInfo é null', () => {
      store.clearUserInfo() // garante que userInfo seja null
      store.updateUserInfo({ name: 'Novo nome' })
      expect(store.userInfo).toBeNull()
    })

    it('limpa as informações do usuário', () => {
      store.setUserInfo(mockUser)
      store.setPermissions([{ permissionId: 1, name: 'test', permissionKey: 'test', permissionType: 'menu' }])
      store.setAuthRole({ authRoleId: 1, authRoleName: 'Administrador', roleKey: 'admin' })

      store.clearUserInfo()

      expect(store.userInfo).toBeNull()
      expect(store.permissions).toEqual([])
      expect(store.authRole).toBeNull()
    })
  })

  describe('propriedade computada isAdmin', () => {
    it('retorna true para usuário administrador', () => {
      store.setUserInfo({ userId: '1', isAdmin: '1' })
      expect(store.isAdmin).toBe(true)
    })

    it('retorna false para usuário comum', () => {
      store.setUserInfo({ userId: '2', isAdmin: '0' })
      expect(store.isAdmin).toBe(false)
    })

    it('retorna false quando isAdmin não está definido', () => {
      store.setUserInfo({ userId: '3' })
      expect(store.isAdmin).toBe(false)
    })
  })

  describe('verificação de permissões', () => {
    const mockPermissions: Permission[] = [
      { permissionId: 1, name: 'Gerenciamento de dispositivos', permissionKey: 'device:list', permissionType: 'menu' },
      { permissionId: 2, name: 'Adicionar dispositivo', permissionKey: 'device:add', permissionType: 'button' },
      { permissionId: 3, name: 'Gerenciamento de usuários', permissionKey: 'user:list', permissionType: 'menu' },
    ]

    describe('hasPermission', () => {
      it('administrador possui todas as permissões', () => {
        store.setUserInfo({ userId: '1', isAdmin: '1' })
        expect(store.hasPermission('any:permission')).toBe(true)
        expect(store.hasPermission('nonexistent')).toBe(true)
      })

      it('usuário comum verifica permissão específica', () => {
        store.setUserInfo({ userId: '2', isAdmin: '0' })
        store.setPermissions(mockPermissions)

        expect(store.hasPermission('device:list')).toBe(true)
        expect(store.hasPermission('device:add')).toBe(true)
        expect(store.hasPermission('device:delete')).toBe(false)
      })

      it('retorna false quando não há permissão', () => {
        store.setUserInfo({ userId: '2', isAdmin: '0' })
        store.setPermissions([])
        expect(store.hasPermission('device:list')).toBe(false)
      })
    })

    describe('hasAnyPermission', () => {
      it('administrador sempre retorna true', () => {
        store.setUserInfo({ userId: '1', isAdmin: '1' })
        expect(store.hasAnyPermission(['nonexistent'])).toBe(true)
      })

      it('retorna true se possuir ao menos uma permissão', () => {
        store.setUserInfo({ userId: '2', isAdmin: '0' })
        store.setPermissions(mockPermissions)

        expect(store.hasAnyPermission(['device:list', 'device:delete'])).toBe(true)
        expect(store.hasAnyPermission(['device:delete', 'role:list'])).toBe(false)
      })
    })

    describe('hasAllPermissions', () => {
      it('administrador sempre retorna true', () => {
        store.setUserInfo({ userId: '1', isAdmin: '1' })
        expect(store.hasAllPermissions(['a', 'b', 'c'])).toBe(true)
      })

      it('retorna true apenas se possuir todas as permissões', () => {
        store.setUserInfo({ userId: '2', isAdmin: '0' })
        store.setPermissions(mockPermissions)

        expect(store.hasAllPermissions(['device:list', 'device:add'])).toBe(true)
        expect(store.hasAllPermissions(['device:list', 'device:delete'])).toBe(false)
      })
    })
  })

  describe('gerenciamento de Token', () => {
    it('define e limpa o token', () => {
      store.setToken('test-token-123')
      expect(store.token).toBe('test-token-123')

      store.setRefreshToken('refresh-token-456')
      expect(store.refreshToken).toBe('refresh-token-456')

      store.clearToken()
      expect(store.token).toBe('')
      expect(store.refreshToken).toBe('')
    })
  })

  describe('setAuthRole', () => {
    it('define as informações da função de permissão do backend', () => {
      const mockRole: AuthRole = { authRoleId: 1, authRoleName: 'Administrador', roleKey: 'admin' }
      store.setAuthRole(mockRole)
      expect(store.authRole).toEqual(mockRole)
    })
  })

  describe('gerenciamento de estado da UI', () => {
    it('define o estado de dispositivo móvel', () => {
      expect(store.isMobile).toBe(false)
      store.setMobileType(true)
      expect(store.isMobile).toBe(true)
    })

    it('define o estilo de navegação', () => {
      expect(store.navigationStyle).toBe('tabs')
      store.setNavigationStyle('sidebar')
      expect(store.navigationStyle).toBe('sidebar')
    })
  })

  describe('configuração do WebSocket', () => {
    it('atualiza a configuração do WebSocket', () => {
      store.updateWsConfig({ deviceName: 'test-device' })
      expect(store.wsConfig.deviceName).toBe('test-device')
      // url deve manter o valor padrão
      expect(store.wsConfig.url).toBeTruthy()
    })
  })
})
