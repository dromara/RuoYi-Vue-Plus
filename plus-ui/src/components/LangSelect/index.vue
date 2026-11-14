<template>
  <el-dropdown trigger="click" @command="handleLanguageChange">
    <div class="lang-select--style">
      <svg-icon icon-class="language" />
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item :disabled="appStore.language === 'zh_CN'" command="zh_CN">中文</el-dropdown-item>
        <el-dropdown-item :disabled="appStore.language === 'en_US'" command="en_US">English</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import SvgIcon from '@/components/SvgIcon/index.vue';
import { useAppStore } from '@/store/modules/app';

const appStore = useAppStore();
const { locale } = useI18n();

const message: any = {
  zh_CN: '切换语言成功！',
  en_US: 'Switch Language Successful!'
};
const handleLanguageChange = (lang: any) => {
  locale.value = lang;
  appStore.changeLanguage(lang);
  ElMessage.success(message[lang] || '切换语言成功！');
};
</script>

<style lang="scss" scoped>
.lang-select--style {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;

  :deep(.svg-icon) {
    width: 16px;
    height: 16px;
  }
}
</style>
