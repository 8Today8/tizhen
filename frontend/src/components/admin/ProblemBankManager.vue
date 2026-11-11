<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleClose,
  Delete,
  Document,
  Edit,
  Plus,
  Refresh,
  Search,
  VideoPlay,
  View
} from '@element-plus/icons-vue'

import {
  createAdminProblem,
  deleteAdminProblem,
  fetchAdminProblem,
  fetchAdminProblemTestData,
  fetchAdminProblems,
  offlineAdminProblem,
  publishAdminProblem,
  replaceAdminProblemTestData,
  updateAdminProblem,
  type AdminProblemCreatePayload,
  type AdminProblemDetail,
  type AdminProblemSummary,
  type AdminProblemUpdatePayload,
  type LearningStatus,
  type ObjectiveType,
  type ProblemDifficulty,
  type ProblemType,
  type TestDataItemInput,
  type TestDataView
} from '@/api/admin'
import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminPage from '@/components/admin/AdminPage.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import AdminTableCard from '@/components/admin/AdminTableCard.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'
import {
  contentStatusLabel,
  contentStatusTagType,
  formatBytes,
  objectiveTypeLabel,
  problemDifficultyLabel,
  problemTypeLabel
} from '@/utils/adminLabels'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  type: ProblemType
}>()

interface TypeMeta {
  title: string
  description: string
  tableTitle: string
  tableDescription: string
  createLabel: string
  emptyText: string
  searchPlaceholder: string
}

const TYPE_META: Record<ProblemType, TypeMeta> = {
  oj: {
    title: 'OJ 题库管理',
    description: '维护站内编程题的题面、样例、运行限制与测试数据；已被引用的题目只能下架。',
    tableTitle: 'OJ 题目列表',
    tableDescription: '按更新时间倒序展示全部状态题目；测试数据正文只在详情与测试数据管理器中查看。',
    createLabel: '新增 OJ 题目',
    emptyText: '没有符合条件的 OJ 题目，可新增题目或调整筛选条件',
    searchPlaceholder: '搜索题目名称'
  },
  objective: {
    title: '客观题管理',
    description: '维护单选、多选与判断题的题干、选项、正确答案、解析与标签；答错自动进入选手错题本。',
    tableTitle: '客观题列表',
    tableDescription: '客观题按题型与难度筛选；发布前必须保证选项和正确答案数量正确。',
    createLabel: '新增客观题',
    emptyText: '没有符合条件的客观题，可新增题目或调整筛选条件',
    searchPlaceholder: '搜索客观题题干'
  },
  short_answer: {
    title: '简答题管理',
    description: '维护简答题题干、参考答案与评分要点；评分要点分数之和必须为 100，作答由 AI 按百分制判分。',
    tableTitle: '简答题列表',
    tableDescription: '发布前必须配置参考答案与评分要点；已有提交的题目只能下架，不能删除。',
    createLabel: '新增简答题',
    emptyText: '没有符合条件的简答题，可新增题目或调整筛选条件',
    searchPlaceholder: '搜索简答题题干'
  },
  external: {
    title: '外链题管理',
    description: '维护外部平台经典题入口，只保存标题、平台、链接与标签，不复制题面内容。',
    tableTitle: '外链题列表',
    tableDescription: '外链题以平台与链接为主要信息；发布后前台直接跳转外部地址。',
    createLabel: '新增外链题',
    emptyText: '没有符合条件的外链题，可新增题目或调整筛选条件',
    searchPlaceholder: '搜索外链题名称'
  }
}

const meta = computed(() => TYPE_META[props.type])

