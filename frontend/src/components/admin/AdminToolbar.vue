<template>
  <div class="admin-toolbar">
    <div class="admin-toolbar__filters">
      <slot />
    </div>
    <div v-if="$slots.meta" class="admin-toolbar__meta">
      <slot name="meta" />
    </div>
  </div>
</template>
