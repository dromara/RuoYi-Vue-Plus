# 前端约定

## 优先参考的代码来源

- 默认远端基线：`https://gitee.com/JavaLionLi/plus-ui`，分支 `6.X-Vue`。引用远端文件时必须同时写明 `branch=6.X-Vue` 和仓库内路径。
- 当前目标目录下最近似页面、API、types。
- 标准单表：`src/views/demo/demo/index.vue`、`src/api/demo/demo/index.ts`、`src/api/demo/demo/types.ts`。
- 树表：`src/views/demo/tree/index.vue`、`src/views/workflow/category/index.vue`。
- 复杂系统页：`src/views/system/user/index.vue`、`src/views/system/role/index.vue`、`src/views/system/post/index.vue`、`src/views/system/config/index.vue`。
- workflow 页：`src/views/workflow/*`、`src/api/workflow/*`。
- 监控页：`src/views/monitor/*`、`src/api/monitor/*`。
- 公共 hooks：`src/hooks/async/useLoading.ts`、`src/hooks/dialog/*`、`src/hooks/form/*`、`src/hooks/table/*`、`src/hooks/tree/*`。
- 项目内 generator 模板：
  `gen/api.ts.ftl`
  `gen/types.ts.ftl`
  `gen/index.vue.ftl`
  `gen/index-tree.vue.ftl`

## 基础栈与格式

- 技术栈是 Vue 3 + TypeScript + Element Plus + Vite + Pinia。
- 包管理按仓库现状使用 pnpm。
- `.editorconfig` 要求 UTF-8、LF、2 空格缩进。
- 当前仓库没有 `.prettierrc`；格式化使用 `pnpm run fmt`，lint 使用 `pnpm lint`。
- 不要在一个页面里混入与仓库不一致的格式和写法。
- 不要把本机绝对路径写进 skill 输出或参考文档；跨环境参考统一使用 Gitee 仓库 URL、分支和仓库内相对路径。

## API 文件规则

- 标准 API 文件放在 `src/api/<module>/<business>/index.ts`，同目录维护 `types.ts`。
- import 顺序优先跟随附近文件，标准生成页常见形式：
  `import type { XxxForm, XxxQuery, XxxVO } from '@/api/<module>/<business>/types';`
  `import type { PageResult } from '@/api/types';`
  `import type { AxiosPromise } from '@/utils/api-types';`
  `import request from '@/utils/request';`
- 不要从 `axios` 引入 `AxiosPromise`。
- 列表分页接口通常返回 `AxiosPromise<PageResult<XxxVO>>`。
- 树表列表接口通常返回 `AxiosPromise<XxxVO[]>`。
- 详情接口返回 `AxiosPromise<XxxVO>`；复杂详情返回单独的 `InfoVO`。
- 标准函数命名：
  `listXxx` -> `GET /<module>/<business>/list`
  `getXxx` -> `GET /<module>/<business>/{id}`
  `addXxx` -> `POST /<module>/<business>`
  `updateXxx` -> `PUT /<module>/<business>`
  `delXxx` -> `DELETE /<module>/<business>/{id or ids}`
  `changeXxxStatus` -> `PUT /<module>/<business>/changeStatus`
- query string 用 `params`，请求体用 `data`。
- 加密、防重复提交等 headers 直接写在请求配置里，例如用户重置密码中的 `isEncrypt`、`repeatSubmit`。
- 当前仓库有些 API 使用 `export const`，有些使用 `export function`；新增标准 CRUD 优先跟随 `gen/api.ts.ftl` 和相邻模块。
- 只有相邻模块已有 `export default { ... }` 聚合时才新增默认导出。

## 类型文件规则

- 标准类型定义 `VO`、`Form`、`Query`，必要时补 `InfoVO`、`TreeVO`、`ResetPwdForm` 等扩展类型。
- `Form` 通常继承 `BaseEntity`。
- 非树表 `Query` 通常继承 `PageQuery`。
- 树表 `Query` 通常不继承 `PageQuery`。
- ID 字段通常使用 `string | number`，批量删除参数使用 `string | number | Array<string | number>`。
- Java 数值类型映射为 `number`，Boolean 映射为 `boolean`，日期/文本默认 `string`。
- 日期范围查询保留 `params?: any`，不要因为它看起来宽松就删掉。
- 列表对象、表单对象、查询对象职责分开；字段不一致时不要强行复用一个接口。
- 能明确写出类型时不要用 `any`；组件库、字典或历史接口确实无法收窄时再保留。

## Vue 页面结构规则

- 页面优先使用 `<script setup name="Xxx" lang="ts">`。
- 标准根节点使用 `class="p-2 app-container <module>-<business>-page"`；已有页面是特殊布局时保持原样。
- 标准列表页结构：
  搜索卡片 `search-panel`
  表格卡片 `table-panel`
  工具栏 `toolbar-shell`
  表格 `data-table`
  `right-toolbar`
  `pagination`
  新增/编辑 `el-dialog`
- 搜索区通过 `useSearchToggle` 控制 `showSearch`，头部点击切换。
- 列表 loading 通过 `useLoading(true)` 和 `withLoading`。
- 选择状态通过 `useTableSelection<XxxVO>(item => item.id)` 返回 `ids`、`single`、`multiple`、`handleSelectionChange`。
- 表单弹窗优先使用 `useFormDialog({ form, formRef, initialFormData })`，返回 `dialog`、`resetForm`、`openDialog`、`showDialog`、`closeDialog`。
- 仅需要简单弹窗状态或一个页面多个弹窗时使用 `useDialogState`。
- 日期范围查询优先使用 `useDateRangeQuery()`；带后端参数名时使用 `useDateRangeQuery('CreateTime')` 等相邻页面模式。
- 查询和表单状态通常放在 `reactive<PageData<Form, Query>>({ form, queryParams, rules })`，再 `toRefs(data)`。

