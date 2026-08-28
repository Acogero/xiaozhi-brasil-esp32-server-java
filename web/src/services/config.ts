import { http } from './request'
import api from './api'
import type { Config, ConfigQueryParams } from '@/types/config'
import type { PlatformConfig } from '@/types/agent'

/**
 * Consulta a lista de configurações
 */
export function queryConfigs(params: Partial<ConfigQueryParams>) {
  return http.getPage<Config>(api.config.query, params)
}

/**
 * Adiciona uma configuração
 */
export function addConfig(data: Partial<Config>) {
  return http.post(api.config.add, data)
}

/**
 * Atualiza uma configuração
 */
export function updateConfig(data: Partial<Config>) {
  return http.put(`${api.config.update}/${data.configId}`, data)
}

/**
 * Exclui uma configuração
 */
export function deleteConfig(configId: number) {
  return http.delete(`${api.config.delete}/${configId}`)
}

/**
 * Consulta a configuração da plataforma
 */
export function queryPlatformConfig(configType: string, provider: string) {
  return http.getPage<Config>(api.config.query, {
    configType,
    provider
  })
}

/**
 * Adiciona uma configuração de plataforma
 */
export function addPlatformConfig(data: Partial<PlatformConfig>) {
  return http.post(api.config.add, data)
}

/**
 * Atualiza a configuração da plataforma
 */
export function updatePlatformConfig(data: Partial<PlatformConfig>) {
  return http.put(`${api.config.update}/${data.configId}`, data)
}
