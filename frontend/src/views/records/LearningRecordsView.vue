<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, CircleCheck, Refresh } from '@element-plus/icons-vue'

import { fetchPlayerStats, type PlayerStats } from '@/api/account'
import {
  fetchLearningRoute,
  fetchLearningRoutes,
  type RouteSummary,
  type StageSummary
} from '@/api/learning'
import {
  fetchSubmissions,
  type ProblemType,
  type SubmissionListItem,
  type SubmissionStatus
} from '@/api/problem'
import type { PageResult } from '@/types/api'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatDateTime } from '@/utils/format'
import {
  languageLabel,
  statusTagType,
  submissionStatusLabel,
  verdictLabel,
  verdictTagType
} from '@/utils/problemLabels'

interface RouteProgressEntry {
  summary: RouteSummary
  stages: StageSummary[]
  detailError: boolean
}

const PAGE_SIZE = 10

const TYPE_LABELS: Record<string, string> = {
  oj: 'OJ 编程',
  objective: '客观题',
  short_answer: '简答题',
  external: '外链题'
}

const router = useRouter()

const stats = ref<PlayerStats | null>(null)
const statsLoading = ref(true)

const routeProgress = ref<RouteProgressEntry[]>([])
const progressLoading = ref(true)
const progressError = ref('')

const filters = reactive({
  problemType: '' as ProblemType | '',
  status: '' as SubmissionStatus | ''
})
const submissions = ref<PageResult<SubmissionListItem> | null>(null)
const submissionsLoading = ref(true)
const submissionsError = ref('')
const page = ref(1)

const wrongBookCount = computed(() => stats.value?.wrongBook.activeCount ?? 0)
const totalStages = computed(() => stats.value?.learning.totalStages ?? 0)
const completedStages = computed(() => stats.value?.learning.completedStages ?? 0)
const overallPercent = computed(() => {
  if (totalStages.value <= 0) {
    return 0
  }
  return Math.round((completedStages.value / totalStages.value) * 100)
})

function displayTime(value: string | null): string {
  return value ? formatDateTime(value) : '—'
}

function typeLabel(type: string): string {
  return TYPE_LABELS[type] ?? type
}

function completionText(stage: StageSummary): string {
  return stage.completed
    ? stage.completedAt
      ? `完成于 ${displayTime(stage.completedAt)}`
      : '已完成'
    : '待完成'
}

async function loadStats() {
  statsLoading.value = true
  try {
    stats.value = await fetchPlayerStats()
  } catch {
    stats.value = null
  } finally {
    statsLoading.value = false
  }
}

async function loadProgress() {
  progressLoading.value = true
  progressError.value = ''
  try {
    const routes = await fetchLearningRoutes({ page: 1, pageSize: 100 })
    const details = await Promise.allSettled(
      routes.items.map((route) => fetchLearningRoute(route.id))
    )
    routeProgress.value = routes.items.map((summary, index) => {
      const detail = details[index]
      return {
        summary,
        stages: detail.status === 'fulfilled' ? detail.value.stages : [],
        detailError: detail.status === 'rejected'
      }
    })
  } catch (error) {
    routeProgress.value = []
    progressError.value = resolveApiErrorMessage(error, '阶段进度加载失败，请稍后重试')
  } finally {
    progressLoading.value = false
  }
}

async function loadSubmissions() {
  submissionsLoading.value = true
  submissionsError.value = ''
  try {
    submissions.value = await fetchSubmissions({
      problemType: filters.problemType || undefined,
      status: filters.status || undefined,
      page: page.value,
      pageSize: PAGE_SIZE
    })
  } catch (error) {
    submissions.value = null
    submissionsError.value = resolveApiErrorMessage(error, '提交历史加载失败，请稍后重试')
  } finally {
    submissionsLoading.value = false
  }
}

watch(
  () => [filters.problemType, filters.status],
  () => {
    page.value = 1
    void loadSubmissions()
  }
)

function changePage(nextPage: number) {
  page.value = nextPage
  void loadSubmissions()
}

function openProblem(problemId: string) {
  void router.push({ name: 'problem-detail', params: { problemId } })
}

onMounted(() => {
  void loadStats()
  void loadProgress()
  void loadSubmissions()
})
</script>

