<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleClose,
  Delete,
  Edit,
  Plus,
  Refresh,
  Search,
  VideoPlay,
  View
} from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

import {
  createAdminRoute,
  deleteAdminRoute,
  fetchAdminRoute,
  fetchAdminRoutes,
  offlineAdminRoute,
  publishAdminRoute,
  updateAdminRoute,
  type AdminRouteDetail,
  type AdminRouteView,
  type LearningStatus
} from '@/api/admin'
import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminPage from '@/components/admin/AdminPage.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import AdminTableCard from '@/components/admin/AdminTableCard.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'
import { contentStatusLabel, contentStatusTagType } from '@/utils/adminLabels'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

const keyword = ref('')
const statusFilter = ref<'' | LearningStatus>('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)
const routes = ref<AdminRouteView[]>([])

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<AdminRouteDetail | null>(null)

const formVisible = ref(false)
const formSubmitting = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<string | null>(null)
const form = reactive({
  name: '',
  description: '',
  coverUrl: '',
  sortOrder: 0
})
const formRules: FormRules = {
  name: [
    { required: true, message: '请输入路线名称', trigger: 'blur' },
    { max: 100, message: '路线名称不能超过 100 个字符', trigger: 'blur' }
  ],
  coverUrl: [
    { max: 512, message: '封面地址不能超过 512 个字符', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        const text = String(value ?? '').trim()
        if (!text) {
          callback()
          return
        }
        try {
          const url = new URL(text)
          if (url.protocol !== 'http:' && url.protocol !== 'https:') {
            throw new Error('invalid protocol')
          }
          callback()
        } catch {
          callback(new Error('请输入有效的 http 或 https 封面地址'))
        }
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

async function loadRoutes() {
  loading.value = true
  try {
    const result = await fetchAdminRoutes(queryParams())
    routes.value = result.items
    total.value = result.total
  } catch (error) {
    routes.value = []
    total.value = 0
    ElMessage.error(resolveApiErrorMessage(error, '路线列表加载失败'))
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  void loadRoutes()
}

function handleResetFilters() {
  keyword.value = ''
  statusFilter.value = ''
  handleSearch()
}

function resetForm() {
  form.name = ''
  form.description = ''
  form.coverUrl = ''
  form.sortOrder = 0
}

function openCreateDialog() {
  editingId.value = null
  resetForm()
  formVisible.value = true
  formRef.value?.clearValidate()
}

function openEditDialog(row: AdminRouteView) {
  editingId.value = row.id
  form.name = row.name
  form.description = row.description ?? ''
  form.coverUrl = row.coverUrl ?? ''
  form.sortOrder = row.sortOrder
  formVisible.value = true
  formRef.value?.clearValidate()
}

async function submitForm() {
  const instance = formRef.value
  if (!instance) {
    return
  }
  const valid = await instance.validate().catch(() => false)
  if (!valid) {
    return
  }
  formSubmitting.value = true
  try {
    const name = form.name.trim()
    const description = form.description.trim()
    const coverUrl = form.coverUrl.trim()
    if (editingId.value) {
      await updateAdminRoute(editingId.value, {
        name,
        description: description || null,
        coverUrl: coverUrl || null,
        sortOrder: form.sortOrder
      })
      ElMessage.success(`已保存路线「${name}」`)
      await loadRoutes()
      await refreshDetailIfOpen(editingId.value)
    } else {
      await createAdminRoute({
        name,
        description: description || undefined,
        coverUrl: coverUrl || undefined,
        sortOrder: form.sortOrder
      })
      page.value = 1
      ElMessage.success(`已新增路线「${name}」，当前为草稿`)
      await loadRoutes()
    }
    formVisible.value = false
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, editingId.value ? '路线保存失败' : '路线创建失败'))
  } finally {
    formSubmitting.value = false
  }
}

async function openDetail(row: AdminRouteView) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await fetchAdminRoute(row.id)
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '路线详情加载失败'))
  } finally {
    detailLoading.value = false
  }
}

async function refreshDetailIfOpen(routeId: string) {
  if (!detailVisible.value || detail.value?.id !== routeId) {
    return
  }
  try {
    detail.value = await fetchAdminRoute(routeId)
  } catch {
    detailVisible.value = false
    detail.value = null
  }
}

