import type { Ref } from 'vue';

interface UseTreeTableExpandOptions<T> {
  tableRef: Ref<ElTableInstance | undefined>;
  data: Ref<T[]>;
  initialExpandAll?: boolean;
  getChildren?: (row: T) => T[] | undefined;
}

export function useTreeTableExpand<T extends Record<string, any>>(options: UseTreeTableExpandOptions<T>) {
  const { tableRef, data, initialExpandAll = true, getChildren = row => row.children as T[] | undefined } = options;

  const isExpandAll = ref(initialExpandAll);

  const toggleRows = (rows: T[], expanded: boolean) => {
    rows.forEach(row => {
      tableRef.value?.toggleRowExpansion(row, expanded);
      const children = getChildren(row);
      if (children?.length) {
        toggleRows(children, expanded);
      }
    });
  };

  const setExpandAll = (expanded: boolean) => {
    isExpandAll.value = expanded;
    toggleRows(data.value, expanded);
  };

  const handleToggleExpandAll = () => {
    setExpandAll(!isExpandAll.value);
  };

  return {
    isExpandAll,
    setExpandAll,
    handleToggleExpandAll
  };
}
