import type { Ref } from 'vue';

interface UseSearchResetOptions<T extends Record<string, any>> {
  queryFormRef: Ref<ElFormInstance | undefined>;
  queryParams?: Ref<T>;
  pageNumKey?: keyof T;
  pageSizeKey?: keyof T;
  initialPageNum?: number;
  initialPageSize?: number;
  resetExtras?: () => void;
  afterReset?: () => void;
}

export function useSearchReset<T extends Record<string, any>>(options: UseSearchResetOptions<T>) {
  const {
    queryFormRef,
    queryParams,
    pageNumKey,
    pageSizeKey,
    initialPageNum = 1,
    initialPageSize,
    resetExtras,
    afterReset
  } = options;

  const resetQuery = () => {
    queryFormRef.value?.resetFields();
    if (queryParams?.value) {
      if (pageNumKey) {
        queryParams.value[pageNumKey] = initialPageNum as T[keyof T];
      }
      if (pageSizeKey && initialPageSize !== undefined) {
        queryParams.value[pageSizeKey] = initialPageSize as T[keyof T];
      }
    }
    resetExtras?.();
    afterReset?.();
  };

  return {
    resetQuery
  };
}
