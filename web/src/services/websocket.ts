// Serviço WebSocket - Versão Vue3 TypeScript

// =============================
// Definições de tipo
// =============================

export interface WebSocketConfig {
  url: string
  deviceId?: string
  macAddress?: string
  deviceName?: string
  token?: string
}

export interface WebSocketMessage {
  type: 'stt' | 'tts' | 'listen' | 'audio' | 'system'
  state?: 'start' | 'stop' | 'text' | 'sentence_start'
  text?: string
  session_id?: string
  [key: string]: unknown
}

export interface ChatMessage {
  id: string
  content: string
  type: 'text' | 'audio' | 'stt' | 'tts' | 'system'
  isUser: boolean
  timestamp: Date
  isLoading?: boolean
  duration?: string
  audioData?: ArrayBuffer | Blob
}

export interface ConnectionStatus {
  isConnected: boolean
  connectionStatus: string
  connectionTime: Date | null
  sessionId: string | null
}

// =============================
// Variáveis de estado
// =============================

let webSocket: WebSocket | null = null
let isConnecting = false
let reconnectTimer: number | null = null
let reconnectAttempts = 0
const maxReconnectAttempts = 5
const reconnectDelay = 2000

// Relacionado ao efeito de máquina de escrever
let typewriterTimer: number | null = null
let typewriterQueue: string[] = [] // Fila de textos aguardando digitação
let isTyping = false // Indica se está digitando no momento
const TYPING_SPEED = 50 // Intervalo de exibição entre caracteres (milissegundos)

// Status de conexão
const connectionStatus: ConnectionStatus = {
  isConnected: false,
  connectionStatus: 'Desconectado',
  connectionTime: null,
  sessionId: null
}

import { reactive } from 'vue'

// Lista de mensagens - utiliza array reativo
export const messages: ChatMessage[] = reactive([])

// Mensagem de resposta da IA em construção no momento
let currentAIMessage: ChatMessage | null = null

// Funções de callback
type MessageHandler = (data: WebSocketMessage) => void
type StatusChangeHandler = (status: ConnectionStatus) => void
type BinaryHandler = (data: ArrayBuffer) => void

const messageHandlers: Set<MessageHandler> = new Set()
const statusChangeCallbacks: Set<StatusChangeHandler> = new Set()
let binaryHandler: BinaryHandler | null = null

// =============================
// Gerenciamento de logs
// =============================

type LogLevel = 'debug' | 'info' | 'success' | 'warning' | 'error'

interface LogEntry {
  message: string
  type: LogLevel
  time: Date
}

const LOG_LEVELS: Record<LogLevel, number> = {
  debug: 0,
  info: 1,
  success: 2,
  warning: 3,
  error: 4
}

let currentLogLevel = LOG_LEVELS.debug
let logHistory: LogEntry[] = []
const MAX_LOG_HISTORY = 500

export function log(message: string, type: LogLevel = 'info'): LogEntry {
  if (LOG_LEVELS[type] < currentLogLevel) {
    return { message, type, time: new Date() }
  }

  const entry: LogEntry = {
    message,
    type,
    time: new Date()
  }

  logHistory.push(entry)

  if (logHistory.length > MAX_LOG_HISTORY) {
    logHistory = logHistory.slice(-MAX_LOG_HISTORY)
  }

  switch (type) {
    case 'error':
      console.error(message)
      break
    case 'warning':
      console.warn(message)
      break
    case 'success':
      console.log('%c' + message, 'color: green')
      break
    case 'debug':
      console.debug(message)
      break
    default:
      console.log(message)
  }

  return entry
}

export function getLogs(): LogEntry[] {
  return [...logHistory]
}

export function clearLogs(): boolean {
  logHistory = []
  return true
}

export function setLogLevel(level: LogLevel): boolean {
  if (LOG_LEVELS[level] !== undefined) {
    currentLogLevel = LOG_LEVELS[level]
    return true
  }
  return false
}

// =============================
// Gerenciamento de mensagens
// =============================

