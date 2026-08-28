// Serviço de processamento de áudio - Versão Vue3 TypeScript

import { log } from './websocket'

// =============================
// Definições de tipo
// =============================

interface OpusDecoderModule {
  _opus_decoder_get_size: (channels: number) => number
  _opus_decoder_init: (decoder: number, sampleRate: number, channels: number) => number
  _opus_decode: (
    decoder: number,
    data: number,
    len: number,
    pcm: number,
    frameSize: number,
    decodeFec: number
  ) => number
  _malloc: (size: number) => number
  _free: (ptr: number) => void
  HEAPU8: Uint8Array
  HEAP16: Int16Array
}

interface OpusDecoder {
  channels: number
  rate: number
  frameSize: number
  module: OpusDecoderModule
  decoderPtr: number | null
  init: () => boolean
  decode: (opusData: Uint8Array) => Int16Array
  destroy: () => void
}

interface AudioConfig {
  sampleRate: number
  channels: number
  frameSize: number
}

interface StreamingContext {
  queue: number[]
  playing: boolean
  endOfStream: boolean
  source: AudioBufferSourceNode | null
  totalSamples: number
  lastPlayTime: number
  analyser: AnalyserNode | null
  decodeOpusFrames: (opusFrames: Uint8Array[]) => Promise<void>
  startPlaying: () => void
}

// Possível estrutura do Opus Module
interface OpusModule {
  instance?: OpusDecoderModule
  _opus_decoder_get_size?: (channels: number) => number
  _opus_decoder_init?: (decoder: number, sampleRate: number, channels: number) => number
  _opus_decode?: (
    decoder: number,
    data: number,
    len: number,
    pcm: number,
    frameSize: number,
    decodeFec: number
  ) => number
  _malloc?: (size: number) => number
  _free?: (ptr: number) => void
  HEAPU8?: Uint8Array
  HEAP16?: Int16Array
}

declare global {
  interface Window {
    Module?: OpusModule
    ModuleInstance?: OpusDecoderModule
    audioContext?: AudioContext
    streamingContext?: StreamingContext
    enableAudio?: () => Promise<boolean>
  }
}

// =============================
// Configuração
// =============================

const defaultConfig: AudioConfig = {
  sampleRate: 16000,
  channels: 1,
  frameSize: 960 // 60ms @ 16kHz
}

// =============================
// Variáveis de estado
// =============================

let audioContext: AudioContext | null = null
let opusDecoder: OpusDecoder | null = null
let audioBufferQueue: Uint8Array[] = []
let isAudioBuffering = false
let isAudioPlaying = false
let streamingContext: StreamingContext | null = null
let audioContextResumePromise: Promise<AudioContext> | null = null

// =============================
// Inicialização do contexto de áudio
// =============================

async function initAudioContext(): Promise<AudioContext | null> {
  if (audioContext) {
    if (audioContext.state === 'suspended' && !audioContextResumePromise) {
      audioContextResumePromise = new Promise((resolve) => {
        log('Contexto de áudio pausado. É necessária interação do usuário para retomar.', 'warning')

        const resumeAudioContext = async () => {
          try {
            if (audioContext) {
              await audioContext.resume()
              log('Contexto de áudio retomado por meio de interação do usuário', 'success')
              resolve(audioContext)

              ;['click', 'touchstart', 'keydown'].forEach(event => {
                document.removeEventListener(event, resumeAudioContext)
              })
              audioContextResumePromise = null
            }
          } catch (err) {
            log('Falha ao retomar o contexto de áudio: ' + err, 'error')
          }
        }

        ;['click', 'touchstart', 'keydown'].forEach(event => {
          document.addEventListener(event, resumeAudioContext, { once: false })
        })
      })

      return audioContextResumePromise
    }
    return audioContext
  }

  try {
    const AudioContextClass = window.AudioContext || (window as unknown as Record<string, unknown>).webkitAudioContext
    audioContext = new AudioContextClass({
      sampleRate: defaultConfig.sampleRate,
      latencyHint: 'interactive'
    })

    if (audioContext.state === 'suspended') {
      log('O contexto de áudio recém-criado está em estado pausado. É necessária interação do usuário para iniciá-lo.', 'warning')

      audioContextResumePromise = new Promise((resolve) => {
        const resumeAudioContext = async () => {
          try {
            if (audioContext) {
              await audioContext.resume()
              log('Contexto de áudio iniciado por meio de interação do usuário', 'success')
              resolve(audioContext)

              ;['click', 'touchstart', 'keydown'].forEach(event => {
                document.removeEventListener(event, resumeAudioContext)
              })
              audioContextResumePromise = null
            }
          } catch (err) {
            log('Falha ao iniciar o contexto de áudio: ' + err, 'error')
          }
        }

        ;['click', 'touchstart', 'keydown'].forEach(event => {
          document.addEventListener(event, resumeAudioContext, { once: false })
        })
      })

      return audioContextResumePromise
    }

    window.audioContext = audioContext

    return audioContext
  } catch (error) {
    log('Falha ao inicializar o contexto de áudio:' + error, 'error')
    return null
  }
}

