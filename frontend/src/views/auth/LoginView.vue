<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Key, Lock, Timer, Trophy, User } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import {
  fetchPlayerCsrf,
  loginPlayer,
  registerPlayer,
  type PlayerLoginPayload,
  type PlayerRegisterPayload,
  type PlayerSession
} from '@/api/account'
import { ApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

type TabKey = 'login' | 'register'

const AUTH_ERROR_MESSAGES: Record<string, string> = {
  AUTH_INVALID_CREDENTIALS: '用户名或密码错误',
  AUTH_ACCOUNT_DISABLED: '该账号已被禁用，请联系管理员',
  AUTH_USERNAME_TAKEN: '该用户名已被注册，请更换一个',
  COMMON_RATE_LIMITED: '登录尝试过于频繁，请稍后再试',
  COMMON_CSRF_INVALID: '安全校验已失效，请重新尝试',
  CLIENT_TIMEOUT: '请求超时，请稍后重试',
  CLIENT_NETWORK_ERROR: '网络连接异常，请检查网络后重试'
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeTab = ref<TabKey>(readTab(route.query.tab))
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const loginForm = reactive<PlayerLoginPayload>({
  username: '',
  password: ''
})
const registerForm = reactive<PlayerRegisterPayload>({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})
const submitting = ref(false)
const csrfLoading = ref(false)
const csrfReady = ref(false)
const csrfError = ref('')
const errorMessage = ref('')
let csrfPromise: Promise<void> | null = null

const loginRules: FormRules<PlayerLoginPayload> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { max: 20, message: '用户名不能超过 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { max: 64, message: '密码不能超过 64 个字符', trigger: 'blur' }
  ]
}

const registerRules: FormRules<PlayerRegisterPayload> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9_]{3,20}$/,
      message: '用户名需为 3–20 位字母、数字或下划线',
      trigger: 'blur'
    }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 64, message: '昵称不能超过 64 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码需为 6–64 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

function readTab(value: unknown): TabKey {
  return value === 'register' ? 'register' : 'login'
}

watch(
  () => route.query.tab,
  (value) => {
    activeTab.value = readTab(value)
  }
)

watch(
  () => registerForm.password,
  () => {
    if (registerForm.confirmPassword) {
      registerFormRef.value?.validateField('confirmPassword').catch(() => undefined)
    }
  }
)

function switchTab(tab: TabKey) {
  if (activeTab.value === tab) {
    return
  }
  activeTab.value = tab
  errorMessage.value = ''
  const query = { ...route.query }
  if (tab === 'register') {
    query.tab = 'register'
  } else {
    delete query.tab
  }
  void router.replace({ path: '/login', query })
}

function ensureCsrf(): Promise<void> {
  if (csrfReady.value) {
    return Promise.resolve()
  }
  if (csrfPromise) {
    return csrfPromise
  }

  csrfLoading.value = true
  csrfPromise = fetchPlayerCsrf()
    .then(() => {
      csrfReady.value = true
      csrfError.value = ''
    })
    .catch(() => {
      csrfReady.value = false
      csrfError.value = '无法获取安全校验令牌，请检查网络后重试'
      throw new Error('csrf-unavailable')
    })
    .finally(() => {
      csrfLoading.value = false
      csrfPromise = null
    })
  return csrfPromise
}

async function withCsrfRetry<T>(operation: () => Promise<T>): Promise<T> {
  await ensureCsrf()
  try {
    return await operation()
  } catch (error) {
    if (error instanceof ApiError && error.code === 'COMMON_CSRF_INVALID') {
      csrfReady.value = false
      await ensureCsrf()
      return operation()
    }
    throw error
  }
}

function resolveRedirect() {
  const redirect = route.query.redirect
  if (typeof redirect !== 'string' || !redirect.startsWith('/') || redirect.startsWith('//')) {
    return '/'
  }

  const path = redirect.split(/[?#]/)[0]
  if (path === '/login' || path === '/register' || path.startsWith('/admin')) {
    return '/'
  }
  return redirect
}

function resolveError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'COMMON_VALIDATION_FAILED' && error.fieldErrors.length > 0) {
      return error.fieldErrors[0]?.message ?? error.message
    }
    return AUTH_ERROR_MESSAGES[error.code] ?? error.message ?? '操作失败，请稍后重试'
  }
  if (error instanceof Error && error.message === 'csrf-unavailable') {
    return '无法获取安全校验令牌，请检查网络后重试'
  }
  return '操作失败，请稍后重试'
}

async function completeAuth(session: PlayerSession, message: string) {
  authStore.setUser(session.user)
  ElMessage.success(message)
  await router.replace(resolveRedirect())
}

async function handleLogin() {
  if (submitting.value || !loginFormRef.value) {
    return
  }

  errorMessage.value = ''
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    const session = await withCsrfRetry(() =>
      loginPlayer({
        username: loginForm.username.trim(),
        password: loginForm.password
      })
    )
    await completeAuth(session, `欢迎回来，${session.user.nickname}`)
  } catch (error) {
    errorMessage.value = resolveError(error)
  } finally {
    submitting.value = false
  }
}

