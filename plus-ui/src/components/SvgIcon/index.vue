<template>
  <Icon v-if="isIconify" :icon="normalizedIcon" :class="svgClass" :style="iconStyle" aria-hidden="true" />
  <svg v-else :class="svgClass" aria-hidden="true" :style="iconStyle">
    <use :xlink:href="iconName" :fill="color" />
  </svg>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue';
import { propTypes } from '@/utils/propTypes';

const props = defineProps({
  iconClass: propTypes.string.isRequired,
  className: propTypes.string.def(''),
  color: propTypes.string.def(''),
  size: propTypes.string.def('')
});
const normalizedIcon = computed(() => props.iconClass?.replace(/^i-/, ''));
const isIconify = computed(() => normalizedIcon.value?.includes(':'));
const iconName = computed(() => `#icon-${normalizedIcon.value}`);
const svgClass = computed(() => {
  if (props.className) {
    return `svg-icon ${props.className}`;
  }
  return 'svg-icon';
});
const iconStyle = computed(() => ({
  color: props.color || undefined,
  fontSize: props.size || undefined
}));
</script>

<style lang="scss" scoped>
.sub-el-icon,
.nav-icon {
  display: inline-block;
  font-size: 15px;
  margin-right: 12px;
  position: relative;
}

.svg-icon {
  width: 1em;
  height: 1em;
  position: relative;
  fill: currentColor;
  vertical-align: -2px;
  flex-shrink: 0;
}
</style>
