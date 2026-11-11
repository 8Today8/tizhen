<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Check,
  CopyDocument,
  Link,
  Loading,
  Refresh,
  SwitchButton,
  Timer,
  Trophy,
  VideoPlay,
  Warning
} from '@element-plus/icons-vue'

import {
  cancelBattleRoom,
  createBattleSubmission,
  fetchBattleRoom,
  setBattleReady,
  startBattle,
  surrenderBattle,
  type BattleRoom
} from '@/api/battle'
import {
  fetchProblem,
  fetchSubmission,
  type OjLanguage,
  type ProblemDetail,
  type SubmissionDetail
} from '@/api/problem'
import CodeEditor from '@/components/problem/CodeEditor.vue'
import { useBattleSocket } from '@/composables/useBattleSocket'
import { useAuthStore } from '@/stores/auth'
import { resolveApiErrorMessage } from '@/utils/apiError'
import {
  battleDifficultyLabel,
  battleEndReasonLabel,
  battleResultLabel,
  battleStatusLabel,
  battleStatusTagType,
  formatCountdown
} from '@/utils/battleLabels'
import { formatDateTime, formatDuration } from '@/utils/format'
import { renderMarkdown } from '@/utils/markdown'
import {
  isInProgressStatus,
  isTerminalStatus,
  languageLabel,
  statusTagType,
  submissionStatusLabel,
  verdictLabel,
  verdictTagType
} from '@/utils/problemLabels'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const DEFAULT_CODE = `#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    return 0;
}
`

const roomId = computed(() => String(route.params.roomId ?? ''))
const room = ref<BattleRoom | null>(null)
const loading = ref(true)
const loadError = ref('')
const actionLoading = ref('')
const lastNotice = ref('')

const problemDetail = ref<ProblemDetail | null>(null)
const problemLoading = ref(false)
const problemError = ref('')

const language = ref<OjLanguage>('cpp17')
const code = ref(DEFAULT_CODE)
const submitting = ref(false)
const myLatestSubmission = ref<SubmissionDetail | null>(null)

const nowTick = ref(Date.now())
let clockTimer: number | undefined
let submissionPollTimer: number | undefined
let expiryTriggered = false
let skipLeaveGuard = false

const socket = useBattleSocket(roomId.value, {
  onRoom: handleRoom,
  onEvent: handleSocketEvent,
  onClose: handleSocketClose,
  onError: (message) => {
    lastNotice.value = message
  }
})

const myPlayer = computed(() => {
  if (!room.value) {
    return null
  }
  return isCreator.value ? room.value.creator : room.value.opponent
})
const isCreator = computed(
  () => room.value?.creator.user.id === authStore.user?.id
)
const isOngoing = computed(() => room.value?.status === 'ongoing')
const isFinished = computed(() => room.value?.status === 'finished')
const isCancelled = computed(() => room.value?.status === 'cancelled')
const canReady = computed(
  () => room.value?.status === 'waiting' || room.value?.status === 'ready'
)
const canStart = computed(
  () =>
    isCreator.value &&
    room.value?.status === 'ready' &&
    room.value.creatorReady &&
    room.value.opponentReady
)
const bothReady = computed(
  () => Boolean(room.value?.creatorReady && room.value?.opponentReady)
)

const remainingSeconds = computed(() => {
  if (!room.value?.endsAt || room.value.status !== 'ongoing') {
    return null
  }
  return Math.max(0, Math.floor((new Date(room.value.endsAt).getTime() - nowTick.value) / 1000))
})
const remainingText = computed(() =>
  remainingSeconds.value === null ? '--:--' : formatCountdown(remainingSeconds.value)
)
const battleDurationText = computed(() => {
  if (!room.value?.startedAt || !room.value.endedAt) {
    return '—'
  }
  const seconds = Math.max(
    0,
    Math.floor(
      (new Date(room.value.endedAt).getTime() - new Date(room.value.startedAt).getTime()) / 1000
    )
  )
  return formatDuration(seconds)
})

