<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleClose,
  Delete,
  Edit,
  Plus,
  Refresh,
  Search,
  VideoPlay,
  View
} from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'

import {
  createAdminCourse,
  createAdminStage,
  deleteAdminCourse,
  deleteAdminStage,
  fetchAdminProblems,
  fetchAdminRoutes,
  fetchAdminStage,
  fetchAdminStages,
  offlineAdminStage,
  publishAdminStage,
  replaceAdminStageProblems,
  updateAdminCourse,
  updateAdminStage,
  type AdminProblemBrief,
  type AdminProblemSummary,
  type AdminRouteView,
  type AdminStageDetail,
  type AdminStageProblemView,
  type AdminStageSummary,
  type LearningStatus
} from '@/api/admin'
import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminPage from '@/components/admin/AdminPage.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import AdminTableCard from '@/components/admin/AdminTableCard.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'
import {
  contentStatusLabel,
  contentStatusTagType,
  objectiveTypeLabel,
  problemDifficultyLabel,
  problemTypeLabel
} from '@/utils/adminLabels'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatDateTime } from '@/utils/format'

interface StageStatusTarget {
  id: string
  name: string
  status: LearningStatus
}

interface StageEditorTarget {
  id: string
  routeId: string
}

const BILIBILI_URL_PATTERN =
  /^https?:\/\/(?:www\.|m\.)?bilibili\.com\/video\/BV[0-9A-Za-z]{10}(?:[/?#].*)?$/

const route = useRoute()
const router = useRouter()

const routeOptions = ref<AdminRouteView[]>([])
const routeFilter = ref(readRouteIdFromQuery())
const keyword = ref('')
const statusFilter = ref<'' | LearningStatus>('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)
const stages = ref<AdminStageSummary[]>([])

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<AdminStageDetail | null>(null)

const stageFormVisible = ref(false)
const stageFormSubmitting = ref(false)
const stageFormRef = ref<FormInstance>()
const editingStageId = ref<string | null>(null)
const stageForm = reactive({
  routeId: '',
  name: '',
  content: '',
  sortOrder: 0
})
const stageFormRules: FormRules = {
  routeId: [{ required: true, message: '请选择所属路线', trigger: 'change' }],
  name: [
    { required: true, message: '请输入阶段名称', trigger: 'blur' },
    { max: 100, message: '阶段名称不能超过 100 个字符', trigger: 'blur' }
  ],
  content: [{ max: 200_000, message: '阶段内容不能超过 200000 个字符', trigger: 'blur' }]
}

const courseFormVisible = ref(false)
const courseFormSubmitting = ref(false)
const courseFormRef = ref<FormInstance>()
const editingCourseId = ref<string | null>(null)
const courseForm = reactive<{
  title: string
  url: string
  durationSeconds: number | null
  source: string
  sortOrder: number
  status: LearningStatus
}>({
  title: '',
  url: '',
  durationSeconds: null,
  source: '',
  sortOrder: 0,
  status: 'published'
})
const courseFormRules: FormRules = {
  title: [
    { required: true, message: '请输入课程标题', trigger: 'blur' },
    { max: 200, message: '课程标题不能超过 200 个字符', trigger: 'blur' }
  ],
  url: [
    { required: true, message: '请输入 B 站视频链接', trigger: 'blur' },
    { max: 512, message: '课程链接不能超过 512 个字符', trigger: 'blur' },
    {
      pattern: BILIBILI_URL_PATTERN,
      message: '请输入形如 https://www.bilibili.com/video/BV... 的链接',
      trigger: 'blur'
    }
  ],
  source: [{ max: 128, message: '课程来源不能超过 128 个字符', trigger: 'blur' }]
}

const problemsDialogVisible = ref(false)
const problemsSubmitting = ref(false)
const problemDraft = ref<AdminStageProblemView[]>([])

const pickerVisible = ref(false)
const pickerLoading = ref(false)
const pickerProblems = ref<AdminProblemSummary[]>([])
const pickerTotal = ref(0)
const pickerPage = ref(1)
const pickerPageSize = ref(10)
const pickerKeyword = ref('')
const pickerType = ref('')
const pickerDifficulty = ref('')

function readRouteIdFromQuery(): string {
  return typeof route.query.routeId === 'string' ? route.query.routeId : ''
}

function stageQueryParams() {
  return {
    routeId: routeFilter.value || undefined,
    keyword: keyword.value.trim() || undefined,
    status: statusFilter.value || undefined,
    page: page.value,
    pageSize: pageSize.value
  }
}

async function loadRouteOptions() {
  try {
    const result = await fetchAdminRoutes({ page: 1, pageSize: 100 })
    routeOptions.value = result.items
  } catch (error) {
    routeOptions.value = []
    ElMessage.error(resolveApiErrorMessage(error, '路线选项加载失败'))
  }
}

async function loadStages() {
  loading.value = true
  try {
    const result = await fetchAdminStages(stageQueryParams())
    stages.value = result.items
    total.value = result.total
  } catch (error) {
    stages.value = []
    total.value = 0
    ElMessage.error(resolveApiErrorMessage(error, '阶段列表加载失败'))
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  void loadStages()
}

function handleRouteFilterChange(value: string) {
  page.value = 1
  void router.replace({
    query: {
      ...route.query,
      routeId: value || undefined
    }
  })
  void loadStages()
}

function handleResetFilters() {
  keyword.value = ''
  statusFilter.value = ''
  routeFilter.value = ''
  page.value = 1
  void router.replace({
    query: {
      ...route.query,
      routeId: undefined
    }
  })
  void loadStages()
}

async function openDetail(row: { id: string }) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await fetchAdminStage(row.id)
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '阶段详情加载失败'))
  } finally {
    detailLoading.value = false
  }
}

