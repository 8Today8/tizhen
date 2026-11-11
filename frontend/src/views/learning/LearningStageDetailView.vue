<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Check,
  CircleCheck,
  Clock,
  Document,
  Link,
  Refresh,
  TopRight,
  VideoPlay
} from '@element-plus/icons-vue'

import {
  fetchLearningStage,
  updateExternalProblemCompletion,
  updateStageCompletion,
  type StageDetail,
  type StageProblemView
} from '@/api/learning'
import { withPlayerCsrfRetry } from '@/api/csrf'
import { objectiveTypeLabel, problemDifficultyLabel, problemTypeLabel } from '@/utils/adminLabels'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatDateTime, formatDuration } from '@/utils/format'
import { renderMarkdown } from '@/utils/markdown'

const route = useRoute()
const stage = ref<StageDetail | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const stageSaving = ref(false)
const externalSavingId = ref<string | null>(null)

const contentHtml = computed(() => renderMarkdown(stage.value?.content))

function problemMeta(problem: StageProblemView): string[] {
  const meta: string[] = []
  if (problem.problem.objectiveType) {
    meta.push(objectiveTypeLabel(problem.problem.objectiveType))
  }
  if (problem.problem.difficulty) {
    meta.push(`难度 · ${problemDifficultyLabel(problem.problem.difficulty)}`)
  }
  if (problem.problem.source) {
    meta.push(problem.problem.source)
  }
  if (problem.problem.platform) {
    meta.push(problem.problem.platform)
  }
  return meta
}

async function loadStage() {
  const stageId = String(route.params.stageId ?? '')
  loading.value = true
  errorMessage.value = ''
  stage.value = null
  try {
    stage.value = await fetchLearningStage(stageId)
  } catch (error) {
    errorMessage.value = resolveApiErrorMessage(error, '阶段内容加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function toggleStageCompletion() {
  const current = stage.value
  if (!current || stageSaving.value) {
    return
  }

  const target = !current.completed
  stageSaving.value = true
  try {
    const result = await withPlayerCsrfRetry(() =>
      updateStageCompletion(current.id, target)
    )
    current.completed = result.completed
    current.completedAt = result.completedAt
    ElMessage.success(result.completed ? '阶段已标记为完成' : '已取消阶段完成标记')
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '阶段进度更新失败，请稍后重试'))
  } finally {
    stageSaving.value = false
  }
}

async function toggleExternalCompletion(problem: StageProblemView) {
  if (externalSavingId.value) {
    return
  }

  const target = !problem.externalCompleted
  externalSavingId.value = problem.problem.id
  try {
    const result = await withPlayerCsrfRetry(() =>
      updateExternalProblemCompletion(problem.problem.id, target)
    )
    problem.externalCompleted = result.completed
    problem.completedAt = result.completedAt
    ElMessage.success(result.completed ? '外链题已标记完成' : '已取消外链题完成状态')
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '外链题状态更新失败，请稍后重试'))
  } finally {
    externalSavingId.value = null
  }
}

watch(
  () => route.params.stageId,
  () => {
    void loadStage()
  },
  { immediate: true }
)
</script>

