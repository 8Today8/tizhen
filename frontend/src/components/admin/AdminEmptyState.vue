<script setup lang="ts">
withDefaults(
  defineProps<{
    description?: string
  }>(),
  {
    description: '暂无数据'
  }
)
</script>

<template>
  <el-empty class="admin-empty" :description="description" :image-size="64" />
</template>
