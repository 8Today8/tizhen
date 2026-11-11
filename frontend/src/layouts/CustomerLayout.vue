<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Moon, Sunny } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { fetchPlayerCsrf, logoutPlayer } from '@/api/account'
import { ApiError } from '@/api/http'
import { useTheme } from '@/composables/useTheme'
import { useAuthStore } from '@/stores/auth'
import { resolveApiErrorMessage } from '@/utils/apiError'

const { mode, toggleTheme } = useTheme()
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loggingOut = ref(false)

const primaryNav = [
  { label: '学习路线', to: '/learning' },
  { label: '题库', to: '/problems' },
  { label: '榜单', to: '/leaderboard' },
  { label: '1v1 对战', to: '/battle' },
  { label: 'AI 造数据', to: '/datagen' },
  { label: 'XCPC', to: '/xcpc' }
]

const moreNav = [
  { label: '个人中心', to: '/profile' },
  { label: '学习记录', to: '/records' },
  { label: '错题本', to: '/wrongbook' }
]

const activePath = computed(() => {
  const all = [...primaryNav, ...moreNav]
  return all.find((item) => route.path === item.to || route.path.startsWith(`${item.to}/`))?.to ?? ''
})

const userInitial = computed(() => authStore.user?.nickname.charAt(0) || '选')

function handleMore(command: string) {
  router.push(command)
}

function handleUserCommand(command: string) {
  if (command === 'logout') {
    void handleLogout()
    return
  }
  router.push(command)
}

async function handleLogout() {
  if (loggingOut.value) {
    return
  }
  try {
    await ElMessageBox.confirm('退出后需要重新登录才能继续使用账号功能。', '退出登录', {
      confirmButtonText: '退出登录',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  loggingOut.value = true
  try {
    try {
      await logoutPlayer()
    } catch (error) {
      // CSRF Token 过期时重新获取并重试一次，避免用户被卡在已登录状态。
      if (error instanceof ApiError && error.code === 'COMMON_CSRF_INVALID') {
        await fetchPlayerCsrf()
        await logoutPlayer()
      } else {
        throw error
      }
    }
    authStore.invalidatePlayer()
    ElMessage.success('已退出登录')
    await router.replace('/')
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '退出登录失败，请稍后重试'))
  } finally {
    loggingOut.value = false
  }
}
</script>

<template>
  <div class="customer-layout">
    <header class="site-header">
      <div class="site-header__inner">
        <RouterLink class="brand" to="/">
          <span class="brand__mark">阵</span>
          <span class="brand__text">
            <strong>题阵</strong>
            <small>Algorithm Training Ground</small>
          </span>
        </RouterLink>

        <nav class="site-nav" aria-label="主导航">
          <RouterLink
            v-for="item in primaryNav"
            :key="item.to"
            :to="item.to"
            class="site-nav__link"
            :class="{ 'is-active': activePath === item.to }"
          >
            {{ item.label }}
          </RouterLink>

          <el-dropdown trigger="hover" @command="handleMore">
            <button class="site-nav__link site-nav__more" type="button">
              更多
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="item in moreNav" :key="item.to" :command="item.to">
                  {{ item.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </nav>

        <div class="site-actions">
          <el-tooltip :content="mode === 'light' ? '切换到暗淡主题' : '切换到明亮主题'">
            <el-button
              circle
              :icon="mode === 'light' ? Moon : Sunny"
              aria-label="切换主题"
              @click="toggleTheme"
            />
          </el-tooltip>

          <el-dropdown
            v-if="authStore.user"
            trigger="click"
            :disabled="loggingOut"
            @command="handleUserCommand"
          >
            <button class="site-user__trigger" type="button">
              <el-avatar
                :size="30"
                :src="authStore.user.avatarUrl || undefined"
                class="site-user__avatar"
              >
                {{ userInitial }}
              </el-avatar>
              <span class="site-user__name">{{ authStore.user.nickname }}</span>
              <el-icon class="site-user__chevron"><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="/records">学习记录</el-dropdown-item>
                <el-dropdown-item command="/wrongbook">错题本</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-button v-else type="primary" plain @click="router.push('/login')">登录</el-button>
        </div>
      </div>
    </header>

    <main class="site-main">
      <RouterView v-slot="{ Component }">
        <Transition name="fade" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>

    <footer class="site-footer">
      <div class="site-footer__inner">
        <span>题阵 · 算法训练与对战平台</span>
        <span class="site-footer__meta">Java 21 · Spring Boot 3.5 · Vue 3 全栈实现</span>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.customer-layout {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
}

.site-header {
  position: sticky;
  top: 0;
  z-index: 20;
  border-bottom: 1px solid var(--tz-color-border);
  background: var(--tz-header-bg);
  backdrop-filter: blur(14px);
}

.site-header__inner {
  display: flex;
  width: min(1200px, 100% - 40px);
  min-height: var(--tz-header-height);
  margin: 0 auto;
  align-items: center;
  gap: 28px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--tz-color-text);
  text-decoration: none;
}

.brand__mark {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 12px;
  background: linear-gradient(140deg, var(--tz-color-primary), var(--tz-color-accent));
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  box-shadow: var(--tz-shadow-sm);
}

.brand__text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.brand__text strong {
  font-size: 18px;
  letter-spacing: 0.08em;
}

.brand__text small {
  color: var(--tz-color-text-muted);
  font-size: 10px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.site-nav {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 4px;
}

.site-nav__link {
  flex: none;
  padding: 8px 12px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--tz-color-text-secondary);
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  text-decoration: none;
  white-space: nowrap;
  transition:
    background-color 0.18s ease,
    color 0.18s ease;
}

.site-nav__link:hover {
  background: var(--tz-color-surface-muted);
  color: var(--tz-color-text);
}

.site-nav__link.is-active {
  background: var(--tz-color-primary-soft);
  color: var(--tz-color-primary);
  font-weight: 600;
}

.site-nav__more {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.site-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.site-user__trigger {
  display: inline-flex;
  height: 38px;
  padding: 3px 10px 3px 4px;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--tz-color-border);
  border-radius: var(--tz-radius-pill);
  background: var(--tz-color-surface);
  color: var(--tz-color-text);
  cursor: pointer;
  font: inherit;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.site-user__trigger:hover {
  border-color: var(--tz-color-border-strong);
  box-shadow: var(--tz-shadow-sm);
}

.site-user__avatar {
  flex: none;
  background: linear-gradient(140deg, var(--tz-color-primary), var(--tz-color-accent));
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.site-user__name {
  max-width: 110px;
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.site-user__chevron {
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

.site-main {
  width: min(1200px, 100% - 40px);
  margin: 0 auto;
  padding: 36px 0 64px;
  flex: 1;
}

.site-footer {
  border-top: 1px solid var(--tz-color-border);
  background: var(--tz-color-bg-raised);
}

.site-footer__inner {
  display: flex;
  width: min(1200px, 100% - 40px);
  margin: 0 auto;
  padding: 22px 0;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--tz-color-text-muted);
  font-size: 13px;
}

@media (max-width: 900px) {
  .site-header__inner {
    flex-wrap: wrap;
    gap: 12px;
    padding: 12px 0;
  }

  .site-nav {
    order: 3;
    width: 100%;
    overflow-x: auto;
  }

  .site-user__name {
    display: none;
  }

  .site-user__trigger {
    padding-right: 8px;
  }

  .site-main {
    padding-top: 24px;
  }

  .site-footer__inner {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