// =============================
// Carregamento da biblioteca Opus
// =============================

function checkOpusLoaded(): boolean {
  try {
    if (!window.Module) {
      return false
    }

    const module = window.Module

    // Verifica se Module.instance existe e é válido
    if (
      module.instance &&
      typeof module.instance._opus_decoder_get_size === 'function'
    ) {
      window.ModuleInstance = module.instance
      log('Biblioteca Opus carregada com sucesso (usando Module.instance)', 'success')
      return true
    }

    // Verifica se o próprio Module contém os métodos do decodificador
    if (typeof module._opus_decoder_get_size === 'function') {
      // Garante que o module está em conformidade com a interface OpusDecoderModule
      const decoderModule: OpusDecoderModule = {
        _opus_decoder_get_size: module._opus_decoder_get_size,
        _opus_decoder_init: module._opus_decoder_init!,
        _opus_decode: module._opus_decode!,
        _malloc: module._malloc!,
        _free: module._free!,
        HEAPU8: module.HEAPU8!,
        HEAP16: module.HEAP16!
      }
      window.ModuleInstance = decoderModule
      log('Biblioteca Opus carregada com sucesso (usando Module global)', 'success')
      return true
    }

    // Verifica se ModuleInstance já foi definido
    if (
      window.ModuleInstance &&
      typeof window.ModuleInstance._opus_decoder_get_size === 'function'
    ) {
      log('Biblioteca Opus já carregada (usando ModuleInstance)', 'success')
      return true
    }

    return false
  } catch (err) {
    log(`Falha na verificação da biblioteca Opus: ${err}`, 'error')
    return false
  }
}

export function loadOpusLibrary(): Promise<boolean> {
  return new Promise(resolve => {
    if (checkOpusLoaded()) {
      resolve(true)
      return
    }

    log('Tentando carregar libopus.js', 'info')

    const possiblePaths = [
      '/libopus.js',
      '/js/libopus.js',
      '/static/js/libopus.js',
      './libopus.js',
      '../js/libopus.js'
    ]

    const script = document.createElement('script')
    script.async = true

    script.onload = () => {
      log('Script libopus.js carregado com sucesso, aguardando inicialização', 'success')

      const maxAttempts = 100
      let attempts = 0

      const checkModule = () => {
        if (checkOpusLoaded()) {
          log('Biblioteca Opus inicializada com sucesso', 'success')
          resolve(true)
          return
        }

        if (attempts >= maxAttempts) {
          log('Tempo esgotado na inicialização da biblioteca Opus', 'error')
          resolve(false)
          return
        }

        attempts++
        setTimeout(checkModule, 100)
      }

      checkModule()
    }

    script.onerror = () => {
      log('Falha ao carregar libopus.js, tentando o próximo caminho', 'warning')
      tryNextPath()
    }

    let pathIndex = 0

    function tryNextPath() {
      if (pathIndex >= possiblePaths.length) {
        log('Falha em todas as tentativas de caminho', 'error')
        resolve(false)
        return
      }

      const path = possiblePaths[pathIndex]
      pathIndex++

      if (!path) {
        log('Caminho vazio, falha no carregamento', 'error')
        resolve(false)
        return
      }

      log(`Tentando carregar a partir do caminho: ${path}`, 'info')
      script.src = path
      document.head.appendChild(script)
    }

    tryNextPath()
  })
}

