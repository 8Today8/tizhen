<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Refresh, Trophy } from '@element-plus/icons-vue'

import {
  fetchBattleRecords,
  type BattleRecordItem,
  type BattleResult
} from '@/api/battle'
import type { PageResult } from '@/types/api'
import { resolveApiErrorMessage } from '@/utils/apiError'
import {
  battleEndReasonLabel,
  battleResultLabel,
  battleResultTagType
} from '@/utils/battleLabels'
import { formatDateTime, formatDuration } from '@/utils/format'

const PAGE_SIZE = 20
const router = useRouter()

const result = ref<BattleResult | ''>('')
const page = ref(1)
const records = ref<PageResult<BattleRecordItem> | null>(null)
const loading = ref(true)
const errorMessage = ref('')

onMounted(loadRecords)
watch(result, () => {
  page.value = 1
  void loadRecords()
})

async function loadRecords() {
  loading.value = true
  errorMessage.value = ''
  try {
    records.value = await fetchBattleRecords({
      result: result.value,
      page: page.value,
      pageSize: PAGE_SIZE
    })
  } catch (error) {
    errorMessage.value = resolveApiErrorMessage(error, '对战记录加载失败')
  } finally {
    loading.value = false
  }
}

function changePage(nextPage: number) {
  page.value = nextPage
  void loadRecords()
}

function openRecord(roomId: string) {
  void router.push({ name: 'battle-record-detail', params: { roomId } })
}

function recordTime(item: BattleRecordItem) {
  return item.endedAt ? formatDateTime(item.endedAt) : '时间未知'
}
</script>

<template>
  <div class="battle-page battle-records-page">
    <button class="battle-back-link" type="button" @click="router.push({ name: 'battle' })">
      <el-icon><ArrowLeft /></el-icon>
      对战大厅
    </button>

    <section class="battle-records-head">
      <div>
        <span class="learning-eyebrow">BATTLE HISTORY</span>
        <h1>我的对战记录</h1>
        <p>只展示已结束的对局。双方代码不会出现在记录中，仅保留提交结果与耗时。</p>
      </div>
      <div class="battle-records-filter">
        <el-radio-group v-model="result" size="small">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="win">胜利</el-radio-button>
          <el-radio-button value="loss">失败</el-radio-button>
          <el-radio-button value="draw">平局</el-radio-button>
        </el-radio-group>
      </div>
    </section>

    <div v-if="loading" class="battle-state-card">
      <el-skeleton :rows="6" animated />
    </div>

    <div v-else-if="errorMessage" class="battle-state-card">
      <p>{{ errorMessage }}</p>
      <el-button :icon="Refresh" @click="loadRecords">重试</el-button>
    </div>

    <div v-else-if="!records || records.items.length === 0" class="battle-state-card battle-state-card--empty">
      <el-icon><Trophy /></el-icon>
      <h3>{{ result ? '没有符合条件的记录' : '还没有对战记录' }}</h3>
      <p>{{ result ? '切换筛选条件试试。' : '完成第一场 1v1 后，对局会出现在这里。' }}</p>
      <el-button type="primary" @click="router.push({ name: 'battle' })">去对战大厅</el-button>
    </div>

    <template v-else>
      <div class="battle-record-list">
        <button
          v-for="item in records.items"
          :key="item.roomId"
          type="button"
          class="battle-record-card"
          @click="openRecord(item.roomId)"
        >
          <div class="battle-record-card__result">
            <el-tag :type="battleResultTagType(item.resultForMe)" effect="dark" round>
              {{ battleResultLabel(item.resultForMe) }}
            </el-tag>
            <span>{{ battleEndReasonLabel(item.endReason) }}</span>
          </div>

          <div class="battle-record-card__opponent">
            <el-avatar :size="44" :src="item.opponent?.avatarUrl || undefined">
              {{ item.opponent?.nickname.charAt(0) || '?' }}
            </el-avatar>
            <div>
              <strong>{{ item.opponent?.nickname || '未知对手' }}</strong>
              <span>{{ item.opponent?.school || '未填写学校' }}</span>
            </div>
          </div>

          <div class="battle-record-card__problem">
            <span>题目</span>
            <strong>{{ item.problem?.title || '题目已不可用' }}</strong>
            <small>#{{ item.roomCode }}</small>
          </div>

          <div class="battle-record-card__time">
            <strong>{{ formatDuration(item.durationSeconds) }}</strong>
            <span>{{ recordTime(item) }}</span>
          </div>

          <el-icon class="battle-record-card__arrow"><ArrowRight /></el-icon>
        </button>
      </div>

      <div v-if="records.totalPages > 1" class="battle-pagination">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="records.page"
          :page-size="records.pageSize"
          :total="records.total"
          @current-change="changePage"
        />
      </div>
    </template>
  </div>
</template>
