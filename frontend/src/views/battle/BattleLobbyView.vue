<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Plus, Refresh, Trophy } from '@element-plus/icons-vue'

import {
  createBattleRoom,
  fetchBattleRecords,
  type BattleRecordItem
} from '@/api/battle'
import { fetchProblemTags, type ProblemTagOption } from '@/api/problem'
import {
  battleEndReasonLabel,
  battleResultLabel,
  battleResultTagType
} from '@/utils/battleLabels'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

const difficulty = ref('')
const tag = ref('')
const tags = ref<ProblemTagOption[]>([])
const tagsLoading = ref(false)
const creating = ref(false)
const joinCode = ref('')
const joinError = ref('')

const recent = ref<BattleRecordItem[]>([])
const recentLoading = ref(true)
const recentError = ref('')

const difficultyOptions = [
  { label: '不限难度', value: '' },
  { label: '入门', value: 'entry' },
  { label: '基础', value: 'basic' },
  { label: '进阶', value: 'advanced' }
]

onMounted(() => {
  void loadTags()
  void loadRecentRecords()
})

async function loadTags() {
  tagsLoading.value = true
  try {
    const response = await fetchProblemTags()
    tags.value = response.items
  } catch {
    // 标签加载失败不阻塞创建房间，仍可按难度或全题库抽题。
    tags.value = []
  } finally {
    tagsLoading.value = false
  }
}

async function loadRecentRecords() {
  recentLoading.value = true
  recentError.value = ''
  try {
    const page = await fetchBattleRecords({ page: 1, pageSize: 5 })
    recent.value = page.items
  } catch (error) {
    recentError.value = resolveApiErrorMessage(error, '对战记录加载失败')
  } finally {
    recentLoading.value = false
  }
}

async function handleCreate() {
  if (creating.value) {
    return
  }
  creating.value = true
  try {
    const room = await createBattleRoom({
      filterDifficulty: difficulty.value || null,
      filterTag: tag.value || null
    })
    ElMessage.success(`房间已创建，邀请码 ${room.roomCode}`)
    await router.push({ name: 'battle-room', params: { roomId: room.id } })
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '创建房间失败，请稍后重试'))
  } finally {
    creating.value = false
  }
}

function handleJoin() {
  const normalized = joinCode.value.trim().toUpperCase()
  if (!/^[A-Z0-9]{8}$/.test(normalized)) {
    joinError.value = '请输入 8 位邀请码'
    return
  }
  joinError.value = ''
  void router.push({ name: 'battle-join', params: { roomCode: normalized } })
}

function openRecord(roomId: string) {
  void router.push({ name: 'battle-record-detail', params: { roomId } })
}

function recordTime(item: BattleRecordItem) {
  if (!item.endedAt && !item.startedAt) {
    return '时间未知'
  }
  return formatDateTime(item.endedAt || item.startedAt || '')
}
</script>

