<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search, View } from '@element-plus/icons-vue'

import {
  fetchOperationLog,
  fetchOperationLogs,
  type OperationLogView
} from '@/api/admin'
import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminPage from '@/components/admin/AdminPage.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import AdminTableCard from '@/components/admin/AdminTableCard.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'
import { actionLabel, roleLabel, targetTypeLabel } from '@/utils/adminLabels'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatDateTime } from '@/utils/format'

const actionOptions = [
  'login',
  'create',
  'update',
  'delete',
  'publish',
  'offline',
  'enable',
  'disable',
  'reset_password',
  'refresh_cf',
  'review'
]

const targetTypeOptions = [
  'admin_user',
  'user',
  'route',
  'stage',
  'course',
  'problem',
  'external_account',
  'manual_declaration',
  'data_gen_task',
  'leaderboard'
]

const adminId = ref('')
const actionFilter = ref('')
const targetTypeFilter = ref('')
const targetId = ref('')
const timeRange = ref<string[] | null>(null)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)
const logs = ref<OperationLogView[]>([])

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<OperationLogView | null>(null)

const extraText = computed(() => {
  if (!detail.value?.extra) {
    return ''
  }
  try {
    return JSON.stringify(detail.value.extra, null, 2)
  } catch {
    return String(detail.value.extra)
  }
})

function normalizeId(value: string): string | undefined {
  const trimmed = value.trim()
  return /^\d+$/.test(trimmed) ? trimmed : undefined
}

function toInstant(localValue: string | undefined): string | undefined {
  if (!localValue) {
    return undefined
  }
  const date = new Date(localValue.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) {
    return undefined
  }
  return date.toISOString()
}

async function loadLogs() {
  loading.value = true
  try {
    const result = await fetchOperationLogs({
      adminId: normalizeId(adminId.value),
      action: actionFilter.value || undefined,
      targetType: targetTypeFilter.value || undefined,
      targetId: normalizeId(targetId.value),
      from: toInstant(timeRange.value?.[0]),
      to: toInstant(timeRange.value?.[1]),
      page: page.value,
      pageSize: pageSize.value
    })
    logs.value = result.items
    total.value = result.total
  } catch (error) {
    logs.value = []
    total.value = 0
    ElMessage.error(resolveApiErrorMessage(error, '操作日志加载失败'))
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  void loadLogs()
}

function handleReset() {
  adminId.value = ''
  actionFilter.value = ''
  targetTypeFilter.value = ''
  targetId.value = ''
  timeRange.value = null
  handleSearch()
}

async function openDetail(log: OperationLogView) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await fetchOperationLog(log.id)
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '日志详情加载失败'))
  } finally {
    detailLoading.value = false
  }
}

function displayTime(value: string | null): string {
  return value ? formatDateTime(value) : '—'
}

function actionTagType(action: string): 'success' | 'danger' | 'warning' | 'info' | 'primary' {
  if (action === 'delete' || action === 'disable') {
    return 'danger'
  }
  if (action === 'create' || action === 'enable') {
    return 'success'
  }
  if (action === 'refresh_cf' || action === 'reset_password' || action === 'review') {
    return 'warning'
  }
  if (action === 'login') {
    return 'info'
  }
  return 'primary'
}

onMounted(loadLogs)
</script>

