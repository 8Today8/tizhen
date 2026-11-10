<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Clock,
  Collection,
  Document,
  EditPen,
  Expand,
  Fold,
  Guide,
  Link,
  MagicStick,
  Monitor,
  Moon,
  Odometer,
  Setting,
  Sunny,
  SwitchButton,
  Trophy,
  User,
  UserFilled
} from '@element-plus/icons-vue'
import type { MenuInstance } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchAdminCsrf, logoutAdmin } from '@/api/admin'
import { ApiError } from '@/api/http'
import { useTheme } from '@/composables/useTheme'
import { useAuthStore } from '@/stores/auth'

interface AdminMenuItem {
  path: string
  label: string
  icon: Component
}

interface AdminMenuGroup {
  key: string
  title: string
  icon: Component
  items: AdminMenuItem[]
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { mode, toggleTheme } = useTheme()

const collapsed = ref(false)
const mobileOpen = ref(false)
const isMobile = ref(false)
const loggingOut = ref(false)

const adminName = computed(() => authStore.admin?.displayName ?? '管理员')
const adminRoleName = computed(() => authStore.admin?.roleName ?? '后台账号')
const adminInitial = computed(() => adminName.value.slice(0, 1))

const baseMenuGroups: AdminMenuGroup[] = [
  {
    key: 'overview',
    title: '概览',
    icon: Odometer,
    items: [{ path: '/admin', label: '运营工作台', icon: Odometer }]
  },
  {
    key: 'content',
    title: '内容',
    icon: Guide,
    items: [
      { path: '/admin/routes', label: '路线管理', icon: Guide },
      { path: '/admin/stages', label: '阶段管理', icon: Collection }
    ]
  },
  {
    key: 'problems',
    title: '题库',
    icon: Monitor,
    items: [
      { path: '/admin/problems', label: 'OJ 题库', icon: Monitor },
      { path: '/admin/objective', label: '客观题', icon: Document },
      { path: '/admin/short', label: '简答题', icon: EditPen },
      { path: '/admin/external', label: '外链题', icon: Link }
    ]
  },
  {
    key: 'operations',
    title: '运营',
    icon: Trophy,
    items: [
      { path: '/admin/users', label: '用户管理', icon: User },
      { path: '/admin/leaderboard', label: '榜单管理', icon: Trophy },
      { path: '/admin/datagen', label: '造数据任务', icon: MagicStick }
    ]
  },
  {
    key: 'system',
    title: '系统',
    icon: Setting,
    items: [
      { path: '/admin/roles', label: '角色与管理员', icon: UserFilled },
      { path: '/admin/logs', label: '操作日志', icon: Clock }
    ]
  }
]

// 角色固定的前提下只做菜单级隐藏；接口权限仍由后端校验。
const menuGroups = computed<AdminMenuGroup[]>(() => {
  const isSuperAdmin = authStore.admin?.roleCode === 'super_admin'
  return baseMenuGroups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => isSuperAdmin || item.path !== '/admin/roles')
    }))
    .filter((group) => group.items.length > 0)
})

const menuRef = ref<MenuInstance>()
// 刷新进入后台时分组保持收起，避免侧边栏一进来就是全量展开状态。
const defaultOpenedGroups: string[] = []

const pageTitle = computed(() => route.meta.title ?? '运营管理')
const activeMenu = computed(() => {
  const items = menuGroups.value.flatMap((group) => group.items)
  return items.find((item) => route.path === item.path)?.path ?? route.path
})
const sidebarClosed = computed(() => (isMobile.value ? !mobileOpen.value : collapsed.value))

let mediaQuery: MediaQueryList | null = null

function syncMediaState(matches: boolean) {
  isMobile.value = matches
  if (!matches) {
    mobileOpen.value = false
  }
}

function handleMediaChange(event: MediaQueryListEvent) {
  syncMediaState(event.matches)
}

onMounted(() => {
  mediaQuery = window.matchMedia('(max-width: 900px)')
  syncMediaState(mediaQuery.matches)
  mediaQuery.addEventListener('change', handleMediaChange)

  // Element Plus 挂载时会自动展开当前路由所在分组，这里统一收起。
  nextTick(() => {
    menuGroups.value.forEach((group) => menuRef.value?.close(group.key))
  })
})

onBeforeUnmount(() => {
  mediaQuery?.removeEventListener('change', handleMediaChange)
})

watch(
  () => route.fullPath,
  () => {
    mobileOpen.value = false
  }
)

// 通过快捷入口等外部跳转进入页面时，自动展开其所在分组。
watch(
  () => route.path,
  (path) => {
    const group = menuGroups.value.find((item) => item.items.some((menu) => menu.path === path))
    if (group) {
      menuRef.value?.open(group.key)
    }
  }
)

