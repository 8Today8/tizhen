<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Download,
  FolderOpened,
  Refresh,
  Tickets,
  WarningFilled
} from '@element-plus/icons-vue'

import {
  datagenArchiveDownloadUrl,
  datagenFileDownloadUrl,
  fetchDataGenTask,
  type DataGenFile,
  type DataGenTaskDetail
} from '@/api/datagen'
import { download } from '@/api/http'
import CodeEditor from '@/components/problem/CodeEditor.vue'
import { resolveApiErrorMessage } from '@/utils/apiError'
import {
  datagenStatusLabel,
  failureStageLabel,
  formatFileSize,
  generationModeLabel,
  isRunningStatus,
  statusTagType,
  stdLanguageLabel,
  validationStatusLabel,
  validationTagType
} from '@/utils/datagen'
import { formatDateTime } from '@/utils/format'

const POLL_INTERVAL_MS = 2500

const route = useRoute()
const taskId = computed(() => String(route.params.taskId ?? ''))

const detail = ref<DataGenTaskDetail | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const downloadingKey = ref('')
let pollTimer: ReturnType<typeof setTimeout> | null = null

const running = computed(() => (detail.value ? isRunningStatus(detail.value.status) : false))
const dataRangeItems = computed(() => detail.value?.dataRange ?? [])
const archiveAvailable = computed(() =>
  (detail.value?.files ?? []).some((file) => file.inputAvailable || file.outputAvailable)
)
const summary = computed(() => detail.value?.resultSummary ?? null)

const summaryGroupCount = computed(() => summaryNumber('groupCount'))
const summaryStdTime = computed(() => summaryNumber('totalStdTimeMs'))
const summaryInputBytes = computed(() => summaryNumber('totalInputBytes'))
const summaryOutputBytes = computed(() => summaryNumber('totalOutputBytes'))

function summaryNumber(key: string): number | null {
  const value = summary.value?.[key]
  return typeof value === 'number' ? value : null
}

async function loadDetail(silent = false) {
  if (!taskId.value) {
    return
  }
  if (!silent) {
    loading.value = true
    errorMessage.value = ''
  }
  try {
    detail.value = await fetchDataGenTask(taskId.value)
  } catch (error) {
    if (!silent) {
      detail.value = null
      errorMessage.value = resolveApiErrorMessage(error, '任务详情加载失败')
    }
  } finally {
    if (!silent) {
      loading.value = false
    }
    schedulePoll()
  }
}

function schedulePoll() {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
  if (running.value) {
    pollTimer = setTimeout(() => {
      void loadDetail(true)
    }, POLL_INTERVAL_MS)
  }
}

