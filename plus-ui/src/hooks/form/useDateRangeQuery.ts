import { addDateRange } from '@/utils/ruoyi';

export function useDateRangeQuery(propName?: string) {
  const dateRange = ref<any>(['', '']);

  const applyDateRange = <T extends Record<string, any>>(queryParams: T) => {
    return addDateRange(queryParams, dateRange.value, propName);
  };

  const resetDateRange = () => {
    dateRange.value = ['', ''];
  };

  return {
    dateRange,
    applyDateRange,
    resetDateRange
  };
}
