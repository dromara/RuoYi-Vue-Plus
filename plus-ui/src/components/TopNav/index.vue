<template>
  <el-menu
    class="mix-topnav-menu"
    :default-active="activeMenu"
    mode="horizontal"
    :ellipsis="false"
    @select="handleSelect"
  >
    <template v-for="(item, index) in topMenus">
      <el-menu-item v-if="index < visibleNumber" :key="index" :index="item.path">
        <svg-icon
          v-if="item.meta && item.meta.icon && item.meta.icon !== '#'"
          :icon-class="item.meta ? item.meta.icon : ''"
        />
        {{ item.meta?.title }}
      </el-menu-item>
    </template>

    <!-- 顶部菜单超出数量折叠 -->
    <el-sub-menu
      v-if="topMenus.length > visibleNumber"
      class="el-sub-menu__hide-arrow"
      popper-class="mix-topnav-popper"
      index="more"
    >
      <template #title>更多菜单</template>
      <template v-for="(item, index) in topMenus">
        <el-menu-item v-if="index >= visibleNumber" :key="index" :index="item.path">
          <svg-icon :icon-class="item.meta ? item.meta.icon : ''" />
          {{ item.meta?.title }}
        </el-menu-item>
      </template>
    </el-sub-menu>
  </el-menu>
</template>

<script setup lang="ts">
import { RouteRecordRaw } from 'vue-router';
import { constantRoutes } from '@/router';
import { useAppStore } from '@/store/modules/app';
import { usePermissionStore } from '@/store/modules/permission';
import { useSettingsStore } from '@/store/modules/settings';
import { isHttp } from '@/utils/validate';

// 顶部栏初始数
const visibleNumber = ref<number>(-1);
// 隐藏侧边栏路由
const hideList = ['/index', '/user/profile'];

const appStore = useAppStore();
const settingsStore = useSettingsStore();
const permissionStore = usePermissionStore();
const route = useRoute();
const router = useRouter();

// 主题颜色
const theme = computed(() => settingsStore.theme);
// 所有的路由信息
const routers = computed(() => permissionStore.getTopbarRoutes());

// 顶部显示菜单
const topMenus = computed(() => {
  const topMenus: RouteRecordRaw[] = [];
  routers.value.map(menu => {
    if (menu.hidden !== true) {
      // 兼容顶部栏一级菜单内部跳转
      if (menu.path === '/' && menu.children) {
        topMenus.push(menu.children ? menu.children[0] : menu);
      } else {
        topMenus.push(menu);
      }
    }
  });
  return topMenus;
});

// 设置子路由
const childrenMenus = computed(() => {
  const childrenMenus: RouteRecordRaw[] = [];
  routers.value.map(router => {
    router.children?.forEach(item => {
      if (item.parentPath === undefined) {
        if (router.path === '/') {
          item.path = '/' + item.path;
        } else {
          if (!isHttp(item.path)) {
            item.path = router.path + '/' + item.path;
          }
        }
        item.parentPath = router.path;
      }
      childrenMenus.push(item);
    });
  });
  return constantRoutes.concat(childrenMenus);
});

// 默认激活的菜单
const activeMenu = computed(() => {
  const { meta } = route;
  let path = meta.activeMenu || route.path;
  if (path === '/index' || route.path === '/index') {
    path = '/system/user';
  }
  let activePath = path;
  if (path !== undefined && path.lastIndexOf('/') > 0 && hideList.indexOf(path) === -1) {
    const tmpPath = path.substring(1, path.length);
    if (!route.meta.link) {
      activePath = '/' + tmpPath.substring(0, tmpPath.indexOf('/'));
      appStore.toggleSideBarHide(false);
    }
  } else if (!route.children) {
    activePath = path;
    appStore.toggleSideBarHide(true);
  }
  activeRoutes(activePath);
  return activePath;
});

