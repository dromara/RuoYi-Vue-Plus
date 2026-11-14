<template>
  <div class="layout-search-dialog">
    <el-dialog
      v-model="state.isShowSearch"
      width="640px"
      destroy-on-close
      append-to-body
      :show-close="false"
      @close="handleClose"
      @opened="onDialogOpened"
    >
      <el-input
        ref="searchInputRef"
        v-model="state.menuQuery"
        size="large"
        clearable
        placeholder="菜单搜索，支持标题、URL模糊查询"
        @input="querySearch"
        @keydown="handleSearchKeydown"
      >
        <template #prefix>
          <svg-icon class-name="search-icon" icon-class="search" />
        </template>
      </el-input>

      <div v-if="state.menuQuery && state.options.length > 0" class="result-count">
        找到
        <strong>{{ state.options.length }}</strong>
        个结果
      </div>

      <el-scrollbar wrap-class="layout-search-scrollbar">
        <div class="result-wrap">
          <template v-if="state.options.length > 0">
            <div
              v-for="(item, index) in state.options"
              :key="item.path + item.fullTitle"
              class="search-item"
              :class="{ 'is-active': index === state.activeIndex }"
              :style="activeStyle(index)"
              @mouseenter="state.activeIndex = index"
              @mouseleave="state.activeIndex = -1"
              @click="handleSelect(item)"
            >
              <div class="search-item__icon">
                <svg-icon v-if="item.icon" :icon-class="item.icon" class-name="menu-icon" />
                <svg-icon v-else icon-class="guide" class-name="menu-icon" />
              </div>
              <div class="search-item__info">
                <div class="menu-title" v-html="highlightText(item.fullTitle)"></div>
                <div class="menu-path" v-html="highlightText(item.path)"></div>
              </div>
              <svg-icon v-show="index === state.activeIndex" icon-class="enter" class-name="search-enter" />
            </div>
          </template>

          <div v-else-if="state.menuQuery" class="empty-state">
            <el-icon class="empty-icon"><Search /></el-icon>
            <p class="empty-text">
              未找到 "
              <strong>{{ state.menuQuery }}</strong>
              " 相关菜单
            </p>
            <p class="empty-tip">试试其他关键词或路径</p>
          </div>
        </div>
      </el-scrollbar>

      <div class="search-footer">
        <span class="shortcut-item">
          <kbd>↑</kbd>
          <kbd>↓</kbd>
          切换
        </span>
        <span class="shortcut-item">
          <kbd>↵</kbd>
          选择
        </span>
        <span class="shortcut-item">
          <kbd>Esc</kbd>
          关闭
        </span>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="layoutBreadcrumbSearch">
import type { RouteRecordRaw } from 'vue-router';
import { Search } from '@element-plus/icons-vue';
import { usePermissionStore } from '@/store/modules/permission';
import { useSettingsStore } from '@/store/modules/settings';
import { getNormalPath } from '@/utils/ruoyi';
import { isHttp } from '@/utils/validate';

type SearchMenuItem = {
  path: string;
  icon: string;
  title: string[];
  fullTitle: string;
  query?: string;
};

type SearchState = {
  isShowSearch: boolean;
  menuQuery: string;
  menuList: SearchMenuItem[];
  options: SearchMenuItem[];
  activeIndex: number;
};

const router = useRouter();
const permissionStore = usePermissionStore();
const settingsStore = useSettingsStore();
const searchInputRef = ref<any>();

const routes = computed(() => permissionStore.defaultRoutes);
const theme = computed(() => settingsStore.theme);

const state = reactive<SearchState>({
  isShowSearch: false,
  menuQuery: '',
  menuList: [],
  options: [],
  activeIndex: -1
});

const buildSearchPool = () => {
  state.menuList = generateRoutes(routes.value as RouteRecordRaw[]);
  if (!state.menuQuery) {
    state.options = state.menuList;
  }
};

const openSearch = () => {
  state.menuQuery = '';
  state.activeIndex = -1;
  buildSearchPool();
  state.isShowSearch = true;
};

const onDialogOpened = () => {
  nextTick(() => {
    searchInputRef.value?.focus?.();
  });
};

const handleClose = () => {
  searchInputRef.value?.blur?.();
  state.menuQuery = '';
  state.activeIndex = -1;
  state.options = state.menuList;
  state.isShowSearch = false;
};

const generateRoutes = (routeList: RouteRecordRaw[], basePath = '', prefixTitle: string[] = []): SearchMenuItem[] => {
  let result: SearchMenuItem[] = [];
  routeList.forEach(route => {
    if (route.hidden) {
      return;
    }

    const currentPath = route.path?.startsWith('/') ? route.path : `/${route.path ?? ''}`;
    const data: SearchMenuItem = {
      path: !isHttp(route.path || '') ? getNormalPath(basePath + currentPath) : String(route.path || ''),
      icon: String(route.meta?.icon || ''),
      title: [...prefixTitle],
      fullTitle: ''
    };

    if (route.meta?.title) {
      data.title = [...data.title, String(route.meta.title)];
      data.fullTitle = data.title.join(' / ');
      if (route.redirect !== 'noRedirect') {
        result.push(data);
      }
    }

    if (route.query) {
      data.query = String(route.query);
    }

    if (route.children?.length) {
      result = [...result, ...generateRoutes(route.children, data.path, data.title)];
    }
  });
  return result;
};

