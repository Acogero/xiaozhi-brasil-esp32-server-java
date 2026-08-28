/**
 * Definições de tipos relacionados ao chat Web
 */

/**
 * Unidade de Token da saída em streaming do LLM, distinguindo processo de raciocínio e resposta final
 */
export interface ChatToken {
  type: 'thinking' | 'content'
  text: string
}

/**
 * Mensagem de chat (modelo de visão do frontend)
 */
export interface ChatMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  /** Conteúdo do processo de raciocínio (só tem valor quando assistant e o pensamento profundo está ativado) */
  thinking?: string
  /** Se o pensamento já foi concluído (alterna a UI: pensando... → pensamento concluído) */
  thinkingDone?: boolean
  timestamp: Date
  /** Recebendo em streaming */
  streaming?: boolean
}