<template>
  <div class="battle-page">
    <section class="battle-hero">
      <div class="battle-hero__copy">
        <span class="learning-eyebrow">M07 · 1v1 BATTLE</span>
        <h1>同题竞速，<span>先 AC 者胜。</span></h1>
        <p>
          创建一间邀请制房间，系统按难度与专题抽取一道 OJ 题。双方在 30 分钟内独立作答，
          服务端判题完成顺序决定胜负；提交计入首次 AC、榜单与错题本。
        </p>
        <div class="battle-hero__rules">
          <span>30 分钟限时</span>
          <span>先 AC 结束对局</span>
          <span>断线判负</span>
          <span>双方代码互不可见</span>
        </div>
      </div>

      <div class="battle-hero__panel">
        <div class="battle-panel-title">
          <el-icon><Plus /></el-icon>
          创建对战房间
        </div>

        <div class="battle-form-field">
          <label for="battle-difficulty">难度筛选</label>
          <el-select id="battle-difficulty" v-model="difficulty" placeholder="不限难度">
            <el-option
              v-for="option in difficultyOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </div>

        <div class="battle-form-field">
          <label for="battle-tag">专题标签</label>
          <el-select
            id="battle-tag"
            v-model="tag"
            clearable
            filterable
            :loading="tagsLoading"
            placeholder="不限专题"
          >
            <el-option
              v-for="option in tags"
              :key="option.value"
              :label="`${option.value}（${option.count}）`"
              :value="option.value"
            />
          </el-select>
        </div>

        <el-button
          class="battle-primary-button"
          type="primary"
          size="large"
          :loading="creating"
          @click="handleCreate"
        >
          创建房间并获取邀请码
        </el-button>

        <div class="battle-join">
          <div class="battle-join__title">已有邀请码？</div>
          <div class="battle-join__row">
            <el-input
              v-model="joinCode"
              maxlength="8"
              placeholder="输入 8 位邀请码"
              @keyup.enter="handleJoin"
            />
            <el-button :icon="ArrowRight" @click="handleJoin">加入</el-button>
          </div>
          <p v-if="joinError" class="battle-inline-error">{{ joinError }}</p>
        </div>
      </div>
    </section>

    <section class="learning-section">
      <div class="learning-section__head">
        <div>
          <h2>最近对战</h2>
          <p>只展示已结束的对局，点击查看双方提交结果与结束原因。</p>
        </div>
        <router-link class="battle-text-link" :to="{ name: 'battle-records' }">
          <el-icon><Trophy /></el-icon>
          全部记录
        </router-link>
      </div>

      <div v-if="recentLoading" class="battle-state-card">
        <el-skeleton :rows="3" animated />
      </div>

      <div v-else-if="recentError" class="battle-state-card">
        <p>{{ recentError }}</p>
        <el-button :icon="Refresh" @click="loadRecentRecords">重试</el-button>
      </div>

      <div v-else-if="recent.length === 0" class="battle-state-card battle-state-card--empty">
        <el-icon><Trophy /></el-icon>
        <h3>还没有对战记录</h3>
        <p>创建房间或输入好友的邀请码，完成第一场 1v1。</p>
      </div>

      <div v-else class="battle-recent-list">
        <button
          v-for="item in recent"
          :key="item.roomId"
          type="button"
          class="battle-recent-item"
          @click="openRecord(item.roomId)"
        >
          <div class="battle-recent-item__result">
            <el-tag :type="battleResultTagType(item.resultForMe)" effect="dark" round>
              {{ battleResultLabel(item.resultForMe) }}
            </el-tag>
            <span>{{ battleEndReasonLabel(item.endReason) }}</span>
          </div>
          <div class="battle-recent-item__main">
            <strong>
              {{ item.opponent?.nickname || '未知对手' }}
              <small>#{{ item.roomCode }}</small>
            </strong>
            <span>{{ item.problem?.title || '题目已不可用' }}</span>
          </div>
          <time>{{ recordTime(item) }}</time>
          <el-icon class="battle-recent-item__arrow"><ArrowRight /></el-icon>
        </button>
      </div>
    </section>

    <section class="battle-info-grid">
      <article class="battle-info-card">
        <span class="battle-info-card__index">01</span>
        <h3>邀请制房间</h3>
        <p>房主设置难度或专题筛选，生成 8 位邀请码；好友凭码预览并加入，不支持随机匹配与观战。</p>
      </article>
      <article class="battle-info-card">
        <span class="battle-info-card__index">02</span>
        <h3>服务端判胜负</h3>
        <p>双方就绪后由房主开始，服务端抽取共享题目。判题按完成顺序处理，条件更新保证只有一方先 AC 获胜。</p>
      </article>
      <article class="battle-info-card">
        <span class="battle-info-card__index">03</span>
        <h3>完整训练闭环</h3>
        <p>对战中 AC 仍计入首次 AC 与榜单，未通过提交进入错题本。结束后可回看双方提交结果，但不展示代码。</p>
      </article>
    </section>
  </div>
</template>
