<template>
  <el-drawer
    v-model="showSettings"
    class="settings-drawer"
    :with-header="false"
    direction="rtl"
    size="300px"
    close-on-click-modal
  >
    <h3 class="drawer-title">菜单导航设置</h3>
    <div class="nav-wrap">
      <el-tooltip content="左侧菜单" placement="bottom">
        <div
          class="item left"
          @click="handleNavType(NavTypeEnum.LEFT)"
          :style="{ '--theme': theme }"
          :class="{ activeItem: navType == NavTypeEnum.LEFT }"
        >
          <b></b>
          <b></b>
        </div>
      </el-tooltip>

      <el-tooltip content="混合菜单" placement="bottom">
        <div
          class="item mix"
          @click="handleNavType(NavTypeEnum.MIX)"
          :style="{ '--theme': theme }"
          :class="{ activeItem: navType == NavTypeEnum.MIX }"
        >
          <b></b>
          <b></b>
        </div>
      </el-tooltip>
      <el-tooltip content="顶部菜单" placement="bottom">
        <div
          class="item top"
          @click="handleNavType(NavTypeEnum.TOP)"
          :style="{ '--theme': theme }"
          :class="{ activeItem: navType == NavTypeEnum.TOP }"
        >
          <b></b>
          <b></b>
        </div>
      </el-tooltip>
    </div>

    <h3 class="drawer-title">主题风格设置</h3>

    <div class="setting-drawer-block-checbox">
      <div class="setting-drawer-block-checbox-item" @click="handleTheme(SideThemeEnum.DARK)">
        <img src="@/assets/images/dark.svg" alt="dark" />
        <div v-if="sideTheme === 'theme-dark'" class="setting-drawer-block-checbox-selectIcon" style="display: block">
          <i aria-label="图标: check" class="anticon anticon-check">
            <svg
              viewBox="64 64 896 896"
              data-icon="check"
              width="1em"
              height="1em"
              :fill="theme"
              aria-hidden="true"
              focusable="false"
            >
              <path
                d="M912 190h-69.9c-9.8 0-19.1 4.5-25.1 12.2L404.7 724.5 207 474a32 32 0 0 0-25.1-12.2H112c-6.7 0-10.4 7.7-6.3 12.9l273.9 347c12.8 16.2 37.4 16.2 50.3 0l488.4-618.9c4.1-5.1.4-12.8-6.3-12.8z"
              />
            </svg>
          </i>
        </div>
      </div>
      <div class="setting-drawer-block-checbox-item" @click="handleTheme(SideThemeEnum.LIGHT)">
        <img src="@/assets/images/light.svg" alt="light" />
        <div v-if="sideTheme === 'theme-light'" class="setting-drawer-block-checbox-selectIcon" style="display: block">
          <i aria-label="图标: check" class="anticon anticon-check">
            <svg
              viewBox="64 64 896 896"
              data-icon="check"
              width="1em"
              height="1em"
              :fill="theme"
              aria-hidden="true"
              focusable="false"
            >
              <path
                d="M912 190h-69.9c-9.8 0-19.1 4.5-25.1 12.2L404.7 724.5 207 474a32 32 0 0 0-25.1-12.2H112c-6.7 0-10.4 7.7-6.3 12.9l273.9 347c12.8 16.2 37.4 16.2 50.3 0l488.4-618.9c4.1-5.1.4-12.8-6.3-12.8z"
              />
            </svg>
          </i>
        </div>
      </div>
    </div>
    <div class="drawer-item">
      <span>主题颜色</span>
      <span class="comp-style">
        <el-color-picker v-model="theme" :predefine="predefineColors" @change="themeChange" />
      </span>
    </div>
    <div class="drawer-item">
      <span>深色模式</span>
      <span class="comp-style">
        <el-switch v-model="isDark" class="drawer-switch" @change="toggleDark" />
      </span>
    </div>
    <div class="drawer-item">
      <span>页面圆角</span>
      <span class="comp-style">
        <el-slider v-model="radiusBase" :min="0" :max="32" :step="2" style="width: 120px" @change="radiusBaseChange" />
      </span>
    </div>

    <el-divider />

    <h3 class="drawer-title">系统布局配置</h3>

    <div class="drawer-item">
      <span>开启 Tags-Views</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.tagsView" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>持久化标签页</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.tagsViewPersist" :disabled="!settingsStore.tagsView" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>显示页签图标</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.tagsIcon" :disabled="!settingsStore.tagsView" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>固定 Header</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.fixedHeader" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>显示 Logo</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.sidebarLogo" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>动态标题</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.dynamicTitle" class="drawer-switch" @change="dynamicTitleChange" />
      </span>
    </div>

    <div class="drawer-item">
      <span>全高表格</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.fullHeightTable" class="drawer-switch" />
      </span>
    </div>

    <el-divider />

    <el-button type="primary" plain icon="DocumentAdd" @click="saveSetting">保存配置</el-button>
    <el-button plain icon="Refresh" @click="resetSetting">重置配置</el-button>
  </el-drawer>
