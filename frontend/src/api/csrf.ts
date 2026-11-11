import { fetchPlayerCsrf } from './account'
import { ApiError } from './http'

/**
 * 选手端写操作统一处理 CSRF 过期：重新获取令牌后重试一次。
 */
export async function withPlayerCsrfRetry<T>(action: () => Promise<T>): Promise<T> {
  try {
    return await action()
  } catch (error) {
    if (error instanceof ApiError && error.code === 'COMMON_CSRF_INVALID') {
      await fetchPlayerCsrf()
      return action()
    }
    throw error
  }
}
