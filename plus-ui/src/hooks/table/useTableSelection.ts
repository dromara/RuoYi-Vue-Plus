const isSelectableId = (value: unknown): value is string | number => {
  return typeof value === 'string' || typeof value === 'number';
};

export function useTableSelection<T, ID extends string | number = string | number>(getRowId: (row: T) => ID) {
  const ids = ref<ID[]>([]);
  const selectedRows = ref<T[]>([]);
  const single = ref(true);
  const multiple = ref(true);

  const handleSelectionChange = (selection: T[]) => {
    selectedRows.value = selection;
    ids.value = selection.map(getRowId).filter(isSelectableId) as ID[];
    single.value = selection.length !== 1;
    multiple.value = selection.length === 0;
  };

  const clearSelection = () => {
    selectedRows.value = [];
    ids.value = [];
    single.value = true;
    multiple.value = true;
  };

  return {
    ids,
    selectedRows,
    single,
    multiple,
    handleSelectionChange,
    clearSelection
  };
}
