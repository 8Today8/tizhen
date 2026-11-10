<script setup lang="ts">
defineProps<{
  title?: string
  description?: string
}>()
</script>

<template>
  <section class="admin-table-card">
    <header v-if="title || $slots['head-extra']" class="admin-table-card__head">
      <div>
        <h2 v-if="title" class="admin-table-card__title">{{ title }}</h2>
        <p v-if="description" class="admin-table-card__desc">{{ description }}</p>
      </div>
      <div v-if="$slots['head-extra']" class="admin-table-card__head-extra">
        <slot name="head-extra" />
      </div>
    </header>

    <div v-if="$slots.toolbar" class="admin-table-card__toolbar">
      <slot name="toolbar" />
    </div>

    <slot />

    <footer v-if="$slots.footer" class="admin-table-card__footer">
      <slot name="footer" />
    </footer>
  </section>
</template>
