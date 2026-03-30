# 使用案例

## 案例 1：新增标准单表 CRUD

### 用户提问示例

```text
使用 $ruoyi-plus-ai-coding 在 system 模块新增一个 client 管理的标准 CRUD。
请参考 generator 模板和现有 system 模块写法，补齐 entity、bo、vo、mapper、service、controller。
```

### 期望执行方式

- 先读 generator 的 `domain/bo/vo/service/serviceImpl/controller` 模板。
- 再读 `system` 模块里最接近的现有管理模块。
- 先生成骨架，再补权限、日志、校验、导出等细节。

## 案例 2：修改已有复杂模块

### 用户提问示例

```text
使用 $ruoyi-plus-ai-coding 修改 workflow/category 的查询和导出逻辑，保持现有模块风格，不要简化成模板式单表 CRUD。
```

### 期望执行方式

- 先读当前 workflow 模块同类代码。
- 判断这是“复杂模块增强”，不是“从零生成”。
- 增量修改原逻辑，不要重写整个 service/controller。

## 案例 3：补唯一性校验与删除前校验

### 用户提问示例

```text
使用 $ruoyi-plus-ai-coding 为 demo/demo 模块补充新增和修改时的唯一性校验，并补充删除前校验。
```

### 期望执行方式

- 优先修改 `validEntityBeforeSave(...)`。
- 根据模块现有风格补 `ServiceException` 或显式失败返回。
- 删除逻辑只补必要校验，不重构整套 CRUD。

## 案例 4：补数据权限与联表查询

### 用户提问示例

```text
使用 $ruoyi-plus-ai-coding 为 system 模块某个列表查询增加部门数据权限和联表字段返回，参考现有 user mapper 的 MPJ 与 DataPermission 写法。
```

### 期望执行方式

- 先看 `SysUserMapper` 和相关 service。
- 判断需要 `BaseMapperPlus` 重写还是 MPJ 联表。
- 保持权限注解和联表风格一致。

## 案例 5：新增后端接口并同步前端骨架

### 用户提问示例

```text
使用 $ruoyi-plus-ai-coding 为 monitor/cache 新增一个导出接口，并同步补齐前端 api/types 调用骨架。
```

### 期望执行方式

- 先补后端 `controller/service`。
- 再根据后端路由补前端 `src/api` 或 generator 风格的前端骨架。
- 保证导出接口路径和前端下载调用一致。

## 案例 6：推荐的高质量任务描述

下面这种描述最容易得到稳定结果：

```text
使用 $ruoyi-plus-ai-coding 在 workflow 模块新增一个标准列表管理功能：
1. 需要分页、导出、详情、增删改
2. 查询包含状态和创建时间范围
3. 保持现有 workflow 模块风格
4. 参考 generator 模板生成基础骨架
5. 删除前需要做业务校验
```

## 不推荐的任务描述

下面这种描述太模糊，容易导致产物偏离项目：

```text
帮我加个后端接口
```

更好的写法至少要补充：

- 模块名
- 表或业务名
- 是新增还是修改
- 是否需要分页、导出、权限、数据范围、联表
- 想参考哪个现有模块
