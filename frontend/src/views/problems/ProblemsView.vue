<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Aim, ArrowRight, Check, Refresh, Search } from '@element-plus/icons-vue'

import {
  fetchProblems,
  fetchProblemTags,
  fetchRandomProblem,
  type ObjectiveType,
  type ProblemSummary,
  type ProblemTagOption,
  type ProblemType
} from '@/api/problem'
import { ApiError } from '@/api/http'
import {
  objectiveTypeLabel,
  problemDifficultyLabel,
  problemTypeLabel
} from '@/utils/adminLabels'
import { resolveApiErrorMessage } from '@/utils/apiError'

const PAGE_SIZE = 12

const router = useRouter()

const filters = reactive({
  type: '' as ProblemType | '',
  objectiveType: '' as ObjectiveType | '',
  difficulty: '',
  source: '',
  tag: '',
  keyword: ''
})

const problems = ref<ProblemSummary[]>([])
const tags = ref<ProblemTagOption[]>([])
const page = ref(1)
const total = ref(0)
const totalPages = ref(0)
const loading = ref(true)
const errorMessage = ref('')
const randomLoading = ref(false)

const activeFilterCount = computed(() => {
  return [
    filters.type,
    filters.objectiveType,
    filters.difficulty,
    filters.source,
    filters.tag,
    filters.keyword
  ].filter((value) => value !== '').length
})

function buildQuery() {
  return {
    type: filters.type || undefined,
    objectiveType: filters.objectiveType || undefined,
    difficulty: filters.difficulty || undefined,
    source: filters.source.trim() || undefined,
    tag: filters.tag || undefined,
    keyword: filters.keyword.trim() || undefined,
    page: page.value,
    pageSize: PAGE_SIZE
  }
}

