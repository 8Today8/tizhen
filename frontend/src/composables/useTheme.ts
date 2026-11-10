import { storeToRefs } from 'pinia'

import { useThemeStore, type ThemeMode } from '@/stores/theme'

/**
 * 主题组合式函数：页面只通过它读取模式与切换主题，不直接操作 DOM。
 */
export function useTheme() {
  const themeStore = useThemeStore()
  const { mode } = storeToRefs(themeStore)

  function toggleTheme() {
    themeStore.toggle()
  }

  function setTheme(next: ThemeMode) {
    themeStore.apply(next)
  }

  function initializeTheme() {
    themeStore.initialize()
  }

  return { mode, toggleTheme, setTheme, initializeTheme }
}