const statementHtml = computed(() => renderMarkdown(problemDetail.value?.statement))
const inputFormatHtml = computed(() => renderMarkdown(problemDetail.value?.inputFormat))
const outputFormatHtml = computed(() => renderMarkdown(problemDetail.value?.outputFormat))

const connectionText = computed(() => {
  if (isFinished.value || isCancelled.value) {
    return '对局已结束'
  }
  if (socket.state.value === 'online') {
    return '实时连接正常'
  }
  if (socket.state.value === 'connecting') {
    return '正在建立实时连接'
  }
  if (socket.state.value === 'offline') {
    return '实时连接已断开'
  }
  return '实时连接已关闭'
})
const connectionTone = computed(() => {
  if (isFinished.value || isCancelled.value) {
    return 'idle'
  }
  if (socket.state.value === 'online') {
    return 'online'
  }
  if (socket.state.value === 'connecting') {
    return 'connecting'
  }
  return 'offline'
})
// 仅等待/准备阶段允许主动重连；对战中连接断开即按断线判负，不恢复比赛。
const canReconnect = computed(
  () => socket.state.value === 'offline' && canReady.value
)

const resultHeadline = computed(() => {
  if (!room.value) {
    return ''
  }
  if (isCancelled.value) {
    return '房间已取消'
  }
  if (room.value.resultForMe === 'win') {
    return '本局获胜'
  }
  if (room.value.resultForMe === 'loss') {
    return '本局落败'
  }
  if (room.value.resultForMe === 'draw') {
    return '本局平局'
  }
  return '对局已结束'
})
const resultTone = computed(() => {
  if (isCancelled.value) {
    return 'cancelled'
  }
  return room.value?.resultForMe ?? 'draw'
})

const myLatestStatusText = computed(() => {
  const submission = myLatestSubmission.value
  if (!submission) {
    return ''
  }
  if (isInProgressStatus(submission.status)) {
    return submissionStatusLabel(submission.status)
  }
  return verdictLabel(submission.verdict)
})

onMounted(() => {
  void loadRoom()
  clockTimer = window.setInterval(() => {
    nowTick.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  if (clockTimer) {
    window.clearInterval(clockTimer)
  }
  stopSubmissionPolling()
})

onBeforeRouteLeave(async () => {
  if (!isOngoing.value || skipLeaveGuard) {
    return true
  }
  try {
    await ElMessageBox.confirm(
      '比赛仍在进行。离开页面会关闭实时连接，服务端将按断线判负。',
      '离开对战',
      {
        confirmButtonText: '仍然离开',
        cancelButtonText: '留在房间',
        type: 'warning'
      }
    )
    return true
  } catch {
    return false
  }
})

watch(
  () => room.value?.problem?.id,
  (problemId) => {
    if (problemId && problemDetail.value?.id !== problemId) {
      void loadProblem(problemId)
    }
  }
)

watch(remainingSeconds, (value) => {
  if (value === 0 && isOngoing.value && !expiryTriggered) {
    expiryTriggered = true
    window.setTimeout(() => {
      void refreshRoomSilently()
    }, 1200)
  }
})

watch(
  () => room.value?.status,
  (status) => {
    if (status !== 'ongoing') {
      expiryTriggered = false
      stopSubmissionPolling()
    }
  }
)

async function loadRoom() {
  loading.value = true
  loadError.value = ''
  try {
    handleRoom(await fetchBattleRoom(roomId.value))
  } catch (error) {
    loadError.value = resolveApiErrorMessage(error, '房间不存在或当前账号不可见')
  } finally {
    loading.value = false
  }
}

function handleRoom(next: BattleRoom) {
  room.value = next
  if (next.status === 'finished' || next.status === 'cancelled') {
    stopSubmissionPolling()
  }
}

async function refreshRoomSilently() {
  try {
    handleRoom(await fetchBattleRoom(roomId.value))
  } catch {
    // 静默刷新失败不影响当前界面，下一次 WS 快照或手动刷新仍可恢复。
  }
}

async function loadProblem(problemId: string) {
  problemLoading.value = true
  problemError.value = ''
  try {
    problemDetail.value = await fetchProblem(problemId)
  } catch (error) {
    problemError.value = resolveApiErrorMessage(error, '题目加载失败')
  } finally {
    problemLoading.value = false
  }
}

async function toggleReady() {
  if (!room.value || !myPlayer.value || actionLoading.value) {
    return
  }
  actionLoading.value = 'ready'
  try {
    handleRoom(await setBattleReady(room.value.id, !myPlayer.value.ready))
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '准备状态更新失败'))
    await refreshRoomSilently()
  } finally {
    actionLoading.value = ''
  }
}

