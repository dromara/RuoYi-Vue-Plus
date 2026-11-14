import type { Ref } from 'vue';

type TableSortOrder = 'ascending' | 'descending' | null;

interface TableSortState {
  prop: string;
  order: TableSortOrder;
}

interface UseTableSortQueryOptions<T extends Record<string, any>> {
  queryParams: Ref<T>;
  defaultSort: TableSortState;
  tableRef?: Ref<ElTableInstance | undefined>;
  onSortChange?: () => void;
}

export function useTableSortQuery<T extends { orderByColumn?: string; isAsc?: string | null }>(
  options: UseTableSortQueryOptions<T>
) {
  const { queryParams, defaultSort: initialSort, tableRef, onSortChange } = options;
  const defaultSort = ref<TableSortState>({ ...initialSort });

  const handleSortChange = (column: { prop: string; order: TableSortOrder }) => {
    queryParams.value.orderByColumn = column.prop as T['orderByColumn'];
    queryParams.value.isAsc = column.order as T['isAsc'];
    onSortChange?.();
  };

  const resetSort = () => {
    queryParams.value.orderByColumn = defaultSort.value.prop as T['orderByColumn'];
    queryParams.value.isAsc = defaultSort.value.order as T['isAsc'];
    if (tableRef?.value && defaultSort.value.order) {
      tableRef.value.sort(defaultSort.value.prop, defaultSort.value.order);
      return;
    }
    onSortChange?.();
  };

  return {
    defaultSort,
    handleSortChange,
    resetSort
  };
}
