import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import type { MenuItem, MenuMeta } from '@/types/menu'

/**
 * Composable de gerenciamento de menu
 * Processa a expansão, seleção e filtragem por permissão do menu lateral
 */
export function useMenu() {
  const route = useRoute()
  const router = useRouter()
  const userStore = useUserStore()

  // Keys de menus expandidos (inicializados com base no menu pai da rota atual)
  const initialOpenKey = route.meta?.parent as string | undefined
  const openKeys = ref<string[]>(initialOpenKey ? [initialOpenKey] : [])

  // Sincroniza a expansão do menu pai correspondente quando a rota muda
  watch(
    () => route.meta?.parent,
    (parent) => {
      if (parent && !openKeys.value.includes(parent as string)) {
        openKeys.value = [parent as string]
      }
    }
  )
  
  // Keys de submenus de nível raiz (usados no modo acordeão)
  const rootSubmenuKeys = ['router.parent.roleManagement', 'router.parent.configManagement', 'router.parent.settings', 'router.parent.memoryManagement']

  // Mapeamento de ícones dos menus pai
  const parentIconMap: Record<string, string> = {
    'router.parent.roleManagement': 'UserAddOutlined',
    'router.parent.configManagement': 'SettingOutlined',
    'router.parent.settings': 'SettingOutlined',
    'router.parent.memoryManagement': 'DatabaseOutlined',
  }

  // Obtém todos os itens de menu (a partir da configuração de rotas)
  const menuItems = computed<MenuItem[]>(() => {
    // Encontra a rota do layout principal
    const mainRoute = router.getRoutes().find(r => r.path === '/' && r.children)
    if (!mainRoute?.children) return []
    
    // Mapeamento de menus, usado para organizar a relação pai-filho
    const menuMap = new Map<string, MenuItem>()
    const rootMenus: MenuItem[] = []
    
    // Percorre as rotas para construir o menu
    mainRoute.children
      .filter(route => 
        route.meta?.title && 
        !route.meta?.hideInMenu &&
        // Exclui rotas exibidas apenas no cabeçalho do lado do usuário
        !route.meta?.showInUserHeader
      )
      .forEach(route => {
        const menuItem: MenuItem = {
          path: `/${route.path}`,
          name: route.name as string,
          meta: route.meta as MenuMeta,
          children: []
        }
        
        // Se houver parent, é um submenu
        if (route.meta?.parent) {
          const parentKey = route.meta.parent as string
          
          // Busca ou cria o menu pai
          if (!menuMap.has(parentKey)) {
            const parentMenu: MenuItem = {
              path: parentKey,
              name: parentKey,
              meta: {
                title: parentKey, // Aqui já é uma chave de i18n
                icon: parentIconMap[parentKey] || 'SettingOutlined',
                isAdmin: route.meta.isAdmin
              },
              children: []
            }
            menuMap.set(parentKey, parentMenu)
            rootMenus.push(parentMenu)
          }
          
          // Adiciona ao submenu do menu pai
          const parentMenu = menuMap.get(parentKey)!
          if (!parentMenu.children) parentMenu.children = []
          parentMenu.children.push(menuItem)
        } else {
          // Sem parent, é um menu raiz
          rootMenus.push(menuItem)
          menuMap.set(menuItem.path, menuItem)
        }
      })
    
    return rootMenus
  })

  const { isAdmin } = userStore

  // Menu filtrado (com base na permissão)
  const filteredMenuItems = computed(() => {
    return filterMenuByPermission(menuItems.value)
  })

  // Key do menu atualmente selecionado
  const selectedKeys = computed(() => {
    return [route.path]
  })

  /**
   * Filtra o menu com base na permissão
   */
  function filterMenuByPermission(items: MenuItem[]): MenuItem[] {
    return items.reduce<MenuItem[]>((result, item) => {
      const children = item.children?.length ? filterMenuByPermission(item.children) : []

      if (children.length > 0) {
        result.push({
          ...item,
          children,
        })
        return result
      }

      if (item.meta.isAdmin && !isAdmin) {
        return result
      }
      if (item.meta.permission && !userStore.hasPermission(item.meta.permission)) {
        return result
      }
      if (item.meta.permissions?.length && !userStore.hasAnyPermission(item.meta.permissions)) {
        return result
      }

      result.push({
        ...item,
        children: undefined,
      })
      return result
    }, [])
  }

  /**
   * Processa a mudança de expansão do menu (modo acordeão)
   */
  function handleOpenChange(keys: string[]) {
    const latestOpenKey = keys.find(key => !openKeys.value.includes(key))

    if (latestOpenKey && rootSubmenuKeys.includes(latestOpenKey)) {
      // Ao abrir um novo menu de nível raiz, mantém apenas este (acordeão)
      openKeys.value = [latestOpenKey]
    } else {
      openKeys.value = keys
    }
  }

  /**
   * Processa o clique no menu
   */
  function handleMenuClick(path: string) {
    router.push(path)
  }

  return {
    openKeys,
    selectedKeys,
    menuItems: filteredMenuItems,
    isAdmin,
    handleOpenChange,
    handleMenuClick,
  }
}