async function handleStart() {
  if (!room.value || actionLoading.value) {
    return
  }
  actionLoading.value = 'start'
  try {
    handleRoom(await startBattle(room.value.id))
    ElMessage.success('对战开始，计时已启动')
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '开始对战失败'))
    await refreshRoomSilently()
  } finally {
    actionLoading.value = ''
  }
}

async function handleSurrender() {
  if (!room.value || actionLoading.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      '认输后本局立即结束并判定为失败，无法恢复。',
      '确认认输',
      {
        confirmButtonText: '确认认输',
        cancelButtonText: '继续比赛',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  actionLoading.value = 'surrender'
  try {
    handleRoom(await surrenderBattle(room.value.id))
    ElMessage.warning('已认输')
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '认输失败'))
    await refreshRoomSilently()
  } finally {
    actionLoading.value = ''
  }
}

async function handleCancelRoom() {
  if (!room.value || actionLoading.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      '取消后邀请码立即失效，对手将收到房间已取消提示。',
      '取消房间',
      {
        confirmButtonText: '取消房间',
        cancelButtonText: '保留房间',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  actionLoading.value = 'cancel'
  try {
    await cancelBattleRoom(room.value.id)
    ElMessage.success('房间已取消')
    skipLeaveGuard = true
    await router.replace({ name: 'battle' })
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '取消房间失败'))
    await refreshRoomSilently()
  } finally {
    actionLoading.value = ''
  }
}

async function handleSubmit() {
  if (!room.value || submitting.value) {
    return
  }
  if (!code.value.trim()) {
    ElMessage.warning('请先填写代码')
    return
  }

  submitting.value = true
  try {
    const submission = await createBattleSubmission(room.value.id, {
      language: language.value,
      code: code.value
    })
    myLatestSubmission.value = submission
    ElMessage.success('已提交，等待判题')
    pollSubmission(submission.id)
    void refreshRoomSilently()
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '提交失败'))
    await refreshRoomSilently()
  } finally {
    submitting.value = false
  }
}

function pollSubmission(submissionId: string) {
  stopSubmissionPolling()
  const startedAt = Date.now()

  const tick = async () => {
    try {
      const detail = await fetchSubmission(submissionId)
      myLatestSubmission.value = detail
      if (isTerminalStatus(detail.status)) {
        stopSubmissionPolling()
        await refreshRoomSilently()
        return
      }
      if (Date.now() - startedAt > 90_000) {
        stopSubmissionPolling()
        return
      }
      submissionPollTimer = window.setTimeout(tick, 1500)
    } catch {
      stopSubmissionPolling()
    }
  }

  submissionPollTimer = window.setTimeout(tick, 800)
}

function stopSubmissionPolling() {
  if (submissionPollTimer) {
    window.clearTimeout(submissionPollTimer)
    submissionPollTimer = undefined
  }
}