const keyword = ref('')
const difficultyFilter = ref<'' | ProblemDifficulty>('')
const statusFilter = ref<'' | LearningStatus>('')
const objectiveTypeFilter = ref<'' | ObjectiveType>('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)
const problems = ref<AdminProblemSummary[]>([])

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<AdminProblemDetail | null>(null)

const testDataLoading = ref(false)
const testDataItems = ref<TestDataView[]>([])
const testDataPage = ref(1)
const testDataPageSize = ref(10)
const testDataTotal = ref(0)

const testDataDialogVisible = ref(false)
const testDataLoadingAll = ref(false)
const testDataSaving = ref(false)
const testDataDraft = ref<TestDataItemInput[]>([])

interface SampleDraft {
  inputData: string
  outputData: string
  explanation: string
}

interface OptionDraft {
  content: string
  isCorrect: boolean
}

interface RubricDraft {
  point: string
  score: number | undefined
  description: string
}

interface ProblemFormState {
  title: string
  statement: string
  difficulty: ProblemDifficulty
  source: string
  tags: string[]
  inputFormat: string
  outputFormat: string
  timeLimitMs: number | undefined
  memoryLimitMb: number | undefined
  samples: SampleDraft[]
  objectiveType: ObjectiveType
  options: OptionDraft[]
  explanation: string
  referenceAnswer: string
  scoringRubric: RubricDraft[]
  externalUrl: string
  platform: string
}

function emptySample(): SampleDraft {
  return { inputData: '', outputData: '', explanation: '' }
}

function defaultOptions(): OptionDraft[] {
  return [
    { content: '', isCorrect: true },
    { content: '', isCorrect: false }
  ]
}

function defaultForm(): ProblemFormState {
  return {
    title: '',
    statement: '',
    difficulty: 'entry',
    source: '站内原创',
    tags: [],
    inputFormat: '',
    outputFormat: '',
    timeLimitMs: 1000,
    memoryLimitMb: 256,
    samples: [emptySample()],
    objectiveType: 'single',
    options: defaultOptions(),
    explanation: '',
    referenceAnswer: '',
    scoringRubric: [{ point: '', score: 100, description: '' }],
    externalUrl: '',
    platform: ''
  }
}

const formVisible = ref(false)
const formSubmitting = ref(false)
const formLoading = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<string | null>(null)
const form = reactive<ProblemFormState>(defaultForm())

function validateHttpUrl(
  _rule: unknown,
  value: unknown,
  callback: (error?: Error) => void
) {
  const text = String(value ?? '').trim()
  if (!text) {
    callback()
    return
  }
  try {
    const url = new URL(text)
    if (url.protocol !== 'http:' && url.protocol !== 'https:') {
      throw new Error('invalid protocol')
    }
    callback()
  } catch {
    callback(new Error('请输入有效的 http 或 https 链接'))
  }
}

const formRules = computed<FormRules>(() => {
  const rules: FormRules = {
    title: [
      { required: true, message: '请输入题目名称', trigger: 'blur' },
      { max: 200, message: '题目名称不能超过 200 个字符', trigger: 'blur' }
    ],
    difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
    source: [
      { required: true, message: '请输入来源', trigger: 'blur' },
      { max: 64, message: '来源不能超过 64 个字符', trigger: 'blur' }
    ]
  }
  if (props.type === 'external') {
    rules.externalUrl = [
      { required: true, message: '请输入外链地址', trigger: 'blur' },
      { validator: validateHttpUrl, trigger: 'blur' }
    ]
    rules.platform = [
      { required: true, message: '请输入平台名称', trigger: 'blur' },
      { max: 32, message: '平台不能超过 32 个字符', trigger: 'blur' }
    ]
  } else {
    rules.statement = [
      { required: true, message: '请输入题面', trigger: 'blur' },
      { max: 100000, message: '题面不能超过 100000 个字符', trigger: 'blur' }
    ]
  }
  if (props.type === 'short_answer') {
    rules.referenceAnswer = [
      { required: true, message: '请输入参考答案', trigger: 'blur' },
      { max: 20000, message: '参考答案不能超过 20000 个字符', trigger: 'blur' }
    ]
  }
  return rules
})

function roundScore(value: number) {
  return Math.round(value * 10) / 10
}

const rubricTotal = computed(() =>
  roundScore(form.scoringRubric.reduce((sum, item) => sum + (Number(item.score) || 0), 0))
)

const detailRubricTotal = computed(() =>
  roundScore(
    (detail.value?.scoringRubric ?? []).reduce(
      (sum, item) => sum + (Number(item.score) || 0),
      0
    )
  )
)

const testDataTotalBytes = computed(() =>
  testDataDraft.value.reduce(
    (sum, item) => sum + utf8Bytes(item.inputData) + utf8Bytes(item.outputData),
    0
  )
)

function queryParams() {
  return {
    type: props.type,
    keyword: keyword.value.trim() || undefined,
    difficulty: difficultyFilter.value || undefined,
    status: statusFilter.value || undefined,
    objectiveType:
      props.type === 'objective' ? objectiveTypeFilter.value || undefined : undefined,
    page: page.value,
    pageSize: pageSize.value
  }
}

async function loadProblems() {
  loading.value = true
  try {
    const result = await fetchAdminProblems(queryParams())
    problems.value = result.items
    total.value = result.total
  } catch (error) {
    problems.value = []
    total.value = 0
    ElMessage.error(resolveApiErrorMessage(error, '题目列表加载失败'))
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  void loadProblems()
}

function handleResetFilters() {
  keyword.value = ''
  difficultyFilter.value = ''
  statusFilter.value = ''
  objectiveTypeFilter.value = ''
  handleSearch()
}

async function loadTestData(problemId: string) {
  testDataLoading.value = true
  try {
    const result = await fetchAdminProblemTestData(problemId, {
      page: testDataPage.value,
      pageSize: testDataPageSize.value
    })
    testDataItems.value = result.items
    testDataTotal.value = result.total
  } catch (error) {
    testDataItems.value = []
    testDataTotal.value = 0
    ElMessage.error(resolveApiErrorMessage(error, '测试数据加载失败'))
  } finally {
    testDataLoading.value = false
  }
}

async function openDetail(row: AdminProblemSummary) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  testDataItems.value = []
  testDataTotal.value = 0
  testDataPage.value = 1
  try {
    detail.value = await fetchAdminProblem(row.id)
    if (props.type === 'oj') {
      await loadTestData(row.id)
    }
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '题目详情加载失败'))
  } finally {
    detailLoading.value = false
  }
}

async function refreshDetailIfOpen(problemId: string) {
  if (!detailVisible.value || detail.value?.id !== problemId) {
    return
  }
  try {
    detail.value = await fetchAdminProblem(problemId)
    if (props.type === 'oj') {
      await loadTestData(problemId)
    }
  } catch {
    detailVisible.value = false
    detail.value = null
  }
}

async function openTestDataManager(row: AdminProblemSummary) {
  await openDetail(row)
  if (detail.value?.id === row.id) {
    await openTestDataDialog()
  }
}

function resetForm() {
  Object.assign(form, defaultForm())
}

function openCreateDialog() {
  editingId.value = null
  resetForm()
  formVisible.value = true
  void nextTick(() => formRef.value?.clearValidate())
}

async function openEditDialog(row: AdminProblemSummary) {
  editingId.value = row.id
  resetForm()
  formVisible.value = true
  formLoading.value = true
  try {
    const data = await fetchAdminProblem(row.id)
    editingId.value = data.id
    form.title = data.title
    form.statement = data.statement ?? ''
    form.difficulty = data.difficulty
    form.source = data.source
    form.tags = [...data.tags]

    if (props.type === 'oj') {
      form.inputFormat = data.inputFormat ?? ''
      form.outputFormat = data.outputFormat ?? ''
      form.timeLimitMs = data.timeLimitMs ?? undefined
      form.memoryLimitMb = data.memoryLimitMb ?? undefined
      form.samples = data.samples.length
        ? data.samples.map((sample) => ({
            inputData: sample.inputData ?? '',
            outputData: sample.outputData ?? '',
            explanation: sample.explanation ?? ''
          }))
        : [emptySample()]
    } else if (props.type === 'objective') {
      form.objectiveType = data.objectiveType ?? 'single'
      form.options = data.options.length
        ? data.options.map((option) => ({
            content: option.content,
            isCorrect: option.isCorrect
          }))
        : defaultOptions()
      form.explanation = data.explanation ?? ''
    } else if (props.type === 'short_answer') {
      form.referenceAnswer = data.referenceAnswer ?? ''
      form.scoringRubric = data.scoringRubric.length
        ? data.scoringRubric.map((item) => ({
            point: item.point,
            score: Number(item.score),
            description: item.description ?? ''
          }))
        : [{ point: '', score: 100, description: '' }]
    } else {
      form.platform = data.platform ?? ''
      form.externalUrl = data.externalUrl ?? ''
    }

    await nextTick()
    formRef.value?.clearValidate()
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '题目详情加载失败'))
    formVisible.value = false
  } finally {
    formLoading.value = false
  }
}

