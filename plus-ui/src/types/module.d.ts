import type { LanguageType } from '@/lang';
import auth from '@/plugins/auth';
import cache from '@/plugins/cache';
import download from '@/plugins/download';
import modal from '@/plugins/modal';
import tab from '@/plugins/tab';

declare module 'vue' {
  interface ComponentCustomProperties {
    $modal: typeof modal;
    $tab: typeof tab;
    $download: typeof download;
    $auth: typeof auth;
    $cache: typeof cache;
    /**
     * i18n $t方法支持ts类型提示
     * @param key i18n key
     */
    $t(key: ObjKeysToUnion<LanguageType>): string;
  }
}

/**
 * { a: 1, b: { ba: { baa: 1, bab: 2 }, bb: 2} } ---> a | b.ba.baa | b.ba.bab | b.bb
 * https://juejin.cn/post/7280062870670606397
 */
export type ObjKeysToUnion<T, P extends string = ''> = T extends object
  ? {
      [K in keyof T]: ObjKeysToUnion<T[K], P extends '' ? `${K & string}` : `${P}.${K & string}`>;
    }[keyof T]
  : P;
