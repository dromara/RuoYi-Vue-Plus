<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Collection,
  Download,
  PriceTag,
  SwitchButton,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeMenu = computed(() => route.path)

const navItems = [
  { path: '/repos', label: '仓库列表', icon: Collection },
  { path: '/import', label: '导入中心', icon: Download },
  { path: '/tags', label: '标签管理', icon: PriceTag },
]

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="pc-layout">
    <el-aside width="220px" class="pc-aside">
      <div class="brand">
        <span class="brand-mark">★</span>
        <div>
          <div class="brand-title">Stars 知识库</div>
          <div class="brand-sub">GitHub Stars 管理</div>
        </div>
      </div>
      <el-menu :default-active="activeMenu" router class="pc-menu">
        <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-footer">
        <el-button text type="danger" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </el-button>
      </div>
    </el-aside>
    <el-container>
      <el-header class="pc-header">
        <h1>{{ route.meta.title }}</h1>
      </el-header>
      <el-main class="pc-main">
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped lang="scss">
.pc-layout {
  min-height: 100vh;
  background: #f5f7fa;
}

.pc-aside {
  display: flex;
  flex-direction: column;
  background: #1f2937;
  color: #fff;
}

.brand {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 24px 20px;
}

.brand-mark {
  font-size: 28px;
  color: #fbbf24;
}

.brand-title {
  font-size: 18px;
  font-weight: 600;
}

.brand-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
}

.pc-menu {
  flex: 1;
  border-right: none;
  background: transparent;
}

.pc-menu :deep(.el-menu-item) {
  color: #d1d5db;
}

.pc-menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background: rgba(59, 130, 246, 0.25);
}

.aside-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.pc-header {
  display: flex;
  align-items: center;
  height: 64px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;

  h1 {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
    color: #111827;
  }
}

.pc-main {
  padding: 24px;
}
</style>
