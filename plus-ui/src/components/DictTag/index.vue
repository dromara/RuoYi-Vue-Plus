<template>
  <div>
    <template v-for="(item, index) in options">
      <template v-if="isValueMatch(item.value)">
        <span
          v-if="
            ((item.elTagType as string) === 'default' || (item.elTagType as string) === '') &&
            (item.elTagClass === '' || item.elTagClass == null)
          "
          :key="item.value"
          :index="index"
          :class="item.elTagClass"
        >
          {{ item.label + ' ' }}
        </span>
        <el-tag
          v-else
          :key="item.value + ''"
          :disable-transitions="true"
          :index="index"
          :type="
            item.elTagType === 'primary' ||
            item.elTagType === 'success' ||
            item.elTagType === 'info' ||
            item.elTagType === 'warning' ||
            item.elTagType === 'danger'
              ? item.elTagType
              : 'primary'
          "
          :class="item.elTagClass"
        >
          {{ item.label + ' ' }}
        </el-tag>
      </template>
    </template>
    <template v-if="unmatch && showValue">
      {{ unmatchArray }}
    </template>
  </div>
</template>

<script setup lang="ts">
interface Props {
  options: Array<DictDataOption>;
  value: number | string | Array<number | string>;
  showValue?: boolean;
  separator?: string;
}
const props = withDefaults(defineProps<Props>(), {
  showValue: true,
  separator: ','
});

const values = computed(() => {
  if (props.value === '' || props.value === null || typeof props.value === 'undefined') return [];
  if (typeof props.value === 'number' || typeof props.value === 'boolean') return [props.value];
  return Array.isArray(props.value) ? props.value.map(item => '' + item) : String(props.value).split(props.separator);
});

const unmatch = computed(() => {
  if (props.options?.length == 0 || props.value === '' || props.value === null || typeof props.value === 'undefined')
    return false;
  // 传入值为非数组
  let unmatch = false; // 添加一个标志来判断是否有未匹配项
  values.value.forEach(item => {
    if (!props.options.some(v => v.value == item)) {
      unmatch = true; // 如果有未匹配项，将标志设置为true
    }
  });
  return unmatch; // 返回标志的值
});

const unmatchArray = computed(() => {
  // 记录未匹配的项
  const itemUnmatchArray: Array<string | number> = [];
  if (props.value !== '' && props.value !== null && typeof props.value !== 'undefined') {
    values.value.forEach(item => {
      if (!props.options.some(v => v.value === item)) {
        itemUnmatchArray.push(item);
      }
    });
  }
  // 没有value不显示
  return handleArray(itemUnmatchArray);
});

const handleArray = (array: Array<string | number>) => {
  if (array.length === 0) return '';
  return array.reduce((pre, cur) => {
    return pre + ' ' + cur;
  });
};

const isValueMatch = (itemValue: any) => {
  return values.value.some(val => val == itemValue);
};
</script>

<style lang="scss" scoped>
.el-tag + .el-tag {
  margin-left: 10px;
}
</style>
