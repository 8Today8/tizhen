import { ElMessage } from 'element-plus'

/**
 * 布局骨架阶段的统一操作反馈。
 * 业务接口接入前，操作入口给出明确提示，避免点击后无任何响应。
 */
export function usePendingFeature() {
  function notifyPending(feature: string) {
    ElMessage.info(`${feature}将在对应业务模块接入后开放`)
  }

  return { notifyPending }
}
