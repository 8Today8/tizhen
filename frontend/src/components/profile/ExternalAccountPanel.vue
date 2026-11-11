<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Link, Refresh, TopRight } from '@element-plus/icons-vue'

import {
  bindExternalAccount,
  fetchMyExternalAccounts,
  unbindExternalAccount,
  type ExternalAccount
} from '@/api/external'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatDateTime } from '@/utils/format'

const emit = defineEmits<{ changed: [] }>()

const CODEFORCES_HANDLE_PATTERN = /^[A-Za-z0-9_-]{3,24}$/

const accounts = ref<ExternalAccount[]>([])
const loading = ref(true)
const errorMessage = ref('')
const submitting = ref(false)
const dialogVisible = ref(false)
const form = reactive({
  handle: '',
  profileUrl: ''
})

const cfAccount = computed(
  () => accounts.value.find((account) => account.platform === 'codeforces') ?? null
)

function displayTime(value: string | null): string {
  return value ? formatDateTime(value) : '—'
}

function isValidOptionalUrl(value: string): boolean {
  const trimmed = value.trim()
  if (!trimmed) {
    return true
  }
  if (trimmed.length > 512) {
    return false
  }
  try {
    const url = new URL(trimmed)
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}

async function loadAccounts() {
  loading.value = true
  errorMessage.value = ''
  try {
    accounts.value = (await fetchMyExternalAccounts()).items
  } catch (error) {
    accounts.value = []
    errorMessage.value = resolveApiErrorMessage(error, '外部账号加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function openDialog() {
  form.handle = ''
  form.profileUrl = ''
  dialogVisible.value = true
}

async function submitBind() {
  if (submitting.value) {
    return
  }
  const handle = form.handle.trim()
  if (!CODEFORCES_HANDLE_PATTERN.test(handle)) {
    ElMessage.warning('Handle 需为 3–24 位字母、数字、下划线或连字符')
    return
  }
  if (!isValidOptionalUrl(form.profileUrl)) {
    ElMessage.warning('主页地址需为合法的 http/https URL，且不超过 512 个字符')
    return
  }

  submitting.value = true
  try {
    await bindExternalAccount({
      platform: 'codeforces',
      handle,
      profileUrl: form.profileUrl.trim() || null
    })
    ElMessage.success('Codeforces 账号已绑定')
    dialogVisible.value = false
    await loadAccounts()
    emit('changed')
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '绑定 CF 账号失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

async function handleUnbind() {
  const account = cfAccount.value
  if (!account) {
    return
  }
  try {
    await ElMessageBox.confirm(
      '解绑后该 CF 账号立即不再计入榜单，历史站内成绩不受影响。确认解绑吗？',
      '解绑 Codeforces',
      { confirmButtonText: '解绑', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  submitting.value = true
  try {
    await unbindExternalAccount(account.id)
    ElMessage.success('Codeforces 账号已解绑')
    await loadAccounts()
    emit('changed')
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '解绑 CF 账号失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

onMounted(loadAccounts)
</script>

<template>
  <section class="profile-panel" :class="{ 'profile-panel--bound': Boolean(cfAccount) }">
    <header class="profile-panel__head">
      <span class="profile-panel__icon profile-panel__icon--violet"><el-icon><Link /></el-icon></span>
      <div>
        <h2>Codeforces 绑定</h2>
        <p>绑定后自动同步本周与历史过题数，并计入榜单。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" text class="profile-panel__refresh" @click="loadAccounts">
        刷新
      </el-button>
    </header>

    <div v-if="loading" class="profile-panel__body">
      <el-skeleton :rows="4" animated />
    </div>

    <div v-else-if="errorMessage" class="profile-panel__body profile-panel__state">
      <p>{{ errorMessage }}</p>
      <el-button size="small" :icon="Refresh" @click="loadAccounts">重新加载</el-button>
    </div>

    <div v-else-if="cfAccount" class="profile-panel__body">
      <div class="profile-cf">
        <div class="profile-cf__identity">
          <span class="profile-cf__handle">{{ cfAccount.handle }}</span>
          <el-tag
            :type="cfAccount.verificationStatus === 'verified' ? 'success' : 'warning'"
            effect="plain"
            size="small"
          >
            {{ cfAccount.verificationStatus === 'verified' ? '已验证' : '模拟同步' }}
          </el-tag>
        </div>
        <div class="profile-cf__stats">
          <div>
            <span>本周过题</span>
            <strong>{{ cfAccount.weeklyCount }}</strong>
          </div>
          <div>
            <span>历史累计</span>
            <strong>{{ cfAccount.allTimeCount }}</strong>
          </div>
        </div>
        <dl class="profile-cf__meta">
          <div>
            <dt>数据来源</dt>
            <dd>{{ cfAccount.dataSource === 'api' ? 'Codeforces API' : '模拟数据' }}</dd>
          </div>
          <div>
            <dt>最近同步</dt>
            <dd>{{ displayTime(cfAccount.lastSyncAt) }}</dd>
          </div>
        </dl>
        <div class="profile-panel__actions">
          <a
            v-if="cfAccount.profileUrl"
            class="profile-text-link"
            :href="cfAccount.profileUrl"
            target="_blank"
            rel="noopener noreferrer"
          >
            查看 CF 主页
            <el-icon><TopRight /></el-icon>
          </a>
          <el-button size="small" :loading="submitting" @click="handleUnbind">解绑</el-button>
        </div>
      </div>
    </div>

    <div v-else class="profile-panel__body profile-panel__empty">
      <p>尚未绑定 Codeforces 账号。绑定后，外部过题数据会出现在榜单中。</p>
      <el-button type="primary" :icon="Link" @click="openDialog">绑定 CF 账号</el-button>
    </div>

    <el-dialog v-model="dialogVisible" title="绑定 Codeforces" width="min(480px, 92vw)">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="一个 CF Handle 只能绑定一个平台账号；绑定时会读取公开数据完成校验。"
      />
      <el-form label-position="top" class="profile-dialog-form" @submit.prevent>
        <el-form-item label="Codeforces Handle" required>
          <el-input
            v-model="form.handle"
            maxlength="24"
            placeholder="例如 tourist"
            @keyup.enter="submitBind"
          />
        </el-form-item>
        <el-form-item label="主页地址（选填）">
          <el-input
            v-model="form.profileUrl"
            maxlength="512"
            placeholder="留空则使用 Codeforces 默认主页"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBind">绑定</el-button>
      </template>
    </el-dialog>
  </section>
</template>
