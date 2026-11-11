import { api } from './http'

/**
 * M01 选手账号 API。
 * 端点定义见 docs/design/04.API接口设计.md §6.1.1。
 */
export interface PlayerProfile {
  id: string
  username: string
  nickname: string
  email: string | null
  phone: string | null
  avatarUrl: string | null
  school: string | null
  status: 'active' | 'disabled'
  lastLoginAt: string | null
  createdAt: string
}

export interface PlayerSession {
  user: PlayerProfile
  csrfToken: string
}

export interface PlayerCsrfToken {
  token: string
  headerName: string
  cookieName: string
}

export interface PlayerLoginPayload {
  username: string
  password: string
}

export interface PlayerRegisterPayload {
  username: string
  nickname: string
  password: string
  confirmPassword: string
}

export interface UpdatePlayerProfilePayload {
  nickname?: string
  email?: string
  phone?: string | null
  avatarUrl?: string | null
  school?: string | null
}

export interface PlayerStatsPeriod {
  rank: number | null
  score: number
}

export interface PlayerStats {
  learning: {
    completedStages: number
    totalStages: number
  }
  submissions: {
    total: number
    ojAcCount: number
    objectiveAnswered: number
    shortAnswerAnswered: number
  }
  wrongBook: {
    activeCount: number
  }
  battles: {
    total: number
    wins: number
    losses: number
    draws: number
  }
  leaderboard: {
    week: PlayerStatsPeriod
    allTime: PlayerStatsPeriod
  }
}

export function fetchPlayerCsrf(): Promise<PlayerCsrfToken> {
  return api.get<PlayerCsrfToken>('/auth/csrf')
}

export function loginPlayer(payload: PlayerLoginPayload): Promise<PlayerSession> {
  return api.post<PlayerSession>('/auth/login', payload)
}

export function registerPlayer(payload: PlayerRegisterPayload): Promise<PlayerSession> {
  return api.post<PlayerSession>('/auth/register', payload)
}

export function logoutPlayer(): Promise<void> {
  return api.post<void>('/auth/logout')
}

export function fetchPlayerMe(): Promise<PlayerProfile> {
  return api.get<PlayerProfile>('/me')
}

export function updatePlayerProfile(
  payload: UpdatePlayerProfilePayload
): Promise<PlayerProfile> {
  return api.patch<PlayerProfile>('/me', payload)
}

export function fetchPlayerStats(): Promise<PlayerStats> {
  return api.get<PlayerStats>('/me/stats')
}

/**
 * 上传头像并立即绑定到当前账号，返回最新资料。
 */
export function uploadPlayerAvatar(file: File): Promise<PlayerProfile> {
  const formData = new FormData()
  formData.append('file', file)
  return api.post<PlayerProfile>('/me/avatar', formData)
}
