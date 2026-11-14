import vue from '@vitejs/plugin-vue';

import createUnoCss from './unocss.js';
import createAutoImport from './auto-import.js';
import createComponents from './components.js';
import createSvgIconsPlugin from './svg-icon.js';
import createCompression from './compression.js';
import createSetupExtend from './setup-extend.js';
import { viteCheckTransitionPlugin } from './check-transition.js';

export default (viteEnv: any, isBuild = false): [] => {
  const vitePlugins: any = [];
  vitePlugins.push(vue());
  vitePlugins.push(createUnoCss());
  vitePlugins.push(createAutoImport());
  vitePlugins.push(createComponents());
  vitePlugins.push(createCompression(viteEnv));
  vitePlugins.push(createSvgIconsPlugin());
  vitePlugins.push(createSetupExtend());
  vitePlugins.push(viteCheckTransitionPlugin())
  return vitePlugins;
};
