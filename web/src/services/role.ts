import { http } from './request'
import api from './api'
import type { Role, RoleQueryParams, RoleFormData, TestVoiceParams } from '@/types/role'
import type { McpToolItem, SystemGlobalToolSummary } from '@/types/mcpTool'

/**
 * Consulta a lista de personagens
 */
export function queryRoles(params: Partial<RoleQueryParams>) {
  return http.getPage<Role>(api.role.query, params)
}

/**
 * Adiciona um personagem
 */
export function addRole(data: Partial<RoleFormData> & { avatar?: string }) {
  return http.post<Role>(api.role.add, data)
}

/**
 * Atualiza um personagem
 */
export function updateRole(data: Partial<RoleFormData>) {
  const { roleId, ...payload } = data
  return http.put<Role>(`${api.role.update}/${roleId}`, payload)
}

/**
 * Exclui um personagem
 */
export function deleteRole(roleId: number) {
  return http.delete(`${api.role.delete}/${roleId}`)
}

/**
 * Testa a voz
 */
export function testVoice(data: Partial<TestVoiceParams>) {
  return http.get<string>(api.role.testVoice, data)
}

/**
 * Obtém a lista local de timbres de voz do sherpa-onnx (escaneamento dinâmico do diretório models/tts)
 */
export function querySherpaVoices() {
  return http.getList<Record<string, string>>(api.role.sherpaVoices, {})
}

/**
 * Obtém a lista global de ferramentas do sistema
 */
export function getSystemGlobalTools() {
  return http.getList<SystemGlobalToolSummary>(api.mcpTool.systemGlobalTools, {})
}

/**
 * Obtém a lista de ferramentas desabilitadas para o personagem
 */
export function getDisabledTools(roleId: number) {
  return http.get<{ roleDisabled: string[]; globalDisabled: string[] }>(
    `${api.mcpTool.disabledTools}/${roleId}/disabled-tools`
  )
}

/**
 * Atualiza em lote o status de desabilitação das ferramentas
 */
export function updateToolsStatus(roleId: number, excludeTools: string[]) {
  return http.post(`${api.mcpTool.batchExclude}/${roleId}/exclude-tools`, { excludeTools })
}
