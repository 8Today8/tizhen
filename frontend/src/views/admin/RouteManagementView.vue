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
    title="路线管理"
    description="维护学习路线、难度、排序与上下架状态；保存后前台路线列表立即生效。"
  >
    <template #actions>
      <el-button type="primary" @click="notifyPending('新增路线')">新增路线</el-button>
    </template>

    <AdminTableCard>
      <template #toolbar>
        <AdminToolbar>
          <el-input
            v-model="keyword"
            class="admin-search"
            placeholder="搜索路线名称或副标题"
            clearable
          />
          <template #meta>共 0 条路线</template>
        </AdminToolbar>
      </template>

      <el-table :data="[]">
        <el-table-column label="路线" min-width="220" />
        <el-table-column label="难度" width="96" />
        <el-table-column label="阶段" width="88" align="center" />
        <el-table-column label="题目" width="88" align="center" />
        <el-table-column label="排序" width="88" align="center" />
        <el-table-column label="状态" width="100" />
        <el-table-column label="操作" width="190" />
        <template #empty>
          <AdminEmptyState description="暂无路线数据，接入路线管理接口后展示" />
        </template>
      </el-table>

      <template #footer>
        <AdminPagination />
      </template>
    </AdminTableCard>
  </AdminPage>
</template>
