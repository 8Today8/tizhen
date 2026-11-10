<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Key, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

import {
  createAdminAccount,
  fetchAdminAccounts,
  fetchAdminRoles,
  resetAdminAccountPassword,
  updateAdminAccountStatus,
  type AdminAccountView,
  type RoleView
} from '@/api/admin'
import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminPage from '@/components/admin/AdminPage.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import AdminTableCard from '@/components/admin/AdminTableCard.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'
import { useAuthStore } from '@/stores/auth'
import { roleLabel } from '@/utils/adminLabels'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const authStore = useAuthStore()
const canManage = computed(() => authStore.admin?.roleCode === 'super_admin')

const roleLoading = ref(false)
const roles = ref<RoleView[]>([])

const keyword = ref('')
const statusFilter = ref('')
const roleFilter = ref('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const accountsLoading = ref(false)
const accounts = ref<AdminAccountView[]>([])

const createVisible = ref(false)
const createSubmitting = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({
  username: '',
  displayName: '',
  password: '',
  confirmPassword: ''
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
  displayName: [
    { required: true, message: '请输入显示名称', trigger: 'blur' },
    { max: 64, message: '显示名称不能超过 64 个字符', trigger: 'blur' }
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
  ]
}

const resetVisible = ref(false)
const resetSubmitting = ref(false)
const resetFormRef = ref<FormInstance>()
const resetTarget = ref<AdminAccountView | null>(null)
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

const roleDescriptions: Record<string, string> = {
  super_admin: '拥有全部后台权限，包括用户状态、CF 刷新与管理员账号管理。',
  content_operator: '可查看用户、榜单、造数据任务与操作日志，不能执行高危写操作。'
}

function formatPermissions(permissions: string[]): string[] {
  if (permissions.includes('*')) {
    return ['全部权限']
  }
  return permissions
}

async function loadRoles() {
  if (!canManage.value) {
    return
  }
  roleLoading.value = true
  try {
    roles.value = await fetchAdminRoles()
  } catch (error) {
    roles.value = []
    ElMessage.error(resolveApiErrorMessage(error, '角色列表加载失败'))
  } finally {
    roleLoading.value = false
  }
}

async function loadAccounts() {
  if (!canManage.value) {
    return
  }
  accountsLoading.value = true
  try {
    const result = await fetchAdminAccounts({
      keyword: keyword.value.trim() || undefined,
      status: statusFilter.value || undefined,
      roleCode: roleFilter.value || undefined,
      page: page.value,
      pageSize: pageSize.value
    })
    accounts.value = result.items
    total.value = result.total
  } catch (error) {
    accounts.value = []
    total.value = 0
    ElMessage.error(resolveApiErrorMessage(error, '管理员账号加载失败'))
  } finally {
    accountsLoading.value = false
  }
}

function handleSearch() {
  page.value = 1
  void loadAccounts()
}

function handleResetFilters() {
  keyword.value = ''
  statusFilter.value = ''
  roleFilter.value = ''
  handleSearch()
}

function openCreateDialog() {
  createForm.username = ''
  createForm.displayName = ''
  createForm.password = ''
  createForm.confirmPassword = ''
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
    const account = await createAdminAccount({
      username: createForm.username.trim(),
      displayName: createForm.displayName.trim(),
      password: createForm.password,
      roleCode: 'content_operator'
    })
    ElMessage.success(`已创建内容运营账号 ${account.username}`)
    createVisible.value = false
    await loadAccounts()
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '管理员账号创建失败'))
  } finally {
    createSubmitting.value = false
  }
}

function openResetDialog(row: AdminAccountView) {
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
    await resetAdminAccountPassword(target.id, resetForm.newPassword)
    resetVisible.value = false
    ElMessage.success(`已重置 ${target.username} 的密码，原会话已失效`)
    if (authStore.admin?.id === target.id) {
      authStore.setAdmin(null)
      await router.replace({ name: 'admin-login' })
      return
    }
    await loadAccounts()
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '密码重置失败'))
  } finally {
    resetSubmitting.value = false
  }
}

async function toggleStatus(row: AdminAccountView) {
  const disabling = row.status === 'active'
  try {
    await ElMessageBox.confirm(
      disabling
        ? `禁用后 ${row.displayName}（${row.username}）的登录会话会立即失效。确认禁用吗？`
        : `确认恢复 ${row.displayName}（${row.username}）的后台访问权限吗？`,
      disabling ? '禁用管理员' : '启用管理员',
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
    await updateAdminAccountStatus(row.id, disabling ? 'disabled' : 'active')
    ElMessage.success(disabling ? '管理员已禁用' : '管理员已启用')
    await loadAccounts()
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '状态更新失败'))
  }
}

function displayTime(value: string | null): string {
  return value ? formatDateTime(value) : '—'
}

function isSelf(row: AdminAccountView): boolean {
  return authStore.admin?.id === row.id
}

onMounted(() => {
  if (canManage.value) {
    void loadRoles()
    void loadAccounts()
  }
})
</script>

