import api from './api'
import { useUserStore } from '@/store/user'

export interface UploadResponse {
  code: number
  message: string
  url: string
  fileName?: string
  newFileName?: string
  fileHash?: string
  hash?: string
}

export interface UploadOptions {
  onProgress?: (percent: number) => void
  fullResponse?: boolean
}

/**
 * Método genérico de upload de arquivos
 * @param file Arquivo a ser enviado
 * @param type Tipo de arquivo: avatar, firmware, etc.
 * @param options Opções de configuração do upload
 * @returns Por padrão retorna a URL; quando fullResponse=true, retorna a resposta completa
 */
export function uploadFile(
  file: File,
  type: string = 'avatar',
  options?: UploadOptions
): Promise<string | UploadResponse> {
  return new Promise((resolve, reject) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('type', type)

    const xhr = new XMLHttpRequest()
    // Usa a URL completa da API, evitando que o caminho relativo seja resolvido para o domínio do frontend
    const baseURL = import.meta.env.VITE_API_BASE_URL || ''
    const uploadUrl = `${baseURL}${api.upload}`
    xhr.open('POST', uploadUrl, true)

    // Adiciona o token de autenticação (formato Bearer)
    const userStore = useUserStore()
    if (userStore.token) {
      xhr.setRequestHeader('Authorization', `Bearer ${userStore.token}`)
    }

    if (options?.onProgress) {
      xhr.upload.onprogress = (event) => {
        if (event.lengthComputable) {
          const percent = Math.round((event.loaded / event.total) * 100)
          options.onProgress!(percent)
        }
      }
    }

    xhr.onload = function () {
      if (xhr.status === 200) {
        try {
          const response: UploadResponse = JSON.parse(xhr.responseText)
          if (response.code === 200) {
            resolve(options?.fullResponse ? response : response.url)
          } else {
            reject(new Error(response.message || 'Falha no upload'))
          }
        } catch (error) {
          reject(new Error('Falha ao analisar a resposta'))
        }
      } else {
        reject(new Error(`Falha no upload, código de status: ${xhr.status}`))
      }
    }

    xhr.onerror = function () {
      reject(new Error('Erro de rede'))
    }

    xhr.send(formData)
  })
}
