import axios, { type AxiosError, type AxiosRequestConfig, type AxiosResponse } from 'axios'

import type { ApiErrorPayload, ApiResponse, FieldErrorItem } from '@/types/api'
import { readCookie } from '@/utils/cookie'

interface ApiErrorOptions {
  code: string
  message: string
  status?: number
  fieldErrors?: FieldErrorItem[] | null
  requestId?: string
}

/**
 * 前端统一 API 异常。客户端只依赖 code 分支，不解析 message 文案。
 */
export class ApiError extends Error {
  readonly code: string
  readonly status?: number
  readonly fieldErrors: FieldErrorItem[]
  readonly requestId?: string

  constructor(options: ApiErrorOptions) {
    super(options.message)
    this.name = 'ApiError'
    this.code = options.code
    this.status = options.status
    this.fieldErrors = options.fieldErrors ?? []
    this.requestId = options.requestId
  }
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15_000,
  withCredentials: true,
  headers: {
    Accept: 'application/json'
  }
})

http.interceptors.request.use((config) => {
  const method = (config.method ?? 'get').toLowerCase()
  if (['post', 'put', 'patch', 'delete'].includes(method)) {
    const requestUrl = config.url ?? ''
    const isAdminRequest = requestUrl.startsWith('/admin') || requestUrl.startsWith('admin/')
    const token = readCookie(isAdminRequest ? 'ADMIN-XSRF-TOKEN' : 'XSRF-TOKEN')
    if (token) {
      config.headers.set('X-XSRF-TOKEN', token)
    }
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorPayload>) => Promise.reject(toApiError(error))
)

export function toApiError(error: AxiosError<ApiErrorPayload>): ApiError {
  const response = error.response
  const payload = response?.data

  if (payload && typeof payload === 'object' && 'code' in payload && 'message' in payload) {
    return new ApiError({
      code: payload.code,
      message: payload.message,
      status: response?.status,
      fieldErrors: payload.fieldErrors,
      requestId: payload.requestId
    })
  }

  if (error.code === 'ECONNABORTED') {
    return new ApiError({ code: 'CLIENT_TIMEOUT', message: '请求超时，请稍后重试' })
  }

  return new ApiError({
    code: 'CLIENT_NETWORK_ERROR',
    message: error.message || '网络异常，请稍后重试'
  })
}

function unwrap<T>(response: AxiosResponse<ApiResponse<T>>): T {
  if (response.status === 204) {
    return undefined as T
  }

  const payload = response.data
  if (!payload || payload.success !== true) {
    throw new ApiError({
      code: payload?.code ?? 'CLIENT_INVALID_RESPONSE',
      message: payload?.message ?? '服务端返回格式异常',
      status: response.status,
      fieldErrors: payload?.fieldErrors,
      requestId: payload?.requestId
    })
  }
  return payload.data
}

export interface DownloadResult {
  blob: Blob
  filename: string | null
}

/**
 * 下载二进制接口。失败响应为 JSON 时仍转换为统一 ApiError。
 */
export async function download(url: string): Promise<DownloadResult> {
  try {
    const response = await http.get<Blob>(url, { responseType: 'blob' })
    return {
      blob: response.data,
      filename: parseFilename(response.headers['content-disposition'])
    }
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.data instanceof Blob) {
      const text = await error.response.data.text()
      try {
        const payload = JSON.parse(text) as ApiErrorPayload
        if (payload && typeof payload === 'object' && 'code' in payload && 'message' in payload) {
          throw new ApiError({
            code: payload.code,
            message: payload.message,
            status: error.response.status,
            fieldErrors: payload.fieldErrors,
            requestId: payload.requestId
          })
        }
      } catch (parseError) {
        if (parseError instanceof ApiError) {
          throw parseError
        }
      }
    }
    throw toApiError(error as AxiosError<ApiErrorPayload>)
  }
}

function parseFilename(disposition: string | undefined): string | null {
  if (!disposition) {
    return null
  }
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (encoded?.[1]) {
    try {
      return decodeURIComponent(encoded[1])
    } catch {
      return encoded[1]
    }
  }
  const plain = /filename="?([^";]+)"?/i.exec(disposition)
  return plain?.[1] ?? null
}

export const api = {
  async get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await http.get<ApiResponse<T>>(url, config)
    return unwrap(response)
  },

  async post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response = await http.post<ApiResponse<T>>(url, data, config)
    return unwrap(response)
  },

  async put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response = await http.put<ApiResponse<T>>(url, data, config)
    return unwrap(response)
  },

  async patch<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response = await http.patch<ApiResponse<T>>(url, data, config)
    return unwrap(response)
  },

  async delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await http.delete<ApiResponse<T>>(url, config)
    return unwrap(response)
  }
}
