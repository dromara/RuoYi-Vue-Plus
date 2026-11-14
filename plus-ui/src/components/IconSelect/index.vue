<template>
  <div class="relative" :style="{ width: width }">
    <el-input v-model="modelValue" readonly placeholder="点击选择图标" @click="visible = !visible">
      <template #prepend>
        <svg-icon :icon-class="modelValue" />
      </template>
    </el-input>

    <el-popover shadow="none" :visible="visible" placement="bottom-end" trigger="click" :width="450">
      <template #reference>
        <div
          class="cursor-pointer text-[#999] absolute right-[10px] top-0 height-[32px] leading-[32px]"
          @click="visible = !visible"
        >
          <CaretTop v-show="visible" />
          <CaretBottom v-show="!visible" />
        </div>
      </template>

      <el-input v-model="filterValue" class="p-2" placeholder="搜索图标" clearable @input="filterIcons" />

      <div class="iconify-panel">
        <div class="iconify-heading">也可以直接输入 Iconify 图标名</div>
        <div class="iconify-form">
          <el-input
            v-model="customIcon"
            placeholder="例如：mdi:account-circle-outline"
            clearable
            @keyup.enter="applyCustomIcon"
          />
          <el-button type="primary" plain @click="applyCustomIcon">使用</el-button>
        </div>
      </div>

      <el-scrollbar height="w-[200px]">
        <ul class="icon-list">
          <el-tooltip
            v-for="(iconName, index) in iconNames"
            :key="index"
            :content="iconName"
            placement="bottom"
            effect="light"
            :teleported="false"
            :enterable="false"
          >
            <li :class="['icon-item', { active: modelValue == iconName }]" @click="selectedIcon(iconName)">
              <svg-icon color="var(--el-text-color-regular)" :icon-class="iconName" />
            </li>
          </el-tooltip>
        </ul>
      </el-scrollbar>
    </el-popover>
  </div>
</template>

<script setup lang="ts">
import icons from '@/components/IconSelect/requireIcons';
import { propTypes } from '@/utils/propTypes';

const props = defineProps({
  modelValue: propTypes.string.isRequired,
  width: propTypes.string.def('400px')
});

const emit = defineEmits(['update:modelValue']);
const visible = ref(false);
const { modelValue, width } = toRefs(props);
const iconNames = ref<string[]>(icons);

const filterValue = ref('');
const customIcon = ref('');

/**
 * 筛选图标
 */
const filterIcons = () => {
  if (filterValue.value) {
    iconNames.value = icons.filter(iconName => iconName.includes(filterValue.value));
  } else {
    iconNames.value = icons;
  }
};
/**
 * 选择图标
 * @param iconName 选择的图标名称
 */
const selectedIcon = (iconName: string) => {
  emit('update:modelValue', iconName);
  visible.value = false;
};

const applyCustomIcon = () => {
  const value = customIcon.value.trim();
  if (!value) return;
  emit('update:modelValue', value);
  visible.value = false;
};

watch(
  () => props.modelValue,
  value => {
    customIcon.value = value?.includes(':') ? value : '';
  },
  { immediate: true }
);
</script>

<style lang="scss" scoped>
.el-scrollbar {
  max-height: calc(50vh - 100px) !important;
  overflow-y: auto;
}
.el-divider--horizontal {
  margin: 10px auto !important;
}

.iconify-panel {
  padding: 2px 4px 12px;
}

.iconify-heading {
  margin-bottom: 8px;
  color: var(--app-text-muted);
  font-size: 12px;
  font-weight: 600;
}

.iconify-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.icon-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(48px, 1fr));
  gap: 8px;
  padding: 4px;
  margin-top: 10px;

  .icon-item {
    cursor: pointer;
    min-height: 44px;
    padding: 8px 6px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    border: 1px solid var(--app-surface-border);
    border-radius: 12px;
    background: var(--app-surface-bg);
    transition:
      border-color 0.2s ease,
      background-color 0.2s ease,
      color 0.2s ease,
      transform 0.2s ease;

    &:hover {
      border-color: var(--el-color-primary);
      background: var(--app-accent-soft);
      color: var(--el-color-primary);
      transform: translateY(-1px);
    }
  }

  .active {
    border-color: var(--el-color-primary);
    background: rgba(64, 158, 255, 0.12);
    color: var(--el-color-primary);
  }
}
</style>