const setVisibleNumber = () => {
  let width = document.body.getBoundingClientRect().width;
  if (width >= 1000) {
    width -= 420;
  }
  visibleNumber.value = Math.max(1, Math.floor(width / 3 / 92) + 2);
};

const handleSelect = (key: string) => {
  const route = routers.value.find(item => item.path === key);
  if (isHttp(key)) {
    // http(s):// 路径新窗口打开
    window.open(key, '_blank');
  } else if (!route || !route.children) {
    // 没有子路由路径内部打开
    const routeMenu = childrenMenus.value.find(item => item.path === key);
    if (routeMenu && routeMenu.query) {
      const query = JSON.parse(routeMenu.query);
      router.push({ path: key, query: query });
    } else {
      router.push({ path: key });
    }
    appStore.toggleSideBarHide(true);
  } else {
    // 显示左侧联动菜单
    activeRoutes(key);
    appStore.toggleSideBarHide(false);
  }
};

const activeRoutes = (key: string) => {
  const routes: RouteRecordRaw[] = [];
  if (childrenMenus.value && childrenMenus.value.length > 0) {
    childrenMenus.value.map(item => {
      if (key == item.parentPath || (key == 'index' && '' == item.path)) {
        routes.push(item);
      }
    });
  }
  if (routes.length > 0) {
    permissionStore.setSidebarRouters(routes);
  } else {
    appStore.toggleSideBarHide(true);
  }
  return routes;
};

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
.mix-topnav-menu.el-menu--horizontal {
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
  min-width: 0;
  height: 44px;
  padding: 0;
  border: none !important;
  background: transparent !important;

  &::after {
    display: none !important;
  }
}

#app .mix-topnav-menu.el-menu--horizontal > .el-menu-item,
#app .mix-topnav-menu.el-menu--horizontal > .el-sub-menu > .el-sub-menu__title {
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

#app .mix-topnav-menu.el-menu--horizontal > .el-menu-item:hover,
#app .mix-topnav-menu.el-menu--horizontal > .el-sub-menu > .el-sub-menu__title:hover {
  background: linear-gradient(180deg, var(--topbar-pill-hover-bg), var(--topbar-pill-bg)) !important;
  border-color: var(--topbar-pill-border) !important;
  color: v-bind(theme) !important;
  transform: translateY(-1px);
}

#app .mix-topnav-menu.el-menu--horizontal > .el-menu-item.is-active,
#app .mix-topnav-menu.el-menu--horizontal > .el-sub-menu.is-active > .el-sub-menu__title {
  background: linear-gradient(180deg, var(--topbar-pill-hover-bg), var(--topbar-pill-active-bg)) !important;
  border-color: var(--topbar-pill-active-border) !important;
  color: v-bind(theme) !important;
  box-shadow: none !important;
  transform: none !important;
}

#app .mix-topnav-menu.el-menu--horizontal > .el-menu-item .svg-icon,
#app .mix-topnav-menu.el-menu--horizontal > .el-sub-menu > .el-sub-menu__title .svg-icon {
  width: 14px;
  height: 14px;
  margin-right: 0 !important;
  flex-shrink: 0;
}

#app .mix-topnav-menu.el-menu--horizontal > .el-menu-item span,
#app .mix-topnav-menu.el-menu--horizontal > .el-sub-menu > .el-sub-menu__title span {
  color: inherit !important;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.01em;
}

.mix-topnav-menu .el-sub-menu .el-sub-menu__icon-arrow {
  position: static;
  margin: 0 0 0 2px;
  display: block !important;
  color: inherit;
  font-size: 11px;
}

.mix-topnav-menu.el-menu--horizontal > .el-menu-item::after,
.mix-topnav-menu.el-menu--horizontal > .el-sub-menu > .el-sub-menu__title::after {
  display: none !important;
}

.mix-topnav-menu.el-menu--horizontal > .el-sub-menu.el-sub-menu__hide-arrow > .el-sub-menu__title {
  color: var(--topbar-pill-muted) !important;
  padding-right: 16px !important;
}

