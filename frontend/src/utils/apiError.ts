import { ApiError } from '@/api/http'

/**
 * 后台页面统一错误提示：优先展示服务端稳定错误码对应的可读文案。
 */
export function resolveApiErrorMessage(error: unknown, fallback = '操作失败，请稍后重试'): string {
  if (error instanceof ApiError && error.message) {
    return error.message
  }
  return fallback
}
