<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Key, Lock, Monitor, Timer, User } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import {
  fetchAdminCsrf,
  loginAdmin,
  type AdminLoginPayload,
  type AdminSession
} from '@/api/admin'
import { ApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const LOGIN_ERROR_MESSAGES: Record<string, string> = {
  AUTH_INVALID_CREDENTIALS: '账号或密码不正确，请重新输入',
  ADMIN_ACCOUNT_DISABLED: '该管理员账号已被停用，请联系超级管理员',
  COMMON_RATE_LIMITED: '登录尝试过于频繁，请稍后再试',
  COMMON_CSRF_INVALID: '安全校验已失效，请重新登录',
  COMMON_VALIDATION_FAILED: '请输入有效的管理员账号与密码',
  CLIENT_TIMEOUT: '请求超时，请稍后重试',
  CLIENT_NETWORK_ERROR: '网络连接异常，请检查网络后重试'
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const form = reactive<AdminLoginPayload>({
  username: '',
  password: ''
})
const submitting = ref(false)
const csrfLoading = ref(false)
const csrfReady = ref(false)
const csrfError = ref('')
const errorMessage = ref('')

const rules: FormRules<AdminLoginPayload> = {
  username: [
    { required: true, message: '请输入管理员账号', trigger: 'blur' },
    { max: 32, message: '管理员账号不能超过 32 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { max: 64, message: '密码不能超过 64 个字符', trigger: 'blur' }
  ]
}

async function prepareCsrf() {
  if (csrfLoading.value) {
    return
  }
  csrfLoading.value = true
  try {
    await fetchAdminCsrf()
    csrfReady.value = true
    csrfError.value = ''
  } catch {
    csrfReady.value = false
    csrfError.value = '无法获取安全校验令牌，请检查网络后重试'
    throw new Error('csrf-unavailable')
  } finally {
    csrfLoading.value = false
  }
}

function resolveRedirect() {
  const redirect = route.query.redirect
  if (typeof redirect !== 'string' || !redirect.startsWith('/admin')) {
    return '/admin'
  }

  const path = redirect.split(/[?#]/)[0]
  if (path !== '/admin' && !path.startsWith('/admin/')) {
    return '/admin'
  }
  return path === '/admin/login' ? '/admin' : redirect
}

function resolveError(error: unknown) {
  if (error instanceof ApiError) {
    if (error.code === 'COMMON_CSRF_INVALID') {
      csrfReady.value = false
    }
    return LOGIN_ERROR_MESSAGES[error.code] ?? error.message ?? '登录失败，请稍后重试'
  }
  return '登录失败，请稍后重试'
}

async function signIn() {
  if (!csrfReady.value) {
    await prepareCsrf()
  }
  return loginAdmin({
    username: form.username.trim(),
    password: form.password
  })
}

async function completeLogin(session: AdminSession) {
  authStore.setAdmin(session)
  ElMessage.success(`欢迎回来，${session.displayName}`)
  await router.replace(resolveRedirect())
}

async function handleSubmit() {
  if (submitting.value || csrfLoading.value || !formRef.value) {
    return
  }

  errorMessage.value = ''
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    await completeLogin(await signIn())
  } catch (error) {
    // CSRF Token 失效时重新获取并重试一次。
    if (error instanceof ApiError && error.code === 'COMMON_CSRF_INVALID') {
      csrfReady.value = false
      try {
        await completeLogin(await signIn())
        return
      } catch (retryError) {
        errorMessage.value = resolveError(retryError)
        return
      }
    }
    errorMessage.value = resolveError(error)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  prepareCsrf().catch(() => undefined)
})
</script>

<template>
  <div class="auth-card">
    <header class="auth-card__head">
      <span class="auth-card__badge">
        <el-icon><Monitor /></el-icon>
        运营后台
      </span>
      <h1>管理员登录</h1>
      <p class="auth-card__desc">使用后台账号进入运营控制台，管理学习内容、题库与平台运营。</p>
    </header>

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
      :description="`${csrfError}。提交登录时将自动重试。`"
      type="warning"
      :closable="false"
      show-icon
    />

    <el-form
      ref="formRef"
      class="auth-form"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent="handleSubmit"
    >
      <el-form-item label="管理员账号" prop="username">
        <el-input
          v-model="form.username"
          :prefix-icon="User"
          name="username"
          placeholder="请输入管理员账号"
          autocomplete="username"
          maxlength="32"
          :disabled="submitting"
        />
      </el-form-item>

      <el-form-item label="登录密码" prop="password">
        <el-input
          v-model="form.password"
          :prefix-icon="Lock"
          name="password"
          type="password"
          placeholder="请输入登录密码"
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
        {{ submitting ? '正在验证…' : '登录后台' }}
      </el-button>
    </el-form>

    <div class="auth-security">
      <span><el-icon><Lock /></el-icon>独立会话</span>
      <span><el-icon><Key /></el-icon>CSRF 防护</span>
      <span><el-icon><Timer /></el-icon>失败限流</span>
    </div>

    <div class="auth-divider">相关入口</div>
    <footer class="auth-card__foot">
      <RouterLink to="/login">选手登录</RouterLink>
      <RouterLink to="/">返回首页</RouterLink>
    </footer>
  </div>
</template>