// =============================
// Decodificador Opus
// =============================

function createOpusDecoder(mod: OpusDecoderModule): OpusDecoder {
  try {
    const SAMPLE_RATE = 16000
    const CHANNELS = 1
    const FRAME_SIZE = 960

    const decoder: OpusDecoder = {
      channels: CHANNELS,
      rate: SAMPLE_RATE,
      frameSize: FRAME_SIZE,
      module: mod,
      decoderPtr: null,

      init: function () {
        if (this.decoderPtr) return true

        const decoderSize = mod._opus_decoder_get_size(this.channels)
        log('Tamanho do decodificador Opus:' + decoderSize + ' bytes', 'debug')

        this.decoderPtr = mod._malloc(decoderSize)
        if (!this.decoderPtr) {
        throw new Error('Não foi possível alocar memória para o decodificador')
        }

        const err = mod._opus_decoder_init(this.decoderPtr, this.rate, this.channels)

        if (err < 0) {
          this.destroy()
          throw new Error(`Falha ao inicializar o decodificador Opus: ${err}`)
        }

        log('Decodificador Opus inicializado com sucesso', 'success')
        return true
      },

      decode: function (opusData: Uint8Array): Int16Array {
        if (!this.decoderPtr) {
          if (!this.init()) {
            throw new Error('Decodificador não inicializado e não foi possível inicializá-lo')
          }
        }

        try {
          const mod = this.module

          const opusPtr = mod._malloc(opusData.length)
          mod.HEAPU8.set(opusData, opusPtr)

          const pcmPtr = mod._malloc(this.frameSize * 2)

          const decodedSamples = mod._opus_decode(
            this.decoderPtr!,
            opusPtr,
            opusData.length,
            pcmPtr,
            this.frameSize,
            0
          )

          if (decodedSamples < 0) {
            mod._free(opusPtr)
            mod._free(pcmPtr)
            throw new Error(`Falha na decodificação Opus: ${decodedSamples}`)
          }

          const decodedData = new Int16Array(decodedSamples)
          for (let i = 0; i < decodedSamples; i++) {
            const heapValue = mod.HEAP16[(pcmPtr >> 1) + i]
            if (heapValue !== undefined) {
              decodedData[i] = heapValue
            }
          }

          mod._free(opusPtr)
          mod._free(pcmPtr)

          return decodedData
        } catch (error) {
          log('Erro de decodificação Opus:' + error, 'error')
          return new Int16Array(0)
        }
      },

      destroy: function () {
        if (this.decoderPtr) {
          this.module._free(this.decoderPtr)
          this.decoderPtr = null
        }
      }
    }

    if (!decoder.init()) {
      throw new Error('Falha ao inicializar o decodificador Opus')
    }

    opusDecoder = decoder
    return decoder
  } catch (error) {
    log('Falha ao inicializar o decodificador Opus:' + error, 'error')
    opusDecoder = null
    throw error
  }
}

export async function initOpusDecoder(): Promise<OpusDecoder | null> {
  if (opusDecoder) {
    return opusDecoder
  }

  try {
    const opusLoaded = await loadOpusLibrary()
    if (!opusLoaded) {
      throw new Error('Biblioteca Opus não carregada')
    }

    const mod = window.ModuleInstance
    if (!mod) {
      throw new Error('ModuleInstance indisponível')
    }

    return createOpusDecoder(mod)
  } catch (error) {
    log(`Falha ao inicializar o decodificador Opus:` + error, 'error')
    throw error
  }
}

// =============================
// Reprodução de áudio
// =============================

