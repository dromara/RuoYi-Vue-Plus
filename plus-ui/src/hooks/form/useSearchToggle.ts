export function useSearchToggle(initialValue = true) {
  const showSearch = ref(initialValue);

  const toggleSearch = () => {
    showSearch.value = !showSearch.value;
  };

  const setShowSearch = (value: boolean) => {
    showSearch.value = value;
  };

  return {
    showSearch,
    toggleSearch,
    setShowSearch
  };
}
