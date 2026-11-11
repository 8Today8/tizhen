import type { PageResult } from '@/types/api'

import { api } from './http'

/**
 * M04 题库、提交与判题 API。
 * 端点定义见 docs/design/04.API接口设计.md §6.4 PRB-01～PRB-04、SUB-01～SUB-03。
 */
export type ProblemType = 'oj' | 'objective' | 'short_answer' | 'external'
export type ObjectiveType = 'judge' | 'single' | 'multiple'
export type SubmissionStatus = 'pending' | 'judging' | 'grading' | 'finished' | 'failed'
export type SubmissionVerdict = 'AC' | 'WA' | 'TLE' | 'MLE' | 'RE' | 'CE' | 'SYSTEM_ERROR'
export type OjLanguage = 'cpp17' | 'python310'

export interface ProblemSummary {
  id: string
  type: ProblemType
  objectiveType: ObjectiveType | null
  title: string
  difficulty: string
  source: string | null
  platform: string | null
  externalUrl: string | null
  tags: string[]
  solvedByMe: boolean
  attemptedByMe: boolean
  externalCompleted: boolean | null
}

export interface ProblemSample {
  inputData: string
  outputData: string
  explanation: string | null
  sortOrder: number
}

export interface ProblemOption {
  optionKey: string
  content: string
  sortOrder: number
}

export interface MyProblemProgress {
  attemptCount: number
  firstAc: boolean
  firstAcAt: string | null
  lastVerdict: SubmissionVerdict | null
  lastSubmittedAt: string | null
}

export interface ProblemDetail {
  id: string
  type: ProblemType
  objectiveType: ObjectiveType | null
  title: string
  statement: string
  inputFormat: string | null
  outputFormat: string | null
  difficulty: string
  source: string | null
  tags: string[]
  timeLimitMs: number | null
  memoryLimitMb: number | null
  maxScore: number | null
  externalUrl: string | null
  platform: string | null
  externalCompleted: boolean | null
  samples: ProblemSample[]
  options: ProblemOption[]
  myProgress: MyProblemProgress
}

export interface ProblemTagOption {
  value: string
  count: number
}

export interface ProblemTagListResponse {
  items: ProblemTagOption[]
}

export interface SubmissionProblemBrief {
  id: string
  type: ProblemType
  title: string
}

export interface SubmissionListItem {
  id: string
  problem: SubmissionProblemBrief
  battleId: string | null
  language: OjLanguage | null
  status: SubmissionStatus
  verdict: SubmissionVerdict | null
  score: number | null
  createdAt: string
  judgedAt: string | null
}

export interface ScoringRubricItem {
  point: string
  score: number
  description: string | null
}

export interface SubmissionDetail {
  id: string
  problem: SubmissionProblemBrief
  battleId: string | null
  language: OjLanguage | null
  code: string | null
  answerJson: string[] | null
  answerText: string | null
  status: SubmissionStatus
  verdict: SubmissionVerdict | null
  score: number | null
  aiFeedback: string | null
  failureReason: string | null
  judgeDetail: string | null
  compileOutput: string | null
  judgeTimeMs: number | null
  memoryKb: number | null
  judgeStartedAt: string | null
  judgedAt: string | null
  createdAt: string
  firstAc: boolean
  correctOptions: string[] | null
  explanation: string | null
  referenceAnswer: string | null
  scoringRubric: ScoringRubricItem[] | null
}

export interface ProblemQuery {
  type?: ProblemType | ''
  objectiveType?: ObjectiveType | ''
  difficulty?: string
  source?: string
  tag?: string
  keyword?: string
  page?: number
  pageSize?: number
}

export interface RandomProblemQuery {
  type?: ProblemType | ''
  difficulty?: string
  tag?: string
}

export interface SubmissionListQuery {
  problemId?: string
  problemType?: string
  status?: SubmissionStatus | ''
  page?: number
  pageSize?: number
}

export interface OjSubmissionPayload {
  language: OjLanguage
  code: string
}

export interface ObjectiveSubmissionPayload {
  selectedOptions: string[]
}

export interface ShortAnswerSubmissionPayload {
  answerText: string
}

export type SubmissionPayload =
  | OjSubmissionPayload
  | ObjectiveSubmissionPayload
  | ShortAnswerSubmissionPayload

export function fetchProblems(query: ProblemQuery = {}): Promise<PageResult<ProblemSummary>> {
  return api.get<PageResult<ProblemSummary>>('/problems', { params: cleanQuery(query) })
}

export function fetchProblemTags(): Promise<ProblemTagListResponse> {
  return api.get<ProblemTagListResponse>('/problems/tags')
}

export function fetchRandomProblem(query: RandomProblemQuery = {}): Promise<ProblemSummary> {
  return api.get<ProblemSummary>('/problems/random', { params: cleanQuery(query) })
}

export function fetchProblem(problemId: string): Promise<ProblemDetail> {
  return api.get<ProblemDetail>(`/problems/${problemId}`)
}

export function createSubmission(
  problemId: string,
  payload: SubmissionPayload
): Promise<SubmissionDetail> {
  return api.post<SubmissionDetail>(`/problems/${problemId}/submissions`, payload)
}

export function fetchSubmissions(
  query: SubmissionListQuery = {}
): Promise<PageResult<SubmissionListItem>> {
  return api.get<PageResult<SubmissionListItem>>('/submissions', {
    params: cleanQuery(query)
  })
}

export function fetchSubmission(submissionId: string): Promise<SubmissionDetail> {
  return api.get<SubmissionDetail>(`/submissions/${submissionId}`)
}

function cleanQuery(query: object): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(query).filter(([, value]) => value !== undefined && value !== '' && value !== null)
  )
}
