<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules, TableInstance } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheck,
  CircleClose,
  Delete,
  Key,
  Plus,
  Refresh,
  Search,
  View
} from '@element-plus/icons-vue'

import {
  batchDeleteAdminUsers,
  batchUpdateAdminUserStatus,
  createAdminUser,
  fetchAdminUserDetail,
  fetchAdminUsers,
  resetAdminUserPassword,
  updateAdminUserAvatar,
  updateAdminUserStatus,
  uploadAdminAvatar,
  type AdminUserDetail,
  type AdminUserSummary
} from '@/api/admin'
import AvatarUploader from '@/components/base/AvatarUploader.vue'
import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminPage from '@/components/admin/AdminPage.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import AdminTableCard from '@/components/admin/AdminTableCard.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'
import { useAuthStore } from '@/stores/auth'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatDateTime } from '@/utils/format'

const authStore = useAuthStore()
const canManageUsers = computed(() => authStore.admin?.roleCode === 'super_admin')

const keyword = ref('')
const statusFilter = ref<'' | 'active' | 'disabled'>('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)
const users = ref<AdminUserSummary[]>([])
const userTableRef = ref<TableInstance>()
const selectedUsers = ref<AdminUserSummary[]>([])

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<AdminUserDetail | null>(null)

const createVisible = ref(false)
const createSubmitting = ref(false)
const createAvatarUploading = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  school: '',
  avatarUrl: null as string | null
})
const createRules: FormRules = {
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
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码需为 6–64 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== createForm.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
  phone: [{ max: 32, message: '手机号不能超过 32 个字符', trigger: 'blur' }],
  school: [{ max: 128, message: '学校不能超过 128 个字符', trigger: 'blur' }]
}

const resetVisible = ref(false)
const resetSubmitting = ref(false)
const resetFormRef = ref<FormInstance>()
const resetTarget = ref<AdminUserSummary | null>(null)
const resetForm = reactive({
  newPassword: '',
  confirmPassword: ''
})
const resetRules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码需为 6–64 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== resetForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

function queryParams() {
  return {
    keyword: keyword.value.trim() || undefined,
    status: statusFilter.value || undefined,
    page: page.value,
    pageSize: pageSize.value
  }
}

function handleSelectionChange(selection: AdminUserSummary[]) {
  selectedUsers.value = selection
}

function clearSelection() {
  userTableRef.value?.clearSelection()
  selectedUsers.value = []
}

async function loadUsers() {
  loading.value = true
  try {
    const result = await fetchAdminUsers(queryParams())
    users.value = result.items
    total.value = result.total
  } catch (error) {
    users.value = []
    total.value = 0
    ElMessage.error(resolveApiErrorMessage(error, '用户列表加载失败'))
  } finally {
    loading.value = false
    clearSelection()
  }
}

function handleSearch() {
  page.value = 1
  void loadUsers()
}

function handleReset() {
  keyword.value = ''
  statusFilter.value = ''
  handleSearch()
}

async function openDetail(userId: string) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await fetchAdminUserDetail(userId)
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '用户详情加载失败'))
  } finally {
    detailLoading.value = false
  }
}

async function toggleStatus(row: AdminUserSummary) {
  const disabling = row.status === 'active'
  try {
    await ElMessageBox.confirm(
      disabling
        ? `禁用后 ${row.nickname} 将立即退出登录并从榜单移除，历史数据保留。确认禁用吗？`
        : `确认恢复 ${row.nickname} 的账号使用权限吗？`,
      disabling ? '禁用用户' : '启用用户',
      {
        confirmButtonText: disabling ? '确认禁用' : '确认启用',
        cancelButtonText: '取消',
        type: disabling ? 'warning' : 'info'
      }
    )
  } catch {
    return
  }

  try {
    await updateAdminUserStatus(row.id, disabling ? 'disabled' : 'active')
    ElMessage.success(disabling ? '用户已禁用' : '用户已启用')
    await loadUsers()
    if (detail.value?.user.id === row.id) {
      detail.value = await fetchAdminUserDetail(row.id)
    }
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '状态更新失败'))
  }
}

