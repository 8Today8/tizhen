<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Calendar,
  DataLine,
  EditPen,
  Message,
  Phone,
  Refresh,
  School,
  Trophy,
  User
} from '@element-plus/icons-vue'

import {
  fetchPlayerMe,
  fetchPlayerStats,
  updatePlayerProfile,
  type PlayerProfile,
  type PlayerStats,
  type UpdatePlayerProfilePayload
} from '@/api/account'
import AvatarUploader from '@/components/profile/AvatarUploader.vue'
import ExternalAccountPanel from '@/components/profile/ExternalAccountPanel.vue'
import LlmCredentialPanel from '@/components/profile/LlmCredentialPanel.vue'
import ManualDeclarationPanel from '@/components/profile/ManualDeclarationPanel.vue'
import { useAuthStore } from '@/stores/auth'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatDateTime } from '@/utils/format'

const authStore = useAuthStore()

const stats = ref<PlayerStats | null>(null)
const statsLoading = ref(true)
const statsError = ref('')
const profileLoading = ref(false)
const profileError = ref('')
const saving = ref(false)

const form = reactive({
  nickname: '',
  email: '',
  phone: '',
  school: ''
})

const user = computed(() => authStore.user)
const userInitial = computed(() => user.value?.nickname.charAt(0) || '选')
const avatarPreview = computed(() => user.value?.avatarUrl || undefined)

const learningPercent = computed(() => {
  const learning = stats.value?.learning
  if (!learning || learning.totalStages <= 0) {
    return 0
  }
  return Math.round((learning.completedStages / learning.totalStages) * 100)
})

const weekRankText = computed(() => {
  const rank = stats.value?.leaderboard.week.rank
  return rank == null ? '未上榜' : `#${rank}`
})

const allTimeRankText = computed(() => {
  const rank = stats.value?.leaderboard.allTime.rank
  return rank == null ? '未上榜' : `#${rank}`
})

const battleSummary = computed(() => {
  const battles = stats.value?.battles
  if (!battles) {
    return '暂无对战'
  }
  return `${battles.wins} 胜 · ${battles.losses} 负 · ${battles.draws} 平`
})

function displayTime(value: string | null): string {
  return value ? formatDateTime(value) : '—'
}

function syncForm(profile: PlayerProfile) {
  form.nickname = profile.nickname
  form.email = profile.email ?? ''
  form.phone = profile.phone ?? ''
  form.school = profile.school ?? ''
}

async function loadProfile() {
  profileLoading.value = true
  profileError.value = ''
  try {
    const profile = await fetchPlayerMe()
    authStore.setUser(profile)
    syncForm(profile)
  } catch (error) {
    profileError.value = resolveApiErrorMessage(error, '个人资料加载失败，请稍后重试')
  } finally {
    profileLoading.value = false
  }
}

async function loadStats() {
  statsLoading.value = true
  statsError.value = ''
  try {
    stats.value = await fetchPlayerStats()
  } catch (error) {
    stats.value = null
    statsError.value = resolveApiErrorMessage(error, '个人统计加载失败，请稍后重试')
  } finally {
    statsLoading.value = false
  }
}

function resetForm() {
  if (user.value) {
    syncForm(user.value)
  }
}

function handleAvatarUpdated(profile: PlayerProfile) {
  authStore.setUser(profile)
}

async function handleSave() {
  if (saving.value) {
    return
  }
  const original = user.value
  if (!original) {
    return
  }

  const nickname = form.nickname.trim()
  if (!nickname || nickname.length > 64) {
    ElMessage.warning('昵称需为 1–64 个字符')
    return
  }

  const email = form.email.trim()
  if (email !== (original.email ?? '')) {
    if (!email) {
      ElMessage.warning('邮箱不能清空，只能修改为新的邮箱')
      return
    }
    if (email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      ElMessage.warning('邮箱格式不正确')
      return
    }
  }

  const phone = form.phone.trim()
  if (phone && (phone.length < 6 || phone.length > 32)) {
    ElMessage.warning('手机号需为 6–32 个字符')
    return
  }

  const school = form.school.trim()
  if (school.length > 128) {
    ElMessage.warning('学校不能超过 128 个字符')
    return
  }

  const payload: UpdatePlayerProfilePayload = {}
  if (nickname !== original.nickname) {
    payload.nickname = nickname
  }
  if (email !== (original.email ?? '')) {
    payload.email = email
  }
  if (phone !== (original.phone ?? '')) {
    payload.phone = phone || null
  }
  const currentSchool = school || null
  if (currentSchool !== (original.school ?? null)) {
    payload.school = currentSchool
  }

  if (Object.keys(payload).length === 0) {
    ElMessage.info('资料没有变化')
    return
  }

  saving.value = true
  try {
    const updated = await updatePlayerProfile(payload)
    authStore.setUser(updated)
    syncForm(updated)
    ElMessage.success('个人资料已更新')
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '保存资料失败，请稍后重试'))
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (authStore.user) {
    syncForm(authStore.user)
  }
  void loadProfile()
  void loadStats()
})
</script>

