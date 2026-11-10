export interface FieldErrorItem {
  field: string
  message: string
}

export interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
  fieldErrors?: FieldErrorItem[] | null
  requestId: string
  timestamp: string
}

export interface ApiErrorPayload {
  success: false
  code: string
  message: string
  data?: null
  fieldErrors?: FieldErrorItem[] | null
  requestId?: string
  timestamp?: string
}

export interface PageResult<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface PageQuery {
  page?: number
  pageSize?: number
  keyword?: string
}