</template>

<script setup lang="ts">
import { NavTypeEnum } from '@/enums/NavTypeEnum';
import { SideThemeEnum } from '@/enums/SideThemeEnum';
import modal from '@/plugins/modal';
import defaultSettings from '@/settings';
import { useAppStore } from '@/store/modules/app';
import { usePermissionStore } from '@/store/modules/permission';
import { useSettingsStore } from '@/store/modules/settings';
import { useDynamicTitle } from '@/utils/dynamicTitle';
import { handleThemeStyle } from '@/utils/theme';

const appStore = useAppStore();
const settingsStore = useSettingsStore();
const permissionStore = usePermissionStore();

const showSettings = ref(false);
const theme = ref(settingsStore.theme);
const sideTheme = ref(settingsStore.sideTheme);
const storeSettings = computed(() => settingsStore);
const predefineColors = ref(['#409EFF', '#ff4500', '#ff8c00', '#ffd700', '#90ee90', '#00ced1', '#1e90ff', '#c71585']);
const navType = ref(settingsStore.navType);
const radiusBase = ref(settingsStore.radiusBase);
// 是否暗黑模式
const isDark = useDark({
  storageKey: 'useDarkKey',
  valueDark: 'dark',
  valueLight: 'light'
});
// 匹配菜单颜色
watch(isDark, () => {
  if (isDark.value) {
    settingsStore.sideTheme = SideThemeEnum.DARK;
  } else {
    settingsStore.sideTheme = sideTheme.value;
  }
});
const toggleDark = () => useToggle(isDark);

/** 菜单导航设置 */
watch(
  navType,
  val => {
    if (val === NavTypeEnum.TOP) {
      appStore.toggleSideBarHide(true);
      permissionStore.setSidebarRouters(permissionStore.defaultRoutes as any);
    } else if (val === NavTypeEnum.LEFT) {
      appStore.toggleSideBarHide(false);
      permissionStore.setSidebarRouters(permissionStore.defaultRoutes as any);
    } else if (val === NavTypeEnum.MIX) {
      appStore.toggleSideBarHide(false);
    }
  },
  { immediate: true }
);

const handleNavType = (val: NavTypeEnum) => {
  settingsStore.navType = val;
  navType.value = val;
};

const dynamicTitleChange = () => {
  // 动态设置网页标题
  useDynamicTitle();
};