function optionKey(index: number) {
  return String.fromCharCode(65 + index)
}

function addSample() {
  if (form.samples.length >= 20) {
    ElMessage.warning('样例最多 20 组')
    return
  }
  form.samples.push(emptySample())
}

function removeSample(index: number) {
  form.samples.splice(index, 1)
  if (!form.samples.length) {
    form.samples.push(emptySample())
  }
}

function addOption() {
  if (form.options.length >= 10) {
    ElMessage.warning('选项最多 10 个')
    return
  }
  form.options.push({ content: '', isCorrect: false })
}

function removeOption(index: number) {
  if (form.options.length <= 2) {
    ElMessage.warning('客观题至少保留 2 个选项')
    return
  }
  form.options.splice(index, 1)
}

function handleObjectiveTypeChange() {
  if (form.objectiveType === 'multiple') {
    if (!form.options.some((option) => option.isCorrect) && form.options.length) {
      form.options[0].isCorrect = true
    }
    return
  }
  let found = false
  for (const option of form.options) {
    if (!option.isCorrect) {
      continue
    }
    if (found) {
      option.isCorrect = false
    } else {
      found = true
    }
  }
  if (!found && form.options.length) {
    form.options[0].isCorrect = true
  }
}

function handleCorrectToggle(index: number) {
  if (form.objectiveType === 'multiple') {
    return
  }
  if (!form.options[index]?.isCorrect) {
    return
  }
  form.options.forEach((option, itemIndex) => {
    option.isCorrect = itemIndex === index
  })
}

function addRubricItem() {
  if (form.scoringRubric.length >= 20) {
    ElMessage.warning('评分要点最多 20 项')
    return
  }
  form.scoringRubric.push({ point: '', score: undefined, description: '' })
}

function removeRubricItem(index: number) {
  if (form.scoringRubric.length <= 1) {
    ElMessage.warning('至少保留 1 项评分要点')
    return
  }
  form.scoringRubric.splice(index, 1)
}

function validateTypeSpecific(): string | null {
  if (props.type === 'oj') {
    if (form.samples.length > 20) {
      return '样例最多 20 组'
    }
    return null
  }

  if (props.type === 'objective') {
    if (form.options.length < 2 || form.options.length > 10) {
      return '客观题需配置 2–10 个选项'
    }
    if (form.options.some((option) => !option.content.trim())) {
      return '请填写所有选项内容'
    }
    const correctCount = form.options.filter((option) => option.isCorrect).length
    if (form.objectiveType === 'multiple') {
      return correctCount >= 1 ? null : '多选题至少需要 1 个正确选项'
    }
    return correctCount === 1 ? null : '判断/单选题必须且只能有 1 个正确选项'
  }

  if (props.type === 'short_answer') {
    if (!form.referenceAnswer.trim()) {
      return '请填写参考答案'
    }
    if (!form.scoringRubric.length || form.scoringRubric.length > 20) {
      return '评分要点必须为 1–20 项'
    }
    if (form.scoringRubric.some((item) => !item.point.trim())) {
      return '请填写所有评分要点'
    }
    if (
      form.scoringRubric.some(
        (item) => !Number.isFinite(Number(item.score)) || Number(item.score) < 0
      )
    ) {
      return '评分要点分数必须大于等于 0'
    }
    if (Math.abs(rubricTotal.value - 100) > 0.001) {
      return '评分要点分数之和必须等于 100'
    }
    return null
  }

  if (!form.platform.trim()) {
    return '请填写平台名称'
  }
  if (!form.externalUrl.trim()) {
    return '请填写外链地址'
  }
  try {
    const url = new URL(form.externalUrl.trim())
    if (url.protocol !== 'http:' && url.protocol !== 'https:') {
      throw new Error('invalid protocol')
    }
  } catch {
    return '请输入有效的 http 或 https 外链地址'
  }
  return null
}

function buildPayload(): AdminProblemCreatePayload {
  const common: AdminProblemCreatePayload = {
    type: props.type,
    title: form.title.trim(),
    difficulty: form.difficulty,
    source: form.source.trim(),
    tags: form.tags.map((tag) => tag.trim()).filter(Boolean)
  }

  if (props.type === 'external') {
    return {
      ...common,
      platform: form.platform.trim(),
      externalUrl: form.externalUrl.trim()
    }
  }

  const statement = form.statement.trim()

  if (props.type === 'oj') {
    return {
      ...common,
      statement,
      inputFormat: form.inputFormat.trim() || null,
      outputFormat: form.outputFormat.trim() || null,
      timeLimitMs: form.timeLimitMs ?? null,
      memoryLimitMb: form.memoryLimitMb ?? null,
      samples: form.samples
        .filter(
          (sample) =>
            sample.inputData !== '' ||
            sample.outputData !== '' ||
            sample.explanation.trim() !== ''
        )
        .map((sample, index) => ({
          inputData: sample.inputData,
          outputData: sample.outputData,
          explanation: sample.explanation.trim() || null,
          sortOrder: index
        }))
    }
  }

  if (props.type === 'objective') {
    return {
      ...common,
      statement,
      objectiveType: form.objectiveType,
      explanation: form.explanation.trim() || null,
      options: form.options.map((option, index) => ({
        optionKey: optionKey(index),
        content: option.content.trim(),
        isCorrect: option.isCorrect,
        sortOrder: index
      }))
    }
  }

  return {
    ...common,
    statement,
    referenceAnswer: form.referenceAnswer.trim(),
    maxScore: 100,
    scoringRubric: form.scoringRubric.map((item) => ({
      point: item.point.trim(),
      score: Number(item.score ?? 0),
      description: item.description.trim() || null
    }))
  }
}

function toUpdatePayload(payload: AdminProblemCreatePayload): AdminProblemUpdatePayload {
  const { type: _omitted, ...rest } = payload
  void _omitted
  return rest
}

