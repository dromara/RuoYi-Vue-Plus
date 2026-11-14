import type { App } from 'vue';
import auth from './auth';
import cache from './cache';
import download from './download';
import modal from './modal';
import tab from './tab';

/**
 * 仅挂载无法在组合式中通过 import 替代的实例级能力（与业务模块解耦的壳层 API）。
 * 字典、时间、树、请求下载等请在 setup 中从 @/utils、@/api 显式导入。
 */
export default function installPlugin(app: App) {
  app.config.globalProperties.$tab = tab;
  app.config.globalProperties.$modal = modal;
  app.config.globalProperties.$cache = cache;
  app.config.globalProperties.$download = download;
  app.config.globalProperties.$auth = auth;
}
