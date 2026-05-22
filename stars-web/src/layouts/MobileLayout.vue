<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Collection, Download, PriceTag } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const tabs = [
  { path: '/repos', label: '仓库', icon: Collection },
  { path: '/import', label: '导入', icon: Download },
  { path: '/tags', label: '标签', icon: PriceTag },
]

const activeTab = computed(() => {
  if (route.path.startsWith('/repos')) return '/repos'
  return route.path
})

function navigate(path: string) {
  if (route.path !== path) {
    router.push(path)
  }
}
</script>

<template>
  <div class="mobile-layout">
    <main class="mobile-main">
      <slot />
    </main>
    <nav class="mobile-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.path"
        type="button"
        class="tab-item"
        :class="{ active: activeTab === tab.path }"
        @click="navigate(tab.path)"
      >
        <el-icon><component :is="tab.icon" /></el-icon>
        <span>{{ tab.label }}</span>
      </button>
    </nav>
  </div>
</template>

<style scoped lang="scss">
.mobile-layout {
  min-height: 100vh;
  background: #f5f7fa;
}

.mobile-main {
  min-height: calc(100vh - 64px);
  padding: 12px 12px 80px;
}

.mobile-tabs {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 100;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  height: 64px;
  background: #fff;
  border-top: 1px solid #e5e7eb;
}

.tab-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: #6b7280;
  font-size: 12px;
  cursor: pointer;
}

.tab-item.active {
  color: #2563eb;
}
</style>