function openCreateDialog() {
  createForm.username = ''
  createForm.nickname = ''
  createForm.password = ''
  createForm.confirmPassword = ''
  createForm.email = ''
  createForm.phone = ''
  createForm.school = ''
  createForm.avatarUrl = null
  createAvatarUploading.value = false
  createVisible.value = true
  createFormRef.value?.clearValidate()
}

async function submitCreate() {
  const form = createFormRef.value
  if (!form) {
    return
  }
  const valid = await form.validate().catch(() => false)
  if (!valid) {
    return
  }
  createSubmitting.value = true
  try {
    const user = await createAdminUser({
      username: createForm.username.trim(),
      nickname: createForm.nickname.trim(),
      password: createForm.password,
      email: createForm.email.trim() || undefined,
      phone: createForm.phone.trim() || undefined,
      school: createForm.school.trim() || undefined,
      avatarUrl: createForm.avatarUrl || undefined
    })
    createVisible.value = false
    page.value = 1
    ElMessage.success(`已新增用户 ${user.username}`)
    await loadUsers()
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '用户创建失败'))
  } finally {
    createSubmitting.value = false
  }
}

async function handleDetailAvatarChange(avatarUrl: string | null) {
  if (!detail.value) {
    return
  }
  const userId = detail.value.user.id
  try {
    const updated = await updateAdminUserAvatar(userId, avatarUrl)
    detail.value = { ...detail.value, user: updated }
    ElMessage.success(avatarUrl ? '头像已更新' : '头像已移除')
    await loadUsers()
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '头像更新失败'))
  }
}

function openResetDialog(row: AdminUserSummary) {
  resetTarget.value = row
  resetForm.newPassword = ''
  resetForm.confirmPassword = ''
  resetVisible.value = true
  resetFormRef.value?.clearValidate()
}

async function submitReset() {
  const form = resetFormRef.value
  if (!form || !resetTarget.value) {
    return
  }
  const valid = await form.validate().catch(() => false)
  if (!valid) {
    return
  }
  const target = resetTarget.value
  resetSubmitting.value = true
  try {
    await resetAdminUserPassword(target.id, resetForm.newPassword)
    resetVisible.value = false
    ElMessage.success(`已重置 ${target.nickname}（${target.username}）的密码`)
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '密码重置失败'))
  } finally {
    resetSubmitting.value = false
  }
}

async function batchToggleStatus(status: 'active' | 'disabled') {
  const targets = selectedUsers.value.filter((user) => user.status !== status)
  if (!targets.length) {
    ElMessage.info(status === 'disabled' ? '所选用户均已禁用' : '所选用户均已启用')
    return
  }
  const disabling = status === 'disabled'
  try {
    await ElMessageBox.confirm(
      disabling
        ? `禁用后这 ${targets.length} 位用户将立即退出登录并从榜单移除，历史数据保留。确认批量禁用吗？`
        : `确认恢复这 ${targets.length} 位用户的账号使用权限吗？`,
      disabling ? '批量禁用用户' : '批量启用用户',
      {
        confirmButtonText: disabling ? '确认禁用' : '确认启用',
        cancelButtonText: '取消',
        type: disabling ? 'warning' : 'info'
      }
    )
  } catch {
    return
  }

  try {
    await batchUpdateAdminUserStatus(
      targets.map((user) => user.id),
      status
    )
    ElMessage.success(disabling ? `已禁用 ${targets.length} 位用户` : `已启用 ${targets.length} 位用户`)
    await loadUsers()
    await refreshDetailIfAffected(targets.map((user) => user.id))
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, disabling ? '批量禁用失败' : '批量启用失败'))
  }
}

