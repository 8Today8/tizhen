<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Key, Refresh } from '@element-plus/icons-vue'

import {
  deleteLlmCredential,
  fetchLlmCredential,
  fetchLlmProviders,
  saveLlmCredential,
  type LlmCredential,
  type LlmProvider
} from '@/api/datagen'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatDateTime } from '@/utils/format'

const credential = ref<LlmCredential | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const dialogVisible = ref(false)
const providers = ref<LlmProvider[]>([])
const providersLoading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const dialogError = ref('')
const form = reactive({
  provider: 'deepseek',
  model: '',
  apiKey: ''
})

const activeProvider = computed(
  () => providers.value.find((provider) => provider.provider === form.provider) ?? null
)

watch(
  () => form.provider,
  () => {
    const provider = activeProvider.value
    if (provider && !provider.models.includes(form.model)) {
      form.model = provider.defaultModel
    }
  }
)

function displayTime(value: string | null): string {
  return value ? formatDateTime(value) : '—'
}

function statusTagType(status: LlmCredential['status']): 'success' | 'danger' {
  return status === 'valid' ? 'success' : 'danger'
}

async function loadCredential() {
  loading.value = true
  errorMessage.value = ''
  try {
    credential.value = await fetchLlmCredential()
  } catch (error) {
    credential.value = null
    errorMessage.value = resolveApiErrorMessage(error, 'LLM 凭证加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function openDialog() {
  dialogVisible.value = true
  dialogError.value = ''
  form.apiKey = ''
  if (!providers.value.length) {
    providersLoading.value = true
    try {
      providers.value = await fetchLlmProviders()
    } catch (error) {
      dialogError.value = resolveApiErrorMessage(error, 'LLM 服务商加载失败')
    } finally {
      providersLoading.value = false
    }
  }
  const current = credential.value
  if (current?.configured && current.provider) {
    form.provider = current.provider
    form.model = current.model ?? ''
  } else if (providers.value.length) {
    form.provider = providers.value[0].provider
    form.model = providers.value[0].defaultModel
  }
  if (!form.model && activeProvider.value) {
    form.model = activeProvider.value.defaultModel
  }
}

async function handleSave() {
  if (saving.value) {
    return
  }
  const apiKey = form.apiKey.trim()
  if (apiKey.length < 8) {
    ElMessage.warning('请填写有效的 API Key（至少 8 位）')
    return
  }
  if (!form.model) {
    ElMessage.warning('请选择模型')
    return
  }

  saving.value = true
  dialogError.value = ''
  try {
    credential.value = await saveLlmCredential({
      provider: form.provider,
      apiKey,
      model: form.model
    })
    form.apiKey = ''
    dialogVisible.value = false
    ElMessage.success('LLM 凭证已验证并保存')
  } catch (error) {
    dialogError.value = resolveApiErrorMessage(error, '凭证校验失败，请检查 API Key 与模型')
  } finally {
    saving.value = false
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm(
      '删除后需要重新配置凭证才能使用 LLM 造数据模式，随机模式不受影响。',
      '删除 LLM 凭证',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  deleting.value = true
  dialogError.value = ''
  try {
    await deleteLlmCredential()
    credential.value = await fetchLlmCredential()
    dialogVisible.value = false
    ElMessage.success('凭证已删除')
  } catch (error) {
    dialogError.value = resolveApiErrorMessage(error, '凭证删除失败')
  } finally {
    deleting.value = false
  }
}

onMounted(loadCredential)
</script>

<template>
  <section class="profile-panel">
    <header class="profile-panel__head">
      <span class="profile-panel__icon profile-panel__icon--mint"><el-icon><Key /></el-icon></span>
      <div>
        <h2>LLM 凭证</h2>
        <p>密钥加密保存，仅用于你发起的 AI 造数据任务，不会回显明文。</p>
      </div>
      <el-button :icon="Refresh" text :loading="loading" class="profile-panel__refresh" @click="loadCredential">
        刷新
      </el-button>
    </header>

    <div v-if="loading" class="profile-panel__body">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="errorMessage" class="profile-panel__body profile-panel__state">
      <p>{{ errorMessage }}</p>
      <el-button size="small" :icon="Refresh" @click="loadCredential">重新加载</el-button>
    </div>

    <div v-else-if="credential?.configured" class="profile-panel__body">
      <dl class="profile-credential">
        <div>
          <dt>服务商</dt>
          <dd>{{ credential.providerName }}</dd>
        </div>
        <div>
          <dt>模型</dt>
          <dd>{{ credential.model ?? '—' }}</dd>
        </div>
        <div>
          <dt>密钥</dt>
          <dd class="profile-mono">{{ credential.apiKeyMasked }}</dd>
        </div>
        <div>
          <dt>状态</dt>
          <dd>
            <el-tag :type="statusTagType(credential.status)" effect="plain" size="small">
              {{ credential.status === 'valid' ? '已验证' : '已失效' }}
            </el-tag>
          </dd>
        </div>
      </dl>
      <el-alert
        v-if="credential.status === 'invalid'"
        type="error"
        :closable="false"
        show-icon
        :title="credential.lastValidationError || '凭证已失效，请重新配置'"
      />
      <el-alert
        v-if="credential.hasActiveTask"
        type="warning"
        :closable="false"
        show-icon
        title="存在进行中的 LLM 造数据任务，任务结束前不能替换或删除凭证"
      />
      <p class="profile-panel__note">最近验证：{{ displayTime(credential.lastValidatedAt) }}</p>
      <div class="profile-panel__actions">
        <el-button type="primary" plain :icon="Key" :disabled="credential.hasActiveTask" @click="openDialog">
          更新凭证
        </el-button>
      </div>
    </div>

    <div v-else class="profile-panel__body profile-panel__empty">
      <p>尚未配置 LLM 凭证。随机造数据模式不受影响，两种 LLM 模式需要先配置。</p>
      <el-button type="primary" :icon="Key" @click="openDialog">配置 LLM 凭证</el-button>
    </div>

    <el-dialog v-model="dialogVisible" title="LLM 凭证" width="min(520px, 92vw)">
      <div v-loading="providersLoading" class="profile-dialog-body">
        <el-alert
          v-if="credential?.hasActiveTask"
          type="warning"
          :closable="false"
          show-icon
          title="正在进行的 LLM 造数据任务期间不能修改或删除凭证"
        />
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="保存前会调用一次服务商接口验证密钥；密钥只加密保存，不会回显。"
        />
        <el-form label-position="top">
          <el-form-item label="服务商" required>
            <el-select v-model="form.provider" class="profile-wide-control">
              <el-option
                v-for="provider in providers"
                :key="provider.provider"
                :label="provider.name"
                :value="provider.provider"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="模型" required>
            <el-select v-model="form.model" class="profile-wide-control">
              <el-option
                v-for="model in activeProvider?.models ?? []"
                :key="model"
                :label="model"
                :value="model"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="API Key" required>
            <el-input
              v-model="form.apiKey"
              type="password"
              show-password
              autocomplete="off"
              placeholder="粘贴服务商控制台生成的 API Key"
            />
          </el-form-item>
        </el-form>
        <el-alert
          v-if="dialogError"
          type="error"
          :closable="false"
          show-icon
          :title="dialogError"
        />
      </div>
      <template #footer>
        <div class="profile-dialog-footer">
          <el-button
            v-if="credential?.configured"
            type="danger"
            plain
            :loading="deleting"
            :disabled="credential?.hasActiveTask"
            @click="handleDelete"
          >
            删除凭证
          </el-button>
          <span v-else></span>
          <div>
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button
              type="primary"
              :loading="saving"
              :disabled="credential?.hasActiveTask"
              @click="handleSave"
            >
              验证并保存
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </section>
</template>
