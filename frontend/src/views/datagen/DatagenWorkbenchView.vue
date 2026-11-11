<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowRight,
  CircleCheck,
  Delete,
  Key,
  Lock,
  MagicStick,
  Plus,
  Refresh,
  WarningFilled
} from '@element-plus/icons-vue'

import {
  createDataGenTask,
  deleteLlmCredential,
  fetchDataGenOptions,
  fetchDataGenTasks,
  fetchLlmCredential,
  fetchLlmProviders,
  saveLlmCredential,
  type DataGenMode,
  type DataGenOptions,
  type DataGenStdLanguage,
  type DataGenTaskListItem,
  type DataRangeType,
  type LlmCredential,
  type LlmProvider
} from '@/api/datagen'
import CodeEditor from '@/components/problem/CodeEditor.vue'
import { resolveApiErrorMessage } from '@/utils/apiError'
import {
  datagenStatusLabel,
  generationModeLabel,
  statusTagType
} from '@/utils/datagen'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

const CPP_TEMPLATE = `#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    if (!(cin >> n)) return 0;
    cout << n << '\\n';
    return 0;
}
`

const PYTHON_TEMPLATE = `import sys


def main() -> None:
    data = sys.stdin.read().split()
    if not data:
        return
    n = int(data[0])
    print(n)


if __name__ == "__main__":
    main()
`

const MODE_META: Record<
  DataGenMode,
  { title: string; short: string; description: string; tag: string }
> = {
  random: {
    title: '随机生成',
    short: 'Random',
    description: '按数据范围逐组随机取值，适合批量生成基础数据。',
    tag: '无需凭证'
  },
  llm_generator: {
    title: 'LLM 构造器',
    short: 'Generator',
    description: '由 LLM 编写 Python 构造器，在沙箱内逐组生成输入。',
    tag: '需要凭证'
  },
  llm_direct_data: {
    title: 'LLM 直接生成',
    short: 'Direct',
    description: 'LLM 直接返回全部测试输入，逐组校验后运行标准程序。',
    tag: '需要凭证'
  }
}

interface RangeForm {
  key: number
  name: string
  type: DataRangeType
  min: string
  max: string
}

const loading = ref(true)
const loadError = ref('')
const options = ref<DataGenOptions | null>(null)
const recentTasks = ref<DataGenTaskListItem[]>([])
const recentLoading = ref(false)

const mode = ref<DataGenMode>('random')
const form = reactive({
  title: '',
  statement: '',
  inputFormat: '',
  outputFormat: '',
  stdLanguage: 'cpp17' as DataGenStdLanguage,
  stdCode: CPP_TEMPLATE,
  groupCount: 10,
  timeLimitMs: 1000,
  memoryLimitMb: 256,
  specialRequirements: ''
})
const ranges = ref<RangeForm[]>([{ key: 1, name: 'n', type: 'int', min: '1', max: '10' }])
let rangeSequence = 2
let codeTouched = false

const submitting = ref(false)

const credential = computed<LlmCredential | null>(
  () => options.value?.llmCredential ?? null
)
const credentialUsable = computed(
  () => credential.value?.configured === true && credential.value.status === 'valid'
)
const hasActiveTask = computed(() => options.value?.hasActiveTask === true)
const selectedModeRequiresCredential = computed(() => {
  const option = options.value?.modes.find((item) => item.code === mode.value)
  return option?.requiresCredential ?? false
})
const credentialBlocksSubmit = computed(
  () => selectedModeRequiresCredential.value && !credentialUsable.value
)
const llmBlockedByActiveTask = computed(
  () => hasActiveTask.value && selectedModeRequiresCredential.value
)
const submitDisabled = computed(
  () => submitting.value || credentialBlocksSubmit.value || llmBlockedByActiveTask.value
)

function handleStdCodeChange(value: string) {
  form.stdCode = value
  codeTouched = true
}

