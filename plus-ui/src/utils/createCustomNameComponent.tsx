/**
 * 后台返回的路由动态生成name 解决缓存问题
 * 感谢 @fourteendp
 * 详见 https://github.com/vbenjs/vue-vben-admin/issues/3927
 */
import { Component, defineComponent, h } from 'vue';

interface Options {
  name?: string;
}

export function createCustomNameComponent(loader: () => Promise<any>, options: Options = {}): () => Promise<Component> {
  const { name } = options;
  let component: Component | null = null;
  let wrappedComponent: Component | null = null;

  const load = async () => {
    try {
      const { default: loadedComponent } = await loader();
      component = loadedComponent;
    } catch (error) {
      console.error(`Cannot resolve component ${name}, error:`, error);
    }
  };

  return async () => {
    if (!component) {
      await load();
    }

    if (!wrappedComponent) {
      wrappedComponent = defineComponent({
        name,
        render() {
          return h(component as Component);
        }
      });
    }

    return Promise.resolve(wrappedComponent);
  };
}