<template>
  <div class="records-page">
    <section class="records-hero">
      <div class="records-hero__copy">
        <span class="learning-eyebrow">TRAINING LOG</span>
        <h1>把每一次推进，<br /><span>沉淀成可回看的记录。</span></h1>
        <p>
          阶段完成情况、提交与判题结果集中在这里。继续沿着路线推进，
          或回到错题本复盘最近的失分点。
        </p>
        <div class="records-hero__actions">
          <RouterLink class="records-primary-link" to="/learning">
            继续学习路线
            <el-icon><ArrowRight /></el-icon>
          </RouterLink>
          <RouterLink class="records-secondary-link" to="/wrongbook">
            错题本 · {{ wrongBookCount }} 题
          </RouterLink>
        </div>
      </div>

      <div class="records-hero__panel">
        <div class="records-hero__ring">
          <strong>{{ overallPercent }}<small>%</small></strong>
          <span>阶段完成度</span>
        </div>
        <dl class="records-hero__stats">
          <div>
            <dt>已完成阶段</dt>
            <dd>
              {{ statsLoading ? '—' : completedStages }}<small v-if="!statsLoading">/{{ totalStages }}</small>
            </dd>
          </div>
          <div>
            <dt>OJ 首次 AC</dt>
            <dd>{{ statsLoading ? '—' : stats?.submissions.ojAcCount ?? 0 }}</dd>
          </div>
          <div>
            <dt>提交 / 作答</dt>
            <dd>{{ statsLoading ? '—' : stats?.submissions.total ?? 0 }}</dd>
          </div>
        </dl>
        <p class="records-hero__note">
          进度按当前仍发布的学习内容统计；被下架或删除的阶段不计入总数。
        </p>
      </div>
    </section>

    <section class="records-section">
      <header class="records-section__head">
        <div>
          <h2>阶段进度</h2>
          <p>按路线查看每个阶段的完成状态与配套内容数量。</p>
        </div>
        <el-button :icon="Refresh" :loading="progressLoading" @click="loadProgress">刷新</el-button>
      </header>

      <div v-if="progressLoading" class="records-state-card">
        <el-skeleton :rows="6" animated />
      </div>

      <div v-else-if="progressError" class="records-state-card">
        <span class="records-state-card__icon records-state-card__icon--danger">!</span>
        <h3>阶段进度暂时没有加载出来</h3>
        <p>{{ progressError }}</p>
        <el-button :icon="Refresh" @click="loadProgress">重新加载</el-button>
      </div>

      <div v-else-if="routeProgress.length === 0" class="records-state-card">
        <span class="records-state-card__icon">◌</span>
        <h3>暂无可展示的学习路线</h3>
        <p>运营发布路线后，阶段进度会在这里按路线展示。</p>
      </div>

      <div v-else class="route-progress-list">
        <article v-for="entry in routeProgress" :key="entry.summary.id" class="route-progress-card">
          <header class="route-progress-card__head">
            <div class="route-progress-card__title">
              <RouterLink :to="{ name: 'learning-route-detail', params: { routeId: entry.summary.id } }">
                {{ entry.summary.name }}
                <el-icon><ArrowRight /></el-icon>
              </RouterLink>
              <p>{{ entry.summary.description || '沿阶段逐步推进学习路线。' }}</p>
            </div>
            <div class="route-progress-card__progress">
              <strong>{{ entry.summary.completedStageCount }}<small>/{{ entry.summary.stageCount }}</small></strong>
              <el-progress
                :percentage="entry.summary.progressPercent"
                :stroke-width="7"
                :show-text="false"
              />
            </div>
          </header>

          <div v-if="entry.stages.length" class="stage-track">
            <RouterLink
              v-for="(stage, stageIndex) in entry.stages"
              :key="stage.id"
              :to="{ name: 'learning-stage-detail', params: { stageId: stage.id } }"
              class="stage-chip"
              :class="{ 'stage-chip--done': stage.completed }"
            >
              <span class="stage-chip__index">{{ String(stageIndex + 1).padStart(2, '0') }}</span>
              <span class="stage-chip__body">
                <strong>{{ stage.name }}</strong>
                <small>{{ completionText(stage) }}</small>
                <small>{{ stage.courseCount }} 课程 · {{ stage.problemCount }} 道题</small>
              </span>
              <el-icon v-if="stage.completed" class="stage-chip__check"><CircleCheck /></el-icon>
            </RouterLink>
          </div>

          <p v-else class="route-progress-card__fallback">
            <template v-if="entry.detailError">阶段明细加载失败，请进入路线详情查看。</template>
            <template v-else>该路线暂无已发布阶段。</template>
          </p>
        </article>
      </div>
    </section>

    <section class="records-section">
      <header class="records-section__head">
        <div>
          <h2>提交历史</h2>
          <p>包含普通练习与对战提交；结果由后端判题或 AI 评分后写入。</p>
        </div>
        <div class="records-filters">
          <el-select v-model="filters.problemType" size="small" placeholder="全部题型" class="records-filter">
            <el-option label="全部题型" value="" />
            <el-option label="OJ 编程" value="oj" />
            <el-option label="客观题" value="objective" />
            <el-option label="简答题" value="short_answer" />
          </el-select>
          <el-select v-model="filters.status" size="small" placeholder="全部状态" class="records-filter">
            <el-option label="全部状态" value="" />
            <el-option label="等待判题" value="pending" />
            <el-option label="判题中" value="judging" />
            <el-option label="评分中" value="grading" />
            <el-option label="已完成" value="finished" />
            <el-option label="处理失败" value="failed" />
          </el-select>
          <el-button :icon="Refresh" :loading="submissionsLoading" @click="loadSubmissions">刷新</el-button>
        </div>
      </header>

      <div v-if="submissionsLoading && !submissions" class="records-state-card">
        <el-skeleton :rows="8" animated />
      </div>

      <div v-else-if="submissionsError" class="records-state-card">
        <span class="records-state-card__icon records-state-card__icon--danger">!</span>
        <h3>提交历史暂时没有加载出来</h3>
        <p>{{ submissionsError }}</p>
        <el-button :icon="Refresh" @click="loadSubmissions">重新加载</el-button>
      </div>

      <div v-else-if="!submissions || submissions.items.length === 0" class="records-state-card">
        <span class="records-state-card__icon">◌</span>
        <h3>{{ filters.problemType || filters.status ? '没有符合条件的提交' : '还没有提交记录' }}</h3>
        <p>完成一次练习或对战提交后，判题结果会出现在这里。</p>
        <el-button
          v-if="!filters.problemType && !filters.status"
          type="primary"
          @click="router.push('/problems')"
        >
          去题库练习
        </el-button>
      </div>

      <div v-else class="records-table-card">
        <div class="records-table-wrap">
          <table class="records-table">
            <thead>
              <tr>
                <th>题目</th>
                <th>题型</th>
                <th>状态</th>
                <th>结果</th>
                <th class="records-col-num">分数</th>
                <th>提交时间</th>
                <th class="records-col-action">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in submissions.items" :key="item.id" @click="openProblem(item.problem.id)">
                <td>
                  <div class="records-problem-cell">
                    <strong>{{ item.problem.title }}</strong>
                    <span v-if="item.battleId" class="records-battle-tag">对战</span>
                  </div>
                </td>
                <td>{{ typeLabel(item.problem.type) }}</td>
                <td>
                  <el-tag :type="statusTagType(item.status)" effect="plain" size="small">
                    {{ submissionStatusLabel(item.status) }}
                  </el-tag>
                </td>
                <td>
                  <el-tag v-if="item.verdict" :type="verdictTagType(item.verdict)" effect="plain" size="small">
                    {{ verdictLabel(item.verdict) }}
                  </el-tag>
                  <span v-else class="records-muted">—</span>
                </td>
                <td class="records-col-num">
                  {{ item.score == null ? '—' : item.score }}
                </td>
                <td>
                  <div class="records-time">
                    <span>{{ displayTime(item.createdAt) }}</span>
                    <small v-if="item.judgedAt">判于 {{ displayTime(item.judgedAt) }}</small>
                    <small v-if="item.language" class="records-mono">{{ languageLabel(item.language) }}</small>
                  </div>
                </td>
                <td class="records-col-action">
                  <el-button
                    size="small"
                    text
                    type="primary"
                    @click.stop="openProblem(item.problem.id)"
                  >
                    查看题目
                  </el-button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="(submissions.totalPages ?? 0) > 1" class="records-pagination">
          <el-pagination
            background
            layout="prev, pager, next"
            :current-page="page"
            :page-size="PAGE_SIZE"
            :total="submissions.total"
            @current-change="changePage"
          />
        </div>
      </div>
    </section>
  </div>
</template>