## 页面行为规则

- `getList` 负责设置 loading、调用列表接口、回填列表和 `total`。
- `handleQuery` 先把 `queryParams.value.pageNum = 1`，再调用 `getList()`；树表无分页时只调用 `getList()`。
- `resetQuery` 使用 `useSearchReset`，分页页传 `pageNumKey: 'pageNum'`，需要时传 `pageSizeKey` 和 `resetExtras`。
- `handleAdd` 使用 `openDialog('添加xxx')`；如果有树/联动选项，打开前后按现有页面加载选项。
- `handleUpdate` 先 `reset()`，再按行或 `ids.value[0]` 查详情，`Object.assign(form.value, res.data)`，最后 `showDialog('修改xxx')`。
- `submitForm` 表单校验通过后设置 `buttonLoading`，根据主键判断新增或修改，成功后 `modal.msgSuccess('操作成功')`、关闭弹窗、刷新列表。
- `handleDelete` 使用 `modal.confirm(...)` 二次确认，再调用删除接口，成功提示并刷新。
- `handleExport` 使用 `requestDownload('<module>/<business>/export', { ...queryParams.value }, '<business>_<timestamp>.xlsx')`。
- 状态切换失败时要把 switch 值回滚，参考 generator 模板和现有 `system/user`、`system/role`。
- 导入上传使用 `globalHeaders()`、`import.meta.env.VITE_APP_BASE_API`、`ElUpload`，优先参考 `system/user` 或流程定义页面。

## 字典、权限与公共工具

- 字典使用 `useDict`：
  `const { sys_normal_disable } = toRefs<any>(useDict('sys_normal_disable'));`
- 新增代码默认使用当前项目主流 `v-hasPermi`。
- 如果正在修改的文件已经混用或使用 `v-has-permi`，保持同文件现状，不为统一写法而重排无关代码。
- `el-dropdown-item` 延迟加载导致权限指令不可靠时，使用 `v-if="checkPermi([...])"`，参考 `system/user`。
- 常用工具：
  `modal` from `@/plugins/modal`
  `download as requestDownload` from `@/utils/request`
  `useDict` from `@/utils/dict`
  `checkPermi` from `@/utils/permission`
  `handleTree`、`parseStrEmpty` from `@/utils/ruoyi`

## 组件与样式规则

- 优先复用公共组件：`right-toolbar`、`pagination`、`DictTag` / `dict-tag`、`ImageUpload` / `image-upload`、`ImagePreview` / `image-preview`、`FileUpload` / `file-upload`、`Editor` / `editor`、`TreePanel`。
- 标准页面尽量使用已有页面壳类，不堆大量内联样式。
- 复杂页面需要 scoped SCSS 时优先复用：
  `@use '@/assets/styles/components/page-shell' as pageShell;`
  再按现有 mixin 使用。
- 不要为了单页需求修改全局组件样式。
- 类名保持模块语义：`system-client-page`、`workflow-category-page`、`demo-demo-page`。

## 树表规则

- 树表列表接口通常返回数组，页面通过 `handleTree<T>(res.data, 'id', 'parentId')` 组树。
- 使用 `row-key`、`:tree-props="{ children: 'children', hasChildren: 'hasChildren' }"`。
- 展开/折叠使用 `useTreeTableExpand`。
- 表单中上级节点使用 `el-tree-select`。
- 新增子节点时从当前行回填 `parentId`。
- 删除确认文案优先使用业务名称，而不是批量 ID 文案。

## 与 gen 模板的关系

- `gen` 是 plus-ui 前端项目内的生成模板；默认远端位置是 `https://gitee.com/JavaLionLi/plus-ui/tree/6.X-Vue/gen`，分支必须标记为 `6.X-Vue`。
- 本地仓库存在对应文件时优先读本地 `gen`，但不要把本地绝对路径写入 skill 或交付内容。
- 新增标准单表页面时读取 `gen/index.vue.ftl`、`gen/api.ts.ftl`、`gen/types.ts.ftl`。
- 新增树表页面时读取 `gen/index-tree.vue.ftl`、`gen/api.ts.ftl`、`gen/types.ts.ftl`。
- `gen` 模板是标准骨架，不是最终答案；落地时仍要对照目标模块真实页面和公共 hooks。
- 当前前端项目已经把生成页升级为 hooks 版：`useLoading`、`useFormDialog`、`useSearchReset`、`useTableSelection`、`useDateRangeQuery`。
- 新增标准 CRUD 时，先从 `gen` 确认字段、权限、导出、状态切换、排序、日期范围等，再落成当前项目的实际页面壳。
- 修改已有页面时，不要把现有强业务逻辑替换回 `gen` 的简化逻辑。

## 验证规则

- 只改文档或 skill：运行 skill 基础校验即可。
- 改前端 TS/Vue/API/types：优先运行 `pnpm exec vue-tsc --noEmit`。
- 改页面模板、import、权限或较多文件：再运行 `pnpm lint`。
- 改公共 hooks、组件、构建相关或大范围页面：再运行 `pnpm build`。
- 如果验证因为环境、依赖或权限失败，交付时说明失败命令和原因。

## 避免事项

- 不要从 `axios` 引入 `AxiosPromise`。
- 不要绕开 `request` 或 `requestDownload` 自造请求/下载封装。
- 不要跳过 `types.ts`，把类型全写在页面里。
- 不要删除日期范围 `params`、权限指令、导出、导入、树筛选、列显隐等现有能力。
- 不要为了“更整洁”重写复杂页面的大块业务逻辑。
- 不要在新增标准页里使用与仓库不一致的 UI 壳或状态管理方式。
