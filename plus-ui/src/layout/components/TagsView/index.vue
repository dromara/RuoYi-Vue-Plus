<template>
  <div id="tags-view-container" class="tags-view-container">
    <span v-if="canScrollLeft" class="tags-nav-btn tags-nav-btn--left" @click="scrollLeft">
      <el-icon><ArrowLeft /></el-icon>
    </span>

    <scroll-pane ref="scrollPaneRef" class="tags-view-wrapper" @scroll="handleScroll" @update-arrows="updateArrowState">
      <router-link
        v-for="tag in visitedViews"
        :key="tag.fullPath || tag.path"
        :data-tag-key="tag.fullPath || tag.path"
        :class="{ active: isActive(tag), 'has-icon': tagsIcon }"
        :to="tag.fullPath || tag.path || '/'"
        class="tags-view-item"
        :style="activeStyle(tag)"
        @click.middle="!isAffix(tag) ? closeSelectedTag(tag) : undefined"
        @contextmenu.prevent="openMenu(tag, $event)"
      >
        <svg-icon v-if="tagsIcon && tag.meta && tag.meta.icon && tag.meta.icon !== '#'" :icon-class="tag.meta.icon" />
        <span class="tags-view-item-title">{{ tag.title || tag.meta?.title }}</span>
        <span v-if="!isAffix(tag)" @click.prevent.stop="closeSelectedTag(tag)">
          <Close class="el-icon-close" style="width: 1em; height: 1em; vertical-align: middle" />
        </span>
      </router-link>
    </scroll-pane>

    <span v-if="canScrollRight" class="tags-nav-btn tags-nav-btn--right" @click="scrollRight">
      <el-icon><ArrowRight /></el-icon>
    </span>

    <el-dropdown class="tags-action-dropdown" trigger="click" placement="bottom-end" @command="handleDropdownCommand">
      <span class="tags-action-btn">
        <el-icon><ArrowDown /></el-icon>
      </span>
      <template #dropdown>
        <el-dropdown-menu class="tags-dropdown-menu">
          <el-dropdown-item v-if="!isAffix(selectedDropdownTag)" command="close">关闭当前</el-dropdown-item>
          <el-dropdown-item command="closeOthers">关闭其他</el-dropdown-item>
          <el-dropdown-item command="closeLeft" :disabled="isFirstView()">关闭左侧</el-dropdown-item>
          <el-dropdown-item command="closeRight" :disabled="isLastView()">关闭右侧</el-dropdown-item>
          <el-dropdown-item command="closeAll">全部关闭</el-dropdown-item>
          <el-dropdown-item command="fullscreen" divided>
            <template v-if="!isFullscreen">
              <FullScreen />
              <span>全屏显示</span>
            </template>
            <template v-else>
              <CloseBold />
              <span>退出全屏</span>
            </template>
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <span class="tags-action-btn tags-refresh-btn" title="刷新页面" @click="refreshSelectedTag(selectedDropdownTag)">
      <el-icon><RefreshRight /></el-icon>
      <span>刷新</span>
    </span>

    <ul v-show="visible" :style="{ left: left + 'px', top: top + 'px' }" class="contextmenu">
      <li @click="refreshSelectedTag(selectedTag)">
        <RefreshRight style="width: 1em; height: 1em" />
        刷新页面
      </li>
      <li v-if="!isAffix(selectedTag)" @click="closeSelectedTag(selectedTag)">
        <Close style="width: 1em; height: 1em" />
        关闭当前
      </li>
      <li @click="closeOthersTags">
        <CircleClose style="width: 1em; height: 1em" />
        关闭其他
      </li>
      <li v-if="!isFirstView()" @click="closeLeftTags">
        <Back style="width: 1em; height: 1em" />
        关闭左侧
      </li>
      <li v-if="!isLastView()" @click="closeRightTags">
        <Right style="width: 1em; height: 1em" />
        关闭右侧
      </li>
      <li @click="closeAllTags(selectedTag)">
        <CircleClose style="width: 1em; height: 1em" />
        全部关闭
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import type { RouteLocationNormalized, RouteRecordRaw } from 'vue-router';
import {
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  Back,
  CircleClose,
  Close,
  CloseBold,
  FullScreen,
  RefreshRight,
  Right
} from '@element-plus/icons-vue';
import tab from '@/plugins/tab';
import { usePermissionStore } from '@/store/modules/permission';
import { useSettingsStore } from '@/store/modules/settings';
import { useTagsViewStore } from '@/store/modules/tagsView';
import { getNormalPath } from '@/utils/ruoyi';
import ScrollPane from './ScrollPane.vue';