<template>
  <AdminPage
    title="操作日志"
    description="记录后台关键操作，便于追踪内容变更、用户状态调整、榜单刷新与管理员账号维护。日志只读，不支持修改或删除。"
  >
    <template #actions>
      <el-tag type="info" effect="plain">日志永久保留</el-tag>
    </template>

    <AdminTableCard>
      <template #toolbar>
        <AdminToolbar>
          <el-input
            v-model="adminId"
            class="admin-filter"
            placeholder="操作人管理员 ID"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="actionFilter"
            class="admin-filter"
            placeholder="全部动作"
            @change="handleSearch"
          >
            <el-option label="全部动作" value="" />
            <el-option
              v-for="action in actionOptions"
              :key="action"
              :label="actionLabel(action)"
              :value="action"
            />
          </el-select>
          <el-select
            v-model="targetTypeFilter"
            class="admin-filter-wide"
            placeholder="全部对象类型"
            @change="handleSearch"
          >
            <el-option label="全部对象类型" value="" />
            <el-option
              v-for="targetType in targetTypeOptions"
              :key="targetType"
              :label="targetTypeLabel(targetType)"
              :value="targetType"
            />
          </el-select>
          <el-input
            v-model="targetId"
            class="admin-filter"
            placeholder="对象 ID"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-date-picker
            v-model="timeRange"
            class="admin-date-range"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            :clearable="true"
            @change="handleSearch"
          />
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <template #meta>共 {{ total }} 条</template>
        </AdminToolbar>
      </template>

      <el-table v-loading="loading" :data="logs" row-key="id">
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ displayTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作人" min-width="135">
          <template #default="{ row }">
            <div class="log-admin-cell">
              <strong>{{ row.admin.displayName || '未知管理员' }}</strong>
              <small>{{ row.admin.username || `ID ${row.admin.id}` }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="110">
          <template #default="{ row }">{{ roleLabel(row.admin.roleCode) }}</template>
        </el-table-column>
        <el-table-column label="动作" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="actionTagType(row.action)" effect="plain">{{ actionLabel(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="对象" min-width="140">
          <template #default="{ row }">
            <span>{{ targetTypeLabel(row.targetType) }}</span>
            <span v-if="row.targetId" class="log-target-id">#{{ row.targetId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="详情" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.detail || '—' }}</template>
        </el-table-column>
        <el-table-column label="IP" width="120">
          <template #default="{ row }">{{ row.ipAddress || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <AdminEmptyState description="暂无操作日志" />
        </template>
      </el-table>

      <template #footer>
        <AdminPagination
          v-model:page="page"
          v-model:page-size="pageSize"
          :total="total"
          @change="loadLogs"
        />
      </template>
    </AdminTableCard>

    <el-drawer
      v-model="detailVisible"
      class="admin-detail-drawer"
      title="操作日志详情"
      size="560px"
    >
      <div v-loading="detailLoading" class="log-detail">
        <template v-if="detail">
          <div class="log-detail__head">
            <div>
              <h3>{{ actionLabel(detail.action) }}</h3>
              <p>{{ detail.detail || '无补充说明' }}</p>
            </div>
            <el-tag :type="actionTagType(detail.action)" effect="plain">
              {{ actionLabel(detail.action) }}
            </el-tag>
          </div>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="日志 ID">{{ detail.id }}</el-descriptions-item>
            <el-descriptions-item label="时间">{{ displayTime(detail.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="操作人">
              {{ detail.admin.displayName || '未知管理员' }}
            </el-descriptions-item>
            <el-descriptions-item label="账号">
              {{ detail.admin.username || `ID ${detail.admin.id}` }}
            </el-descriptions-item>
            <el-descriptions-item label="角色">{{ roleLabel(detail.admin.roleCode) }}</el-descriptions-item>
            <el-descriptions-item label="对象类型">{{ targetTypeLabel(detail.targetType) }}</el-descriptions-item>
            <el-descriptions-item label="对象 ID">{{ detail.targetId || '—' }}</el-descriptions-item>
            <el-descriptions-item label="IP">{{ detail.ipAddress || '—' }}</el-descriptions-item>
            <el-descriptions-item label="请求 ID" :span="2">
              <code>{{ detail.requestId || '—' }}</code>
            </el-descriptions-item>
            <el-descriptions-item label="User-Agent" :span="2">
              <span class="log-user-agent">{{ detail.userAgent || '—' }}</span>
            </el-descriptions-item>
          </el-descriptions>

          <section class="log-detail__section">
            <h4>附加信息</h4>
            <pre v-if="extraText" class="log-extra">{{ extraText }}</pre>
            <el-empty v-else description="无附加信息" :image-size="48" />
          </section>
        </template>
        <AdminEmptyState v-else-if="!detailLoading" description="未能加载日志详情" />
      </div>
    </el-drawer>
  </AdminPage>
</template>

<style scoped>
.admin-filter-wide {
  width: 150px;
}

.admin-date-range {
  width: 360px;
}

.log-admin-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.log-admin-cell strong {
  font-size: 13px;
}

.log-admin-cell small {
  color: var(--tz-color-text-muted);
  font-size: 11px;
}

.log-target-id {
  margin-left: 4px;
  color: var(--tz-color-text-muted);
  font-family: 'JetBrains Mono', 'Cascadia Code', Consolas, monospace;
  font-size: 12px;
}

.log-detail {
  display: flex;
  min-height: 200px;
  flex-direction: column;
  gap: 18px;
}

.log-detail__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.log-detail__head h3 {
  margin: 0;
  font-size: 18px;
}

.log-detail__head p {
  margin: 6px 0 0;
  color: var(--tz-color-text-muted);
  font-size: 12px;
  line-height: 1.6;
}

.log-detail__section h4 {
  margin: 0 0 10px;
  font-size: 14px;
}

.log-extra {
  overflow: auto;
  max-height: 320px;
  margin: 0;
  padding: 12px 14px;
  border: 1px solid var(--tz-color-border);
  border-radius: var(--tz-radius-sm);
  background: var(--tz-color-surface-muted);
  color: var(--tz-color-text-secondary);
  font-family: 'JetBrains Mono', 'Cascadia Code', Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.log-user-agent {
  word-break: break-all;
}

code {
  font-family: 'JetBrains Mono', 'Cascadia Code', Consolas, monospace;
  font-size: 12px;
  word-break: break-all;
}

@media (max-width: 640px) {
  .admin-date-range {
    width: 100%;
  }
}
</style>