export function addMessage(message: Partial<ChatMessage>): ChatMessage | null {
  if (!message.content) return null

  const newMessage: ChatMessage = {
    id: message.id || `msg_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`,
    content: String(message.content).trim(),
    type: message.type || 'text',
    isUser: !!message.isUser,
    timestamp: message.timestamp || new Date(),
    isLoading: !!message.isLoading
  }

  messages.push(newMessage)

  log(
    `Adicionando mensagem de ${newMessage.isUser ? 'usuário' : 'IA'}: ${newMessage.content.substring(0, 50)}${
      newMessage.content.length > 50 ? '...' : ''
    }`,
    'debug'
  )

  return newMessage
}

export function clearMessages(): boolean {
  messages.splice(0, messages.length)
  currentAIMessage = null // Reseta a mensagem da IA atual
  log('Todas as mensagens foram limpas', 'info')
  return true
}

// =============================
// Gerenciamento de callbacks
// =============================

export function registerMessageHandler(handler: MessageHandler): boolean {
  if (typeof handler === 'function') {
    messageHandlers.add(handler)
    return true
  }
  return false
}

export function unregisterMessageHandler(handler: MessageHandler): boolean {
  return messageHandlers.delete(handler)
}

export function registerStatusChangeCallback(callback: StatusChangeHandler): boolean {
  if (typeof callback === 'function') {
    statusChangeCallbacks.add(callback)
    return true
  }
  return false
}

export function unregisterStatusChangeCallback(callback: StatusChangeHandler): boolean {
  return statusChangeCallbacks.delete(callback)
}

export function registerBinaryHandler(handler: BinaryHandler): void {
  binaryHandler = handler
  log('✅ Função de tratamento de mensagens binárias registrada', 'info')
}

// Notifica alteração de status
function notifyStatusChange(): void {
  const status = { ...connectionStatus }

  statusChangeCallbacks.forEach(callback => {
    try {
      callback(status)
    } catch (error) {
      log(`Erro ao executar callback de alteração de status: ${error}`, 'error')
    }
  })
}

// =============================
// Conexão WebSocket
// =============================

export async function connectToServer(config: WebSocketConfig): Promise<boolean> {
  if (webSocket && webSocket.readyState === WebSocket.OPEN) {
    log('WebSocket já está conectado', 'info')
    return true
  }

  if (isConnecting) {
    log('WebSocket está conectando...', 'info')
    return false
  }

  try {
    isConnecting = true
    connectionStatus.connectionStatus = 'Conectando...'
    connectionStatus.isConnected = false

    // Limpa o temporizador de reconexão anterior
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }

    // Fecha a conexão existente
    if (webSocket) {
      try {
        webSocket.close()
      } catch (e) {
        // Ignora erro de fechamento
      }
    }

    // Monta a URL de conexão
    let url = config.url
    if (!url.endsWith('/')) {
      url += '/'
    }

    // Adiciona parâmetros de consulta
    const params = new URLSearchParams()
    if (config.deviceId) {
      params.append('device-id', config.deviceId)
    }
    if (config.macAddress || config.deviceName) {
      params.append('mac_address', config.deviceName || config.macAddress || '')
    }
    if (config.token) {
      params.append('token', config.token)
    }

    const queryString = params.toString()
    if (queryString) {
      url += '?' + queryString
    }

    log(`Conectando a: ${url}`, 'info')

    // Cria a conexão WebSocket
    webSocket = new WebSocket(url)
    webSocket.binaryType = 'arraybuffer'

    // Evento de abertura da conexão
    webSocket.onopen = () => {
      isConnecting = false
      connectionStatus.isConnected = true
      connectionStatus.connectionStatus = 'Conectado'
      connectionStatus.connectionTime = new Date()
      reconnectAttempts = 0
      log('Conexão WebSocket estabelecida', 'success')
      notifyStatusChange()
    }

    // Evento de recebimento de mensagem
    webSocket.onmessage = (event) => {
      handleWebSocketMessage(event)
    }

    // Evento de fechamento da conexão
    webSocket.onclose = (event) => {
      isConnecting = false
      connectionStatus.isConnected = false

      if (event.wasClean) {
        connectionStatus.connectionStatus = 'Desconectado'
        log(`Conexão WebSocket fechada: código=${event.code}, motivo=${event.reason}`, 'info')
      } else {
        connectionStatus.connectionStatus = 'Conexão desconectada'
        log('Conexão WebSocket desconectada inesperadamente', 'error')
        scheduleReconnect(config)
      }

      notifyStatusChange()
    }

    // Evento de erro de conexão
    webSocket.onerror = () => {
      isConnecting = false
      connectionStatus.isConnected = false
      connectionStatus.connectionStatus = 'Erro de conexão'
      log('Erro na conexão WebSocket', 'error')
      notifyStatusChange()
    }

    // Aguarda a conclusão da conexão ou timeout
    return new Promise((resolve) => {
      const timeoutId = setTimeout(() => {
        if (!connectionStatus.isConnected) {
          log('Tempo de conexão WebSocket esgotado', 'error')
          isConnecting = false
          connectionStatus.connectionStatus = 'Tempo de conexão esgotado'

          try {
            webSocket?.close()
          } catch (e) {
            // Ignora erro de fechamento
          }

          resolve(false)
        }
      }, 5000)

      const checkConnected = () => {
        if (connectionStatus.isConnected) {
          clearTimeout(timeoutId)
          resolve(true)
        } else if (
          connectionStatus.connectionStatus.includes('Erro') ||
          connectionStatus.connectionStatus.includes('esgotado') ||
          connectionStatus.connectionStatus.includes('Falha')
        ) {
          clearTimeout(timeoutId)
          resolve(false)
        } else {
          setTimeout(checkConnected, 100)
        }
      }

      checkConnected()
    })
  } catch (error) {
    isConnecting = false
    connectionStatus.isConnected = false
    connectionStatus.connectionStatus = 'Falha na conexão'
    log(`Falha na conexão WebSocket: ${error}`, 'error')
    notifyStatusChange()
    return false
  }
}