<template>
  <div class="learning-page">
    <nav class="learning-breadcrumb" aria-label="面包屑">
      <RouterLink to="/learning">学习路线</RouterLink>
      <span>/</span>
      <RouterLink
        v-if="stage"
        :to="{ name: 'learning-route-detail', params: { routeId: stage.routeId } }"
      >
        {{ stage.routeName }}
      </RouterLink>
      <span v-else>路线详情</span>
      <span>/</span>
      <span aria-current="page">{{ stage?.name ?? '阶段详情' }}</span>
    </nav>

    <div v-if="loading" class="learning-detail-skeleton">
      <el-skeleton animated :rows="3" />
      <div class="learning-detail-skeleton__stages">
        <el-skeleton v-for="index in 2" :key="index" animated :rows="4" />
      </div>
    </div>

    <div v-else-if="errorMessage" class="learning-state">
      <span class="learning-state__icon learning-state__icon--danger">!</span>
      <h3>这个阶段暂时无法打开</h3>
      <p>{{ errorMessage }}</p>
      <div class="learning-state__actions">
        <el-button :icon="Refresh" @click="loadStage">重新加载</el-button>
        <RouterLink class="learning-text-link" to="/learning">返回路线列表</RouterLink>
      </div>
    </div>

    <template v-else-if="stage">
      <section class="stage-detail-hero" :class="{ 'is-completed': stage.completed }">
        <div class="stage-detail-hero__main">
          <span class="learning-eyebrow">STAGE · {{ stage.routeName }}</span>
          <div class="stage-detail-hero__title">
            <h1>{{ stage.name }}</h1>
            <el-tag v-if="stage.completed" type="success" effect="plain" round>已完成</el-tag>
            <el-tag v-else effect="plain" round>进行中</el-tag>
          </div>
          <div class="stage-detail-hero__meta">
            <span>
              <el-icon><VideoPlay /></el-icon>
              {{ stage.courses.length }} 节课程
            </span>
            <span>
              <el-icon><Document /></el-icon>
              {{ stage.problems.length }} 道练习
            </span>
            <span v-if="stage.completedAt">
              <el-icon><Clock /></el-icon>
              完成于 {{ formatDateTime(stage.completedAt) }}
            </span>
          </div>
        </div>

        <div class="stage-detail-hero__action">
          <span class="stage-detail-hero__hint">
            {{ stage.completed ? '阶段进度已计入所属路线。' : '学完本阶段后，手动确认完成。' }}
          </span>
          <el-button
            :type="stage.completed ? 'default' : 'primary'"
            :icon="CircleCheck"
            :loading="stageSaving"
            size="large"
            @click="toggleStageCompletion"
          >
            {{ stage.completed ? '取消完成' : '标记为已完成' }}
          </el-button>
        </div>
      </section>

      <section class="learning-panel">
        <div class="learning-panel__head">
          <div>
            <h2>阶段内容</h2>
            <p>先阅读阶段说明，再结合课程与题目完成练习。</p>
          </div>
          <span class="learning-panel__eyebrow">STAGE NOTES</span>
        </div>
        <div
          v-if="contentHtml"
          class="markdown-body stage-content"
          v-html="contentHtml"
        ></div>
        <p v-else class="learning-panel__empty">本阶段暂无补充说明，直接查看课程与练习。</p>
      </section>

      <section class="learning-panel">
        <div class="learning-panel__head">
          <div>
            <h2>配套课程</h2>
            <p>课程在新窗口打开，观看课程不会自动改变阶段进度。</p>
          </div>
          <span class="learning-count">{{ stage.courses.length }} 节</span>
        </div>

        <p v-if="stage.courses.length === 0" class="learning-panel__empty">
          本阶段暂时没有配套课程。
        </p>
        <div v-else class="course-list">
          <template v-for="(course, index) in stage.courses" :key="course.id">
            <a
              v-if="course.url"
              :href="course.url"
              class="course-card"
              target="_blank"
              rel="noopener noreferrer"
            >
              <span class="course-card__index">{{ String(index + 1).padStart(2, '0') }}</span>
              <div class="course-card__body">
                <h3>{{ course.title }}</h3>
                <div class="course-card__meta">
                  <span>课程来源 · {{ course.source || '未标注' }}</span>
                  <span>课程时长 · {{ formatDuration(course.durationSeconds) }}</span>
                </div>
              </div>
              <span class="course-card__action">
                去学习
                <el-icon><TopRight /></el-icon>
              </span>
            </a>
            <div v-else class="course-card">
              <span class="course-card__index">{{ String(index + 1).padStart(2, '0') }}</span>
              <div class="course-card__body">
                <h3>{{ course.title }}</h3>
                <div class="course-card__meta">
                  <span>课程来源 · {{ course.source || '未标注' }}</span>
                  <span>课程时长 · {{ formatDuration(course.durationSeconds) }}</span>
                </div>
              </div>
              <span class="course-card__action">链接待补充</span>
            </div>
          </template>
        </div>
      </section>

      <section class="learning-panel">
        <div class="learning-panel__head">
          <div>
            <h2>阶段练习</h2>
            <p>站内题目按类型准备；外链题完成后回到这里手动确认状态。</p>
          </div>
          <span class="learning-count">{{ stage.problems.length }} 道</span>
        </div>

        <p v-if="stage.problems.length === 0" class="learning-panel__empty">
          本阶段暂时没有配套练习。
        </p>
        <div v-else class="problem-list">
          <div
            v-for="problem in stage.problems"
            :key="problem.problem.id"
            class="problem-row"
            :class="{ 'is-external': problem.problem.type === 'external' }"
          >
            <span class="problem-row__type">
              {{ problemTypeLabel(problem.problem.type) }}
            </span>

            <div class="problem-row__body">
              <h3>{{ problem.problem.title }}</h3>
              <div class="problem-row__meta">
                <span v-for="(item, metaIndex) in problemMeta(problem)" :key="metaIndex">
                  {{ item }}
                </span>
              </div>
            </div>

            <div v-if="problem.problem.type === 'external'" class="problem-row__actions">
              <a
                v-if="problem.problem.externalUrl"
                :href="problem.problem.externalUrl"
                class="problem-link"
                target="_blank"
                rel="noopener noreferrer"
              >
                <el-icon><Link /></el-icon>
                访问题目
              </a>
              <button
                type="button"
                class="completion-toggle"
                :class="{ 'is-completed': problem.externalCompleted }"
                :disabled="externalSavingId === problem.problem.id"
                :aria-pressed="problem.externalCompleted === true"
                @click="toggleExternalCompletion(problem)"
              >
                <el-icon v-if="problem.externalCompleted"><Check /></el-icon>
                <span>{{ problem.externalCompleted ? '已完成' : '标记完成' }}</span>
              </button>
            </div>
            <span v-else class="problem-row__local">站内练习</span>
          </div>
        </div>
      </section>

      <div class="learning-back-row">
        <RouterLink
          class="learning-back-link"
          :to="{ name: 'learning-route-detail', params: { routeId: stage.routeId } }"
        >
          <el-icon><ArrowLeft /></el-icon>
          返回 {{ stage.routeName }}
        </RouterLink>
      </div>
    </template>
  </div>
</template>
