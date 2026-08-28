/**
 * Constantes de caminhos de rota
 * Gerencia de forma centralizada todos os caminhos de rota, evitando strings fixas espalhadas pelo código
 */
export const ROUTES = {
  LOGIN: '/login',
  REGISTER: '/register',
  FORGET: '/forget',
  DASHBOARD: '/dashboard',
  DEVICE: '/device',
  ROLE: '/role',
  TEMPLATE: '/template',
  MEMORY_CHAT: '/memory/chat',
  MEMORY_SUMMARY: '/memory/summary',
  AUTH_ROLE: '/auth-role',
  SETTING_ACCOUNT: '/setting/account',
  SETTING_CONFIG: '/setting/config',
  ERROR_403: '/403',
  ERROR_404: '/404',
  ABOUT: '/about',
} as const
