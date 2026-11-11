import { api } from './http'

/**
 * M02 外部成绩接入：CF 绑定与手动申报。
 * 端点定义见 docs/design/04.API接口设计.md §6.2 EXT-01～EXT-03、DEC-01～DEC-03。
 */
export type ExternalPlatform = 'codeforces'

export interface ExternalAccount {
  id: string
  platform: string
  handle: string
  profileUrl: string | null
  verificationStatus: 'verified' | 'simulated' | 'unverified'
  dataSource: 'simulated' | 'api'
  weeklyCount: number
  weeklyWeekStart: string | null
  allTimeCount: number
  lastSyncAt: string | null
  status: 'active' | 'unbound'
  unboundAt: string | null
  createdAt: string
}

export interface ExternalAccountList {
  items: ExternalAccount[]
}

export interface BindExternalAccountPayload {
  platform: ExternalPlatform
  handle: string
  profileUrl?: string | null
}

export interface ManualDeclaration {
  id: string
  platform: string
  account: string
  profileUrl: string | null
  count: number
  weekStartDate: string
  verificationStatus: 'unverified' | 'verified' | 'rejected'
  createdAt: string
  updatedAt: string
}

export interface ManualDeclarationPageData {
  items: ManualDeclaration[]
  page: number
  pageSize: number
  total: number
  totalPages: number
  weekStartDate: string
}

export interface ManualDeclarationQuery {
  page?: number
  pageSize?: number
}

export interface UpsertManualDeclarationPayload {
  platform: string
  account: string
  profileUrl?: string | null
  count: number
}

export function fetchMyExternalAccounts(): Promise<ExternalAccountList> {
  return api.get<ExternalAccountList>('/me/external-accounts')
}

export function bindExternalAccount(
  payload: BindExternalAccountPayload
): Promise<ExternalAccount> {
  return api.post<ExternalAccount>('/me/external-accounts', payload)
}

export function unbindExternalAccount(accountId: string): Promise<void> {
  return api.delete<void>(`/me/external-accounts/${accountId}`)
}

export function fetchMyManualDeclarations(
  query: ManualDeclarationQuery = {}
): Promise<ManualDeclarationPageData> {
  return api.get<ManualDeclarationPageData>('/me/manual-declarations', { params: query })
}

export function upsertManualDeclaration(
  payload: UpsertManualDeclarationPayload
): Promise<ManualDeclaration> {
  return api.put<ManualDeclaration>('/me/manual-declarations', payload)
}

export function deleteManualDeclaration(declarationId: string): Promise<void> {
  return api.delete<void>(`/me/manual-declarations/${declarationId}`)
}