function handleSocketEvent(message: {
  type: string
  payload: Record<string, unknown>
}) {
  if (message.type === 'battle.player.joined') {
    lastNotice.value = '对手已加入房间'
    return
  }
  if (message.type === 'battle.started') {
    lastNotice.value = '对战已开始，30 分钟倒计时启动'
    void refreshRoomSilently()
    return
  }
  if (message.type === 'battle.ended') {
    const result = message.payload.resultForMe
    lastNotice.value =
      typeof result === 'string'
        ? `对局结束：${battleResultLabel(result as 'win' | 'loss' | 'draw')}`
        : '房间已结束'
    void refreshRoomSilently()
    return
  }
  if (message.type === 'battle.player.ready') {
    const userId = String(message.payload.userId ?? '')
    const ready = message.payload.ready === true
    const nickname = playerNickname(userId)
    lastNotice.value = ready ? `${nickname} 已准备` : `${nickname} 取消准备`
    return
  }
  if (message.type === 'battle.player.connection') {
    const userId = String(message.payload.userId ?? '')
    const online = message.payload.online === true
    lastNotice.value = `${playerNickname(userId)} ${online ? '已上线' : '已离线'}`
    return
  }
  if (message.type === 'battle.submission.updated') {
    const userId = String(message.payload.userId ?? '')
    const nickname = playerNickname(userId)
    const verdict = message.payload.verdict
    const status = message.payload.status
    lastNotice.value =
      typeof verdict === 'string'
        ? `${nickname} 最新判题结果：${verdict}`
        : `${nickname} 的提交状态：${String(status ?? '更新')}`
  }
}

