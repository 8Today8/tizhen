import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { fetchAdminMe } from '@/api/admin'
import AdminLayout from '@/layouts/AdminLayout.vue'
import AuthLayout from '@/layouts/AuthLayout.vue'
import CustomerLayout from '@/layouts/CustomerLayout.vue'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    requiresAuth?: boolean
    requiresAdmin?: boolean
    requiresSuperAdmin?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: CustomerLayout,
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/home/HomeView.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'learning',
        name: 'learning',
        component: () => import('@/views/learning/LearningRoutesView.vue'),
        meta: { title: '学习路线' }
      },
      {
        path: 'problems',
        name: 'problems',
        component: () => import('@/views/problems/ProblemsView.vue'),
        meta: { title: '题库' }
      },
      {
        path: 'leaderboard',
        name: 'leaderboard',
        component: () => import('@/views/leaderboard/LeaderboardView.vue'),
        meta: { title: '榜单' }
      },
      {
        path: 'battle',
        name: 'battle',
        component: () => import('@/views/battle/BattleLobbyView.vue'),
        meta: { title: '对战' }
      },
      {
        path: 'datagen',
        name: 'datagen',
        component: () => import('@/views/datagen/DatagenWorkbenchView.vue'),
        meta: { title: 'AI 造数据' }
      },
      {
        path: 'records',
        name: 'records',
        component: () => import('@/views/records/LearningRecordsView.vue'),
        meta: { title: '学习记录' }
      },
      {
        path: 'wrongbook',
        name: 'wrongbook',
        component: () => import('@/views/records/WrongBookView.vue'),
        meta: { title: '错题本' }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/profile/ProfileView.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'xcpc',
        name: 'xcpc',
        component: () => import('@/views/xcpc/XcpcView.vue'),
        meta: { title: 'XCPC' }
      }
    ]
  },
  {
    path: '/login',
    component: AuthLayout,
    children: [
      {
        path: '',
        name: 'login',
        component: () => import('@/views/auth/LoginView.vue'),
        meta: { title: '选手登录' }
      }
    ]
  },
  {
    path: '/register',
    component: AuthLayout,
    children: [
      {
        path: '',
        name: 'register',
        component: () => import('@/views/auth/RegisterView.vue'),
        meta: { title: '注册账号' }
      }
    ]
  },
  {
    path: '/admin/login',
    component: AuthLayout,
    children: [
      {
        path: '',
        name: 'admin-login',
        component: () => import('@/views/auth/AdminLoginView.vue'),
        meta: { title: '后台登录' }
      }
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    children: [
      {
        path: '',
        name: 'admin-dashboard',
        component: () => import('@/views/admin/AdminDashboardView.vue'),
        meta: { title: '运营工作台', requiresAdmin: true }
      },
      {
        path: 'routes',
        name: 'admin-routes',
        component: () => import('@/views/admin/RouteManagementView.vue'),
        meta: { title: '路线管理', requiresAdmin: true }
      },
      {
        path: 'stages',
        name: 'admin-stages',
        component: () => import('@/views/admin/StageManagementView.vue'),
        meta: { title: '阶段管理', requiresAdmin: true }
      },
      {
        path: 'problems',
        name: 'admin-problems',
        component: () => import('@/views/admin/ProblemManagementView.vue'),
        meta: { title: 'OJ 题库管理', requiresAdmin: true }
      },
      {
        path: 'objective',
        name: 'admin-objective',
        component: () => import('@/views/admin/ObjectiveProblemManagementView.vue'),
        meta: { title: '客观题管理', requiresAdmin: true }
      },
      {
        path: 'short',
        name: 'admin-short',
        component: () => import('@/views/admin/ShortAnswerManagementView.vue'),
        meta: { title: '简答题管理', requiresAdmin: true }
      },
      {
        path: 'external',
        name: 'admin-external',
        component: () => import('@/views/admin/ExternalProblemManagementView.vue'),
        meta: { title: '外链题管理', requiresAdmin: true }
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/UserManagementView.vue'),
        meta: { title: '用户管理', requiresAdmin: true }
      },
      {
        path: 'leaderboard',
        name: 'admin-leaderboard',
        component: () => import('@/views/admin/LeaderboardManagementView.vue'),
        meta: { title: '榜单管理', requiresAdmin: true }
      },
      {
        path: 'datagen',
        name: 'admin-datagen',
        component: () => import('@/views/admin/DatagenManagementView.vue'),
        meta: { title: '造数据任务', requiresAdmin: true }
      },
      {
        path: 'roles',
        name: 'admin-roles',
        component: () => import('@/views/admin/RoleManagementView.vue'),
        meta: { title: '角色与管理员', requiresAdmin: true, requiresSuperAdmin: true }
      },
      {
        path: 'logs',
        name: 'admin-logs',
        component: () => import('@/views/admin/OperationLogView.vue'),
        meta: { title: '操作日志', requiresAdmin: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} · 题阵` : '题阵'
  // 路由守卫只负责前端导航体验；真正的身份认证和接口权限校验由后端执行。
  const authStore = useAuthStore()

  if (to.name === 'admin-login') {
    return authStore.isAdminAuthenticated ? { name: 'admin-dashboard' } : true
  }

  if (to.meta.requiresAdmin && !authStore.isAdminAuthenticated) {
    try {
      authStore.setAdmin(await fetchAdminMe())
    } catch {
      return {
        name: 'admin-login',
        query: { redirect: to.fullPath }
      }
    }
  }

  // 仅做前端导航拦截，真正的权限校验仍由后端服务执行。
  if (to.meta.requiresSuperAdmin && authStore.admin?.roleCode !== 'super_admin') {
    return { name: 'admin-dashboard' }
  }

  return true
})

export default router