// Agenda a reconexão
function scheduleReconnect(config: WebSocketConfig): void {
  if (reconnectAttempts >= maxReconnectAttempts) {
    log(`Número máximo de tentativas de reconexão atingido (${maxReconnectAttempts}), interrompendo a reconexão`, 'warning')
    connectionStatus.connectionStatus = 'Falha na reconexão'
    notifyStatusChange()
    return
  }

  const delay = reconnectDelay * Math.pow(1.5, reconnectAttempts)

  log(
    `Reconexão programada para daqui a ${delay / 1000} segundos (tentativa ${reconnectAttempts + 1}/${maxReconnectAttempts})`,
    'info'
  )
  connectionStatus.connectionStatus = `${Math.ceil(delay / 1000)}s até a reconexão...`
  notifyStatusChange()

  reconnectTimer = window.setTimeout(() => {
    reconnectAttempts++
    connectToServer(config)
  }, delay)
}

// Processa mensagens WebSocket
function handleWebSocketMessage(event: MessageEvent): void {
  try {
    // Verifica detalhadamente o tipo da mensagem
    log(`📨 Mensagem WebSocket recebida, tipo: ${typeof event.data}, construtor: ${event.data.constructor.name}`, 'debug')
    
    // Verifica se são dados binários
    if (event.data instanceof ArrayBuffer) {
      log(`🔢 Dados binários recebidos: ${event.data.byteLength} bytes`, 'info')
      if (binaryHandler) {
        log('✅ Chamando a função de tratamento de dados binários', 'debug')
        binaryHandler(event.data)
      } else {
        log('❌ Nenhuma função de tratamento de mensagens binárias registrada', 'warning')
      }
      return
    }

    // Verifica se são dados Blob
    if (event.data instanceof Blob) {
      log(`🔢 Dados Blob recebidos: ${event.data.size} bytes`, 'info')
      event.data.arrayBuffer().then(buffer => {
        if (binaryHandler) {
          log('✅ Chamando a função de tratamento de dados binários (Blob convertido para ArrayBuffer)', 'debug')
          binaryHandler(buffer)
        } else {
          log('❌ Nenhuma função de tratamento de mensagens binárias registrada', 'warning')
        }
      })
      return
    }

    // Processa dados de texto
    log(`📝 Mensagem de texto recebida: ${event.data.substring(0, 100)}...`, 'debug')
    const data: WebSocketMessage = JSON.parse(event.data)

    // Registra o ID da sessão
    if (data.session_id && !connectionStatus.sessionId) {
      connectionStatus.sessionId = data.session_id
      log(`ID da sessão: ${connectionStatus.sessionId}`, 'info')
      notifyStatusChange()
    }

    // Processa de acordo com o tipo de mensagem
    switch (data.type) {
      case 'stt':
        handleSTTMessage(data)
        break
      case 'tts':
        handleTTSMessage(data)
        break
      default:
        log(`Mensagem de tipo desconhecido recebida: ${data.type}`, 'warning')
    }

    // Chama todas as funções de tratamento de mensagens registradas
    messageHandlers.forEach(handler => {
      try {
        handler(data)
      } catch (error) {
        log(`Erro ao executar a função de tratamento de mensagens: ${error}`, 'error')
      }
    })
  } catch (error) {
    log(`Erro ao processar mensagem WebSocket: ${error}`, 'error')
  }
}

