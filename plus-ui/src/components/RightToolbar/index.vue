<template>
  <div class="top-right-btn" :style="style">
    <el-row class="toolbar-row">
      <el-tooltip
        v-if="search"
        class="item"
        effect="dark"
        :content="showSearch ? '隐藏搜索' : '显示搜索'"
        placement="top"
      >
        <el-button circle icon="Search" @click="toggleSearch()" />
      </el-tooltip>
      <el-tooltip class="item" effect="dark" content="刷新" placement="top">
        <el-button circle icon="Refresh" @click="refresh()" />
      </el-tooltip>
      <el-tooltip v-if="columns" class="item" effect="dark" content="显示/隐藏列" placement="top">
        <div class="show-btn">
          <el-popover placement="bottom" trigger="click">
            <div class="tree-header">显示/隐藏列</div>
            <el-tree
              ref="columnRef"
              :data="columns"
              show-checkbox
              node-key="key"
              :props="{ label: 'label', children: 'children' } as any"
              @check="columnChange"
            ></el-tree>
            <template #reference>
              <el-button circle icon="Menu" />
            </template>
          </el-popover>
        </div>
      </el-tooltip>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import cache from '@/plugins/cache';
import { propTypes } from '@/utils/propTypes';

const props = defineProps({
  showSearch: propTypes.bool.def(true),
  columns: propTypes.fieldOption,
  search: propTypes.bool.def(true),
  gutter: propTypes.number.def(10),
  /* 列显隐状态记忆的 localStorage key（传入则启用记忆，不传则不记忆） */
  storageKey: propTypes.string.def('')
});

const columnRef = ref<ElTreeInstance>();
const emits = defineEmits(['update:showSearch', 'queryTable']);

const style = computed(() => {
  const ret: any = {};
  if (props.gutter) {
    ret.marginRight = `${props.gutter / 2}px`;
  }
  return ret;
});

// 搜索
function toggleSearch() {
  emits('update:showSearch', !props.showSearch);
}

// 刷新
function refresh() {
  emits('queryTable');
}

// 将当前列显隐状态持久化到 localStorage
function saveStorage() {
  if (!props.storageKey) return;
  try {
    const state: Record<string, boolean> = {};
    props.columns?.forEach((col, index) => {
      state[index] = col.visible;
    });
    cache.local.setJSON(props.storageKey, state);
  } catch (e) {}
}

// 更改数据列的显示和隐藏
function columnChange(...args: any[]) {
  props.columns?.forEach(item => {
    item.visible = args[1].checkedKeys.includes(item.key);
  });
  saveStorage();
}

// 显隐列初始默认隐藏列
onMounted(() => {
  // 如果传入了 storageKey，从 localStorage 恢复列显隐状态
  if (props.storageKey) {
    try {
      const saved = cache.local.getJSON(props.storageKey);
      if (saved && typeof saved === 'object') {
        props.columns?.forEach((col, index) => {
          if (saved[index] !== undefined) col.visible = saved[index];
        });
      }
    } catch (e) {}
  }
  props.columns?.forEach(item => {
    if (item.visible) {
      columnRef.value?.setChecked(item.key, true, false);
    }
  });
});
</script>

<style lang="scss" scoped>
:deep(.el-button.is-circle) {
  width: 38px;
  height: 38px;
  border-radius: 14px;
  background: var(--app-elevated-soft-bg);
  border: 1px solid var(--app-surface-border);
  color: var(--app-text-muted);
  transition:
    transform 0.25s ease,
    border-color 0.25s ease,
    color 0.25s ease,
    background 0.25s ease;

  &:hover {
    transform: translateY(-1px);
    color: var(--app-accent-strong);
    background: var(--app-accent-soft);
    border-color: rgba(53, 109, 255, 0.2);
  }
}

:deep(.el-transfer__button) {
  border-radius: 50%;
  display: block;
  margin-left: 0px;
}
:deep(.el-transfer__button:first-child) {
  margin-bottom: 10px;
}

.my-el-transfer {
  text-align: center;
}

.toolbar-row {
  gap: 8px;
}

.tree-header {
  width: 100%;
  line-height: 24px;
  text-align: center;
  font-weight: 700;
  color: var(--app-text-title);
}

.show-btn {
  margin-left: 0;
}
</style>
