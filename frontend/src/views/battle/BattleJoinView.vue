<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Check, Refresh, UserFilled } from '@element-plus/icons-vue'

import {
  fetchBattleRoomByCode,
  joinBattleRoom,
  type BattleRoomPreview
} from '@/api/battle'
import { battleStatusLabel, battleStatusTagType } from '@/utils/battleLabels'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const roomCode = computed(() => String(route.params.roomCode ?? '').trim().toUpperCase())
const preview = ref<BattleRoomPreview | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const joining = ref(false)

onMounted(loadPreview)

async function loadPreview() {
  loading.value = true
  errorMessage.value = ''
  try {
    preview.value = await fetchBattleRoomByCode(roomCode.value)
  } catch (error) {
    errorMessage.value = resolveApiErrorMessage(error, '房间不存在或已不可用')
  } finally {
    loading.value = false
  }
}

async function handleJoin() {
  if (!preview.value || joining.value) {
    return
  }
  joining.value = true
  try {
    const room = await joinBattleRoom(preview.value.roomCode)
    ElMessage.success('已加入房间')
    await router.replace({ name: 'battle-room', params: { roomId: room.id } })
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '加入房间失败'))
    await loadPreview()
  } finally {
    joining.value = false
  }
}

function backToLobby() {
  void router.push({ name: 'battle' })
}
</script>

<template>
  <div class="battle-page battle-join-page">
    <button class="battle-back-link" type="button" @click="backToLobby">
      <el-icon><ArrowLeft /></el-icon>
      返回对战大厅
    </button>

    <div v-if="loading" class="battle-state-card">
      <el-skeleton :rows="5" animated />
    </div>

    <div v-else-if="errorMessage" class="battle-state-card battle-state-card--empty">
      <h2>无法打开邀请</h2>
      <p>{{ errorMessage }}</p>
      <div class="battle-state-actions">
        <el-button :icon="Refresh" @click="loadPreview">重试</el-button>
        <el-button type="primary" @click="backToLobby">返回大厅</el-button>
      </div>
    </div>

    <section v-else-if="preview" class="battle-invite-card">
      <div class="battle-invite-card__head">
        <span class="learning-eyebrow">BATTLE INVITATION</span>
        <el-tag :type="battleStatusTagType(preview.status)" round>
          {{ battleStatusLabel(preview.status) }}
        </el-tag>
      </div>

      <div class="battle-invite-code">
        <span>房间邀请码</span>
        <strong>{{ preview.roomCode }}</strong>
      </div>

      <div class="battle-invite-players">
        <div class="battle-invite-player">
          <el-avatar :size="52" :src="preview.creator.avatarUrl || undefined">
            {{ preview.creator.nickname.charAt(0) }}
          </el-avatar>
          <div>
            <span>房主</span>
            <strong>{{ preview.creator.nickname }}</strong>
            <small>{{ preview.creator.school || '未填写学校' }}</small>
          </div>
        </div>

        <div class="battle-invite-versus">VS</div>

        <div class="battle-invite-player" :class="{ 'is-empty': !preview.opponent }">
          <el-avatar :size="52" :src="preview.opponent?.avatarUrl || undefined">
            <el-icon v-if="!preview.opponent"><UserFilled /></el-icon>
            <template v-else>{{ preview.opponent.nickname.charAt(0) }}</template>
          </el-avatar>
          <div>
            <span>挑战者</span>
            <strong>{{ preview.opponent?.nickname || '等待加入' }}</strong>
            <small>{{ preview.opponent?.school || '虚位以待' }}</small>
          </div>
        </div>
      </div>

      <p class="battle-invite-note">
        创建于 {{ formatDateTime(preview.createdAt) }}。加入后双方准备，由房主开始并抽取共享题目。
      </p>

      <el-button
        v-if="preview.canJoin"
        class="battle-primary-button"
        type="primary"
        size="large"
        :loading="joining"
        :icon="Check"
        @click="handleJoin"
      >
        加入房间
      </el-button>

      <div v-else class="battle-invite-blocked">
        {{ preview.opponent ? '房间已有两名选手，无法再加入。' : '当前房间状态不允许加入。' }}
      </div>
    </section>
  </div>
</template>
