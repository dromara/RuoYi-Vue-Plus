import AutoImport from 'unplugin-auto-import/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';
import { resolve } from 'path';

export default () => {
  return AutoImport({
    // 自动导入 Vue 相关函数
    imports: ['vue', 'vue-router', '@vueuse/core', 'pinia'],
    resolvers: [
      // 自动导入 Element Plus 相关函数ElMessage, ElMessageBox... (带样式)
      ElementPlusResolver({
        importStyle: false
      })
    ],
    vueTemplate: true, // 是否在 vue 模板中自动导入
    dts: resolve(import.meta.dirname, '../../src/types/auto-imports.d.ts')
  });
};
