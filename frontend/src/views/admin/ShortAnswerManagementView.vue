<script setup lang="ts">
import { ref } from 'vue'

import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminPage from '@/components/admin/AdminPage.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import AdminTableCard from '@/components/admin/AdminTableCard.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'
import { usePendingFeature } from '@/composables/usePendingFeature'

const keyword = ref('')
const status = ref('all')
const { notifyPending } = usePendingFeature()
</script>

<template>
  <AdminPage
    title="简答题管理"
    description="维护简答题题干、参考答案与评分要点；未配置评分要点时不允许发布，作答由 AI 按百分制判分。"
  >
    <template #actions>
      <el-button type="primary" @click="notifyPending('新增简答题')">新增简答题</el-button>
    </template>

    <AdminTableCard>
      <template #toolbar>
        <AdminToolbar>
          <el-input
            v-model="keyword"
            class="admin-search"
            placeholder="搜索简答题题干或标签"
            clearable
          />
          <el-select v-model="status" class="admin-filter">
            <el-option label="全部状态" value="all" />
            <el-option label="已上架" value="published" />
            <el-option label="草稿" value="draft" />
            <el-option label="已下架" value="offline" />
          </el-select>
          <template #meta>共 0 道</template>
        </AdminToolbar>
      </template>

      <el-table :data="[]">
        <el-table-column label="题目" min-width="260" />
        <el-table-column label="难度" width="96" />
        <el-table-column label="标签" min-width="160" />
        <el-table-column label="评分要点" width="110" align="center" />
        <el-table-column label="状态" width="100" />
        <el-table-column label="操作" width="190" />
        <template #empty>
          <AdminEmptyState description="暂无简答题，接入题库管理接口后展示" />
        </template>
      </el-table>

      <template #footer>
        <AdminPagination />
      </template>
    </AdminTableCard>
  </AdminPage>
</template>
