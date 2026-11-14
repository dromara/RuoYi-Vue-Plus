<template>
  <el-scrollbar ref="scrollContainerRef" :vertical="false" class="scroll-container" @wheel.prevent="handleScroll">
    <slot />
  </el-scrollbar>
</template>

<script setup lang="ts">
import type { RouteLocationNormalized } from 'vue-router';

const tagAndTagSpacing = 4;

const emits = defineEmits(['scroll', 'updateArrows']);
const scrollContainerRef = ref<ElScrollbarInstance>();

const getScrollWrapper = (): HTMLElement | null => {
  return scrollContainerRef.value?.wrapRef ?? null;
};

const emitScroll = () => {
  emits('scroll');
  emits('updateArrows');
};

onMounted(() => {
  getScrollWrapper()?.addEventListener('scroll', emitScroll, true);
});

onBeforeUnmount(() => {
  getScrollWrapper()?.removeEventListener('scroll', emitScroll, true);
});

const smoothScrollTo = (target: number) => {
  const scrollWrapper = getScrollWrapper();
  if (!scrollWrapper) {
    return;
  }
  scrollWrapper.scrollTo({ left: target, behavior: 'smooth' });
  setTimeout(() => {
    emits('updateArrows');
  }, 350);
};

const handleScroll = (e: WheelEvent) => {
  const eventDelta = (e as any).wheelDelta || -e.deltaY * 40;
  const scrollWrapper = getScrollWrapper();
  if (!scrollWrapper) {
    return;
  }

  scrollWrapper.scrollLeft += eventDelta / 4;
  emits('updateArrows');
};

const moveToTarget = (currentTag: RouteLocationNormalized) => {
  const container = scrollContainerRef.value?.$el as HTMLElement | undefined;
  const scrollWrapper = getScrollWrapper();
  if (!container || !scrollWrapper) {
    return;
  }

  const containerWidth = container.offsetWidth;
  const tagKey = currentTag.fullPath || currentTag.path;
  const tagListDom = Array.from(document.querySelectorAll('.tags-view-item')) as HTMLElement[];
  const currentIndex = tagListDom.findIndex(item => item.dataset.tagKey === tagKey);
  if (currentIndex === -1) {
    return;
  }

  const currentElement = tagListDom[currentIndex];
  const firstTag = tagListDom[0];
  const lastTag = tagListDom[tagListDom.length - 1];

  if (currentElement === firstTag) {
    smoothScrollTo(0);
    return;
  }

  if (currentElement === lastTag) {
    smoothScrollTo(scrollWrapper.scrollWidth - containerWidth);
    return;
  }

  const prevTag = tagListDom[currentIndex - 1];
  const nextTag = tagListDom[currentIndex + 1];
  if (!prevTag || !nextTag) {
    return;
  }

  const afterNextTagOffsetLeft = nextTag.offsetLeft + nextTag.offsetWidth + tagAndTagSpacing;
  const beforePrevTagOffsetLeft = prevTag.offsetLeft - tagAndTagSpacing;

  if (afterNextTagOffsetLeft > scrollWrapper.scrollLeft + containerWidth) {
    smoothScrollTo(afterNextTagOffsetLeft - containerWidth);
  } else if (beforePrevTagOffsetLeft < scrollWrapper.scrollLeft) {
    smoothScrollTo(beforePrevTagOffsetLeft);
  }
};

const scrollToStart = () => {
  smoothScrollTo(0);
};

const scrollToEnd = () => {
  const scrollWrapper = getScrollWrapper();
  if (!scrollWrapper) {
    return;
  }
  smoothScrollTo(scrollWrapper.scrollWidth - scrollWrapper.clientWidth);
};

const getScrollState = () => {
  const scrollWrapper = getScrollWrapper();
  if (!scrollWrapper) {
    return { canLeft: false, canRight: false };
  }

  return {
    canLeft: scrollWrapper.scrollLeft > 0,
    canRight: scrollWrapper.scrollLeft < scrollWrapper.scrollWidth - scrollWrapper.clientWidth - 1
  };
};

defineExpose({
  moveToTarget,
  scrollToStart,
  scrollToEnd,
  getScrollState
});
</script>

<style lang="scss" scoped>
.scroll-container {
  white-space: nowrap;
  position: relative;
  overflow: hidden;
  width: 100%;

  :deep(.el-scrollbar__bar) {
    bottom: 0;
  }

  :deep(.el-scrollbar__wrap) {
    height: 34px;
    display: flex;
    align-items: center;
  }
}
</style>