function toggleSidebar() {
  if (isMobile.value) {
    mobileOpen.value = !mobileOpen.value
    return
  }
  collapsed.value = !collapsed.value
}

async function handleLogout() {
  if (loggingOut.value) {
    return
  }

  try {
    await ElMessageBox.confirm(
      '退出后需要重新输入管理员账号和密码才能进入后台，确认退出吗？',
      '退出登录',
      {
        confirmButtonText: '确认退出',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    // 取消或关闭确认框时不执行退出。
    return
  }

  loggingOut.value = true
  try {
    try {
      await logoutAdmin()
    } catch (error) {
      // CSRF Token 失效时刷新一次并重试；会话已结束时无需重试。
      if (error instanceof ApiError && error.code === 'COMMON_CSRF_INVALID') {
        await fetchAdminCsrf()
        await logoutAdmin()
      } else {
        throw error
      }
    }
  } catch (error) {
    const sessionEnded = error instanceof ApiError
      && (error.status === 401 || error.code === 'AUTH_SESSION_EXPIRED')
    if (!sessionEnded) {
      ElMessage.error('退出登录失败，请稍后重试')
      loggingOut.value = false
      return
    }
  } finally {
    loggingOut.value = false
  }

  authStore.setAdmin(null)
  ElMessage.success('已安全退出后台')
  await router.replace({ name: 'admin-login' })
}
</script>

<template>
  <div class="admin-layout">
    <Transition name="fade">
      <div v-if="isMobile && mobileOpen" class="admin-overlay" @click="mobileOpen = false" />
    </Transition>

    <aside
      class="admin-sidebar"
      :class="{ 'is-collapsed': collapsed && !isMobile, 'is-open': mobileOpen }"
    >
      <RouterLink class="admin-brand" to="/admin" @click="mobileOpen = false">
        <span class="admin-brand__mark">阵</span>
        <span v-show="!collapsed || isMobile" class="admin-brand__text">
          <strong>题阵后台</strong>
          <small>Operations Console</small>
        </span>
      </RouterLink>

      <el-menu
        ref="menuRef"
        class="admin-menu"
        :default-active="activeMenu"
        :default-openeds="defaultOpenedGroups"
        :collapse="collapsed && !isMobile"
        :collapse-transition="false"
        router
      >
        <el-sub-menu v-for="group in menuGroups" :key="group.key" :index="group.key">
          <template #title>
            <el-icon v-if="collapsed && !isMobile" class="admin-group__icon">
              <component :is="group.icon" />
            </el-icon>
            <span>{{ group.title }}</span>
          </template>
          <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.label }}</template>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>

      <div class="admin-user">
        <el-avatar :size="32" class="admin-user__avatar">{{ adminInitial }}</el-avatar>
        <div v-show="!collapsed || isMobile" class="admin-user__info">
          <strong>{{ adminName }}</strong>
          <span>{{ adminRoleName }}</span>
        </div>
        <el-button
          v-show="!collapsed || isMobile"
          text
          circle
          :icon="SwitchButton"
          :loading="loggingOut"
          aria-label="退出登录"
          @click="handleLogout"
        />
      </div>
    </aside>

    <div class="admin-body">
      <header class="admin-header">
        <div class="admin-header__left">
          <el-button
            text
            :icon="sidebarClosed ? Expand : Fold"
            :aria-label="sidebarClosed ? '展开侧边栏' : '折叠侧边栏'"
            @click="toggleSidebar"
          />
          <el-breadcrumb class="admin-breadcrumb" separator="/">
            <el-breadcrumb-item :to="{ path: '/admin' }">运营后台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="admin-header__actions">
          <el-tooltip :content="mode === 'light' ? '切换到暗淡主题' : '切换到明亮主题'">
            <el-button
              circle
              :icon="mode === 'light' ? Moon : Sunny"
              aria-label="切换主题"
              @click="toggleTheme"
            />
          </el-tooltip>
          <el-button
            class="admin-logout-button"
            plain
            :icon="SwitchButton"
            :loading="loggingOut"
            aria-label="退出登录"
            @click="handleLogout"
          >
            退出登录
          </el-button>
          <el-tag class="admin-role-tag" type="info" effect="plain">{{ adminRoleName }}</el-tag>
        </div>
      </header>

      <main class="admin-main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--tz-color-bg);
}

.admin-sidebar {
  display: flex;
  width: 232px;
  flex: none;
  height: 100vh;
  flex-direction: column;
  border-right: 1px solid var(--tz-color-border);
  background: var(--tz-color-bg-raised);
  transition:
    width 0.2s ease,
    transform 0.2s ease;
}

.admin-sidebar.is-collapsed {
  width: 64px;
}

.admin-brand {
  display: flex;
  height: var(--tz-header-height);
  padding: 0 16px;
  align-items: center;
  gap: 10px;
  overflow: hidden;
  border-bottom: 1px solid var(--tz-color-border);
  color: var(--tz-color-text);
  text-decoration: none;
}

