import type { BattleEndReason, BattleResult, BattleStatus } from '@/api/battle'

const STATUS_LABELS: Record<BattleStatus, string> = {
  waiting: '等待对手',
  ready: '双方已准备',
  ongoing: '对战中',
  finished: '已结束',
  cancelled: '已取消'
}

const RESULT_LABELS: Record<BattleResult, string> = {
  win: '胜利',
  loss: '失败',
  draw: '平局'
}

const END_REASON_LABELS: Record<BattleEndReason, string> = {
  first_ac: '先 AC 获胜',
  timeout: '时间耗尽',
  surrender: '认输',
  disconnect: '断线判负',
  cancelled: '房主取消'
}

export function battleStatusLabel(status: BattleStatus | null): string {
  return status ? STATUS_LABELS[status] ?? status : '—'
}

export function battleStatusTagType(
  status: BattleStatus
): 'success' | 'info' | 'warning' | 'danger' | 'primary' {
  if (status === 'ongoing') {
    return 'primary'
  }
  if (status === 'ready') {
    return 'success'
  }
  if (status === 'finished') {
    return 'info'
  }
  if (status === 'cancelled') {
    return 'danger'
  }
  return 'warning'
}

export function battleResultLabel(result: BattleResult | null): string {
  return result ? RESULT_LABELS[result] ?? result : '—'
}

export function battleResultTagType(
  result: BattleResult | null
): 'success' | 'info' | 'warning' | 'danger' | 'primary' {
  if (result === 'win') {
    return 'success'
  }
  if (result === 'loss') {
    return 'danger'
  }
  return 'info'
}

export function battleEndReasonLabel(reason: BattleEndReason | null): string {
  return reason ? END_REASON_LABELS[reason] ?? reason : '—'
}

export function battleDifficultyLabel(difficulty: string | null): string {
  if (difficulty === 'entry') {
    return '入门'
  }
  if (difficulty === 'basic') {
    return '基础'
  }
  if (difficulty === 'advanced') {
    return '进阶'
  }
  return '不限难度'
}

export function formatCountdown(totalSeconds: number): string {
  const safe = Math.max(0, Math.floor(totalSeconds))
  const hours = Math.floor(safe / 3600)
  const minutes = Math.floor((safe % 3600) / 60)
  const seconds = safe % 60
  const mm = String(minutes).padStart(2, '0')
  const ss = String(seconds).padStart(2, '0')
  return hours > 0 ? `${hours}:${mm}:${ss}` : `${mm}:${ss}`
}
