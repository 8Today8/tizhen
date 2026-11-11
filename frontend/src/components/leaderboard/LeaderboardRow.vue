<script setup lang="ts">
import { computed } from 'vue'

import type { LeaderboardItem } from '@/api/leaderboard'
import { formatDateTime } from '@/utils/format'
import {
  cfSourceLabel,
  manualSourceLabel,
  rankToneClass
} from '@/utils/leaderboard'

const props = defineProps<{
  item: LeaderboardItem
}>()

const playerInitial = computed(() => props.item.user.nickname?.slice(0, 1) || '?')

function displayTime(value: string | null): string {
  return value ? formatDateTime(value) : '—'
}
</script>

<template>
  <tr class="lb-row" :class="{ 'is-me': item.isMe }">
    <td>
      <span :class="rankToneClass(item.rank)">{{ item.rank }}</span>
    </td>
    <td>
      <div class="lb-user">
        <el-avatar :size="36" :src="item.user.avatarUrl || undefined">
          {{ playerInitial }}
        </el-avatar>
        <div class="lb-user__text">
          <strong>
            {{ item.user.nickname }}
            <em v-if="item.isMe" class="lb-me-tag">我</em>
          </strong>
          <span>{{ item.user.school || '未填写学校' }}</span>
        </div>
      </div>
    </td>
    <td class="lb-num">{{ item.internalSolved }}</td>
    <td class="lb-num">{{ item.cfSolved }}</td>
    <td class="lb-num">{{ item.manualSolved }}</td>
    <td class="lb-num">
      <strong class="lb-total">{{ item.totalScore }}</strong>
    </td>
    <td>
      <div class="lb-sources">
        <template v-if="item.internalSolved > 0">
          <span class="lb-source lb-source--internal">站内首次 AC</span>
        </template>
        <template v-if="item.cfSolved > 0">
          <span class="lb-source lb-source--cf">
            {{ cfSourceLabel(item.cfVerificationStatus) || 'CF 过题' }}
          </span>
          <small v-if="item.cfLastSyncAt" class="lb-source-time">
            同步 {{ displayTime(item.cfLastSyncAt) }}
          </small>
        </template>
        <template v-if="item.manualSolved > 0">
          <span class="lb-source lb-source--manual">
            {{ manualSourceLabel(item.manualVerificationStatus) || '手动申报' }}
          </span>
          <small v-if="item.manualUpdatedAt" class="lb-source-time">
            更新 {{ displayTime(item.manualUpdatedAt) }}
          </small>
        </template>
        <span
          v-if="item.internalSolved === 0 && item.cfSolved === 0 && item.manualSolved === 0"
          class="lb-source-time"
        >
          —
        </span>
      </div>
    </td>
  </tr>
</template>
