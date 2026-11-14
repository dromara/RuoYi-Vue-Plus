<template>
  <el-col
    :lg="modelCollapsed ? 1 : expandedSpan"
    :xs="24"
    class="tree-panel-col"
    :class="{ 'is-collapsed': modelCollapsed }"
  >
    <el-card shadow="hover" class="side-panel tree-panel-shell" :class="{ 'is-collapsed': modelCollapsed }">
      <template #header>
        <div
          class="panel-heading search-panel-toggle tree-panel-header"
          :class="{ 'is-collapsed': modelCollapsed }"
          @click.stop="toggleCollapsed"
        >
          <div v-show="!modelCollapsed">
            <h3>{{ title }}</h3>
          </div>
        </div>
      </template>
      <template v-if="!modelCollapsed">
        <el-input v-model="filterText" :placeholder="placeholder" prefix-icon="Search" clearable />
        <div ref="treeWrapRef" class="dept-tree-wrap">
          <el-tree-v2
            ref="treeRef"
            class="dept-tree"
            :data="treeData"
            :props="mergedTreeProps as any"
            :height="treeHeight"
            :item-size="28"
            :expand-on-click-node="false"
            :default-expanded-keys="defaultExpandedKeys"
            :filter-method="internalFilterNode"
            highlight-current
            @node-click="onNodeClick"
          >
            <template #default="{ node }">
              <span class="tree-node-label" :class="{ 'is-disabled': isNodeDisabled(node.data) }">
                <span>{{ node.label }}</span>
                <el-tooltip v-if="isNodeDisabled(node.data)" content="停用" placement="top">
                  <el-icon class="tree-node-disabled-icon"><CircleCloseFilled /></el-icon>
                </el-tooltip>
              </span>
            </template>
          </el-tree-v2>
        </div>
      </template>
    </el-card>
  </el-col>
</template>

<script setup lang="ts">
import type { TreeKey, TreeV2Instance } from 'element-plus';

const props = withDefaults(
  defineProps<{
    /** 面板标题 */
    title: string;
    /** 搜索输入框占位文字 */
    placeholder?: string;
    /** 树数据 */
    data: any[];
    /** el-tree node-key */
    nodeKey?: string;
    /** el-tree props 配置 */
    treeProps?: Record<string, string>;
    /** 节点禁用字段名，未提供或字段不存在则忽略 */
    disabledField?: string;
    /** 展开时占的栅格列数 (lg) */
    expandedSpan?: number;
    /** 默认过滤字段名 (用于内置 filterNode) */
    filterField?: string;
    /** 自定义过滤方法，传入则覆盖默认 filterField 逻辑 */
    filterNodeMethod?: (value: string, data: any) => boolean;
    /** 折叠状态 (v-model:collapsed) */
    collapsed?: boolean;
  }>(),
  {
    placeholder: '请输入名称',
    nodeKey: 'id',
    treeProps: () => ({ label: 'label', children: 'children' }),
    disabledField: 'disabled',
    expandedSpan: 5,
    filterField: 'label',
    collapsed: false
  }
);

const emit = defineEmits<{
  'update:collapsed': [value: boolean];
  'node-click': [data: any, node: any, component: any];
}>();

const modelCollapsed = computed({
  get: () => props.collapsed,
  set: (val: boolean) => emit('update:collapsed', val)
});

const filterText = ref('');
const treeRef = ref<TreeV2Instance>();
const treeWrapRef = ref<HTMLElement>();
const treeData = shallowRef<any[]>([]);
const defaultExpandedKeys = shallowRef<TreeKey[]>([]);
const { height: treeWrapHeight } = useElementSize(treeWrapRef);
const treeHeight = computed(() => Math.max(Math.floor(treeWrapHeight.value), 180));

const mergedTreeProps = computed(() => {
  const label = props.treeProps?.label ?? 'label';
  const children = props.treeProps?.children ?? 'children';
  const value = props.treeProps?.value ?? props.nodeKey;
  if (!props.disabledField) {
    return {
      ...props.treeProps,
      value,
      label,
      children
    };
  }
  return {
    ...props.treeProps,
    value,
    label,
    children,
    disabled: props.treeProps?.disabled ?? props.disabledField
  };
});

const isNodeDisabled = (data: any) => {
  if (!props.disabledField) {
    return false;
  }
  return Boolean(data?.[props.disabledField]);
};

const toggleCollapsed = () => {
  modelCollapsed.value = !modelCollapsed.value;
};

