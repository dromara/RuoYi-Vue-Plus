import type { PluginOption } from 'vite';

import { normalizePath } from 'vite';
import { parse } from 'vue/compiler-sfc';

/** 模板 AST 元素节点类型 */
const NODE_TYPE_ELEMENT = 1;
/** 模板 AST 注释节点类型 */
const NODE_TYPE_COMMENT = 3;
/** 模板 AST 指令属性类型 */
const NODE_TYPE_DIRECTIVE = 7;

/**
 * 判断节点是否为元素节点
 */
function isElement(node: any): boolean {
  return node?.type === NODE_TYPE_ELEMENT;
}

/**
 * 判断节点是否为注释节点
 * @description 注释节点同样是 transition 的一个子节点，会导致多节点
 */
function isComment(node: any): boolean {
  return node?.type === NODE_TYPE_COMMENT;
}

/**
 * 节点用于报错展示的标签文本
 */
function nodeLabel(node: any): string {
  return isComment(node) ? '<!--注释-->' : `<${node.tag}>`;
}

/**
 * 判断元素是否带有指定指令（如 v-if / v-else-if / v-else）
 */
function hasDirective(node: any, name: string): boolean {
  return (node.props || []).some(
    (prop: any) => prop.type === NODE_TYPE_DIRECTIVE && prop.name === name,
  );
}

/**
 * 判断多个元素子节点是否构成合法的 v-if / v-else-if / v-else 分支链
 * @description 分支链任意时刻只渲染一个节点，对外层 transition 是合法的
 */
function isConditionalChain(elements: any[]): boolean {
  // 首个节点必须是 v-if，后续节点必须都是 v-else-if 或 v-else
  if (!hasDirective(elements[0], 'if')) {
    return false;
  }
  return elements
    .slice(1)
    .every((el) => hasDirective(el, 'else-if') || hasDirective(el, 'else'));
}

/**
 * 检测页面模板根节点是否存在多标签的 vite 插件
 * @description 外层路由/页面切换已用 <transition> 包裹，每个页面即为 transition 的子节点。
 * Vue 的 <transition> 只能管理单个子节点，因此页面模板出现多个根节点会导致过渡失效。
 * 该插件在编译 src/views 下的页面时检测此类写法并直接报错。
 */
export const viteCheckTransitionPlugin = (): PluginOption => {
  // 仅检测 views 目录下的页面
  const targetDir = normalizePath('/src/views/');

  return {
    enforce: 'pre',
    name: 'vite:check-transition',
    transform(code, id) {
      const normalizedId = normalizePath(id);

      // 跳过非 views 下的文件、非 .vue 文件以及带 query 的子请求
      if (
        normalizedId.includes('?') ||
        !normalizedId.endsWith('.vue') ||
        !normalizedId.includes(targetDir)
      ) {
        return null;
      }

      const { descriptor, errors: parseErrors } = parse(code, {
        filename: id,
      });

      // 解析失败交由 vue 插件处理，这里不重复报错
      if (parseErrors.length > 0 || !descriptor.template?.ast) {
        return null;
      }

      // 统计根级元素节点与注释节点，二者都是 transition 的子节点，忽略空白文本
      const rootNodes = (descriptor.template.ast.children || []).filter(
        (child: any) => isElement(child) || isComment(child),
      );

      // 仅元素节点参与 v-if 分支链判断（注释不属于分支链）
      const rootElements = rootNodes.filter((node: any) => isElement(node));
      const isChain =
        rootNodes.length === rootElements.length &&
        isConditionalChain(rootElements);

      if (rootNodes.length > 1 && !isChain) {
        const relativePath = normalizedId.slice(
          normalizedId.indexOf('/src/') + 1,
        );
        const tags = rootNodes.map((node: any) => nodeLabel(node)).join(' ');

        throw new Error(
          `[check-transition] ${relativePath} 存在多个根节点: ${tags}\n  页面外层已包裹 <transition>，模板必须只有单个根节点，请用一个容器元素(如 div)包裹，并删除多余的注释/空标签。`,
        );
      }

      return null;
    },
  };
};
