import type { Router } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ROUTES } from '@/router/routes'
import { cancelPendingRequests } from '@/services/request'
import { i18n } from '@/locales'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

// Configurar NProgress
NProgress.configure({ showSpinner: false, speed: 500 })

// Lista branca de rotas que não exigem login
const whiteList: string[] = [ROUTES.LOGIN, ROUTES.REGISTER, ROUTES.FORGET]

export function setupRouterGuards(router: Router) {
  // Guarda de entrada - executado antes da navegação
  router.beforeEach((to, from, next) => {
    // Cancela todas as requisições em andamento da página anterior
    cancelPendingRequests()

    // Inicia a barra de progresso
    NProgress.start()

    // Define o título da página
    const baseTitle = import.meta.env.VITE_APP_TITLE || 'Connect Ai - Plataforma de Gerenciamento de IoT Inteligente'
    if (to.meta.title) {
      // Se for uma chave de tradução, traduz
      const title = to.meta.title.startsWith('router.')
        ? i18n.global.t(to.meta.title)
        : to.meta.title
      document.title = `${title} - ${baseTitle}`
    } else {
      document.title = baseTitle
    }

    const userStore = useUserStore()
    const hasToken = !!userStore.token
    const { isAdmin } = userStore

    // 1. Tratamento para não autenticado
    if (!hasToken) {
      if (whiteList.includes(to.path)) {
        // Está na lista branca, acesso direto
        next()
      } else {
        // Não está na lista branca, redireciona para a página de login
        next(`${ROUTES.LOGIN}?redirect=${to.path}`)
        NProgress.done()
      }
      return
    }

    // 2. Tratamento para já autenticado
    if (to.path === ROUTES.LOGIN) {
      // Se já estiver autenticado, ao acessar a página de login redireciona para a página inicial
      next({ path: ROUTES.DASHBOARD })
      NProgress.done()
      return
    }

    // 2.1 Trata o redirecionamento do caminho raiz (redireciona para páginas iniciais diferentes conforme o tipo de usuário)
    if (to.path === '/') {
      const defaultPath = isAdmin ? ROUTES.DASHBOARD : ROUTES.DEVICE
      next({ path: defaultPath })
      NProgress.done()
      return
    }

    // 3. Verificação de permissão
    if (to.meta.requiresAuth) {
      // Verifica se é necessária permissão de administrador
      if (to.meta.isAdmin && !isAdmin) {
        console.warn(`Usuário sem permissão para acessar: ${to.path}`)
        next(ROUTES.ERROR_403)
        NProgress.done()
        return
      }

      // Verifica permissão específica
      if (to.meta.permission) {
        const hasPermission = userStore.hasPermission(to.meta.permission)
        if (!hasPermission) {
          console.warn(`Usuário sem permissão para acessar: ${to.path}, permissão necessária: ${to.meta.permission}`)
          next(ROUTES.ERROR_403)
          NProgress.done()
          return
        }
      }

      // Verifica múltiplas permissões (basta ter uma)
      if (to.meta.permissions && to.meta.permissions.length > 0) {
        const hasAnyPermission = userStore.hasAnyPermission(to.meta.permissions)
        if (!hasAnyPermission) {
          console.warn(`Usuário sem permissão para acessar: ${to.path}, é necessária uma destas permissões: ${to.meta.permissions.join(', ')}`)
          next(ROUTES.ERROR_403)
          NProgress.done()
          return
        }
      }
    }

    // 4. Libera a navegação
    next()
  })

  // Guarda de saída - executado após a navegação
  router.afterEach(() => {
    // Finaliza a barra de progresso
    NProgress.done()
  })

  // Tratamento de erros
  router.onError((error) => {
    console.error('Erro de rota:', error)
    NProgress.done()

    // Detecta falha na importação dinâmica (falha ao carregar chunk)
    if (
      error.message?.includes('Failed to fetch dynamically imported module') ||
      error.message?.includes('Importing a module script failed') ||
      (error.message?.includes('Failed to fetch') && error.message?.match(/\.js/))
    ) {
      console.warn('Falha ao carregar o módulo de rota, a versão da página pode ter sido atualizada, a página será recarregada')

      // Aguarda um curto período antes de recarregar, evitando o flash causado por um recarregamento imediato
      setTimeout(() => {
        window.location.reload()
      }, 100)
    }
  })
}

// Exemplo de uso:
// Em main.ts:
// import { setupRouterGuards } from './router/guards'
// setupRouterGuards(router)
