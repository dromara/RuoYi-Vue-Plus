---
name: frontend-crud-coding
description: 在 plus-ui 前端项目中按真实 Vue 3 + TypeScript + Element Plus + oxlint/oxfmt 代码风格生成或修改页面、API、types、hooks 接入和样式壳。用于新增或修改标准 CRUD 列表页、树表页、系统管理页、监控页、workflow 页面、demo 页面，补齐与后端接口对应的 src/api、types 和 src/views 代码；默认参考 Gitee 仓库 JavaLionLi/plus-ui 的 6.X-Vue 分支，触发后应先读取适用 references，再阅读目标模块真实代码和项目内 gen 代码生成模板。
---

# 前端编码规范

先对齐当前前端项目里的真实实现，再参考项目内 `gen` 目录下的代码生成模板。不要只套通用 Vue 模板，也不要把 generator 模板原样复制进来而忽略当前项目已经演进出的 hooks、页面壳、类型入口和下载方式。

## 项目基线

- 基线仓库：`https://gitee.com/JavaLionLi/plus-ui`
- 默认分支：`6.X-Vue`
- 远端引用必须同时标记仓库、分支和文件路径，例如 `https://gitee.com/JavaLionLi/plus-ui/blob/6.X-Vue/gen/index.vue.ftl` 或 `branch=6.X-Vue, path=gen/index.vue.ftl`。
- 不要在 skill、reference 或 agent 文档中写本机绝对路径；本地文件读取以运行时工作目录为准。
- 如果用户指定其他分支，先按用户分支读取同一仓库的对应文件，并在结果中说明使用的分支。

## 执行流程

1. 判断任务类型：新增标准 CRUD、树表、已有页面增强、复杂业务页、只补 API/types。
2. 按“文档读取规则”读取必要 reference，不一次性展开所有资料。
3. 阅读目标目录下最近似的真实代码：
   - 标准单表优先看 `src/views/demo/demo/index.vue`、`src/api/demo/demo/*`。
   - 树表优先看 `src/views/demo/tree/index.vue`、`src/views/workflow/category/index.vue`。
   - 系统复杂页优先看 `src/views/system/user/index.vue`、`system/role`、`system/post`、`system/config`。
   - workflow 业务页优先看 `src/views/workflow/*` 同类页面。
4. 新增标准页面前，对照项目内模板确认基础骨架：
   - API 模板：`gen/api.ts.ftl`
   - types 模板：`gen/types.ts.ftl`
   - 标准单表页模板：`gen/index.vue.ftl`
   - 树表页模板：`gen/index-tree.vue.ftl`
5. 新增代码时通常同步维护 `src/api/<module>/<business>/index.ts`、`types.ts`、`src/views/<module>/<business>/index.vue`。
6. 增强已有页面时只做增量修改，保留原页面的树筛选、导入导出、列显隐、权限、字典、弹窗和路由跳转能力。
7. 修改完成后按影响范围运行验证：优先 `pnpm exec vue-tsc --noEmit`，改动页面或导入时再跑 `pnpm lint`，大范围变更再跑 `pnpm build`。

## 文档读取规则

- 前端 API、types、页面、hooks、样式和验证规则，先读 [references/frontend.md](references/frontend.md)。
- 不确定任务边界、需要标准用例或提问方式时，再读 [references/examples.md](references/examples.md)。
- reference 只约束实现方式和自检范围；发生冲突时，以当前模块真实代码和实际调用点为准。

## 优先级规则

发生冲突时按下面顺序决策：

1. 目标目录下最近似页面、API、types 的真实实现。
2. 当前项目公共 hooks、组件、工具和样式约定。
3. 项目内 `gen` 代码生成模板。
4. 通用 Vue 3 / Element Plus 习惯。

也就是说：

- 同模块已有页面怎么写，优先怎么写。
- 没有现成页面时，使用项目内 `gen` 模板作为骨架，再改成当前项目风格。
- 复杂模块不能为了“标准 CRUD”退化成裸模板页。

## 仓库通用规则

- 遵循 [`.editorconfig`](../../../.editorconfig)：UTF-8、LF、2 空格缩进；Markdown 例外。
- 当前仓库没有 `.prettierrc`，格式脚本是 `pnpm run fmt` 调用 `oxfmt .`，lint 脚本是 `pnpm lint` 调用 `oxlint src`。
- 页面优先使用 `<script setup name="Xxx" lang="ts">`。
- API 返回类型优先从 `@/utils/api-types` 引入 `AxiosPromise`，分页结果从 `@/api/types` 引入 `PageResult`。
- 请求统一通过 `@/utils/request`，导出下载使用 `import { download as requestDownload } from '@/utils/request';`。
- 标准列表页优先复用 `useLoading`、`useSearchToggle`、`useSearchReset`、`useTableSelection`、`useFormDialog`、`useDateRangeQuery`。
- 页面壳优先使用 `p-2 app-container <module>-<business>-page`、`search-panel`、`toolbar-shell`、`data-table`、`right-toolbar`、`pagination`。
- 新页面不要无故引入另一套状态管理、请求封装、样式体系或权限写法。

