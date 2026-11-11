<script setup lang="ts">
import { computed, defineAsyncComponent, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Check,
  Clock,
  Link,
  Loading,
  Refresh,
  TopRight,
  Trophy,
  Warning
} from '@element-plus/icons-vue'

import {
  createSubmission,
  fetchProblem,
  fetchSubmission,
  fetchSubmissions,
  type OjLanguage,
  type ProblemDetail,
  type SubmissionDetail,
  type SubmissionListItem
} from '@/api/problem'
import { withPlayerCsrfRetry } from '@/api/csrf'
import { updateExternalProblemCompletion } from '@/api/learning'
import {
  objectiveTypeLabel,
  problemDifficultyLabel,
  problemTypeLabel
} from '@/utils/adminLabels'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatDateTime } from '@/utils/format'
import { renderMarkdown } from '@/utils/markdown'
import {
  compactVerdictLabel,
  isInProgressStatus,
  isTerminalStatus,
  languageLabel,
  statusTagType,
  submissionStatusLabel,
  verdictLabel,
  verdictTagType
} from '@/utils/problemLabels'
import { useAuthStore } from '@/stores/auth'
import { readStorage, writeStorage } from '@/utils/storage'

// 编辑器体积较大，仅在 OJ 题目实际渲染时加载。
const CodeEditor = defineAsyncComponent(() => import('@/components/problem/CodeEditor.vue'))

const HISTORY_PAGE_SIZE = 12
const MAX_POLL_ATTEMPTS = 180

const CODE_TEMPLATES: Record<OjLanguage, string> = {
  cpp17: `#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    // 在这里编写你的代码

    return 0;
}
`,
  python310: `import sys

def solve() -> None:
    data = sys.stdin.read().split()
    # 在这里编写你的代码

if __name__ == "__main__":
    solve()
`
}

const route = useRoute()
const authStore = useAuthStore()

const problem = ref<ProblemDetail | null>(null)
const loading = ref(true)
const errorMessage = ref('')

const submissions = ref<SubmissionListItem[]>([])
const submissionsLoading = ref(false)
const historyError = ref('')

const currentSubmission = ref<SubmissionDetail | null>(null)
const submissionLoading = ref(false)
const submitting = ref(false)
const externalSaving = ref(false)

const selectedOptions = ref<string[]>([])
const singleOption = ref('')
const answerText = ref('')
const language = ref<OjLanguage>('cpp17')
const code = ref('')

const polling = ref(false)
let pollTimer: number | null = null
let activePollSubmissionId = ''
let pollAttempts = 0
let problemLoadToken = 0

const isOj = computed(() => problem.value?.type === 'oj')
const isObjective = computed(() => problem.value?.type === 'objective')
const isShortAnswer = computed(() => problem.value?.type === 'short_answer')
const isExternal = computed(() => problem.value?.type === 'external')
const isMultipleChoice = computed(() => problem.value?.objectiveType === 'multiple')

const statementHtml = computed(() => renderMarkdown(problem.value?.statement))
const inputFormatHtml = computed(() => renderMarkdown(problem.value?.inputFormat))
const outputFormatHtml = computed(() => renderMarkdown(problem.value?.outputFormat))

const answerTitle = computed(() => {
  if (isOj.value) {
    return '编写代码'
  }
  if (isObjective.value) {
    return '选择答案'
  }
  return '填写答案'
})

const submitLabel = computed(() => (isOj.value ? '提交代码' : '提交答案'))

const currentInProgress = computed(
  () => currentSubmission.value !== null && isInProgressStatus(currentSubmission.value.status)
)

function codeTemplate(value: OjLanguage): string {
  return CODE_TEMPLATES[value]
}

function draftKey(problemId: string, value: OjLanguage): string {
  const userId = authStore.user?.id ?? 'anonymous'
  return `tizhen:problem-draft:${userId}:${problemId}:${value}`
}

function loadDraft(problemId: string, value: OjLanguage): string {
  const draft = readStorage(draftKey(problemId, value))
  return draft && draft.trim().length > 0 ? draft : codeTemplate(value)
}

function resetAnswerForm(detail: ProblemDetail) {
  selectedOptions.value = []
  singleOption.value = ''
  answerText.value = ''
  language.value = 'cpp17'
  code.value = detail.type === 'oj' ? loadDraft(detail.id, 'cpp17') : ''
}

