import { http } from './request'
import api from './api'
import type { User, UserQueryParams, UpdateUserParams } from '@/types/user'
import type { LoginResponse } from '@/store/user'

/**
 * Login do usuário
 */
export function login(data: { username: string; password: string }) {
  return http.post<LoginResponse>(api.user.login, data)
}

/**
 * Login por código de verificação via celular
 */
export function telLogin(data: { tel: string; code: string }) {
  return http.post<LoginResponse>(api.user.telLogin, data)
}

/**
 * Verifica a validade do Token
 * Utilizado para validar o status de login ao atualizar a página
 */
export function checkToken() {
  return http.get<LoginResponse>(api.user.checkToken)
}

/**
 * Atualiza o Token
 * Estende o período de validade do login
 */
export function refreshToken() {
  return http.post<LoginResponse>(api.user.refreshToken)
}

/**
 * Registro de usuário
 */
export function register(data: {
  name: string
  username: string
  email: string
  tel?: string
  password: string
  verifyCode: string
}) {
  // O backend espera o parâmetro com o nome code; o frontend usa verifyCode por ser mais semântico
  const { verifyCode, ...rest } = data
  return http.post(api.user.add, { ...rest, code: verifyCode })
}

/**
 * Redefine a senha
 */
export function resetPassword(data: {
  email: string
  code: string
  password: string
}) {
  return http.post(api.user.resetPassword, data)
}

/**
 * Verifica se o usuário existe
 */
export function checkUser(data: { username?: string; email?: string }) {
  return http.get(api.user.checkUser, data)
}

/**
 * Envia o código de verificação por e-mail
 */
export function sendEmailCaptcha(data: { email: string; type: string }) {
  return http.post(api.user.sendEmailCaptcha, data)
}

/**
 * Envia o código de verificação por SMS
 */
export function sendSmsCaptcha(data: { tel: string; type: string }) {
  return http.post(api.user.sendSmsCaptcha, data)
}

/**
 * Valida o código de verificação
 */
export function checkCaptcha(data: { email: string; code: string; type: string }) {
  return http.get(api.user.checkCaptcha, data)
}

/**
 * Consulta a lista de usuários
 */
export function queryUsers(params: Partial<UserQueryParams>) {
  return http.getPage<User>(api.user.query, params)
}

/**
 * Atualiza as informações do usuário
 */
export function updateUser(data: Partial<UpdateUserParams>) {
  const { userId, ...updateData } = data
  return http.put(`${api.user.update}/${userId}`, updateData)
}

/**
 * Adiciona um usuário
 */
export function addUser(data: Partial<User>) {
  return http.post(api.user.add, data)
}