async function submitForm() {
  const instance = formRef.value
  if (!instance) {
    return
  }
  const valid = await instance.validate().catch(() => false)
  if (!valid) {
    return
  }
  const typeError = validateTypeSpecific()
  if (typeError) {
    ElMessage.warning(typeError)
    return
  }

  formSubmitting.value = true
  try {
    const payload = buildPayload()
    if (editingId.value) {
      const updated = await updateAdminProblem(editingId.value, toUpdatePayload(payload))
      ElMessage.success(`已保存题目「${updated.title}」`)
      formVisible.value = false
      await loadProblems()
      await refreshDetailIfOpen(updated.id)
    } else {
      const created = await createAdminProblem(payload)
      ElMessage.success(`已创建题目「${created.title}」，当前为草稿`)
      formVisible.value = false
      page.value = 1
      await loadProblems()
    }
  } catch (error) {
    ElMessage.error(
      resolveApiErrorMessage(error, editingId.value ? '题目保存失败' : '题目创建失败')
    )
  } finally {
    formSubmitting.value = false
  }
}

async function toggleStatus(row: AdminProblemSummary) {
  const publishing = row.status !== 'published'
  try {
    await ElMessageBox.confirm(
      publishing
        ? `发布后「${row.title}」将立即出现在前台题库中。发布前请确认题型配置与测试数据完整。确认发布吗？`
        : `下架后「${row.title}」将不再出现在前台，历史提交与榜单记录保留。确认下架吗？`,
      publishing ? '发布题目' : '下架题目',
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
      await publishAdminProblem(row.id)
    } else {
      await offlineAdminProblem(row.id)
    }
    ElMessage.success(publishing ? '题目已发布' : '题目已下架')
    await loadProblems()
    await refreshDetailIfOpen(row.id)
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, publishing ? '发布失败' : '下架失败'))
  }
}

async function confirmDelete(row: AdminProblemSummary) {
  try {
    await ElMessageBox.confirm(
      `删除后「${row.title}」将从后台移除。存在阶段、提交、首次 AC、错题本、对战或外链完成记录时不能删除，只能下架。确认删除吗？`,
      '删除题目',
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
    await deleteAdminProblem(row.id)
    if (detail.value?.id === row.id) {
      detailVisible.value = false
      detail.value = null
    }
    if (problems.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    ElMessage.success('题目已删除')
    await loadProblems()
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '题目删除失败'))
  }
}

function preview(value: string | null | undefined, emptyLabel: string) {
  const text = value ?? ''
  if (!text) {
    return emptyLabel
  }
  return text.length > 80 ? `${text.slice(0, 80)}…` : text
}

function utf8Bytes(value: string) {
  return new TextEncoder().encode(value).length
}

async function openTestDataDialog() {
  if (!detail.value) {
    return
  }
  testDataDialogVisible.value = true
  testDataLoadingAll.value = true
  testDataDraft.value = []
  try {
    const all: TestDataView[] = []
    let currentPage = 1
    let loadedTotal = 0
    do {
      const result = await fetchAdminProblemTestData(detail.value.id, {
        page: currentPage,
        pageSize: 100
      })
      all.push(...result.items)
      loadedTotal = result.total
      currentPage += 1
    } while (all.length < loadedTotal && currentPage <= 5)
    testDataDraft.value = all.map((item) => ({
      inputData: item.inputData,
      outputData: item.outputData
    }))
    if (!testDataDraft.value.length) {
      testDataDraft.value.push({ inputData: '', outputData: '' })
    }
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '测试数据加载失败'))
    testDataDialogVisible.value = false
  } finally {
    testDataLoadingAll.value = false
  }
}

function handleTestDataPageChange() {
  if (!detail.value) {
    return
  }
  void loadTestData(detail.value.id)
}

function addTestDataRow() {
  if (testDataDraft.value.length >= 500) {
    ElMessage.warning('测试数据最多 500 组')
    return
  }
  testDataDraft.value.push({ inputData: '', outputData: '' })
}

function removeTestDataRow(index: number) {
  if (testDataDraft.value.length <= 1) {
    ElMessage.warning('至少保留 1 组测试数据')
    return
  }
  testDataDraft.value.splice(index, 1)
}

async function saveTestData() {
  if (!detail.value) {
    return
  }
  if (!testDataDraft.value.length || testDataDraft.value.length > 500) {
    ElMessage.warning('测试数据必须为 1–500 组')
    return
  }
  for (let index = 0; index < testDataDraft.value.length; index += 1) {
    const item = testDataDraft.value[index]
    if (utf8Bytes(item.inputData) > 1048576 || utf8Bytes(item.outputData) > 1048576) {
      ElMessage.warning(`第 ${index + 1} 组测试数据的输入或输出超过 1 MB`)
      return
    }
  }
  if (testDataTotalBytes.value > 10485760) {
    ElMessage.warning('测试数据总量超过 10 MB')
    return
  }

  testDataSaving.value = true
  try {
    await replaceAdminProblemTestData(
      detail.value.id,
      testDataDraft.value.map((item) => ({
        inputData: item.inputData,
        outputData: item.outputData
      }))
    )
    ElMessage.success(`已保存 ${testDataDraft.value.length} 组测试数据`)
    testDataDialogVisible.value = false
    testDataPage.value = 1
    await loadTestData(detail.value.id)
    await refreshDetailIfOpen(detail.value.id)
    await loadProblems()
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '测试数据保存失败'))
  } finally {
    testDataSaving.value = false
  }
}

onMounted(loadProblems)
</script>

