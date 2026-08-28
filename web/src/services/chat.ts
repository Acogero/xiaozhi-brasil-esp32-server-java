import { http } from './request'
import { useUserStore } from '@/store/user'
import type { ChatToken } from '@/types/chat'

export type { ChatToken }

const BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

/**
 * Abre uma sessão de chat Web.
 * Quando sessionId não é informado, cria uma nova sessão; quando um sessionId existente é informado, tenta continuá-la (o backend valida a propriedade).
 */
export function openChatSession(roleId: number, sessionId?: string) {
  return http.post<{ sessionId: string }>('/chat/open', null, {
    params: sessionId ? { roleId, sessionId } : { roleId },
  })
}

/**
 * Encerra a sessão de chat Web
 */
export function closeChatSession(sessionId: string) {
  return http.post('/chat/close', null, {
    params: { sessionId },
  })
}

/**
 * Chat em streaming (SSE), retorna um leitor de stream no estilo EventSource.
 * Como o SSE requer o uso do fetch nativo (o axios não suporta leitura em streaming), aqui não se utiliza o wrapper http.
 */
export async function* chatStream(
  sessionId: string,
  text: string,
  signal?: AbortSignal
): AsyncGenerator<ChatToken> {
  const userStore = useUserStore()
  const url = `${BASE_URL}/chat/stream?sessionId=${encodeURIComponent(sessionId)}&text=${encodeURIComponent(text)}`

  const response = await fetch(url, {
    method: 'GET',
    headers: {
      Accept: 'text/event-stream',
      Authorization: userStore.token ? `Bearer ${userStore.token}` : '',
    },
    signal,
  })

  if (!response.ok) {
    throw new Error(`Falha na requisição de chat: ${response.status}`)
  }

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('Não foi possível ler o stream de resposta')
  }

  const decoder = new TextDecoder()
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })

      // Analisa a linha de dados SSE
      const lines = buffer.split('\n')
    buffer = lines.pop() || '' // A última linha pode estar incompleta, deixada para a próxima vez

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (data) {
            try {
              yield JSON.parse(data) as ChatToken
            } catch {
              // Compatibilidade com texto simples (degrada para content)
              yield { type: 'content', text: data } as ChatToken
            }
          }
        }
      }
    }
    // Processa o buffer restante
    if (buffer.startsWith('data:')) {
      const data = buffer.slice(5).trim()
      if (data) {
        try {
          yield JSON.parse(data) as ChatToken
        } catch {
          yield { type: 'content', text: data } as ChatToken
        }
      }
    }
  } finally {
    reader.releaseLock()
  }
}
