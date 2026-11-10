function getLocalStorage(): Storage | null {
  if (typeof window === 'undefined') {
    return null
  }
  try {
    return window.localStorage
  } catch {
    // 浏览器禁用存储时按无存储处理，不中断页面。
    return null
  }
}

export function readStorage(key: string): string | null {
  try {
    return getLocalStorage()?.getItem(key) ?? null
  } catch {
    return null
  }
}

export function writeStorage(key: string, value: string): void {
  try {
    getLocalStorage()?.setItem(key, value)
  } catch {
    // 写入失败（隐私模式、配额不足）不影响当前会话。
  }
}

export function removeStorage(key: string): void {
  try {
    getLocalStorage()?.removeItem(key)
  } catch {
    // 同上。
  }
}