async function batchDeleteSelected() {
  const targets = selectedUsers.value.slice()
  if (!targets.length) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `删除后这 ${targets.length} 位用户将从用户管理移除并立即退出登录，历史成绩与关联数据保留。确认批量删除吗？`,
      '批量删除用户',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  const targetIds = targets.map((user) => user.id)
  try {
    await batchDeleteAdminUsers(targetIds)
    if (detail.value && targetIds.includes(detail.value.user.id)) {
      detailVisible.value = false
      detail.value = null
    }
    if (users.value.length === targets.length && page.value > 1) {
      page.value -= 1
    }
    ElMessage.success(`已删除 ${targets.length} 位用户`)
    await loadUsers()
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '批量删除失败'))
  }
}

async function refreshDetailIfAffected(userIds: string[]) {
  if (!detailVisible.value || !detail.value || !userIds.includes(detail.value.user.id)) {
    return
  }
  try {
    detail.value = await fetchAdminUserDetail(detail.value.user.id)
  } catch {
    detailVisible.value = false
    detail.value = null
  }
}

function displayTime(value: string | null): string {
  return value ? formatDateTime(value) : '—'
}

function statusTagType(status: string): 'success' | 'info' {
  return status === 'active' ? 'success' : 'info'
}

onMounted(loadUsers)
</script>

