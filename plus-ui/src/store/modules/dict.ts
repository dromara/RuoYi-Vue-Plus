import { defineStore } from 'pinia';

export const useDictStore = defineStore('dict', () => {
  const dict = ref<Map<string, DictDataOption[]>>(new Map());

  /**
   * 获取字典
   * @param _key 字典key
   */
  const getDict = (_key: string): DictDataOption[] | null => {
    if (!_key) {
      return null;
    }
    return dict.value.get(_key) || null;
  };

  /**
   * 设置字典
   * @param _key 字典key
   * @param _value 字典value
   */
  const setDict = (_key: string, _value: DictDataOption[]) => {
    if (!_key) {
      return false;
    }
    try {
      dict.value.set(_key, _value);
      return true;
    } catch (e) {
      console.error('Error in setDict:', e);
      return false;
    }
  };

  /**
   * 删除字典
   * @param _key
   */
  const removeDict = (_key: string): boolean => {
    if (!_key) {
      return false;
    }
    try {
      return dict.value.delete(_key);
    } catch (e) {
      console.error('Error in removeDict:', e);
      return false;
    }
  };

  /**
   * 清空字典
   */
  const cleanDict = (): void => {
    dict.value.clear();
  };

  return {
    dict,
    getDict,
    setDict,
    removeDict,
    cleanDict
  };
});
