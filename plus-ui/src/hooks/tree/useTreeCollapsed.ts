export function useTreeCollapsed(initialValue = false) {
  const treeCollapsed = ref(initialValue);

  const toggleCollapsed = () => {
    treeCollapsed.value = !treeCollapsed.value;
  };

  const setCollapsed = (value: boolean) => {
    treeCollapsed.value = value;
  };

  return {
    treeCollapsed,
    toggleCollapsed,
    setCollapsed
  };
}