async function refreshDetailIfOpen(stageId: string) {
  if (!detailVisible.value || detail.value?.id !== stageId) {
    return
  }
  try {
    detail.value = await fetchAdminStage(stageId)
  } catch {
    detailVisible.value = false
    detail.value = null
  }
}

function openCreateStageDialog() {
  if (!routeOptions.value.length) {
    ElMessage.warning('请先在路线管理中创建路线')
    return
  }
  editingStageId.value = null
  stageForm.routeId = routeFilter.value || routeOptions.value[0]?.id || ''
  stageForm.name = ''
  stageForm.content = ''
  stageForm.sortOrder = 0
  stageFormVisible.value = true
  stageFormRef.value?.clearValidate()
}

async function openEditStageDialog(row: StageEditorTarget) {
  try {
    const stageDetail = await fetchAdminStage(row.id)
    editingStageId.value = stageDetail.id
    stageForm.routeId = stageDetail.routeId
    stageForm.name = stageDetail.name
    stageForm.content = stageDetail.content ?? ''
    stageForm.sortOrder = stageDetail.sortOrder
    stageFormVisible.value = true
    stageFormRef.value?.clearValidate()
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '阶段详情加载失败'))
  }
}

async function submitStageForm() {
  const instance = stageFormRef.value
  if (!instance) {
    return
  }
  const valid = await instance.validate().catch(() => false)
  if (!valid) {
    return
  }
  stageFormSubmitting.value = true
  try {
    const name = stageForm.name.trim()
    const content = stageForm.content.trim()
    if (editingStageId.value) {
      const updated = await updateAdminStage(editingStageId.value, {
        name,
        content: content || null,
        sortOrder: stageForm.sortOrder
      })
      stageFormVisible.value = false
      ElMessage.success(`已保存阶段「${name}」`)
      if (detail.value?.id === updated.id) {
        detail.value = updated
      }
      await loadStages()
    } else {
      const created = await createAdminStage({
        routeId: stageForm.routeId,
        name,
        content: content || undefined,
        sortOrder: stageForm.sortOrder
      })
      stageFormVisible.value = false
      ElMessage.success(`已新增阶段「${name}」，当前为草稿`)
      await loadStages()
      detail.value = created
      detailVisible.value = true
    }
  } catch (error) {
    ElMessage.error(
      resolveApiErrorMessage(error, editingStageId.value ? '阶段保存失败' : '阶段创建失败')
    )
  } finally {
    stageFormSubmitting.value = false
  }
}