const visible = ref(false);
const top = ref(0);
const left = ref(0);
const selectedTag = ref<RouteLocationNormalized>();
const affixTags = ref<RouteLocationNormalized[]>([]);
const canScrollLeft = ref(false);
const canScrollRight = ref(false);
const isFullscreen = ref(false);
const scrollPaneRef = ref<InstanceType<typeof ScrollPane>>();
const fullscreenModeClass = 'tags-fullscreen-mode';

const route = useRoute();
const router = useRouter();
const settingsStore = useSettingsStore();
const permissionStore = usePermissionStore();
const tagsViewStore = useTagsViewStore();

const visitedViews = computed(() => tagsViewStore.getVisitedViews());
const routes = computed(() => permissionStore.getRoutes());
const tagsIcon = computed(() => settingsStore.tagsIcon);
const selectedDropdownTag = computed<RouteLocationNormalized | undefined>(() => {
  return visitedViews.value.find(tag => isActive(tag)) || selectedTag.value;
});

watch(route, () => {
  addTags();
  moveToCurrentTag();
});

watch(visible, value => {
  if (value) {
    document.body.addEventListener('click', closeMenu);
  } else {
    document.body.removeEventListener('click', closeMenu);
  }
});

watch(
  visitedViews,
  () => {
    nextTick(() => {
      updateArrowState();
    });
  },
  { deep: true }
);

const isActive = (currentRoute: RouteLocationNormalized): boolean => {
  return currentRoute.path === route.path;
};

const activeStyle = (tag: RouteLocationNormalized) => {
  if (!isActive(tag)) return {};
  return {
    backgroundColor: 'var(--tags-view-active-bg)',
    borderColor: 'var(--tags-view-active-border-color)'
  };
};

const isAffix = (tag?: RouteLocationNormalized) => {
  return !!tag?.meta?.affix;
};

const getOperateTag = () => {
  return selectedTag.value?.fullPath ? selectedTag.value : selectedDropdownTag.value;
};

const isFirstView = () => {
  const tag = getOperateTag();
  if (!tag) {
    return false;
  }
  try {
    return tag.fullPath === '/index' || tag.fullPath === visitedViews.value[1]?.fullPath;
  } catch {
    return false;
  }
};

const isLastView = () => {
  const tag = getOperateTag();
  if (!tag) {
    return false;
  }
  try {
    return tag.fullPath === visitedViews.value[visitedViews.value.length - 1]?.fullPath;
  } catch {
    return false;
  }
};

const filterAffixTags = (routeList: RouteRecordRaw[], basePath = '') => {
  let tags: RouteLocationNormalized[] = [];
  routeList.forEach(item => {
    if (item.meta?.affix) {
      const tagPath = getNormalPath(basePath + '/' + item.path);
      tags.push({
        hash: '',
        matched: [],
        params: {},
        query: {},
        redirectedFrom: undefined,
        fullPath: tagPath,
        path: tagPath,
        name: item.name as string,
        meta: { ...item.meta },
        title: item.meta?.title || 'no-name'
      } as RouteLocationNormalized);
    }
    if (item.children) {
      const tempTags = filterAffixTags(item.children, item.path);
      if (tempTags.length >= 1) {
        tags = [...tags, ...tempTags];
      }
    }
  });
  return tags;
};

const initTags = () => {
  if (settingsStore.tagsViewPersist) {
    tagsViewStore.loadPersistedViews();
  }
  const tags = filterAffixTags(routes.value);
  affixTags.value = tags;
  for (const tag of tags) {
    if (tag.name) {
      tagsViewStore.addAffixView(tag);
    }
  }
};

