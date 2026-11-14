import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';
import { resolve } from 'path';

export default () => {
  return Components({
    resolvers: [
      // 自动导入 Element Plus 组件
      ElementPlusResolver({
        importStyle: false
      })
    ],
    dts: resolve(import.meta.dirname, '../../src/types/components.d.ts')
  });
};