async function toggleStatus(row: StageStatusTarget) {
  const publishing = row.status !== 'published'
  try {
    await ElMessageBox.confirm(
      publishing
        ? `发布后阶段「${row.name}」将立即出现在前台学习内容中。确认发布吗？`
        : `下架后阶段「${row.name}」将不再出现在前台，已产生的学习进度保留。确认下架吗？`,
      publishing ? '发布阶段' : '下架阶段',
      {
        confirmButtonText: publishing ? '确认发布' : '确认下架',
        cancelButtonText: '取消',
        type: publishing ? 'info' : 'warning'
      }
    )
  } catch {
    return
  }

  try {
    if (publishing) {
      await publishAdminStage(row.id)
    } else {
      await offlineAdminStage(row.id)
    }
    ElMessage.success(publishing ? '阶段已发布' : '阶段已下架')
    await loadStages()
    await refreshDetailIfOpen(row.id)
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, publishing ? '发布失败' : '下架失败'))
  }
}

async function confirmDeleteStage(row: AdminStageSummary) {
  try {
    await ElMessageBox.confirm(
      `删除后阶段「${row.name}」将从后台移除。存在课程、题目关联或学习进度时不能删除。确认删除吗？`,
      '删除阶段',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  try {
    await deleteAdminStage(row.id)
    if (detail.value?.id === row.id) {
      detailVisible.value = false
      detail.value = null
    }
    if (stages.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    ElMessage.success('阶段已删除')
    await loadStages()
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '阶段删除失败'))
  }
}

function openCreateCourseDialog() {
  editingCourseId.value = null
  courseForm.title = ''
  courseForm.url = ''
  courseForm.durationSeconds = null
  courseForm.source = ''
  courseForm.sortOrder = detail.value?.courses.length ?? 0
  courseForm.status = 'published'
  courseFormVisible.value = true
  courseFormRef.value?.clearValidate()
}

function openEditCourseDialog(course: AdminStageDetail['courses'][number]) {
  editingCourseId.value = course.id
  courseForm.title = course.title
  courseForm.url = course.url
  courseForm.durationSeconds = course.durationSeconds
  courseForm.source = course.source ?? ''
  courseForm.sortOrder = course.sortOrder
  courseForm.status = course.status
  courseFormVisible.value = true
  courseFormRef.value?.clearValidate()
}

async function submitCourseForm() {
  const instance = courseFormRef.value
  if (!instance || !detail.value) {
    return
  }
  const valid = await instance.validate().catch(() => false)
  if (!valid) {
    return
  }
  courseFormSubmitting.value = true
  try {
    const stageId = detail.value.id
    const payload = {
      title: courseForm.title.trim(),
      url: courseForm.url.trim(),
      durationSeconds: courseForm.durationSeconds ?? null,
      source: courseForm.source.trim() || null,
      sortOrder: courseForm.sortOrder,
      status: courseForm.status
    }
    if (editingCourseId.value) {
      await updateAdminCourse(editingCourseId.value, payload)
      ElMessage.success(`已保存课程「${payload.title}」`)
    } else {
      await createAdminCourse(stageId, payload)
      ElMessage.success(`已新增课程「${payload.title}」`)
    }
    courseFormVisible.value = false
    detail.value = await fetchAdminStage(stageId)
    await loadStages()
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, editingCourseId.value ? '课程保存失败' : '课程创建失败'))
  } finally {
    courseFormSubmitting.value = false
  }
}

async function confirmDeleteCourse(course: AdminStageDetail['courses'][number]) {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `删除后课程「${course.title}」将立即从前台阶段内容中移除。确认删除吗？`,
      '删除课程',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  const stageId = detail.value.id
  try {
    await deleteAdminCourse(course.id)
    ElMessage.success('课程已删除')
    detail.value = await fetchAdminStage(stageId)
    await loadStages()
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '课程删除失败'))
  }
}

function openProblemsDialog() {
  if (!detail.value) {
    return
  }
  problemDraft.value = detail.value.problems
    .slice()
    .sort((left, right) => left.sortOrder - right.sortOrder)
    .map((item) => ({
      problem: { ...item.problem },
      sortOrder: item.sortOrder
    }))
  problemsDialogVisible.value = true
}

function moveDraftProblem(index: number, offset: number) {
  const target = index + offset
  if (target < 0 || target >= problemDraft.value.length) {
    return
  }
  const [moved] = problemDraft.value.splice(index, 1)
  if (moved) {
    problemDraft.value.splice(target, 0, moved)
  }
}