const addTags = () => {
  if (typeof route.query.title === 'string') {
    route.meta.title = route.query.title;
  }
  if (route.name) {
    tagsViewStore.addView(route as RouteLocationNormalized);
  }
};

const moveToCurrentTag = () => {
  nextTick(() => {
    for (const item of visitedViews.value) {
      if (item.path === route.path) {
        scrollPaneRef.value?.moveToTarget(item);
        if (item.fullPath !== route.fullPath) {
          tagsViewStore.updateVisitedView(route as RouteLocationNormalized);
        }
        break;
      }
    }
  });
};

const refreshSelectedTag = (view?: RouteLocationNormalized) => {
  if (!view) {
    return;
  }
  tab.refreshPage(view);
  if (route.meta.link) {
    tagsViewStore.delIframeView(route as RouteLocationNormalized);
  }
};

const closeSelectedTag = (view?: RouteLocationNormalized) => {
  if (!view) {
    return;
  }
  tab.closePage(view).then(({ visitedViews }: { visitedViews: RouteLocationNormalized[] }) => {
    if (isActive(view)) {
      toLastView(visitedViews, view);
    }
  });
};

const closeRightTags = () => {
  const tag = getOperateTag();
  if (!tag) {
    return;
  }
  tab.closeRightPage(tag).then((views: RouteLocationNormalized[]) => {
    if (!views.find(item => item.fullPath === route.fullPath)) {
      toLastView(views);
    }
  });
};

const closeLeftTags = () => {
  const tag = getOperateTag();
  if (!tag) {
    return;
  }
  tab.closeLeftPage(tag).then((views: RouteLocationNormalized[]) => {
    if (!views.find(item => item.fullPath === route.fullPath)) {
      toLastView(views);
    }
  });
};

const closeOthersTags = () => {
  const tag = getOperateTag();
  if (!tag) {
    return;
  }
  router.push(tag.fullPath || tag.path || '/').catch(() => {});
  tab.closeOtherPage(tag).then(() => {
    moveToCurrentTag();
  });
};

const closeAllTags = (view?: RouteLocationNormalized) => {
  tab.closeAllPage().then(({ visitedViews: views }: { visitedViews: RouteLocationNormalized[] }) => {
    if (affixTags.value.some(tag => tag.path === route.path)) {
      return;
    }
    toLastView(views, view);
  });
};

const toLastView = (views: RouteLocationNormalized[], view?: RouteLocationNormalized) => {
  const latestView = views.slice(-1)[0];
  if (latestView?.fullPath) {
    router.push(latestView.fullPath);
  } else if (view?.name === 'Dashboard' && view.fullPath) {
    router.replace({ path: '/redirect' + view.fullPath });
  } else {
    router.push('/');
  }
};

const scrollLeft = () => {
  if (!canScrollLeft.value) {
    return;
  }
  scrollPaneRef.value?.scrollToStart();
};

const scrollRight = () => {
  if (!canScrollRight.value) {
    return;
  }
  scrollPaneRef.value?.scrollToEnd();
};

const updateArrowState = () => {
  nextTick(() => {
    const state = scrollPaneRef.value?.getScrollState();
    canScrollLeft.value = !!state?.canLeft;
    canScrollRight.value = !!state?.canRight;
  });
};

const syncFullscreenLayout = () => {
  if (!isFullscreen.value) {
    return;
  }
  const mainContainer = document.querySelector('.main-container') as HTMLElement | null;
  const layoutHeader = mainContainer?.querySelector('.layout-header') as HTMLElement | null;
  if (!mainContainer || !layoutHeader) {
    return;
  }
  const headerHeight = Math.ceil(layoutHeader.getBoundingClientRect().height);
  mainContainer.style.setProperty('--tags-fullscreen-header-height', `${headerHeight}px`);
};

const enterFullscreenMode = async () => {
  const mainContainer = document.querySelector('.main-container') as HTMLElement | null;
  if (!mainContainer) {
    return;
  }
  document.body.classList.add(fullscreenModeClass);
  mainContainer.classList.add(fullscreenModeClass);
  isFullscreen.value = true;
  await nextTick();
  syncFullscreenLayout();
};

