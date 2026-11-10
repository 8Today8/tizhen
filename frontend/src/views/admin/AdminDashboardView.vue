<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { Component } from 'vue'
import {
  Clock,
  Collection,
  Document,
  EditPen,
  Guide,
  Link,
  MagicStick,
  Monitor,
  Trophy,
  User,
  UserFilled
} from '@element-plus/icons-vue'

import { fetchOperationLogs, type OperationLogView } from '@/api/admin'
import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminPage from '@/components/admin/AdminPage.vue'
import AdminTableCard from '@/components/admin/AdminTableCard.vue'
import { useAuthStore } from '@/stores/auth'
import { actionLabel, targetTypeLabel } from '@/utils/adminLabels'
import { formatDateTime } from '@/utils/format'

interface QuickEntry {
  title: string
  description: string
  to: string
  icon: Component
}

const authStore = useAuthStore()
const allQuickEntries: QuickEntry[] = [
  { title: '路线管理', description: '维护学习路线与上下架', to: '/admin/routes', icon: Guide },
  { title: '阶段管理', description: '维护阶段、题目与课程', to: '/admin/stages', icon: Collection },
  { title: 'OJ 题库', description: '编程题、测试数据与限制', to: '/admin/problems', icon: Monitor },
  { title: '客观题', description: '题干、选项与正确答案', to: '/admin/objective', icon: Document },
  { title: '简答题', description: '参考答案与评分要点', to: '/admin/short', icon: EditPen },
  { title: '外链题', description: '外部平台题目入口', to: '/admin/external', icon: Link },
  { title: '用户管理', description: '查看选手与账号状态', to: '/admin/users', icon: User },
  { title: '榜单管理', description: '周榜、总榜与手动申报审核', to: '/admin/leaderboard', icon: Trophy },
  { title: '造数据任务', description: '查看任务与产出文件', to: '/admin/datagen', icon: MagicStick },
  { title: '角色与管理员', description: '固定角色与后台账号', to: '/admin/roles', icon: UserFilled },
  { title: '操作日志', description: '追踪后台关键操作', to: '/admin/logs', icon: Clock }
]
const quickEntries = computed(() =>
  allQuickEntries.filter(
    (entry) => authStore.admin?.roleCode === 'super_admin' || entry.to !== '/admin/roles'
  )
)

const recentLogs = ref<OperationLogView[]>([])
const recentLoading = ref(false)

async function loadRecentLogs() {
  recentLoading.value = true
  try {
    const result = await fetchOperationLogs({ page: 1, pageSize: 5 })
    recentLogs.value = result.items
  } catch {
    recentLogs.value = []
  } finally {
    recentLoading.value = false
  }
}

onMounted(loadRecentLogs)
</script>

<template>
  <AdminPage
    title="运营工作台"
    description="从左侧菜单或快捷入口进入各管理模块；首期后台不做数据看板，聚焦内容维护与运营数据查看。"
  >
    <template #actions>
      <el-tag type="info" effect="plain">布局骨架</el-tag>
    </template>

    <section class="admin-welcome tz-card">
      <div class="admin-welcome__text">
        <span class="admin-welcome__eyebrow">Operations Console</span>
        <h2>内容与运营管理</h2>
        <p>维护学习路线、阶段与四类题库内容，查看用户、榜单与造数据任务，并追踪后台关键操作。</p>
      </div>
      <div class="admin-welcome__mark">阵</div>
    </section>

    <section class="admin-quick-grid" aria-label="后台快捷入口">
      <RouterLink
        v-for="entry in quickEntries"
        :key="entry.to"
        class="admin-quick-card"
        :to="entry.to"
      >
        <span class="admin-quick-card__icon">
          <el-icon><component :is="entry.icon" /></el-icon>
        </span>
        <span class="admin-quick-card__text">
          <strong>{{ entry.title }}</strong>
          <span>{{ entry.description }}</span>
        </span>
      </RouterLink>
    </section>

    <AdminTableCard
      title="最近操作"
      description="后台关键动作会记录在此，可在操作日志页查看全部记录。"
    >
      <template #head-extra>
        <RouterLink class="admin-inline-link" to="/admin/logs">查看全部日志</RouterLink>
      </template>

      <el-table v-loading="recentLoading" :data="recentLogs" row-key="id">
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作人" width="150">
          <template #default="{ row }">{{ row.admin.displayName || row.admin.username || '未知管理员' }}</template>
        </el-table-column>
        <el-table-column label="动作" width="130">
          <template #default="{ row }">{{ actionLabel(row.action) }}</template>
        </el-table-column>
        <el-table-column label="对象" min-width="160">
          <template #default="{ row }">{{ targetTypeLabel(row.targetType) }}{{ row.targetId ? ` #${row.targetId}` : '' }}</template>
        </el-table-column>
        <el-table-column label="详情" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.detail || '—' }}</template>
        </el-table-column>
        <template #empty>
          <AdminEmptyState description="暂无操作记录" />
        </template>
      </el-table>
    </AdminTableCard>
  </AdminPage>
</template>

<style scoped>
.admin-welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  overflow: hidden;
  background: var(--tz-hero-gradient);
}

.admin-welcome__eyebrow {
  color: var(--tz-color-text-muted);
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.admin-welcome__text h2 {
  margin: 10px 0 8px;
  font-size: 22px;
  letter-spacing: -0.01em;
}

.admin-welcome__text p {
  max-width: 720px;
  margin: 0;
  color: var(--tz-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.admin-welcome__mark {
  display: grid;
  width: 84px;
  height: 84px;
  flex: none;
  place-items: center;
  border-radius: 24px;
  background: linear-gradient(140deg, var(--tz-color-primary), var(--tz-color-accent));
  color: #fff;
  font-size: 34px;
  font-weight: 700;
  box-shadow: var(--tz-shadow-md);
}

.admin-quick-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
}

.admin-quick-card {
  display: flex;
  min-width: 0;
  padding: 16px;
  align-items: center;
  gap: 12px;
  border: 1px solid var(--tz-color-border);
  border-radius: var(--tz-radius-md);
  background: var(--tz-color-surface);
  box-shadow: var(--tz-shadow-sm);
  color: var(--tz-color-text);
  text-decoration: none;
  transition:
    transform 0.16s ease,
    border-color 0.16s ease;
}

.admin-quick-card:hover {
  border-color: var(--tz-color-primary);
  transform: translateY(-2px);
}

.admin-quick-card__icon {
  display: grid;
  width: 38px;
  height: 38px;
  flex: none;
  place-items: center;
  border-radius: 12px;
  background: var(--tz-color-primary-soft);
  color: var(--tz-color-primary);
  font-size: 18px;
}

.admin-quick-card__text {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.admin-quick-card__text strong {
  font-size: 14px;
}

.admin-quick-card__text span {
  overflow: hidden;
  color: var(--tz-color-text-muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .admin-welcome {
    align-items: flex-start;
  }

  .admin-welcome__mark {
    width: 56px;
    height: 56px;
    border-radius: 18px;
    font-size: 24px;
  }
}
</style>
