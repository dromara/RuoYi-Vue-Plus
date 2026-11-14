export function useLoading(initialValue = false) {
  const loading = ref(initialValue);

  const setLoading = (value: boolean) => {
    loading.value = value;
  };

  const withLoading = async <T>(task: () => Promise<T>) => {
    loading.value = true;
    try {
      return await task();
    } finally {
      loading.value = false;
    }
  };

  return {
    loading,
    setLoading,
    withLoading
  };
}