function removeDraftProblem(index: number) {
  problemDraft.value.splice(index, 1)
}

function isProblemAdded(problemId: string): boolean {
  return problemDraft.value.some((item) => item.problem.id === problemId)
}

function addPickerProblem(problem: AdminProblemSummary) {
  if (isProblemAdded(problem.id)) {
    return
  }
  const brief: AdminProblemBrief = {
    id: problem.id,
    type: problem.type,
    objectiveType: problem.objectiveType,
    title: problem.title,
    difficulty: problem.difficulty,
    source: problem.source,
    platform: problem.platform,
    status: problem.status
  }
  problemDraft.value.push({ problem: brief, sortOrder: problemDraft.value.length })
}

async function saveProblems() {
  if (!detail.value) {
    return
  }
  const unavailable = problemDraft.value.filter(
    (item) => item.problem.status !== 'published'
  )
  if (unavailable.length) {
    ElMessage.warning(`存在 ${unavailable.length} 道未发布或已下架题目，请移除后再保存`)
    return
  }
  const stageId = detail.value.id
  problemsSubmitting.value = true
  try {
    await replaceAdminStageProblems(
      stageId,
      problemDraft.value.map((item, index) => ({
        problemId: item.problem.id,
        sortOrder: index
      }))
    )
    problemsDialogVisible.value = false
    ElMessage.success('阶段题目已更新')
    detail.value = await fetchAdminStage(stageId)
    await loadStages()
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '阶段题目保存失败'))
  } finally {
    problemsSubmitting.value = false
  }
}

function openProblemPicker() {
  pickerKeyword.value = ''
  pickerType.value = ''
  pickerDifficulty.value = ''
  pickerPage.value = 1
  pickerVisible.value = true
  void loadPickerProblems()
}

async function loadPickerProblems() {
  pickerLoading.value = true
  try {
    const result = await fetchAdminProblems({
      status: 'published',
      type: pickerType.value || undefined,
      difficulty: pickerDifficulty.value || undefined,
      keyword: pickerKeyword.value.trim() || undefined,
      page: pickerPage.value,
      pageSize: pickerPageSize.value
    })
    pickerProblems.value = result.items
    pickerTotal.value = result.total
  } catch (error) {
    pickerProblems.value = []
    pickerTotal.value = 0
    ElMessage.error(resolveApiErrorMessage(error, '题目列表加载失败'))
  } finally {
    pickerLoading.value = false
  }
}

function handlePickerSearch() {
  pickerPage.value = 1
  void loadPickerProblems()
}