function handleLanguageChange(language: DataGenStdLanguage) {
  if (!codeTouched || form.stdCode.trim() === '') {
    form.stdCode = language === 'python310' ? PYTHON_TEMPLATE : CPP_TEMPLATE
    codeTouched = false
  }
}

function addRange() {
  if (ranges.value.length >= 20) {
    ElMessage.warning('数据范围最多配置 20 个变量')
    return
  }
  ranges.value.push({
    key: rangeSequence++,
    name: `a${ranges.value.length + 1}`,
    type: 'int',
    min: '1',
    max: '10'
  })
}

function removeRange(key: number) {
  if (ranges.value.length <= 1) {
    ElMessage.warning('至少保留一个数据范围变量')
    return
  }
  ranges.value = ranges.value.filter((range) => range.key !== key)
}

async function loadPage() {
  loading.value = true
  loadError.value = ''
  try {
    const [result] = await Promise.all([fetchDataGenOptions(), loadRecentTasks()])
    options.value = result
    if (!result.modes.some((item) => item.code === mode.value)) {
      mode.value = result.modes[0]?.code ?? 'random'
    }
  } catch (error) {
    loadError.value = resolveApiErrorMessage(error, '造数据工作台加载失败')
  } finally {
    loading.value = false
  }
}

async function loadRecentTasks() {
  recentLoading.value = true
  try {
    const result = await fetchDataGenTasks({ page: 1, pageSize: 4 })
    recentTasks.value = result.items
  } catch {
    recentTasks.value = []
  } finally {
    recentLoading.value = false
  }
}

function validateForm(): string | null {
  const title = form.title.trim()
  if (!title) {
    return '请填写任务名称'
  }
  if (title.length > 200) {
    return '任务名称不能超过 200 字'
  }
  if (!form.statement.trim()) {
    return '请填写题面'
  }
  if (!form.inputFormat.trim()) {
    return '请填写输入格式'
  }
  if (!form.outputFormat.trim()) {
    return '请填写输出格式'
  }
  if (!form.stdCode.trim()) {
    return '请填写标准程序'
  }
  if (new TextEncoder().encode(form.stdCode).length > 65_536) {
    return '标准程序不能超过 64 KB'
  }
  if (ranges.value.length < 1 || ranges.value.length > 20) {
    return '数据范围必须包含 1–20 个变量'
  }

  const names = new Set<string>()
  for (const range of ranges.value) {
    const name = range.name.trim()
    if (!/^[A-Za-z_][A-Za-z0-9_]{0,31}$/.test(name)) {
      return '变量名需以字母或下划线开头，只能包含字母、数字与下划线，最长 32 位'
    }
    if (names.has(name)) {
      return `变量名 ${name} 重复`
    }
    names.add(name)
    const min = range.min.trim()
    const max = range.max.trim()
    const integer = range.type === 'int' || range.type === 'long long'
    const pattern = integer ? /^-?\d+$/ : /^-?\d+(\.\d+)?$/
    if (!pattern.test(min) || !pattern.test(max)) {
      return `变量 ${name} 的最小值或最大值格式不正确`
    }
    if (integer) {
      if (BigInt(min) > BigInt(max)) {
        return `变量 ${name} 的最小值不能大于最大值`
      }
    } else if (Number(min) > Number(max)) {
      return `变量 ${name} 的最小值不能大于最大值`
    }
  }

  const limits = options.value?.limits
  if (limits) {
    if (form.groupCount < limits.groupCount.min || form.groupCount > limits.groupCount.max) {
      return `数据组数需在 ${limits.groupCount.min}–${limits.groupCount.max} 之间`
    }
    if (form.timeLimitMs < limits.timeLimitMs.min || form.timeLimitMs > limits.timeLimitMs.max) {
      return `时间限制需在 ${limits.timeLimitMs.min}–${limits.timeLimitMs.max} ms 之间`
    }
    if (
      form.memoryLimitMb < limits.memoryLimitMb.min ||
      form.memoryLimitMb > limits.memoryLimitMb.max
    ) {
      return `内存限制需在 ${limits.memoryLimitMb.min}–${limits.memoryLimitMb.max} MB 之间`
    }
  }
  if (mode.value !== 'random' && form.specialRequirements.length > 2000) {
    return '特殊构造要求不能超过 2000 字'
  }
  return null
}

