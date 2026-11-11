import type { SubmissionStatus, SubmissionVerdict } from '@/api/problem'

const STATUS_LABELS: Record<SubmissionStatus, string> = {
  pending: '等待判题',
  judging: '判题中',
  grading: '评分中',
  finished: '已完成',
  failed: '处理失败'
}

const VERDICT_LABELS: Record<SubmissionVerdict, string> = {
  AC: '通过',
  WA: '答案错误',
  TLE: '运行超时',
  MLE: '内存超限',
  RE: '运行错误',
  CE: '编译错误',
  SYSTEM_ERROR: '系统错误'
}

const COMPACT_VERDICT_LABELS: Record<SubmissionVerdict, string> = {
  AC: 'AC',
  WA: 'WA',
  TLE: 'TLE',
  MLE: 'MLE',
  RE: 'RE',
  CE: 'CE',
  SYSTEM_ERROR: 'SE'
}

export function submissionStatusLabel(status: SubmissionStatus): string {
  return STATUS_LABELS[status] ?? status
}

export function verdictLabel(verdict: SubmissionVerdict | null): string {
  if (!verdict) {
    return '—'
  }
  return VERDICT_LABELS[verdict] ?? verdict
}

export function compactVerdictLabel(verdict: SubmissionVerdict | null): string {
  if (!verdict) {
    return '—'
  }
  return COMPACT_VERDICT_LABELS[verdict] ?? verdict
}

export function statusTagType(
  status: SubmissionStatus
): 'success' | 'info' | 'warning' | 'danger' | 'primary' {
  if (status === 'finished') {
    return 'success'
  }
  if (status === 'failed') {
    return 'danger'
  }
  return 'warning'
}

export function verdictTagType(
  verdict: SubmissionVerdict | null
): 'success' | 'info' | 'warning' | 'danger' | 'primary' {
  if (verdict === 'AC') {
    return 'success'
  }
  if (verdict === null) {
    return 'info'
  }
  if (verdict === 'WA' || verdict === 'SYSTEM_ERROR') {
    return 'danger'
  }
  return 'warning'
}

export function isInProgressStatus(status: SubmissionStatus): boolean {
  return status === 'pending' || status === 'judging' || status === 'grading'
}

export function isTerminalStatus(status: SubmissionStatus): boolean {
  return status === 'finished' || status === 'failed'
}

export function languageLabel(language: string | null): string {
  if (language === 'cpp17') {
    return 'C++17'
  }
  if (language === 'python310') {
    return 'Python 3.10'
  }
  return '—'
}