const querySearch = (query: string) => {
  state.activeIndex = -1;
  if (!query) {
    state.options = state.menuList;
    return;
  }

  const keyword = query.toLowerCase();
  state.options = state.menuList.filter(item => {
    return item.fullTitle.toLowerCase().includes(keyword) || item.path.toLowerCase().includes(keyword);
  });
};

const navigateResult = (direction: 'up' | 'down') => {
  if (!state.options.length) {
    return;
  }

  if (direction === 'up') {
    state.activeIndex = state.activeIndex <= 0 ? state.options.length - 1 : state.activeIndex - 1;
    return;
  }

  state.activeIndex = state.activeIndex >= state.options.length - 1 ? 0 : state.activeIndex + 1;
};

const handleSearchKeydown = (event: KeyboardEvent) => {
  if (event.key === 'ArrowUp') {
    event.preventDefault();
    navigateResult('up');
    return;
  }
  if (event.key === 'ArrowDown') {
    event.preventDefault();
    navigateResult('down');
    return;
  }
  if (event.key === 'Enter') {
    event.preventDefault();
    selectActiveResult();
  }
};

const selectActiveResult = () => {
  if (state.options.length === 0) {
    return;
  }

  const current = state.activeIndex >= 0 ? state.options[state.activeIndex] : state.options[0];
  handleSelect(current);
};

const handleSelect = async (item: SearchMenuItem) => {
  if (isHttp(item.path)) {
    const startIndex = item.path.indexOf('http');
    window.open(item.path.substring(startIndex), '_blank');
  } else if (item.query) {
    try {
      await router.push({ path: item.path, query: JSON.parse(item.query) });
    } catch {
      await router.push(item.path);
    }
  } else {
    await router.push(item.path);
  }

  handleClose();
};

const activeStyle = (index: number) => {
  if (index !== state.activeIndex) {
    return {};
  }
  return {
    backgroundColor: theme.value,
    color: '#fff'
  };
};

const highlightText = (text: string) => {
  if (!text || !state.menuQuery) {
    return text;
  }

  const escapedKeyword = escapeRegExp(state.menuQuery);
  const reg = new RegExp(`(${escapedKeyword})`, 'gi');
  return text.replace(reg, '<span class="highlight">$1</span>');
};

const escapeRegExp = (value: string) => {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
};

watch(
  routes,
  () => {
    buildSearchPool();
  },
  { deep: true, immediate: true }
);

defineExpose({
  openSearch
});
</script>

<style lang="scss" scoped>
.layout-search-dialog {
  :deep(.el-dialog) {
    border-radius: 22px;
    overflow: hidden;
    padding: 0;
  }

  :deep(.el-dialog__header) {
    display: none;
  }

  :deep(.el-dialog__body) {
    padding: 18px 18px 0;
  }

  :deep(.el-input__wrapper) {
    min-height: 52px;
    border-radius: var(--app-radius-base);
  }

  :deep(.highlight) {
    color: #ef4444;
    font-weight: 600;
  }

  :deep(.is-active .highlight) {
    color: rgba(255, 255, 255, 0.92);
  }
}

.result-count {
  padding: 10px 6px 0;
  font-size: 12px;
  color: var(--app-text-muted);

  strong {
    color: #ef4444;
    font-weight: 600;
  }
}

.result-wrap {
  height: 300px;
  margin: 8px 0 0;
}

.search-item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 56px;
  margin-bottom: 6px;
  padding: 10px 12px;
  border-radius: var(--app-radius-base);
  cursor: pointer;
  transition:
    background-color 0.18s ease,
    transform 0.18s ease,
    color 0.18s ease;

  &:hover {
    transform: translateY(-1px);
    background: rgba(64, 158, 255, 0.08);
  }

  &.is-active {
    transform: none;
  }
}

.search-item__icon {
  width: 28px;
  display: inline-flex;
  justify-content: center;
  flex-shrink: 0;

  .menu-icon {
    width: 18px;
    height: 18px;
  }
}

.search-item__info {
  flex: 1;
  min-width: 0;
}

.menu-title,
.menu-path {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.menu-title {
  font-size: 14px;
  font-weight: 600;
  color: inherit;
}

.menu-path {
  margin-top: 4px;
  font-size: 12px;
  color: var(--app-text-muted);
}

.search-item.is-active .menu-path {
  color: rgba(255, 255, 255, 0.8);
}

.search-enter {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--app-text-muted);
}

.empty-icon {
  font-size: 40px;
  margin-bottom: 14px;
  color: var(--app-text-muted);
}

.empty-text {
  margin: 0 0 6px;
  font-size: 14px;

  strong {
    color: var(--app-text-title);
  }
}

.empty-tip {
  margin: 0;
  font-size: 12px;
}

.search-footer {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 14px 18px 16px;
  border-top: 1px solid var(--app-surface-border);
  color: var(--app-text-muted);
  font-size: 12px;
}

.shortcut-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  border: 1px solid var(--app-surface-border);
  border-radius: 6px;
  background: var(--app-elevated-soft-bg);
  color: var(--app-text-title);
  font-size: 11px;
  line-height: 1;
  box-shadow: inset 0 -1px 0 var(--app-surface-border);
}
</style>
