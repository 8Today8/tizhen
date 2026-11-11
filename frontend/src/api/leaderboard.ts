import { api } from './http'

/**
 * M05 选手端榜单 API。
 * 端点定义见 docs/design/04.API接口设计.md §6.5 LB-01。
 */
export type LeaderboardPeriod = 'week' | 'all'

export interface LeaderboardUser {
  id: string
  nickname: string
  avatarUrl: string | null
  school: string | null
}

export interface LeaderboardItem {
  rank: number
  user: LeaderboardUser
  totalScore: number
  internalSolved: number
  cfSolved: number
  manualSolved: number
  cfVerificationStatus: string | null
  cfDataSource: string | null
  cfLastSyncAt: string | null
  manualVerificationStatus: string | null
  manualUpdatedAt: string | null
  isMe: boolean
}

export interface LeaderboardPageData {
  period: LeaderboardPeriod
  weekStartDate: string | null
  generatedAt: string
  items: LeaderboardItem[]
  me: LeaderboardItem | null
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface LeaderboardQuery {
  period: LeaderboardPeriod
  page?: number
  pageSize?: number
}

export function fetchLeaderboard(query: LeaderboardQuery): Promise<LeaderboardPageData> {
  return api.get<LeaderboardPageData>('/leaderboard', { params: query })
}
