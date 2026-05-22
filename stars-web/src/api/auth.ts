import http, { unwrap } from '@/api/http'
import type { ApiResponse } from '@/types/api'

export interface LoginPayload {
  clientId: string
  grantType: string
  tenantId?: string
  username: string
  password: string
  code?: string
  uuid?: string
}

export interface LoginResult {
  access_token: string
  expire_in: number
  client_id: string
}

export interface CaptchaResult {
  captchaEnabled: boolean
  uuid?: string
  img?: string
}

export function getCaptcha() {
  return http.get<ApiResponse<CaptchaResult>>('/auth/code').then(unwrap)
}

export function login(payload: LoginPayload) {
  return http.post<ApiResponse<LoginResult>>('/auth/login', payload).then(unwrap)
}

export function logout() {
  return http.post<ApiResponse<void>>('/auth/logout')
}
