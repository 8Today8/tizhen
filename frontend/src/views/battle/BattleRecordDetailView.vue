<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Refresh, Trophy } from '@element-plus/icons-vue'

import { fetchBattleRecord, type BattleRecordDetail } from '@/api/battle'
import type { SubmissionListItem } from '@/api/problem'
import { resolveApiErrorMessage } from '@/utils/apiError'
import {
  battleEndReasonLabel,
  battleResultLabel
} from '@/utils/battleLabels'
import { formatDateTime, formatDuration } from '@/utils/format'
import {
  languageLabel,
  statusTagType,
  submissionStatusLabel,
  verdictLabel,
  verdictTagType
} from '@/utils/problemLabels'

const route = useRoute()
const router = useRouter()

const roomId = computed(() => String(route.params.roomId ?? ''))
const record = ref<BattleRecordDetail | null>(null)
const loading = ref(true)
const errorMessage = ref('')

onMounted(loadRecord)

async function loadRecord() {
  loading.value = true
  errorMessage.value = ''
  try {
    record.value = await fetchBattleRecord(roomId.value)
  } catch (error) {
    errorMessage.value = resolveApiErrorMessage(error, '对战记录不存在或当前账号不可见')
  } finally {
    loading.value = false
  }
}

function submissionTime(submission: SubmissionListItem) {
  return formatDateTime(submission.judgedAt || submission.createdAt)
}
</script>

<template>
  <div class="battle-page battle-record-detail-page">
    <button class="battle-back-link" type="button" @click="router.push({ name: 'battle-records' })">
      <el-icon><ArrowLeft /></el-icon>
      对战记录
    </button>

    <div v-if="loading" class="battle-state-card">
      <el-skeleton :rows="7" animated />
    </div>

    <div v-else-if="errorMessage" class="battle-state-card battle-state-card--empty">
      <h2>无法查看记录</h2>
      <p>{{ errorMessage }}</p>
      <div class="battle-state-actions">
        <el-button :icon="Refresh" @click="loadRecord">重试</el-button>
        <el-button type="primary" @click="router.push({ name: 'battle-records' })">
          返回记录列表
        </el-button>
      </div>
    </div>

    <template v-else-if="record">
      <section
        class="battle-result-banner battle-result-banner--detail"
        :class="`is-${record.resultForMe}`"
      >
        <div class="battle-result-banner__icon">
          <el-icon><Trophy /></el-icon>
        </div>
        <div class="battle-result-banner__copy">
          <span class="learning-eyebrow">BATTLE RECORD</span>
          <h1>{{ battleResultLabel(record.resultForMe) }}</h1>
          <p>
            <span>{{ battleEndReasonLabel(record.endReason) }}</span>
            <span>房间 #{{ record.roomCode }}</span>
            <span v-if="record.endedAt">结束于 {{ formatDateTime(record.endedAt) }}</span>
          </p>
        </div>
        <div class="battle-result-banner__actions">
          <el-button type="primary" @click="router.push({ name: 'battle' })">
            返回对战大厅
          </el-button>
        </div>
      </section>

      <section class="battle-record-summary">
        <div class="battle-record-summary__player">
          <el-avatar :size="52" :src="record.opponent?.avatarUrl || undefined">
            {{ record.opponent?.nickname.charAt(0) || '?' }}
          </el-avatar>
          <div>
            <span>对手</span>
            <strong>{{ record.opponent?.nickname || '未知对手' }}</strong>
            <small>{{ record.opponent?.school || '未填写学校' }}</small>
          </div>
        </div>
        <div class="battle-record-summary__stat">
          <span>题目</span>
          <strong>{{ record.problem?.title || '题目已不可用' }}</strong>
        </div>
        <div class="battle-record-summary__stat">
          <span>用时</span>
          <strong>{{ formatDuration(record.durationSeconds) }}</strong>
        </div>
        <div class="battle-record-summary__stat">
          <span>开始时间</span>
          <strong>{{ record.startedAt ? formatDateTime(record.startedAt) : '未开始' }}</strong>
        </div>
      </section>

      <div class="battle-submission-columns">
        <section class="battle-submission-panel">
          <div class="battle-submission-panel__head">
            <div>
              <span class="learning-eyebrow">MY SUBMISSIONS</span>
              <h2>我的提交</h2>
            </div>
            <span class="learning-count">{{ record.mySubmissions.length }} 次</span>
          </div>

          <div v-if="record.mySubmissions.length === 0" class="battle-submission-empty">
            本局没有提交记录。
          </div>

          <div v-else class="battle-submission-list">
            <article
              v-for="submission in record.mySubmissions"
              :key="submission.id"
              class="battle-submission-row"
            >
              <div class="battle-submission-row__main">
                <strong>#{{ submission.id }}</strong>
                <span>{{ languageLabel(submission.language) }}</span>
                <time>{{ submissionTime(submission) }}</time>
              </div>
              <div class="battle-submission-row__tags">
                <el-tag :type="statusTagType(submission.status)" effect="plain" round>
                  {{ submissionStatusLabel(submission.status) }}
                </el-tag>
                <el-tag
                  v-if="submission.verdict"
                  :type="verdictTagType(submission.verdict)"
                  effect="plain"
                  round
                >
                  {{ verdictLabel(submission.verdict) }}
                </el-tag>
              </div>
            </article>
          </div>
        </section>

        <section class="battle-submission-panel">
          <div class="battle-submission-panel__head">
            <div>
              <span class="learning-eyebrow">OPPONENT SUBMISSIONS</span>
              <h2>对手提交</h2>
            </div>
            <span class="learning-count">{{ record.opponentSubmissions.length }} 次</span>
          </div>

          <div v-if="record.opponentSubmissions.length === 0" class="battle-submission-empty">
            对手本局没有提交记录。
          </div>

          <div v-else class="battle-submission-list">
            <article
              v-for="submission in record.opponentSubmissions"
              :key="submission.id"
              class="battle-submission-row"
            >
              <div class="battle-submission-row__main">
                <strong>#{{ submission.id }}</strong>
                <span>{{ languageLabel(submission.language) }}</span>
                <time>{{ submissionTime(submission) }}</time>
              </div>
              <div class="battle-submission-row__tags">
                <el-tag :type="statusTagType(submission.status)" effect="plain" round>
                  {{ submissionStatusLabel(submission.status) }}
                </el-tag>
                <el-tag
                  v-if="submission.verdict"
                  :type="verdictTagType(submission.verdict)"
                  effect="plain"
                  round
                >
                  {{ verdictLabel(submission.verdict) }}
                </el-tag>
              </div>
            </article>
          </div>
        </section>
      </div>

      <p class="battle-record-privacy">
        对战记录只保留提交状态、结果与时间，双方代码互不可见，也不提供回放或观战数据。
      </p>
    </template>
  </div>
</template>