// Processa mensagens STT (reconhecimento de voz)
function handleSTTMessage(data: WebSocketMessage): void {
  if (data.text) {
    addMessage({
      content: data.text,
      type: 'stt',
      isUser: true
    })
    log(`Resultado do reconhecimento de voz: ${data.text}`, 'info')
  }
}

// Efeito de máquina de escrever: exibe o texto caractere por caractere
function startTypewriter(text: string): void {
  // Adiciona o texto à fila
  typewriterQueue.push(text)
  
  // Se não estiver digitando, inicia a máquina de escrever
  if (!isTyping) {
    processTypewriterQueue()
  }
}

// Processa a fila da máquina de escrever
function processTypewriterQueue(): void {
  if (typewriterQueue.length === 0) {
    isTyping = false
    return
  }
  
  isTyping = true
  const text = typewriterQueue.shift()!
  const chars = Array.from(text) // Suporta emoji e caracteres multibyte
  let currentIndex = 0
  
  // Se for a primeira digitação, cria a mensagem
  if (!currentAIMessage) {
    currentAIMessage = {
      id: `msg_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`,
      content: '',
      type: 'tts',
      isUser: false,
      timestamp: new Date(),
      isLoading: false
    }
    messages.push(currentAIMessage)
    log(`📝 Nova mensagem de resposta da IA criada (ID: ${currentAIMessage.id})`, 'info')
  }
  
  // Adiciona caractere por caractere
  const typeNextChar = () => {
    if (currentIndex < chars.length) {
      currentAIMessage!.content += chars[currentIndex]
      currentIndex++
      
      // Força o acionamento da atualização reativa
      const index = messages.findIndex(msg => msg.id === currentAIMessage!.id)
      if (index !== -1) {
        messages[index] = { ...currentAIMessage! }
      }
      
      typewriterTimer = window.setTimeout(typeNextChar, TYPING_SPEED)
    } else {
      // Texto atual concluído, processando o próximo
      log(`✅ Digitação concluída: "${text}"`, 'debug')
      processTypewriterQueue()
    }
  }
  
  typeNextChar()
}

// Interrompe o efeito de máquina de escrever
function stopTypewriter(): void {
  if (typewriterTimer) {
    clearTimeout(typewriterTimer)
    typewriterTimer = null
  }
  isTyping = false
  typewriterQueue = []
}

// Processa mensagens TTS (texto para voz)
function handleTTSMessage(data: WebSocketMessage): void {
  if (data.state === 'start') {
    log('🎵 TTS iniciado, preparando para receber áudio', 'info')
    
    // Reinicia a máquina de escrever e a mensagem da IA atual
    stopTypewriter()
    currentAIMessage = null
    
    // Notifica o serviço de áudio para se preparar a receber um novo stream de áudio
    if (window.dispatchEvent) {
      window.dispatchEvent(new CustomEvent('audio-stream-start'))
    }
  } else if (data.state === 'sentence_start' && data.text) {
    // Adiciona a nova frase à fila da máquina de escrever
    log(`📥 Nova frase recebida: "${data.text}"`, 'info')
    startTypewriter(data.text)
  } else if (data.state === 'stop') {
    log('🛑 TTS finalizado, stream de áudio encerrado', 'info')
    
    // Aguarda a conclusão da máquina de escrever antes de limpar (aguarda no máximo 10 segundos)
    let waitCount = 0
    const maxWait = 100 // 100 * 100ms = 10s
    const waitForTyping = () => {
      if (!isTyping && typewriterQueue.length === 0 || waitCount >= maxWait) {
        if (currentAIMessage) {
          log(`✅ Resposta da IA concluída, conteúdo final: "${currentAIMessage.content}"`, 'info')
          currentAIMessage = null
        }
      } else {
        waitCount++
        setTimeout(waitForTyping, 100)
      }
    }
    waitForTyping()
    
    // Notifica o serviço de áudio que o stream terminou
    if (window.dispatchEvent) {
      window.dispatchEvent(new CustomEvent('audio-stream-end'))
    }
  }
}

