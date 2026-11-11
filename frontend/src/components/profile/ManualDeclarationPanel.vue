<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { EditPen, Plus, Refresh, Tickets } from '@element-plus/icons-vue'

import {
  deleteManualDeclaration,
  fetchMyManualDeclarations,
  upsertManualDeclaration,
  type ManualDeclaration
} from '@/api/external'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatDateTime } from '@/utils/format'
import { declarationStatusLabel, declarationStatusType } from '@/utils/leaderboard'

const emit = defineEmits<{ changed: [] }>()

const SUGGESTED_PLATFORMS = ['牛客', '洛谷', 'AtCoder']

const declarations = ref<ManualDeclaration[]>([])
const weekStartDate = ref('')
const loading = ref(true)
const errorMessage = ref('')
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  platform: '牛客',
  account: '',
  profileUrl: '',
  count: 0
})
const rules: FormRules = {
  platform: [
    { required: true, message: '请选择或输入平台', trigger: 'change' },
    { max: 32, message: '平台名称不能超过 32 个字符', trigger: 'blur' }
  ],
  account: [
    { required: true, message: '请输入平台账号', trigger: 'blur' },
    { max: 128, message: '平台账号不能超过 128 个字符', trigger: 'blur' }
  ],
  count: [{ required: true, message: '请输入本周过题数', trigger: 'change' }]
}

function displayTime(value: string): string {
  return formatDateTime(value)
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

async function loadDeclarations() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await fetchMyManualDeclarations({ page: 1, pageSize: 100 })
    declarations.value = result.items
    weekStartDate.value = result.weekStartDate
  } catch (error) {
    declarations.value = []
    errorMessage.value = resolveApiErrorMessage(error, '本周申报记录加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.platform = '牛客'
  form.account = ''
  form.profileUrl = ''
  form.count = 0
  formRef.value?.clearValidate()
}

function openDialog(declaration?: ManualDeclaration) {
  resetForm()
  if (declaration) {
    form.platform = declaration.platform
    form.account = declaration.account
    form.profileUrl = declaration.profileUrl ?? ''
    form.count = declaration.count
  }
  dialogVisible.value = true
}

async function submit() {
  if (submitting.value) {
    return
  }
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (!isValidOptionalUrl(form.profileUrl)) {
    ElMessage.warning('主页地址需为合法的 http/https URL，且不超过 512 个字符')
    return
  }

  submitting.value = true
  try {
    await upsertManualDeclaration({
      platform: form.platform.trim(),
      account: form.account.trim(),
      profileUrl: form.profileUrl.trim() || null,
      count: form.count
    })
    ElMessage.success('申报已保存，审核通过后计入榜单')
    dialogVisible.value = false
    await loadDeclarations()
    emit('changed')
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '保存申报失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

async function handleDelete(declaration: ManualDeclaration) {
  try {
    await ElMessageBox.confirm(
      `确认删除「${declaration.platform}」本周的申报记录？删除后立即不计入榜单。`,
      '删除手动申报',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  try {
    await deleteManualDeclaration(declaration.id)
    ElMessage.success('申报已删除')
    await loadDeclarations()
    emit('changed')
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '删除申报失败，请稍后重试'))
  }
}

onMounted(loadDeclarations)
</script>

<template>
  <section class="profile-panel">
    <header class="profile-panel__head">
      <span class="profile-panel__icon profile-panel__icon--amber"><el-icon><Tickets /></el-icon></span>
      <div>
        <h2>其他平台申报</h2>
        <p>
          按北京时间自然周申报；同一平台重复提交会覆盖并重新审核。
          <span v-if="weekStartDate">当前统计周 {{ weekStartDate }} 起。</span>
        </p>
      </div>
      <div class="profile-panel__head-actions">
        <el-button :icon="Refresh" text :loading="loading" @click="loadDeclarations">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增申报</el-button>
      </div>
    </header>

    <div v-if="loading" class="profile-panel__body">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="errorMessage" class="profile-panel__body profile-panel__state">
      <p>{{ errorMessage }}</p>
      <el-button size="small" :icon="Refresh" @click="loadDeclarations">重新加载</el-button>
    </div>

    <div v-else-if="declarations.length === 0" class="profile-panel__body profile-panel__empty">
      <p>本周还没有手动申报记录。未审核或已拒绝的申报不会计入榜单。</p>
      <el-button type="primary" plain :icon="Plus" @click="openDialog()">申报本周成绩</el-button>
    </div>

    <ul v-else class="profile-declarations">
      <li v-for="declaration in declarations" :key="declaration.id" class="profile-declaration">
        <div class="profile-declaration__main">
          <div class="profile-declaration__title">
            <strong>{{ declaration.platform }}</strong>
            <span>{{ declaration.account }}</span>
            <el-tag :type="declarationStatusType(declaration.verificationStatus)" effect="plain" size="small">
              {{ declarationStatusLabel(declaration.verificationStatus) }}
            </el-tag>
          </div>
          <p>
            本周过题 <strong>{{ declaration.count }}</strong> · 更新于
            {{ displayTime(declaration.updatedAt) }}
          </p>
        </div>
        <div class="profile-declaration__actions">
          <el-button size="small" text :icon="EditPen" @click="openDialog(declaration)">编辑</el-button>
          <el-button size="small" text type="danger" @click="handleDelete(declaration)">删除</el-button>
        </div>
      </li>
    </ul>

    <el-dialog v-model="dialogVisible" title="手动申报本周过题数" width="min(540px, 92vw)">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="申报范围为当前自然周；审核通过前不会计入榜单。"
      />
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <div class="profile-form-grid">
          <el-form-item label="平台" prop="platform">
            <el-select
              v-model="form.platform"
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入平台"
            >
              <el-option
                v-for="platform in SUGGESTED_PLATFORMS"
                :key="platform"
                :label="platform"
                :value="platform"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="本周过题数" prop="count">
            <el-input-number
              v-model="form.count"
              :min="0"
              :max="100000"
              :step="1"
              step-strictly
              controls-position="right"
            />
          </el-form-item>
        </div>
        <el-form-item label="平台账号" prop="account">
          <el-input v-model="form.account" maxlength="128" placeholder="账号名或用户 ID" />
        </el-form-item>
        <el-form-item label="主页地址（选填）">
          <el-input v-model="form.profileUrl" maxlength="512" placeholder="https://..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存申报</el-button>
      </template>
    </el-dialog>
  </section>
</template>
