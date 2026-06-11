import type { User } from '@/types/user'

/** Token 有效期：24 小时（毫秒） */
const TOKEN_TTL = 24 * 60 * 60 * 1000

interface TokenPayload {
  userId: string
  username: string
  role: 1 | 2
  exp: number
}

/** 生成 token（Base64 编码 JSON，含 24h 过期时间） */
export function generateToken(user: User): string {
  const payload: TokenPayload = {
    userId: user.userId,
    username: user.username,
    role: user.role,
    exp: Date.now() + TOKEN_TTL,
  }
  return btoa(JSON.stringify(payload))
}

/** 解析 token，过期返回 null */
export function parseToken(token: string): TokenPayload | null {
  try {
    const payload: TokenPayload = JSON.parse(atob(token))
    if (Date.now() >= payload.exp) {
      return null // 已过期
    }
    return payload
  } catch {
    return null
  }
}

/** 从 token 还原 User 信息 */
export function tokenToUser(token: string): User | null {
  const payload = parseToken(token)
  if (!payload) return null
  return {
    userId: payload.userId,
    username: payload.username,
    role: payload.role,
    ctime: '',
    utime: '',
  }
}
