/**
 * Definição de constantes enumeradas
 * Usado para substituir números mágicos, melhorando a legibilidade e a manutenibilidade do código
 */

/**
 * Código de status HTTP
 */
export enum HttpStatus {
  SUCCESS = 200,
  UNAUTHORIZED = 401,
  FORBIDDEN = 403,
  NOT_FOUND = 404,
  SERVER_ERROR = 500,
}

/**
 * Código de resposta da API
 */
export enum ApiCode {
  SUCCESS = 200,
  ERROR = 500,
  UNAUTHORIZED = 401,
  FORBIDDEN = 403,
}

/**
 * Status do usuário
 */
export enum UserState {
  DISABLED = 0, // Desativado
  NORMAL = 1,   // Normal
}

/**
 * Tipo de usuário
 */
export enum UserType {
  NORMAL = 0,   // Usuário comum
  ADMIN = 1,    // Administrador
}

/**
 * Status do dispositivo
 */
export enum DeviceState {
  OFFLINE = 0,  // Offline
  ONLINE = 1,   // Online
}

/**
 * Tipo de mensagem
 */
export enum MessageType {
  TEXT = 'text',
  AUDIO = 'audio',
  IMAGE = 'image',
  SYSTEM = 'system',
}

/**
 * Tipo de remetente da mensagem
 */
export enum SenderType {
  USER = 'user',
  ASSISTANT = 'assistant',
  SYSTEM = 'system',
}

/**
 * Status do WebSocket
 */
export enum WebSocketState {
  CONNECTING = 0, // Conectando
  OPEN = 1,       // Conectado
  CLOSING = 2,    // Fechando
  CLOSED = 3,     // Fechado
}

/**
 * Modo de tema
 */
export enum ThemeMode {
  LIGHT = 'light',
  DARK = 'dark',
  AUTO = 'auto',
}

/**
 * Idioma
 */
export enum Locale {
  ZH_CN = 'zh-CN',
  EN_US = 'en-US',
}

/**
 * Estilo de navegação
 */
export enum NavigationStyle {
  SIDEBAR = 'sidebar',
  TABS = 'tabs',
}

/**
 * Tipo de operação da tabela
 */
export enum TableAction {
  ADD = 'add',
  EDIT = 'edit',
  DELETE = 'delete',
  VIEW = 'view',
}

/**
 * Status de upload de arquivo
 */
export enum UploadStatus {
  READY = 'ready',
  UPLOADING = 'uploading',
  SUCCESS = 'success',
  ERROR = 'error',
}

/**
 * Tipo de função
 */
export enum RoleType {
  CUSTOM = 'custom',
  SYSTEM = 'system',
}

/**
 * Tipo de configuração
 */
export enum ConfigType {
  LLM = 'llm',           // Modelo de linguagem grande
  STT = 'stt',           // Reconhecimento de voz
  TTS = 'tts',           // Síntese de voz
  AGENT = 'agent',       // Agente inteligente
}

/**
 * Indicador de sucesso/falha
 */
export const SUCCESS = true
export const FAILURE = false

/**
 * Indicador de sim/não
 */
export const YES = 1
export const NO = 0

