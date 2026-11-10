<script setup lang="ts">
import { ref } from 'vue'

import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminPage from '@/components/admin/AdminPage.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import AdminTableCard from '@/components/admin/AdminTableCard.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'
import { usePendingFeature } from '@/composables/usePendingFeature'

const keyword = ref('')
const difficulty = ref('all')
const status = ref('all')
const { notifyPending } = usePendingFeature()
</script>

<template>
  <AdminPage
    title="OJ 题库管理"
    description="维护站内编程题的题面、测试数据、运行限制、标签与上下架状态；已被引用的题目只能下架。"
  >
    <template #actions>
      <el-button type="primary" @click="notifyPending('新增题目')">新增题目</el-button>
    </template>

    <AdminTableCard>
      <template #toolbar>
        <AdminToolbar>
          <el-input
            v-model="keyword"
            class="admin-search"
            placeholder="搜索题目名称或标签"
            clearable
          />
          <el-select v-model="difficulty" class="admin-filter">
            <el-option label="全部难度" value="all" />
            <el-option label="入门" value="entry" />
            <el-option label="基础" value="basic" />
            <el-option label="进阶" value="advanced" />
          </el-select>
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
        <el-table-column label="题目" min-width="240" />
        <el-table-column label="难度" width="96" />
        <el-table-column label="标签" min-width="160" />
        <el-table-column label="提交 / 通过" width="120" align="center" />
        <el-table-column label="状态" width="100" />
        <el-table-column label="操作" width="210" />
        <template #empty>
          <AdminEmptyState description="暂无 OJ 题目，接入题库管理接口后展示" />
        </template>
      </el-table>

      <template #footer>
        <AdminPagination />
      </template>
    </AdminTableCard>
  </AdminPage>
</template>
