<template>
  <div
    ref="imageWrapperRef"
    class="image-wrapper"
    @wheel="handleMouseWheel"
    @mousedown="handleMouseDown"
    @mousemove="handleMouseMove"
    @mouseup="handleMouseUp"
    @mouseleave="handleMouseLeave"
    @dblclick="resetTransform"
    :style="transformStyle"
  >
    <el-card class="box-card">
      <el-image :src="props.imgUrl" class="scalable-image" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
// Props 定义方式变化
const props = defineProps({
  imgUrl: {
    type: String,
    default: () => ''
  }
});

const imageWrapperRef = ref<HTMLElement | null>(null);
const scale = ref(1); // 初始缩放比例
const maxScale = 3; // 最大缩放比例
const minScale = 0.5; // 最小缩放比例

let isDragging = false;
let startX = 0;
let startY = 0;
let currentTranslateX = 0;
let currentTranslateY = 0;

const handleMouseWheel = (event: WheelEvent) => {
  event.preventDefault();
  let newScale = scale.value - event.deltaY / 1000;
  newScale = Math.max(minScale, Math.min(newScale, maxScale));
  if (newScale !== scale.value) {
    scale.value = newScale;
    resetDragPosition(); // 重置拖拽位置，使图片居中
  }
};

const handleMouseDown = (event: MouseEvent) => {
  if (scale.value > 1) {
    event.preventDefault(); // 阻止默认行为，防止拖拽
    isDragging = true;
    startX = event.clientX;
    startY = event.clientY;
  }
};

const handleMouseMove = (event: MouseEvent) => {
  if (!isDragging || !imageWrapperRef.value) return;

  const deltaX = event.clientX - startX;
  const deltaY = event.clientY - startY;
  startX = event.clientX;
  startY = event.clientY;

  currentTranslateX += deltaX;
  currentTranslateY += deltaY;

  // 边界检测，防止图片被拖出容器
  const bounds = getBounds();
  if (currentTranslateX > bounds.maxTranslateX) {
    currentTranslateX = bounds.maxTranslateX;
  } else if (currentTranslateX < bounds.minTranslateX) {
    currentTranslateX = bounds.minTranslateX;
  }

  if (currentTranslateY > bounds.maxTranslateY) {
    currentTranslateY = bounds.maxTranslateY;
  } else if (currentTranslateY < bounds.minTranslateY) {
    currentTranslateY = bounds.minTranslateY;
  }

  applyTransform();
};

const handleMouseUp = () => {
  isDragging = false;
};

const handleMouseLeave = () => {
  isDragging = false;
};

const resetTransform = () => {
  scale.value = 1;
  currentTranslateX = 0;
  currentTranslateY = 0;
  applyTransform();
};

const resetDragPosition = () => {
  currentTranslateX = 0;
  currentTranslateY = 0;
  applyTransform();
};

const applyTransform = () => {
  if (imageWrapperRef.value) {
    imageWrapperRef.value.style.transform = `translate(${currentTranslateX}px, ${currentTranslateY}px) scale(${scale.value})`;
  }
};

const getBounds = () => {
  if (!imageWrapperRef.value)
    return {
      minTranslateX: 0,
      maxTranslateX: 0,
      minTranslateY: 0,
      maxTranslateY: 0
    };

  const imgRect = imageWrapperRef.value.getBoundingClientRect();
  const containerRect = imageWrapperRef.value.parentElement?.getBoundingClientRect() ?? imgRect;

  const minTranslateX = (containerRect.width - imgRect.width * scale.value) / 2;
  const maxTranslateX = -(containerRect.width - imgRect.width * scale.value) / 2;
  const minTranslateY = (containerRect.height - imgRect.height * scale.value) / 2;
  const maxTranslateY = -(containerRect.height - imgRect.height * scale.value) / 2;

  return { minTranslateX, maxTranslateX, minTranslateY, maxTranslateY };
};

const transformStyle = computed(() => ({
  transition: isDragging ? 'none' : 'transform 0.2s ease'
}));
</script>

<style scoped>
.image-wrapper {
  width: 100%;
  overflow: hidden;
  position: relative;
  margin: 0 auto;
  display: flex;
  justify-content: center;
  align-items: center;
  user-select: none; /* 禁用文本选择 */
  cursor: grab; /* 设置初始鼠标指针为可拖动 */
}

.image-wrapper:active {
  cursor: grabbing; /* 当正在拖动时改变鼠标指针 */
}

.scalable-image {
  object-fit: contain;
  width: 100%;
  padding: 15px;
}
</style>
