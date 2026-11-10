<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    description: string
    status?: string
  }>(),
  {
    status: '待接入'
  }
)
</script>

<template>
  <section class="tz-page">
    <header class="placeholder-header">
      <div>
        <h1 class="tz-page-title">{{ title }}</h1>
        <p class="tz-page-subtitle">{{ description }}</p>
      </div>
      <el-tag type="info" effect="plain">{{ status }}</el-tag>
    </header>

    <div class="tz-card placeholder-card">
      <p class="placeholder-card__title">工程骨架已就绪</p>
      <p class="tz-muted">
        该页面的路由、布局与主题已搭好，业务接口和数据展示将在对应模块开发时接入。
      </p>
      <RouterLink to="/" class="placeholder-card__link">返回首页</RouterLink>
    </div>
  </section>
</template>

<style scoped>
.placeholder-header {
  display: flex;
  margin-bottom: 24px;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.placeholder-card {
  max-width: 720px;
}

.placeholder-card__title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
}

.placeholder-card__link {
  display: inline-block;
  margin-top: 18px;
  color: var(--tz-color-primary);
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
}

.placeholder-card__link:hover {
  text-decoration: underline;
}
</style>
