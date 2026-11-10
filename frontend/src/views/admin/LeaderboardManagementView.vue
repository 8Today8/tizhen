<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'

import {
  fetchAdminLeaderboard,
  fetchAdminManualDeclarations,
  reviewAdminManualDeclaration,
  syncCfAccounts,
  type AdminLeaderboardPage,
  type AdminManualDeclarationItem,
  type LeaderboardItem
} from '@/api/admin'
import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminPage from '@/components/admin/AdminPage.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import AdminTableCard from '@/components/admin/AdminTableCard.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/format'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { manualVerificationLabel, verificationLabel } from '@/utils/adminLabels'

const authStore = useAuthStore()
const canSyncCf = computed(() => authStore.admin?.roleCode === 'super_admin')
const canReviewDeclaration = computed(() => authStore.admin?.roleCode === 'super_admin')

const activeTab = ref<'week' | 'all'>('week')
const rankingLoading = ref(false)
const ranking = ref<AdminLeaderboardPage | null>(null)
const rankPage = ref(1)
const rankPageSize = ref(20)

const declarationLoading = ref(false)
const declarations = ref<AdminManualDeclarationItem[]>([])
const declarationTotal = ref(0)
const declarationPage = ref(1)
const declarationPageSize = ref(20)
const declarationUserKeyword = ref('')
const declarationPlatform = ref('')
const declarationWeek = ref('')
const syncing = ref(false)

const reviewVisible = ref(false)
const reviewSubmitting = ref(false)
const reviewTarget = ref<AdminManualDeclarationItem | null>(null)
const reviewForm = reactive<{
  status: 'verified' | 'rejected'
  note: string
}>({
  status: 'verified',
  note: ''
})

const rankingTitle = computed(() => (activeTab.value === 'week' ? '周榜排名' : '总榜排名'))
const rankingDescription = computed(() =>
  activeTab.value === 'week'
    ? '本周站内首次 AC + CF 模拟同步 + 审核通过的手动申报；总分相同按站内过题数降序。'
    : '历史累计站内首次 AC + CF 历史过题 + 审核通过的手动申报；只读视图，不提供改分入口。'
)
const rankingMeta = computed(() => {
  if (!ranking.value) {
    return ''
  }
  const parts: string[] = []
  if (ranking.value.weekStartDate) {
    parts.push(`统计周：${ranking.value.weekStartDate} 起`)
  }
  parts.push(`生成于 ${formatDateTime(ranking.value.generatedAt)}`)
  return parts.join(' · ')
})

function normalizeUserId(value: string): string | undefined {
  const trimmed = value.trim()
  if (!trimmed) {
    return undefined
  }
  if (!/^\d+$/.test(trimmed)) {
    return trimmed
  }
  return trimmed
}

/** 将任一所选日期归一到所在自然周的周一，与后端“周起始日”口径保持一致。 */
function normalizeToMonday(value: string): string | undefined {
  if (!value) {
    return undefined
  }
  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) {
    return undefined
  }
  const weekday = date.getDay()
  const offset = weekday === 0 ? -6 : 1 - weekday
  date.setDate(date.getDate() + offset)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

async function loadRanking() {
  rankingLoading.value = true
  try {
    ranking.value = await fetchAdminLeaderboard({
      period: activeTab.value,
      page: rankPage.value,
      pageSize: rankPageSize.value
    })
  } catch (error) {
    ranking.value = null
    ElMessage.error(resolveApiErrorMessage(error, '榜单加载失败'))
  } finally {
    rankingLoading.value = false
  }
}

function handleTabChange() {
  rankPage.value = 1
  void loadRanking()
}

async function loadDeclarations() {
  declarationLoading.value = true
  try {
    const result = await fetchAdminManualDeclarations({
      userId: normalizeUserId(declarationUserKeyword.value),
      platform: declarationPlatform.value.trim() || undefined,
      weekStartDate: normalizeToMonday(declarationWeek.value),
      page: declarationPage.value,
      pageSize: declarationPageSize.value
    })
    declarations.value = result.items
    declarationTotal.value = result.total
  } catch (error) {
    declarations.value = []
    declarationTotal.value = 0
    ElMessage.error(resolveApiErrorMessage(error, '手动申报记录加载失败'))
  } finally {
    declarationLoading.value = false
  }
}

function searchDeclarations() {
  declarationPage.value = 1
  void loadDeclarations()
}

