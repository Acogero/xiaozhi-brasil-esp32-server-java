/**
 * Utilitário de tratamento de URL de recursos
 */

/**
 * Obtém a URL do recurso (trata caminhos relativos)
 * Referência à implementação do projeto Vue2
 */
export function getResourceUrl(path?: string): string | undefined {
  if (!path) return undefined
  
  // Se já for uma URL completa, retorna diretamente
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path
  }
  
  // Garante que a URL comece com /
  if (!path.startsWith('/')) {
    path = '/' + path
  }

  // Usa o endereço completo do backend (necessário tanto em desenvolvimento quanto em produção)
  const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8091'

  if (backendUrl) {
    // Remove a barra inicial, pois vamos passar a URL completa para o componente
    if (path.startsWith('/')) {
      path = path.substring(1)
    }

    // Constrói a URL completa
    return `${backendUrl}/${path}`
  }

  // Se a URL do backend não estiver configurada, retorna o caminho relativo
  return path
}
