<template>
  <div class="p-2 app-container iframe-page">
    <div class="iframe-page__inner">
      <i-frame v-model:src="url"></i-frame>
    </div>
  </div>
</template>

<script setup lang="ts">
const url = ref(import.meta.env.VITE_APP_MONITOR_ADMIN);
</script>

<style lang="scss" scoped>
.iframe-page__inner {
  overflow: hidden;
  border-radius: 14px;
  background: var(--app-surface-bg);
  box-shadow: var(--app-shadow-sm);
}
</style>
