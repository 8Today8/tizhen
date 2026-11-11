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
        meta: { title: '学习路线', requiresAuth: true }
      },
      {
        path: 'learning/routes/:routeId',
        name: 'learning-route-detail',
        component: () => import('@/views/learning/LearningRouteDetailView.vue'),
        meta: { title: '路线详情', requiresAuth: true }
      },
      {
        path: 'learning/stages/:stageId',
        name: 'learning-stage-detail',
        component: () => import('@/views/learning/LearningStageDetailView.vue'),
        meta: { title: '阶段详情', requiresAuth: true }
      },
      {
        path: 'problems',
        name: 'problems',
        component: () => import('@/views/problems/ProblemsView.vue'),
        meta: { title: '题库', requiresAuth: true }
      },
      {
        path: 'problems/:problemId',
        name: 'problem-detail',
        component: () => import('@/views/problems/ProblemDetailView.vue'),
        meta: { title: '题目详情', requiresAuth: true }
      },
      {
        path: 'leaderboard',
        name: 'leaderboard',
        component: () => import('@/views/leaderboard/LeaderboardView.vue'),
        meta: { title: '榜单', requiresAuth: true }
      },
      {
        path: 'battle',
        name: 'battle',
        component: () => import('@/views/battle/BattleLobbyView.vue'),
        meta: { title: '对战', requiresAuth: true }
      },
      {
        path: 'battle/rooms/:roomId',
        name: 'battle-room',
        component: () => import('@/views/battle/BattleRoomView.vue'),
        meta: { title: '对战房间', requiresAuth: true }
      },
      {
        path: 'battle/join/:roomCode',
        name: 'battle-join',
        component: () => import('@/views/battle/BattleJoinView.vue'),
        meta: { title: '加入对战', requiresAuth: true }
      },
      {
        path: 'battle/records',
        name: 'battle-records',
        component: () => import('@/views/battle/BattleRecordsView.vue'),
        meta: { title: '对战记录', requiresAuth: true }
      },
      {
        path: 'battle/records/:roomId',
        name: 'battle-record-detail',
        component: () => import('@/views/battle/BattleRecordDetailView.vue'),
        meta: { title: '对战记录详情', requiresAuth: true }
      },
      {
        path: 'datagen',
        name: 'datagen',
        component: () => import('@/views/datagen/DatagenWorkbenchView.vue'),
        meta: { title: 'AI 造数据', requiresAuth: true }
      },
      {
        path: 'datagen/tasks',
        name: 'datagen-tasks',
        component: () => import('@/views/datagen/DatagenTasksView.vue'),
        meta: { title: '造数据任务', requiresAuth: true }
      },
      {
        path: 'datagen/tasks/:taskId',
        name: 'datagen-task-detail',
        component: () => import('@/views/datagen/DatagenTaskDetailView.vue'),
        meta: { title: '造数据任务详情', requiresAuth: true }
      },
      {
        path: 'records',
        name: 'records',
        component: () => import('@/views/records/LearningRecordsView.vue'),
        meta: { title: '学习记录', requiresAuth: true }
      },
      {
        path: 'wrongbook',
        name: 'wrongbook',
        component: () => import('@/views/records/WrongBookView.vue'),
        meta: { title: '错题本', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/profile/ProfileView.vue'),
        meta: { title: '个人中心', requiresAuth: true }
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
    name: 'register',
    redirect: (to) => ({
      name: 'login',
      query: { ...to.query, tab: 'register' }
    })
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

  if (to.path.startsWith('/admin')) {
    return true
  }

  await authStore.hydratePlayer()

  if ((to.name === 'login' || to.name === 'register') && authStore.isAuthenticated) {
    return { path: '/' }
  }

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return {
      name: 'login',
      query: { redirect: to.fullPath }
    }
  }

  return true
})

export default router
