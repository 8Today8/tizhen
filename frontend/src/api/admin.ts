import type { PageResult } from '@/types/api'

import { api } from './http'

/**
 * M01 / M05 / M08 / M09 后台接口。端点定义见 docs/design/04.API接口设计.md。
 */
export interface AdminSession {
  id: string
  username: string
  displayName: string
  avatarUrl: string | null
  roleCode: 'super_admin' | 'content_operator'
  roleName: string
  permissions: string[]
  csrfToken: string
}

export interface AdminCsrfToken {
  token: string
  headerName: string
  cookieName: string
}

export interface AdminLoginPayload {
  username: string
  password: string
}

export interface UserBrief {
  id: string
  username: string | null
  nickname: string | null
  avatarUrl: string | null
  school: string | null
}

export interface AdminUserSummary {
  id: string
  username: string
  nickname: string
  email: string | null
  phone: string | null
  avatarUrl: string | null
  school: string | null
  cfHandle: string | null
  status: 'active' | 'disabled'
  lastLoginAt: string | null
  createdAt: string
}

export interface AdminExternalAccountView {
  id: string
  platform: string
  handle: string
  profileUrl: string | null
  verificationStatus: string
  dataSource: string
  weeklyCount: number
  weeklyWeekStart: string | null
  allTimeCount: number
  lastSyncAt: string | null
  status: string
  unboundAt: string | null
  createdAt: string
}

export interface AdminLlmCredentialView {
  configured: boolean
  provider: string | null
  providerName: string | null
  model: string | null
  status: string | null
  lastValidatedAt: string | null
  lastValidationError: string | null
}

export interface AdminUserDetail {
  user: AdminUserSummary
  cfAccounts: AdminExternalAccountView[]
  llmCredential: AdminLlmCredentialView | null
  dataGenTaskCount: number
  submissionCount: number
  firstAcCount: number
  wrongBookCount: number
  battleCount: number
}

export interface AdminUserQuery {
  keyword?: string
  status?: string
  page?: number
  pageSize?: number
}

export interface AdminCreateUserPayload {
  username: string
  nickname: string
  password: string
  email?: string
  phone?: string
  school?: string
}

export interface AdminLeaderboardUser {
  id: string
  username: string
  nickname: string
  avatarUrl: string | null
  school: string | null
  email: string | null
  cfHandle: string | null
}

export interface LeaderboardItem {
  rank: number
  user: AdminLeaderboardUser
  totalScore: number
  internalSolved: number
  cfSolved: number
  manualSolved: number
  cfVerificationStatus: string | null
  cfDataSource: string | null
  cfLastSyncAt: string | null
  manualVerificationStatus: string | null
  manualUpdatedAt: string | null
}

