<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Refresh, Search } from '@element-plus/icons-vue'

import type { ProblemType } from '@/api/problem'
import { fetchWrongBook, removeWrongBook, type WrongBookItem } from '@/api/record'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatDateTime } from '@/utils/format'

const PAGE_SIZE = 10

const TYPE_LABELS: Record<string, string> = {
  oj: 'OJ 编程',
  objective: '客观题',
  short_answer: '简答题'
}
const DIFFICULTY_LABELS: Record<string, string> = {
  entry: '入门',
  basic: '基础',
  advanced: '进阶'
}

const router = useRouter()

const items = ref<WrongBookItem[]>([])
const page = ref(1)
const total = ref(0)
const totalPages = ref(0)
const loading = ref(true)
const errorMessage = ref('')
const removingId = ref('')
const filters = reactive({
  problemType: '' as ProblemType | '',
  keyword: ''
})

function typeLabel(type: string): string {
  return TYPE_LABELS[type] ?? type
}

function difficultyLabel(difficulty: string): string {
  return DIFFICULTY_LABELS[difficulty] ?? difficulty
}

function difficultyTagType(difficulty: string): 'success' | 'warning' | 'danger' | 'info' {
  if (difficulty === 'entry') {
    return 'success'
  }
  if (difficulty === 'advanced') {
    return 'danger'
  }
  if (difficulty === 'basic') {
    return 'warning'
  }
  return 'info'
}