## 目录映射规则

通常按下面关系组织代码：

- 后端 `/system/user/*` 对应 `src/api/system/user/*` 与 `src/views/system/user/*`
- 后端 `/monitor/xxx/*` 对应 `src/api/monitor/xxx/*` 与 `src/views/monitor/xxx/*`
- 后端 `/workflow/xxx/*` 对应 `src/api/workflow/xxx/*` 与 `src/views/workflow/xxx/*`
- 后端 `/demo/xxx/*` 对应 `src/api/demo/xxx/*` 与 `src/views/demo/xxx/*`

标准新增通常至少包含：

- `src/api/<module>/<business>/index.ts`
- `src/api/<module>/<business>/types.ts`
- `src/views/<module>/<business>/index.vue`

按业务复杂度，可能继续补：

- 导入弹窗
- 详情抽屉或详情页
- 树筛选面板
- 列显隐配置
- 分配/授权子页面
- 自定义 SCSS 样式

## 任务分型

### 1. 标准单表 CRUD

以 `gen/index.vue.ftl`、`gen/api.ts.ftl`、`gen/types.ts.ftl` 和 `src/views/demo/demo/index.vue` 为主要起点，补齐列表、搜索、分页、新增、编辑、删除、导出、权限、类型和验证。

### 2. 树表 CRUD

以 `src/views/demo/tree/index.vue`、`src/views/workflow/category/index.vue` 为主要起点。列表接口通常返回数组而不是 `PageResult`，页面使用 `handleTree`、`useTreeTableExpand`，`Query` 通常不继承 `PageQuery`。

### 3. 强业务页面

如果页面包含树筛选、导入导出、更多菜单、状态切换、角色分配、详情抽屉、复杂校验、联动选择或独立路由，优先增量修改现有页面。不要重写成简单 CRUD。

### 4. 工作流页面

workflow 目录优先参考 `src/views/workflow/*`。流程定义、流程实例、任务列表、请假申请等页面通常有业务按钮、弹窗和路由跳转，不要硬套 system 模块。

### 5. 只补 API 和 types

只维护 `src/api/<module>/<business>/index.ts` 与 `types.ts`，但仍要与后端路由、返回结构、当前模块导入方式和类型入口一致。

## 输出要求

使用本 skill 时，默认期望产出应满足：

- 类型完整，不把页面逻辑大量写成 `any`。
- API 路径、函数名、权限标识与后端接口保持一致。
- 标准页查询、重置、分页、弹窗、提交、删除、导出流程闭环完整。
- 复杂页面保留原有交互能力和业务约束。
- 代码体现当前项目 hooks、页面壳和下载方式，而不是 `gen` 模板裸输出。
- 交付前说明运行过的验证命令；如果无法验证，说明原因。

## 快速检查清单

- `AxiosPromise` 是否来自 `@/utils/api-types`。
- `PageResult` 是否来自 `@/api/types`。
- API `params` 和 `data` 是否与后端方法一致。
- 日期范围是否通过 `useDateRangeQuery` 或附近页面现有方式处理。
- 列表 loading 是否通过 `useLoading` 或原页面方式维护。
- 弹窗是否通过 `useFormDialog` 或原页面方式维护。
- 多选状态是否通过 `useTableSelection` 或原页面方式维护。
- 权限指令是否保持同文件一致，默认使用当前项目主流 `v-hasPermi`。
- 导出是否使用 `requestDownload('<module>/<business>/export', { ...queryParams.value }, '<name>_<time>.xlsx')`。
- 页面壳是否保留 `search-panel`、`table-panel`、`toolbar-shell`、`data-table`、`right-toolbar`、`pagination`。

## 推荐提问方式

推荐把请求描述到下面粒度：

- 目标模块和业务名
- 后端接口前缀
- 是新增页面、修改页面，还是只补 API/types
- 是否需要导入、导出、树筛选、树表、状态切换、字典、权限按钮
- 希望参考哪个现有页面

例如：

- 使用 `$frontend-crud-coding` 为 `/system/client` 补一套标准 CRUD 页面，参考 `gen` 模板、`demo/demo` 和 `system/client`。
- 使用 `$frontend-crud-coding` 修改 `workflow/category` 列表页，增加导出按钮和状态筛选，保持当前 workflow 风格。
