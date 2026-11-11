<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  ArrowLeft,
  ArrowRight,
  Check,
  Clock,
  Document,
  Refresh,
  VideoPlay
} from '@element-plus/icons-vue'

import { fetchLearningRoute, type RouteDetail, type StageSummary } from '@/api/learning'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const detail = ref<RouteDetail | null>(null)
const loading = ref(true)
const errorMessage = ref('')

const completedStages = computed(
  () => detail.value?.stages.filter((stage) => stage.completed).length ?? 0
)

function coursePreview(stage: StageSummary): string {
  if (stage.courses.length === 0) {
    return '暂无课程，直接进入阶段查看练习内容。'
  }
  const names = stage.courses.slice(0, 3).map((course) => course.title)
  return stage.courses.length > 3 ? `${names.join(' · ')} 等 ${stage.courses.length} 节` : names.join(' · ')
}

async function loadRoute() {
  const routeId = String(route.params.routeId ?? '')
  loading.value = true
  errorMessage.value = ''
  detail.value = null
  try {
    detail.value = await fetchLearningRoute(routeId)
  } catch (error) {
    errorMessage.value = resolveApiErrorMessage(error, '路线详情加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

watch(
  () => route.params.routeId,
  () => {
    void loadRoute()
  },
  { immediate: true }
)
</script>

<template>
  <div class="learning-page">
    <nav class="learning-breadcrumb" aria-label="面包屑">
      <RouterLink to="/learning">学习路线</RouterLink>
      <span>/</span>
      <span aria-current="page">{{ detail?.name ?? '路线详情' }}</span>
    </nav>

    <div v-if="loading" class="learning-detail-skeleton">
      <el-skeleton animated :rows="4" />
      <div class="learning-detail-skeleton__stages">
        <el-skeleton v-for="index in 3" :key="index" animated :rows="2" />
      </div>
    </div>

    <div v-else-if="errorMessage" class="learning-state">
      <span class="learning-state__icon learning-state__icon--danger">!</span>
      <h3>这条路线暂时无法打开</h3>
      <p>{{ errorMessage }}</p>
      <div class="learning-state__actions">
        <el-button :icon="Refresh" @click="loadRoute">重新加载</el-button>
        <RouterLink class="learning-text-link" to="/learning">返回路线列表</RouterLink>
      </div>
    </div>

    <template v-else-if="detail">
      <section class="route-detail-hero">
        <div class="route-detail-hero__main">
          <span class="learning-eyebrow">ROUTE {{ String(detail.sortOrder).padStart(2, '0') }}</span>
          <h1>{{ detail.name }}</h1>
          <p>{{ detail.description || '按阶段推进学习内容，完成每个阶段后手动标记进度。' }}</p>

          <div class="route-detail-hero__stats">
            <div>
              <span>阶段总数</span>
              <strong>{{ detail.stageCount }}</strong>
            </div>
            <div>
              <span>已完成</span>
              <strong>{{ completedStages }}</strong>
            </div>
            <div>
              <span>总进度</span>
              <strong>{{ detail.progressPercent }}%</strong>
            </div>
          </div>

          <div class="route-detail-hero__progress">
            <el-progress
              :percentage="detail.progressPercent"
              :stroke-width="10"
              :show-text="false"
            />
            <span>完成 {{ completedStages }} / {{ detail.stageCount }} 个阶段</span>
          </div>
        </div>

        <div v-if="detail.coverUrl" class="route-detail-hero__cover">
          <img :src="detail.coverUrl" :alt="`${detail.name}封面`" />
        </div>
        <div v-else class="route-detail-hero__cover route-detail-hero__cover--fallback">
          <span>{{ detail.progressPercent }}</span>
          <small>PERCENT COMPLETE</small>
        </div>
      </section>

      <section class="learning-section">
        <div class="learning-section__head">
          <div>
            <h2>学习阶段</h2>
            <p>阶段按顺序解锁学习，进入阶段可查看内容、课程与配套题目。</p>
          </div>
          <span class="learning-count">{{ detail.stageCount }} 个阶段</span>
        </div>

        <div v-if="detail.stages.length === 0" class="learning-state learning-state--compact">
          <span class="learning-state__icon">◌</span>
          <h3>路线还在准备中</h3>
          <p>当前没有已发布的阶段，稍后再来看看。</p>
        </div>

        <ol v-else class="stage-list">
          <li v-for="(stage, index) in detail.stages" :key="stage.id">
            <RouterLink
              :to="{ name: 'learning-stage-detail', params: { stageId: stage.id } }"
              class="stage-row"
              :class="{ 'is-completed': stage.completed }"
            >
              <div class="stage-row__rail">
                <span class="stage-row__marker">
                  <el-icon v-if="stage.completed"><Check /></el-icon>
                  <template v-else>{{ String(index + 1).padStart(2, '0') }}</template>
                </span>
                <span v-if="index < detail.stages.length - 1" class="stage-row__connector"></span>
              </div>

              <div class="stage-row__body">
                <div class="stage-row__head">
                  <h3>{{ stage.name }}</h3>
                  <el-tag v-if="stage.completed" type="success" effect="plain" round size="small">
                    已完成
                  </el-tag>
                  <el-tag v-else effect="plain" round size="small">进行中</el-tag>
                </div>

                <p class="stage-row__courses">{{ coursePreview(stage) }}</p>

                <div class="stage-row__meta">
                  <span>
                    <el-icon><VideoPlay /></el-icon>
                    {{ stage.courseCount }} 节课程
                  </span>
                  <span>
                    <el-icon><Document /></el-icon>
                    {{ stage.problemCount }} 道练习
                  </span>
                  <span v-if="stage.completed && stage.completedAt">
                    <el-icon><Clock /></el-icon>
                    完成于 {{ formatDateTime(stage.completedAt) }}
                  </span>
                </div>
              </div>

              <el-icon class="stage-row__arrow"><ArrowRight /></el-icon>
            </RouterLink>
          </li>
        </ol>
      </section>

      <div class="learning-back-row">
        <RouterLink class="learning-back-link" to="/learning">
          <el-icon><ArrowLeft /></el-icon>
          返回全部路线
        </RouterLink>
      </div>
    </template>
  </div>
</template>