function resetDeclarations() {
  declarationUserKeyword.value = ''
  declarationPlatform.value = ''
  declarationWeek.value = ''
  searchDeclarations()
}

function openReview(row: AdminManualDeclarationItem, status: 'verified' | 'rejected') {
  reviewTarget.value = row
  reviewForm.status = status
  reviewForm.note = row.reviewNote ?? ''
  reviewVisible.value = true
}

async function submitReview() {
  const target = reviewTarget.value
  if (!target) {
    return
  }
  const note = reviewForm.note.trim()
  if (note.length > 200) {
    ElMessage.warning('审核备注不能超过 200 个字符')
    return
  }

  reviewSubmitting.value = true
  try {
    await reviewAdminManualDeclaration(target.id, {
      status: reviewForm.status,
      note: note || null
    })
    ElMessage.success(
      reviewForm.status === 'verified' ? '申报已通过，已计入周榜和总榜' : '申报已拒绝，不再计入榜单'
    )
    reviewVisible.value = false
    reviewTarget.value = null
    await Promise.all([loadDeclarations(), loadRanking()])
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '审核手动申报失败'))
  } finally {
    reviewSubmitting.value = false
  }
}

async function handleSyncCf() {
  try {
    await ElMessageBox.confirm(
      '将模拟刷新全部有效 CF 账号的周过题数与历史过题数，不会影响站内成绩。确认执行吗？',
      '刷新 CF 模拟数据',
      {
        confirmButtonText: '开始刷新',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  syncing.value = true
  try {
    const accepted = await syncCfAccounts()
    ElMessage.success(`已提交刷新，共 ${accepted.accountCount} 个有效 CF 账号，稍后自动刷新榜单`)
    window.setTimeout(() => {
      void loadRanking()
    }, 1200)
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '刷新 CF 模拟数据失败'))
  } finally {
    syncing.value = false
  }
}

function displayTime(value: string | null): string {
  return value ? formatDateTime(value) : '—'
}

function rankClass(rank: number): string {
  return rank <= 3 ? 'rank-badge is-top' : 'rank-badge'
}

function verificationTagType(status: string | null): 'success' | 'warning' | 'info' {
  if (status === 'verified') {
    return 'success'
  }
  if (status === 'simulated') {
    return 'warning'
  }
  return 'info'
}

function manualStatusTagType(status: string): 'success' | 'danger' | 'warning' | 'info' {
  if (status === 'verified') {
    return 'success'
  }
  if (status === 'rejected') {
    return 'danger'
  }
  if (status === 'unverified') {
    return 'warning'
  }
  return 'info'
}

function playerInitial(item: LeaderboardItem): string {
  return item.user.nickname?.slice(0, 1) ?? ''
}

onMounted(() => {
  void loadRanking()
  void loadDeclarations()
})
</script>

<template>
  <AdminPage
    title="榜单管理"
    description="同页查看周榜与总榜、手动申报记录并审核；总榜为只读视图，申报审核与 CF 模拟数据刷新仅超级管理员可操作。"
  >
    <template #actions>
      <el-tag type="info" effect="plain">榜单实时派生，不支持改分</el-tag>
      <el-button
        v-if="canSyncCf"
        type="primary"
        plain
        :icon="Refresh"
        :loading="syncing"
        @click="handleSyncCf"
      >
        刷新 CF 模拟数据
      </el-button>
    </template>

    <el-tabs v-model="activeTab" class="admin-page-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="周榜" name="week" />
      <el-tab-pane label="总榜" name="all" />
    </el-tabs>

    <AdminTableCard :title="rankingTitle" :description="rankingDescription">
      <template #head-extra>
        <el-button :icon="Refresh" :loading="rankingLoading" @click="loadRanking">刷新</el-button>
      </template>

      <template #toolbar>
        <AdminToolbar>
          <span class="leaderboard-hint">
            周榜统计口径：北京时间自然周（周一 00:00 起），禁用用户不参与排名。
          </span>
          <template #meta>{{ rankingMeta }}</template>
        </AdminToolbar>
      </template>

      <el-table v-loading="rankingLoading" :data="ranking?.items ?? []" row-key="user.id">
        <el-table-column label="排名" width="70" align="center">
          <template #default="{ row }">
            <span :class="rankClass(row.rank)">{{ row.rank }}</span>
          </template>
        </el-table-column>
        <el-table-column label="选手" min-width="200">
          <template #default="{ row }">
            <div class="leaderboard-player">
              <el-avatar :size="32" :src="row.user.avatarUrl || undefined">
                {{ playerInitial(row) }}
              </el-avatar>
              <span class="leaderboard-player__text">
                <strong>{{ row.user.nickname }}</strong>
                <small>{{ row.user.school || '未填写学校' }} · ID {{ row.user.id }}</small>
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="站内" width="70" align="center">
          <template #default="{ row }">{{ row.internalSolved }}</template>
        </el-table-column>
        <el-table-column label="CF" width="70" align="center">
          <template #default="{ row }">{{ row.cfSolved }}</template>
        </el-table-column>
        <el-table-column label="手动" width="70" align="center">
          <template #default="{ row }">{{ row.manualSolved }}</template>
        </el-table-column>
        <el-table-column label="总分" width="80" align="center">
          <template #default="{ row }"><strong class="leaderboard-score">{{ row.totalScore }}</strong></template>
        </el-table-column>
        <el-table-column label="CF Handle" min-width="120">
          <template #default="{ row }">
            <span>{{ row.user.cfHandle || '未绑定' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="CF 校验" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="verificationTagType(row.cfVerificationStatus)" effect="plain">
              {{ verificationLabel(row.cfVerificationStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="CF 同步时间" width="160">
          <template #default="{ row }">{{ displayTime(row.cfLastSyncAt) }}</template>
        </el-table-column>
        <el-table-column label="申报更新时间" width="160">
          <template #default="{ row }">{{ displayTime(row.manualUpdatedAt) }}</template>
        </el-table-column>
        <template #empty>
          <AdminEmptyState description="暂无榜单数据，等待选手产生成绩后展示" />
        </template>
      </el-table>

      <template #footer>
        <AdminPagination
          v-model:page="rankPage"
          v-model:page-size="rankPageSize"
          :total="ranking?.total ?? 0"
          @change="loadRanking"
        />
      </template>
    </AdminTableCard>

    <AdminTableCard
      title="手动申报记录"
      description="审核通过后计入周榜/总榜；未审核或已拒绝不计分。超级管理员可审核，内容运营只读；已失效记录仅供核对。"
    >
      <template #toolbar>
        <AdminToolbar>
          <el-input
            v-model="declarationUserKeyword"
            class="admin-search"
            placeholder="按用户 ID 筛选"
            clearable
            @keyup.enter="searchDeclarations"
            @clear="searchDeclarations"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="declarationPlatform"
            class="admin-filter leaderboard-platform-filter"
            placeholder="平台，如 nowcoder"
            clearable
            @keyup.enter="searchDeclarations"
            @clear="searchDeclarations"
          />
          <el-date-picker
            v-model="declarationWeek"
            class="admin-date-filter"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择自然周内任意日期"
            :clearable="true"
            @change="searchDeclarations"
          />
          <el-button type="primary" @click="searchDeclarations">查询</el-button>
          <el-button @click="resetDeclarations">重置</el-button>
          <template #meta>共 {{ declarationTotal }} 条；日期会归一到所在周周一</template>
        </AdminToolbar>
      </template>

      <el-table v-loading="declarationLoading" :data="declarations" row-key="id">
        <el-table-column label="用户" min-width="150">
          <template #default="{ row }">
            <div class="leaderboard-player">
              <el-avatar :size="28" :src="row.user.avatarUrl || undefined">
                {{ row.user.nickname?.slice(0, 1) }}
              </el-avatar>
              <span class="leaderboard-player__text">
                <strong>{{ row.user.nickname || '未知用户' }}</strong>
                <small>{{ row.username || `ID ${row.user.id}` }}</small>
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="platform" label="平台" width="90" />
        <el-table-column prop="account" label="平台账号" min-width="110" />
        <el-table-column label="过题数" width="70" align="center">
          <template #default="{ row }"><strong>{{ row.count }}</strong></template>
        </el-table-column>
        <el-table-column prop="weekStartDate" label="统计周" width="110" />
        <el-table-column label="校验状态" width="120">
          <template #default="{ row }">
            <div class="declaration-status-cell">
              <el-tag :type="manualStatusTagType(row.verificationStatus)" effect="plain">
                {{ manualVerificationLabel(row.verificationStatus) }}
              </el-tag>
              <el-tag v-if="!row.activeFlag" type="info" size="small" effect="plain">已失效</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="申报时间" width="155">
          <template #default="{ row }">{{ displayTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="审核信息" min-width="170">
          <template #default="{ row }">
            <div class="declaration-review-cell">
              <span v-if="row.reviewedAt">
                {{ row.reviewedByName || '管理员' }} · {{ displayTime(row.reviewedAt) }}
              </span>
              <span v-else class="admin-muted">尚未审核</span>
              <small v-if="row.reviewNote" :title="row.reviewNote">{{ row.reviewNote }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          v-if="canReviewDeclaration"
          label="操作"
          width="130"
          fixed="right"
        >
          <template #default="{ row }">
            <template v-if="row.activeFlag">
              <el-button
                v-if="row.verificationStatus !== 'verified'"
                link
                type="success"
                @click="openReview(row, 'verified')"
              >
                通过
              </el-button>
              <el-button
                v-if="row.verificationStatus !== 'rejected'"
                link
                type="danger"
                @click="openReview(row, 'rejected')"
              >
                拒绝
              </el-button>
            </template>
            <span v-else class="admin-muted">不可审核</span>
          </template>
        </el-table-column>
        <template #empty>
          <AdminEmptyState description="暂无手动申报记录" />
        </template>
      </el-table>

      <template #footer>
        <AdminPagination
          v-model:page="declarationPage"
          v-model:page-size="declarationPageSize"
          :total="declarationTotal"
          @change="loadDeclarations"
        />
      </template>
    </AdminTableCard>

    <el-dialog
      v-model="reviewVisible"
      title="审核手动申报"
      width="520px"
      destroy-on-close
      class="declaration-review-dialog"
    >
      <template v-if="reviewTarget">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="选手">
            {{ reviewTarget.user.nickname || '未知用户' }}
          </el-descriptions-item>
          <el-descriptions-item label="平台">{{ reviewTarget.platform }}</el-descriptions-item>
          <el-descriptions-item label="平台账号">{{ reviewTarget.account }}</el-descriptions-item>
          <el-descriptions-item label="过题数">{{ reviewTarget.count }}</el-descriptions-item>
          <el-descriptions-item label="统计周">{{ reviewTarget.weekStartDate }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="manualStatusTagType(reviewTarget.verificationStatus)" effect="plain">
              {{ manualVerificationLabel(reviewTarget.verificationStatus) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-form label-position="top" class="declaration-review-form">
          <el-form-item label="审核结果" required>
            <el-radio-group v-model="reviewForm.status">
              <el-radio-button value="verified">通过并计入榜单</el-radio-button>
              <el-radio-button value="rejected">拒绝且不计分</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="审核备注（选填）">
            <el-input
              v-model="reviewForm.note"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              placeholder="可填写核对结果或拒绝原因，仅后台可见"
            />
          </el-form-item>
        </el-form>

        <el-alert
          :type="reviewForm.status === 'verified' ? 'success' : 'warning'"
          :closable="false"
          show-icon
          :title="
            reviewForm.status === 'verified'
              ? '通过后该申报立即计入周榜与总榜。'
              : '拒绝后该申报保留记录，但不再计入周榜与总榜。'
          "
        />
      </template>

      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">
          确认审核
        </el-button>
      </template>
    </el-dialog>
  </AdminPage>
</template>

<style scoped>
.leaderboard-hint {
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

.leaderboard-platform-filter {
  width: 180px;
}

.leaderboard-player {
  display: flex;
  align-items: center;
  gap: 10px;
}

.leaderboard-player__text {
  display: flex;
  min-width: 0;
  flex-direction: column;
  line-height: 1.4;
}

.leaderboard-player__text strong {
  font-size: 13px;
}

.leaderboard-player__text small {
  overflow: hidden;
  color: var(--tz-color-text-muted);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.leaderboard-score {
  color: var(--tz-color-primary);
  font-size: 15px;
}

.declaration-status-cell {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.declaration-review-cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
  line-height: 1.5;
}

.declaration-review-cell span {
  font-size: 12px;
}

.declaration-review-cell small {
  overflow: hidden;
  color: var(--tz-color-text-muted);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.declaration-review-form {
  margin-top: 18px;
}

.declaration-review-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.rank-badge {
  display: inline-grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 9px;
  background: var(--tz-color-surface-muted);
  color: var(--tz-color-text-secondary);
  font-size: 13px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.rank-badge.is-top {
  background: linear-gradient(140deg, var(--tz-color-primary), var(--tz-color-accent));
  color: #fff;
}

.admin-date-filter {
  width: 200px;
}

@media (max-width: 768px) {
  .leaderboard-platform-filter {
    width: 100%;
  }
}
</style>
