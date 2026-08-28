/**
 * Serviço relacionado ao gerenciamento de agentes
 */
import { http } from './request'
import api from './api'
import type { Agent, AgentQueryParams } from '@/types/agent'

/**
 * Consulta a lista de agentes
 */
export function queryAgents(params: Partial<AgentQueryParams>) {
  return http.getPage<Agent>(api.agent.query, params)
}
