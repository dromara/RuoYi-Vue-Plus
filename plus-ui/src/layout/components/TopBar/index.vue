<template>
  <el-menu
    class="topbar-menu"
    :ellipsis="false"
    :default-active="activeMenu"
    :style="{ '--el-menu-active-color': theme }"
    mode="horizontal"
  >
    <sidebar-item
      :key="menuRoute.path + index"
      v-for="(menuRoute, index) in topMenus"
      :item="menuRoute"
      :base-path="menuRoute.path"
      popper-class="topbar-menu-popper"
    />

    <el-sub-menu
      index="more"
      class="el-sub-menu__hide-arrow"
      popper-class="topbar-menu-popper"
      v-if="moreRoutes.length > 0"
    >
      <template #title>
        <span>更多菜单</span>
      </template>
      <sidebar-item
        :key="menuRoute.path + index"
        v-for="(menuRoute, index) in moreRoutes"
        :item="menuRoute"
        :base-path="menuRoute.path"
        popper-class="topbar-menu-popper"
      />
    </el-sub-menu>
  </el-menu>
</template>

<script setup lang="ts">
import type { RouteRecordRaw } from 'vue-router';
import { usePermissionStore } from '@/store/modules/permission';
import { useSettingsStore } from '@/store/modules/settings';
import SidebarItem from '../Sidebar/SidebarItem.vue';

const route = useRoute();
const settingsStore = useSettingsStore();
const permissionStore = usePermissionStore();

const theme = computed(() => settingsStore.theme);
const activeMenu = computed(() => {
  const { meta, path } = route;
  if (meta.activeMenu) {
    return meta.activeMenu;
  }
  return path;
});

const visibleNumber = ref(5);
const topMenus = computed((): RouteRecordRaw[] => {
  return permissionStore.sidebarRouters.filter(f => !f.hidden).slice(0, visibleNumber.value) as RouteRecordRaw[];
});
const moreRoutes = computed((): RouteRecordRaw[] => {
  return permissionStore.sidebarRouters.filter(f => !f.hidden).slice(visibleNumber.value) as RouteRecordRaw[];
});
function setVisibleNumber() {
  let width = document.body.getBoundingClientRect().width;
  if (width >= 1000) {
    width -= 420;
  }
  visibleNumber.value = Math.max(1, Math.floor(width / 3 / 92) + 2);
}

onMounted(() => {
  window.addEventListener('resize', setVisibleNumber);
});
onBeforeUnmount(() => {
  window.removeEventListener('resize', setVisibleNumber);
});

onMounted(() => {
  setVisibleNumber();
});
</script>

<style lang="scss">
.topbar-menu.el-menu--horizontal {
  --topbar-pill-bg: var(--app-elevated-soft-bg);
  --topbar-pill-hover-bg: var(--app-elevated-close-bg);
  --topbar-pill-active-bg: var(--app-elevated-close-bg);
  --topbar-pill-text: var(--app-text-title);
  --topbar-pill-muted: var(--app-text-muted);
  --topbar-pill-border: var(--app-surface-border);
  --topbar-pill-active-border: var(--app-surface-border);

  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
  height: 44px;
  padding: 0;
  border: none !important;
  background: transparent !important;

  &::after {
    display: none !important;
  }
}

#app .topbar-menu.el-menu--horizontal > div > a > .el-menu-item,
#app .topbar-menu.el-menu--horizontal > div > .el-sub-menu > .el-sub-menu__title,
#app .topbar-menu.el-menu--horizontal > .el-sub-menu > .el-sub-menu__title {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: 36px !important;
  line-height: 36px !important;
  margin: 0 !important;
  padding: 0 18px !important;
  border: 1px solid transparent !important;
  border-radius: 13px;
  color: var(--topbar-pill-text) !important;
  background: transparent !important;
  transition:
    background-color 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease,
    transform 0.2s ease;
}

