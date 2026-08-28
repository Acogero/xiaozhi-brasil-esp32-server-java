// useWebSocket composable - Gerenciamento de conexão WebSocket

import { ref, onBeforeUnmount } from 'vue'
import {
  connectToServer,
  disconnectFromServer,
  sendTextMessage,
  startDirectRecording,
  stopDirectRecording,
  isWebSocketConnected,
  registerMessageHandler,
  unregisterMessageHandler,
  registerStatusChangeCallback,
  unregisterStatusChangeCallback,
  registerBinaryHandler,
  messages,
  clearMessages,
  type WebSocketConfig,
  type WebSocketMessage,
  type ConnectionStatus
} from '@/services/websocket'
import { initAudio, handleBinaryAudioMessage } from '@/services/audio'

export function useWebSocket() {
  // Estado da conexão
  const isConnected = ref(false)
  const connectionStatus = ref('Não conectado')
  const connectionTime = ref<Date | null>(null)
  const sessionId = ref<string | null>(null)

  // Callback de mudança de estado
  const handleStatusChange = (status: ConnectionStatus) => {
    isConnected.value = status.isConnected
    connectionStatus.value = status.connectionStatus
    connectionTime.value = status.connectionTime
    sessionId.value = status.sessionId
  }

  // Registra o callback de mudança de estado
  registerStatusChangeCallback(handleStatusChange)

  // Limpa ao desmontar o componente
  onBeforeUnmount(() => {
    unregisterStatusChangeCallback(handleStatusChange)
  })

  // Conecta ao servidor
  const connect = async (config: WebSocketConfig): Promise<boolean> => {
    try {
      // Inicializa o sistema de áudio
      await initAudio()

      // Registra a função de tratamento de dados binários
      registerBinaryHandler(handleBinaryAudioMessage)

      // Conecta ao WebSocket
      const success = await connectToServer(config)
      return success
    } catch (error) {
      console.error('Falha na conexão:', error)
      return false
    }
  }

  // Desconecta
  const disconnect = (): boolean => {
    return disconnectFromServer()
  }

  // Envia mensagem de texto
  const sendText = (text: string): boolean => {
    return sendTextMessage(text)
  }

  // Inicia a gravação
  const startRecording = async (): Promise<boolean> => {
    try {
      return await startDirectRecording()
    } catch (error) {
      console.error('Falha ao iniciar a gravação:', error)
      throw error
    }
  }

  // Para a gravação
  const stopRecording = async (): Promise<boolean> => {
    try {
      return await stopDirectRecording()
    } catch (error) {
      console.error('Falha ao parar a gravação:', error)
      throw error
    }
  }

  // Verifica o estado da conexão
  const checkConnected = (): boolean => {
    return isWebSocketConnected()
  }

  // Registra a função de tratamento de mensagens
  const onMessage = (handler: (data: WebSocketMessage) => void): void => {
    registerMessageHandler(handler)
  }

  // Remove a função de tratamento de mensagens
  const offMessage = (handler: (data: WebSocketMessage) => void): void => {
    unregisterMessageHandler(handler)
  }

  // Limpa as mensagens
  const clearAllMessages = (): boolean => {
    return clearMessages()
  }

  return {
    // Estado
    isConnected,
    connectionStatus,
    connectionTime,
    sessionId,
    messages,

    // Métodos
    connect,
    disconnect,
    sendText,
    startRecording,
    stopRecording,
    checkConnected,
    onMessage,
    offMessage,
    clearAllMessages
  }
}

