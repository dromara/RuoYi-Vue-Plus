# 前端约定

## 优先参考的代码来源

- `ruoyi-modules/ruoyi-gen/src/main/resources/fm/<frontendType>/*.ftl`
- 默认 Vue 模板在 `fm/vue`，React 模板在 `fm/react`
- 前端工程中与目标模块最接近的现有页面

当前 boot4 仓库通常只含后端与 generator 前端模板；如果前端工程不在当前仓库根目录，先以 generator 模板约定为准，再对照用户提供的前端工程或官方前端分支：

- Vue 前端：`https://gitee.com/JavaLionLi/plus-ui/tree/6.X-Vue`
- React 前端：`https://gitee.com/JavaLionLi/plus-ui/tree/6.X-React`

## 前端模板选择规则

- `gen_table.frontend_type` 存字符串，值直接对应 `fm` 下的模板目录，例如 `vue`、`react`。
- 生成器按 `fm/<frontendType>/api.ts.ftl`、`types.ts.ftl`、`index.*.ftl`、`index-tree.*.ftl` 查找模板。
- 页面输出后缀由页面模板文件名决定：`index.vue.ftl` 输出 `index.vue`，`index.tsx.ftl` 输出 `index.tsx`。
- 新增其他前端时优先只新增 `fm/<frontendType>` 目录和对应 FTL 文件，不在 Java 代码里增加数字枚举或硬编码分支。

## API 文件规则

- Vue 模板从 `@/utils/request` 引入 `request`，从 `@/utils/api-types` 引入 `AxiosPromise`，从 `@/api/types` 引入 `PageResult`。
- React 模板从 `@/api/request` 引入 `request`，从 `@/api/types` 引入 `R`、`PageResult`。
- 本模块类型：Vue 模板从 `@/api/<module>/<business>/types` 引入，React 模板从 `./types` 引入。
- Vue 列表接口通常返回 `AxiosPromise<PageResult<Vo>>`；React 列表接口通常返回 `request<R<PageResult<Vo>>>(...)`。
- 常规接口命名和路由保持：
  `listXxx` -> `GET /<module>/<business>/list`
  `getXxx` -> `GET /<module>/<business>/{id}`
  `addXxx` -> `POST /<module>/<business>`
  `updateXxx` -> `PUT /<module>/<business>`
  `delXxx` -> `DELETE /<module>/<business>/{id or ids}`

## 类型文件规则

- 定义 `VO`、`Form`、`Query`。
- `Form` 通常继承 `BaseEntity`。
- 非树表页面的 `Query` 通常继承 `PageQuery`。
- 各类 ID 字段通常用 `string | number`。
- Java 数值类型通常映射为 `number`。
- Boolean 映射为 `boolean`。
- 其他生成字段默认多为 `string`。
- 存在日期范围查询时保留 `params`：Vue 模板通常是 `params?: any`，React 模板通常是 `params?: Record<string, unknown>`。

## Vue 页面规则

- 使用 `<script setup lang="ts">`。
- 常见 import 来自本模块 API 和本地 `types`。
- 新版生成器优先使用 hooks：`useLoading`、`useSearchToggle`、`useSearchReset`、`useTableSelection`、`useFormDialog`，日期范围使用 `useDateRangeQuery`。
- 字典通常通过 `toRefs<any>(useDict(...))` 解构。
- 常见状态包括：列表数组、`loading`、`buttonLoading`、`showSearch`、`ids`、`single`、`multiple`、`total`。
- 查询和表单状态通常放在 `reactive<PageData<Form, Query>>({...})` 中，并通过 `toRefs(data)` 暴露。
- 弹窗状态优先由 `useFormDialog` 返回的 `dialog`、`openDialog`、`showDialog`、`closeDialog` 管理。
- 表单引用通常命名为 `queryFormRef` 和 `<business>FormRef`。

## React 页面规则