const getNodeKey = (data: any) => data?.[props.nodeKey] as TreeKey | undefined;

const collectExpandedKeys = (nodes: any[], keys: TreeKey[] = []) => {
  const childrenField = props.treeProps?.children ?? 'children';
  for (const node of nodes) {
    const children = node?.[childrenField];
    if (Array.isArray(children) && children.length > 0) {
      const key = getNodeKey(node);
      if (key !== undefined) {
        keys.push(key);
      }
      collectExpandedKeys(children, keys);
    }
  }
  return keys;
};

const internalFilterNode = (value: string, data: any) => {
  if (props.filterNodeMethod) {
    return props.filterNodeMethod(value, data);
  }
  if (!value) return true;
  return data[props.filterField]?.indexOf(value) !== -1;
};

const filterTree = useDebounceFn(() => {
  treeRef.value?.filter(filterText.value);
}, 200);

watch(filterText, () => filterTree());

watch(
  () => props.data,
  (data) => {
    const rawData = toRaw(data);
    treeData.value = rawData;
    defaultExpandedKeys.value = collectExpandedKeys(rawData);
    nextTick(() => {
      treeRef.value?.setData(rawData);
      treeRef.value?.setExpandedKeys(defaultExpandedKeys.value);
      if (filterText.value) {
        filterTree();
      }
    });
  },
  { immediate: true }
);

const onNodeClick = (data: any, node: any, component: any) => {
  if (isNodeDisabled(data)) {
    treeRef.value?.setCurrentKey(undefined as any);
    return;
  }
  emit('node-click', data, node, component);
};

const contentSpan = computed(() => (modelCollapsed.value ? 23 : 24 - props.expandedSpan));

const setCurrentKey = (key: any) => treeRef.value?.setCurrentKey(key);

defineExpose({
  treeRef,
  contentSpan,
  setCurrentKey
});
</script>

<style lang="scss" scoped>
$mobile-breakpoint: 900px;

.tree-panel-col {
  min-width: 0;
  transition:
    max-width 0.24s ease,
    flex-basis 0.24s ease;
}

.tree-panel-col.is-collapsed {
  max-width: 56px;
  flex: 0 0 56px;
}

.tree-panel-shell,
.side-panel {
  height: 100%;
}

.tree-panel-shell {
  --tree-panel-max-height: 620px;
}

.tree-panel-shell :deep(.el-card__header) {
  display: block;
  padding: 12px 16px !important;
}

.tree-panel-shell :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: var(--tree-panel-max-height);
  min-height: 0;
  max-height: var(--tree-panel-max-height);
  overflow: hidden;
}

.tree-panel-header {
  cursor: pointer;
  user-select: none;
}

.tree-panel-header::after {
  display: block;
  width: 9px;
  min-width: 9px;
  height: 9px;
  margin-left: auto;
  flex-shrink: 0;
  transform-origin: center;
  transform: rotate(135deg);
}

.tree-panel-header.is-collapsed {
  justify-content: center;
}

.side-panel.is-collapsed :deep(.el-card__body) {
  display: none;
}

.tree-panel-shell.is-collapsed :deep(.el-card__header) {
  padding: 12px 0 !important;
}

.tree-panel-shell.is-collapsed .tree-panel-header::after {
  margin-left: 0;
  transform: rotate(-45deg);
}

.dept-tree-wrap {
  margin-top: 8px;
  flex: 1 1 auto;
  min-height: 180px;
  max-height: 100%;
  min-width: 0;
}

.dept-tree {
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
}

.dept-tree::-webkit-scrollbar {
  width: 6px;
}

.dept-tree::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: var(--app-text-muted);
  opacity: 0.55;
}

.dept-tree::-webkit-scrollbar-track {
  background: transparent;
}

.tree-panel-shell :deep(.tree-node-label) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  line-height: 22px;
  color: var(--el-text-color-primary);
}

.tree-panel-shell :deep(.tree-node-label.is-disabled) {
  color: var(--el-color-danger);
}

.tree-panel-shell :deep(.tree-node-disabled-icon) {
  flex: 0 0 auto;
  margin-top: 1px;
  font-size: 14px;
  color: var(--el-color-danger);
}

@media (max-width: $mobile-breakpoint) {
  .tree-panel-col {
    max-width: 100%;
    flex: 0 0 100%;
  }

  .tree-panel-shell {
    --tree-panel-max-height: 420px;
  }

  .side-panel.is-collapsed {
    height: auto;
  }
}
</style>
