import type { DataGenMode, DataGenStdLanguage } from '@/api/datagen'

const MODE_LABELS: Record<DataGenMode, string> = {
  random: '随机生成',
  llm_generator: 'LLM 构造器',
  llm_direct_data: 'LLM 直接生成'
}

const STATUS_LABELS: Record<string, string> = {
  pending: '等待中',
  llm_generating: 'LLM 构造中',
  llm_data_generating: 'LLM 生成数据中',
  generating: '生成中',
  success: '已完成',
  failed: '失败',
  not_applicable: '不适用',
  ready: '就绪',
  validating: '校验中'
}

const FAILURE_STAGE_LABELS: Record<string, string> = {
  llm_generator: 'LLM 构造器生成',
  llm_data: 'LLM 数据生成',
  generator: '构造器执行',
  validation: '数据校验',
  std: '标准程序',
  system: '系统处理'
}

const VALIDATION_STATUS_LABELS: Record<string, string> = {
  passed: '通过',
  failed: '未通过',
  pending: '待校验'
}

export function generationModeLabel(mode: string): string {
  return MODE_LABELS[mode as DataGenMode] ?? mode
}

export function datagenStatusLabel(status: string): string {
  return STATUS_LABELS[status] ?? status
}

export function failureStageLabel(stage: string | null): string {
  if (!stage) {
    return '未知阶段'
  }
  return FAILURE_STAGE_LABELS[stage] ?? stage
}

export function validationStatusLabel(status: string): string {
  return VALIDATION_STATUS_LABELS[status] ?? status
}

export function stdLanguageLabel(language: DataGenStdLanguage | string): string {
  if (language === 'cpp17') {
    return 'C++17'
  }
  if (language === 'python310') {
    return 'Python 3.10'
  }
  return language
}

export function isRunningStatus(status: string): boolean {
  return ['pending', 'llm_generating', 'llm_data_generating', 'generating'].includes(status)
}

export function statusTagType(
  status: string
): 'success' | 'danger' | 'warning' | 'info' {
  if (status === 'success' || status === 'ready' || status === 'valid') {
    return 'success'
  }
  if (status === 'failed' || status === 'invalid') {
    return 'danger'
  }
  if (status === 'pending' || status === 'not_applicable') {
    return 'info'
  }
  return 'warning'
}

export function formatFileSize(bytes: number | null): string {
  if (bytes === null || bytes === undefined) {
    return '—'
  }
  if (bytes < 1024) {
    return `${bytes} B`
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  }
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

export function validationTagType(status: string): 'success' | 'danger' | 'info' {
  if (status === 'passed') {
    return 'success'
  }
  if (status === 'failed') {
    return 'danger'
  }
  return 'info'
}
