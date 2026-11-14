<template>
  <div class="navbar" :class="'nav' + navType">
    <div class="navbar-left">
      <div v-if="navType !== NavTypeEnum.TOP" class="hamburger-shell">
        <hamburger
          id="hamburger-container"
          :is-active="appStore.sidebar.opened"
          class="hamburger-container"
          @toggle-click="toggleSideBar"
        />
      </div>
      <router-link v-else-if="showLogo" to="/" class="navtop-logo-shell">
        <img :src="appLogo" class="navtop-logo-icon" alt="logo" />
      </router-link>

      <div class="nav-context">
        <breadcrumb v-if="navType == NavTypeEnum.LEFT" id="breadcrumb-container" class="breadcrumb-container" />
        <top-nav v-if="navType == NavTypeEnum.MIX" id="topmenu-container" class="topmenu-container" />

        <template v-if="navType == NavTypeEnum.TOP">
          <top-bar id="topbar-container" class="topbar-container" />
        </template>
      </div>
    </div>
    <div class="right-menu flex align-center">
      <template v-if="appStore.device !== 'mobile'">
        <search-menu ref="searchMenuRef" />
        <el-tooltip content="搜索" effect="dark" placement="bottom">
          <div class="right-menu-item hover-effect" @click="openSearchMenu">
            <svg-icon class-name="search-icon" icon-class="search" />
          </div>
        </el-tooltip>
        <!-- 消息 -->
        <el-tooltip :content="$t('navbar.message')" effect="dark" placement="bottom">
          <div>
            <el-popover placement="bottom" trigger="click" transition="el-zoom-in-top" :width="300" :persistent="false">
              <template #reference>
                <el-badge :value="noticeStore.unreadCount.value > 0 ? noticeStore.unreadCount.value : ''" :max="99">
                  <div class="right-menu-item hover-effect message-trigger">
                    <svg-icon icon-class="message" />
                  </div>
                </el-badge>
              </template>
              <template #default>
                <notice></notice>
              </template>
            </el-popover>
          </div>
        </el-tooltip>
        <el-tooltip content="Github" effect="dark" placement="bottom">
          <ruo-yi-git id="ruoyi-git" class="right-menu-item hover-effect" />
        </el-tooltip>

        <el-tooltip :content="$t('navbar.document')" effect="dark" placement="bottom">
          <ruo-yi-doc id="ruoyi-doc" class="right-menu-item hover-effect" />
        </el-tooltip>

        <el-tooltip :content="$t('navbar.full')" effect="dark" placement="bottom">
          <screenfull id="screenfull" class="right-menu-item hover-effect" />
        </el-tooltip>

        <el-tooltip :content="$t('navbar.language')" effect="dark" placement="bottom">
          <lang-select id="lang-select" class="right-menu-item hover-effect" />
        </el-tooltip>

        <el-tooltip :content="$t('navbar.layoutSize')" effect="dark" placement="bottom">
          <size-select id="size-select" class="right-menu-item hover-effect" />
        </el-tooltip>
      </template>
      <div class="avatar-container">
        <el-dropdown class="avatar-dropdown" trigger="click" @command="handleCommand">
          <div class="avatar-wrapper">
            <img :src="userStore.avatar" class="user-avatar" />
            <div class="avatar-meta">
              <span class="avatar-name">{{ displayName }}</span>
              <span class="avatar-role">Workspace</span>
            </div>
            <el-icon class="avatar-arrow"><caret-bottom /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <router-link to="/user/profile">
                <el-dropdown-item>{{ $t('navbar.personalCenter') }}</el-dropdown-item>
              </router-link>
              <el-dropdown-item v-if="settingsStore.showSettings" command="setLayout">
                <span>{{ $t('navbar.layoutSetting') }}</span>
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <span>{{ $t('navbar.logout') }}</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ElMessageBoxOptions } from 'element-plus';
import { CaretBottom } from '@element-plus/icons-vue';
import appLogo from '@/assets/logo/logo.png';
import { NavTypeEnum } from '@/enums/NavTypeEnum';
import tab from '@/plugins/tab';
import router from '@/router';
import { useAppStore } from '@/store/modules/app';
import { useNoticeStore } from '@/store/modules/notice';
import { useSettingsStore } from '@/store/modules/settings';
import { useUserStore } from '@/store/modules/user';
import notice from './notice/index.vue';
import TopBar from './TopBar/index.vue';
import SearchMenu from './TopBar/search.vue';

