<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Moon, Sunny } from '@element-plus/icons-vue'
import { useTheme } from '@/composables/useTheme'

const { mode, toggleTheme } = useTheme()
const route = useRoute()
const router = useRouter()

const primaryNav = [
  { label: '学习路线', to: '/learning' },
  { label: '题库', to: '/problems' },
  { label: '榜单', to: '/leaderboard' },
  { label: '1v1 对战', to: '/battle' },
  { label: 'AI 造数据', to: '/datagen' }
]

const moreNav = [
  { label: '学习记录', to: '/records' },
  { label: '错题本', to: '/wrongbook' },
  { label: '个人中心', to: '/profile' },
  { label: 'XCPC', to: '/xcpc' }
]

const activePath = computed(() => {
  const all = [...primaryNav, ...moreNav]
  return all.find((item) => route.path === item.to || route.path.startsWith(`${item.to}/`))?.to ?? ''
})

function handleMore(command: string) {
  router.push(command)
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
          <el-button type="primary" plain @click="router.push('/login')">登录</el-button>
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
        <span class="site-footer__meta">当前为工程骨架版本，业务功能按模块逐步接入。</span>
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
  padding: 8px 12px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--tz-color-text-secondary);
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  text-decoration: none;
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

  .site-main {
    padding-top: 24px;
  }

  .site-footer__inner {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