async function loadProblem() {
  const problemId = String(route.params.problemId ?? '')
  const token = ++problemLoadToken
  stopPolling()
  loading.value = true
  errorMessage.value = ''
  problem.value = null
  currentSubmission.value = null
  submissions.value = []
  historyError.value = ''
  try {
    const detail = await fetchProblem(problemId)
    if (token !== problemLoadToken) {
      return
    }
    problem.value = detail
    resetAnswerForm(detail)
    void loadSubmissions(detail.id)
  } catch (error) {
    if (token === problemLoadToken) {
      errorMessage.value = resolveApiErrorMessage(error, '题目加载失败，请稍后重试')
    }
  } finally {
    if (token === problemLoadToken) {
      loading.value = false
    }
  }
}

async function loadSubmissions(problemId: string, silent = false) {
  if (!silent) {
    submissionsLoading.value = true
  }
  historyError.value = ''
  try {
    const result = await fetchSubmissions({
      problemId,
      page: 1,
      pageSize: HISTORY_PAGE_SIZE
    })
    if (problem.value?.id !== problemId) {
      return
    }
    submissions.value = result.items
    if (!silent && currentSubmission.value === null) {
      const inProgress = result.items.find((item) => isInProgressStatus(item.status))
      if (inProgress) {
        void openSubmission(inProgress.id)
      }
    }
  } catch (error) {
    if (problem.value?.id === problemId) {
      historyError.value = resolveApiErrorMessage(error, '提交记录加载失败，请稍后重试')
    }
  } finally {
    submissionsLoading.value = false
  }
}

async function refreshHistory() {
  const currentProblem = problem.value
  if (!currentProblem) {
    return
  }
  await loadSubmissions(currentProblem.id, true)
}

async function refreshProgress() {
  const currentProblem = problem.value
  if (!currentProblem) {
    return
  }
  try {
    const fresh = await fetchProblem(currentProblem.id)
    currentProblem.myProgress = fresh.myProgress
    currentProblem.externalCompleted = fresh.externalCompleted
  } catch {
    // 进度刷新失败不影响当前判题结果展示。
  }
}

async function openSubmission(submissionId: string) {
  if (submissionLoading.value) {
    return
  }
  submissionLoading.value = true
  stopPolling()
  try {
    const detail = await fetchSubmission(submissionId)
    if (!problem.value || detail.problem.id !== problem.value.id) {
      return
    }
    currentSubmission.value = detail
    if (isInProgressStatus(detail.status)) {
      startPolling(detail.id)
    }
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '提交详情加载失败，请稍后重试'))
  } finally {
    submissionLoading.value = false
  }
}

function handleCreated(detail: SubmissionDetail) {
  currentSubmission.value = detail
  void refreshHistory()
  if (isInProgressStatus(detail.status)) {
    startPolling(detail.id)
  } else {
    void refreshProgress()
  }
}