async function loadItems() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await fetchWrongBook({
      problemType: filters.problemType || undefined,
      keyword: filters.keyword.trim() || undefined,
      page: page.value,
      pageSize: PAGE_SIZE
    })
    items.value = result.items
    total.value = result.total
    totalPages.value = result.totalPages
  } catch (error) {
    items.value = []
    total.value = 0
    totalPages.value = 0
    errorMessage.value = resolveApiErrorMessage(error, '错题本加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  void loadItems()
}

watch(
  () => filters.problemType,
  () => search()
)

function changePage(nextPage: number) {
  page.value = nextPage
  void loadItems()
}

function resetFilters() {
  filters.problemType = ''
  filters.keyword = ''
  page.value = 1
  void loadItems()
}

function openProblem(problemId: string) {
  void router.push({ name: 'problem-detail', params: { problemId } })
}

async function handleRemove(item: WrongBookItem) {
  try {
    await ElMessageBox.confirm(
      `确认把「${item.problem.title}」移出错题本？再次答错时它会自动回到这里。`,
      '移出错题本',
      { confirmButtonText: '移出', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  removingId.value = item.id
  try {
    await removeWrongBook(item.id)
    ElMessage.success('已移出错题本')
    if (items.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await loadItems()
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '移除错题失败，请稍后重试'))
  } finally {
    removingId.value = ''
  }
}

onMounted(loadItems)
</script>

<template>
  <div class="wrongbook-page">
    <section class="wrongbook-hero">
      <div class="wrongbook-hero__copy">
        <span class="learning-eyebrow">REVIEW QUEUE</span>
        <h1>把错误收拢，<br /><span>一道一道重新拿下。</span></h1>
        <p>
          OJ 非 AC、客观题错误答案与简答题未通过的记录会自动聚合在这里。
          按最近出错时间排序，优先处理刚刚暴露的问题。
        </p>
        <div class="wrongbook-hero__actions">
          <el-button type="primary" @click="router.push('/problems')">去题库练习</el-button>
          <el-button @click="router.push('/records')">查看学习记录</el-button>
        </div>
      </div>

      <div class="wrongbook-hero__panel">
        <span class="wrongbook-hero__label">待复习错题</span>
        <strong class="wrongbook-hero__count">{{ loading ? '—' : total }}</strong>
        <span class="wrongbook-hero__unit">道</span>
        <p>同一道题累计出错次数；再次答错会重新激活已移除的记录。</p>
      </div>
    </section>

    <section class="wrongbook-section">
      <header class="wrongbook-section__head">
        <div>
          <h2>错题列表</h2>
          <p>只展示未移除的记录；不包含提交代码、测试点与参考答案。</p>
        </div>
        <div class="wrongbook-filters">
          <el-select v-model="filters.problemType" class="wrongbook-filter" placeholder="全部题型">
            <el-option label="全部题型" value="" />
            <el-option label="OJ 编程" value="oj" />
            <el-option label="客观题" value="objective" />
            <el-option label="简答题" value="short_answer" />
          </el-select>
          <el-input
            v-model="filters.keyword"
            class="wrongbook-keyword"
            clearable
            placeholder="按题目标题搜索"
            maxlength="100"
            @keyup.enter="search"
            @clear="search"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" plain :icon="Search" @click="search">搜索</el-button>
          <el-button :icon="Refresh" :loading="loading" @click="loadItems">刷新</el-button>
        </div>
      </header>

      <div v-if="loading" class="records-state-card">
        <el-skeleton :rows="6" animated />
      </div>

      <div v-else-if="errorMessage" class="records-state-card">
        <span class="records-state-card__icon records-state-card__icon--danger">!</span>
        <h3>错题本暂时没有加载出来</h3>
        <p>{{ errorMessage }}</p>
        <el-button :icon="Refresh" @click="loadItems">重新加载</el-button>
      </div>

      <div v-else-if="items.length === 0" class="records-state-card">
        <span class="records-state-card__icon">◌</span>
        <h3>{{ filters.problemType || filters.keyword ? '没有符合条件的错题' : '错题本是空的' }}</h3>
        <p>
          {{
            filters.problemType || filters.keyword
              ? '换一个题型或关键词试试。'
              : '开始练习后，答错的题目会自动进入这里。'
          }}
        </p>
        <el-button v-if="filters.problemType || filters.keyword" @click="resetFilters">清除筛选</el-button>
        <el-button v-else type="primary" @click="router.push('/problems')">去题库练习</el-button>
      </div>

      <template v-else>
        <div class="wrongbook-list">
          <article v-for="item in items" :key="item.id" class="wrongbook-card">
            <div class="wrongbook-card__main">
              <div class="wrongbook-card__title">
                <button type="button" class="wrongbook-card__link" @click="openProblem(item.problem.id)">
                  {{ item.problem.title }}
                  <el-icon><ArrowRight /></el-icon>
                </button>
                <el-tag effect="plain" size="small" class="wrongbook-card__type">
                  {{ typeLabel(item.problem.type) }}
                </el-tag>
                <el-tag
                  :type="difficultyTagType(item.problem.difficulty)"
                  effect="plain"
                  size="small"
                >
                  {{ difficultyLabel(item.problem.difficulty) }}
                </el-tag>
              </div>
              <p class="wrongbook-card__error">
                最近错误
                <strong>{{ item.latestError }}</strong>
                <span v-if="item.latestScore != null">· 最近得分 {{ item.latestScore }}</span>
              </p>
            </div>

            <div class="wrongbook-card__meta">
              <div>
                <span>累计错误</span>
                <strong>{{ item.errorCount }} 次</strong>
              </div>
              <div>
                <span>最近出错</span>
                <strong>{{ formatDateTime(item.lastErrorAt) }}</strong>
              </div>
            </div>

            <div class="wrongbook-card__actions">
              <el-button size="small" type="primary" plain @click="openProblem(item.problem.id)">
                重新练习
              </el-button>
              <el-button
                size="small"
                text
                type="danger"
                :loading="removingId === item.id"
                :disabled="Boolean(removingId)"
                @click="handleRemove(item)"
              >
                移出
              </el-button>
            </div>
          </article>
        </div>

        <div v-if="totalPages > 1" class="records-pagination">
          <el-pagination
            background
            layout="prev, pager, next"
            :current-page="page"
            :page-size="PAGE_SIZE"
            :total="total"
            @current-change="changePage"
          />
        </div>
      </template>
    </section>
  </div>
</template>
