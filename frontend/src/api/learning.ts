import type { PageResult } from '@/types/api'

import { api } from './http'

/**
 * M03 学习内容与进度 API。
 * 端点定义见 docs/design/04.API接口设计.md §6.3。
 */
export interface CourseView {
  id: string
  title: string
  url: string
  durationSeconds: number | null
  source: string | null
  sortOrder: number
}

export interface RouteSummary {
  id: string
  name: string
  description: string | null
  coverUrl: string | null
  sortOrder: number
  stageCount: number
  completedStageCount: number
  progressPercent: number
}

export interface StageSummary {
  id: string
  name: string
  sortOrder: number
  completed: boolean
  completedAt: string | null
  courseCount: number
  problemCount: number
  courses: CourseView[]
}

export interface RouteDetail extends RouteSummary {
  stages: StageSummary[]
}

export interface StageProblemSummary {
  id: string
  type: string
  objectiveType: string | null
  title: string
  difficulty: string
  source: string | null
  platform: string | null
  externalUrl: string | null
}

export interface StageProblemView {
  problem: StageProblemSummary
  sortOrder: number
  externalCompleted: boolean | null
  completedAt: string | null
}

export interface StageDetail {
  id: string
  routeId: string
  routeName: string
  name: string
  content: string | null
  completed: boolean
  completedAt: string | null
  courses: CourseView[]
  problems: StageProblemView[]
}

export interface StageProgressView {
  stageId: string
  completed: boolean
  completedAt: string | null
}

export interface ExternalCompletionView {
  problemId: string
  completed: boolean
  completionSource: string
  completedAt: string | null
}

export interface LearningRouteQuery {
  page?: number
  pageSize?: number
}

export function fetchLearningRoutes(
  query: LearningRouteQuery = {}
): Promise<PageResult<RouteSummary>> {
  return api.get<PageResult<RouteSummary>>('/learning/routes', { params: query })
}

export function fetchLearningRoute(routeId: string): Promise<RouteDetail> {
  return api.get<RouteDetail>(`/learning/routes/${routeId}`)
}

export function fetchLearningStage(stageId: string): Promise<StageDetail> {
  return api.get<StageDetail>(`/learning/stages/${stageId}`)
}

export function updateStageCompletion(
  stageId: string,
  completed: boolean
): Promise<StageProgressView> {
  return api.put<StageProgressView>(`/learning/stages/${stageId}/progress`, { completed })
}

export function updateExternalProblemCompletion(
  problemId: string,
  completed: boolean
): Promise<ExternalCompletionView> {
  return api.put<ExternalCompletionView>(`/learning/external-problems/${problemId}/completion`, {
    completed
  })
}