<template>
  <div class="profile-page">
    <section v-if="user" class="profile-hero">
      <div class="profile-hero__identity">
        <el-avatar :size="92" :src="avatarPreview" class="profile-hero__avatar">
          {{ userInitial }}
        </el-avatar>
        <div class="profile-hero__copy">
          <span class="profile-eyebrow">PLAYER PROFILE</span>
          <h1>{{ user.nickname }}</h1>
          <p>
            @{{ user.username }}
            <span class="profile-hero__dot">·</span>
            {{ user.school || '暂未填写学校' }}
          </p>
          <div class="profile-hero__meta">
            <el-tag :type="user.status === 'active' ? 'success' : 'danger'" effect="plain" size="small">
              {{ user.status === 'active' ? '账号正常' : '账号已禁用' }}
            </el-tag>
            <span><el-icon><Calendar /></el-icon>加入于 {{ displayTime(user.createdAt) }}</span>
            <span><el-icon><User /></el-icon>最近登录 {{ displayTime(user.lastLoginAt) }}</span>
          </div>
        </div>
      </div>

      <div class="profile-hero__rank">
        <div>
          <span>本周排名</span>
          <strong>{{ statsLoading ? '—' : weekRankText }}</strong>
          <em>{{ stats?.leaderboard.week.score ?? 0 }} 分</em>
        </div>
        <div>
          <span>总榜排名</span>
          <strong>{{ statsLoading ? '—' : allTimeRankText }}</strong>
          <em>{{ stats?.leaderboard.allTime.score ?? 0 }} 分</em>
        </div>
      </div>
    </section>

    <section class="profile-stats" aria-label="个人统计">
      <div v-if="statsLoading" class="profile-stats__skeleton">
        <el-skeleton v-for="index in 6" :key="index" :rows="2" animated />
      </div>
      <div v-else-if="statsError" class="profile-stats__error">
        <span>{{ statsError }}</span>
        <el-button size="small" :icon="Refresh" @click="loadStats">重新加载</el-button>
      </div>
      <template v-else-if="stats">
        <article class="profile-stat">
          <span class="profile-stat__label">阶段进度</span>
          <strong>{{ stats.learning.completedStages }}<small>/{{ stats.learning.totalStages }}</small></strong>
          <div class="profile-stat__progress">
            <el-progress :percentage="learningPercent" :stroke-width="6" :show-text="false" />
            <span>{{ learningPercent }}%</span>
          </div>
        </article>
        <article class="profile-stat">
          <span class="profile-stat__label">提交 / 作答</span>
          <strong>{{ stats.submissions.total }}</strong>
          <p>客观题 {{ stats.submissions.objectiveAnswered }} · 简答题 {{ stats.submissions.shortAnswerAnswered }}</p>
        </article>
        <article class="profile-stat">
          <span class="profile-stat__label">OJ 首次 AC</span>
          <strong>{{ stats.submissions.ojAcCount }}</strong>
          <p>站内编程题去重统计</p>
        </article>
        <article class="profile-stat">
          <span class="profile-stat__label">待复习错题</span>
          <strong>{{ stats.wrongBook.activeCount }}</strong>
          <RouterLink to="/wrongbook" class="profile-stat__link">
            打开错题本 <el-icon><ArrowRight /></el-icon>
          </RouterLink>
        </article>
        <article class="profile-stat">
          <span class="profile-stat__label">完成对战</span>
          <strong>{{ stats.battles.total }}</strong>
          <p>{{ battleSummary }}</p>
        </article>
        <article class="profile-stat">
          <span class="profile-stat__label">榜单表现</span>
          <strong>{{ weekRankText }}</strong>
          <p>本周 · 总榜 {{ allTimeRankText }}</p>
        </article>
      </template>
    </section>

    <div class="profile-grid">
      <div class="profile-main">
        <section class="profile-panel profile-panel--form">
          <header class="profile-panel__head">
            <span class="profile-panel__icon profile-panel__icon--violet"><el-icon><EditPen /></el-icon></span>
            <div>
              <h2>编辑资料</h2>
              <p>昵称会显示在榜单与对战房间中；邮箱、手机号不会公开展示。</p>
            </div>
          </header>

          <div v-if="profileError" class="profile-panel__body profile-panel__state">
            <p>{{ profileError }}</p>
            <el-button size="small" :icon="Refresh" @click="loadProfile">重新加载</el-button>
          </div>

          <div v-else-if="profileLoading && !user" class="profile-panel__body">
            <el-skeleton :rows="5" animated />
          </div>

          <form v-else class="profile-panel__body profile-form" @submit.prevent="handleSave">
            <AvatarUploader
              :avatar-url="user?.avatarUrl ?? null"
              :initial="userInitial"
              @updated="handleAvatarUpdated"
            />

            <div class="profile-form-grid">
              <el-form-item label="昵称" required>
                <el-input v-model="form.nickname" maxlength="64" show-word-limit placeholder="1–64 个字符" />
              </el-form-item>
              <el-form-item label="学校">
                <el-input v-model="form.school" maxlength="128" placeholder="选填，最多 128 个字符" />
              </el-form-item>
            </div>

            <el-form-item label="绑定邮箱">
              <el-input v-model="form.email" maxlength="254" placeholder="name@example.com" />
              <p class="profile-form__hint">
                邮箱全局唯一，首期仅做基础绑定与展示，不发送验证邮件；已绑定邮箱不可清空。
              </p>
            </el-form-item>

            <el-form-item label="手机号">
              <el-input v-model="form.phone" maxlength="32" placeholder="选填，6–32 个字符；留空清除" />
            </el-form-item>

            <div class="profile-form__actions">
              <el-button @click="resetForm">重置</el-button>
              <el-button type="primary" :loading="saving" native-type="submit">保存资料</el-button>
            </div>
          </form>
        </section>

        <ExternalAccountPanel class="profile-panel--cf" @changed="loadStats" />
      </div>

      <div class="profile-side">
        <section class="profile-panel profile-panel--overview">
          <header class="profile-panel__head">
            <span class="profile-panel__icon profile-panel__icon--amber"><el-icon><User /></el-icon></span>
            <div>
              <h2>账号概览</h2>
              <p>用户名与登录凭证由账号系统管理。</p>
            </div>
          </header>
          <div class="profile-panel__body">
            <dl class="profile-overview">
              <div>
                <dt>用户名</dt>
                <dd>{{ user?.username }} <span class="profile-overview__lock">不可修改</span></dd>
              </div>
              <div>
                <dt><el-icon><Message /></el-icon>邮箱</dt>
                <dd>{{ user?.email || '未绑定' }}</dd>
              </div>
              <div>
                <dt><el-icon><Phone /></el-icon>手机号</dt>
                <dd>{{ user?.phone || '未填写' }}</dd>
              </div>
              <div>
                <dt><el-icon><School /></el-icon>学校</dt>
                <dd>{{ user?.school || '未填写' }}</dd>
              </div>
              <div>
                <dt><el-icon><Calendar /></el-icon>注册时间</dt>
                <dd>{{ displayTime(user?.createdAt ?? null) }}</dd>
              </div>
            </dl>
            <div class="profile-quick-links">
              <RouterLink to="/records"><el-icon><DataLine /></el-icon>学习记录</RouterLink>
              <RouterLink to="/wrongbook"><el-icon><EditPen /></el-icon>错题本</RouterLink>
              <RouterLink to="/leaderboard"><el-icon><Trophy /></el-icon>榜单</RouterLink>
            </div>
          </div>
        </section>

        <LlmCredentialPanel class="profile-panel--credential" />
        <ManualDeclarationPanel class="profile-panel--declarations" @changed="loadStats" />
      </div>
    </div>
  </div>
</template>