async function handleRegister() {
  if (submitting.value || !registerFormRef.value) {
    return
  }

  errorMessage.value = ''
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    const session = await withCsrfRetry(() =>
      registerPlayer({
        username: registerForm.username.trim(),
        nickname: registerForm.nickname.trim(),
        password: registerForm.password,
        confirmPassword: registerForm.confirmPassword
      })
    )
    await completeAuth(session, `注册成功，欢迎你，${session.user.nickname}`)
  } catch (error) {
    errorMessage.value = resolveError(error)
  } finally {
    submitting.value = false
  }
}

void ensureCsrf().catch(() => undefined)
</script>

<template>
  <div class="auth-card">
    <header class="auth-card__head">
      <span class="auth-card__badge">
        <el-icon><Trophy /></el-icon>
        选手入口
      </span>
      <h1>{{ activeTab === 'login' ? '欢迎回来' : '创建账号' }}</h1>
      <p class="auth-card__desc">
        {{
          activeTab === 'login'
            ? '登录后继续学习路线、提交题目，并与对手展开实时对战。'
            : '加入题阵，沿学习路线逐步进阶，在题库和对战中验证成长。'
        }}
      </p>
    </header>

    <div class="auth-tabs" role="tablist" aria-label="选手账号">
      <button
        class="auth-tab"
        :class="{ 'is-active': activeTab === 'login' }"
        type="button"
        role="tab"
        :aria-selected="activeTab === 'login'"
        @click="switchTab('login')"
      >
        登录
      </button>
      <button
        class="auth-tab"
        :class="{ 'is-active': activeTab === 'register' }"
        type="button"
        role="tab"
        :aria-selected="activeTab === 'register'"
        @click="switchTab('register')"
      >
        注册
      </button>
    </div>

    <el-alert
      v-if="errorMessage"
      class="auth-alert"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
    />
    <el-alert
      v-else-if="csrfError"
      class="auth-alert"
      title="安全校验初始化失败"
      :description="`${csrfError}。提交时将自动重试。`"
      type="warning"
      :closable="false"
      show-icon
    />

    <el-form
      v-show="activeTab === 'login'"
      ref="loginFormRef"
      class="auth-form"
      :model="loginForm"
      :rules="loginRules"
      label-position="top"
      @submit.prevent="handleLogin"
    >
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="loginForm.username"
          :prefix-icon="User"
          name="username"
          placeholder="请输入用户名"
          autocomplete="username"
          maxlength="20"
          :disabled="submitting"
        />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="loginForm.password"
          :prefix-icon="Lock"
          name="password"
          type="password"
          placeholder="请输入密码"
          autocomplete="current-password"
          maxlength="64"
          show-password
          :disabled="submitting"
        />
      </el-form-item>

      <el-button
        class="auth-form__submit"
        type="primary"
        size="large"
        native-type="submit"
        :loading="submitting"
        :disabled="csrfLoading"
      >
        {{ submitting ? '正在登录…' : '登录' }}
      </el-button>
    </el-form>

    <el-form
      v-show="activeTab === 'register'"
      ref="registerFormRef"
      class="auth-form"
      :model="registerForm"
      :rules="registerRules"
      label-position="top"
      @submit.prevent="handleRegister"
    >
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="registerForm.username"
          :prefix-icon="User"
          name="username"
          placeholder="3–20 位字母、数字或下划线"
          autocomplete="username"
          maxlength="20"
          :disabled="submitting"
        />
      </el-form-item>

      <el-form-item label="昵称" prop="nickname">
        <el-input
          v-model="registerForm.nickname"
          placeholder="用于榜单、错题本等站内展示"
          maxlength="64"
          :disabled="submitting"
        />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="registerForm.password"
          :prefix-icon="Lock"
          name="password"
          type="password"
          placeholder="至少 6 位"
          autocomplete="new-password"
          maxlength="64"
          show-password
          :disabled="submitting"
        />
      </el-form-item>

      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="registerForm.confirmPassword"
          :prefix-icon="Lock"
          name="confirmPassword"
          type="password"
          placeholder="请再次输入密码"
          autocomplete="new-password"
          maxlength="64"
          show-password
          :disabled="submitting"
        />
      </el-form-item>

      <p class="auth-form__hint">注册成功后自动登录；用户名仅用于登录，昵称可在个人中心修改。</p>

      <el-button
        class="auth-form__submit"
        type="primary"
        size="large"
        native-type="submit"
        :loading="submitting"
        :disabled="csrfLoading"
      >
        {{ submitting ? '正在注册…' : '注册并登录' }}
      </el-button>
    </el-form>

    <div class="auth-security">
      <span><el-icon><Lock /></el-icon>密码加密</span>
      <span><el-icon><Key /></el-icon>CSRF 防护</span>
      <span><el-icon><Timer /></el-icon>失败限流</span>
    </div>

    <div class="auth-divider">相关入口</div>
    <footer class="auth-card__foot">
      <RouterLink to="/admin/login">管理员登录入口</RouterLink>
      <RouterLink to="/">返回首页</RouterLink>
    </footer>
  </div>
</template>
