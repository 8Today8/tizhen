import { ref } from 'vue'
import { defineStore } from 'pinia'

import { readStorage, writeStorage } from '@/utils/storage'

export type ThemeMode = 'light' | 'dusk'

const THEME_STORAGE_KEY = 'tizhen.theme'

function readInitialMode(): ThemeMode {
  return readStorage(THEME_STORAGE_KEY) === 'dusk' ? 'dusk' : 'light'
}

/**
 * 明亮（奶油糖果色）与暗淡（ dusk ）双主题。Element Plus 暗色变量通过 .dark 类启用，
 * 设计令牌通过 data-theme 覆盖。
 */
export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>(readInitialMode())

  function apply(next: ThemeMode) {
    mode.value = next
    const root = document.documentElement
    root.dataset.theme = next
    root.classList.toggle('dark', next === 'dusk')
    writeStorage(THEME_STORAGE_KEY, next)
  }

  function toggle() {
    apply(mode.value === 'light' ? 'dusk' : 'light')
  }

  function initialize() {
    apply(mode.value)
  }

  return { mode, apply, toggle, initialize }
})
