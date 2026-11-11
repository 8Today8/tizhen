import type { ManualDeclaration } from '@/api/external'

/**
 * 榜单展示文案。后端返回稳定状态码，页面只做展示映射。
 */
export function rankToneClass(rank: number): string {
  if (rank === 1) {
    return 'lb-rank lb-rank--gold'
  }
  if (rank === 2) {
    return 'lb-rank lb-rank--silver'
  }
  if (rank === 3) {
    return 'lb-rank lb-rank--bronze'
  }
  return 'lb-rank'
}

export function cfSourceLabel(status: string | null): string | null {
  if (!status) {
    return null
  }
  const labels: Record<string, string> = {
    verified: 'CF 已验证',
    simulated: 'CF 模拟同步',
    unverified: 'CF 待校验'
  }
  return labels[status] ?? `CF ${status}`
}

export function manualSourceLabel(status: string | null): string | null {
  return status === 'verified' ? '手动已审核' : null
}

export function declarationStatusLabel(status: ManualDeclaration['verificationStatus']): string {
  const labels: Record<ManualDeclaration['verificationStatus'], string> = {
    unverified: '待审核',
    verified: '已通过',
    rejected: '已拒绝'
  }
  return labels[status]
}

export function declarationStatusType(
  status: ManualDeclaration['verificationStatus']
): 'success' | 'warning' | 'danger' {
  if (status === 'verified') {
    return 'success'
  }
  if (status === 'rejected') {
    return 'danger'
  }
  return 'warning'
}
