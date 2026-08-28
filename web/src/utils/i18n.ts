import { i18n } from '@/locales'

/**
 * Obtém a tradução do título da rota
 * @param routeName Nome da rota
 * @returns Título traduzido
 */
export function getRouteTitle(routeName: string): string {
  const routeTitleMap: Record<string, string> = {
    'login': 'router.title.login',
    'register': 'router.title.register',
    'forget': 'router.title.forget',
    'dashboard': 'router.title.dashboard',
    'user': 'router.title.user',
    'device': 'router.title.device',
    'message': 'router.title.message',
    'role': 'router.title.role',
    'auth-role': 'router.title.authRole',
    'template': 'router.title.template',
    'config-model': 'router.title.modelConfig',
    'config-agent': 'router.title.agent',
    'config-stt': 'router.title.sttConfig',
    'config-tts': 'router.title.ttsConfig',
    'setting-account': 'router.title.account',
    'setting-config': 'router.title.personalConfig',
    '403': 'router.title.error403',
    '404': 'router.title.error404',
  }

  const translationKey = routeTitleMap[routeName]
  if (translationKey) {
    return i18n.global.t(translationKey)
  }
  
  // Se não encontrar a tradução correspondente, retorna o nome original
  return routeName
}

/**
 * Obtém a tradução do menu pai
 * @param parentName Nome do menu pai
 * @returns Nome do menu pai traduzido
 */
export function getParentTitle(parentName: string): string {
  const parentTitleMap: Record<string, string> = {
    'Gerenciamento de Personas': 'router.parent.roleManagement',
    'Gerenciamento de Configurações': 'router.parent.configManagement',
    'Configurações': 'router.parent.settings',
  }

  const translationKey = parentTitleMap[parentName]
  if (translationKey) {
    return i18n.global.t(translationKey)
  }
  
  return parentName
}