async function submitOj() {
  const currentProblem = problem.value
  if (!currentProblem || submitting.value) {
    return
  }
  if (code.value.trim().length === 0) {
    ElMessage.warning('请先编写代码')
    return
  }
  submitting.value = true
  try {
    const detail = await withPlayerCsrfRetry(() =>
      createSubmission(currentProblem.id, {
        language: language.value,
        code: code.value
      })
    )
    handleCreated(detail)
    ElMessage.success('提交成功，正在判题')
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '提交失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

async function submitObjective() {
  const currentProblem = problem.value
  if (!currentProblem || submitting.value) {
    return
  }
  const selected = isMultipleChoice.value
    ? [...selectedOptions.value]
    : singleOption.value
      ? [singleOption.value]
      : []
  if (selected.length === 0) {
    ElMessage.warning('请先选择答案')
    return
  }
  submitting.value = true
  try {
    const detail = await withPlayerCsrfRetry(() =>
      createSubmission(currentProblem.id, { selectedOptions: selected })
    )
    handleCreated(detail)
    ElMessage.success(detail.verdict === 'AC' ? '回答正确' : '回答错误，已记入错题本')
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '提交失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

async function submitShortAnswer() {
  const currentProblem = problem.value
  if (!currentProblem || submitting.value) {
    return
  }
  if (answerText.value.trim().length === 0) {
    ElMessage.warning('请先填写答案')
    return
  }
  submitting.value = true
  try {
    const detail = await withPlayerCsrfRetry(() =>
      createSubmission(currentProblem.id, { answerText: answerText.value })
    )
    handleCreated(detail)
    ElMessage.success('已提交，AI 正在评分')
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '提交失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

async function toggleExternalCompletion() {
  const currentProblem = problem.value
  if (!currentProblem || externalSaving.value) {
    return
  }
  externalSaving.value = true
  try {
    const result = await withPlayerCsrfRetry(() =>
      updateExternalProblemCompletion(currentProblem.id, !currentProblem.externalCompleted)
    )
    currentProblem.externalCompleted = result.completed
    ElMessage.success(result.completed ? '外链题已标记完成' : '已取消外链题完成状态')
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '外链题状态更新失败，请稍后重试'))
  } finally {
    externalSaving.value = false
  }
}

function handleLanguageChange(value: string | number | boolean | undefined) {
  const currentProblem = problem.value
  if (!currentProblem) {
    return
  }
  const next: OjLanguage = value === 'python310' ? 'python310' : 'cpp17'
  if (next === language.value) {
    return
  }
  writeStorage(draftKey(currentProblem.id, language.value), code.value)
  language.value = next
  code.value = loadDraft(currentProblem.id, next)
}

function resetCode() {
  code.value = codeTemplate(language.value)
}

function startPolling(submissionId: string) {
  stopPolling()
  activePollSubmissionId = submissionId
  pollAttempts = 0
  polling.value = true
  schedulePoll(submissionId, 500)
}

function schedulePoll(submissionId: string, delay: number) {
  pollTimer = window.setTimeout(async () => {
    if (activePollSubmissionId !== submissionId) {
      return
    }
    pollAttempts += 1
    try {
      const detail = await fetchSubmission(submissionId)
      if (activePollSubmissionId !== submissionId) {
        return
      }
      currentSubmission.value = detail
      if (isTerminalStatus(detail.status)) {
        stopPolling()
        void refreshHistory()
        void refreshProgress()
        if (detail.status === 'finished') {
          ElMessage.success(`判题完成：${verdictLabel(detail.verdict)}`)
        } else {
          ElMessage.warning(detail.failureReason || '处理失败，请稍后重试')
        }
      } else if (pollAttempts < MAX_POLL_ATTEMPTS) {
        schedulePoll(submissionId, nextPollDelay(pollAttempts))
      } else {
        stopPolling()
        ElMessage.warning('判题时间较长，请稍后刷新页面查看结果')
      }
    } catch {
      if (activePollSubmissionId !== submissionId) {
        return
      }
      stopPolling()
      ElMessage.error('判题结果获取失败，请刷新页面重试')
    }
  }, delay)
}

function nextPollDelay(attempt: number): number {
  return Math.min(2_000, Math.round(500 * Math.pow(1.35, attempt)))
}

function stopPolling() {
  if (pollTimer !== null) {
    window.clearTimeout(pollTimer)
    pollTimer = null
  }
  polling.value = false
  activePollSubmissionId = ''
}

function optionState(optionKey: string): string {
  const result = currentSubmission.value
  if (!result || result.status !== 'finished' || !result.correctOptions) {
    return ''
  }
  if (result.correctOptions.includes(optionKey)) {
    return 'is-correct'
  }
  if (result.answerJson?.includes(optionKey)) {
    return 'is-wrong'
  }
  return ''
}

function optionContent(optionKey: string): string {
  return problem.value?.options.find((option) => option.optionKey === optionKey)?.content ?? ''
}

function formatScore(score: number | null): string {
  if (score === null) {
    return '—'
  }
  return String(Math.round(score * 100) / 100)
}

function formatMemory(memoryKb: number | null): string {
  if (memoryKb === null) {
    return '—'
  }
  if (memoryKb < 1024) {
    return `${memoryKb} KB`
  }
  return `${(memoryKb / 1024).toFixed(1)} MB`
}

function historySummary(item: SubmissionListItem): string {
  if (isInProgressStatus(item.status)) {
    return submissionStatusLabel(item.status)
  }
  return verdictLabel(item.verdict)
}

function handleGlobalShortcut(event: KeyboardEvent) {
  if ((event.ctrlKey || event.metaKey) && event.key === 'Enter' && isOj.value) {
    event.preventDefault()
    void submitOj()
  }
}

watch(
  () => route.params.problemId,
  () => {
    void loadProblem()
  },
  { immediate: true }
)

watch(code, (value) => {
  const currentProblem = problem.value
  if (currentProblem?.type === 'oj') {
    writeStorage(draftKey(currentProblem.id, language.value), value)
  }
})

onMounted(() => {
  window.addEventListener('keydown', handleGlobalShortcut)
})

onBeforeUnmount(() => {
  stopPolling()
  window.removeEventListener('keydown', handleGlobalShortcut)
})
</script>

<template>
  <div class="problem-detail-page learning-page">
    <nav class="learning-breadcrumb" aria-label="面包屑">
      <RouterLink to="/problems">题库</RouterLink>
      <span>/</span>
      <span aria-current="page">{{ problem?.title ?? '题目详情' }}</span>
    </nav>

    <div v-if="loading" class="problem-detail-skeleton">
      <el-skeleton animated :rows="2" />
      <div class="problem-detail-skeleton__grid">
        <el-skeleton animated :rows="8" />
        <el-skeleton animated :rows="5" />
      </div>
    </div>

    <div v-else-if="errorMessage" class="learning-state">
      <span class="learning-state__icon learning-state__icon--danger">!</span>
      <h3>这道题暂时无法打开</h3>
      <p>{{ errorMessage }}</p>
      <div class="learning-state__actions">
        <el-button :icon="Refresh" @click="loadProblem">重新加载</el-button>
        <RouterLink class="problem-text-link" to="/problems">返回到题库</RouterLink>
      </div>
    </div>

    <template v-else-if="problem">
      <section class="problem-detail-hero" :class="`problem-detail-hero--${problem.type}`">
        <div class="problem-detail-hero__main">
          <div class="problem-detail-hero__tags">
            <span class="problem-type-pill">{{ problemTypeLabel(problem.type) }}</span>
            <span v-if="problem.objectiveType" class="problem-card__subtype">
              {{ objectiveTypeLabel(problem.objectiveType) }}
            </span>
            <span class="problem-difficulty-pill">
              {{ problemDifficultyLabel(problem.difficulty) }}
            </span>
            <span v-if="problem.myProgress.firstAc" class="problem-solved-pill">
              <el-icon><Trophy /></el-icon>
              已通过
            </span>
          </div>

          <h1>{{ problem.title }}</h1>

          <div class="problem-detail-hero__meta">
            <span v-if="problem.source && problem.source !== problem.platform">{{ problem.source }}</span>
            <span v-if="problem.platform">{{ problem.platform }}</span>
            <span v-if="problem.type === 'oj' && problem.timeLimitMs">
              时限 {{ problem.timeLimitMs }} ms
            </span>
            <span v-if="problem.type === 'oj' && problem.memoryLimitMb">
              内存 {{ problem.memoryLimitMb }} MB
            </span>
            <span v-if="problem.maxScore">满分 {{ problem.maxScore }}</span>
          </div>

          <div v-if="problem.tags.length > 0" class="problem-detail-hero__tag-list">
            <span v-for="tag in problem.tags" :key="tag">{{ tag }}</span>
          </div>
        </div>

        <div class="problem-progress-card">
          <span class="problem-progress-card__label">
            {{ isExternal ? '外链进度' : '我的作答' }}
          </span>
          <template v-if="isExternal">
            <strong
              class="problem-progress-card__status"
              :class="{ 'is-completed': problem.externalCompleted }"
            >
              {{ problem.externalCompleted ? '已完成' : '未完成' }}
            </strong>
            <span class="problem-progress-card__unit">
              {{ problem.platform || '外部平台' }}
            </span>
            <div class="problem-progress-card__rows">
              <span>
                <el-icon><Check /></el-icon>
                完成状态由你手动标记
              </span>
              <span v-if="problem.externalUrl">完成练习后可随时返回更新</span>
            </div>
          </template>
          <template v-else>
            <strong>{{ problem.myProgress.attemptCount }}</strong>
            <span class="problem-progress-card__unit">次提交</span>
            <div class="problem-progress-card__rows">
              <span v-if="problem.myProgress.firstAc">
                <el-icon><Trophy /></el-icon>
                首次通过于
                {{ problem.myProgress.firstAcAt ? formatDateTime(problem.myProgress.firstAcAt) : '未知时间' }}
              </span>
              <span v-else>
                <el-icon><Clock /></el-icon>
                尚未通过
              </span>
              <span v-if="problem.myProgress.lastSubmittedAt">
                最近提交 {{ formatDateTime(problem.myProgress.lastSubmittedAt) }}
              </span>
              <span v-if="problem.myProgress.lastVerdict">
                最近结果 {{ verdictLabel(problem.myProgress.lastVerdict) }}
              </span>
            </div>
          </template>
        </div>
      </section>

      <div class="problem-detail-grid">
        <div class="problem-detail-main">
          <section class="problem-panel">
            <div class="problem-panel__head">
              <div>
                <h2>题目描述</h2>
                <p v-if="isOj">题面中的公式与样例仅作为题目说明，测试数据不会公开。</p>
                <p v-else-if="isObjective">提交后立即判分，正确答案与解析仅在自己查看结果时展示。</p>
                <p v-else-if="isShortAnswer">评分要点与参考答案不会在提交前展示。</p>
                <p v-else>题目在外部平台完成，题阵只记录你的手动完成状态。</p>
              </div>
              <span class="problem-panel__eyebrow">STATEMENT</span>
            </div>
            <div class="markdown-body problem-statement" v-html="statementHtml"></div>
          </section>

          <section v-if="isOj" class="problem-panel problem-format-panel">
            <div class="problem-format-section">
              <h2>输入格式</h2>
              <div class="markdown-body" v-html="inputFormatHtml"></div>
              <p v-if="!inputFormatHtml" class="problem-panel__empty">题目未单独说明输入格式。</p>
            </div>
            <div class="problem-format-section">
              <h2>输出格式</h2>
              <div class="markdown-body" v-html="outputFormatHtml"></div>
              <p v-if="!outputFormatHtml" class="problem-panel__empty">题目未单独说明输出格式。</p>
            </div>
          </section>

          <section v-if="isOj && problem.samples.length > 0" class="problem-panel">
            <div class="problem-panel__head">
              <div>
                <h2>样例</h2>
                <p>样例只用于理解题意，判题以隐藏测试点为准。</p>
              </div>
              <span class="learning-count">{{ problem.samples.length }} 组</span>
            </div>
            <div class="problem-sample-list">
              <article
                v-for="(sample, index) in problem.samples"
                :key="sample.sortOrder"
                class="problem-sample"
              >
                <span class="problem-sample__index">样例 {{ index + 1 }}</span>
                <div class="problem-sample__columns">
                  <div>
                    <span class="problem-sample__label">输入</span>
                    <pre>{{ sample.inputData }}</pre>
                  </div>
                  <div>
                    <span class="problem-sample__label">输出</span>
                    <pre>{{ sample.outputData }}</pre>
                  </div>
                </div>
                <p v-if="sample.explanation" class="problem-sample__explanation">
                  {{ sample.explanation }}
                </p>
              </article>
            </div>
          </section>

          <section v-if="isExternal" class="problem-panel problem-external-panel">
            <div class="problem-panel__head">
              <div>
                <h2>外链练习</h2>
                <p>该题在外部平台完成，题阵只记录你的手动完成状态。</p>
              </div>
              <span class="problem-panel__eyebrow">EXTERNAL</span>
            </div>
            <div class="problem-external-actions">
              <a
                v-if="problem.externalUrl"
                :href="problem.externalUrl"
                class="problem-external-link"
                target="_blank"
                rel="noopener noreferrer"
              >
                <el-icon><Link /></el-icon>
                打开 {{ problem.platform || '外部平台' }} 题目
                <el-icon><TopRight /></el-icon>
              </a>
              <span v-else class="problem-panel__empty">该题暂未配置外链地址。</span>

              <button
                type="button"
                class="completion-toggle"
                :class="{ 'is-completed': problem.externalCompleted }"
                :disabled="externalSaving"
                :aria-pressed="problem.externalCompleted === true"
                @click="toggleExternalCompletion"
              >
                <el-icon v-if="problem.externalCompleted"><Check /></el-icon>
                <span>
                  {{ problem.externalCompleted ? '已完成' : '标记为已完成' }}
                </span>
              </button>
            </div>
          </section>

          <section v-else class="problem-panel problem-answer-panel">
            <div class="problem-panel__head">
              <div>
                <h2>{{ answerTitle }}</h2>
                <p v-if="isOj">代码只在沙箱中运行，提交后会自动判题。</p>
                <p v-else-if="isObjective">选择你认为正确的选项，提交后立即判分。</p>
                <p v-else>答案提交后由平台 AI 依据评分要点判分。</p>
              </div>
              <span class="problem-panel__eyebrow">ANSWER</span>
            </div>

            <template v-if="isOj">
              <div class="problem-editor-toolbar">
                <el-radio-group
                  :model-value="language"
                  size="small"
                  @change="handleLanguageChange"
                >
                  <el-radio-button value="cpp17">C++17</el-radio-button>
                  <el-radio-button value="python310">Python 3.10</el-radio-button>
                </el-radio-group>
                <div class="problem-editor-toolbar__right">
                  <span>Ctrl / ⌘ + Enter 提交</span>
                  <el-button text :icon="Refresh" @click="resetCode">恢复模板</el-button>
                </div>
              </div>
              <CodeEditor v-model="code" :language="language" :readonly="submitting" />
              <div class="problem-answer-actions">
                <span class="problem-answer-hint">
                  {{ languageLabel(language) }} · 代码最长 64 KB
                </span>
                <el-button type="primary" :loading="submitting" @click="submitOj">
                  {{ submitLabel }}
                </el-button>
              </div>
            </template>

            <template v-else-if="isObjective">
              <el-radio-group
                v-if="!isMultipleChoice"
                v-model="singleOption"
                class="objective-options"
              >
                <el-radio
                  v-for="option in problem.options"
                  :key="option.optionKey"
                  :value="option.optionKey"
                  class="objective-option"
                  :class="optionState(option.optionKey)"
                >
                  <span class="objective-option__key">{{ option.optionKey }}</span>
                  <span class="objective-option__content">{{ option.content }}</span>
                </el-radio>
              </el-radio-group>
              <el-checkbox-group v-else v-model="selectedOptions" class="objective-options">
                <el-checkbox
                  v-for="option in problem.options"
                  :key="option.optionKey"
                  :value="option.optionKey"
                  class="objective-option"
                  :class="optionState(option.optionKey)"
                >
                  <span class="objective-option__key">{{ option.optionKey }}</span>
                  <span class="objective-option__content">{{ option.content }}</span>
                </el-checkbox>
              </el-checkbox-group>

              <div class="problem-answer-actions">
                <span class="problem-answer-hint">
                  {{ isMultipleChoice ? '多选题，可选多个选项' : '单选，只能选择一个选项' }}
                </span>
                <el-button type="primary" :loading="submitting" @click="submitObjective">
                  {{ submitLabel }}
                </el-button>
              </div>
            </template>

            <template v-else-if="isShortAnswer">
              <el-input
                v-model="answerText"
                type="textarea"
                :rows="8"
                maxlength="10000"
                show-word-limit
                resize="vertical"
                placeholder="用清晰的要点组织你的答案，AI 会结合参考答案与评分要点评分。"
                :disabled="submitting"
              />
              <div class="problem-answer-actions">
                <span class="problem-answer-hint">满分 {{ problem.maxScore ?? 100 }} 分，60 分及以上视为通过</span>
                <el-button type="primary" :loading="submitting" @click="submitShortAnswer">
                  {{ submitLabel }}
                </el-button>
              </div>
            </template>
          </section>

          <section v-if="submissionLoading" class="problem-panel">
            <el-skeleton animated :rows="4" />
          </section>

          <section v-else-if="currentSubmission" class="problem-panel problem-result-panel">
            <div class="problem-result-head">
              <div>
                <span class="problem-result-head__eyebrow">
                  提交 #{{ currentSubmission.id }}
                </span>
                <h2>
                  提交结果
                  <el-tag
                    v-if="isInProgressStatus(currentSubmission.status)"
                    :type="statusTagType(currentSubmission.status)"
                    effect="plain"
                    round
                  >
                    {{ submissionStatusLabel(currentSubmission.status) }}
                  </el-tag>
                  <el-tag
                    v-else-if="currentSubmission.status === 'failed'"
                    type="danger"
                    effect="plain"
                    round
                  >
                    处理失败
                  </el-tag>
                  <el-tag
                    v-else-if="currentSubmission.verdict"
                    :type="verdictTagType(currentSubmission.verdict)"
                    effect="plain"
                    round
                  >
                    {{ verdictLabel(currentSubmission.verdict) }}
                  </el-tag>
                </h2>
                <span class="problem-result-head__time">
                  提交于 {{ formatDateTime(currentSubmission.createdAt) }}
                </span>
              </div>
              <el-tag
                v-if="currentSubmission.firstAc"
                class="problem-result-head__first-ac"
                type="success"
                effect="dark"
                round
              >
                首次通过
              </el-tag>
            </div>

            <div v-if="currentInProgress" class="problem-result-judging">
              <el-icon class="is-loading"><Loading /></el-icon>
              <div>
                <strong>
                  {{ currentSubmission.status === 'grading' ? 'AI 正在评分' : '判题进行中' }}
                </strong>
                <p>结果会自动更新，无需刷新页面。</p>
              </div>
            </div>

            <template v-else>
              <div v-if="currentSubmission.status === 'failed'" class="problem-result-failure">
                <el-icon><Warning /></el-icon>
                <div>
                  <strong>本次处理失败</strong>
                  <p>{{ currentSubmission.failureReason || '请稍后重新提交' }}</p>
                </div>
              </div>

              <template v-else>
                <div class="problem-result-stats">
                  <div v-if="currentSubmission.score !== null">
                    <span>得分</span>
                    <strong>{{ formatScore(currentSubmission.score) }}</strong>
                  </div>
                  <div v-if="currentSubmission.judgeTimeMs !== null">
                    <span>运行时间</span>
                    <strong>{{ currentSubmission.judgeTimeMs }} ms</strong>
                  </div>
                  <div v-if="currentSubmission.memoryKb !== null">
                    <span>内存占用</span>
                    <strong>{{ formatMemory(currentSubmission.memoryKb) }}</strong>
                  </div>
                  <div v-if="currentSubmission.language">
                    <span>语言</span>
                    <strong>{{ languageLabel(currentSubmission.language) }}</strong>
                  </div>
                  <div v-if="currentSubmission.judgedAt">
                    <span>完成时间</span>
                    <strong>{{ formatDateTime(currentSubmission.judgedAt) }}</strong>
                  </div>
                </div>

                <div v-if="currentSubmission.judgeDetail" class="problem-result-block">
                  <span class="problem-result-block__label">判题结果</span>
                  <p>{{ currentSubmission.judgeDetail }}</p>
                </div>

                <div v-if="currentSubmission.compileOutput" class="problem-result-block">
                  <span class="problem-result-block__label">编译输出</span>
                  <pre class="problem-result-code">{{ currentSubmission.compileOutput }}</pre>
                </div>

                <div v-if="currentSubmission.aiFeedback" class="problem-result-block">
                  <span class="problem-result-block__label">AI 评语</span>
                  <p class="problem-result-pre">{{ currentSubmission.aiFeedback }}</p>
                </div>

                <div
                  v-if="currentSubmission.correctOptions && currentSubmission.correctOptions.length > 0"
                  class="problem-result-block"
                >
                  <span class="problem-result-block__label">正确答案</span>
                  <div class="problem-result-options">
                    <span
                      v-for="key in currentSubmission.correctOptions"
                      :key="key"
                      class="problem-result-option"
                    >
                      <strong>{{ key }}</strong>
                      {{ optionContent(key) }}
                    </span>
                  </div>
                </div>

                <div v-if="currentSubmission.explanation" class="problem-result-block">
                  <span class="problem-result-block__label">题目解析</span>
                  <p class="problem-result-pre">{{ currentSubmission.explanation }}</p>
                </div>

                <div v-if="currentSubmission.referenceAnswer" class="problem-result-block">
                  <span class="problem-result-block__label">参考答案</span>
                  <p class="problem-result-pre">{{ currentSubmission.referenceAnswer }}</p>
                </div>

                <div
                  v-if="currentSubmission.scoringRubric && currentSubmission.scoringRubric.length > 0"
                  class="problem-result-block"
                >
                  <span class="problem-result-block__label">评分要点</span>
                  <ul class="problem-rubric">
                    <li
                      v-for="(item, index) in currentSubmission.scoringRubric"
                      :key="`${item.point}-${index}`"
                    >
                      <span>{{ item.point }}</span>
                      <strong>{{ formatScore(item.score) }} 分</strong>
                      <small v-if="item.description">{{ item.description }}</small>
                    </li>
                  </ul>
                </div>

                <el-collapse
                  v-if="currentSubmission.code || currentSubmission.answerText || (currentSubmission.answerJson && currentSubmission.answerJson.length > 0)"
                  class="problem-result-collapse"
                >
                  <el-collapse-item title="查看本次提交内容" name="answer">
                    <pre v-if="currentSubmission.code" class="problem-result-code">{{ currentSubmission.code }}</pre>
                    <p
                      v-else-if="currentSubmission.answerText"
                      class="problem-result-pre"
                    >{{ currentSubmission.answerText }}</p>
                    <p v-else-if="currentSubmission.answerJson">
                      已选：{{ currentSubmission.answerJson.join('、') }}
                    </p>
                  </el-collapse-item>
                </el-collapse>
              </template>
            </template>
          </section>
        </div>

        <aside class="problem-detail-aside">
          <section class="problem-panel problem-history-panel">
            <div class="problem-panel__head">
              <div>
                <h2>我的提交</h2>
                <p>仅展示当前题目的作答记录。</p>
              </div>
              <el-button
                text
                :icon="Refresh"
                :loading="submissionsLoading"
                aria-label="刷新提交记录"
                @click="refreshHistory"
              />
            </div>

            <p v-if="historyError" class="problem-history-error">{{ historyError }}</p>
            <el-skeleton v-else-if="submissionsLoading" animated :rows="4" />
            <p v-else-if="submissions.length === 0" class="problem-panel__empty">
              还没有提交记录，完成一次作答后会显示在这里。
            </p>
            <div v-else class="problem-history-list">
              <button
                v-for="item in submissions"
                :key="item.id"
                type="button"
                class="problem-history-row"
                :class="{ 'is-active': currentSubmission?.id === item.id }"
                @click="openSubmission(item.id)"
              >
                <span class="problem-history-row__main">
                  <strong>{{ compactVerdictLabel(item.verdict) }}</strong>
                  <span>{{ historySummary(item) }}</span>
                  <el-tag
                    v-if="item.score !== null"
                    size="small"
                    effect="plain"
                    round
                  >
                    {{ formatScore(item.score) }} 分
                  </el-tag>
                </span>
                <span class="problem-history-row__meta">
                  <span>{{ formatDateTime(item.createdAt) }}</span>
                  <span v-if="item.language">{{ languageLabel(item.language) }}</span>
                </span>
              </button>
            </div>
          </section>

          <section class="problem-panel problem-side-note">
            <span class="problem-panel__eyebrow">NOTE</span>
            <h2>作答提示</h2>
            <template v-if="isOj">
              <p>判题会逐组运行隐藏测试数据，结果包含用时与内存占用。</p>
              <p>首次通过只记录一次；非 AC 提交会同步到错题本。</p>
            </template>
            <template v-else-if="isObjective">
              <p>提交后立即返回结果，正确答案与解析仅在自己查看结果时展示。</p>
            </template>
            <template v-else-if="isShortAnswer">
              <p>平台 AI 会参考参考答案和评分要点判分，60 分及以上记为通过。</p>
              <p>AI 判分失败不会写入错题本，可以稍后重新提交。</p>
            </template>
            <template v-else>
              <p>外链题不提供站内提交入口，完成练习后请手动标记状态。</p>
            </template>
            <RouterLink class="problem-text-link" to="/problems">
              <el-icon><ArrowLeft /></el-icon>
              返回题库
            </RouterLink>
          </section>
        </aside>
      </div>
    </template>
  </div>
</template>