#app .topbar-menu.el-menu--horizontal > div > a > .el-menu-item:hover,
#app .topbar-menu.el-menu--horizontal > div > .el-sub-menu > .el-sub-menu__title:hover,
#app .topbar-menu.el-menu--horizontal > .el-sub-menu > .el-sub-menu__title:hover {
  background: linear-gradient(180deg, var(--topbar-pill-hover-bg), var(--topbar-pill-bg)) !important;
  border-color: var(--topbar-pill-border) !important;
  color: v-bind(theme) !important;
  transform: translateY(-1px);
}

#app .topbar-menu.el-menu--horizontal > div > a > .el-menu-item.is-active,
#app .topbar-menu.el-menu--horizontal > div > .el-sub-menu.is-active > .el-sub-menu__title,
#app .topbar-menu.el-menu--horizontal > .el-sub-menu.is-active > .el-sub-menu__title {
  background: linear-gradient(180deg, var(--topbar-pill-hover-bg), var(--topbar-pill-active-bg)) !important;
  border-color: var(--topbar-pill-active-border) !important;
  color: v-bind(theme) !important;
  box-shadow: none !important;
  transform: none !important;
}

#app .topbar-menu.el-menu--horizontal .el-menu-item .svg-icon + span,
#app .topbar-menu.el-menu--horizontal .el-sub-menu__title .svg-icon + span {
  margin-left: 2px;
}

#app .topbar-menu.el-menu--horizontal > div > a > .el-menu-item .svg-icon,
#app .topbar-menu.el-menu--horizontal > div > .el-sub-menu > .el-sub-menu__title .svg-icon,
#app .topbar-menu.el-menu--horizontal > .el-sub-menu > .el-sub-menu__title .svg-icon {
  width: 14px;
  height: 14px;
  margin-right: 0 !important;
  flex-shrink: 0;
}

#app .topbar-menu.el-menu--horizontal > div > a > .el-menu-item span,
#app .topbar-menu.el-menu--horizontal > div > .el-sub-menu > .el-sub-menu__title span,
#app .topbar-menu.el-menu--horizontal > .el-sub-menu > .el-sub-menu__title span {
  color: inherit !important;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.01em;
}

.topbar-menu.el-menu--horizontal > div > a > .el-menu-item.is-active span,
.topbar-menu.el-menu--horizontal > div > .el-sub-menu.is-active > .el-sub-menu__title span,
.topbar-menu.el-menu--horizontal > .el-sub-menu.is-active > .el-sub-menu__title span {
  font-weight: 500;
}

.topbar-menu .el-sub-menu .el-sub-menu__icon-arrow {
  position: static;
  margin: 0 0 0 2px;
  display: block !important;
  color: inherit;
  font-size: 11px;
}

.topbar-menu.el-menu--horizontal > div > a > .el-menu-item::after,
.topbar-menu.el-menu--horizontal > div > .el-sub-menu > .el-sub-menu__title::after,
.topbar-menu.el-menu--horizontal > .el-sub-menu > .el-sub-menu__title::after {
  display: none !important;
}

.topbar-menu.el-menu--horizontal > .el-sub-menu.el-sub-menu__hide-arrow > .el-sub-menu__title {
  color: var(--topbar-pill-muted) !important;
  padding-right: 16px !important;
}

html.dark .topbar-menu.el-menu--horizontal {
  --topbar-pill-bg: rgba(30, 41, 59, 0.64);
  --topbar-pill-hover-bg: rgba(51, 65, 85, 0.88);
  --topbar-pill-active-bg: rgba(51, 65, 85, 0.78);
  --topbar-pill-border: rgba(71, 85, 105, 0.28);
  --topbar-pill-active-border: rgba(71, 85, 105, 0.32);
}

html.dark #app .topbar-menu.el-menu--horizontal > div > a > .el-menu-item.is-active,
html.dark #app .topbar-menu.el-menu--horizontal > div > .el-sub-menu.is-active > .el-sub-menu__title,
html.dark #app .topbar-menu.el-menu--horizontal > .el-sub-menu.is-active > .el-sub-menu__title {
  box-shadow: none !important;
}

