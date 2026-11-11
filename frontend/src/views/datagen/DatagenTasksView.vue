<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Plus, Refresh, View } from '@element-plus/icons-vue'

import {
  fetchDataGenTasks,
  type DataGenTaskListItem,
  type DataGenTaskStatus
} from '@/api/datagen'
import { resolveApiErrorMessage } from '@/utils/apiError'
import {
  datagenStatusLabel,
  generationModeLabel,
  isRunningStatus,
  statusTagType,
  stdLanguageLabel
} from '@/utils/datagen'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

const tasks = ref<DataGenTaskListItem[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const statusFilter = ref('')
const modeFilter = ref('')

const statusOptions = [
  { value: 'pending', label: '等待中' },
  { value: 'llm_generating', label: 'LLM 构造中' },
  { value: 'llm_data_generating', label: 'LLM 生成数据中' },
  { value: 'generating', label: '生成中' },
  { value: 'success', label: '已完成' },
  { value: 'failed', label: '失败' }
]

const modeOptions = [
  { value: 'random', label: '随机生成' },
  { value: 'llm_generator', label: 'LLM 构造器' },
  { value: 'llm_direct_data', label: 'LLM 直接生成' }
]

async function loadTasks() {
  loading.value = true
  try {
    const result = await fetchDataGenTasks({
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

function handleFilterChange() {
  page.value = 1
  void loadTasks()
}

function handleReset() {
  statusFilter.value = ''
  modeFilter.value = ''
  handleFilterChange()
}

function handlePageChange(nextPage: number) {
  page.value = nextPage
  void loadTasks()
}

function openTask(task: DataGenTaskListItem) {
  void router.push(`/datagen/tasks/${task.id}`)
}

function displayTime(value: string | null): string {
  return value ? formatDateTime(value) : '—'
}

function progressLabel(task: DataGenTaskListItem): string {
  if (isRunningStatus(task.status)) {
    return '实时生成中，点击查看进度'
  }
  if (task.status === 'failed') {
    return '查看失败阶段与原因'
  }
  return '查看产出文件'
}

function statusTag(task: DataGenTaskListItem): DataGenTaskStatus {
  return task.status
}

onMounted(loadTasks)
</script>

<template>
  <div class="datagen-page">
    <header class="datagen-list-head">
      <div>
        <RouterLink to="/datagen" class="datagen-back-link">
          <el-icon><ArrowLeft /></el-icon>
          返回工作台
        </RouterLink>
        <h1>我的造数据任务</h1>
        <p>任务记录永久保留，产出文件自完成起保留 7 天。失败任务不会自动重试。</p>
      </div>
      <div class="datagen-list-head__actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadTasks">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="router.push('/datagen')">
          新建任务
        </el-button>
      </div>
    </header>

    <section class="datagen-list-card tz-card">
      <div class="datagen-toolbar">
        <el-select
          v-model="statusFilter"
          class="datagen-toolbar__select"
          placeholder="全部状态"
          @change="handleFilterChange"
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
          class="datagen-toolbar__select datagen-toolbar__select--wide"
          placeholder="全部生成方式"
          @change="handleFilterChange"
        >
          <el-option label="全部生成方式" value="" />
          <el-option
            v-for="option in modeOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-button @click="handleReset">重置</el-button>
        <span class="datagen-toolbar__meta">共 {{ total }} 个任务</span>
      </div>

      <el-table
        v-loading="loading"
        :data="tasks"
        row-key="id"
        class="datagen-table"
        @row-click="openTask"
      >
        <el-table-column label="任务" min-width="260">
          <template #default="{ row }">
            <div class="datagen-task-title">
              <strong>{{ row.title }}</strong>
              <small>{{ progressLabel(row) }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="生成方式" width="130">
          <template #default="{ row }">
            <span class="datagen-cell-strong">{{ generationModeLabel(row.generationMode) }}</span>
            <small v-if="row.llmModel" class="datagen-cell-sub">{{ row.llmModel }}</small>
          </template>
        </el-table-column>
        <el-table-column label="标准程序" width="120">
          <template #default="{ row }">
            {{ stdLanguageLabel(row.stdLanguage) }}
          </template>
        </el-table-column>
        <el-table-column label="组数" width="80" align="center">
          <template #default="{ row }">{{ row.groupCount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(statusTag(row))" effect="plain">
              <span class="datagen-status-dot" :class="{ 'is-running': isRunningStatus(row.status) }"></span>
              {{ datagenStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ displayTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="完成时间" width="170">
          <template #default="{ row }">{{ displayTime(row.finishedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click.stop="openTask(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="还没有造数据任务">
            <el-button type="primary" @click="router.push('/datagen')">去创建第一个任务</el-button>
          </el-empty>
        </template>
      </el-table>

      <div class="datagen-pagination">
        <el-pagination
          :current-page="page"
          :page-size="pageSize"
          :total="total"
          background
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </section>
  </div>
</template>
