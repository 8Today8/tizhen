<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Refresh, Search, View } from '@element-plus/icons-vue'

import {
  fetchAdminDatagenTask,
  fetchAdminDatagenTasks,
  type AdminDataGenTaskDetail,
  type AdminDataGenTaskListItem,
  type DataGenFileView
} from '@/api/admin'
import { download } from '@/api/http'
import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminPage from '@/components/admin/AdminPage.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import AdminTableCard from '@/components/admin/AdminTableCard.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'
import {
  datagenStatusLabel,
  failureStageLabel,
  formatBytes,
  generationModeLabel,
  validationStatusLabel
} from '@/utils/adminLabels'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatDateTime } from '@/utils/format'

const userId = ref('')
const statusFilter = ref('')
const modeFilter = ref('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)
const tasks = ref<AdminDataGenTaskListItem[]>([])

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<AdminDataGenTaskDetail | null>(null)
const downloadingKey = ref('')

const statusOptions = [
  { value: 'pending', label: '等待中' },
  { value: 'llm_generating', label: 'LLM 构造中' },
  { value: 'llm_data_generating', label: 'LLM 生成数据中' },
  { value: 'generating', label: '生成中' },
  { value: 'success', label: '成功' },
  { value: 'failed', label: '失败' }
]

const modeOptions = [
  { value: 'random', label: '随机生成' },
  { value: 'llm_generator', label: 'LLM 构造器' },
  { value: 'llm_direct_data', label: 'LLM 直接生成' }
]

const dataRangeItems = computed(() => {
  const range = detail.value?.dataRange
  return Array.isArray(range) ? range : []
})

const hasResultSummary = computed(() => {
  const summary = detail.value?.resultSummary
  return summary !== null && summary !== undefined && Object.keys(summary).length > 0
})

const archiveAvailable = computed(() =>
  (detail.value?.files ?? []).some((file) => file.inputAvailable || file.outputAvailable)
)

function normalizeUserId(value: string): string | undefined {
  const trimmed = value.trim()
  return /^\d+$/.test(trimmed) ? trimmed : undefined
}

async function loadTasks() {
  loading.value = true
  try {
    const result = await fetchAdminDatagenTasks({
      userId: normalizeUserId(userId.value),
      status: statusFilter.value || undefined,
      generationMode: modeFilter.value || undefined,
      page: page.value,
      pageSize: pageSize.value
    })
    tasks.value = result.items
    total.value = result.total
  } catch (error) {
    tasks.value = []
    total.value = 0
    ElMessage.error(resolveApiErrorMessage(error, '造数据任务加载失败'))
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  void loadTasks()
}

function handleReset() {
  userId.value = ''
  statusFilter.value = ''
  modeFilter.value = ''
  handleSearch()
}

async function openDetail(taskId: string) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await fetchAdminDatagenTask(taskId)
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '任务详情加载失败'))
  } finally {
    detailLoading.value = false
  }
}

async function downloadFile(file: DataGenFileView, kind: 'input' | 'output') {
  if (!detail.value) {
    return
  }
  const key = `${file.id}:${kind}`
  downloadingKey.value = key
  try {
    const result = await download(
      `/admin/datagen/tasks/${detail.value.id}/files/${file.id}/${kind}`
    )
    saveBlob(result.blob, result.filename ?? `group-${file.groupIndex}.${kind === 'input' ? 'in' : 'out'}`)
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '文件下载失败'))
  } finally {
    downloadingKey.value = ''
  }
}

async function downloadArchive() {
  if (!detail.value) {
    return
  }
  const key = 'archive'
  downloadingKey.value = key
  try {
    const result = await download(`/admin/datagen/tasks/${detail.value.id}/files/archive`)
    saveBlob(result.blob, result.filename ?? `task-${detail.value.id}.zip`)
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '打包下载失败'))
  } finally {
    downloadingKey.value = ''
  }
}

function saveBlob(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  window.URL.revokeObjectURL(url)
}

function displayTime(value: string | null): string {
  return value ? formatDateTime(value) : '—'
}

function statusTagType(status: string): 'success' | 'danger' | 'warning' | 'info' {
  if (status === 'success') {
    return 'success'
  }
  if (status === 'failed') {
    return 'danger'
  }
  if (status === 'pending') {
    return 'info'
  }
  return 'warning'
}

function validationTagType(status: string): 'success' | 'danger' | 'info' {
  if (status === 'passed') {
    return 'success'
  }
  if (status === 'failed') {
    return 'danger'
  }
  return 'info'
}