.topbar-menu-popper.el-popper {
  border: none !important;
  border-radius: var(--app-radius-base) !important;
  background: transparent !important;
  box-shadow: none !important;
  padding: 0 !important;
}

.topbar-menu-popper.el-popper .el-popper__arrow {
  display: none !important;
}

.topbar-menu-popper .el-menu--popup {
  min-width: 188px;
  padding: 10px !important;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: var(--app-radius-base);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.94)) !important;
  box-shadow:
    0 18px 40px rgba(15, 23, 42, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.topbar-menu-popper .el-menu--popup .el-menu-item,
.topbar-menu-popper .el-menu--popup .el-sub-menu__title {
  display: flex;
  align-items: center;
  height: 38px;
  line-height: 38px;
  padding: 0 14px !important;
  margin: 0 0 4px !important;
  border-radius: 12px;
  color: var(--app-text-title) !important;
  background: transparent !important;
  transition:
    background-color 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.topbar-menu-popper .el-menu--popup .el-menu-item:last-child,
.topbar-menu-popper .el-menu--popup .el-sub-menu:last-child > .el-sub-menu__title {
  margin-bottom: 0 !important;
}

.topbar-menu-popper .el-menu--popup .el-menu-item:hover,
.topbar-menu-popper .el-menu--popup .el-sub-menu__title:hover {
  background: rgba(64, 158, 255, 0.08) !important;
  color: v-bind(theme) !important;
  transform: translateX(1px);
}

.topbar-menu-popper .el-menu--popup .el-menu-item.is-active,
.topbar-menu-popper .el-menu--popup .el-sub-menu.is-active > .el-sub-menu__title {
  background: linear-gradient(180deg, rgba(64, 158, 255, 0.16), rgba(64, 158, 255, 0.1)) !important;
  color: v-bind(theme) !important;
  box-shadow: inset 0 0 0 1px rgba(64, 158, 255, 0.12);
}

.topbar-menu-popper .el-menu--popup .svg-icon {
  width: 14px;
  height: 14px;
  margin-right: 10px !important;
}

.topbar-menu-popper .el-menu--popup .menu-title,
.topbar-menu-popper .el-menu--popup .el-sub-menu__title > span,
.topbar-menu-popper .el-menu--popup .el-menu-item > span {
  display: inline-flex;
  align-items: center;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.01em;
}

.topbar-menu-popper .el-menu--popup .el-sub-menu__icon-arrow {
  position: static;
  margin-left: auto;
  margin-top: 0;
  font-size: 11px;
  align-self: center;
}

html.dark .topbar-menu-popper .el-menu--popup {
  border-color: rgba(71, 85, 105, 0.34);
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.98), rgba(30, 41, 59, 0.94)) !important;
  box-shadow:
    0 20px 42px rgba(0, 0, 0, 0.34),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

html.dark .topbar-menu-popper .el-menu--popup .el-menu-item,
html.dark .topbar-menu-popper .el-menu--popup .el-sub-menu__title {
  color: #e5edf8 !important;
}

html.dark .topbar-menu-popper .el-menu--popup .el-menu-item:hover,
html.dark .topbar-menu-popper .el-menu--popup .el-sub-menu__title:hover {
  background: rgba(96, 165, 250, 0.14) !important;
}

html.dark .topbar-menu-popper .el-menu--popup .el-menu-item.is-active,
html.dark .topbar-menu-popper .el-menu--popup .el-sub-menu.is-active > .el-sub-menu__title {
  background: linear-gradient(180deg, rgba(37, 99, 235, 0.28), rgba(59, 130, 246, 0.18)) !important;
  box-shadow: inset 0 0 0 1px rgba(96, 165, 250, 0.16);
}
</style>
