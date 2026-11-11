import type { PageResult } from '@/types/api'
import type {
  OjLanguage,
  ProblemType,
  SubmissionDetail,
  SubmissionListItem,
  SubmissionVerdict
} from '@/api/problem'

import { api } from './http'

/**
 * M07 选手端 1v1 对战 API。
 * 端点定义见 docs/design/04.API接口设计.md §6.7 BAT-01～BAT-11。
 */
export type BattleStatus = 'waiting' | 'ready' | 'ongoing' | 'finished' | 'cancelled'
export type BattleResult = 'win' | 'loss' | 'draw'
export type BattleEndReason =
  | 'first_ac'
  | 'timeout'
  | 'surrender'
  | 'disconnect'
  | 'cancelled'

export interface BattleUser {
  id: string
  username: string | null
  nickname: string
  avatarUrl: string | null
  school: string | null
}

export interface BattlePlayer {
  user: BattleUser
  ready: boolean
  online: boolean
  submissionCount: number
  lastVerdict: SubmissionVerdict | null
}

export interface BattleProblem {
  id: string
  type: ProblemType
  title: string
  difficulty: string
  tags: string[]
}

export interface BattleRoom {
  id: string
  roomCode: string
  status: BattleStatus
  creator: BattlePlayer
  opponent: BattlePlayer | null
  filterDifficulty: string | null
  filterTag: string | null
  creatorReady: boolean
  opponentReady: boolean
  problem: BattleProblem | null
  startedAt: string | null
  endsAt: string | null
  endedAt: string | null
  resultForMe: BattleResult | null
  endReason: BattleEndReason | null
  createdAt: string
}

export interface BattleRoomPreview {
  roomCode: string
  status: BattleStatus
  creator: BattleUser
  opponent: BattleUser | null
  canJoin: boolean
  createdAt: string
}

export interface BattleRecordProblem {
  id: string
  title: string
}

export interface BattleRecordItem {
  roomId: string
  roomCode: string
  opponent: BattleUser | null
  problem: BattleRecordProblem | null
  resultForMe: BattleResult
  endReason: BattleEndReason | null
  startedAt: string | null
  endedAt: string | null
  durationSeconds: number | null
}

export interface BattleRecordDetail extends BattleRecordItem {
  mySubmissions: SubmissionListItem[]
  opponentSubmissions: SubmissionListItem[]
}

export interface CreateBattleRoomPayload {
  filterDifficulty?: string | null
  filterTag?: string | null
}

export interface BattleRecordQuery {
  result?: BattleResult | ''
  page?: number
  pageSize?: number
}

export function createBattleRoom(payload: CreateBattleRoomPayload): Promise<BattleRoom> {
  return api.post<BattleRoom>('/battles/rooms', payload)
}

export function fetchBattleRoomByCode(roomCode: string): Promise<BattleRoomPreview> {
  return api.get<BattleRoomPreview>(`/battles/rooms/by-code/${encodeURIComponent(roomCode)}`)
}

export function joinBattleRoom(roomCode: string): Promise<BattleRoom> {
  return api.post<BattleRoom>('/battles/rooms/join', { roomCode })
}

export function fetchBattleRoom(roomId: string): Promise<BattleRoom> {
  return api.get<BattleRoom>(`/battles/rooms/${roomId}`)
}

export function setBattleReady(roomId: string, ready: boolean): Promise<BattleRoom> {
  return api.post<BattleRoom>(`/battles/rooms/${roomId}/ready`, { ready })
}

export function startBattle(roomId: string): Promise<BattleRoom> {
  return api.post<BattleRoom>(`/battles/rooms/${roomId}/start`)
}

export function createBattleSubmission(
  roomId: string,
  payload: { language: OjLanguage; code: string }
): Promise<SubmissionDetail> {
  return api.post(`/battles/rooms/${roomId}/submissions`, payload)
}

export function surrenderBattle(roomId: string): Promise<BattleRoom> {
  return api.post<BattleRoom>(`/battles/rooms/${roomId}/surrender`)
}

export function cancelBattleRoom(roomId: string): Promise<void> {
  return api.delete<void>(`/battles/rooms/${roomId}`)
}

export function fetchBattleRecords(
  query: BattleRecordQuery = {}
): Promise<PageResult<BattleRecordItem>> {
  return api.get<PageResult<BattleRecordItem>>('/battles/records', {
    params: Object.fromEntries(
      Object.entries(query).filter(
        ([, value]) => value !== undefined && value !== '' && value !== null
      )
    )
  })
}

export function fetchBattleRecord(roomId: string): Promise<BattleRecordDetail> {
  return api.get<BattleRecordDetail>(`/battles/records/${roomId}`)
}
