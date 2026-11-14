import { createSvgIconsPlugin } from 'vite-plugin-svg-icons-ng';
import { resolve } from 'path';

export default () => {
  return createSvgIconsPlugin({
    // 指定需要缓存的图标文件夹
    iconDirs: [resolve(import.meta.dirname, '../../src/assets/icons/svg')],
    // 指定symbolId格式
    symbolId: 'icon-[dir]-[name]'
  });
};
