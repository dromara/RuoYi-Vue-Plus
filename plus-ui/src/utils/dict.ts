import { getDicts } from '@/api/system/dict/data';
import { useDictStore } from '@/store/modules/dict';

const pendingRequests = new Map<string, Promise<DictDataOption[]>>();

/**
 * 获取字典数据
 */
export const useDict = (...args: string[]): { [key: string]: DictDataOption[] } => {
  const res = reactive<{ [key: string]: DictDataOption[] }>({});

  args.forEach(async dictType => {
    res[dictType] = [];
    const dicts = useDictStore().getDict(dictType);
    if (dicts) {
      res[dictType] = dicts;
    } else {
      if (!pendingRequests.has(dictType)) {
        const request = getDicts(dictType)
          .then(resp => {
            const data = resp.data.map(
              (p): DictDataOption => ({
                label: p.dictLabel,
                value: p.dictValue,
                elTagType: p.listClass,
                elTagClass: p.cssClass
              })
            );
            useDictStore().setDict(dictType, data);
            return data;
          })
          .finally(() => pendingRequests.delete(dictType));
        pendingRequests.set(dictType, request);
      }
      res[dictType] = await pendingRequests.get(dictType)!;
    }
  });
  return res;
};
