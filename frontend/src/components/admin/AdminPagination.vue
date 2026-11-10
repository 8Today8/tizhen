<script setup lang="ts">
withDefaults(
  defineProps<{
    total?: number
    page?: number
    pageSize?: number
    pageSizes?: number[]
  }>(),
  {
    total: 0,
    page: 1,
    pageSize: 20,
    pageSizes: () => [10, 20, 50, 100]
  }
)

const emit = defineEmits<{
  'update:page': [value: number]
  'update:pageSize': [value: number]
  change: []
}>()

function handleCurrentChange(value: number) {
  emit('update:page', value)
  emit('change')
}

function handleSizeChange(value: number) {
  emit('update:pageSize', value)
  emit('update:page', 1)
  emit('change')
}
</script>

<template>
  <el-pagination
    class="admin-pagination"
    background
    layout="total, sizes, prev, pager, next"
    :total="total"
    :current-page="page"
    :page-size="pageSize"
    :page-sizes="pageSizes"
    @current-change="handleCurrentChange"
    @size-change="handleSizeChange"
  />
</template>