- 使用 `index.tsx`，组件默认导出 `<BusinessName>Page`。
- 页面主体优先沿用 Ant Design Pro：`PageContainer`、`ProTable`、`ModalForm`、`ProColumns`、`ActionType`。
- 表单优先使用 `Form.useForm<Form>()`，弹窗开关优先使用 `ahooks` 的 `useBoolean`。
- 权限通过 `useUserStore` 取 `userInfo`，再用 `hasPermi(userInfo, ['module:business:action'])` 生成 `canAdd`、`canEdit`、`canRemove`、`canExport`。
- 表格选择使用 `useTableSelection<VO>(row => row.id)`；表格刷新使用 `actionRef.current?.reload()` 或 `reloadAndRest?.()`。
- 字典使用 `useDict` 和 `dictOptions`，展示使用 `DictTag`。
- 日期范围使用 `useDateRangeQuery`，在 `ProTable` 的 `request` 中由 `toPageQuery(params)` 转查询参数后再应用范围字段。
- 导出使用 `useTableExport`，路径保持 `/<module>/<business>/export`。
- 文件、图片、富文本组件使用 React 工程已有的 `FileUpload`、`ImageUpload`、`ImagePreview`、`RichTextEditor`。

## Vue 页面行为规则

- `getList` 负责通过 `withLoading` 设置 loading、处理日期范围参数、调用列表接口、回填 `rows` 和 `total`。
- `handleQuery` 通常先把 `pageNum` 重置为 `1`，再重新查询。
- `resetQuery` 优先使用 `useSearchReset`，通过 `resetExtras` 清空日期范围，再重新加载。
- `handleSelectionChange` 优先使用 `useTableSelection` 返回的方法，更新 `ids`、`single`、`multiple`。
- `handleAdd` 先重置表单，再通过 `openDialog` 打开弹窗。
- `handleUpdate` 先重置并查详情，再 `Object.assign(form.value, res.data)`，最后通过 `showDialog` 打开弹窗。
- `submitForm` 校验表单、切换 `buttonLoading`、根据主键判断调用新增还是更新、提示成功并刷新列表。
- `handleDelete` 使用 `modal.confirm(...)` 确认，再调用删除接口并刷新。
- `handleExport` 使用 `download as requestDownload` 从 `@/utils/request` 导出的下载方法。

## React 页面行为规则

- React `ProTable` 页面通过 `request` 回调加载列表并返回 `toTableData(res)`；新增、修改、删除成功后调用 `actionRef` 刷新。
- React 弹窗提交函数根据主键判断调用新增还是更新，成功后 `message.success('操作成功')` 并重置表单。

## 模板结构规则

- 优先保持生成器的页面布局结构，不在 Vue 和 React 之间互相移植组件体系。
- Vue 保留 `v-hasPermi="['module:business:add']"` 这类权限指令。
- Vue 继续使用仓库已有组件：`right-toolbar`、`pagination`、`dict-tag`、`image-preview`、`image-upload`、`file-upload`、`editor`。
- React 继续使用仓库已有组件：`RowActions`、`DictTag`、`ImagePreview`、`ImageUpload`、`FileUpload`、`RichTextEditor`。
- 已有页面对时间列使用 `parseTime` 时，新页面保持一致。
- Vue BETWEEN 日期查询继续使用 `el-date-picker`，脚本侧通过 `useDateRangeQuery` 生成 `dateRangeXxx`、`applyXxxDateRange`、`resetXxxDateRange`。
- React BETWEEN 日期查询继续使用 `ProTable` 的 `dateTimeRange` 搜索列，查询侧通过 `useDateRangeQuery` 写入 `params`。

## 避免事项

- 生成器风格页面不要突然换成完全不同的状态管理方式，除非该前端目录本身已经这么做。
- 模块已使用字典时，不要把选项文案硬编码到页面里。
- 不要让 API 函数名和路由段偏离后端约定。
- 后端 BO/service 依赖 begin/end 参数时，不要从查询对象里删掉 `params` 和日期范围处理。
- 不要把 Vue 的 `proxy`、`v-hasPermi`、Element Plus 组件写进 React 页面，也不要把 React 的 `ProTable`、`ModalForm`、Ant Design 权限判断写进 Vue 页面。