function convertInt16ToFloat32(int16Data: Int16Array): number[] {
  const float32Data: number[] = []
  for (let i = 0; i < int16Data.length; i++) {
    const sample = int16Data[i]
    if (sample !== undefined) {
      float32Data.push(sample / (sample < 0 ? 0x8000 : 0x7fff))
    }
  }
  return float32Data
}

function resetAudioBuffer(): void {
  audioBufferQueue = []
  isAudioBuffering = false
  isAudioPlaying = false
}

function addAudioToBuffer(opusData: Uint8Array): boolean {
  audioBufferQueue.push(opusData)

  // Se não estiver reproduzindo, inicia o processo de buffer
  if (!isAudioPlaying && !isAudioBuffering) {
    startAudioBuffering()
  }
  // Se estiver reproduzindo mas não houver segmento atual e houver dados suficientes, aciona a decodificação
  else if (isAudioPlaying && streamingContext && !streamingContext.playing && audioBufferQueue.length >= 3) {
    log('🔄 Novos dados recebidos durante a reprodução, decodificando imediatamente', 'debug')
    const frames = [...audioBufferQueue]
    audioBufferQueue = []
    streamingContext.decodeOpusFrames(frames)
  }

  return true
}

function startAudioBuffering(): boolean {
  if (isAudioBuffering || isAudioPlaying) return false

  isAudioBuffering = true
  log('Iniciando buffer de áudio...', 'info')

  initOpusDecoder().catch(error => {
    log(`Falha ao pré-inicializar o decodificador Opus: ${error}`, 'warning')
  })

  setTimeout(() => {
    if (isAudioBuffering && audioBufferQueue.length > 0) {
      log(`Tempo de buffer esgotado, pacotes em buffer no momento: ${audioBufferQueue.length}, iniciando reprodução`, 'info')
      playBufferedAudio()
    }
  }, 300)

  const bufferThreshold = 3
  const bufferCheckInterval = setInterval(() => {
    if (!isAudioBuffering) {
      clearInterval(bufferCheckInterval)
      return
    }

    if (audioBufferQueue.length >= bufferThreshold) {
      clearInterval(bufferCheckInterval)
      log(`${audioBufferQueue.length} pacotes de áudio em buffer, iniciando reprodução`, 'info')
      playBufferedAudio()
    }
  }, 50)

  return true
}

