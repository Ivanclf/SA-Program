/** 用户信息 */
export interface User {
  userId: string
  username: string
  role: 1 | 2        // 1=管理员, 2=审核员
  ctime: string
  utime: string
}

/** 登录请求 */
export interface LoginRequest {
  username: string
  password: string
}

/** 注册请求 */
export interface RegisterRequest {
  username: string
  password: string
  role: 1 | 2
}