const exitFullscreenMode = () => {
  const mainContainer = document.querySelector('.main-container') as HTMLElement | null;
  document.body.classList.remove(fullscreenModeClass);
  mainContainer?.classList.remove(fullscreenModeClass);
  mainContainer?.style.removeProperty('--tags-fullscreen-header-height');
  document.querySelector<HTMLElement>('.tags-action-dropdown .tags-action-btn')?.blur();
  isFullscreen.value = false;
};

const toggleFullscreen = async () => {
  if (isFullscreen.value) {
    exitFullscreenMode();
    return;
  }
  await enterFullscreenMode();
};

const handleKeyDown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && isFullscreen.value) {
    exitFullscreenMode();
  }
};

const handleResize = () => {
  updateArrowState();
  syncFullscreenLayout();
};

const handleDropdownCommand = (command: string) => {
  const tag = selectedDropdownTag.value;
  if (!tag) {
    return;
  }
  selectedTag.value = tag;
  switch (command) {
    case 'fullscreen':
      toggleFullscreen();
      break;
    case 'close':
      closeSelectedTag(tag);
      break;
    case 'closeOthers':
      closeOthersTags();
      break;
    case 'closeLeft':
      closeLeftTags();
      break;
    case 'closeRight':
      closeRightTags();
      break;
    case 'closeAll':
      closeAllTags(tag);
      break;
    default:
      break;
  }
};

const openMenu = (tag: RouteLocationNormalized, e: MouseEvent) => {
  left.value = e.clientX;
  top.value = e.clientY;
  visible.value = true;
  selectedTag.value = tag;
};

const closeMenu = () => {
  visible.value = false;
};

const handleScroll = () => {
  closeMenu();
  updateArrowState();
};

onMounted(() => {
  initTags();
  addTags();
  updateArrowState();
  window.addEventListener('resize', handleResize);
  window.addEventListener('keydown', handleKeyDown);
});

onBeforeUnmount(() => {
  exitFullscreenMode();
  window.removeEventListener('resize', handleResize);
  window.removeEventListener('keydown', handleKeyDown);
  document.body.removeEventListener('click', closeMenu);
});
</script>

