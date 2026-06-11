import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, registerApi, logoutApi, getUserApi } from '@/api/auth'
import { generateToken, parseToken } from '@/utils/token'
import type { User, LoginRequest, RegisterRequest } from '@/types/user'

export const useAuthStore = defineStore('auth', () => {
  // ---- State ----
  const rawToken = localStorage.getItem('token')
  const token = ref<string | null>(parseToken(rawToken ?? '') ? rawToken : null)
  const user = ref<User | null>(null)

  // 初始化时从 localStorage 恢复用户信息（token 已通过 parseToken 验证有效期）
  if (token.value) {
    const savedUser = localStorage.getItem('user')
    if (savedUser) {
      try {
        user.value = JSON.parse(savedUser)
      } catch {
        // ignore
      }
    }
  } else {
    // 过期或无效，清理
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  // ---- Getters ----
  const isLoggedIn = computed(() => !!token.value && !!user.value)
  const isAdmin = computed(() => user.value?.role === 1)
  const isAuditor = computed(() => user.value?.role === 2)

  // ---- Actions ----
  /** 登录 */
  async function login(data: LoginRequest) {
    const res = await loginApi(data)
    const u = res.data
    const t = generateToken(u)
    localStorage.setItem('token', t)
    localStorage.setItem('user', JSON.stringify(u))
    token.value = t
    user.value = u
  }

  /** 注册 */
  async function register(data: RegisterRequest) {
    const res = await registerApi(data)
    const u = res.data
    const t = generateToken(u)
    localStorage.setItem('token', t)
    localStorage.setItem('user', JSON.stringify(u))
    token.value = t
    user.value = u
  }

  /** 登出 */
  async function logout() {
    if (user.value?.userId) {
      try {
        await logoutApi(user.value.userId)
      } catch {
        // ignore
      }
    }
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    token.value = null
    user.value = null
  }

  /** 从后端刷新用户信息 */
  async function fetchUser() {
    if (!token.value) return
    const payload = parseToken(token.value)
    if (!payload) {
      logout()
      return
    }
    try {
      const res = await getUserApi(payload.userId)
      user.value = res.data
      localStorage.setItem('user', JSON.stringify(res.data))
    } catch {
      logout()
    }
  }

  return {
    token,
    user,
    isLoggedIn,
    isAdmin,
    isAuditor,
    login,
    register,
    logout,
    fetchUser,
  }
})
