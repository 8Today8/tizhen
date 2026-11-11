<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, Refresh } from '@element-plus/icons-vue'

import { fetchLearningRoutes, type RouteSummary } from '@/api/learning'
import { resolveApiErrorMessage } from '@/utils/apiError'

const PAGE_SIZE = 9

const routes = ref<RouteSummary[]>([])
const page = ref(1)
const total = ref(0)
const totalPages = ref(0)
const loading = ref(true)
const errorMessage = ref('')

const completedOnPage = computed(() =>
  routes.value.reduce((sum, route) => sum + route.completedStageCount, 0)
)

async function loadRoutes() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await fetchLearningRoutes({ page: page.value, pageSize: PAGE_SIZE })
    routes.value = result.items
    total.value = result.total
    totalPages.value = result.totalPages
  } catch (error) {
    errorMessage.value = resolveApiErrorMessage(error, '学习路线加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function handlePageChange(nextPage: number) {
  page.value = nextPage
  void loadRoutes()
}

function routeTone(index: number): string {
  return `route-card--tone-${(index % 4) + 1}`
}

onMounted(loadRoutes)
</script>

<template>
  <div class="learning-page">
    <section class="learning-hero">
      <div class="learning-hero__copy">
        <span class="learning-eyebrow">LEARNING ROADMAP</span>
        <h1>沿着路线推进，<br /><span>让每次练习都有上下文。</span></h1>
        <p>
          先理解阶段内容，再通过课程与配套题目巩固。完成一个阶段，就向前推进一格，
          学习节奏和进度都留在同一条路线里。
        </p>
      </div>

      <div class="learning-hero__panel" aria-label="学习闭环">
        <span class="learning-hero__panel-title">学习闭环</span>
        <ol class="learning-flow">
          <li><span>01</span>选择路线</li>
          <li><span>02</span>研读阶段内容</li>
          <li><span>03</span>观看课程与练习</li>
          <li><span>04</span>标记阶段完成</li>
        </ol>
        <p class="learning-hero__note">外链题需在站外完成，回到阶段页手动确认。</p>
      </div>
    </section>

    <section class="learning-section">
      <div class="learning-section__head">
        <div>
          <h2>全部路线</h2>
          <p>按运营编排的顺序展示已发布路线，进度按已完成的阶段数计算。</p>
        </div>
        <span v-if="!loading && !errorMessage" class="learning-count">
          共 {{ total }} 条 · 本页完成 {{ completedOnPage }} 个阶段
        </span>
      </div>

      <div v-if="loading" class="route-grid" aria-busy="true" aria-label="正在加载学习路线">
        <div v-for="index in 6" :key="index" class="route-skeleton">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="image" class="route-skeleton__cover" />
              <div class="route-skeleton__body">
                <el-skeleton-item variant="h3" style="width: 58%" />
                <el-skeleton-item variant="text" />
                <el-skeleton-item variant="text" style="width: 76%" />
                <el-skeleton-item variant="text" style="width: 42%" />
              </div>
            </template>
          </el-skeleton>
        </div>
      </div>

      <div v-else-if="errorMessage" class="learning-state">
        <span class="learning-state__icon learning-state__icon--danger">!</span>
        <h3>路线暂时没有加载出来</h3>
        <p>{{ errorMessage }}</p>
        <el-button :icon="Refresh" @click="loadRoutes">重新加载</el-button>
      </div>

      <div v-else-if="routes.length === 0" class="learning-state">
        <span class="learning-state__icon">◌</span>
        <h3>暂无可学习的路线</h3>
        <p>运营发布学习路线后，这里会展示完整的阶段与进度。</p>
      </div>

      <template v-else>
        <div class="route-grid">
          <RouterLink
            v-for="(route, index) in routes"
            :key="route.id"
            :to="{ name: 'learning-route-detail', params: { routeId: route.id } }"
            class="route-card"
            :class="routeTone(index)"
          >
            <div class="route-card__cover">
              <img
                v-if="route.coverUrl"
                :src="route.coverUrl"
                :alt="`${route.name}封面`"
                loading="lazy"
              />
              <div v-else class="route-card__cover-fallback">
                <span class="route-card__index">{{ String(index + 1).padStart(2, '0') }}</span>
                <span class="route-card__cover-label">ROUTE</span>
              </div>
              <span class="route-card__percent">{{ route.progressPercent }}%</span>
            </div>

            <div class="route-card__body">
              <div class="route-card__title-row">
                <h3>{{ route.name }}</h3>
                <el-icon class="route-card__arrow"><ArrowRight /></el-icon>
              </div>
              <p class="route-card__description">
                {{ route.description || '沿阶段逐步建立完整的算法知识结构。' }}
              </p>

              <div class="route-card__progress">
                <el-progress
                  :percentage="route.progressPercent"
                  :stroke-width="7"
                  :show-text="false"
                />
              </div>

              <div class="route-card__meta">
                <span>{{ route.stageCount }} 个阶段</span>
                <span>{{ route.completedStageCount }} 个已完成</span>
              </div>
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