.mix-topnav-popper.el-popper {
  border: none !important;
  border-radius: var(--app-radius-base) !important;
  background: transparent !important;
  box-shadow: none !important;
  padding: 0 !important;
}

.mix-topnav-popper.el-popper .el-popper__arrow {
  display: none !important;
}

.mix-topnav-popper .el-menu--popup {
  min-width: 188px;
  padding: 10px !important;
  border: 1px solid var(--app-surface-border);
  border-radius: var(--app-radius-base);
  background: var(--app-surface-bg) !important;
  box-shadow: var(--app-shadow-md);
}

.mix-topnav-popper .el-menu--popup .el-menu-item,
.mix-topnav-popper .el-menu--popup .el-sub-menu__title {
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

.mix-topnav-popper .el-menu--popup .el-menu-item:last-child,
.mix-topnav-popper .el-menu--popup .el-sub-menu:last-child > .el-sub-menu__title {
  margin-bottom: 0 !important;
}

.mix-topnav-popper .el-menu--popup .el-menu-item:hover,
.mix-topnav-popper .el-menu--popup .el-sub-menu__title:hover {
  background: rgba(64, 158, 255, 0.08) !important;
  color: v-bind(theme) !important;
  transform: translateX(1px);
}

.mix-topnav-popper .el-menu--popup .el-menu-item.is-active,
.mix-topnav-popper .el-menu--popup .el-sub-menu.is-active > .el-sub-menu__title {
  background: linear-gradient(180deg, rgba(64, 158, 255, 0.16), rgba(64, 158, 255, 0.1)) !important;
  color: v-bind(theme) !important;
  box-shadow: inset 0 0 0 1px rgba(64, 158, 255, 0.12);
}

.mix-topnav-popper .el-menu--popup .svg-icon {
  width: 14px;
  height: 14px;
  margin-right: 10px !important;
}

.mix-topnav-popper .el-menu--popup .el-sub-menu__title > span,
.mix-topnav-popper .el-menu--popup .el-menu-item > span {
  display: inline-flex;
  align-items: center;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.01em;
}

.mix-topnav-popper .el-menu--popup .el-sub-menu__icon-arrow {
  position: static;
  margin-left: auto;
  margin-top: 0;
  font-size: 11px;
  align-self: center;
}

html.dark .mix-topnav-menu.el-menu--horizontal {
  --topbar-pill-bg: rgba(30, 41, 59, 0.64);
  --topbar-pill-hover-bg: rgba(51, 65, 85, 0.88);
  --topbar-pill-active-bg: rgba(51, 65, 85, 0.78);
  --topbar-pill-border: rgba(71, 85, 105, 0.28);
  --topbar-pill-active-border: rgba(71, 85, 105, 0.32);
}

html.dark .mix-topnav-popper .el-menu--popup {
  border-color: rgba(71, 85, 105, 0.34);
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.98), rgba(30, 41, 59, 0.94)) !important;
  box-shadow:
    0 20px 42px rgba(0, 0, 0, 0.34),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

html.dark .mix-topnav-popper .el-menu--popup .el-menu-item,
html.dark .mix-topnav-popper .el-menu--popup .el-sub-menu__title {
  color: #e5edf8 !important;
}

html.dark .mix-topnav-popper .el-menu--popup .el-menu-item:hover,
html.dark .mix-topnav-popper .el-menu--popup .el-sub-menu__title:hover {
  background: rgba(96, 165, 250, 0.14) !important;
}

html.dark .mix-topnav-popper .el-menu--popup .el-menu-item.is-active,
html.dark .mix-topnav-popper .el-menu--popup .el-sub-menu.is-active > .el-sub-menu__title {
  background: linear-gradient(180deg, rgba(37, 99, 235, 0.28), rgba(59, 130, 246, 0.18)) !important;
  box-shadow: inset 0 0 0 1px rgba(96, 165, 250, 0.16);
}
</style>
