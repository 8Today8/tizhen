import type { PageResult } from '@/types/api'

import { api } from './http'

/**
 * M08 AI 造数据 API。
 * 端点定义见 docs/design/04.API接口设计.md §6.1.2、§6.8。
 */
export type DataGenMode = 'random' | 'llm_generator' | 'llm_direct_data'
export type DataGenTaskStatus =
  | 'pending'
  | 'llm_generating'
  | 'llm_data_generating'
  | 'generating'
  | 'success'
  | 'failed'
export type DataGenStdLanguage = 'cpp17' | 'python310'
export type DataRangeType = 'int' | 'long long' | 'double'
export type LlmCredentialStatus = 'valid' | 'invalid'

export interface LlmProvider {
  provider: string
  name: string
  defaultModel: string
  models: string[]
}

export interface LlmCredential {
  configured: boolean
  provider: string | null
  providerName: string | null
  model: string | null
  apiKeyMasked: string | null
  status: LlmCredentialStatus | null
  lastValidatedAt: string | null
  lastValidationError: string | null
  hasActiveTask: boolean
}

export interface DataGenRange {
  min: number
  max: number
}

export interface DataGenLimits {
  groupCount: DataGenRange
  timeLimitMs: DataGenRange
  memoryLimitMb: DataGenRange
}

export interface DataGenModeOption {
  code: DataGenMode
  requiresCredential: boolean
}

export interface DataGenEnumOption {
  value: string
  label: string
}

export interface DataGenOptions {
  modes: DataGenModeOption[]
  stdLanguages: DataGenEnumOption[]
  dataRangeTypes: DataRangeType[]
  limits: DataGenLimits
  llmCredential: LlmCredential
  hasActiveTask: boolean
}

export interface DataGenRangePayload {
  name: string
  type: DataRangeType
  min: string
  max: string
}

export interface CreateDataGenTaskPayload {
  title: string
  statement: string
  inputFormat: string
  outputFormat: string
  stdCode: string
  stdLanguage: DataGenStdLanguage
  dataRange: DataGenRangePayload[]
  groupCount: number
  generationMode: DataGenMode
  specialRequirements?: string
  timeLimitMs: number
  memoryLimitMb: number
}

export interface DataGenFile {
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

export interface DataGenTaskListItem {
  id: string
  title: string
  generationMode: DataGenMode
  stdLanguage: DataGenStdLanguage
  groupCount: number
  status: DataGenTaskStatus
  llmProvider: string | null
  llmModel: string | null
  createdAt: string
  startedAt: string | null
  finishedAt: string | null
  filesExpireAt: string
  filesCleanedAt: string | null
}

export interface DataGenTaskDetail {
  id: string
  title: string
  statement: string
  inputFormat: string
  outputFormat: string
  stdCode: string
  stdLanguage: DataGenStdLanguage
  dataRange: Array<{ name: string; type: DataRangeType; min: number | string; max: number | string }> | null
  groupCount: number
  generationMode: DataGenMode
  specialRequirements: string | null
  llmProvider: string | null
  llmModel: string | null
  generatorStatus: string
  llmDataStatus: string
  status: DataGenTaskStatus
  failureStage: string | null
  failureReason: string | null
  resultSummary: Record<string, unknown> | null
  timeLimitMs: number
  memoryLimitMb: number
  startedAt: string | null
  finishedAt: string | null
  filesExpireAt: string
  filesCleanedAt: string | null
  files: DataGenFile[]
}

export interface DataGenTaskQuery {
  status?: string
  generationMode?: string
  page?: number
  pageSize?: number
}

export interface SaveLlmCredentialPayload {
  provider: string
  apiKey: string
  model?: string
}

export function fetchDataGenOptions(): Promise<DataGenOptions> {
  return api.get<DataGenOptions>('/datagen/options')
}

export function createDataGenTask(
  payload: CreateDataGenTaskPayload
): Promise<DataGenTaskDetail> {
  return api.post<DataGenTaskDetail>('/datagen/tasks', payload)
}

export function fetchDataGenTasks(
  params: DataGenTaskQuery
): Promise<PageResult<DataGenTaskListItem>> {
  return api.get<PageResult<DataGenTaskListItem>>('/datagen/tasks', { params })
}

export function fetchDataGenTask(taskId: string): Promise<DataGenTaskDetail> {
  return api.get<DataGenTaskDetail>(`/datagen/tasks/${taskId}`)
}

export function fetchLlmProviders(): Promise<LlmProvider[]> {
  return api
    .get<{ items: LlmProvider[] }>('/llm-providers')
    .then((result) => result.items)
}

export function fetchLlmCredential(): Promise<LlmCredential> {
  return api.get<LlmCredential>('/me/llm-credential')
}

export function saveLlmCredential(
  payload: SaveLlmCredentialPayload
): Promise<LlmCredential> {
  return api.put<LlmCredential>('/me/llm-credential', payload)
}

export function deleteLlmCredential(): Promise<void> {
  return api.delete<void>('/me/llm-credential')
}

export function datagenFileDownloadUrl(
  taskId: string,
  file: DataGenFile,
  kind: 'input' | 'output'
): string {
  return `/datagen/tasks/${taskId}/files/${file.id}/${kind}`
}

export function datagenArchiveDownloadUrl(taskId: string): string {
  return `/datagen/tasks/${taskId}/files/archive`
}