.admin-brand__mark {
  display: grid;
  width: 32px;
  height: 32px;
  flex: none;
  place-items: center;
  border-radius: 10px;
  background: linear-gradient(140deg, var(--tz-color-primary), var(--tz-color-accent));
  color: #fff;
  font-weight: 700;
}

.admin-brand__text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  white-space: nowrap;
}

.admin-brand__text strong {
  font-size: 15px;
}

.admin-brand__text small {
  color: var(--tz-color-text-muted);
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.admin-menu {
  flex: 1;
  overflow-y: auto;
  border-right: none;
  background: transparent;
}

.admin-menu :deep(.el-sub-menu__title) {
  height: 38px;
  margin: 4px 10px 2px;
  padding: 0 14px 0 20px;
  border-radius: 8px;
  color: var(--tz-color-text-secondary);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.04em;
  line-height: 38px;
}

.admin-menu :deep(.el-sub-menu__title:hover) {
  background: var(--tz-color-surface-muted);
  color: var(--tz-color-text);
}

.admin-menu :deep(.el-sub-menu__icon-arrow) {
  right: 14px;
  font-size: 12px;
}

.admin-sidebar.is-collapsed .admin-menu :deep(.el-sub-menu__title) {
  justify-content: center;
  margin: 2px 10px;
  padding: 0;
}

.admin-sidebar.is-collapsed .admin-menu :deep(.el-sub-menu__title span),
.admin-sidebar.is-collapsed .admin-menu :deep(.el-sub-menu__icon-arrow) {
  display: none;
}

.admin-sidebar.is-collapsed .admin-group__icon {
  font-size: 17px;
}

.admin-menu :deep(.el-menu-item) {
  height: 42px;
  margin: 2px 10px;
  border-radius: 10px;
  color: var(--tz-color-text-secondary);
}

.admin-menu :deep(.el-menu-item:hover) {
  background: var(--tz-color-surface-muted);
  color: var(--tz-color-text);
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: var(--tz-color-primary-soft);
  color: var(--tz-color-primary);
  font-weight: 600;
}

.admin-menu :deep(.el-menu-item .el-icon) {
  font-size: 17px;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-top: 1px solid var(--tz-color-border);
}

.admin-user__avatar {
  flex: none;
  background: var(--tz-color-primary-soft);
  color: var(--tz-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.admin-user__info {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  line-height: 1.3;
}

.admin-user__info strong {
  font-size: 13px;
}

.admin-user__info span {
  color: var(--tz-color-text-muted);
  font-size: 11px;
}

.admin-sidebar.is-collapsed .admin-user {
  justify-content: center;
  padding: 12px 8px;
}

.admin-body {
  display: flex;
  min-width: 0;
  height: 100vh;
  flex: 1;
  flex-direction: column;
}

.admin-header {
  display: flex;
  min-height: var(--tz-header-height);
  padding: 0 24px;
  flex: none;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--tz-color-border);
  background: var(--tz-header-bg);
  backdrop-filter: blur(14px);
}

.admin-header__left,
.admin-header__actions {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.admin-header__left {
  flex: 1;
}

.admin-breadcrumb {
  min-width: 0;
}

.admin-breadcrumb :deep(.el-breadcrumb__inner) {
  color: var(--tz-color-text-muted);
  font-weight: 400;
}

.admin-breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--tz-color-text);
  font-weight: 600;
}

.admin-breadcrumb :deep(.el-breadcrumb__inner.is-link:hover) {
  color: var(--tz-color-primary);
}

.admin-main {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px 40px;
}

.admin-logout-button:hover,
.admin-logout-button:focus-visible {
  border-color: var(--tz-color-danger);
  color: var(--tz-color-danger);
}

.admin-overlay {
  position: fixed;
  inset: 0;
  z-index: 55;
  background: rgba(20, 21, 26, 0.38);
  backdrop-filter: blur(2px);
}

@media (max-width: 900px) {
  .admin-sidebar,
  .admin-sidebar.is-collapsed {
    position: fixed;
    inset: 0 auto 0 0;
    z-index: 60;
    width: 248px;
    transform: translateX(-100%);
    box-shadow: var(--tz-shadow-md);
  }

  .admin-sidebar.is-open {
    transform: translateX(0);
  }

  .admin-header {
    padding: 0 16px;
  }

  .admin-main {
    padding: 20px 16px 32px;
  }

  .admin-role-tag {
    display: none;
  }
}

@media (max-width: 640px) {
  .admin-header__left {
    gap: 4px;
  }

  .admin-breadcrumb :deep(.el-breadcrumb__item:first-child) {
    display: none;
  }

  .admin-header__actions :deep(.el-button span) {
    display: none;
  }
}
</style>