const themeChange = (val: string) => {
  settingsStore.theme = val;
  handleThemeStyle(val);
};
const radiusBaseChange = (val: number) => {
  settingsStore.radiusBase = val;
  // 更新 CSS 变量
  document.documentElement.style.setProperty('--app-radius-base', `${val}px`);
};
const handleTheme = (val: string) => {
  sideTheme.value = val;
  if (isDark.value && val === SideThemeEnum.LIGHT) {
    // 暗黑模式颜色不变
    settingsStore.sideTheme = SideThemeEnum.DARK;
    return;
  }
  settingsStore.sideTheme = val;
};
const saveSetting = () => {
  modal.loading('正在保存到本地，请稍候...');
  const settings = useStorage<LayoutSetting>('layout-setting', defaultSettings);
  if (!storeSettings.value.tagsViewPersist) {
    localStorage.removeItem('tags-view-visited');
  }
  settings.value.tagsView = storeSettings.value.tagsView;
  settings.value.tagsViewPersist = storeSettings.value.tagsViewPersist;
  settings.value.tagsIcon = storeSettings.value.tagsIcon;
  settings.value.fixedHeader = storeSettings.value.fixedHeader;
  settings.value.sidebarLogo = storeSettings.value.sidebarLogo;
  settings.value.dynamicTitle = storeSettings.value.dynamicTitle;
  settings.value.sideTheme = storeSettings.value.sideTheme;
  settings.value.theme = storeSettings.value.theme;
  settings.value.navType = storeSettings.value.navType;
  settings.value.radiusBase = storeSettings.value.radiusBase;
  settings.value.fullHeightTable = storeSettings.value.fullHeightTable;
  setTimeout(() => {
    modal.closeLoading();
  }, 1000);
};
const resetSetting = () => {
  modal.loading('正在清除设置缓存并刷新，请稍候...');
  localStorage.removeItem('tags-view-visited');
  useStorage<any>('layout-setting', null).value = null;
  setTimeout('window.location.reload()', 1000);
};
const openSetting = () => {
  showSettings.value = true;
};

onMounted(() => {
  radiusBaseChange(storeSettings.value.radiusBase);
});

defineExpose({
  openSetting
});
</script>

<style lang="scss" scoped>
.settings-drawer {
  :deep(.el-drawer__body) {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 18px 18px 20px;
  }
}

.setting-drawer-title {
  margin-bottom: 12px;
  color: var(--app-text-title);
  line-height: 22px;
  font-weight: bold;
  .drawer-title {
    font-size: 14px;
  }
}
.setting-drawer-block-checbox {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-top: 10px;
  margin-bottom: 20px;

  .setting-drawer-block-checbox-item {
    position: relative;
    margin-right: 16px;
    border-radius: 12px;
    cursor: pointer;
    overflow: hidden;
    border: 1px solid var(--app-surface-border);
    transition:
      border-color 0.2s ease,
      transform 0.2s ease;

    &:hover {
      border-color: rgba(64, 158, 255, 0.28);
      transform: translateY(-1px);
    }

    img {
      width: 48px;
      height: 48px;
      display: block;
    }

    .custom-img {
      width: 48px;
      height: 38px;
      border-radius: 5px;
      box-shadow: 1px 1px 2px #898484;
    }

    .setting-drawer-block-checbox-selectIcon {
      position: absolute;
      top: 0;
      right: 0;
      width: 100%;
      height: 100%;
      padding-top: 15px;
      padding-left: 24px;
      color: var(--app-accent-strong);
    }
  }
}

.drawer-item {
  padding: 12px 0;
  font-size: 14px;
  color: var(--app-text-title);
  border-bottom: 1px solid var(--app-surface-border);

  .comp-style {
    float: right;
    margin: -3px 8px 0px 0px;
  }
}

// 导航模式
.nav-wrap {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-top: 10px;
  margin-bottom: 20px;

  .activeItem {
    border: 2px solid #{'var(--theme)'} !important;
  }

  .item {
    position: relative;
    margin-right: 16px;
    cursor: pointer;
    width: 56px;
    height: 48px;
    border-radius: 12px;
    background: var(--app-elevated-soft-bg);
    border: 2px solid transparent;
    transition:
      transform 0.2s ease,
      border-color 0.2s ease;

    &:hover {
      transform: translateY(-1px);
    }
  }

  .left {
    b:first-child {
      display: block;
      height: 30%;
      background: #fff;
    }

    b:last-child {
      width: 30%;
      background: #1b2a47;
      position: absolute;
      height: 100%;
      top: 0;
      border-radius: 4px 0 0 4px;
    }
  }

  .mix {
    b:first-child {
      border-radius: 4px 4px 0 0;
      display: block;
      height: 30%;
      background: #1b2a47;
    }

    b:last-child {
      width: 30%;
      background: #1b2a47;
      position: absolute;
      height: 70%;
      border-radius: 0 0 0 4px;
    }
  }

  .top {
    b:first-child {
      display: block;
      height: 30%;
      background: #1b2a47;
      border-radius: 4px 4px 0 0;
    }

    b:last-child {
      display: none;
    }
  }
}
</style>