<template>
  <AdminPage :title="meta.title" :description="meta.description">
    <template #actions>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">
        {{ meta.createLabel }}
      </el-button>
    </template>

    <AdminTableCard :title="meta.tableTitle" :description="meta.tableDescription">
      <template #toolbar>
        <AdminToolbar>
          <el-input
            v-model="keyword"
            class="admin-search"
            :placeholder="meta.searchPlaceholder"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="difficultyFilter"
            class="admin-filter"
            placeholder="全部难度"
            @change="handleSearch"
          >
            <el-option label="全部难度" value="" />
            <el-option label="入门" value="entry" />
            <el-option label="基础" value="basic" />
            <el-option label="进阶" value="advanced" />
          </el-select>
          <el-select
            v-if="props.type === 'objective'"
            v-model="objectiveTypeFilter"
            class="admin-filter"
            placeholder="全部题型"
            @change="handleSearch"
          >
            <el-option label="全部题型" value="" />
            <el-option label="判断题" value="judge" />
            <el-option label="单选题" value="single" />
            <el-option label="多选题" value="multiple" />
          </el-select>
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
          <template #meta>共 {{ total }} 道题目</template>
        </AdminToolbar>
      </template>

      <el-table v-loading="loading" :data="problems" row-key="id">
        <el-table-column label="题目" min-width="280">
          <template #default="{ row }">
            <div class="problem-title-cell">
              <strong>{{ row.title }}</strong>
              <div v-if="row.tags.length" class="problem-title-cell__tags">
                <el-tag
                  v-for="tag in row.tags"
                  :key="tag"
                  size="small"
                  type="info"
                  effect="plain"
                >
                  {{ tag }}
                </el-tag>
              </div>
              <small>ID {{ row.id }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="86" align="center">
          <template #default="{ row }">
            {{ problemDifficultyLabel(row.difficulty) }}
          </template>
        </el-table-column>
        <el-table-column
          v-if="props.type === 'objective'"
          label="客观题类型"
          width="110"
          align="center"
        >
          <template #default="{ row }">
            {{ objectiveTypeLabel(row.objectiveType) }}
          </template>
        </el-table-column>
        <el-table-column
          v-if="props.type === 'external'"
          prop="platform"
          label="平台"
          width="120"
        />
        <el-table-column label="来源" width="120">
          <template #default="{ row }">{{ row.source || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="96" align="center">
          <template #default="{ row }">
            <el-tag :type="contentStatusTagType(row.status)" effect="plain">
              {{ contentStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column
          label="操作"
          :width="props.type === 'oj' ? 410 : 330"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row)">
              详情
            </el-button>
            <el-button link type="primary" :icon="Edit" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button
              v-if="props.type === 'oj'"
              link
              type="primary"
              :icon="Document"
              @click="openTestDataManager(row)"
            >
              测试数据
            </el-button>
            <el-button
              link
              :type="row.status === 'published' ? 'warning' : 'success'"
              :icon="row.status === 'published' ? CircleClose : VideoPlay"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'published' ? '下架' : '发布' }}
            </el-button>
            <el-button link type="danger" :icon="Delete" @click="confirmDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <AdminEmptyState :description="meta.emptyText" />
        </template>
      </el-table>

      <template #footer>
        <AdminPagination
          v-model:page="page"
          v-model:page-size="pageSize"
          :total="total"
          @change="loadProblems"
        />
      </template>
    </AdminTableCard>

    <el-dialog
      v-model="formVisible"
      class="problem-form-dialog"
      :title="editingId ? `编辑${problemTypeLabel(props.type)}` : meta.createLabel"
      width="920px"
      top="5vh"
      destroy-on-close
    >
      <div v-loading="formLoading">
        <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
          <el-form-item label="题目名称" prop="title">
            <el-input
              v-model="form.title"
              maxlength="200"
              show-word-limit
              placeholder="请输入题目名称"
            />
          </el-form-item>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="难度" prop="difficulty">
                <el-select v-model="form.difficulty" placeholder="请选择难度">
                  <el-option label="入门" value="entry" />
                  <el-option label="基础" value="basic" />
                  <el-option label="进阶" value="advanced" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="来源" prop="source">
                <el-input v-model="form.source" maxlength="64" placeholder="例如：站内原创" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="专题标签">
            <el-select
              v-model="form.tags"
              multiple
              filterable
              allow-create
              default-first-option
              :reserve-keyword="false"
              placeholder="输入标签后按回车，例如：动态规划"
            />
          </el-form-item>

          <template v-if="props.type === 'external'">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="平台" prop="platform">
                  <el-input v-model="form.platform" maxlength="32" placeholder="例如：Codeforces" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="外链地址" prop="externalUrl">
                  <el-input
                    v-model="form.externalUrl"
                    maxlength="512"
                    placeholder="https://..."
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-alert
              type="info"
              :closable="false"
              show-icon
              title="外链题只保存标题、平台、链接与标签，不复制题面内容。"
            />
          </template>

          <template v-else>
            <el-form-item label="题面" prop="statement">
              <el-input
                v-model="form.statement"
                type="textarea"
                :rows="8"
                maxlength="100000"
                show-word-limit
                placeholder="支持 Markdown 题面"
              />
            </el-form-item>
          </template>

          <template v-if="props.type === 'oj'">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="时限（ms）">
                  <el-input-number
                    v-model="form.timeLimitMs"
                    :min="100"
                    :max="60000"
                    :step="100"
                    controls-position="right"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="内存（MB）">
                  <el-input-number
                    v-model="form.memoryLimitMb"
                    :min="16"
                    :max="2048"
                    :step="16"
                    controls-position="right"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="输入格式">
              <el-input
                v-model="form.inputFormat"
                type="textarea"
                :rows="2"
                maxlength="100000"
                placeholder="选填，描述输入数据格式"
              />
            </el-form-item>
            <el-form-item label="输出格式">
              <el-input
                v-model="form.outputFormat"
                type="textarea"
                :rows="2"
                maxlength="100000"
                placeholder="选填，描述输出数据格式"
              />
            </el-form-item>

            <div class="problem-editor">
              <div class="problem-editor__head">
                <h4>样例</h4>
                <el-button link type="primary" :icon="Plus" @click="addSample">
                  添加样例
                </el-button>
              </div>
              <div
                v-for="(sample, index) in form.samples"
                :key="index"
                class="problem-editor__card"
              >
                <div class="problem-editor__card-head">
                  <span>样例 {{ index + 1 }}</span>
                  <el-button link type="danger" :icon="Delete" @click="removeSample(index)">
                    移除
                  </el-button>
                </div>
                <el-row :gutter="12">
                  <el-col :span="12">
                    <el-form-item label="输入" label-width="50px">
                      <el-input v-model="sample.inputData" type="textarea" :rows="3" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="输出" label-width="50px">
                      <el-input v-model="sample.outputData" type="textarea" :rows="3" />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-form-item label="说明" label-width="50px">
                  <el-input v-model="sample.explanation" placeholder="选填" />
                </el-form-item>
              </div>
            </div>
          </template>

          <template v-if="props.type === 'objective'">
            <el-form-item label="客观题类型">
              <el-radio-group
                v-model="form.objectiveType"
                @change="handleObjectiveTypeChange"
              >
                <el-radio-button value="judge">判断题</el-radio-button>
                <el-radio-button value="single">单选题</el-radio-button>
                <el-radio-button value="multiple">多选题</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <div class="problem-editor">
              <div class="problem-editor__head">
                <h4>选项</h4>
                <el-button link type="primary" :icon="Plus" @click="addOption">
                  添加选项
                </el-button>
              </div>
              <div
                v-for="(option, index) in form.options"
                :key="index"
                class="option-row"
              >
                <span class="option-row__key">{{ optionKey(index) }}</span>
                <el-input v-model="option.content" placeholder="选项内容" />
                <el-checkbox
                  v-model="option.isCorrect"
                  @change="handleCorrectToggle(index)"
                >
                  正确答案
                </el-checkbox>
                <el-button
                  link
                  type="danger"
                  :icon="Delete"
                  :disabled="form.options.length <= 2"
                  @click="removeOption(index)"
                >
                  删除
                </el-button>
              </div>
            </div>

            <el-form-item label="解析">
              <el-input
                v-model="form.explanation"
                type="textarea"
                :rows="3"
                maxlength="100000"
                placeholder="选填，客观题解析"
              />
            </el-form-item>
          </template>

          <template v-if="props.type === 'short_answer'">
            <el-form-item label="参考答案" prop="referenceAnswer">
              <el-input
                v-model="form.referenceAnswer"
                type="textarea"
                :rows="6"
                maxlength="20000"
                show-word-limit
                placeholder="请输入用于 AI 判分的参考答案"
              />
            </el-form-item>
            <el-form-item label="满分">
              <el-tag effect="plain">100 分（固定）</el-tag>
            </el-form-item>

            <div class="problem-editor">
              <div class="problem-editor__head">
                <h4>评分要点</h4>
                <div class="problem-editor__head-actions">
                  <el-tag
                    :type="Math.abs(rubricTotal - 100) < 0.001 ? 'success' : 'warning'"
                    effect="plain"
                  >
                    合计 {{ rubricTotal }} / 100
                  </el-tag>
                  <el-button link type="primary" :icon="Plus" @click="addRubricItem">
                    添加要点
                  </el-button>
                </div>
              </div>
              <div
                v-for="(item, index) in form.scoringRubric"
                :key="index"
                class="rubric-row"
              >
                <el-input v-model="item.point" maxlength="200" placeholder="评分要点" />
                <el-input-number
                  v-model="item.score"
                  :min="0"
                  :max="100"
                  :precision="1"
                  :step="5"
                  controls-position="right"
                />
                <el-input
                  v-model="item.description"
                  maxlength="500"
                  placeholder="评分说明（可选）"
                />
                <el-button
                  link
                  type="danger"
                  :icon="Delete"
                  :disabled="form.scoringRubric.length <= 1"
                  @click="removeRubricItem(index)"
                >
                  删除
                </el-button>
              </div>
              <p class="problem-editor__hint">
                评分要点分数之和必须等于 100；保存后由 AI 按参考答案与评分要点判分。
              </p>
            </div>
          </template>

          <el-alert
            v-if="!editingId"
            type="info"
            :closable="false"
            show-icon
            title="新增题目默认为草稿，需要发布后才会出现在前台题库。"
          />
        </el-form>
      </div>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formSubmitting" @click="submitForm">
          {{ editingId ? '保存修改' : '创建题目' }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="detailVisible"
      class="admin-detail-drawer"
      title="题目详情"
      size="760px"
    >
      <div v-loading="detailLoading" class="problem-detail">
        <template v-if="detail">
          <div class="problem-detail__head">
            <div class="problem-detail__title">
              <h2>{{ detail.title }}</h2>
              <el-tag effect="plain">{{ problemTypeLabel(detail.type) }}</el-tag>
              <el-tag v-if="detail.objectiveType" type="info" effect="plain">
                {{ objectiveTypeLabel(detail.objectiveType) }}
              </el-tag>
              <el-tag :type="contentStatusTagType(detail.status)" effect="plain">
                {{ contentStatusLabel(detail.status) }}
              </el-tag>
              <el-tag type="info" effect="plain">
                {{ problemDifficultyLabel(detail.difficulty) }}
              </el-tag>
            </div>
            <p class="problem-detail__source">
              来源：{{ detail.source }}
              <span v-if="detail.platform"> · 平台：{{ detail.platform }}</span>
            </p>
          </div>

          <dl class="problem-detail__meta">
            <div>
              <dt>创建时间</dt>
              <dd>{{ formatDateTime(detail.createdAt) }}</dd>
            </div>
            <div>
              <dt>更新时间</dt>
              <dd>{{ formatDateTime(detail.updatedAt) }}</dd>
            </div>
            <template v-if="detail.type === 'oj'">
              <div>
                <dt>时限</dt>
                <dd>{{ detail.timeLimitMs ? `${detail.timeLimitMs} ms` : '未配置' }}</dd>
              </div>
              <div>
                <dt>内存限制</dt>
                <dd>{{ detail.memoryLimitMb ? `${detail.memoryLimitMb} MB` : '未配置' }}</dd>
              </div>
              <div>
                <dt>样例 / 测试数据</dt>
                <dd>{{ detail.samples.length }} / {{ detail.testDataCount }} 组</dd>
              </div>
            </template>
            <template v-if="detail.type === 'objective'">
              <div>
                <dt>选项数量</dt>
                <dd>{{ detail.options.length }} 个</dd>
              </div>
            </template>
            <template v-if="detail.type === 'short_answer'">
              <div>
                <dt>满分</dt>
                <dd>{{ detail.maxScore }} 分</dd>
              </div>
              <div>
                <dt>评分要点</dt>
                <dd>{{ detail.scoringRubric.length }} 项</dd>
              </div>
            </template>
            <template v-if="detail.type === 'external'">
              <div>
                <dt>平台</dt>
                <dd>{{ detail.platform || '—' }}</dd>
              </div>
            </template>
          </dl>

          <section v-if="detail.type !== 'external'" class="problem-detail__section">
            <h3>题面</h3>
            <pre class="problem-detail__pre">{{ detail.statement || '未填写题面' }}</pre>
          </section>
          <el-alert
            v-else
            type="info"
            :closable="false"
            show-icon
            title="外链题只保存标题、平台、链接与标签，不复制题面内容。"
          />

          <template v-if="detail.type === 'external'">
            <section class="problem-detail__section">
              <h3>外链地址</h3>
              <el-link
                v-if="detail.externalUrl"
                type="primary"
                :href="detail.externalUrl"
                target="_blank"
                rel="noopener noreferrer"
              >
                {{ detail.externalUrl }}
              </el-link>
              <span v-else>未配置</span>
            </section>
          </template>

          <template v-if="detail.type === 'oj'">
            <section class="problem-detail__section">
              <h3>输入 / 输出格式</h3>
              <div class="problem-detail__formats">
                <div>
                  <h4>输入格式</h4>
                  <pre class="problem-detail__pre">{{ detail.inputFormat || '未填写' }}</pre>
                </div>
                <div>
                  <h4>输出格式</h4>
                  <pre class="problem-detail__pre">{{ detail.outputFormat || '未填写' }}</pre>
                </div>
              </div>
            </section>

            <section class="problem-detail__section">
              <h3>样例</h3>
              <el-table :data="detail.samples" size="small" row-key="id">
                <el-table-column prop="sortOrder" label="#" width="50" align="center" />
                <el-table-column label="输入" min-width="160">
                  <template #default="{ row }">
                    <pre class="problem-detail__code">{{ row.inputData || '（空输入）' }}</pre>
                  </template>
                </el-table-column>
                <el-table-column label="输出" min-width="160">
                  <template #default="{ row }">
                    <pre class="problem-detail__code">{{ row.outputData || '（空输出）' }}</pre>
                  </template>
                </el-table-column>
                <el-table-column prop="explanation" label="说明" min-width="120" />
                <template #empty>
                  <AdminEmptyState description="尚未配置样例" />
                </template>
              </el-table>
            </section>

            <section class="problem-detail__section">
              <div class="problem-detail__section-head">
                <h3>测试数据</h3>
                <div class="problem-detail__section-actions">
                  <el-button link type="primary" :icon="Refresh" @click="loadTestData(detail.id)">
                    刷新
                  </el-button>
                  <el-button
                    link
                    type="primary"
                    :icon="Document"
                    @click="openTestDataDialog"
                  >
                    全量替换
                  </el-button>
                </div>
              </div>
              <el-table
                v-loading="testDataLoading"
                :data="testDataItems"
                size="small"
                row-key="id"
              >
                <el-table-column prop="sortOrder" label="#" width="50" align="center" />
                <el-table-column label="输入" min-width="220">
                  <template #default="{ row }">
                    <div class="problem-detail__code">
                      {{ preview(row.inputData, '（空输入）') }}
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="输出" min-width="220">
                  <template #default="{ row }">
                    <div class="problem-detail__code">
                      {{ preview(row.outputData, '（空输出）') }}
                    </div>
                  </template>
                </el-table-column>
                <template #empty>
                  <AdminEmptyState description="尚未配置测试数据，OJ 题发布前至少需要 1 组" />
                </template>
              </el-table>
              <AdminPagination
                v-model:page="testDataPage"
                v-model:page-size="testDataPageSize"
                :total="testDataTotal"
                :page-sizes="[10, 20, 50, 100]"
                @change="handleTestDataPageChange"
              />
            </section>
          </template>

          <template v-if="detail.type === 'objective'">
            <section class="problem-detail__section">
              <h3>选项与正确答案</h3>
              <el-table :data="detail.options" size="small" row-key="id">
                <el-table-column prop="optionKey" label="选项" width="70" align="center" />
                <el-table-column prop="content" label="内容" min-width="240" />
                <el-table-column label="正确答案" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag v-if="row.isCorrect" type="success" effect="plain">正确</el-tag>
                    <span v-else>—</span>
                  </template>
                </el-table-column>
                <template #empty>
                  <AdminEmptyState description="尚未配置选项" />
                </template>
              </el-table>
            </section>
            <section v-if="detail.explanation" class="problem-detail__section">
              <h3>解析</h3>
              <pre class="problem-detail__pre">{{ detail.explanation }}</pre>
            </section>
          </template>

          <template v-if="detail.type === 'short_answer'">
            <section class="problem-detail__section">
              <h3>参考答案</h3>
              <pre class="problem-detail__pre">{{ detail.referenceAnswer || '未配置' }}</pre>
            </section>
            <section class="problem-detail__section">
              <div class="problem-detail__section-head">
                <h3>评分要点</h3>
                <el-tag effect="plain">合计 {{ detailRubricTotal }} / 100</el-tag>
              </div>
              <el-table :data="detail.scoringRubric" size="small">
                <el-table-column prop="point" label="评分要点" min-width="220" />
                <el-table-column prop="score" label="分数" width="80" align="center" />
                <el-table-column prop="description" label="评分说明" min-width="180" />
                <template #empty>
                  <AdminEmptyState description="尚未配置评分要点" />
                </template>
              </el-table>
            </section>
          </template>

          <section class="problem-detail__section">
            <div class="problem-detail__section-head">
              <h3>引用统计</h3>
              <el-tag :type="detail.canDelete ? 'success' : 'warning'" effect="plain">
                {{ detail.canDelete ? '无引用，可删除' : '存在引用，只能下架' }}
              </el-tag>
            </div>
            <dl class="problem-detail__usage">
              <div>
                <dt>阶段引用</dt>
                <dd>{{ detail.usage.stageCount }}</dd>
              </div>
              <div>
                <dt>历史提交</dt>
                <dd>{{ detail.usage.submissionCount }}</dd>
              </div>
              <div>
                <dt>首次 AC</dt>
                <dd>{{ detail.usage.firstAcCount }}</dd>
              </div>
              <div>
                <dt>错题本</dt>
                <dd>{{ detail.usage.wrongBookCount }}</dd>
              </div>
              <div>
                <dt>对战引用</dt>
                <dd>{{ detail.usage.battleCount }}</dd>
              </div>
              <div>
                <dt>外链完成</dt>
                <dd>{{ detail.usage.externalCompletionCount }}</dd>
              </div>
            </dl>
          </section>
        </template>
      </div>
    </el-drawer>

    <el-dialog
      v-model="testDataDialogVisible"
      class="problem-form-dialog"
      title="全量替换测试数据"
      width="980px"
      top="5vh"
      destroy-on-close
    >
      <div v-loading="testDataLoadingAll" class="test-data-editor">
        <el-alert
          type="warning"
          :closable="false"
          show-icon
          title="保存后将物理删除旧测试数据并重建，不保留历史版本；数组顺序即测试点顺序。"
        />
        <div class="test-data-editor__summary">
          <span>共 {{ testDataDraft.length }} 组</span>
          <span>总大小约 {{ formatBytes(testDataTotalBytes) }}</span>
          <el-button link type="primary" :icon="Plus" @click="addTestDataRow">
            添加一组
          </el-button>
        </div>
        <div
          v-for="(item, index) in testDataDraft"
          :key="index"
          class="test-data-row"
        >
          <div class="test-data-row__head">
            <span>测试点 {{ index + 1 }}</span>
            <el-button
              link
              type="danger"
              :icon="Delete"
              :disabled="testDataDraft.length <= 1"
              @click="removeTestDataRow(index)"
            >
              删除
            </el-button>
          </div>
          <el-row :gutter="12">
            <el-col :span="12">
              <div class="test-data-field">
                <label>输入</label>
                <el-input
                  v-model="item.inputData"
                  type="textarea"
                  :autosize="{ minRows: 2, maxRows: 8 }"
                />
              </div>
            </el-col>
            <el-col :span="12">
              <div class="test-data-field">
                <label>输出</label>
                <el-input
                  v-model="item.outputData"
                  type="textarea"
                  :autosize="{ minRows: 2, maxRows: 8 }"
                />
              </div>
            </el-col>
          </el-row>
        </div>
      </div>
      <template #footer>
        <el-button @click="testDataDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="testDataSaving" @click="saveTestData">
          保存全部测试数据
        </el-button>
      </template>
    </el-dialog>
  </AdminPage>
</template>

<style scoped>
:global(.problem-form-dialog) {
  display: flex;
  flex-direction: column;
  max-height: 90vh;
}

:global(.problem-form-dialog .el-dialog__body) {
  flex: 1;
  overflow-y: auto;
  padding-right: 14px;
}

.problem-title-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.problem-title-cell strong {
  color: var(--tz-color-text);
  font-size: 14px;
  font-weight: 600;
}

.problem-title-cell__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.problem-title-cell small {
  color: var(--tz-color-text-muted);
  font-size: 11px;
}

.problem-editor {
  margin: 4px 0 20px;
  padding: 14px;
  border: 1px solid var(--tz-color-border);
  border-radius: 12px;
  background: var(--tz-color-surface-muted);
}

.problem-editor__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.problem-editor__head h4 {
  margin: 0;
  color: var(--tz-color-text);
  font-size: 14px;
}

.problem-editor__head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.problem-editor__card {
  padding: 12px;
  margin-bottom: 12px;
  border: 1px solid var(--tz-color-border);
  border-radius: 10px;
  background: var(--tz-color-surface);
}

.problem-editor__card:last-child {
  margin-bottom: 0;
}

.problem-editor__card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

.problem-editor__hint {
  margin: 8px 0 0;
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

.option-row {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) 110px 70px;
  gap: 10px;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed var(--tz-color-border);
}

.option-row:last-child {
  border-bottom: none;
}

.option-row__key {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: var(--tz-color-text);
  font-weight: 600;
  border: 1px solid var(--tz-color-border);
  border-radius: 8px;
  background: var(--tz-color-surface);
}

.rubric-row {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) 120px minmax(0, 1fr) 70px;
  gap: 10px;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed var(--tz-color-border);
}

