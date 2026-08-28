/**
 * Funções utilitárias relacionadas a tempo
 */
import dayjs from 'dayjs'

/**
 * Retorna uma saudação com base no horário
 */
export function timeFix(): string {
  const hour = dayjs().hour()
  if (hour < 6) {
    return 'Boa madrugada'
  } else if (hour < 9) {
    return 'Bom dia'
  } else if (hour < 12) {
    return 'Bom dia'
  } else if (hour < 14) {
    return 'Bom almoço'
  } else if (hour < 17) {
    return 'Boa tarde'
  } else if (hour < 19) {
    return 'Boa tardinha'
  } else if (hour < 22) {
    return 'Boa noite'
  } else {
    return 'Boa noite'
  }
}

/**
 * Retorna uma mensagem de boas-vindas
 */
export function welcome(): string {
  const welcomeMessages = [
    'Que você seja feliz todos os dias',
    'Hoje é mais um dia cheio de energia',
    'Que todos os seus desejos se realizem',
    'Mantenha o bom humor',
    'Progrida um pouco a cada dia',
    'Força! Você é o melhor',
  ]
  const index = Math.floor(Math.random() * welcomeMessages.length)
  return welcomeMessages[index] as string
}

/**
 * Formata a data (YYYY-MM-DD)
 */
export function formatDate(date: string | Date): string {
  return dayjs(date).format('YYYY-MM-DD')
}

/**
 * Formata a data e hora (YYYY-MM-DD HH:mm:ss)
 */
export function formatDateTime(date: string | Date): string {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

/**
 * Formata o número (adiciona separador de milhar)
 */
export function formatNumber(num: number): string {
  return num ? num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',') : '0'
}

/**
 * Formata a duração (converte segundos em minutos e segundos)
 */
export function formatDuration(seconds: number): string {
  if (!seconds) return '0s'
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = (seconds % 60).toFixed(1)
  return minutes > 0 ? `${minutes}min${remainingSeconds}s` : `${remainingSeconds}s`
}