function prettyJson(value: unknown): string {
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

onMounted(loadTasks)
</script>

<template>
  <AdminPage
    title="造数据任务"
    description="查看全平台生成任务、失败原因与产出文件；失败任务不会自动重试，生成器代码不对选手展示。"
  >
    <template #actions>
      <el-tag type="info" effect="plain">产出文件保留 7 天</el-tag>
    </template>

    <AdminTableCard>
      <template #toolbar>
        <AdminToolbar>
          <el-input
            v-model="userId"
            class="admin-search"
            placeholder="按用户 ID 筛选"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="statusFilter"
            class="admin-filter"
            placeholder="全部状态"
            @change="handleSearch"
          >
            <el-option label="全部状态" value="" />
            <el-option
              v-for="option in statusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-select
            v-model="modeFilter"
            class="admin-filter-wide"
            placeholder="全部生成方式"
            @change="handleSearch"
          >
            <el-option label="全部生成方式" value="" />
            <el-option
              v-for="option in modeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <template #meta>共 {{ total }} 个任务</template>
        </AdminToolbar>
      </template>

      <el-table v-loading="loading" :data="tasks" row-key="id">
        <el-table-column label="任务" min-width="220">
          <template #default="{ row }">
            <div class="datagen-title-cell">
              <strong>{{ row.title }}</strong>
              <small>ID {{ row.id }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="用户" min-width="160">
          <template #default="{ row }">
            <div class="datagen-title-cell">
              <strong>{{ row.user.nickname || '未知用户' }}</strong>
              <small>{{ row.username || `ID ${row.user.id}` }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="生成方式" width="120">
          <template #default="{ row }">{{ generationModeLabel(row.generationMode) }}</template>
        </el-table-column>
        <el-table-column label="标准语言" width="100">
          <template #default="{ row }">{{ row.stdLanguage }}</template>
        </el-table-column>
        <el-table-column label="组数" width="70" align="center">
          <template #default="{ row }">{{ row.groupCount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain">
              {{ datagenStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ displayTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="95" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row.id)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <AdminEmptyState description="暂无造数据任务" />
        </template>
      </el-table>

      <template #footer>
        <AdminPagination
          v-model:page="page"
          v-model:page-size="pageSize"
          :total="total"
          @change="loadTasks"
        />
      </template>
    </AdminTableCard>

    <el-drawer
      v-model="detailVisible"
      class="admin-detail-drawer"
      title="造数据任务详情"
      size="720px"
    >
      <div v-loading="detailLoading" class="datagen-detail">
        <template v-if="detail">
          <div class="datagen-detail__head">
            <div>
              <h3>{{ detail.title }}</h3>
              <p>任务 ID {{ detail.id }} · {{ detail.user.nickname || '未知用户' }}</p>
            </div>
            <el-tag :type="statusTagType(detail.status)" effect="plain">
              {{ datagenStatusLabel(detail.status) }}
            </el-tag>
          </div>

          <el-alert
            v-if="detail.status === 'failed'"
            type="error"
            :closable="false"
            show-icon
            :title="`失败阶段：${failureStageLabel(detail.failureStage)}`"
            :description="detail.failureReason || '任务未记录失败原因，请结合服务端日志排查。'"
          />

          <el-descriptions :column="2" border>
            <el-descriptions-item label="生成方式">
              {{ generationModeLabel(detail.generationMode) }}
            </el-descriptions-item>
            <el-descriptions-item label="组数">{{ detail.groupCount }}</el-descriptions-item>
            <el-descriptions-item label="标准语言">{{ detail.stdLanguage }}</el-descriptions-item>
            <el-descriptions-item label="限制">
              时间 {{ detail.timeLimitMs }} ms / 内存 {{ detail.memoryLimitMb }} MB
            </el-descriptions-item>
            <el-descriptions-item label="LLM 服务商">{{ detail.llmProvider || '—' }}</el-descriptions-item>
            <el-descriptions-item label="LLM 模型">{{ detail.llmModel || '—' }}</el-descriptions-item>
            <el-descriptions-item label="构造器状态">{{ datagenStatusLabel(detail.generatorStatus) }}</el-descriptions-item>
            <el-descriptions-item label="数据状态">{{ datagenStatusLabel(detail.llmDataStatus) }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ displayTime(detail.startedAt) }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ displayTime(detail.finishedAt) }}</el-descriptions-item>
            <el-descriptions-item label="文件到期">{{ displayTime(detail.filesExpireAt) }}</el-descriptions-item>
            <el-descriptions-item label="文件清理时间" :span="2">
              {{ detail.filesCleanedAt ? displayTime(detail.filesCleanedAt) : '尚未清理' }}
            </el-descriptions-item>
          </el-descriptions>

          <section class="datagen-detail__section">
            <h4>题目描述</h4>
            <pre class="datagen-code">{{ detail.statement || '未填写' }}</pre>
          </section>

          <section class="datagen-detail__section">
            <h4>输入 / 输出格式</h4>
            <div class="datagen-format-grid">
              <div>
                <span class="datagen-label">输入格式</span>
                <pre class="datagen-code">{{ detail.inputFormat || '未填写' }}</pre>
              </div>
              <div>
                <span class="datagen-label">输出格式</span>
                <pre class="datagen-code">{{ detail.outputFormat || '未填写' }}</pre>
              </div>
            </div>
          </section>

          <section class="datagen-detail__section">
            <h4>数据范围</h4>
            <el-table v-if="dataRangeItems.length" :data="dataRangeItems" size="small">
              <el-table-column prop="name" label="变量" min-width="120" />
              <el-table-column prop="type" label="类型" width="100" />
              <el-table-column prop="min" label="最小值" width="110" />
              <el-table-column prop="max" label="最大值" width="110" />
            </el-table>
            <el-empty v-else description="未配置数据范围" :image-size="48" />
          </section>

          <section v-if="detail.specialRequirements" class="datagen-detail__section">
            <h4>特殊要求</h4>
            <pre class="datagen-code">{{ detail.specialRequirements }}</pre>
          </section>

          <section class="datagen-detail__section">
            <h4>标准程序</h4>
            <pre class="datagen-code datagen-code--code">{{ detail.stdCode || '未填写' }}</pre>
          </section>

          <section v-if="hasResultSummary" class="datagen-detail__section">
            <h4>生成结果摘要</h4>
            <pre class="datagen-code">{{ prettyJson(detail.resultSummary) }}</pre>
          </section>

          <section class="datagen-detail__section">
            <div class="datagen-files-head">
              <h4>产出文件</h4>
              <el-button
                type="primary"
                plain
                size="small"
                :icon="Download"
                :disabled="!archiveAvailable"
                :loading="downloadingKey === 'archive'"
                @click="downloadArchive"
              >
                打包下载
              </el-button>
            </div>
            <el-table :data="detail.files" size="small">
              <el-table-column label="组" width="60" align="center">
                <template #default="{ row }">#{{ row.groupIndex }}</template>
              </el-table-column>
              <el-table-column label="输入大小" width="100">
                <template #default="{ row }">{{ formatBytes(row.inputSizeBytes) }}</template>
              </el-table-column>
              <el-table-column label="输出大小" width="100">
                <template #default="{ row }">{{ formatBytes(row.outputSizeBytes) }}</template>
              </el-table-column>
              <el-table-column label="标准用时" width="100">
                <template #default="{ row }">{{ row.stdTimeMs === null ? '—' : `${row.stdTimeMs} ms` }}</template>
              </el-table-column>
              <el-table-column label="校验" width="110" align="center">
                <template #default="{ row }">
                  <el-tooltip
                    :content="row.validationMessage || validationStatusLabel(row.validationStatus)"
                    placement="top"
                  >
                    <el-tag :type="validationTagType(row.validationStatus)" effect="plain">
                      {{ validationStatusLabel(row.validationStatus) }}
                    </el-tag>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column label="下载" min-width="170">
                <template #default="{ row }">
                  <div class="datagen-file-actions">
                    <el-button
                      link
                      type="primary"
                      size="small"
                      :disabled="!row.inputAvailable"
                      :loading="downloadingKey === `${row.id}:input`"
                      @click="downloadFile(row, 'input')"
                    >
                      输入
                    </el-button>
                    <el-button
                      link
                      type="primary"
                      size="small"
                      :disabled="!row.outputAvailable"
                      :loading="downloadingKey === `${row.id}:output`"
                      @click="downloadFile(row, 'output')"
                    >
                      输出
                    </el-button>
                    <span v-if="row.fileDeletedAt" class="admin-muted">已过期</span>
                  </div>
                </template>
              </el-table-column>
              <template #empty>
                <AdminEmptyState description="暂无产出文件" />
              </template>
            </el-table>
          </section>
        </template>
        <AdminEmptyState v-else-if="!detailLoading" description="未能加载任务详情" />
      </div>
    </el-drawer>
  </AdminPage>
</template>

<style scoped>
.admin-filter-wide {
  width: 170px;
}

.datagen-title-cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
  line-height: 1.4;
}

.datagen-title-cell strong {
  font-size: 13px;
}

.datagen-title-cell small,
.admin-muted {
  color: var(--tz-color-text-muted);
  font-size: 11px;
}

.datagen-detail {
  display: flex;
  min-height: 200px;
  flex-direction: column;
  gap: 18px;
}

.datagen-detail__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.datagen-detail__head h3 {
  margin: 0;
  font-size: 18px;
}

.datagen-detail__head p {
  margin: 4px 0 0;
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

.datagen-detail__section h4 {
  margin: 0 0 10px;
  font-size: 14px;
}

.datagen-label {
  display: inline-block;
  margin-bottom: 6px;
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

.datagen-code {
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

.datagen-code--code {
  max-height: 360px;
}

.datagen-format-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.datagen-files-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.datagen-files-head h4 {
  margin: 0;
}

.datagen-file-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 640px) {
  .datagen-format-grid {
    grid-template-columns: 1fr;
  }
}
</style>