async function playBufferedAudio(): Promise<boolean> {
  if (isAudioPlaying || audioBufferQueue.length === 0) return false

  isAudioPlaying = true
  isAudioBuffering = false

  try {
    if (!audioContext) {
      audioContext = await initAudioContext()
    }

    if (!audioContext || audioContext.state === 'suspended') {
      log('Contexto de áudio pausado, aguardando interação do usuário...', 'warning')
      isAudioPlaying = false
      return false
    }

    if (!opusDecoder) {
      log('Inicializando decodificador Opus...', 'info')
      try {
        opusDecoder = await initOpusDecoder()
        if (!opusDecoder) {
          throw new Error('Falha ao inicializar o decodificador')
        }
        log('Decodificador Opus inicializado com sucesso', 'success')
      } catch (error) {
        log('Falha ao inicializar o decodificador Opus: ' + error, 'error')
        isAudioPlaying = false
        return false
      }
    }

    if (!streamingContext) {
      streamingContext = {
        queue: [],
        playing: false,
        endOfStream: false,
        source: null,
        totalSamples: 0,
        lastPlayTime: 0,
        analyser: null,

        decodeOpusFrames: async function (opusFrames: Uint8Array[]) {
          if (!opusDecoder) {
            log('Decodificador Opus não inicializado, não é possível decodificar', 'error')
            return
          }

          const decodedSamples: number[] = []
          for (const frame of opusFrames) {
            try {
              const frameData = opusDecoder.decode(frame)
              if (frameData && frameData.length > 0) {
                const floatData = convertInt16ToFloat32(frameData)
                decodedSamples.push(...floatData)
              }
            } catch (error) {
              log('Falha na decodificação Opus: ' + error, 'error')
            }
          }

          if (decodedSamples.length > 0) {
            this.queue.push(...decodedSamples)
            this.totalSamples += decodedSamples.length

            const minSamples = defaultConfig.sampleRate * 0.1
            if (!this.playing && this.queue.length >= minSamples) {
              this.startPlaying()
            }
          } else {
            log('Nenhuma amostra decodificada com sucesso', 'warning')
          }
        },

        startPlaying: function () {
          if (this.playing || this.queue.length === 0 || !audioContext) return

          if (audioContext.state === 'suspended') {
            log('Contexto de áudio ainda pausado, não é possível reproduzir', 'warning')
            return
          }

          this.playing = true

          const minPlaySamples = Math.min(this.queue.length, defaultConfig.sampleRate)
          const currentSamples = this.queue.splice(0, minPlaySamples)
          const audioBuffer = audioContext.createBuffer(
            defaultConfig.channels,
            currentSamples.length,
            defaultConfig.sampleRate
          )

          const channelData = audioBuffer.getChannelData(0)
          for (let i = 0; i < currentSamples.length; i++) {
            const sample = currentSamples[i]
            if (sample !== undefined) {
              channelData[i] = sample
            }
          }

          this.source = audioContext.createBufferSource()
          this.source.buffer = audioBuffer

          const gainNode = audioContext.createGain()

          const fadeDuration = 0.02
          gainNode.gain.setValueAtTime(0, audioContext.currentTime)
          gainNode.gain.linearRampToValueAtTime(1, audioContext.currentTime + fadeDuration)

          const duration = audioBuffer.duration
          if (duration > fadeDuration * 2) {
            gainNode.gain.setValueAtTime(1, audioContext.currentTime + duration - fadeDuration)
            gainNode.gain.linearRampToValueAtTime(0, audioContext.currentTime + duration)
          }

          const analyserNode = audioContext.createAnalyser()
          analyserNode.fftSize = 256
          analyserNode.smoothingTimeConstant = 0.8

          this.source.connect(analyserNode)
          analyserNode.connect(gainNode)
          gainNode.connect(audioContext.destination)

          this.analyser = analyserNode

          this.lastPlayTime = audioContext.currentTime

          log(
            `Iniciando reprodução de ${currentSamples.length} amostras, aproximadamente ${(currentSamples.length / defaultConfig.sampleRate).toFixed(2)} segundos`,
            'debug'
          )

          this.source.onended = () => {
            this.source = null
            this.analyser = null
            this.playing = false

            // Continua reproduzindo os dados na fila
            if (this.queue.length > 0) {
              setTimeout(() => this.startPlaying(), 10)
            }
            // Verifica se há novos dados em buffer
            else if (audioBufferQueue.length > 0) {
              const frames = [...audioBufferQueue]
              audioBufferQueue = []
              this.decodeOpusFrames(frames)
            }
            // O stream terminou explicitamente
            else if (this.endOfStream) {
              log('🏁 Reprodução de áudio concluída (fim do stream)', 'info')
              isAudioPlaying = false
              streamingContext = null
              window.streamingContext = undefined
            }
            // Aguardando mais dados (sem timeout, espera contínua)
            else {
              log('⏳ Aguardando mais dados de áudio...', 'debug')
              // Nenhuma ação necessária, mantém isAudioPlaying = true
              // Quando novos dados chegarem, addAudioToBuffer acionará a continuação da reprodução
            }
          }

          this.source.start()
        }
      }

      window.streamingContext = streamingContext
    }

    const frames = [...audioBufferQueue]
    audioBufferQueue = []

    await streamingContext.decodeOpusFrames(frames)
    return true
  } catch (error) {
    log(`Erro ao reproduzir o áudio em buffer:` + error, 'error')
    isAudioPlaying = false
    streamingContext = null
    window.streamingContext = undefined

    return false
  }
}