export interface AdminLeaderboardPage {
  period: 'week' | 'all'
  weekStartDate: string | null
  generatedAt: string
  items: LeaderboardItem[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface AdminLeaderboardQuery {
  period: 'week' | 'all'
  page?: number
  pageSize?: number
}

export interface AdminManualDeclarationItem {
  id: string
  user: UserBrief
  username: string | null
  platform: string
  account: string
  profileUrl: string | null
  count: number
  weekStartDate: string
  verificationStatus: string
  reviewNote: string | null
  reviewedBy: string | null
  reviewedByName: string | null
  reviewedAt: string | null
  activeFlag: boolean
  createdAt: string
  updatedAt: string
}

export interface AdminManualDeclarationQuery {
  userId?: string
  platform?: string
  weekStartDate?: string
  page?: number
  pageSize?: number
}

export interface ReviewAdminManualDeclarationPayload {
  status: 'verified' | 'rejected'
  note?: string | null
}

export interface CfSyncAccepted {
  accepted: boolean
  accountCount: number
  acceptedAt: string
}

export interface DataGenFileView {
  id: string
  groupIndex: number
  inputSizeBytes: number | null
  outputSizeBytes: number | null
  stdTimeMs: number | null
  validationStatus: string
  validationMessage: string | null
  inputAvailable: boolean
  outputAvailable: boolean
  fileDeletedAt: string | null
}

export interface AdminDataGenTaskListItem {
  id: string
  title: string
  user: UserBrief
  username: string | null
  generationMode: string
  stdLanguage: string
  groupCount: number
  status: string
  llmProvider: string | null
  llmModel: string | null
  createdAt: string
  startedAt: string | null
  finishedAt: string | null
  filesExpireAt: string
  filesCleanedAt: string | null
}

export interface DataRangeItem {
  name: string
  type: string
  min: number
  max: number
}

export interface AdminDataGenTaskDetail {
  id: string
  title: string
  user: UserBrief
  username: string | null
  statement: string
  inputFormat: string
  outputFormat: string
  stdCode: string
  stdLanguage: string
  dataRange: DataRangeItem[] | null
  groupCount: number
  generationMode: string
  specialRequirements: string | null
  llmProvider: string | null
  llmModel: string | null
  generatorStatus: string
  llmDataStatus: string
  status: string
  failureStage: string | null
  failureReason: string | null
  resultSummary: Record<string, unknown> | null
  timeLimitMs: number
  memoryLimitMb: number
  startedAt: string | null
  finishedAt: string | null
  filesExpireAt: string
  filesCleanedAt: string | null
  files: DataGenFileView[]
}

export interface AdminDataGenTaskQuery {
  userId?: string
  status?: string
  generationMode?: string
  page?: number
  pageSize?: number
}

export interface RoleView {
  id: string
  code: string
  name: string
  permissions: string[]
}

export interface AdminAccountView {
  id: string
  username: string
  displayName: string
  avatarUrl: string | null
  roleCode: string
  roleName: string
  status: 'active' | 'disabled'
  lastLoginAt: string | null
  createdAt: string
}

export interface AdminAccountQuery {
  keyword?: string
  status?: string
  roleCode?: string
  page?: number
  pageSize?: number
}

export interface CreateAdminAccountPayload {
  username: string
  displayName: string
  password: string
  roleCode: string
}

export interface AdminLogUser {
  id: string
  username: string | null
  displayName: string | null
  roleCode: string | null
}

export interface OperationLogView {
  id: string
  admin: AdminLogUser
  action: string
  targetType: string
  targetId: string | null
  detail: string | null
  extra: Record<string, unknown> | null
  ipAddress: string | null
  userAgent: string | null
  requestId: string | null
  createdAt: string
}

export interface OperationLogQuery {
  adminId?: string
  action?: string
  targetType?: string
  targetId?: string
  from?: string
  to?: string
  page?: number
  pageSize?: number
}

export function fetchAdminCsrf(): Promise<AdminCsrfToken> {
  return api.get<AdminCsrfToken>('/admin/auth/csrf')
}

export function loginAdmin(payload: AdminLoginPayload): Promise<AdminSession> {
  return api.post<AdminSession>('/admin/auth/login', payload)
}

export function logoutAdmin(): Promise<void> {
  return api.post<void>('/admin/auth/logout')
}

export function fetchAdminMe(): Promise<AdminSession> {
  return api.get<AdminSession>('/admin/me')
}

export function fetchAdminUsers(params: AdminUserQuery): Promise<PageResult<AdminUserSummary>> {
  return api.get<PageResult<AdminUserSummary>>('/admin/users', { params })
}

export function fetchAdminUserDetail(userId: string): Promise<AdminUserDetail> {
  return api.get<AdminUserDetail>(`/admin/users/${userId}`)
}

export function createAdminUser(payload: AdminCreateUserPayload): Promise<AdminUserSummary> {
  return api.post<AdminUserSummary>('/admin/users', payload)
}

export function updateAdminUserStatus(
  userId: string,
  status: 'active' | 'disabled'
): Promise<AdminUserSummary> {
  return api.patch<AdminUserSummary>(`/admin/users/${userId}/status`, { status })
}

export function batchUpdateAdminUserStatus(
  userIds: string[],
  status: 'active' | 'disabled'
): Promise<void> {
  return api.patch<void>('/admin/users/batch/status', { userIds, status })
}

export function batchDeleteAdminUsers(userIds: string[]): Promise<void> {
  return api.delete<void>('/admin/users', { data: { userIds } })
}

export function resetAdminUserPassword(userId: string, newPassword: string): Promise<void> {
  return api.post<void>(`/admin/users/${userId}/reset-password`, { newPassword })
}

export function fetchAdminLeaderboard(
  params: AdminLeaderboardQuery
): Promise<AdminLeaderboardPage> {
  return api.get<AdminLeaderboardPage>('/admin/leaderboard', { params })
}

export function fetchAdminManualDeclarations(
  params: AdminManualDeclarationQuery
): Promise<PageResult<AdminManualDeclarationItem>> {
  return api.get<PageResult<AdminManualDeclarationItem>>('/admin/manual-declarations', { params })
}

export function reviewAdminManualDeclaration(
  declarationId: string,
  payload: ReviewAdminManualDeclarationPayload
): Promise<AdminManualDeclarationItem> {
  return api.patch<AdminManualDeclarationItem>(
    `/admin/manual-declarations/${declarationId}/review`,
    payload
  )
}

export function syncCfAccounts(): Promise<CfSyncAccepted> {
  return api.post<CfSyncAccepted>('/admin/external-accounts/sync-cf')
}

export function fetchAdminDatagenTasks(
  params: AdminDataGenTaskQuery
): Promise<PageResult<AdminDataGenTaskListItem>> {
  return api.get<PageResult<AdminDataGenTaskListItem>>('/admin/datagen/tasks', { params })
}

export function fetchAdminDatagenTask(taskId: string): Promise<AdminDataGenTaskDetail> {
  return api.get<AdminDataGenTaskDetail>(`/admin/datagen/tasks/${taskId}`)
}

export function fetchAdminRoles(): Promise<RoleView[]> {
  return api.get<{ items: RoleView[] }>('/admin/roles').then((result) => result.items)
}

export function fetchAdminAccounts(
  params: AdminAccountQuery
): Promise<PageResult<AdminAccountView>> {
  return api.get<PageResult<AdminAccountView>>('/admin/admin-accounts', { params })
}

export function createAdminAccount(
  payload: CreateAdminAccountPayload
): Promise<AdminAccountView> {
  return api.post<AdminAccountView>('/admin/admin-accounts', payload)
}

export function updateAdminAccountStatus(
  adminId: string,
  status: 'active' | 'disabled'
): Promise<AdminAccountView> {
  return api.patch<AdminAccountView>(`/admin/admin-accounts/${adminId}/status`, { status })
}

export function resetAdminAccountPassword(adminId: string, newPassword: string): Promise<void> {
  return api.post<void>(`/admin/admin-accounts/${adminId}/reset-password`, { newPassword })
}

export function fetchOperationLogs(
  params: OperationLogQuery
): Promise<PageResult<OperationLogView>> {
  return api.get<PageResult<OperationLogView>>('/admin/operation-logs', { params })
}

export function fetchOperationLog(logId: string): Promise<OperationLogView> {
  return api.get<OperationLogView>(`/admin/operation-logs/${logId}`)
}
