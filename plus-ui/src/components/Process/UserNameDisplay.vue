<template>
  <el-tooltip :disabled="!displayText" :content="displayText || emptyText" effect="dark" placement="top-start">
    <div class="user-name-display" :style="{ '--user-name-max-lines': maxLines }">
      {{ displayText || emptyText }}
    </div>
  </el-tooltip>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    content?: string | null;
    emptyText?: string;
    maxLines?: number;
  }>(),
  {
    content: '',
    emptyText: '无',
    maxLines: 3
  }
);

const displayText = computed(() =>
  props.content
    ?.split(',')
    .map(item => item.trim())
    .filter(Boolean)
    .join('、') ?? ''
);
</script>

<style lang="scss" scoped>
.user-name-display {
  display: -webkit-box;
  overflow: hidden;
  width: 100%;
  line-height: 20px;
  max-height: calc(20px * var(--user-name-max-lines));
  white-space: normal;
  word-break: break-all;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: var(--user-name-max-lines);
}
</style>