const appStore = useAppStore();
const userStore = useUserStore();
const settingsStore = useSettingsStore();
const noticeStore = storeToRefs(useNoticeStore());

const navType = computed(() => settingsStore.navType);
const showLogo = computed(() => settingsStore.sidebarLogo);
const displayName = computed(() => userStore.nickname || '管理员');

// 搜索菜单
const searchMenuRef = ref<InstanceType<typeof SearchMenu>>();

const openSearchMenu = () => {
  searchMenuRef.value?.openSearch();
};

const toggleSideBar = () => {
  appStore.toggleSideBar(false);
};

const logout = async () => {
  await ElMessageBox.confirm('确定注销并退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  } as ElMessageBoxOptions);
  userStore.logout().then(() => {
    router.replace({
      path: '/login',
      query: {
        redirect: encodeURIComponent(router.currentRoute.value.fullPath || '/')
      }
    });
    tab.closeAllPage();
  });
};

const emits = defineEmits(['setLayout']);
const setLayout = () => {
  emits('setLayout');
};
// 定义Command方法对象 通过key直接调用方法
const commandMap: { [key: string]: any } = {
  setLayout,
  logout
};
const handleCommand = (command: string) => {
  // 判断是否存在该方法
  if (commandMap[command]) {
    commandMap[command]();
  }
};
</script>

<style lang="scss" scoped>
.navbar.navtop {
  .nav-context {
    flex: 1;
  }

  .navtop-logo-shell {
    width: 48px;
    height: 40px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    margin-right: 12px;
    flex-shrink: 0;
    border-radius: 14px;
    border: 1px solid var(--app-surface-border);
    background: var(--app-surface-bg);
    box-shadow: var(--app-shadow-sm);
    transition:
      transform 0.2s ease,
      border-color 0.2s ease,
      box-shadow 0.2s ease;

    &:hover {
      transform: translateY(-1px);
      border-color: rgba(64, 158, 255, 0.22);
      box-shadow:
        inset 0 1px 0 rgba(255, 255, 255, 0.76),
        0 10px 22px rgba(15, 23, 42, 0.08);
    }
  }

  .navtop-logo-icon {
    width: 32px;
    height: 32px;
    display: block;
    border-radius: 11px;
  }

  .topbar-container {
    flex: 1;
    min-width: 0;
    margin-left: 0;
    padding: 4px 8px;
    border-radius: var(--app-radius-base);
    background: var(--app-surface-bg);
    border: 1px solid var(--app-surface-border);
    box-shadow: var(--app-shadow-sm);
  }
}

:deep(.el-select .el-input__wrapper) {
  height: 30px;
}

:deep(.el-badge__content.is-fixed) {
  top: 8px;
  right: 6px;
}

:deep(.el-badge) {
  display: inline-flex;
  align-items: center;
}

:deep(.el-dropdown) {
  outline: none;
}

.flex {
  display: flex;
}

.align-center {
  align-items: center;
}

.navbar {
  min-height: 52px;
  overflow: hidden;
  position: relative;
  background: var(--app-navbar-bg);
  border: 1px solid var(--app-navbar-border);
  box-shadow: var(--app-navbar-shadow);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: var(--app-radius-base);
  padding: 6px 12px;
  box-sizing: border-box;

  .navbar-left {
    display: flex;
    align-items: center;
    min-width: 0;
    gap: 10px;
    flex: 1;
  }

  .hamburger-shell {
    width: 36px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 12px;
    background: transparent;
    color: var(--app-accent-strong);
    flex-shrink: 0;
    border: 1px solid var(--app-surface-border);
  }

  .hamburger-container {
    line-height: 32px;
    height: 100%;
    cursor: pointer;
    transition: background 0.3s;
    -webkit-tap-highlight-color: transparent;
    display: flex;
    align-items: center;
    flex-shrink: 0;
    justify-content: center;

    &:hover {
      background: transparent;
    }
  }

  .nav-context {
    min-width: 0;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 0;
  }

  .breadcrumb-container {
    flex-shrink: 0;
  }

  .topmenu-container {
    position: static;
    min-width: 0;
  }

  .topbar-container {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    overflow: hidden;
    margin-left: 8px;
  }

  .right-menu {
    height: 100%;
    line-height: 1;
    display: flex;
    align-items: center;
    gap: 6px;
    margin-left: auto;
    flex-wrap: nowrap;

    &:focus {
      outline: none;
    }

    > * {
      flex-shrink: 0;
    }

    .right-menu-item {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      font-size: 16px;
      color: var(--app-text-muted);
      border-radius: 12px;
      vertical-align: text-bottom;
      background: transparent;
      border: 1px solid transparent;
      flex-shrink: 0;

      :deep(.svg-icon),
      :deep(svg),
      :deep(.el-icon) {
        width: 16px;
        height: 16px;
        font-size: 16px;
        display: block;
      }

      &.hover-effect {
        cursor: pointer;
        transition:
          background 0.3s,
          color 0.3s;

        &:hover {
          background: var(--app-accent-soft);
          color: var(--app-accent-strong);
          border-color: rgba(64, 158, 255, 0.16);
        }
      }
    }

    .message-trigger {
      display: inline-flex;
    }

    .avatar-container {
      margin-left: 6px;
      margin-right: 0;
      flex-shrink: 0;

      .avatar-dropdown {
        display: block;
        width: auto;
        height: auto;
        border: none;
        background: transparent;
      }

      .avatar-wrapper {
        position: relative;
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 4px 8px 4px 4px;
        border-radius: var(--app-radius-base);
        background: var(--app-surface-bg);
        border: 1px solid var(--app-surface-border);
        min-width: 0;
        cursor: pointer;
        transition:
          background 0.3s,
          border-color 0.3s;

        &:hover {
          background: var(--app-accent-soft);
          border-color: rgba(64, 158, 255, 0.16);
        }

        .user-avatar {
          cursor: pointer;
          width: 28px;
          height: 28px;
          border-radius: 12px;
          object-fit: cover;
          box-shadow: none;
        }

        .avatar-meta {
          display: flex;
          flex-direction: column;
          min-width: 0;
          gap: 2px;
        }

        .avatar-name {
          max-width: 88px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          color: var(--app-text-title);
          font-size: 12px;
          font-weight: 600;
        }

        .avatar-role {
          color: var(--app-text-muted);
          font-size: 11px;
        }

        .avatar-arrow {
          color: var(--app-text-muted);
          font-size: 12px;
          flex-shrink: 0;
        }
      }
    }
  }
}

html.dark {
  .navbar.navtop .navtop-logo-shell {
    background: linear-gradient(180deg, rgba(15, 23, 42, 0.92), rgba(30, 41, 59, 0.82));
    border-color: rgba(71, 85, 105, 0.42);
    box-shadow:
      inset 0 1px 0 rgba(255, 255, 255, 0.06),
      0 8px 18px rgba(0, 0, 0, 0.24);
  }

  .navbar.navtop .topbar-container {
    background: linear-gradient(180deg, rgba(15, 23, 42, 0.82), rgba(15, 23, 42, 0.7));
    border-color: rgba(71, 85, 105, 0.34);
    box-shadow:
      inset 0 1px 0 rgba(255, 255, 255, 0.05),
      0 8px 22px rgba(0, 0, 0, 0.2);
  }

  .navbar .right-menu .right-menu-item,
  .navbar .right-menu .avatar-wrapper {
    background: var(--app-navbar-bg);
    border-color: var(--app-navbar-border);
  }
}
</style>
