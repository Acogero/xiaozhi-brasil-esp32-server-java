import { http } from './request'
import api from './api'
import type { Device, DeviceQueryParams } from '@/types/device'

/**
 * Consulta a lista de dispositivos
 */
export function queryDevices(params: Partial<DeviceQueryParams>) {
  return http.getPage<Device>(api.device.query, params)
}

/**
 * Adiciona um dispositivo
 */
export function addDevice(code: string) {
  return http.post(api.device.add, { code })
}

/**
 * Atualiza as informações do dispositivo
 */
export function updateDevice(data: Partial<Device>) {
  return http.put(`${api.device.update}/${data.deviceId}`, data)
}

/**
 * Exclui um dispositivo
 */
export function deleteDevice(deviceId: string) {
  return http.delete(`${api.device.delete}/${deviceId}`)
}

/**
 * Limpa a memória do dispositivo
 */
export function clearDeviceMemory(deviceId: string) {
  return http.delete(api.message.delete, { deviceId })
}