async function loadProblems() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await fetchProblems(buildQuery())
    problems.value = result.items
    total.value = result.total
    totalPages.value = result.totalPages
  } catch (error) {
    errorMessage.value = resolveApiErrorMessage(error, '题库加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function loadTags() {
  try {
    const result = await fetchProblemTags()
    tags.value = result.items
  } catch {
    // 标签只作为筛选辅助，失败时不阻塞题目列表。
    tags.value = []
  }
}

function applyFilters() {
  page.value = 1
  void loadProblems()
}

function resetFilters() {
  filters.type = ''
  filters.objectiveType = ''
  filters.difficulty = ''
  filters.source = ''
  filters.tag = ''
  filters.keyword = ''
  page.value = 1
  void loadProblems()
}

function handleTypeChange(value: ProblemType | '') {
  filters.type = value
  if (value !== 'objective') {
    filters.objectiveType = ''
  }
  applyFilters()
}

function toggleTag(value: string) {
  filters.tag = filters.tag === value ? '' : value
  applyFilters()
}

function handlePageChange(nextPage: number) {
  page.value = nextPage
  void loadProblems()
}

async function openRandomProblem() {
  if (randomLoading.value) {
    return
  }
  randomLoading.value = true
  try {
    const problem = await fetchRandomProblem({
      type: filters.type || undefined,
      difficulty: filters.difficulty || undefined,
      tag: filters.tag || undefined
    })
    await router.push({ name: 'problem-detail', params: { problemId: problem.id } })
  } catch (error) {
    if (error instanceof ApiError && error.code === 'PROBLEM_NOT_FOUND') {
      ElMessage.warning('没有符合当前筛选条件的题目')
    } else {
      ElMessage.error(resolveApiErrorMessage(error, '随机抽题失败，请稍后重试'))
    }
  } finally {
    randomLoading.value = false
  }
}

function progressHint(problem: ProblemSummary): string {
  if (problem.type === 'oj') {
    if (problem.solvedByMe) {
      return '已通过'
    }
    return problem.attemptedByMe ? '继续作答' : '尚未作答'
  }
  if (problem.type === 'external') {
    return problem.externalCompleted ? '站外已完成' : '站外练习'
  }
  return problem.attemptedByMe ? '继续作答' : '尚未作答'
}

onMounted(() => {
  void loadProblems()
  void loadTags()
})
</script>

<template>
  <div class="problem-page learning-page">
    <section class="problem-hero">
      <div class="problem-hero__copy">
        <span class="learning-eyebrow">PROBLEM ARCHIVE</span>
        <h1>把知识变成手感，<br /><span>从一道题开始。</span></h1>
        <p>
          站内提供 OJ 编程题、客观题与简答题；外链题保留站外入口。按题型、难度和专题筛选，
          找到当下最合适的练习，提交后结果会留在作答记录里。
        </p>
      </div>

      <div class="problem-hero__panel">
        <span class="problem-hero__panel-label">题库概览</span>
        <strong class="problem-hero__count">{{ total }}</strong>
        <span class="problem-hero__count-label">道已发布题目</span>
        <div class="problem-hero__legend">
          <span><i class="problem-dot problem-dot--oj"></i>OJ 编程</span>
          <span><i class="problem-dot problem-dot--objective"></i>客观题</span>
          <span><i class="problem-dot problem-dot--short"></i>简答题</span>
          <span><i class="problem-dot problem-dot--external"></i>外链题</span>
        </div>
        <el-button
          class="problem-random-button"
          type="primary"
          :icon="Aim"
          :loading="randomLoading"
          @click="openRandomProblem"
        >
          随机抽一题
        </el-button>
      </div>
    </section>

    <section class="problem-filter">
      <div class="problem-filter__row">
        <el-input
          v-model="filters.keyword"
          class="problem-filter__keyword"
          clearable
          placeholder="搜索题目标题"
          :prefix-icon="Search"
          @keyup.enter="applyFilters"
          @clear="applyFilters"
        />
        <el-select
          :model-value="filters.type"
          class="problem-filter__select"
          placeholder="全部题型"
          @change="handleTypeChange"
        >
          <el-option label="全部题型" value="" />
          <el-option label="OJ 编程题" value="oj" />
          <el-option label="客观题" value="objective" />
          <el-option label="简答题" value="short_answer" />
          <el-option label="外链题" value="external" />
        </el-select>
        <el-select
          v-model="filters.objectiveType"
          class="problem-filter__select"
          placeholder="客观题类型"
          :disabled="filters.type !== 'objective'"
          clearable
          @change="applyFilters"
        >
          <el-option label="判断题" value="judge" />
          <el-option label="单选题" value="single" />
          <el-option label="多选题" value="multiple" />
        </el-select>
        <el-select
          v-model="filters.difficulty"
          class="problem-filter__select"
          placeholder="全部难度"
          clearable
          @change="applyFilters"
        >
          <el-option label="入门" value="entry" />
          <el-option label="基础" value="basic" />
          <el-option label="进阶" value="advanced" />
        </el-select>
        <el-input
          v-model="filters.source"
          class="problem-filter__source"
          clearable
          placeholder="来源（精确匹配）"
          @keyup.enter="applyFilters"
          @clear="applyFilters"
        />
        <div class="problem-filter__actions">
          <el-button type="primary" :icon="Search" @click="applyFilters">查询</el-button>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
        </div>
      </div>

      <div v-if="tags.length > 0" class="problem-tag-cloud">
        <span class="problem-tag-cloud__label">专题</span>
        <button
          v-for="tag in tags.slice(0, 16)"
          :key="tag.value"
          type="button"
          class="problem-tag-chip"
          :class="{ 'is-active': filters.tag === tag.value }"
          @click="toggleTag(tag.value)"
        >
          {{ tag.value }}
          <small>{{ tag.count }}</small>
        </button>
      </div>
    </section>

    <section class="problem-section">
      <div class="problem-section__head">
        <div>
          <h2>{{ filters.tag ? `专题 · ${filters.tag}` : '题目列表' }}</h2>
          <p>
            共 {{ total }} 道题
            <template v-if="activeFilterCount > 0"> · 已应用 {{ activeFilterCount }} 个筛选条件</template>
          </p>
        </div>
        <span v-if="!loading && !errorMessage" class="learning-count">
          第 {{ page }} / {{ totalPages || 1 }} 页
        </span>
      </div>

      <div v-if="loading" class="problem-grid" aria-busy="true" aria-label="正在加载题目">
        <div v-for="index in 6" :key="index" class="problem-card-skeleton">
          <el-skeleton animated>
            <template #template>
              <div class="problem-card-skeleton__body">
                <el-skeleton-item variant="text" style="width: 35%" />
                <el-skeleton-item variant="h3" style="width: 72%" />
                <el-skeleton-item variant="text" style="width: 90%" />
                <el-skeleton-item variant="text" style="width: 55%" />
              </div>
            </template>
          </el-skeleton>
        </div>
      </div>

      <div v-else-if="errorMessage" class="learning-state">
        <span class="learning-state__icon learning-state__icon--danger">!</span>
        <h3>题库暂时没有加载出来</h3>
        <p>{{ errorMessage }}</p>
        <el-button :icon="Refresh" @click="loadProblems">重新加载</el-button>
      </div>

      <div v-else-if="problems.length === 0" class="learning-state">
        <span class="learning-state__icon">◌</span>
        <h3>没有找到匹配的题目</h3>
        <p>尝试减少筛选条件，或换一个关键词与专题。</p>
        <el-button :icon="Refresh" @click="resetFilters">清空筛选</el-button>
      </div>

      <template v-else>
        <div class="problem-grid">
          <RouterLink
            v-for="item in problems"
            :key="item.id"
            :to="{ name: 'problem-detail', params: { problemId: item.id } }"
            class="problem-card"
            :class="`problem-card--${item.type}`"
          >
            <div class="problem-card__top">
              <span class="problem-type-pill">{{ problemTypeLabel(item.type) }}</span>
              <span v-if="item.type === 'objective' && item.objectiveType" class="problem-card__subtype">
                {{ objectiveTypeLabel(item.objectiveType) }}
              </span>
              <el-tag size="small" effect="plain" round>
                {{ problemDifficultyLabel(item.difficulty) }}
              </el-tag>
              <span
                v-if="item.type === 'oj' && item.solvedByMe"
                class="problem-card__status problem-card__status--solved"
              >
                <el-icon><Check /></el-icon>
                已通过
              </span>
              <span
                v-else-if="item.type === 'external' && item.externalCompleted"
                class="problem-card__status problem-card__status--solved"
              >
                <el-icon><Check /></el-icon>
                已完成
              </span>
            </div>

            <h3>{{ item.title }}</h3>

            <div v-if="item.tags.length > 0" class="problem-card__tags">
              <span v-for="tag in item.tags.slice(0, 3)" :key="tag">{{ tag }}</span>
              <span v-if="item.tags.length > 3" class="problem-card__tags-more">
                +{{ item.tags.length - 3 }}
              </span>
            </div>
            <p v-else class="problem-card__no-tags">暂无专题标签</p>

            <div class="problem-card__meta">
              <span>{{ progressHint(item) }}</span>
              <span v-if="item.source && item.source !== item.platform">{{ item.source }}</span>
              <span v-if="item.platform">{{ item.platform }}</span>
            </div>

            <div class="problem-card__footer">
              <span>查看题目</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
          </RouterLink>
        </div>

        <div v-if="totalPages > 1" class="learning-pagination">
          <el-pagination
            :current-page="page"
            :page-size="PAGE_SIZE"
            :total="total"
            layout="prev, pager, next"
            background
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </section>
  </div>
</template>
