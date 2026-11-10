<script setup lang="ts">
import { ref } from 'vue'

import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminPage from '@/components/admin/AdminPage.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import AdminTableCard from '@/components/admin/AdminTableCard.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'

const routeFilter = ref('all')
</script>

<template>
  <AdminPage
    title="阶段管理"
    description="跨路线查看阶段，维护阶段内容、关联题目、视频课程与排序；阶段必须归属于某条路线。"
  >
    <AdminTableCard>
      <template #toolbar>
        <AdminToolbar>
          <el-select v-model="routeFilter" class="admin-filter" placeholder="全部路线">
            <el-option label="全部路线" value="all" />
          </el-select>
          <template #meta>共 0 个阶段</template>
        </AdminToolbar>
      </template>

      <el-table :data="[]">
        <el-table-column label="路线" min-width="180" />
        <el-table-column label="阶段" min-width="220" />
        <el-table-column label="顺序" width="80" align="center" />
        <el-table-column label="题目" width="80" align="center" />
        <el-table-column label="课程" width="80" align="center" />
        <el-table-column label="状态" width="100" />
        <el-table-column label="操作" width="190" />
        <template #empty>
          <AdminEmptyState description="暂无阶段数据，请先维护学习路线" />
        </template>
      </el-table>

      <template #footer>
        <AdminPagination />
      </template>
    </AdminTableCard>
  </AdminPage>
</template>
