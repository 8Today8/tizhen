import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import type { PlayerProfile } from '@/api/account'
import { fetchPlayerMe } from '@/api/account'
import type { AdminSession } from '@/api/admin'

export type { AdminSession }
export type SessionUser = PlayerProfile

/**
 * 登录态只保存当前会话快照。真正的认证与权限校验始终由后端执行。
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref<PlayerProfile | null>(null)
  const admin = ref<AdminSession | null>(null)
  const playerHydrated = ref(false)
  let playerHydration: Promise<void> | null = null

  const isAuthenticated = computed(() => user.value !== null)
  const isAdminAuthenticated = computed(() => admin.value !== null)

  function setUser(next: PlayerProfile | null) {
    user.value = next
    playerHydrated.value = true
  }

  function setAdmin(next: AdminSession | null) {
    admin.value = next
  }

  /**
   * 页面刷新后通过 /me 恢复选手会话，整个应用生命周期内只执行一次。
   */
  async function hydratePlayer(): Promise<void> {
    if (playerHydrated.value) {
      return
    }
    if (playerHydration) {
      return playerHydration
    }

    playerHydration = (async () => {
      try {
        user.value = await fetchPlayerMe()
      } catch {
        // 未登录、会话过期或网络异常都按未登录处理；受保护页面会再次引导登录。
        user.value = null
      } finally {
        playerHydrated.value = true
        playerHydration = null
      }
    })()
    return playerHydration
  }

  function invalidatePlayer() {
    user.value = null
    playerHydrated.value = true
  }

  function clear() {
    user.value = null
    admin.value = null
  }

  return {
    user,
    admin,
    playerHydrated,
    isAuthenticated,
    isAdminAuthenticated,
    setUser,
    setAdmin,
    hydratePlayer,
    invalidatePlayer,
    clear
  }
})
