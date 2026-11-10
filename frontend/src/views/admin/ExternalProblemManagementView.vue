<script setup lang="ts">
import { ref } from 'vue'

import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminPage from '@/components/admin/AdminPage.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import AdminTableCard from '@/components/admin/AdminTableCard.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'
import { usePendingFeature } from '@/composables/usePendingFeature'

const keyword = ref('')
const { notifyPending } = usePendingFeature()
</script>

<template>
  <AdminPage
    title="外链题管理"
    description="维护外部平台经典题入口，只保存标题、平台、链接与标签，不复制题面内容。"
  >
    <template #actions>
      <el-button type="primary" @click="notifyPending('新增外链题')">新增外链题</el-button>
    </template>

    <AdminTableCard>
      <template #toolbar>
        <AdminToolbar>
          <el-input
            v-model="keyword"
            class="admin-search"
            placeholder="搜索外链题名称或标签"
            clearable
          />
          <template #meta>共 0 道</template>
        </AdminToolbar>
      </template>

      <el-table :data="[]">
        <el-table-column label="题目" min-width="240" />
        <el-table-column label="平台" width="120" />
        <el-table-column label="难度" width="96" />
        <el-table-column label="标签" min-width="160" />
        <el-table-column label="状态" width="100" />
        <el-table-column label="操作" width="220" />
        <template #empty>
          <AdminEmptyState description="暂无外链题，接入题库管理接口后展示" />
        </template>
      </el-table>

      <template #footer>
        <AdminPagination />
      </template>
    </AdminTableCard>
  </AdminPage>
</template>
