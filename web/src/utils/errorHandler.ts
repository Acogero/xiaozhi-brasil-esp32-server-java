import type { App } from 'vue'
import { message } from 'ant-design-vue'
import { useEventListener } from '@vueuse/core'

// Tipo de erro
interface ErrorInfo {
  message: string
  stack?: string
  componentName?: string
  propsData?: Record<string, unknown>
  url?: string
  line?: number
  column?: number
}

// Coleta de logs de erro
const errorLogs: ErrorInfo[] = []

// Reporta o erro ao servidor (opcional)
function reportError(error: ErrorInfo) {
  // Aqui pode-se chamar a API do backend para reportar o erro
  console.error('Erro reportado:', error)

  // Exemplo: enviar para o backend
  // fetch('/api/error/report', {
  //   method: 'POST',
  //   body: JSON.stringify(error)
  // })
}

// Manipulador de erros do Vue
export function setupErrorHandler(app: App) {
  // 1. Tratamento de erros de componentes Vue
  app.config.errorHandler = (err: unknown, instance, info) => {
    const error = err instanceof Error ? err : new Error(String(err))
    const errorInfo: ErrorInfo = {
      message: error.message || 'Erro desconhecido',
      stack: error.stack,
      componentName: instance?.$options.name || instance?.$options.__name,
      propsData: instance?.$props as Record<string, unknown>,
    }

    // Salva o log de erro
    errorLogs.push(errorInfo)

    // Reporta o erro
    reportError(errorInfo)

    // Exibe o alerta de erro
    message.error({
      content: `Erro de componente: ${errorInfo.message}`,
      duration: 5,
    })

    console.error('Erro do Vue:', err, info)
  }

  if (import.meta.env.DEV) {
    app.config.warnHandler = (msg, instance, trace) => {
      console.warn('Aviso do Vue:', msg, trace)
    }
  }

  useEventListener(window, 'unhandledrejection', (event) => {
    event.preventDefault()

    const reason = event.reason

    if (
      reason?.name === 'CanceledError' ||
      reason?.code === 'ERR_CANCELED' ||
      reason?.code === 'ERR_AUTH_EXPIRED' ||
      reason?.isSilent === true ||
      reason?.isAuthExpired === true ||
      reason?.message?.includes('canceled') ||
      reason?.message?.includes('aborted') ||
      reason?.message?.includes('signal is aborted')
    ) {
      console.debug('Requisição cancelada (comportamento normal):', reason.message)
      return
    }

    // Ignora erros de falha ao carregar arquivo de áudio (404)
    if (
      reason?.message?.includes('Failed to fetch') &&
      reason?.message?.includes('/audio/') &&
      reason?.message?.includes('404')
    ) {
      console.debug('Arquivo de áudio não encontrado (comportamento normal):', reason.message)
      return
    }

    // Ignora falha de decodificação de áudio (áudio muito curto ou problema de formato, já tratado no componente)
    if (reason?.message?.includes('Unable to decode audio data')) {
      return
    }

    // Detecta falha de importação dinâmica (geralmente porque uma nova versão foi implantada e os arquivos antigos foram removidos)
    if (
      reason?.message?.includes('Failed to fetch dynamically imported module') ||
      (reason?.message?.includes('Failed to fetch') && reason?.message?.match(/\.js/))
    ) {
      console.warn('Falha ao carregar módulo dinâmico, a versão da página pode estar desatualizada:', reason.message)

      message.warning({
        content: 'A versão da página foi atualizada, atualizando automaticamente...',
        duration: 2,
        onClose: () => {
          window.location.reload()
        }
      })

      // Atualiza a página automaticamente após 2 segundos
      setTimeout(() => {
        window.location.reload()
      }, 2000)

      return
    }

    const errorInfo: ErrorInfo = {
      message: reason?.message || 'Erro de Promise não tratado',
      stack: reason?.stack,
    }

    errorLogs.push(errorInfo)
    reportError(errorInfo)

    message.error({
      content: `Erro de Promise: ${errorInfo.message}`,
      duration: 5,
    })

    console.error('Erro de Promise não tratado:', reason)
  })

  useEventListener(window, 'error', (event) => {
    if (
      event.message.includes('ResizeObserver loop') ||
      event.message.includes('ResizeObserver loop completed with undelivered notifications')
    ) {
      event.preventDefault()
      return
    }

    const errorInfo: ErrorInfo = {
      message: event.message,
      url: event.filename,
      line: event.lineno,
      column: event.colno,
      stack: event.error?.stack,
    }

    errorLogs.push(errorInfo)
    reportError(errorInfo)

    message.error({
      content: `Erro de script: ${errorInfo.message}`,
      duration: 5,
    })

    console.error('Erro global:', event.error)
  })

  useEventListener(
    window,
    'error',
    (event) => {
      const target = event.target as HTMLElement
      if (target.tagName === 'IMG' || target.tagName === 'SCRIPT' || target.tagName === 'LINK') {
        const resourceUrl = target instanceof HTMLImageElement || target instanceof HTMLScriptElement 
          ? target.src 
          : target instanceof HTMLLinkElement 
          ? target.href 
          : ''
        
        const errorInfo: ErrorInfo = {
          message: `Falha ao carregar recurso: ${resourceUrl}`,
        }

        errorLogs.push(errorInfo)
        reportError(errorInfo)

        console.error('Erro ao carregar recurso:', target)
      }
    },
    { capture: true }
  )
}

// Obtém os logs de erro
export function getErrorLogs() {
  return errorLogs
}

// Limpa os logs de erro
export function clearErrorLogs() {
  errorLogs.length = 0
}