function formatDuration(seconds: number | null): string {
  if (seconds === null || seconds === undefined) {
    return '—'
  }
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const remainSeconds = seconds % 60
  const paddedSeconds = String(remainSeconds).padStart(2, '0')
  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, '0')}:${paddedSeconds}`
  }
  return `${minutes}:${paddedSeconds}`
}

watch(
  () => route.query.routeId,
  (value) => {
    const next = typeof value === 'string' ? value : ''
    if (next === routeFilter.value) {
      return
    }
    routeFilter.value = next
    page.value = 1
    void loadStages()
  }
)

onMounted(() => {
  void loadRouteOptions()
  void loadStages()
})
</script>

<template>
  <AdminPage
    title="阶段管理"
    description="跨路线查看与维护阶段；阶段内容、视频课程与关联题目均在详情中管理。阶段必须归属于一条未删除的路线。"
  >
    <template #actions>
      <el-button type="primary" :icon="Plus" @click="openCreateStageDialog">新增阶段</el-button>
    </template>

    <AdminTableCard
      title="阶段列表"
      description="按路线与阶段排序值展示；草稿和已下架阶段也会出现在后台列表。"
    >
      <template #toolbar>
        <AdminToolbar>
          <el-select
            v-model="routeFilter"
            class="admin-filter-wide"
            placeholder="全部路线"
            filterable
            @change="handleRouteFilterChange"
          >
            <el-option label="全部路线" value="" />
            <el-option
              v-for="routeOption in routeOptions"
              :key="routeOption.id"
              :label="routeOption.name"
              :value="routeOption.id"
            />
          </el-select>
          <el-input
            v-model="keyword"
            class="admin-search"
            placeholder="搜索阶段名称或内容"
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
            <el-option label="草稿" value="draft" />
            <el-option label="已发布" value="published" />
            <el-option label="已下架" value="offline" />
          </el-select>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleResetFilters">重置</el-button>
          <template #meta>共 {{ total }} 个阶段</template>
        </AdminToolbar>
      </template>

      <el-table v-loading="loading" :data="stages" row-key="id">
        <el-table-column prop="routeName" label="路线" min-width="140" />
        <el-table-column label="阶段" min-width="200">
          <template #default="{ row }">
            <div class="stage-name-cell">
              <strong>{{ row.name }}</strong>
              <small>ID {{ row.id }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
        <el-table-column prop="courseCount" label="课程" width="70" align="center" />
        <el-table-column prop="problemCount" label="题目" width="70" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="contentStatusTagType(row.status)" effect="plain">
              {{ contentStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="290" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" :icon="Edit" @click="openEditStageDialog(row)">
              编辑
            </el-button>
            <el-button
              link
              :type="row.status === 'published' ? 'warning' : 'success'"
              :icon="row.status === 'published' ? CircleClose : VideoPlay"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'published' ? '下架' : '发布' }}
            </el-button>
            <el-button link type="danger" :icon="Delete" @click="confirmDeleteStage(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <AdminEmptyState description="没有符合条件的阶段，可新增阶段或调整筛选条件" />
        </template>
      </el-table>

      <template #footer>
        <AdminPagination
          v-model:page="page"
          v-model:page-size="pageSize"
          :total="total"
          @change="loadStages"
        />
      </template>
    </AdminTableCard>

    <el-dialog
      v-model="stageFormVisible"
      :title="editingStageId ? '编辑阶段' : '新增阶段'"
      width="640px"
      destroy-on-close
    >
      <el-form ref="stageFormRef" :model="stageForm" :rules="stageFormRules" label-width="88px">
        <el-form-item label="所属路线" prop="routeId">
          <el-select
            v-model="stageForm.routeId"
            class="stage-route-select"
            placeholder="请选择路线"
            filterable
            :disabled="Boolean(editingStageId)"
          >
            <el-option
              v-for="routeOption in routeOptions"
              :key="routeOption.id"
              :label="routeOption.name"
              :value="routeOption.id"
            />
          </el-select>
          <span v-if="editingStageId" class="stage-form-hint">不支持跨路线移动阶段</span>
        </el-form-item>
        <el-form-item label="阶段名称" prop="name">
          <el-input v-model="stageForm.name" maxlength="100" show-word-limit placeholder="例如：数组与字符串" />
        </el-form-item>
        <el-form-item label="阶段内容" prop="content">
          <el-input
            v-model="stageForm.content"
            type="textarea"
            :rows="8"
            maxlength="200000"
            placeholder="填写阶段的学习目标、知识讲解与练习建议"
          />
        </el-form-item>
        <el-form-item label="排序值" prop="sortOrder">
          <el-input-number v-model="stageForm.sortOrder" :min="-999999" :max="999999" controls-position="right" />
          <span class="stage-form-hint">数值越小越靠前</span>
        </el-form-item>
        <el-alert
          v-if="!editingStageId"
          type="info"
          :closable="false"
          show-icon
          title="新增阶段默认为草稿，需要发布后才会出现在前台。"
        />
      </el-form>
      <template #footer>
        <el-button @click="stageFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="stageFormSubmitting" @click="submitStageForm">
          {{ editingStageId ? '保存修改' : '创建阶段' }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="detailVisible"
      class="admin-detail-drawer"
      title="阶段详情"
      size="720px"
    >
      <div v-loading="detailLoading" class="stage-detail">
        <template v-if="detail">
          <div class="stage-detail__head">
            <div>
              <div class="stage-detail__title">
                <h2>{{ detail.name }}</h2>
                <el-tag :type="contentStatusTagType(detail.status)" effect="plain">
                  {{ contentStatusLabel(detail.status) }}
                </el-tag>
              </div>
              <p>所属路线：{{ detail.routeName }}</p>
            </div>
            <div class="stage-detail__actions">
              <el-button size="small" :icon="Edit" @click="openEditStageDialog(detail)">
                编辑阶段
              </el-button>
              <el-button
                size="small"
                :type="detail.status === 'published' ? 'warning' : 'success'"
                :icon="detail.status === 'published' ? CircleClose : VideoPlay"
                @click="toggleStatus(detail)"
              >
                {{ detail.status === 'published' ? '下架' : '发布' }}
              </el-button>
            </div>
          </div>

          <dl class="stage-detail__meta">
            <div>
              <dt>排序值</dt>
              <dd>{{ detail.sortOrder }}</dd>
            </div>
            <div>
              <dt>更新于</dt>
              <dd>{{ formatDateTime(detail.updatedAt) }}</dd>
            </div>
            <div>
              <dt>课程数量</dt>
              <dd>{{ detail.courses.length }}</dd>
            </div>
            <div>
              <dt>题目数量</dt>
              <dd>{{ detail.problems.length }}</dd>
            </div>
          </dl>

          <section class="stage-section">
            <div class="stage-section__head">
              <div>
                <h3>阶段内容</h3>
                <p>前台展示的图文内容</p>
              </div>
            </div>
            <div class="stage-content">{{ detail.content || '暂无阶段内容' }}</div>
          </section>

          <section class="stage-section">
            <div class="stage-section__head">
              <div>
                <h3>视频课程</h3>
                <p>仅支持 B 站视频链接；观看课程不计入学习进度</p>
              </div>
              <el-button size="small" type="primary" :icon="Plus" @click="openCreateCourseDialog">
                新增课程
              </el-button>
            </div>
            <el-table :data="detail.courses" size="small" row-key="id">
              <el-table-column label="课程" min-width="230">
                <template #default="{ row }">
                  <el-link
                    :href="row.url"
                    target="_blank"
                    rel="noopener noreferrer"
                    type="primary"
                    :underline="false"
                  >
                    {{ row.title }}
                  </el-link>
                </template>
              </el-table-column>
              <el-table-column label="时长" width="80" align="center">
                <template #default="{ row }">{{ formatDuration(row.durationSeconds) }}</template>
              </el-table-column>
              <el-table-column label="来源" min-width="110">
                <template #default="{ row }">{{ row.source || '—' }}</template>
              </el-table-column>
              <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="contentStatusTagType(row.status)" effect="plain" size="small">
                    {{ contentStatusLabel(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="130">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openEditCourseDialog(row)">编辑</el-button>
                  <el-button link type="danger" @click="confirmDeleteCourse(row)">删除</el-button>
                </template>
              </el-table-column>
              <template #empty>
                <AdminEmptyState description="该阶段还没有视频课程" />
              </template>
            </el-table>
          </section>

          <section class="stage-section">
            <div class="stage-section__head">
              <div>
                <h3>关联题目</h3>
                <p>只能关联已发布题目；保存时全量替换关联列表</p>
              </div>
              <el-button size="small" type="primary" :icon="Plus" @click="openProblemsDialog">
                管理题目
              </el-button>
            </div>
            <el-table :data="detail.problems" size="small" row-key="problem.id">
              <el-table-column label="顺序" width="64" align="center">
                <template #default="{ $index }">{{ $index + 1 }}</template>
              </el-table-column>
              <el-table-column label="题目" min-width="240">
                <template #default="{ row }">
                  <div class="problem-title-cell">
                    <strong>{{ row.problem.title }}</strong>
                    <small>ID {{ row.problem.id }}</small>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="类型 / 难度" width="150">
                <template #default="{ row }">
                  {{ problemTypeLabel(row.problem.type) }} ·
                  {{ problemDifficultyLabel(row.problem.difficulty) }}
                </template>
              </el-table-column>
              <el-table-column label="来源" min-width="120">
                <template #default="{ row }">{{ row.problem.source || '—' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag
                    :type="contentStatusTagType(row.problem.status)"
                    effect="plain"
                    size="small"
                  >
                    {{ contentStatusLabel(row.problem.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <template #empty>
                <AdminEmptyState description="该阶段还没有关联题目" />
              </template>
            </el-table>
          </section>
        </template>
      </div>
    </el-drawer>

    <el-dialog v-model="courseFormVisible" :title="editingCourseId ? '编辑课程' : '新增课程'" width="560px" destroy-on-close>
      <el-form ref="courseFormRef" :model="courseForm" :rules="courseFormRules" label-width="96px">
        <el-form-item label="课程标题" prop="title">
          <el-input v-model="courseForm.title" maxlength="200" show-word-limit placeholder="例如：前缀和入门讲解" />
        </el-form-item>
        <el-form-item label="B 站链接" prop="url">
          <el-input v-model="courseForm.url" maxlength="512" placeholder="https://www.bilibili.com/video/BV..." />
        </el-form-item>
        <el-form-item label="时长（秒）" prop="durationSeconds">
          <el-input-number
            v-model="courseForm.durationSeconds"
            :min="0"
            :max="864000"
            :value-on-clear="null"
            controls-position="right"
            placeholder="选填"
          />
        </el-form-item>
        <el-form-item label="来源 / UP 主" prop="source">
          <el-input v-model="courseForm.source" maxlength="128" placeholder="选填" />
        </el-form-item>
        <el-form-item label="排序值" prop="sortOrder">
          <el-input-number v-model="courseForm.sortOrder" :min="-999999" :max="999999" controls-position="right" />
        </el-form-item>
        <el-form-item label="课程状态" prop="status">
          <el-select v-model="courseForm.status" class="stage-route-select">
            <el-option label="草稿" value="draft" />
            <el-option label="已发布" value="published" />
            <el-option label="已下架" value="offline" />
          </el-select>
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="链接会规范化为 https://www.bilibili.com/video/BV...；非 B 站视频将被拒绝。"
        />
      </el-form>
      <template #footer>
        <el-button @click="courseFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="courseFormSubmitting" @click="submitCourseForm">
          {{ editingCourseId ? '保存修改' : '新增课程' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="problemsDialogVisible" title="管理阶段题目" width="760px" destroy-on-close>
      <div class="problems-dialog">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="保存时会全量替换该阶段的题目关联；空列表表示解除全部关联。"
        />
        <div class="problems-dialog__head">
          <span>已选择 {{ problemDraft.length }} 道题目</span>
          <el-button size="small" type="primary" :icon="Plus" @click="openProblemPicker">
            添加题目
          </el-button>
        </div>
        <el-table :data="problemDraft" size="small" row-key="problem.id" max-height="380">
          <el-table-column label="顺序" width="64" align="center">
            <template #default="{ $index }">{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column label="题目" min-width="260">
            <template #default="{ row }">
              <div class="problem-title-cell">
                <strong>{{ row.problem.title }}</strong>
                <small>ID {{ row.problem.id }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="类型 / 难度" width="150">
            <template #default="{ row }">
              {{ problemTypeLabel(row.problem.type) }} ·
              {{ problemDifficultyLabel(row.problem.difficulty) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="contentStatusTagType(row.problem.status)" effect="plain" size="small">
                {{ contentStatusLabel(row.problem.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ $index }">
              <el-button link :disabled="$index === 0" @click="moveDraftProblem($index, -1)">
                上移
              </el-button>
              <el-button
                link
                :disabled="$index === problemDraft.length - 1"
                @click="moveDraftProblem($index, 1)"
              >
                下移
              </el-button>
              <el-button link type="danger" @click="removeDraftProblem($index)">移除</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <AdminEmptyState description="尚未选择题目，可点击「添加题目」选择" />
          </template>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="problemsDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="problemsSubmitting" @click="saveProblems">
          保存题目
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="pickerVisible"
      title="选择题目"
      width="900px"
      append-to-body
      destroy-on-close
    >
      <AdminToolbar>
        <el-input
          v-model="pickerKeyword"
          class="admin-search"
          placeholder="搜索题目标题"
          clearable
          @keyup.enter="handlePickerSearch"
          @clear="handlePickerSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="pickerType" class="admin-filter" @change="handlePickerSearch">
          <el-option label="全部题型" value="" />
          <el-option label="OJ 编程题" value="oj" />
          <el-option label="客观题" value="objective" />
          <el-option label="简答题" value="short_answer" />
          <el-option label="外链题" value="external" />
        </el-select>
        <el-select v-model="pickerDifficulty" class="admin-filter" @change="handlePickerSearch">
          <el-option label="全部难度" value="" />
          <el-option label="入门" value="entry" />
          <el-option label="基础" value="basic" />
          <el-option label="进阶" value="advanced" />
        </el-select>
        <el-button type="primary" @click="handlePickerSearch">查询</el-button>
        <template #meta>共 {{ pickerTotal }} 道已发布题目</template>
      </AdminToolbar>

      <el-table v-loading="pickerLoading" :data="pickerProblems" size="small" row-key="id">
        <el-table-column label="题目" min-width="260">
          <template #default="{ row }">
            <div class="problem-title-cell">
              <strong>{{ row.title }}</strong>
              <small>ID {{ row.id }} · {{ row.platform || '站内' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="题型" width="170">
          <template #default="{ row }">
            {{ problemTypeLabel(row.type) }}
            <span v-if="row.objectiveType" class="picker-objective">
              {{ objectiveTypeLabel(row.objectiveType) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="90" align="center">
          <template #default="{ row }">{{ problemDifficultyLabel(row.difficulty) }}</template>
        </el-table-column>
        <el-table-column label="来源" min-width="120">
          <template #default="{ row }">{{ row.source || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :disabled="isProblemAdded(row.id)"
              @click="addPickerProblem(row)"
            >
              {{ isProblemAdded(row.id) ? '已添加' : '添加' }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <AdminEmptyState description="暂无可选题目（题库中还没有已发布题目）" />
        </template>
      </el-table>

      <div class="picker-footer">
        <AdminPagination
          v-model:page="pickerPage"
          v-model:page-size="pickerPageSize"
          :total="pickerTotal"
          :page-sizes="[10, 20, 50]"
          @change="loadPickerProblems"
        />
      </div>

      <template #footer>
        <el-button type="primary" @click="pickerVisible = false">完成选择</el-button>
      </template>
    </el-dialog>
  </AdminPage>
</template>

<style scoped>
.admin-filter-wide {
  width: 190px;
}

.stage-name-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stage-name-cell strong {
  color: var(--tz-color-text);
  font-size: 14px;
  font-weight: 600;
}

.stage-name-cell small {
  color: var(--tz-color-text-muted);
  font-size: 11px;
}

.stage-route-select {
  width: 100%;
  max-width: 320px;
}

.stage-form-hint {
  margin-left: 12px;
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

.stage-detail__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.stage-detail__title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stage-detail__title h2 {
  margin: 0;
  color: var(--tz-color-text);
  font-size: 20px;
}

.stage-detail__head p {
  margin: 8px 0 0;
  color: var(--tz-color-text-muted);
  font-size: 13px;
}

.stage-detail__actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
}

.stage-detail__meta {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 0 0 24px;
}

.stage-detail__meta > div {
  padding: 10px 12px;
  border: 1px solid var(--tz-color-border);
  border-radius: 10px;
  background: var(--tz-color-surface);
}

.stage-detail__meta dt {
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

.stage-detail__meta dd {
  margin: 4px 0 0;
  color: var(--tz-color-text);
  font-size: 13px;
  font-weight: 600;
}

.stage-section {
  margin-bottom: 28px;
}

.stage-section__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.stage-section__head h3 {
  margin: 0;
  color: var(--tz-color-text);
  font-size: 15px;
}

.stage-section__head p {
  margin: 4px 0 0;
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

.stage-content {
  max-height: 260px;
  overflow: auto;
  padding: 14px 16px;
  border: 1px solid var(--tz-color-border);
  border-radius: 12px;
  background: var(--tz-color-surface);
  color: var(--tz-color-text-secondary);
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.problem-title-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.problem-title-cell strong {
  color: var(--tz-color-text);
  font-size: 13px;
  font-weight: 600;
}

.problem-title-cell small {
  color: var(--tz-color-text-muted);
  font-size: 11px;
}

.problems-dialog {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.problems-dialog__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--tz-color-text-muted);
  font-size: 13px;
}

.picker-objective {
  margin-left: 6px;
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

.picker-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
}

@media (max-width: 900px) {
  .stage-detail__meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .admin-filter-wide {
    width: 100%;
  }

  .stage-detail__head {
    flex-direction: column;
  }

  .stage-detail__meta {
    grid-template-columns: 1fr;
  }

  .stage-section__head {
    flex-direction: column;
  }
}
</style>