<template>
  <AdminPage
    title="角色与管理员"
    description="首期角色固定为超级管理员与内容运营；管理员账号的新增、启用/禁用与重置密码仅超级管理员可操作。"
  >
    <template #actions>
      <el-tag type="info" effect="plain">角色固定，不支持自定义</el-tag>
    </template>

    <el-alert
      v-if="!canManage"
      type="warning"
      :closable="false"
      show-icon
      title="无访问权限"
      description="角色与管理员账号管理仅对超级管理员开放，内容运营账号无法查看此页面。"
    />

    <template v-else>
      <AdminTableCard
        title="角色与权限"
        description="首期角色固定，不支持自定义角色与细粒度权限，菜单按角色隐藏。"
      >
        <template #head-extra>
          <el-button :icon="Refresh" :loading="roleLoading" @click="loadRoles">刷新</el-button>
        </template>

        <el-table v-loading="roleLoading" :data="roles" row-key="id">
          <el-table-column label="角色" min-width="170">
            <template #default="{ row }">
              <div class="role-name-cell">
                <strong>{{ row.name }}</strong>
                <small>{{ row.code }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="说明" min-width="280">
            <template #default="{ row }">
              {{ roleDescriptions[row.code] || '—' }}
            </template>
          </el-table-column>
          <el-table-column label="权限范围" min-width="280">
            <template #default="{ row }">
              <div class="role-permissions">
                <el-tag
                  v-for="permission in formatPermissions(row.permissions)"
                  :key="permission"
                  size="small"
                  effect="plain"
                >
                  {{ permission }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default>
              <el-tag type="success" effect="plain">启用</el-tag>
            </template>
          </el-table-column>
          <template #empty>
            <AdminEmptyState description="暂无角色数据" />
          </template>
        </el-table>
      </AdminTableCard>

      <AdminTableCard
        title="管理员账号"
        description="后台账号不开放注册，由超级管理员创建；账号被禁用或重置密码后，已登录会话立即失效。"
      >
        <template #head-extra>
          <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增管理员</el-button>
        </template>

        <template #toolbar>
          <AdminToolbar>
            <el-input
              v-model="keyword"
              class="admin-search"
              placeholder="搜索用户名或显示名称"
              clearable
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select
              v-model="roleFilter"
              class="admin-filter-wide"
              placeholder="全部角色"
              @change="handleSearch"
            >
              <el-option label="全部角色" value="" />
              <el-option label="超级管理员" value="super_admin" />
              <el-option label="内容运营" value="content_operator" />
            </el-select>
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
            <el-button :icon="Refresh" @click="handleResetFilters">重置</el-button>
            <template #meta>共 {{ total }} 个账号</template>
          </AdminToolbar>
        </template>

        <el-table v-loading="accountsLoading" :data="accounts" row-key="id">
          <el-table-column label="管理员" min-width="200">
            <template #default="{ row }">
              <div class="admin-account-cell">
                <el-avatar :size="32" :src="row.avatarUrl || undefined">
                  {{ row.displayName?.slice(0, 1) }}
                </el-avatar>
                <span>
                  <strong>{{ row.displayName }}</strong>
                  <small>{{ row.username }} · ID {{ row.id }}</small>
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="角色" width="140">
            <template #default="{ row }">
              <el-tag :type="row.roleCode === 'super_admin' ? 'warning' : 'info'" effect="plain">
                {{ roleLabel(row.roleCode) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'active' ? 'success' : 'info'" effect="plain">
                {{ row.status === 'active' ? '正常' : '已禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最近登录" width="170">
            <template #default="{ row }">{{ displayTime(row.lastLoginAt) }}</template>
          </el-table-column>
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">{{ displayTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="230" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :icon="Key" @click="openResetDialog(row)">
                重置密码
              </el-button>
              <el-button
                v-if="!isSelf(row)"
                link
                :type="row.status === 'active' ? 'danger' : 'success'"
                @click="toggleStatus(row)"
              >
                {{ row.status === 'active' ? '禁用' : '启用' }}
              </el-button>
              <el-tag v-else size="small" type="info" effect="plain">当前账号</el-tag>
            </template>
          </el-table-column>
          <template #empty>
            <AdminEmptyState description="暂无管理员账号" />
          </template>
        </el-table>

        <template #footer>
          <AdminPagination
            v-model:page="page"
            v-model:page-size="pageSize"
            :total="total"
            @change="loadAccounts"
          />
        </template>
      </AdminTableCard>
    </template>

    <el-dialog v-model="createVisible" title="新增内容运营账号" width="460px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="92px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" maxlength="20" placeholder="3–20 位字母、数字或下划线" />
        </el-form-item>
        <el-form-item label="显示名称" prop="displayName">
          <el-input v-model="createForm.displayName" maxlength="64" placeholder="例如：内容运营小李" />
        </el-form-item>
        <el-form-item label="角色" prop="roleCode">
          <el-select model-value="content_operator" disabled class="role-select">
            <el-option label="内容运营" value="content_operator" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password maxlength="64" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="createForm.confirmPassword" type="password" show-password maxlength="64" />
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="仅允许创建内容运营账号；超级管理员账号需由数据库初始化。"
        />
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreate">创建账号</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetVisible" title="重置管理员密码" width="440px" destroy-on-close>
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="92px">
        <el-form-item label="目标账号">
          <el-input
            :model-value="resetTarget ? `${resetTarget.displayName}（${resetTarget.username}）` : ''"
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
          title="重置后该账号所有后台会话立即失效，需要重新登录。"
        />
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetSubmitting" @click="submitReset">确认重置</el-button>
      </template>
    </el-dialog>
  </AdminPage>
</template>

<style scoped>
.admin-filter-wide {
  width: 150px;
}

.role-name-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.role-name-cell strong {
  font-size: 13px;
}

.role-name-cell small {
  color: var(--tz-color-text-muted);
  font-size: 11px;
  font-family: 'JetBrains Mono', 'Cascadia Code', Consolas, monospace;
}

.role-permissions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.admin-account-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-account-cell span {
  display: flex;
  min-width: 0;
  flex-direction: column;
  line-height: 1.4;
}

.admin-account-cell strong {
  font-size: 13px;
}

.admin-account-cell small {
  color: var(--tz-color-text-muted);
  font-size: 11px;
}

.role-select {
  width: 100%;
}
</style>