<template>
  <AdminPage
    title="用户管理"
    description="查看选手列表与详情；新增、批量删除、批量启用/禁用与重置密码仅超级管理员可操作，内容运营只能查看。"
  >
    <AdminTableCard
      title="用户列表"
      description="按条件筛选用户；超级管理员可新增用户，或勾选多条记录执行批量操作。"
    >
      <template #head-extra>
        <el-button v-if="canManageUsers" type="primary" :icon="Plus" @click="openCreateDialog">
          新增用户
        </el-button>
      </template>

      <template #toolbar>
        <AdminToolbar>
          <el-input
            v-model="keyword"
            class="admin-search"
            placeholder="搜索用户名、昵称、学校或 CF Handle"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="statusFilter"
            class="admin-filter"
            placeholder="全部状态"
            @change="handleSearch"
          >
            <el-option label="全部状态" value="" />
            <el-option label="正常" value="active" />
            <el-option label="已禁用" value="disabled" />
          </el-select>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <template #meta>共 {{ total }} 位用户</template>
        </AdminToolbar>
      </template>

      <div v-if="canManageUsers" class="admin-batch-bar">
        <span class="admin-batch-bar__summary">
          已选 <strong>{{ selectedUsers.length }}</strong> 位用户
        </span>
        <div class="admin-batch-bar__actions">
          <el-button
            size="small"
            :icon="CircleClose"
            :disabled="!selectedUsers.length"
            @click="batchToggleStatus('disabled')"
          >
            批量禁用
          </el-button>
          <el-button
            size="small"
            :icon="CircleCheck"
            :disabled="!selectedUsers.length"
            @click="batchToggleStatus('active')"
          >
            批量启用
          </el-button>
          <el-button
            size="small"
            type="danger"
            plain
            :icon="Delete"
            :disabled="!selectedUsers.length"
            @click="batchDeleteSelected"
          >
            批量删除
          </el-button>
          <el-button link :disabled="!selectedUsers.length" @click="clearSelection">
            取消选择
          </el-button>
        </div>
      </div>

      <el-table
        ref="userTableRef"
        v-loading="loading"
        :data="users"
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column v-if="canManageUsers" type="selection" width="46" fixed="left" />
        <el-table-column label="用户" min-width="190">
          <template #default="{ row }">
            <div class="admin-user-cell">
              <el-avatar :size="32" :src="row.avatarUrl || undefined">
                {{ row.nickname?.slice(0, 1) }}
              </el-avatar>
              <span class="admin-user-cell__text">
                <strong>{{ row.nickname }}</strong>
                <small>{{ row.username }} · ID {{ row.id }}</small>
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="联系方式" min-width="190">
          <template #default="{ row }">
            <div class="admin-contact-cell">
              <span>{{ row.email || '未绑定邮箱' }}</span>
              <small>{{ row.phone || '未填写手机号' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="school" label="学校" min-width="150">
          <template #default="{ row }">{{ row.school || '—' }}</template>
        </el-table-column>
        <el-table-column label="CF Handle" min-width="130">
          <template #default="{ row }">
            <el-tag v-if="row.cfHandle" type="info" effect="plain">{{ row.cfHandle }}</el-tag>
            <span v-else class="admin-muted">未绑定</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain">
              {{ row.status === 'active' ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="170">
          <template #default="{ row }">{{ displayTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row.id)">详情</el-button>
            <el-button
              v-if="canManageUsers"
              link
              type="primary"
              :icon="Key"
              @click="openResetDialog(row)"
            >
              重置密码
            </el-button>
            <el-button
              v-if="canManageUsers"
              link
              :type="row.status === 'active' ? 'danger' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <AdminEmptyState description="没有符合条件的用户" />
        </template>
      </el-table>

      <template #footer>
        <AdminPagination
          v-model:page="page"
          v-model:page-size="pageSize"
          :total="total"
          @change="loadUsers"
        />
      </template>
    </AdminTableCard>

    <el-dialog v-model="createVisible" title="新增用户" width="520px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="92px">
        <el-form-item label="头像">
          <AvatarUploader
            v-model="createForm.avatarUrl"
            :uploader="uploadAdminAvatar"
            :size="76"
            @update:uploading="createAvatarUploading = $event"
          />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" maxlength="20" placeholder="3–20 位字母、数字或下划线" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="createForm.nickname" maxlength="64" placeholder="站内展示昵称" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password maxlength="64" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="createForm.confirmPassword" type="password" show-password maxlength="64" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="createForm.email" maxlength="254" placeholder="选填" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="createForm.phone" maxlength="32" placeholder="选填，仅展示" />
        </el-form-item>
        <el-form-item label="学校" prop="school">
          <el-input v-model="createForm.school" maxlength="128" placeholder="选填" />
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="新用户创建后默认启用，初始密码仅用于账号登录，请妥善告知用户。"
        />
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="createSubmitting"
          :disabled="createAvatarUploading"
          @click="submitCreate"
        >
          创建用户
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetVisible" title="重置用户密码" width="440px" destroy-on-close>
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="92px">
        <el-form-item label="目标用户">
          <el-input
            :model-value="resetTarget ? `${resetTarget.nickname}（${resetTarget.username}）` : ''"
            disabled
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetForm.newPassword" type="password" show-password maxlength="64" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="resetForm.confirmPassword" type="password" show-password maxlength="64" />
        </el-form-item>
        <el-alert
          type="warning"
          :closable="false"
          show-icon
          title="重置后该用户已登录的会话立即失效，需要使用新密码重新登录。"
        />
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetSubmitting" @click="submitReset">确认重置</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="detailVisible"
      class="admin-detail-drawer"
      title="用户详情"
      size="580px"
    >
      <div v-loading="detailLoading" class="admin-user-detail">
        <template v-if="detail">
          <div class="admin-user-detail__head">
            <AvatarUploader
              :model-value="detail.user.avatarUrl"
              :uploader="uploadAdminAvatar"
              :size="60"
              @update:model-value="handleDetailAvatarChange"
            />
            <div class="admin-user-detail__identity">
              <h3>{{ detail.user.nickname }}</h3>
              <p>{{ detail.user.username }} · ID {{ detail.user.id }}</p>
            </div>
            <el-tag :type="statusTagType(detail.user.status)" effect="plain">
              {{ detail.user.status === 'active' ? '正常' : '已禁用' }}
            </el-tag>
          </div>

          <div class="admin-stat-grid">
            <div class="admin-stat-card"><strong>{{ detail.submissionCount }}</strong><span>提交数</span></div>
            <div class="admin-stat-card"><strong>{{ detail.firstAcCount }}</strong><span>首次 AC</span></div>
            <div class="admin-stat-card"><strong>{{ detail.wrongBookCount }}</strong><span>错题本</span></div>
            <div class="admin-stat-card"><strong>{{ detail.dataGenTaskCount }}</strong><span>造数据任务</span></div>
            <div class="admin-stat-card"><strong>{{ detail.battleCount }}</strong><span>对战数</span></div>
          </div>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="邮箱">{{ detail.user.email || '未绑定' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ detail.user.phone || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="学校">{{ detail.user.school || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="CF Handle">{{ detail.user.cfHandle || '未绑定' }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ displayTime(detail.user.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="最近登录">{{ displayTime(detail.user.lastLoginAt) }}</el-descriptions-item>
          </el-descriptions>

          <section class="admin-user-detail__section">
            <h4>LLM 凭证</h4>
            <el-descriptions v-if="detail.llmCredential" :column="2" border>
              <el-descriptions-item label="配置状态">
                <el-tag :type="detail.llmCredential.configured ? 'success' : 'info'" effect="plain">
                  {{ detail.llmCredential.configured ? '可用' : '未配置/不可用' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="服务商">
                {{ detail.llmCredential.providerName || '—' }}
              </el-descriptions-item>
              <el-descriptions-item label="模型">{{ detail.llmCredential.model || '默认模型' }}</el-descriptions-item>
              <el-descriptions-item label="最近校验">
                {{ displayTime(detail.llmCredential.lastValidatedAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="校验失败原因" :span="2">
                {{ detail.llmCredential.lastValidationError || '—' }}
              </el-descriptions-item>
            </el-descriptions>
            <el-empty v-else description="尚未配置 LLM 凭证" :image-size="48" />
          </section>

          <section class="admin-user-detail__section">
            <h4>CF 绑定历史</h4>
            <el-table :data="detail.cfAccounts" size="small">
              <el-table-column prop="handle" label="Handle" min-width="110" />
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'active' ? 'success' : 'info'" effect="plain">
                    {{ row.status === 'active' ? '有效' : '已解绑' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="本周" width="80" align="center">
                <template #default="{ row }">{{ row.weeklyCount }}</template>
              </el-table-column>
              <el-table-column label="累计" width="80" align="center">
                <template #default="{ row }">{{ row.allTimeCount }}</template>
              </el-table-column>
              <el-table-column label="最近同步" min-width="150">
                <template #default="{ row }">{{ displayTime(row.lastSyncAt) }}</template>
              </el-table-column>
              <template #empty>
                <AdminEmptyState description="暂无 CF 绑定记录" />
              </template>
            </el-table>
          </section>
        </template>
        <AdminEmptyState v-else-if="!detailLoading" description="未能加载用户详情" />
      </div>
    </el-drawer>
  </AdminPage>
</template>

<style scoped>
.admin-batch-bar {
  display: flex;
  margin: 0 20px 14px;
  padding: 10px 14px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid var(--tz-color-border);
  border-radius: var(--tz-radius-sm);
  background: var(--tz-color-surface-muted);
}

.admin-batch-bar__summary {
  color: var(--tz-color-text-secondary);
  font-size: 13px;
}

.admin-batch-bar__summary strong {
  color: var(--tz-color-primary);
  font-size: 15px;
}

.admin-batch-bar__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.admin-user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-user-cell__text,
.admin-contact-cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
  line-height: 1.4;
}

.admin-user-cell__text strong {
  font-size: 13px;
}

.admin-user-cell__text small,
.admin-contact-cell small,
.admin-muted {
  overflow: hidden;
  color: var(--tz-color-text-muted);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-user-detail {
  display: flex;
  min-height: 200px;
  flex-direction: column;
  gap: 18px;
}

.admin-user-detail__head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-user-detail__identity {
  min-width: 0;
  flex: 1;
}

.admin-user-detail__head h3 {
  margin: 0;
  font-size: 18px;
}

.admin-user-detail__head p {
  margin: 4px 0 0;
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

.admin-stat-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.admin-stat-card {
  display: flex;
  padding: 12px 8px;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  border: 1px solid var(--tz-color-border);
  border-radius: var(--tz-radius-sm);
  background: var(--tz-color-surface-muted);
}

.admin-stat-card strong {
  font-size: 18px;
}

.admin-stat-card span {
  color: var(--tz-color-text-muted);
  font-size: 11px;
}

.admin-user-detail__section h4 {
  margin: 0 0 10px;
  font-size: 14px;
}

@media (max-width: 640px) {
  .admin-stat-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
