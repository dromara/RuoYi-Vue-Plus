import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi } from '@/api/auth'
import { CLIENT_ID, TOKEN_KEY } from '@/constants'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))

  const isAuthenticated = computed(() => Boolean(token.value))

  async function login(username: string, password: string, code?: string, uuid?: string) {
    const result = await loginApi({
      clientId: CLIENT_ID,
      grantType: 'password',
      tenantId: '000000',
      username,
      password,
      code,
      uuid,
    })
    token.value = result.access_token
    localStorage.setItem(TOKEN_KEY, result.access_token)
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // ignore logout errors
    } finally {
      token.value = null
      localStorage.removeItem(TOKEN_KEY)
    }
  }

  return {
    token,
    isAuthenticated,
    login,
    logout,
  }
})
