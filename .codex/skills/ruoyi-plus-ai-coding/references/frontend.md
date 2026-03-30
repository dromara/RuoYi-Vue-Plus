# 前端约定

## 优先参考的代码来源

- `ruoyi-modules/ruoyi-gen/src/main/resources/vm/ts/*.vm`
- `ruoyi-modules/ruoyi-gen/src/main/resources/vm/vue/*.vm`
- 前端工程中与目标模块最接近的现有页面

如果任务涉及前端，先看仓库里实际使用的前端目录和同类页面，不要直接套通用 Vue 习惯。

## API 文件规则

- 从 `@/utils/request` 引入 `request`。
- 从 `axios` 引入 `AxiosPromise`。
- 从 `@/api/<module>/<business>/types` 引入本模块类型。
- 列表接口通常返回 `AxiosPromise<PageResult<Vo>>`。
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
- 存在日期范围查询时保留 `params?: any`。

## Vue 页面规则

- 使用 `<script setup lang="ts">`。
- 常见 import 来自本模块 API 和本地 `types`。
- 通过 `getCurrentInstance()` 取 `proxy`，使用项目注入的公共工具。
- 字典通常通过 `proxy?.useDict(...)` 获取，再用 `toRefs` 解构。
- 常见状态包括：列表数组、`loading`、`buttonLoading`、`showSearch`、`ids`、`single`、`multiple`、`total`。
- 查询和表单状态通常放在 `reactive<PageData<Form, Query>>({...})` 中。
- 弹窗状态通常使用 `dialog.visible` 和 `dialog.title`。
- 表单引用通常命名为 `queryFormRef` 和 `<business>FormRef`。

## 页面行为规则

- `getList` 负责设置 loading、处理日期范围参数、调用列表接口、回填 `rows` 和 `total`。
- `handleQuery` 通常先把 `pageNum` 重置为 `1`，再重新查询。
- `resetQuery` 负责清空日期范围和查询表单，然后重新加载。
- `handleSelectionChange` 更新 `ids`、`single`、`multiple`。
- `handleAdd` 先重置表单，再打开弹窗。
- `handleUpdate` 先查详情，再把数据赋值到表单。
- `submitForm` 校验表单、切换 `buttonLoading`、根据主键判断调用新增还是更新、提示成功并刷新列表。
- `handleDelete` 使用 `proxy?.$modal.confirm(...)` 确认，再调用删除接口并刷新。
- `handleExport` 使用 `proxy?.download(...)`。

## 模板结构规则

- 优先保持生成器的页面布局结构：搜索区卡片、表格区卡片、工具栏、分页、弹窗表单。
- 保留 `v-hasPermi="['module:business:add']"` 这类权限指令。
- 继续使用仓库已有组件：`right-toolbar`、`pagination`、`dict-tag`、`image-preview`、`image-upload`、`file-upload`、`editor`。
- 已有页面对时间列使用 `parseTime` 时，新页面保持一致。
- BETWEEN 日期查询继续使用 `el-date-picker` 加 `proxy?.addDateRange(...)`。

## 避免事项

- 生成器风格页面不要突然换成完全不同的状态管理方式，除非该前端目录本身已经这么做。
- 模块已使用字典时，不要把选项文案硬编码到页面里。
- 不要让 API 函数名和路由段偏离后端约定。
- 后端 BO/service 依赖 begin/end 参数时，不要从查询对象里删掉 `params` 和日期范围处理。
