/**
 * 后台枚举展示文案。接口仍返回稳定编码，页面只负责本地化展示。
 */
const ACTION_LABELS: Record<string, string> = {
  login: '登录',
  create: '新增',
  update: '修改',
  delete: '删除',
  publish: '发布',
  offline: '下架',
  enable: '启用',
  disable: '禁用',
  reset_password: '重置密码',
  refresh_cf: '刷新 CF',
  review: '审核'
}

const TARGET_TYPE_LABELS: Record<string, string> = {
  admin_user: '管理员账号',
  user: '选手用户',
  route: '学习路线',
  stage: '学习阶段',
  course: '阶段课程',
  problem: '题目',
  external_account: '外部账号',
  manual_declaration: '手动申报',
  data_gen_task: '造数据任务',
  leaderboard: '榜单'
}

const DATAGEN_STATUS_LABELS: Record<string, string> = {
  pending: '等待中',
  llm_generating: 'LLM 构造中',
  llm_data_generating: 'LLM 生成数据中',
  generating: '生成中',
  ready: '就绪',
  success: '成功',
  failed: '失败',
  not_applicable: '不适用'
}

const FAILURE_STAGE_LABELS: Record<string, string> = {
  llm_generator: 'LLM 构造器',
  llm_data: 'LLM 数据生成',
  generator: '生成器执行',
  validation: '数据校验',
  std: '标准程序',
  system: '系统处理'
}

const GENERATION_MODE_LABELS: Record<string, string> = {
  random: '随机生成',
  llm_generator: 'LLM 构造器',
  llm_direct_data: 'LLM 直接生成'
}

const VERIFICATION_LABELS: Record<string, string> = {
  verified: '已验证',
  simulated: '模拟同步',
  unverified: '未校验',
  rejected: '已拒绝'
}

const MANUAL_VERIFICATION_LABELS: Record<string, string> = {
  verified: '已通过',
  unverified: '未校验',
  rejected: '已拒绝'
}

const ROLE_LABELS: Record<string, string> = {
  super_admin: '超级管理员',
  content_operator: '内容运营'
}

const VALIDATION_LABELS: Record<string, string> = {
  pending: '待校验',
  passed: '通过',
  failed: '失败'
}

export function actionLabel(action: string): string {
  return ACTION_LABELS[action] ?? action
}

export function targetTypeLabel(targetType: string): string {
  return TARGET_TYPE_LABELS[targetType] ?? targetType
}

export function datagenStatusLabel(status: string): string {
  return DATAGEN_STATUS_LABELS[status] ?? status
}

export function generationModeLabel(mode: string): string {
  return GENERATION_MODE_LABELS[mode] ?? mode
}

export function failureStageLabel(stage: string | null): string {
  if (!stage) {
    return '未知'
  }
  return FAILURE_STAGE_LABELS[stage] ?? stage
}

export function verificationLabel(status: string | null): string {
  if (!status) {
    return '—'
  }
  return VERIFICATION_LABELS[status] ?? status
}

export function manualVerificationLabel(status: string | null): string {
  if (!status) {
    return '—'
  }
  return MANUAL_VERIFICATION_LABELS[status] ?? status
}

export function roleLabel(code: string | null): string {
  if (!code) {
    return '—'
  }
  return ROLE_LABELS[code] ?? code
}

export function validationStatusLabel(status: string): string {
  return VALIDATION_LABELS[status] ?? status
}

export function formatBytes(bytes: number | null): string {
  if (bytes === null || bytes === undefined) {
    return '—'
  }
  if (bytes < 1024) {
    return `${bytes} B`
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  }
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}
