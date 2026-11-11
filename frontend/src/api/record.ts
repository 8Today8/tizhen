import type { PageResult } from '@/types/api'

import type { ProblemType } from './problem'
import { api } from './http'

/**
 * M06 学习记录与错题本 API。
 * 端点定义见 docs/design/04.API接口设计.md §6.6 REC-01～REC-02。
 */
export interface WrongBookProblemBrief {
  id: string
  type: ProblemType
  title: string
  difficulty: string
}

export interface WrongBookItem {
  id: string
  problem: WrongBookProblemBrief
  latestError: string
  latestScore: number | null
  errorCount: number
  lastErrorAt: string
}

export interface WrongBookQuery {
  problemType?: ProblemType | ''
  keyword?: string
  page?: number
  pageSize?: number
}

export function fetchWrongBook(
  query: WrongBookQuery = {}
): Promise<PageResult<WrongBookItem>> {
  return api.get<PageResult<WrongBookItem>>('/records/wrong-book', {
    params: Object.fromEntries(
      Object.entries(query).filter(
        ([, value]) => value !== undefined && value !== '' && value !== null
      )
    )
  })
}

export function removeWrongBook(wrongBookId: string): Promise<void> {
  return api.delete<void>(`/records/wrong-book/${wrongBookId}`)
}