function handleSocketClose(code: number) {
  if (code === 4401) {
    lastNotice.value = '登录状态已失效，请重新登录'
    skipLeaveGuard = true
    void router.replace({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (code === 4404) {
    if (!room.value) {
      loadError.value = '房间不存在或当前账号不可见'
    }
    return
  }
  if (code === 4409) {
    lastNotice.value = '对局已结束'
  } else if (isOngoing.value) {
    lastNotice.value = '实时连接已断开，对局可能已按断线判负，正在刷新房间状态'
  } else {
    lastNotice.value = '实时连接已断开，比赛状态可能已变化，请点击重连或刷新房间'
  }
  window.setTimeout(() => {
    void refreshRoomSilently()
  }, 1200)
}

function playerNickname(userId: string) {
  if (!room.value) {
    return '玩家'
  }
  if (room.value.creator.user.id === userId) {
    return room.value.creator.user.nickname
  }
  if (room.value.opponent?.user.id === userId) {
    return room.value.opponent.user.nickname
  }
  return '玩家'
}

async function copyText(value: string, successText: string) {
  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success(successText)
  } catch {
    ElMessage.warning('复制失败，请手动选择文本复制')
  }
}

function copyInviteCode() {
  if (room.value) {
    void copyText(room.value.roomCode, '邀请码已复制')
  }
}

function copyInviteLink() {
  if (room.value) {
    void copyText(
      `${window.location.origin}/battle/join/${room.value.roomCode}`,
      '邀请链接已复制'
    )
  }
}

function reconnect() {
  socket.reconnect()
  void refreshRoomSilently()
}

function backToLobby() {
  void router.push({ name: 'battle' })
}

function openRecord() {
  if (room.value) {
    void router.push({ name: 'battle-record-detail', params: { roomId: room.value.id } })
  }
}
</script>

<template>
  <div class="battle-page battle-room-page">
    <button class="battle-back-link" type="button" @click="backToLobby">
      <el-icon><ArrowLeft /></el-icon>
      对战大厅
    </button>

    <div v-if="loading" class="battle-state-card">
      <el-skeleton :rows="8" animated />
    </div>

    <div v-else-if="loadError" class="battle-state-card battle-state-card--empty">
      <h2>无法进入房间</h2>
      <p>{{ loadError }}</p>
      <div class="battle-state-actions">
        <el-button :icon="Refresh" @click="loadRoom">重试</el-button>
        <el-button type="primary" @click="backToLobby">返回大厅</el-button>
      </div>
    </div>

    <template v-else-if="room">
      <header class="battle-room-head">
        <div class="battle-room-head__identity">
          <span class="learning-eyebrow">BATTLE ROOM</span>
          <div class="battle-room-code">
            <strong>{{ room.roomCode }}</strong>
            <button type="button" class="battle-icon-button" title="复制邀请码" @click="copyInviteCode">
              <el-icon><CopyDocument /></el-icon>
            </button>
          </div>
        </div>

        <div class="battle-room-head__meta">
          <el-tag :type="battleStatusTagType(room.status)" round effect="plain">
            {{ battleStatusLabel(room.status) }}
          </el-tag>
          <span class="battle-connection" :class="`is-${connectionTone}`">
            <i></i>
            {{ connectionText }}
          </span>
          <el-button
            v-if="canReconnect"
            size="small"
            :icon="Refresh"
            @click="reconnect"
          >
            重连
          </el-button>
        </div>
      </header>

      <p v-if="lastNotice" class="battle-notice">
        <el-icon><Warning /></el-icon>
        {{ lastNotice }}
      </p>

      <section class="battle-arena" :class="{ 'is-ongoing': isOngoing }">
        <article class="battle-player-card" :class="{ 'is-me': isCreator }">
          <div class="battle-player-card__top">
            <el-avatar :size="58" :src="room.creator.user.avatarUrl || undefined">
              {{ room.creator.user.nickname.charAt(0) }}
            </el-avatar>
            <div class="battle-player-card__identity">
              <div>
                <strong>{{ room.creator.user.nickname }}</strong>
                <el-tag v-if="isCreator" size="small" round>我</el-tag>
              </div>
              <span>{{ room.creator.user.school || '未填写学校' }}</span>
            </div>
            <span class="battle-online-dot" :class="{ 'is-online': room.creator.online }"></span>
          </div>
          <div class="battle-player-card__stats">
            <span>{{ room.creator.ready ? '已准备' : '未准备' }}</span>
            <span>{{ room.creator.submissionCount }} 次提交</span>
            <span v-if="room.creator.lastVerdict" class="battle-verdict-chip">
              {{ room.creator.lastVerdict }}
            </span>
          </div>
          <div class="battle-player-card__role">房主</div>
        </article>

        <div class="battle-versus">
          <span class="battle-versus__vs">VS</span>
          <div v-if="isOngoing" class="battle-timer">
            <el-icon><Timer /></el-icon>
            <strong>{{ remainingText }}</strong>
            <span>剩余时间</span>
          </div>
          <div v-else-if="isFinished" class="battle-timer battle-timer--finished">
            <el-icon><Trophy /></el-icon>
            <strong>{{ battleResultLabel(room.resultForMe) }}</strong>
            <span>{{ battleEndReasonLabel(room.endReason) }}</span>
          </div>
          <div v-else class="battle-timer">
            <el-icon><Timer /></el-icon>
            <strong>--:--</strong>
            <span>{{ canReady ? '双方准备后开始' : '等待中' }}</span>
          </div>
        </div>

        <article v-if="room.opponent" class="battle-player-card" :class="{ 'is-me': !isCreator }">
          <div class="battle-player-card__top">
            <el-avatar :size="58" :src="room.opponent.user.avatarUrl || undefined">
              {{ room.opponent.user.nickname.charAt(0) }}
            </el-avatar>
            <div class="battle-player-card__identity">
              <div>
                <strong>{{ room.opponent.user.nickname }}</strong>
                <el-tag v-if="!isCreator" size="small" round>我</el-tag>
              </div>
              <span>{{ room.opponent.user.school || '未填写学校' }}</span>
            </div>
            <span class="battle-online-dot" :class="{ 'is-online': room.opponent.online }"></span>
          </div>
          <div class="battle-player-card__stats">
            <span>{{ room.opponent.ready ? '已准备' : '未准备' }}</span>
            <span>{{ room.opponent.submissionCount }} 次提交</span>
            <span v-if="room.opponent.lastVerdict" class="battle-verdict-chip">
              {{ room.opponent.lastVerdict }}
            </span>
          </div>
          <div class="battle-player-card__role">挑战者</div>
        </article>

        <article v-else class="battle-player-card battle-player-card--empty">
          <div class="battle-player-card__empty">
            <el-icon><Link /></el-icon>
            <strong>等待挑战者</strong>
            <span>分享邀请码或链接，好友加入后即可开始</span>
          </div>
        </article>
      </section>

      <section
        v-if="isFinished || isCancelled"
        class="battle-result-banner"
        :class="`is-${resultTone}`"
      >
        <div class="battle-result-banner__icon">
          <el-icon><Trophy /></el-icon>
        </div>
        <div class="battle-result-banner__copy">
          <span class="learning-eyebrow">BATTLE RESULT</span>
          <h2>{{ resultHeadline }}</h2>
          <p>
            <span v-if="room.endReason">{{ battleEndReasonLabel(room.endReason) }}</span>
            <span v-if="room.startedAt && room.endedAt">用时 {{ battleDurationText }}</span>
            <span v-if="room.endedAt">结束于 {{ formatDateTime(room.endedAt) }}</span>
          </p>
        </div>
        <div class="battle-result-banner__actions">
          <el-button :icon="Trophy" @click="openRecord">查看对战记录</el-button>
          <el-button type="primary" @click="backToLobby">返回大厅</el-button>
        </div>
      </section>

      <section v-if="canReady" class="battle-waiting-panel">
        <div class="battle-waiting-panel__invite">
          <span>房间邀请码</span>
          <strong>{{ room.roomCode }}</strong>
          <div>
            <el-button size="small" :icon="CopyDocument" @click="copyInviteCode">复制邀请码</el-button>
            <el-button size="small" :icon="Link" @click="copyInviteLink">复制邀请链接</el-button>
          </div>
        </div>

        <div class="battle-waiting-panel__actions">
          <div class="battle-ready-summary">
            <span>
              房主 {{ room.creatorReady ? '已准备' : '未准备' }}
              · 挑战者 {{ room.opponentReady ? '已准备' : '未准备' }}
            </span>
            <small v-if="!room.opponent">等待挑战者加入后才可开始</small>
            <small v-else-if="bothReady">双方已准备，房主可以开始抽题</small>
            <small v-else>双方都点击准备后，房主即可开始</small>
          </div>

          <el-button
            :type="myPlayer?.ready ? 'default' : 'primary'"
            :loading="actionLoading === 'ready'"
            :disabled="!myPlayer"
            @click="toggleReady"
          >
            {{ myPlayer?.ready ? '取消准备' : '准备' }}
          </el-button>

          <el-button
            v-if="isCreator"
            type="primary"
            :icon="VideoPlay"
            :disabled="!canStart"
            :loading="actionLoading === 'start'"
            @click="handleStart"
          >
            开始对战
          </el-button>

          <el-button
            v-if="isCreator"
            type="danger"
            plain
            :loading="actionLoading === 'cancel'"
            @click="handleCancelRoom"
          >
            取消房间
          </el-button>
        </div>
      </section>

      <div v-if="isOngoing" class="battle-workspace">
        <section class="battle-problem-panel">
          <div v-if="problemLoading" class="battle-panel-loading">
            <el-skeleton :rows="8" animated />
          </div>

          <div v-else-if="problemError" class="battle-panel-loading">
            <el-icon><Warning /></el-icon>
            <p>{{ problemError }}</p>
            <el-button
              v-if="room.problem"
              :icon="Refresh"
              @click="loadProblem(room.problem.id)"
            >
              重新加载题目
            </el-button>
          </div>

          <template v-else-if="problemDetail">
            <div class="battle-problem-head">
              <div>
                <span class="learning-eyebrow">SHARED PROBLEM</span>
                <h2>{{ problemDetail.title }}</h2>
                <div class="battle-problem-meta">
                  <span>{{ battleDifficultyLabel(problemDetail.difficulty) }}</span>
                  <span v-if="problemDetail.timeLimitMs">{{ problemDetail.timeLimitMs }} ms</span>
                  <span v-if="problemDetail.memoryLimitMb">{{ problemDetail.memoryLimitMb }} MB</span>
                </div>
              </div>
              <div class="battle-problem-tags">
                <el-tag
                  v-for="item in problemDetail.tags"
                  :key="item"
                  size="small"
                  effect="plain"
                  round
                >
                  {{ item }}
                </el-tag>
              </div>
            </div>

            <article class="battle-problem-section">
              <h3>题目描述</h3>
              <div class="markdown-body" v-html="statementHtml"></div>
            </article>

            <article class="battle-problem-section battle-problem-format">
              <div>
                <h3>输入格式</h3>
                <div class="markdown-body" v-html="inputFormatHtml"></div>
                <p v-if="!inputFormatHtml" class="battle-muted">题目未单独说明输入格式。</p>
              </div>
              <div>
                <h3>输出格式</h3>
                <div class="markdown-body" v-html="outputFormatHtml"></div>
                <p v-if="!outputFormatHtml" class="battle-muted">题目未单独说明输出格式。</p>
              </div>
            </article>

            <article v-if="problemDetail.samples.length" class="battle-problem-section">
              <h3>样例</h3>
              <div
                v-for="(sample, index) in problemDetail.samples"
                :key="sample.sortOrder"
                class="battle-sample"
              >
                <span>样例 {{ index + 1 }}</span>
                <div>
                  <label>输入</label>
                  <pre>{{ sample.inputData }}</pre>
                </div>
                <div>
                  <label>输出</label>
                  <pre>{{ sample.outputData }}</pre>
                </div>
                <p v-if="sample.explanation">{{ sample.explanation }}</p>
              </div>
            </article>
          </template>
        </section>

        <section class="battle-workbench">
          <div class="battle-workbench__head">
            <div>
              <span class="learning-eyebrow">YOUR SOLUTION</span>
              <h2>作答区</h2>
            </div>
            <div class="battle-workbench__tools">
              <el-select v-model="language" class="battle-language-select">
                <el-option label="C++17" value="cpp17" />
                <el-option label="Python 3.10" value="python310" />
              </el-select>
              <el-button
                type="primary"
                :loading="submitting"
                :disabled="!isOngoing"
                @click="handleSubmit"
              >
                提交判题
              </el-button>
              <el-button type="danger" plain :icon="SwitchButton" @click="handleSurrender">
                认输
              </el-button>
            </div>
          </div>

          <CodeEditor v-model="code" :language="language" />

          <div class="battle-workbench__footer">
            <span>代码长度 {{ code.length }} 字符 · 仅你与判题服务可见</span>
            <span>离开页面会断线判负</span>
          </div>

          <div
            v-if="myLatestSubmission"
            class="battle-latest-submission"
            :class="{ 'is-pending': isInProgressStatus(myLatestSubmission.status) }"
          >
            <div class="battle-latest-submission__status">
              <el-icon v-if="isInProgressStatus(myLatestSubmission.status)" class="is-loading">
                <Loading />
              </el-icon>
              <el-icon v-else-if="myLatestSubmission.verdict === 'AC'">
                <Check />
              </el-icon>
              <el-icon v-else><Warning /></el-icon>
              <div>
                <strong>
                  {{ myLatestStatusText }}
                </strong>
                <span>
                  {{ languageLabel(myLatestSubmission.language) }}
                  · 提交于 {{ formatDateTime(myLatestSubmission.createdAt) }}
                </span>
              </div>
            </div>
            <div class="battle-latest-submission__tags">
              <el-tag
                :type="statusTagType(myLatestSubmission.status)"
                effect="plain"
                round
              >
                {{ submissionStatusLabel(myLatestSubmission.status) }}
              </el-tag>
              <el-tag
                v-if="myLatestSubmission.verdict"
                :type="verdictTagType(myLatestSubmission.verdict)"
                effect="plain"
                round
              >
                {{ verdictLabel(myLatestSubmission.verdict) }}
              </el-tag>
            </div>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>
