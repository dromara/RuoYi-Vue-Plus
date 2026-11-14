// 自定义国际化配置
import { createI18n } from 'vue-i18n';
import { LanguageEnum } from '@/enums/LanguageEnum';
import en_US from '@/lang/en_US';
import zh_CN from '@/lang/zh_CN';

/**
 * 获取当前语言
 * @returns zh-cn|en ...
 */
export const getLanguage = (): LanguageEnum => {
  const language = useStorage<LanguageEnum>('language', LanguageEnum.zh_CN);
  if (language.value) {
    return language.value;
  }
  return LanguageEnum.zh_CN;
};

const i18n = createI18n({
  globalInjection: true,
  allowComposition: true,
  legacy: false,
  locale: getLanguage(),
  messages: {
    zh_CN: zh_CN,
    en_US: en_US
  }
});

export default i18n;

export type LanguageType = typeof zh_CN;
