/**
 * Constantes relacionadas à API
 */

/**
 * Tempo limite da requisição (milissegundos)
 */
export const REQUEST_TIMEOUT = 30000

/**
 * Configuração padrão de paginação
 */
export const DEFAULT_PAGE_SIZE = 10
export const PAGE_SIZE_OPTIONS = [10, 30, 50, 100, 1000]

/**
 * Upload de arquivos
 */
export const MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
export const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
export const ALLOWED_AUDIO_TYPES = ['audio/mp3', 'audio/wav', 'audio/mpeg']

/**
 * Atraso de debounce (milissegundos)
 */
export const DEBOUNCE_DELAY = 500

/**
 * Configuração de reconexão do WebSocket
 */
export const WS_RECONNECT_DELAY = 3000  // Atraso de reconexão
export const WS_MAX_RECONNECT_TIMES = 5 // Número máximo de tentativas de reconexão

/**
 * Regras de validação de formulário
 */
export const VALIDATION_RULES = {
  // Formato de e-mail
  EMAIL_PATTERN: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
  
  // Formato de telefone celular
  PHONE_PATTERN: /^1[3-9]\d{9}$/,
  
  // Força da senha (mínimo de 8 caracteres, com letras e números)
  PASSWORD_PATTERN: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,}$/,
  
  // Nome de usuário (4 a 20 caracteres: letras, números e underscore)
  USERNAME_PATTERN: /^[a-zA-Z0-9_]{4,20}$/,
}

/**
 * Limite de tamanho da senha
 */
export const PASSWORD_MIN_LENGTH = 8
export const PASSWORD_MAX_LENGTH = 32

/**
 * Limite de tamanho do nome de usuário
 */
export const USERNAME_MIN_LENGTH = 4
export const USERNAME_MAX_LENGTH = 20

/**
 * Limite de tamanho do nome do dispositivo
 */
export const DEVICE_NAME_MAX_LENGTH = 50

/**
 * Limite de tamanho do nome da função
 */
export const ROLE_NAME_MAX_LENGTH = 50

/**
 * Limite de tamanho da descrição
 */
export const DESCRIPTION_MAX_LENGTH = 500

