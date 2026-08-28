/**
 * Processador AudioWorklet - usado para gravação de áudio
 * Substitui o ScriptProcessorNode, que está obsoleto
 */
class AudioRecorderProcessor extends AudioWorkletProcessor {
  constructor() {
    super()
    this.bufferSize = 4096
    this.buffer = []
    this.bufferLength = 0
  }

  process(inputs, outputs, parameters) {
    const input = inputs[0]
    
    // Se houver dados de áudio de entrada
    if (input && input.length > 0) {
      const channelData = input[0] // Obtém o primeiro canal
      
      if (channelData && channelData.length > 0) {
        // Copia os dados de áudio (evita problemas de referência)
        const copy = new Float32Array(channelData.length)
        copy.set(channelData)
        
        // Adiciona ao buffer
        this.buffer.push(copy)
        this.bufferLength += copy.length
        
        // Quando o buffer atinge o tamanho especificado, envia os dados
        if (this.bufferLength >= this.bufferSize) {
          // Mescla os dados do buffer
          const mergedBuffer = new Float32Array(this.bufferLength)
          let offset = 0
          
          for (const buf of this.buffer) {
            mergedBuffer.set(buf, offset)
            offset += buf.length
          }
          
          // Envia os dados de áudio para a thread principal
          this.port.postMessage({
            type: 'audio-data',
            data: mergedBuffer
          })
          
          // Limpa o buffer
          this.buffer = []
          this.bufferLength = 0
        }
      }
    }
    
    // Retorna true para indicar que o processamento deve continuar
    return true
  }
}

// Registra o processador
registerProcessor('audio-recorder-processor', AudioRecorderProcessor)

