// translate router.meta.title, be used in breadcrumb sidebar tagsview
import i18n from '@/lang/index';

/**
 * 获取国际化路由，如果不存在则原生返回
 * @param title 路由名称
 * @returns {string}
 */
export const translateRouteTitle = (title: string): string => {
  const hasKey = i18n.global.te('route.' + title);
  if (hasKey) {
    const translatedTitle = i18n.global.t('route.' + title);
    return translatedTitle;
  }
  return title;
};
