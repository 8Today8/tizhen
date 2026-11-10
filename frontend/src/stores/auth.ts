import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import type { AdminSession } from '@/api/admin'

export type { AdminSession }

export interface SessionUser {
  id: string
  username: string
  nickname: string
  avatarUrl: string | null
  school: string | null
}

/**
 * 登录态只保存当前会话快照。真正的认证与权限校验始终由后端执行。
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref<SessionUser | null>(null)
  const admin = ref<AdminSession | null>(null)

  const isAuthenticated = computed(() => user.value !== null)
  const isAdminAuthenticated = computed(() => admin.value !== null)

  function setUser(next: SessionUser | null) {
    user.value = next
  }

  function setAdmin(next: AdminSession | null) {
    admin.value = next
  }

  function clear() {
    user.value = null
    admin.value = null
  }

  return {
    user,
    admin,
    isAuthenticated,
    isAdminAuthenticated,
    setUser,
    setAdmin,
    clear
  }
})
