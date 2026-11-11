const BUSINESS_TIME_ZONE = 'Asia/Shanghai'

const dateTimeFormatter = new Intl.DateTimeFormat('en-CA', {
  timeZone: BUSINESS_TIME_ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hourCycle: 'h23'
})

/**
 * 后端时间统一为 UTC ISO 8601，展示层固定转换为 Asia/Shanghai。
 */
export function formatDateTime(isoString: string): string {
  const parts = dateTimeFormatter.formatToParts(new Date(isoString))
  const pick = (type: Intl.DateTimeFormatPartTypes): string =>
    parts.find((part) => part.type === type)?.value ?? ''

  return `${pick('year')}-${pick('month')}-${pick('day')} ${pick('hour')}:${pick('minute')}:${pick('second')}`
}

/**
 * 课程时长以秒存储，展示为易读的中文时长。
 */
export function formatDuration(seconds: number | null): string {
  if (seconds === null || seconds === undefined || seconds < 0) {
    return '时长未知'
  }
  if (seconds < 60) {
    return `${seconds} 秒`
  }

  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) {
    return minutes > 0 ? `${hours} 小时 ${minutes} 分钟` : `${hours} 小时`
  }
  return `${minutes} 分钟`
}
