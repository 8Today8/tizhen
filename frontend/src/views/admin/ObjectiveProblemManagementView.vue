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
    title="客观题管理"
    description="维护单选、多选与判断题的题干、选项、正确答案、解析与标签；答错自动进入选手错题本。"
  >
    <template #actions>
      <el-button type="primary" @click="notifyPending('新增客观题')">新增客观题</el-button>
    </template>

    <AdminTableCard>
      <template #toolbar>
        <AdminToolbar>
          <el-input
            v-model="keyword"
            class="admin-search"
            placeholder="搜索客观题题干或标签"
            clearable
          />
          <template #meta>共 0 道</template>
        </AdminToolbar>
      </template>

      <el-table :data="[]">
        <el-table-column label="题目" min-width="260" />
        <el-table-column label="题型" width="96" />
        <el-table-column label="难度" width="96" />
        <el-table-column label="标签" min-width="160" />
        <el-table-column label="状态" width="100" />
        <el-table-column label="操作" width="190" />
        <template #empty>
          <AdminEmptyState description="暂无客观题，接入题库管理接口后展示" />
        </template>
      </el-table>

      <template #footer>
        <AdminPagination />
      </template>
    </AdminTableCard>
  </AdminPage>
</template>
