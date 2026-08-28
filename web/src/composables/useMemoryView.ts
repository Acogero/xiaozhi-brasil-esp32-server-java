import { useRouter } from 'vue-router'
import { ROUTES } from '@/router/routes'

export interface MemoryViewParams {
  roleId?: number
  deviceId?: string
}

/**
 * Composable de navegação para a visualização de gerenciamento de memória
 * Fornece um método unificado para navegar até a página de gerenciamento de memória
 * Suporta o envio de roleId e, opcionalmente, deviceId
 */
export function useMemoryView() {
  const router = useRouter()

  /**
   * Navega até a página de gerenciamento de memória
   * @param params Parâmetros de consulta: roleId (obrigatório) e deviceId (opcional)
   */
  const navigateToMemory = (params: MemoryViewParams) => {
    const query: Record<string, string> = {}

    if (params.roleId !== undefined) {
      query.roleId = String(params.roleId)
    }

    if (params.deviceId !== undefined) {
      query.deviceId = params.deviceId
    }

    router.push({
      path: ROUTES.MEMORY_CHAT,
      query
    })
  }

  return {
    navigateToMemory
  }
}
