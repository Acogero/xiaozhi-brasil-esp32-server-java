import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { Device } from '@/types/device'

/**
 * Store de estado de dispositivos
 * Gerencia lista de dispositivos, status online, etc.
 */
export const useDeviceStore = defineStore('device', () => {
  // ========== Lista de dispositivos ==========
  const devices = ref<Device[]>([])
  
  // Lista de dispositivos online
  const onlineDevices = computed(() => {
    return devices.value.filter(device => device.state == '1')
  })

  // Lista de dispositivos offline
  const offlineDevices = computed(() => {
    return devices.value.filter(device => device.state == '0')
  })

  // Total de dispositivos
  const totalDevices = computed(() => devices.value.length)

  // Número de dispositivos online
  const onlineCount = computed(() => onlineDevices.value.length)

  // ========== Dispositivo atualmente selecionado ==========
  const currentDevice = ref<Device | null>(null)

  // ========== Métodos de operação ==========
  
  /**
   * Define a lista de dispositivos
   */
  const setDevices = (list: Device[]) => {
    devices.value = list
  }

  /**
   * Adiciona dispositivo
   */
  const addDevice = (device: Device) => {
    devices.value.push(device)
  }

  /**
   * Atualiza dispositivo
   */
  const updateDevice = (deviceId: string, updates: Partial<Device>) => {
    const index = devices.value.findIndex(d => d.deviceId === deviceId)
    if (index !== -1) {
      const current = devices.value[index]
      if (current) {
        devices.value[index] = {
          ...current,
          ...updates,
          deviceId: current.deviceId,
          state: updates.state !== undefined ? updates.state : current.state,
        }
      }
    }
  }

  /**
   * Remove dispositivo
   */
  const removeDevice = (deviceId: string) => {
    const index = devices.value.findIndex(d => d.deviceId === deviceId)
    if (index !== -1) {
      devices.value.splice(index, 1)
    }
  }

  /**
   * Atualiza o status online do dispositivo
   */
  const updateDeviceStatus = (deviceId: string, online: boolean) => {
    updateDevice(deviceId, { state: online ? '1' : '0' })
  }

  /**
   * Define o dispositivo atualmente selecionado
   */
  const setCurrentDevice = (device: Device | null) => {
    currentDevice.value = device
  }

  /**
   * Obtém dispositivo pelo ID
   */
  const getDeviceById = (deviceId: string): Device | undefined => {
    return devices.value.find(d => d.deviceId === deviceId)
  }

  /**
   * Limpa a lista de dispositivos
   */
  const clearDevices = () => {
    devices.value = []
    currentDevice.value = null
  }

  return {
    // Estado
    devices,
    onlineDevices,
    offlineDevices,
    totalDevices,
    onlineCount,
    currentDevice,
    
    // Métodos
    setDevices,
    addDevice,
    updateDevice,
    removeDevice,
    updateDeviceStatus,
    setCurrentDevice,
    getDeviceById,
    clearDevices,
  }
})

