import { http } from './request'
import api from './api'
import type { MemoryQueryParams, SummaryMemory, ChatMemory } from '@/types/memory'
import type { MessageQueryParams } from '@/types/message'

/**
 * Consulta a memória resumida
 */
export function querySummaryMemory(params: MemoryQueryParams) {
  const { roleId, deviceId, pageNo = 1, pageSize = 10 } = params
  return http.getPage<SummaryMemory>(
    `${api.memory.summary}/${roleId}/${deviceId}`,
    { pageNo, pageSize }
  )
}

/**
 * Consulta a memória de chat (utiliza a interface message existente)
 */
export function queryChatMemory(params: {
  roleId: number
  deviceId: string
  pageNo?: number
  pageSize?: number
  startTime?: string
  endTime?: string
}) {
  const { roleId, deviceId, pageNo = 1, pageSize = 10, startTime, endTime } = params

  const queryParams: Partial<MessageQueryParams> = {
    pageNo,
    pageSize,
    deviceId,
    roleId,
    messageType: 'NORMAL',
  }

  if (startTime) queryParams.startTime = startTime
  if (endTime) queryParams.endTime = endTime

  return http.getPage<ChatMemory>(api.message.query, queryParams)
}

/**
 * Exclui a memória resumida
 */
export function deleteSummaryMemory(roleId: number, deviceId: string, summaryId?: number) {
  const url = `${api.memory.summary}/${roleId}/${deviceId}`
  const params = summaryId ? { id: summaryId } : {}
  return http.delete(url, params)
}