// =============================
// Envio de mensagens
// =============================

function sendJsonMessage(data: Record<string, unknown>): boolean {
  if (!webSocket || webSocket.readyState !== WebSocket.OPEN) {
    log('WebSocket não conectado, não é possível enviar a mensagem', 'error')
    return false
  }

  try {
    const message = JSON.stringify(data)
    webSocket.send(message)
    return true
  } catch (error) {
    log(`Falha ao enviar mensagem JSON: ${error}`, 'error')
    return false
  }
}

export function sendTextMessage(text: string): boolean {
  if (!text || !webSocket || webSocket.readyState !== WebSocket.OPEN) {
    return false
  }

  try {
    const message = {
      type: 'listen',
      state: 'text',
      text: text
    }

    return sendJsonMessage(message)
  } catch (error) {
    log(`Falha ao enviar mensagem de texto: ${error}`, 'error')
    return false
  }
}

export async function startDirectRecording(): Promise<boolean> {
  if (!webSocket || webSocket.readyState !== WebSocket.OPEN) {
    throw new Error('WebSocket não conectado')
  }

  try {
    const startMessage = {
      type: 'stt',
      state: 'start'
    }

    sendJsonMessage(startMessage)
    log('Comando de início de gravação enviado', 'info')

    return true
  } catch (error) {
    log(`Falha ao iniciar a gravação: ${error}`, 'error')
    throw error
  }
}

export async function stopDirectRecording(): Promise<boolean> {
  if (!webSocket || webSocket.readyState !== WebSocket.OPEN) {
    throw new Error('WebSocket não conectado')
  }

  try {
    const stopMessage = {
      type: 'stt',
      state: 'stop'
    }

    sendJsonMessage(stopMessage)
    log('Comando de parada de gravação enviado', 'info')

    return true
  } catch (error) {
    log(`Falha ao parar a gravação: ${error}`, 'error')
    throw error
  }
}

// =============================
// Controle de conexão
// =============================

export async function reconnectToServer(config: WebSocketConfig): Promise<boolean> {
  try {
    log('Reconexão manual acionada...', 'info')

    await disconnectFromServer()

    reconnectAttempts = 0

    return await connectToServer(config)
  } catch (error) {
    log(`Falha na reconexão manual: ${error}`, 'error')
    connectionStatus.connectionStatus = 'Falha na reconexão'
    notifyStatusChange()
    return false
  }
}

export function stopAutoReconnect(): boolean {
  try {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
      log('Reconexão automática interrompida', 'info')
    }

    reconnectAttempts = 0

    if (connectionStatus.connectionStatus.includes('reconexão')) {
      connectionStatus.connectionStatus = 'Reconexão interrompida'
      notifyStatusChange()
    }

    return true
  } catch (error) {
    log(`Falha ao interromper a reconexão automática: ${error}`, 'error')
    return false
  }
}

export function disconnectFromServer(): boolean {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  
  // Interrompe o efeito de máquina de escrever
  stopTypewriter()
  currentAIMessage = null

  if (!webSocket) {
    connectionStatus.sessionId = null
    return true
  }

  try {
    if (webSocket.readyState === WebSocket.OPEN) {
      webSocket.close(1000, 'Desconexão iniciada pelo usuário')
    }

    webSocket = null
    connectionStatus.isConnected = false
    connectionStatus.connectionStatus = 'Desconectado'
    connectionStatus.sessionId = null
    log('Conexão WebSocket desconectada', 'info')
    notifyStatusChange()

    return true
  } catch (error) {
    log(`Falha ao desconectar o WebSocket: ${error}`, 'error')
    return false
  }
}

export function isWebSocketConnected(): boolean {
  return webSocket !== null && webSocket.readyState === WebSocket.OPEN
}

export function getConnectionStatus(): ConnectionStatus {
  return { ...connectionStatus }
}

