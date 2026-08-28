/**
 * Composable de tratamento de avatar
 */
import { getResourceUrl } from '@/utils/resource'

export function useAvatar() {
  /**
   * Obtém a URL do avatar
   * Usa a lógica unificada de tratamento de URL de recursos
   */
  function getAvatarUrl(avatar?: string): string | undefined {
    return getResourceUrl(avatar)
  }

  return {
    getAvatarUrl,
  }
}


