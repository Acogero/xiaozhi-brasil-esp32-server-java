import type { PageQueryParams } from './api'

/**
 * Interface de informações do usuário
 */
export interface User {
  userId: string
  name: string
  username?: string
  email?: string
  tel?: string
  avatar?: string
  state: number // 1-normal 0-desabilitado
  isAdmin: number // 1-administrador 0-usuário comum
  totalDevice?: number // Quantidade de dispositivos
  aliveNumber?: number // Dispositivos online
  totalMessage?: number // Quantidade de mensagens de conversa
  loginTime?: string // Horário do último login
  loginIp?: string // IP do último login
  authRoleId?: number // ID da função de permissão do backend
  authRoleName?: string // Nome da função de permissão do backend
  editable?: boolean // Estado de edição na tabela
}

/**
 * Parâmetros de consulta de usuário
 */
export interface UserQueryParams extends PageQueryParams {
  name?: string // Nome
  email?: string // E-mail
  tel?: string // Telefone
  authRoleId?: number // ID da função de permissão do backend
}

/**
 * Parâmetros de atualização de informações do usuário
 */
export interface UpdateUserParams {
  userId?: string
  username?: string
  name?: string
  email?: string
  tel?: string
  password?: string // Campo de senha
  avatar?: string // Campo de avatar
}