export function stopAudioPlayback(): boolean {
  try {
    isAudioPlaying = false
    isAudioBuffering = false

    if (streamingContext && streamingContext.source) {
      try {
        streamingContext.source.stop()
        streamingContext.source = null
        streamingContext.analyser = null
      } catch (e) {
        // Ignora erros de fonte de áudio já interrompida
      }
    }

    audioBufferQueue = []
    streamingContext = null

    window.streamingContext = undefined

    if (window.dispatchEvent) {
      window.dispatchEvent(new CustomEvent('audio-playback-stopped'))
    }

    log('Reprodução de áudio interrompida', 'info')
    return true
  } catch (error) {
    log(`Falha ao interromper a reprodução de áudio:` + error, 'error')
    return false
  }
}

// =============================
// Funções exportadas
// =============================

export async function initAudio(): Promise<boolean> {
  try {
    const context = await initAudioContext()

    window.enableAudio = async function () {
      try {
        if (audioContext && audioContext.state === 'suspended') {
          await audioContext.resume()
          log('Contexto de áudio retomado', 'success')
        }

        let opusLoaded = false
        for (let i = 0; i < 3; i++) {
          try {
            opusLoaded = await loadOpusLibrary()
            if (opusLoaded) {
              log(`Biblioteca Opus carregada com sucesso (tentativa ${i + 1}/3)`, 'success')
              break
            }
          } catch (err) {
            log(`Tentativa ${i + 1}/3 de carregar libopus.js falhou, tentando novamente`, 'warning')
          }
        }

        if (!opusLoaded) {
          log('Todas as tentativas de carregar a biblioteca Opus falharam, a reprodução de áudio ficará indisponível', 'error')
          return false
        }

        try {
          await initOpusDecoder()
          log('Decodificador Opus inicializado com sucesso', 'success')
          return true
        } catch (err) {
            log(`Falha ao inicializar o decodificador Opus: ${err}, a reprodução de áudio ficará indisponível`, 'error')
          return false
        }
      } catch (error) {
        log('Falha ao habilitar o áudio:' + error, 'error')
        return false
      }
    }

    await loadOpusLibrary()

    log('Sistema de áudio inicializado. Habilite o áudio por meio de interação do usuário.', 'info')

    return true
  } catch (error) {
    log('Falha ao inicializar o áudio:' + error, 'error')
    return false
  }
}

export async function handleBinaryAudioMessage(data: ArrayBuffer): Promise<boolean> {
  try {
    log(`Dados de áudio ArrayBuffer recebidos, tamanho: ${data.byteLength} bytes`, 'debug')

    const opusData = new Uint8Array(data)

    if (opusData.length > 0) {
      addAudioToBuffer(opusData)

      if (window.dispatchEvent) {
        window.dispatchEvent(
          new CustomEvent('audio-data-received', {
            detail: { dataLength: opusData.length }
          })
        )
      }

      return true
    } else {
      log('Quadro de dados de áudio vazio recebido, possivelmente um marcador de fim', 'warning')

      if (audioBufferQueue.length > 0 && !isAudioPlaying) {
        playBufferedAudio()
      }

      if (isAudioPlaying && streamingContext) {
        streamingContext.endOfStream = true
      }

      if (window.dispatchEvent) {
        window.dispatchEvent(new CustomEvent('audio-playback-ended'))
      }

      return true
    }
  } catch (error) {
    log('Erro ao processar mensagem binária:' + error, 'error')
    return false
  }
}

export function cleanupAudio(): boolean {
  try {
    stopAudioPlayback()

    if (opusDecoder && opusDecoder.destroy) {
      opusDecoder.destroy()
      opusDecoder = null
    }

    if (audioContext && audioContext.state !== 'closed') {
      audioContext.close()
      audioContext = null
    }

    resetAudioBuffer()

    log('Recursos de áudio limpos', 'info')
    return true
  } catch (error) {
    log('Falha ao limpar recursos de áudio:' + error, 'error')
    return false
  }
}

export function getAudioState() {
  return {
    isAudioBuffering,
    isAudioPlaying,
    audioBufferQueue,
    analyser: streamingContext && streamingContext.analyser
  }
}