async function handleSubmit() {
  if (submitDisabled.value || submitting.value) {
    return
  }
  const error = validateForm()
  if (error) {
    ElMessage.warning(error)
    return
  }

  submitting.value = true
  try {
    const task = await createDataGenTask({
      title: form.title.trim(),
      statement: form.statement,
      inputFormat: form.inputFormat,
      outputFormat: form.outputFormat,
      stdCode: form.stdCode,
      stdLanguage: form.stdLanguage,
      dataRange: ranges.value.map((range) => ({
        name: range.name.trim(),
        type: range.type,
        min: range.min.trim(),
        max: range.max.trim()
      })),
      groupCount: form.groupCount,
      generationMode: mode.value,
      specialRequirements:
        mode.value === 'random' ? undefined : form.specialRequirements.trim() || undefined,
      timeLimitMs: form.timeLimitMs,
      memoryLimitMb: form.memoryLimitMb
    })
    ElMessage.success('任务已创建，正在后台生成测试数据')
    await router.push(`/datagen/tasks/${task.id}`)
  } catch (submitError) {
    ElMessage.error(resolveApiErrorMessage(submitError, '任务创建失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

/* ---------- LLM 凭证 ---------- */
const credentialDialogVisible = ref(false)
const providers = ref<LlmProvider[]>([])
const providersLoading = ref(false)
const credentialSaving = ref(false)
const credentialDeleting = ref(false)
const credentialError = ref('')
const credentialForm = reactive({
  provider: 'deepseek',
  model: '',
  apiKey: ''
})

const activeProvider = computed(() =>
  providers.value.find((item) => item.provider === credentialForm.provider)
)

watch(
  () => credentialForm.provider,
  () => {
    const provider = activeProvider.value
    if (!provider) {
      return
    }
    if (!provider.models.includes(credentialForm.model)) {
      credentialForm.model = provider.defaultModel
    }
  }
)

async function openCredentialDialog() {
  credentialDialogVisible.value = true
  credentialError.value = ''
  credentialForm.apiKey = ''
  if (!providers.value.length) {
    providersLoading.value = true
    try {
      providers.value = await fetchLlmProviders()
    } catch (error) {
      credentialError.value = resolveApiErrorMessage(error, 'LLM 服务商加载失败')
    } finally {
      providersLoading.value = false
    }
  }
  const current = credential.value
  if (current?.configured && current.provider) {
    credentialForm.provider = current.provider
    credentialForm.model = current.model ?? ''
  } else if (providers.value.length) {
    credentialForm.provider = providers.value[0].provider
    credentialForm.model = providers.value[0].defaultModel
  }
  if (!credentialForm.model && activeProvider.value) {
    credentialForm.model = activeProvider.value.defaultModel
  }
}

async function handleSaveCredential() {
  if (credentialSaving.value) {
    return
  }
  const apiKey = credentialForm.apiKey.trim()
  if (apiKey.length < 8) {
    ElMessage.warning('请填写有效的 API Key（至少 8 位）')
    return
  }
  if (!credentialForm.model) {
    ElMessage.warning('请选择模型')
    return
  }
  credentialSaving.value = true
  try {
    const view = await saveLlmCredential({
      provider: credentialForm.provider,
      apiKey,
      model: credentialForm.model
    })
    if (options.value) {
      options.value.llmCredential = view
    }
    credentialForm.apiKey = ''
    credentialDialogVisible.value = false
    ElMessage.success('LLM 凭证已验证并保存')
  } catch (error) {
    credentialError.value = resolveApiErrorMessage(error, '凭证校验失败，请检查 API Key 与模型')
  } finally {
    credentialSaving.value = false
  }
}

async function handleDeleteCredential() {
  try {
    await ElMessageBox.confirm(
      '删除后需要重新配置凭证才能使用 LLM 造数据模式。',
      '删除 LLM 凭证',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  credentialDeleting.value = true
  credentialError.value = ''
  try {
    await deleteLlmCredential()
    const view = await fetchLlmCredential()
    if (options.value) {
      options.value.llmCredential = view
    }
    credentialDialogVisible.value = false
    ElMessage.success('凭证已删除')
  } catch (error) {
    credentialError.value = resolveApiErrorMessage(error, '凭证删除失败')
  } finally {
    credentialDeleting.value = false
  }
}

async function refreshCredential() {
  try {
    const view = await fetchLlmCredential()
    if (options.value) {
      options.value.llmCredential = view
    }
  } catch {
    // 凭证状态刷新失败不阻塞工作台使用，用户可重新打开配置弹窗。
  }
}

function displayTime(value: string | null): string {
  return value ? formatDateTime(value) : '—'
}

onMounted(loadPage)
</script>

<template>
  <div class="datagen-page">
    <section class="datagen-hero">
      <div class="datagen-hero__copy">
        <span class="datagen-eyebrow">M08 · AI DATA WORKBENCH</span>
        <h1>让题目，先有数据。</h1>
        <p>
          随机生成、LLM 构造器与 LLM 直接生成三种模式，统一在沙箱中运行标准程序，
          产出可直接用于评测的输入输出文件。产出文件保留 7 天。
        </p>
        <div class="datagen-hero__actions">
          <RouterLink class="datagen-hero__link" to="/datagen/tasks">
            查看我的任务
            <el-icon><ArrowRight /></el-icon>
          </RouterLink>
          <span class="datagen-hero__hint">同一时间仅允许一个 LLM 造数据任务</span>
        </div>
      </div>
      <div class="datagen-hero__stats">
        <div class="datagen-stat">
          <span>生成模式</span>
          <strong>3</strong>
          <small>随机 / 构造器 / 直出</small>
        </div>
        <div class="datagen-stat">
          <span>单任务组数</span>
          <strong>50</strong>
          <small>最多 50 组测试数据</small>
        </div>
        <div class="datagen-stat">
          <span>文件保留</span>
          <strong>7d</strong>
          <small>过期自动清理</small>
        </div>
      </div>
    </section>

    <el-alert
      v-if="loadError"
      type="error"
      :closable="false"
      show-icon
      :title="loadError"
    >
      <template #default>
        <el-button size="small" @click="loadPage">重新加载</el-button>
      </template>
    </el-alert>

    <div v-loading="loading" class="datagen-layout">
      <form class="datagen-form tz-card" @submit.prevent="handleSubmit">
        <section class="datagen-section">
          <header class="datagen-section__head">
            <span class="datagen-step">01</span>
            <div>
              <h2>选择生成方式</h2>
              <p>随机模式无需凭证；LLM 模式使用你在个人侧配置的 API Key。</p>
            </div>
          </header>

          <div class="datagen-modes">
            <button
              v-for="option in options?.modes ?? []"
              :key="option.code"
              type="button"
              class="datagen-mode"
              :class="{ 'is-active': mode === option.code }"
              @click="mode = option.code"
            >
              <span class="datagen-mode__short">{{ MODE_META[option.code].short }}</span>
              <strong>{{ MODE_META[option.code].title }}</strong>
              <small>{{ MODE_META[option.code].description }}</small>
              <span class="datagen-mode__tag">{{ MODE_META[option.code].tag }}</span>
            </button>
          </div>

          <el-alert
            v-if="selectedModeRequiresCredential && !credentialUsable"
            type="warning"
            :closable="false"
            show-icon
            title="使用 LLM 模式前需要配置有效的 API Key"
          >
            <template #default>
              <el-button size="small" type="primary" plain @click="openCredentialDialog">
                立即配置
              </el-button>
            </template>
          </el-alert>
        </section>

        <section class="datagen-section">
          <header class="datagen-section__head">
            <span class="datagen-step">02</span>
            <div>
              <h2>题目信息</h2>
              <p>题面、输入输出格式会作为 LLM 生成数据时的上下文。</p>
            </div>
          </header>

          <el-form label-position="top" class="datagen-form-grid">
            <el-form-item label="任务名称" class="datagen-span-2" required>
              <el-input
                v-model="form.title"
                maxlength="200"
                show-word-limit
                placeholder="例如：区间求和 · 随机数据"
              />
            </el-form-item>
            <el-form-item label="题面" class="datagen-span-2" required>
              <el-input
                v-model="form.statement"
                type="textarea"
                :rows="6"
                maxlength="100000"
                placeholder="粘贴完整题面，包含背景、约束与样例说明。"
              />
            </el-form-item>
            <el-form-item label="输入格式" required>
              <el-input
                v-model="form.inputFormat"
                type="textarea"
                :rows="4"
                placeholder="例如：第一行一个整数 n，第二行 n 个整数。"
              />
            </el-form-item>
            <el-form-item label="输出格式" required>
              <el-input
                v-model="form.outputFormat"
                type="textarea"
                :rows="4"
                placeholder="例如：输出一个整数表示答案。"
              />
            </el-form-item>
          </el-form>
        </section>

        <section class="datagen-section">
          <header class="datagen-section__head">
            <span class="datagen-step">03</span>
            <div>
              <h2>数据范围</h2>
              <p>按变量声明取值范围，生成结果会逐组校验前若干个数值。</p>
            </div>
            <el-button :icon="Plus" plain size="small" @click="addRange">添加变量</el-button>
          </header>

          <div class="datagen-ranges">
            <div v-for="(range, index) in ranges" :key="range.key" class="datagen-range">
              <span class="datagen-range__index">{{ String(index + 1).padStart(2, '0') }}</span>
              <el-input v-model="range.name" placeholder="变量名" maxlength="32" />
              <el-select v-model="range.type" class="datagen-range__type">
                <el-option
                  v-for="type in options?.dataRangeTypes ?? []"
                  :key="type"
                  :label="type"
                  :value="type"
                />
              </el-select>
              <el-input v-model="range.min" placeholder="最小值" />
              <span class="datagen-range__dash">—</span>
              <el-input v-model="range.max" placeholder="最大值" />
              <el-button
                circle
                text
                type="danger"
                :icon="Delete"
                :disabled="ranges.length <= 1"
                aria-label="删除变量"
                @click="removeRange(range.key)"
              />
            </div>
          </div>

          <div class="datagen-params">
            <el-form-item label="数据组数">
              <el-input-number
                v-model="form.groupCount"
                :min="options?.limits.groupCount.min ?? 1"
                :max="options?.limits.groupCount.max ?? 50"
                controls-position="right"
              />
            </el-form-item>
            <el-form-item label="时间限制（ms）">
              <el-input-number
                v-model="form.timeLimitMs"
                :min="options?.limits.timeLimitMs.min ?? 100"
                :max="options?.limits.timeLimitMs.max ?? 60000"
                :step="100"
                controls-position="right"
              />
            </el-form-item>
            <el-form-item label="内存限制（MB）">
              <el-input-number
                v-model="form.memoryLimitMb"
                :min="options?.limits.memoryLimitMb.min ?? 16"
                :max="options?.limits.memoryLimitMb.max ?? 2048"
                :step="16"
                controls-position="right"
              />
            </el-form-item>
          </div>

          <el-form-item
            v-if="mode !== 'random'"
            label="特殊构造要求（可选）"
            class="datagen-special"
          >
            <el-input
              v-model="form.specialRequirements"
              type="textarea"
              :rows="3"
              maxlength="2000"
              show-word-limit
              placeholder="例如：包含边界值、构造链状图、保证答案不超过 64 位整数。"
            />
          </el-form-item>
        </section>

        <section class="datagen-section">
          <header class="datagen-section__head">
            <span class="datagen-step">04</span>
            <div>
              <h2>标准程序</h2>
              <p>标准程序会在沙箱中读取每组输入并产出对应输出，编译失败会使任务终止。</p>
            </div>
            <el-select
              v-model="form.stdLanguage"
              class="datagen-language"
              @change="handleLanguageChange"
            >
              <el-option
                v-for="language in options?.stdLanguages ?? []"
                :key="language.value"
                :label="language.label"
                :value="language.value"
              />
            </el-select>
          </header>

          <CodeEditor
            :model-value="form.stdCode"
            :language="form.stdLanguage"
            @update:model-value="handleStdCodeChange"
          />
        </section>

        <footer class="datagen-submit">
          <div class="datagen-submit__meta">
            <el-icon v-if="llmBlockedByActiveTask"><WarningFilled /></el-icon>
            <span v-if="llmBlockedByActiveTask">
              你有一个正在进行的 LLM 造数据任务，完成或失败后才能再次提交。
            </span>
            <span v-else-if="hasActiveTask">
              已有 LLM 任务在运行，你仍可以提交随机生成任务。
            </span>
            <span v-else>提交后任务在后台执行，可在任务列表查看进度。</span>
          </div>
          <el-button
            type="primary"
            size="large"
            native-type="submit"
            :loading="submitting"
            :disabled="submitDisabled"
          >
            <el-icon><MagicStick /></el-icon>
            开始生成
          </el-button>
        </footer>
      </form>

      <aside class="datagen-side">
        <section class="datagen-side-card tz-card">
          <div class="datagen-side-card__head">
            <span class="datagen-side-card__icon"><el-icon><Key /></el-icon></span>
            <div>
              <h3>LLM 凭证</h3>
              <p>密钥加密存储，仅用于你发起的造数据任务。</p>
            </div>
          </div>

          <template v-if="credential?.configured">
            <div class="datagen-credential">
              <div class="datagen-credential__row">
                <span>服务商</span>
                <strong>{{ credential.providerName }}</strong>
              </div>
              <div class="datagen-credential__row">
                <span>模型</span>
                <strong>{{ credential.model }}</strong>
              </div>
              <div class="datagen-credential__row">
                <span>密钥</span>
                <strong class="datagen-mono">{{ credential.apiKeyMasked }}</strong>
              </div>
              <div class="datagen-credential__row">
                <span>状态</span>
                <el-tag :type="statusTagType(credential.status ?? 'invalid')" effect="plain" size="small">
                  {{ credential.status === 'valid' ? '已验证' : '已失效' }}
                </el-tag>
              </div>
            </div>
            <el-alert
              v-if="credential.status === 'invalid'"
              type="error"
              :closable="false"
              show-icon
              :title="credential.lastValidationError || '凭证已失效，请重新配置'"
            />
            <p class="datagen-credential__time">
              最近验证：{{ displayTime(credential.lastValidatedAt) }}
            </p>
            <div class="datagen-side-card__actions">
              <el-button type="primary" plain :icon="Refresh" @click="openCredentialDialog">
                更新凭证
              </el-button>
              <el-button :icon="Lock" @click="refreshCredential">刷新状态</el-button>
            </div>
          </template>
          <template v-else>
            <div class="datagen-credential-empty">
              <el-icon><Lock /></el-icon>
              <p>尚未配置 LLM 凭证，随机生成模式不受影响。</p>
            </div>
            <el-button type="primary" class="datagen-wide-button" :icon="Key" @click="openCredentialDialog">
              配置 LLM 凭证
            </el-button>
          </template>
        </section>

        <section class="datagen-side-card tz-card">
          <div class="datagen-side-card__head">
            <span class="datagen-side-card__icon datagen-side-card__icon--mint">
              <el-icon><CircleCheck /></el-icon>
            </span>
            <div>
              <h3>生成流程</h3>
              <p>任务异步执行，失败不会自动重试。</p>
            </div>
          </div>
          <ol class="datagen-pipeline">
            <li>
              <span>01</span>
              <div>
                <strong>生成输入</strong>
                <small>随机、构造器或 LLM 直出，并校验取值范围。</small>
              </div>
            </li>
            <li>
              <span>02</span>
              <div>
                <strong>运行标准程序</strong>
                <small>逐组在 go-judge 沙箱中产出输出文件。</small>
              </div>
            </li>
            <li>
              <span>03</span>
              <div>
                <strong>下载结果</strong>
                <small>按组下载 .in / .out，或打包为 ZIP。</small>
              </div>
            </li>
          </ol>
        </section>

        <section class="datagen-side-card tz-card">
          <div class="datagen-side-card__head datagen-side-card__head--compact">
            <div>
              <h3>最近任务</h3>
              <p>仅展示最近 4 条记录。</p>
            </div>
            <RouterLink to="/datagen/tasks" class="datagen-text-link">全部</RouterLink>
          </div>

          <div v-loading="recentLoading" class="datagen-recent">
            <RouterLink
              v-for="task in recentTasks"
              :key="task.id"
              :to="`/datagen/tasks/${task.id}`"
              class="datagen-recent__item"
            >
              <div>
                <strong>{{ task.title }}</strong>
                <small>
                  {{ generationModeLabel(task.generationMode) }} · {{ task.groupCount }} 组 ·
                  {{ displayTime(task.createdAt) }}
                </small>
              </div>
              <el-tag :type="statusTagType(task.status)" effect="plain" size="small">
                {{ datagenStatusLabel(task.status) }}
              </el-tag>
            </RouterLink>
            <el-empty v-if="!recentLoading && !recentTasks.length" description="暂无造数据任务" :image-size="56" />
          </div>
        </section>
      </aside>
    </div>

    <el-dialog
      v-model="credentialDialogVisible"
      title="LLM 凭证"
      width="520px"
      class="datagen-dialog"
    >
      <div v-loading="providersLoading" class="datagen-credential-dialog">
        <el-alert
          v-if="credential?.hasActiveTask"
          type="warning"
          :closable="false"
          show-icon
          title="正在进行的 LLM 造数据任务期间不能修改或删除凭证"
        />
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="保存前会调用一次服务商接口验证密钥，密钥仅加密保存，不会回显。"
        />
        <el-form label-position="top">
          <el-form-item label="服务商" required>
            <el-select v-model="credentialForm.provider" class="datagen-wide-button">
              <el-option
                v-for="provider in providers"
                :key="provider.provider"
                :label="provider.name"
                :value="provider.provider"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="模型" required>
            <el-select v-model="credentialForm.model" class="datagen-wide-button">
              <el-option
                v-for="model in activeProvider?.models ?? []"
                :key="model"
                :label="model"
                :value="model"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="API Key" required>
            <el-input
              v-model="credentialForm.apiKey"
              type="password"
              show-password
              autocomplete="off"
              placeholder="粘贴服务商控制台生成的 API Key"
            />
          </el-form-item>
        </el-form>
        <el-alert
          v-if="credentialError"
          type="error"
          :closable="false"
          show-icon
          :title="credentialError"
        />
      </div>
      <template #footer>
        <div class="datagen-dialog__footer">
          <el-button
            v-if="credential?.configured"
            type="danger"
            plain
            :loading="credentialDeleting"
            :disabled="credential?.hasActiveTask"
            @click="handleDeleteCredential"
          >
            删除凭证
          </el-button>
          <span v-else></span>
          <div>
            <el-button @click="credentialDialogVisible = false">取消</el-button>
            <el-button
              type="primary"
              :loading="credentialSaving"
              :disabled="credential?.hasActiveTask"
              @click="handleSaveCredential"
            >
              验证并保存
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
