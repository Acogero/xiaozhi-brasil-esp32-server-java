/**
 * Funções utilitárias de data e hora
 */

/**
 * Formata a data para uma string de data local
 * @param dateString - String de data no formato ISO
 * @param defaultValue - Valor padrão
 * @returns String de data formatada
 */
export function formatDate(dateString?: string, defaultValue: string = '-'): string {
  if (!dateString) return defaultValue
  try {
    return new Date(dateString).toLocaleDateString()
  } catch (error) {
    console.error('Falha ao formatar data:', error)
    return defaultValue
  }
}

/**
 * Formata a data e hora para uma string de data e hora local
 * @param dateString - String de data no formato ISO
 * @param defaultValue - Valor padrão
 * @returns String de data e hora formatada
 */
export function formatDateTime(dateString?: string, defaultValue: string = '-'): string {
  if (!dateString) return defaultValue
  try {
    return new Date(dateString).toLocaleString()
  } catch (error) {
    console.error('Falha ao formatar data e hora:', error)
    return defaultValue
  }
}

/**
 * Obtém a descrição de tempo relativo (ex: agora mesmo, há 5 minutos, há 1 hora)
 * @param dateString - String de data no formato ISO
 * @returns Descrição de tempo relativo
 */
export function getRelativeTime(dateString?: string): string {
  if (!dateString) return '-'
  
  try {
    const now = Date.now()
    const date = new Date(dateString).getTime()
    const diff = now - date
    
    const seconds = Math.floor(diff / 1000)
    const minutes = Math.floor(seconds / 60)
    const hours = Math.floor(minutes / 60)
    const days = Math.floor(hours / 24)
    
    if (seconds < 60) return 'agora mesmo'
    if (minutes < 60) return `há ${minutes} minutos`
    if (hours < 24) return `há ${hours} horas`
    if (days < 7) return `há ${days} dias`
    
    return formatDate(dateString)
  } catch (error) {
    console.error('Falha ao calcular tempo relativo:', error)
    return '-'
  }
}