<style lang="scss" scoped>
.tags-view-container {
  display: flex;
  align-items: flex-start;
  height: 38px;
  width: 100%;
  background-color: var(--app-surface-bg);
  border: 1px solid var(--app-surface-border);
  border-radius: var(--app-radius-md);
  box-shadow: var(--app-shadow-sm);

  $btn-width: 26px;
  $btn-hover-bg: var(--el-fill-color-light);
  $btn-hover-color: var(--el-text-color-primary);

  .tags-nav-btn {
    flex-shrink: 0;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: $btn-width;
    height: 26px;
    margin-top: 4px;
    cursor: pointer;
    color: var(--app-text-muted);
    user-select: none;
    background-color: var(--app-surface-bg);
    border: 1px solid var(--app-surface-border);
    border-radius: var(--app-radius-md);
    transition:
      box-shadow 0.2s ease,
      transform 0.2s ease,
      border-color 0.2s ease,
      color 0.2s ease;

    &:hover:not(.disabled) {
      background: $btn-hover-bg;
      color: $btn-hover-color;
      border-color: var(--el-color-primary-light-5);
      box-shadow: var(--app-shadow-sm);
      transform: translateY(-1px);
    }
  }

  .tags-nav-btn--left {
    margin-left: 8px;
    margin-right: 4px;
  }

  .tags-nav-btn--right {
    margin-left: 4px;
    margin-right: 4px;
  }

  .tags-view-wrapper {
    flex: 1;
    min-width: 0;

    .tags-view-item {
      display: inline-flex;
      align-items: center;
      position: relative;
      cursor: pointer;
      height: 26px;
      line-height: 25px;
      background-color: var(--app-surface-bg);
      border: 1px solid var(--app-surface-border);
      color: var(--el-text-color-regular);
      padding: 0 8px;
      font-size: 12px;
      margin-left: 5px;
      margin-top: 4px;
      border-radius: var(--app-radius-md);
      transition:
        box-shadow 0.2s ease,
        transform 0.2s ease,
        border-color 0.2s ease,
        color 0.2s ease;

      &:hover {
        color: var(--el-color-primary);
        border-color: var(--el-color-primary-light-5);
        box-shadow: var(--app-shadow-sm);
        transform: translateY(-1px);
      }

      &:first-of-type {
        margin-left: 10px;
      }

      &:last-of-type {
        margin-right: 10px;
      }

      &.active {
        background-color: var(--tags-view-active-bg);
        color: var(--el-color-white);
        border-color: var(--tags-view-active-border-color);

        &::before {
          content: '';
          background: var(--el-color-white);
          display: inline-block;
          width: 8px;
          height: 8px;
          border-radius: 50%;
          position: relative;
          margin-right: 5px;
        }
      }
    }
  }

  .tags-view-item.active.has-icon::before {
    content: none !important;
  }

  .tags-view-item-title {
    margin-left: 4px;
    margin-right: 3px;
  }

  .tags-action-dropdown {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    margin-top: 4px;
    margin-left: 4px;
  }

  .tags-action-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    min-width: $btn-width;
    height: 26px;
    padding: 0 8px;
    cursor: pointer;
    color: var(--app-text-muted);
    user-select: none;
    background-color: var(--app-surface-bg);
    border: 1px solid var(--app-surface-border);
    border-radius: var(--app-radius-md);
    transition:
      box-shadow 0.2s ease,
      transform 0.2s ease,
      border-color 0.2s ease,
      color 0.2s ease;

    &:hover {
      background: $btn-hover-bg;
      color: $btn-hover-color;
      border-color: var(--el-color-primary-light-5);
      box-shadow: var(--app-shadow-sm);
      transform: translateY(-1px);
    }
  }

  .tags-refresh-btn {
    width: auto;
    font-size: 12px;
    margin-top: 4px;
    margin-left: 8px;
    margin-right: 8px;
  }

  .contextmenu {
    margin: 0;
    background: var(--app-surface-bg);
    z-index: 3000;
    position: fixed;
    list-style-type: none;
    padding: 5px 0;
    border-radius: var(--app-radius-md);
    font-size: 12px;
    font-weight: 400;
    box-shadow: var(--app-shadow-md);

    li {
      margin: 0;
      padding: 7px 16px;
      cursor: pointer;

      &:hover {
        background: var(--el-fill-color-light);
      }
    }
  }
}
</style>

<style lang="scss">
.tags-view-wrapper {
  .tags-view-item {
    .el-icon-close {
      width: 16px;
      height: 16px;
      vertical-align: 2px;
      border-radius: 50%;
      text-align: center;
      transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
      transform-origin: 100% 50%;

      &:before {
        transform: scale(0.6);
        display: inline-block;
        vertical-align: -3px;
      }

      &:hover {
        background-color: var(--app-surface-border);
        color: var(--el-color-white);
        width: 12px !important;
        height: 12px !important;
      }
    }
  }
}

body.tags-fullscreen-mode {
  overflow: hidden;
}

body.tags-fullscreen-mode .drawer-bg,
body.tags-fullscreen-mode .sidebar-container,
body.tags-fullscreen-mode .navbar {
  display: none !important;
}

.main-container.tags-fullscreen-mode {
  position: fixed !important;
  inset: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  margin-left: 0 !important;
  z-index: 2000;
  overflow: hidden;
  background: var(--app-shell-bg);
}

.main-container.tags-fullscreen-mode .layout-header {
  gap: 0;
  padding: 12px 12px 0;
}

.main-container.tags-fullscreen-mode .layout-header.fixed-header {
  position: relative;
  top: 0;
  right: auto;
  width: auto !important;
}

.main-container.tags-fullscreen-mode .app-main,
.main-container.tags-fullscreen-mode .app-main.with-fixed-header,
.main-container.tags-fullscreen-mode .app-main.with-fixed-header.with-tags-view {
  height: calc(100vh - var(--tags-fullscreen-header-height, 50px));
  min-height: calc(100vh - var(--tags-fullscreen-header-height, 50px)) !important;
  padding-top: 12px !important;
  overflow: auto;
}
</style>
