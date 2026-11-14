<template>
  <div class="sidebar-logo-container" :class="{ collapse: collapse }">
    <transition :enter-active-class="animateConfig.logoAnimate.enter" mode="out-in">
      <router-link v-if="collapse" key="collapse" class="sidebar-logo-link" to="/">
        <img v-if="logo" :src="logo" class="sidebar-logo" />
        <h1 v-else class="sidebar-title">
          {{ title }}
        </h1>
      </router-link>
      <router-link v-else key="expand" class="sidebar-logo-link" to="/">
        <img v-if="logo" :src="logo" class="sidebar-logo" />
        <h1 class="sidebar-title">
          {{ title }}
        </h1>
      </router-link>
    </transition>
  </div>
</template>

<script setup lang="ts">
import animateConfig from '@/animate';
import logo from '@/assets/logo/logo.png';
import { NavTypeEnum } from '@/enums/NavTypeEnum';
import { useSettingsStore } from '@/store/modules/settings';

defineProps({
  collapse: {
    type: Boolean,
    required: true
  }
});

const title = import.meta.env.VITE_APP_LOGO_TITLE;
const settingsStore = useSettingsStore();
const sideTheme = computed(() => settingsStore.sideTheme);
const isTopNav = computed(() => settingsStore.navType === NavTypeEnum.TOP);
const isDarkSide = computed(() => !isTopNav.value && sideTheme.value === 'theme-dark');
const logoSurface = computed(() => (isDarkSide.value ? 'rgba(255, 255, 255, 0.04)' : '#f8fafc'));
const logoBorder = computed(() => (isDarkSide.value ? 'rgba(148, 163, 184, 0.12)' : '#e5e7eb'));
const logoTextColor = computed(() => (isDarkSide.value ? '#f8fbff' : 'var(--app-text-title)'));
</script>

<style lang="scss" scoped>
.sidebarLogoFade-enter-active {
  transition: opacity 1.5s;
}

.sidebarLogoFade-enter,
.sidebarLogoFade-leave-to {
  opacity: 0;
}

.sidebar-logo-container {
  position: relative;
  flex-shrink: 0;
  height: 46px;
  line-height: 46px;
  padding: 0 8px;
  margin-top: 8px;
  background: transparent;
  text-align: center;
  overflow: hidden;
  margin-bottom: 0;

  & .sidebar-logo-link {
    height: 100%;
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    border-radius: 14px;
    background: v-bind(logoSurface);
    border: 1px solid v-bind(logoBorder);

    & .sidebar-logo {
      width: 28px;
      height: 28px;
      vertical-align: middle;
      margin-right: 0;
      margin-left: 0;
      border-radius: 10px;
      box-shadow: none;
    }

    & .sidebar-title {
      display: inline-block;
      margin: 0;
      color: v-bind(logoTextColor);
      font-weight: 600;
      line-height: 1;
      font-size: 15px;
      letter-spacing: 0.02em;
      font-family: 'MiSans', 'HarmonyOS Sans SC', 'PingFang SC', sans-serif;
      vertical-align: middle;
    }
  }

  &.collapse {
    .sidebar-logo-link {
      padding: 0;
    }

    .sidebar-logo {
      margin-right: 0;
    }
  }
}
</style>
