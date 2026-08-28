/**
 * Exportação unificada das Stores
 * Facilita o gerenciamento e importação centralizados
 */
export { useUserStore } from './user'
export { useLoadingStore } from './loading'
export { useAppStore } from './app'
export { useDeviceStore } from './device'

// Exportação de tipos
export type { UserInfo, WebSocketConfig } from './user'
export type { Locale } from './app'
