import { beforeEach, describe, expect, it } from 'vitest'

import { readCookie } from '@/utils/cookie'

describe('readCookie', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
  })

  it('读取指定的 Cookie', () => {
    document.cookie = 'XSRF-TOKEN=token-value'
    expect(readCookie('XSRF-TOKEN')).toBe('token-value')
  })

  it('Cookie 不存在时返回 null', () => {
    expect(readCookie('MISSING-TOKEN')).toBeNull()
  })
})