async function toggleStatus(row: AdminRouteView) {
  const publishing = row.status !== 'published'
  try {
    await ElMessageBox.confirm(
      publishing
        ? `发布后「${row.name}」将立即出现在前台学习路线中。确认发布吗？`
        : `下架后「${row.name}」将不再出现在前台，已产生的学习进度保留。确认下架吗？`,
      publishing ? '发布路线' : '下架路线',
      {
        confirmButtonText: publishing ? '确认发布' : '确认下架',
        cancelButtonText: '取消',
        type: publishing ? 'info' : 'warning'
      }
    )
  } catch {
    return
  }

  try {
    if (publishing) {
      await publishAdminRoute(row.id)
    } else {
      await offlineAdminRoute(row.id)
    }
    ElMessage.success(publishing ? '路线已发布' : '路线已下架')
    await loadRoutes()
    await refreshDetailIfOpen(row.id)
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, publishing ? '发布失败' : '下架失败'))
  }
}

async function confirmDelete(row: AdminRouteView) {
  try {
    await ElMessageBox.confirm(
      `删除后「${row.name}」将从后台移除。存在阶段或学习进度时不能删除。确认删除吗？`,
      '删除路线',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  try {
    await deleteAdminRoute(row.id)
    if (detail.value?.id === row.id) {
      detailVisible.value = false
      detail.value = null
    }
    if (routes.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    ElMessage.success('路线已删除')
    await loadRoutes()
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '路线删除失败'))
  }
}

function goToStages(routeId: string) {
  void router.push({ name: 'admin-stages', query: { routeId } })
}

onMounted(loadRoutes)
</script>

<template>
  <AdminPage
    title="路线管理"
    description="维护学习路线的名称、简介、封面与排序；发布后前台路线列表立即生效，下架不影响已有学习进度。"
  >
    <template #actions>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增路线</el-button>
    </template>

    <AdminTableCard
      title="路线列表"
      description="路线按排序值升序展示；阶段数量实时统计，草稿与已下架路线也会出现在后台列表。"
    >
      <template #toolbar>
        <AdminToolbar>
          <el-input
            v-model="keyword"
            class="admin-search"
            placeholder="搜索路线名称或简介"
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
            <el-option label="草稿" value="draft" />
            <el-option label="已发布" value="published" />
            <el-option label="已下架" value="offline" />
          </el-select>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleResetFilters">重置</el-button>
          <template #meta>共 {{ total }} 条路线</template>
        </AdminToolbar>
      </template>

      <el-table v-loading="loading" :data="routes" row-key="id">
        <el-table-column label="路线" min-width="280">
          <template #default="{ row }">
            <div class="route-name-cell">
              <strong>{{ row.name }}</strong>
              <span class="route-name-cell__desc">{{ row.description || '暂无简介' }}</span>
              <small>ID {{ row.id }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="阶段" width="90" align="center">
          <template #default="{ row }">
            <span class="route-count">{{ row.stageCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="86" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="contentStatusTagType(row.status)" effect="plain">
              {{ contentStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="310" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 'published' ? 'warning' : 'success'"
              :icon="row.status === 'published' ? CircleClose : VideoPlay"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'published' ? '下架' : '发布' }}
            </el-button>
            <el-button link type="danger" :icon="Delete" @click="confirmDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <AdminEmptyState description="没有符合条件的路线，可新增路线或调整筛选条件" />
        </template>
      </el-table>

      <template #footer>
        <AdminPagination
          v-model:page="page"
          v-model:page-size="pageSize"
          :total="total"
          @change="loadRoutes"
        />
      </template>
    </AdminTableCard>

    <el-dialog
      v-model="formVisible"
      :title="editingId ? '编辑路线' : '新增路线'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="88px">
        <el-form-item label="路线名称" prop="name">
          <el-input v-model="form.name" maxlength="100" show-word-limit placeholder="例如：算法基础专题" />
        </el-form-item>
        <el-form-item label="路线简介" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="10000"
            show-word-limit
            placeholder="简要介绍路线目标、适合人群与学习收获"
          />
        </el-form-item>
        <el-form-item label="封面地址" prop="coverUrl">
          <el-input v-model="form.coverUrl" maxlength="512" placeholder="选填，http 或 https 图片链接" />
        </el-form-item>
        <el-form-item label="排序值" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="-999999" :max="999999" controls-position="right" />
          <span class="route-form-hint">数值越小越靠前</span>
        </el-form-item>
        <el-alert
          v-if="!editingId"
          type="info"
          :closable="false"
          show-icon
          title="新增路线默认为草稿，需要发布后才会出现在前台。"
        />
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formSubmitting" @click="submitForm">
          {{ editingId ? '保存修改' : '创建路线' }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="detailVisible"
      class="admin-detail-drawer"
      title="路线详情"
      size="620px"
    >
      <div v-loading="detailLoading" class="route-detail">
        <template v-if="detail">
          <div class="route-detail__head">
            <div class="route-detail__title">
              <h2>{{ detail.name }}</h2>
              <el-tag :type="contentStatusTagType(detail.status)" effect="plain">
                {{ contentStatusLabel(detail.status) }}
              </el-tag>
            </div>
            <p>{{ detail.description || '暂无路线简介' }}</p>
          </div>

          <el-image
            v-if="detail.coverUrl"
            class="route-detail__cover"
            :src="detail.coverUrl"
            fit="cover"
          >
            <template #error>
              <div class="route-detail__cover-error">封面加载失败</div>
            </template>
          </el-image>

          <dl class="route-detail__meta">
            <div>
              <dt>排序值</dt>
              <dd>{{ detail.sortOrder }}</dd>
            </div>
            <div>
              <dt>阶段数量</dt>
              <dd>{{ detail.stageCount }}</dd>
            </div>
            <div>
              <dt>创建时间</dt>
              <dd>{{ formatDateTime(detail.createdAt) }}</dd>
            </div>
            <div>
              <dt>更新时间</dt>
              <dd>{{ formatDateTime(detail.updatedAt) }}</dd>
            </div>
          </dl>

          <div class="route-detail__stages-head">
            <h3>阶段</h3>
            <el-button link type="primary" @click="goToStages(detail.id)">进入阶段管理</el-button>
          </div>
          <el-table :data="detail.stages" size="small" row-key="id">
            <el-table-column prop="name" label="阶段" min-width="180" />
            <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
            <el-table-column label="课程 / 题目" width="110" align="center">
              <template #default="{ row }">{{ row.courseCount }} / {{ row.problemCount }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="contentStatusTagType(row.status)" effect="plain" size="small">
                  {{ contentStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <template #empty>
              <AdminEmptyState description="该路线下还没有阶段" />
            </template>
          </el-table>
        </template>
      </div>
    </el-drawer>
  </AdminPage>
</template>

<style scoped>
.route-name-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.route-name-cell strong {
  color: var(--tz-color-text);
  font-size: 14px;
  font-weight: 600;
}

.route-name-cell__desc {
  overflow: hidden;
  color: var(--tz-color-text-muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-name-cell small {
  color: var(--tz-color-text-muted);
  font-size: 11px;
}

.route-count {
  color: var(--tz-color-text);
  font-weight: 600;
}

.route-form-hint {
  margin-left: 12px;
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

.route-detail__head {
  margin-bottom: 16px;
}

.route-detail__title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.route-detail__title h2 {
  margin: 0;
  color: var(--tz-color-text);
  font-size: 20px;
}

.route-detail__head p {
  margin: 0;
  color: var(--tz-color-text-muted);
  line-height: 1.7;
  white-space: pre-wrap;
}

.route-detail__cover {
  width: 100%;
  height: 200px;
  margin-bottom: 18px;
  border: 1px solid var(--tz-color-border);
  border-radius: 12px;
}

.route-detail__cover-error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 160px;
  color: var(--tz-color-text-muted);
  background: var(--tz-color-surface-muted);
}

.route-detail__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 0 0 24px;
}

.route-detail__meta > div {
  padding: 12px 14px;
  border: 1px solid var(--tz-color-border);
  border-radius: 10px;
  background: var(--tz-color-surface);
}

.route-detail__meta dt {
  color: var(--tz-color-text-muted);
  font-size: 12px;
}

.route-detail__meta dd {
  margin: 4px 0 0;
  color: var(--tz-color-text);
  font-weight: 600;
}

.route-detail__stages-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.route-detail__stages-head h3 {
  margin: 0;
  color: var(--tz-color-text);
  font-size: 15px;
}

@media (max-width: 640px) {
  .route-detail__meta {
    grid-template-columns: 1fr;
  }
}
</style>
