/**
 * Utilitário de log unificado
 * Em desenvolvimento, envia saída para o console; em produção, pode ser integrado a um serviço de reporte de logs
 */

const isDev = import.meta.env.DEV

/**
 * Nível de log
 */
export enum LogLevel {
  DEBUG = 'debug',
  INFO = 'info',
  WARN = 'warn',
  ERROR = 'error'
}

/**
 * Configuração de log
 */
interface LoggerConfig {
  /** Se o log está habilitado */
  enabled: boolean
  /** Nível mínimo de log */
  minLevel: LogLevel
  /** Se deve exibir o timestamp */
  showTimestamp: boolean
  /** Se deve exibir o nível do log */
  showLevel: boolean
}

/**
 * Configuração padrão
 */
const defaultConfig: LoggerConfig = {
  enabled: isDev,
  minLevel: isDev ? LogLevel.DEBUG : LogLevel.ERROR,
  showTimestamp: true,
  showLevel: true
}

/**
 * Peso do nível de log (usado para comparação)
 */
const levelWeight: Record<LogLevel, number> = {
  [LogLevel.DEBUG]: 0,
  [LogLevel.INFO]: 1,
  [LogLevel.WARN]: 2,
  [LogLevel.ERROR]: 3
}

/**
 * Formata o timestamp
 */
function formatTimestamp(): string {
  const now = new Date()
  return `[${now.toLocaleTimeString()}]`
}

/**
 * Formata o prefixo do log
 */
function formatPrefix(level: LogLevel, config: LoggerConfig): string {
  const parts: string[] = []
  
  if (config.showTimestamp) {
    parts.push(formatTimestamp())
  }
  
  if (config.showLevel) {
    parts.push(`[${level.toUpperCase()}]`)
  }
  
  return parts.join(' ')
}

/**
 * Determina se o log deve ser exibido
 */
function shouldLog(level: LogLevel, config: LoggerConfig): boolean {
  if (!config.enabled) return false
  return levelWeight[level] >= levelWeight[config.minLevel]
}

/**
 * Classe Logger
 */
class Logger {
  private config: LoggerConfig

  constructor(config: Partial<LoggerConfig> = {}) {
    this.config = { ...defaultConfig, ...config }
  }

  /**
   * Atualiza a configuração
   */
  configure(config: Partial<LoggerConfig>) {
    this.config = { ...this.config, ...config }
  }

  /**
   * Log de depuração
   */
  debug(...args: any[]) {
    if (shouldLog(LogLevel.DEBUG, this.config)) {
      const prefix = formatPrefix(LogLevel.DEBUG, this.config)
      console.log(prefix, ...args)
    }
  }

  /**
   * Log de informação
   */
  info(...args: any[]) {
    if (shouldLog(LogLevel.INFO, this.config)) {
      const prefix = formatPrefix(LogLevel.INFO, this.config)
      console.info(prefix, ...args)
    }
  }

  /**
   * Log de aviso
   */
  warn(...args: any[]) {
    if (shouldLog(LogLevel.WARN, this.config)) {
      const prefix = formatPrefix(LogLevel.WARN, this.config)
      console.warn(prefix, ...args)
    }
  }

  /**
   * Log de erro
   */
  error(...args: any[]) {
    if (shouldLog(LogLevel.ERROR, this.config)) {
      const prefix = formatPrefix(LogLevel.ERROR, this.config)
      console.error(prefix, ...args)
      
      // Em produção, pode-se adicionar aqui a lógica de reporte de erros
      // if (!isDev) {
      //   reportError(args)
      // }
    }
  }

  /**
   * Início do agrupamento de logs
   */
  group(label: string) {
    if (this.config.enabled) {
      console.group(label)
    }
  }

  /**
   * Fim do agrupamento de logs
   */
  groupEnd() {
    if (this.config.enabled) {
      console.groupEnd()
    }
  }

  /**
   * Saída em tabela
   */
  table(data: any) {
    if (this.config.enabled && isDev) {
      console.table(data)
    }
  }

  /**
   * Início da medição de desempenho
   */
  time(label: string) {
    if (this.config.enabled && isDev) {
      console.time(label)
    }
  }

  /**
   * Fim da medição de desempenho
   */
  timeEnd(label: string) {
    if (this.config.enabled && isDev) {
      console.timeEnd(label)
    }
  }
}

/**
 * Instância padrão do logger
 */
export const logger = new Logger()

/**
 * Cria um logger nomeado (usado para módulos específicos)
 */
export function createLogger(moduleName: string, config?: Partial<LoggerConfig>): Logger {
  const moduleLogger = new Logger(config)
  
  // Sobrescreve os métodos, adicionando o prefixo do nome do módulo
  const originalMethods = {
    debug: moduleLogger.debug.bind(moduleLogger),
    info: moduleLogger.info.bind(moduleLogger),
    warn: moduleLogger.warn.bind(moduleLogger),
    error: moduleLogger.error.bind(moduleLogger)
  }
  
  moduleLogger.debug = (...args: any[]) => originalMethods.debug(`[${moduleName}]`, ...args)
  moduleLogger.info = (...args: any[]) => originalMethods.info(`[${moduleName}]`, ...args)
  moduleLogger.warn = (...args: any[]) => originalMethods.warn(`[${moduleName}]`, ...args)
  moduleLogger.error = (...args: any[]) => originalMethods.error(`[${moduleName}]`, ...args)
  
  return moduleLogger
}

/**
 * Métodos de atalho (compatibilidade com código legado)
 */
export const log = logger.debug.bind(logger)
export const info = logger.info.bind(logger)
export const warn = logger.warn.bind(logger)
export const error = logger.error.bind(logger)

export default logger

