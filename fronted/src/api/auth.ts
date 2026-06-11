import request from './index'
import type { ApiResponse } from '@/types/api'
import type { User, LoginRequest, RegisterRequest } from '@/types/user'

/** 用户登录 */
export function loginApi(data: LoginRequest): Promise<ApiResponse<User>> {
  return request.post('/api/user/login', data)
}

/** 用户注册 */
export function registerApi(data: RegisterRequest): Promise<ApiResponse<User>> {
  return request.post('/api/user/register', data)
}

/** 用户登出 */
export function logoutApi(userId: string): Promise<ApiResponse<null>> {
  return request.post('/api/user/logout', null, { params: { userId } })
}

/** 查询用户详情 */
export function getUserApi(id: string): Promise<ApiResponse<User>> {
  return request.get(`/api/user/${id}`)
}
