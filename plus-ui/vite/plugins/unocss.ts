import UnoCss from 'unocss/vite';

export default () => {
  return UnoCss({
    hmrTopLevelAwait: false // unocss默认是true，低版本浏览器是不支持的，启动后会报错
  });
};
