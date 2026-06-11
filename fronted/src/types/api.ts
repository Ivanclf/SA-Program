/** 统一后端响应结构 */
export interface ApiResponse<T> {
  code: number    // 200 = 成功，400 = 参数错误，404 = 未找到
  message: string
  data: T
}