.rubric-row:last-child {
  border-bottom: none;
}

.problem-detail__head {
  margin-bottom: 16px;
}

.problem-detail__title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.problem-detail__title h2 {
  margin: 0;
  color: var(--tz-color-text);
  font-size: 20px;
}

.problem-detail__source {
  margin: 0;
  color: var(--tz-color-text-muted);
  font-size: 13px;
}

.problem-detail__meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 0 0 22px;
}

.problem-detail__meta > div {
  padding: 12px 14px;
  border: 1px solid var(--tz-color-border);
  border-radius: 10px;
  background: var(--tz-color-surface);
}

.problem-detail__meta dt {
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

.problem-detail__meta dd {
  margin: 4px 0 0;
  color: var(--tz-color-text);
  font-weight: 600;
}

.problem-detail__section {
  margin-bottom: 24px;
}

.problem-detail__section h3 {
  margin: 0 0 10px;
  color: var(--tz-color-text);
  font-size: 15px;
}

.problem-detail__section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.problem-detail__section-head h3 {
  margin: 0;
}

.problem-detail__section-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.problem-detail__pre {
  max-height: 260px;
  margin: 0;
  padding: 12px 14px;
  overflow: auto;
  color: var(--tz-color-text);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  border: 1px solid var(--tz-color-border);
  border-radius: 10px;
  background: var(--tz-color-surface-muted);
}

.problem-detail__code {
  max-height: 120px;
  margin: 0;
  overflow: auto;
  color: var(--tz-color-text);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.problem-detail__formats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.problem-detail__formats h4 {
  margin: 0 0 6px;
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

.problem-detail__usage {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 0;
}

.problem-detail__usage > div {
  padding: 12px 14px;
  border: 1px solid var(--tz-color-border);
  border-radius: 10px;
  background: var(--tz-color-surface);
}

.problem-detail__usage dt {
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

.problem-detail__usage dd {
  margin: 4px 0 0;
  color: var(--tz-color-text);
  font-size: 18px;
  font-weight: 600;
}

.test-data-editor__summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 18px;
  margin: 14px 0;
  color: var(--tz-color-text-muted);
  font-size: 13px;
}

.test-data-row {
  padding: 12px;
  margin-bottom: 12px;
  border: 1px solid var(--tz-color-border);
  border-radius: 10px;
  background: var(--tz-color-surface);
}

.test-data-row__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

.test-data-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.test-data-field label {
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

@media (max-width: 900px) {
  .problem-detail__meta,
  .problem-detail__usage,
  .problem-detail__formats {
    grid-template-columns: 1fr;
  }

  .option-row,
  .rubric-row {
    grid-template-columns: 28px minmax(0, 1fr);
  }
}
</style>
