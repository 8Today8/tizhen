import { describe, expect, it } from 'vitest'

import { formatDateTime } from '@/utils/format'

describe('formatDateTime', () => {
  it('将 UTC 时间转换为 Asia/Shanghai 展示', () => {
    expect(formatDateTime('2026-01-01T00:00:00Z')).toBe('2026-01-01 08:00:00')
  })

  it('跨日转换保持正确', () => {
    expect(formatDateTime('2026-06-30T18:30:00Z')).toBe('2026-07-01 02:30:00')
  })
})
