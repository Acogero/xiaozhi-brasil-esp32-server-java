/**
 * Composable de sessão de chat Web
 * Gerencia de forma unificada o estado da sessão, o fluxo de mensagens e o histórico
 */

import { ref, onBeforeUnmount } from 'vue'
import { message as antMessage } from 'ant-design-vue'
import { openChatSession, closeChatSession, chatStream } from '@/services/chat'
import { queryConversations, queryMessages } from '@/services/message'
import type { ChatMessage } from '@/types/chat'
import type { Conversation, Message } from '@/types/message'

export function useChatSession() {
  // Estado da sessão
  const sessionId = ref<string>('')
  const activeSessionId = ref<string>('') // sessão ativa aberta via openChatSession
  const connecting = ref(false)

  // Lista de sessões do histórico
  const conversations = ref<Conversation[]>([])
  const loadingConversations = ref(false)

  // Mensagens do chat
  const messages = ref<ChatMessage[]>([])
  const sending = ref(false)
  const messageIdCounter = ref(0)

  // Estado de expansão/recolhimento da área de raciocínio
  const thinkingExpanded = ref<Record<number, boolean>>({})

  // AbortController da requisição de streaming atual
  let currentAbort: AbortController | null = null

  function toggleThinking(msgId: number) {
    thinkingExpanded.value[msgId] = !thinkingExpanded.value[msgId]
  }

  async function loadConversations() {
    loadingConversations.value = true
    try {
      // Carrega apenas sessões originadas da Web, evitando misturar com conversas do dispositivo (cada conexão gera um novo sessionId)
      const res = await queryConversations({ pageNo: 1, pageSize: 50, source: 'web' })
      conversations.value = res.data.list
    } catch (e: unknown) {
      antMessage.error('Falha ao carregar o histórico de sessões: ' + (e instanceof Error ? e.message : String(e)))
    } finally {
      loadingConversations.value = false
    }
  }

  /**
   * Seleciona uma sessão do histórico e carrega o registro de mensagens
   * @returns Se a troca foi bem-sucedida (retorna false se estiver enviando ou for a mesma sessão)
   */
  async function selectConversation(
    conv: Conversation,
    onAfterLoad?: () => void
  ): Promise<boolean> {
    if (sending.value) {
      antMessage.warning('A conversa atual está em andamento, tente novamente mais tarde')
      return false
    }

    if (sessionId.value === conv.sessionId) return false

    // Encerra a sessão atualmente ativa, se houver
    abortCurrentStream()
    await closeActiveSessionQuietly()

    sessionId.value = conv.sessionId
    messages.value = []

    // Carrega as mensagens do histórico dessa sessão
    try {
      const res = await queryMessages({
        pageNo: 1,
        pageSize: 100,
        sessionId: conv.sessionId,
      })

      // A API retorna em ordem decrescente (ORDER BY createTime DESC), precisamos exibir em ordem crescente
      const historyMsgs: ChatMessage[] = res.data.list.reverse().map((m: Message) => ({
        id: ++messageIdCounter.value,
        role: m.sender === 'user' ? 'user' : 'assistant',
        content: m.message,
        timestamp: m.createTime ? new Date(m.createTime) : new Date(),
      }))

      messages.value = historyMsgs
      onAfterLoad?.()
      return true
    } catch (e: unknown) {
      antMessage.error('Falha ao carregar o registro de mensagens: ' + (e instanceof Error ? e.message : String(e)))
      return false
    }
  }

  async function startNewChat() {
    abortCurrentStream()
    await closeActiveSessionQuietly()
    sessionId.value = ''
    messages.value = []
    sending.value = false
  }

  function abortCurrentStream() {
    if (currentAbort) {
      currentAbort.abort()
      currentAbort = null
    }
  }

  async function closeActiveSessionQuietly() {
    if (activeSessionId.value) {
      try {
        await closeChatSession(activeSessionId.value)
      } catch {
        // Ignora erros ao encerrar
      }
      activeSessionId.value = ''
    }
  }

  /**
   * Envia a mensagem e trata a resposta em streaming
   * @param text Texto inserido pelo usuário
   * @param roleId ID do personagem atualmente selecionado
   * @param onScroll Callback disparado a cada novo conteúdo recebido (geralmente usado para rolar até o final)
   * @returns Se esta rodada abriu uma nova sessão ou deu continuidade a uma existente (em caso positivo, o chamador pode atualizar a lista de histórico)
   */
  async function sendMessage(
    text: string,
    roleId: number,
    onScroll?: () => void
  ): Promise<{ openedNow: boolean; success: boolean }> {
    if (!text || sending.value) return { openedNow: false, success: false }

    // Se não houver sessão ativa, abre uma: se sessionId já tiver valor (continuação após navegar pelo histórico) → é enviado ao backend para dar continuidade; caso contrário, cria uma nova sessão
    let openedNow = false
    if (!activeSessionId.value) {
      connecting.value = true
      try {
        const resp = await openChatSession(roleId, sessionId.value || undefined)
        const data = resp as unknown as { sessionId: string }
        sessionId.value = data.sessionId
        activeSessionId.value = data.sessionId
        openedNow = true
      } catch (e: unknown) {
        antMessage.error('Falha ao estabelecer a sessão: ' + (e instanceof Error ? e.message : String(e)))
        connecting.value = false
        return { openedNow: false, success: false }
      }
      connecting.value = false
    }

    // Adiciona a mensagem do usuário
    messages.value.push({
      id: ++messageIdCounter.value,
      role: 'user',
      content: text,
      timestamp: new Date(),
    })
    onScroll?.()

    // Adiciona a mensagem de placeholder da IA
    messages.value.push({
      id: ++messageIdCounter.value,
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      streaming: true,
    })
    // Obtém o objeto proxy a partir do array reativo, garantindo que alterações subsequentes disparem a atualização da view
    const assistantMsg = messages.value[messages.value.length - 1]!
    onScroll?.()

    sending.value = true
    currentAbort = new AbortController()

    try {
      for await (const token of chatStream(sessionId.value, text, currentAbort.signal)) {
        if (token.type === 'thinking') {
          assistantMsg.thinking = (assistantMsg.thinking || '') + token.text
        } else {
          // Etapa de raciocínio concluída, marca como finalizada
          if (assistantMsg.thinking && !assistantMsg.thinkingDone) {
            assistantMsg.thinkingDone = true
          }
          assistantMsg.content += token.text
        }
        onScroll?.()
      }
      // Garante que thinking seja marcado como done após o fim do streaming
      if (assistantMsg.thinking && !assistantMsg.thinkingDone) {
        assistantMsg.thinkingDone = true
      }
    } catch (e: unknown) {
      if (e instanceof DOMException && e.name === 'AbortError') {
        // Cancelado pelo usuário
      } else {
        assistantMsg.content += '\n\n⚠️ Resposta interrompida: ' + (e instanceof Error ? e.message : String(e))
      }
    } finally {
      assistantMsg.streaming = false
      sending.value = false
      currentAbort = null
      onScroll?.()
    }

    return { openedNow, success: true }
  }

  // Limpeza ao desmontar o componente
  onBeforeUnmount(() => {
    abortCurrentStream()
    if (activeSessionId.value) {
      closeChatSession(activeSessionId.value).catch(() => {})
    }
  })

  return {
    // Estado
    sessionId,
    activeSessionId,
    connecting,
    sending,
    messages,
    conversations,
    loadingConversations,
    thinkingExpanded,
    // Ações
    loadConversations,
    selectConversation,
    startNewChat,
    sendMessage,
    toggleThinking,
  }
}
