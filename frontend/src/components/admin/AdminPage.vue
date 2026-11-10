<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <section class="admin-page">
    <header class="admin-page__head">
      <div class="admin-page__heading">
        <h1 class="tz-page-title">{{ title }}</h1>
        <p v-if="description" class="tz-page-subtitle">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="admin-page__actions">
        <slot name="actions" />
      </div>
    </header>

    <div class="admin-page__body">
      <slot />
    </div>
  </section>
</template>