function stopPoll() {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

function displayTime(value: string | null): string {
  return value ? formatDateTime(value) : '—'
}

function handleRefresh() {
  stopPoll()
  void loadDetail()
}

async function downloadFile(file: DataGenFile, kind: 'input' | 'output') {
  if (!detail.value) {
    return
  }
  const key = `${file.id}:${kind}`
  downloadingKey.value = key
  try {
    const result = await download(datagenFileDownloadUrl(detail.value.id, file, kind))
    saveBlob(
      result.blob,
      result.filename ?? `group-${file.groupIndex}.${kind === 'input' ? 'in' : 'out'}`
    )
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
  downloadingKey.value = 'archive'
  try {
    const result = await download(datagenArchiveDownloadUrl(detail.value.id))
    saveBlob(result.blob, result.filename ?? `datagen-${detail.value.id}.zip`)
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

function stepState(index: number): 'wait' | 'process' | 'finish' | 'error' {
  if (!detail.value) {
    return 'wait'
  }
  if (detail.value.status === 'failed') {
    return index <= 1 ? 'error' : 'wait'
  }
  if (detail.value.status === 'success') {
    return 'finish'
  }
  if (index === 0) {
    return 'finish'
  }
  if (index === 1) {
    return 'process'
  }
  return 'wait'
}

watch(taskId, () => {
  stopPoll()
  void loadDetail()
})

onMounted(() => {
  void loadDetail()
})

onBeforeUnmount(stopPoll)
</script>

<template>
  <div class="datagen-page">
    <header class="datagen-list-head">
      <div>
        <RouterLink to="/datagen/tasks" class="datagen-back-link">
          <el-icon><ArrowLeft /></el-icon>
          返回任务列表
        </RouterLink>
        <h1>{{ detail?.title || '造数据任务' }}</h1>
        <p v-if="detail">
          任务 ID {{ detail.id }} · {{ generationModeLabel(detail.generationMode) }} ·
          {{ detail.groupCount }} 组数据
        </p>
      </div>
      <div class="datagen-list-head__actions">
        <el-button :icon="Refresh" :loading="loading" @click="handleRefresh">刷新</el-button>
        <el-button
          type="primary"
          :icon="FolderOpened"
          :disabled="!archiveAvailable"
          :loading="downloadingKey === 'archive'"
          @click="downloadArchive"
        >
          打包下载
        </el-button>
      </div>
    </header>

    <el-alert
      v-if="errorMessage"
      type="error"
      :closable="false"
      show-icon
      :title="errorMessage"
    >
      <template #default>
        <el-button size="small" @click="handleRefresh">重新加载</el-button>
      </template>
    </el-alert>

    <div v-loading="loading" class="datagen-detail">
      <template v-if="detail">
        <section class="datagen-detail-hero tz-card">
          <div class="datagen-detail-hero__top">
            <div>
              <div class="datagen-detail-hero__status">
                <el-tag :type="statusTagType(detail.status)" effect="dark" size="large">
                  <span
                    class="datagen-status-dot"
                    :class="{ 'is-running': running }"
                  ></span>
                  {{ datagenStatusLabel(detail.status) }}
                </el-tag>
                <span v-if="running" class="datagen-running-note">
                  正在后台执行，页面每 {{ POLL_INTERVAL_MS / 1000 }} 秒自动刷新
                </span>
                <span v-else class="datagen-running-note">
                  完成于 {{ displayTime(detail.finishedAt) }}
                </span>
              </div>
              <h2>{{ detail.title }}</h2>
              <p>
                {{ generationModeLabel(detail.generationMode) }} ·
                {{ stdLanguageLabel(detail.stdLanguage) }} ·
                时间 {{ detail.timeLimitMs }} ms · 内存 {{ detail.memoryLimitMb }} MB
              </p>
            </div>
            <el-tag type="info" effect="plain">
              文件保留至 {{ displayTime(detail.filesExpireAt) }}
            </el-tag>
          </div>

          <div class="datagen-steps">
            <div
              v-for="(step, index) in [
                { title: '任务已提交', desc: '参数校验与任务入队' },
                { title: '生成测试数据', desc: '输入生成、校验与标准程序运行' },
                { title: '产出可下载', desc: '逐组 .in / .out 与 ZIP 打包' }
              ]"
              :key="step.title"
              class="datagen-step-item"
              :class="`is-${stepState(index)}`"
            >
              <span class="datagen-step-item__dot">
                {{ stepState(index) === 'finish' ? '✓' : index + 1 }}
              </span>
              <div>
                <strong>{{ step.title }}</strong>
                <small>{{ step.desc }}</small>
              </div>
            </div>
          </div>
        </section>

        <el-alert
          v-if="detail.status === 'failed'"
          type="error"
          :closable="false"
          show-icon
          :title="`生成失败 · ${failureStageLabel(detail.failureStage)}`"
          :description="detail.failureReason || '任务未记录失败原因，请重新提交或联系管理员。'"
        />

        <section v-if="detail.status === 'success'" class="datagen-summary-grid">
          <div class="datagen-summary-card">
            <el-icon><Tickets /></el-icon>
            <span>数据组数</span>
            <strong>{{ summaryGroupCount ?? detail.files.length }}</strong>
          </div>
          <div class="datagen-summary-card">
            <el-icon><Download /></el-icon>
            <span>输入体积</span>
            <strong>{{ formatFileSize(summaryInputBytes) }}</strong>
          </div>
          <div class="datagen-summary-card">
            <el-icon><Download /></el-icon>
            <span>输出体积</span>
            <strong>{{ formatFileSize(summaryOutputBytes) }}</strong>
          </div>
          <div class="datagen-summary-card">
            <el-icon><WarningFilled /></el-icon>
            <span>标准程序总用时</span>
            <strong>{{ summaryStdTime === null ? '—' : `${summaryStdTime} ms` }}</strong>
          </div>
        </section>

        <section class="datagen-detail-section tz-card">
          <div class="datagen-detail-section__head">
            <div>
              <h3>产出文件</h3>
              <p>
                文件自任务完成起保留 7 天，过期后元数据仍保留但无法下载。
                <template v-if="detail.filesCleanedAt">
                  本任务文件已于 {{ displayTime(detail.filesCleanedAt) }} 清理。
                </template>
              </p>
            </div>
            <span class="datagen-muted">共 {{ detail.files.length }} 组</span>
          </div>

          <el-table :data="detail.files" row-key="id" class="datagen-table">
            <el-table-column label="组" width="70" align="center">
              <template #default="{ row }">
                <span class="datagen-group-index">#{{ row.groupIndex }}</span>
              </template>
            </el-table-column>
            <el-table-column label="输入大小" width="110">
              <template #default="{ row }">{{ formatFileSize(row.inputSizeBytes) }}</template>
            </el-table-column>
            <el-table-column label="输出大小" width="110">
              <template #default="{ row }">{{ formatFileSize(row.outputSizeBytes) }}</template>
            </el-table-column>
            <el-table-column label="标准用时" width="120">
              <template #default="{ row }">
                {{ row.stdTimeMs === null ? '—' : `${row.stdTimeMs} ms` }}
              </template>
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
            <el-table-column label="下载" min-width="170" align="right">
              <template #default="{ row }">
                <div class="datagen-file-actions">
                  <el-button
                    link
                    type="primary"
                    :disabled="!row.inputAvailable"
                    :loading="downloadingKey === `${row.id}:input`"
                    @click="downloadFile(row, 'input')"
                  >
                    输入 .in
                  </el-button>
                  <el-button
                    link
                    type="primary"
                    :disabled="!row.outputAvailable"
                    :loading="downloadingKey === `${row.id}:output`"
                    @click="downloadFile(row, 'output')"
                  >
                    输出 .out
                  </el-button>
                  <span v-if="row.fileDeletedAt" class="datagen-muted">已清理</span>
                </div>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty
                :description="running ? '数据生成中，请稍候' : '暂无产出文件'"
                :image-size="64"
              />
            </template>
          </el-table>
        </section>

        <div class="datagen-detail-columns">
          <section class="datagen-detail-section tz-card">
            <div class="datagen-detail-section__head">
              <h3>任务配置</h3>
            </div>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="生成方式">
                {{ generationModeLabel(detail.generationMode) }}
              </el-descriptions-item>
              <el-descriptions-item label="标准语言">
                {{ stdLanguageLabel(detail.stdLanguage) }}
              </el-descriptions-item>
              <el-descriptions-item label="限制">
                {{ detail.timeLimitMs }} ms / {{ detail.memoryLimitMb }} MB
              </el-descriptions-item>
              <el-descriptions-item v-if="detail.llmProvider" label="LLM 服务商">
                {{ detail.llmProvider }}
              </el-descriptions-item>
              <el-descriptions-item v-if="detail.llmModel" label="LLM 模型">
                {{ detail.llmModel }}
              </el-descriptions-item>
              <el-descriptions-item
                v-if="detail.generationMode === 'llm_generator'"
                label="构造器状态"
              >
                {{ datagenStatusLabel(detail.generatorStatus) }}
              </el-descriptions-item>
              <el-descriptions-item
                v-if="detail.generationMode === 'llm_direct_data'"
                label="数据生成状态"
              >
                {{ datagenStatusLabel(detail.llmDataStatus) }}
              </el-descriptions-item>
              <el-descriptions-item label="开始时间">
                {{ displayTime(detail.startedAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="完成时间">
                {{ displayTime(detail.finishedAt) }}
              </el-descriptions-item>
            </el-descriptions>
          </section>

          <section class="datagen-detail-section tz-card">
            <div class="datagen-detail-section__head">
              <h3>数据范围</h3>
            </div>
            <el-table :data="dataRangeItems" size="small" class="datagen-range-table">
              <el-table-column prop="name" label="变量" min-width="100" />
              <el-table-column prop="type" label="类型" width="100" />
              <el-table-column label="最小值" min-width="100">
                <template #default="{ row }">{{ row.min }}</template>
              </el-table-column>
              <el-table-column label="最大值" min-width="100">
                <template #default="{ row }">{{ row.max }}</template>
              </el-table-column>
              <template #empty>
                <el-empty description="未配置数据范围" :image-size="48" />
              </template>
            </el-table>
            <div v-if="detail.specialRequirements" class="datagen-special-box">
              <span>特殊构造要求</span>
              <pre>{{ detail.specialRequirements }}</pre>
            </div>
          </section>
        </div>

        <section class="datagen-detail-section tz-card">
          <div class="datagen-detail-section__head">
            <h3>题目信息</h3>
          </div>
          <div class="datagen-problem-grid">
            <div>
              <span class="datagen-label">题面</span>
              <pre class="datagen-text-block">{{ detail.statement }}</pre>
            </div>
            <div>
              <span class="datagen-label">输入格式</span>
              <pre class="datagen-text-block">{{ detail.inputFormat }}</pre>
              <span class="datagen-label">输出格式</span>
              <pre class="datagen-text-block">{{ detail.outputFormat }}</pre>
            </div>
          </div>
        </section>

        <section class="datagen-detail-section tz-card">
          <div class="datagen-detail-section__head">
            <div>
              <h3>标准程序</h3>
              <p>{{ stdLanguageLabel(detail.stdLanguage) }} · 只读预览</p>
            </div>
          </div>
          <CodeEditor
            :model-value="detail.stdCode"
            :language="detail.stdLanguage"
            readonly
          />
        </section>
      </template>

      <el-empty v-else-if="!loading" description="未能加载任务详情" />
    </div>
  </div>
</template>
