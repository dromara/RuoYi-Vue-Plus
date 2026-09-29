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
使用 $ruoyi-plus-ai-coding 为 monitor/cache 新增一个导出接口，并同步补齐 Vue 或 React 前端 api/types 调用骨架。
```

### 期望执行方式

- 先补后端 `controller/service`。
- 再根据后端路由和目标前端类型补 `src/api` 或 generator 风格的前端骨架。
- 保证导出接口路径和前端下载调用一致。

## 案例 6：新增 grantType 认证策略

### 用户提问示例

```text
使用 $ruoyi-plus-ai-coding 新增一个 grantType=sms 的 IAuthStrategy 认证策略，用短信验证码登录。
参考现有 PasswordAuthStrategy 写法，登录失败次数走 SysLoginService.checkLogin，
登录成功走 LoginHelper.login，客户端走 ISysClientService 校验。
```

### 期望执行方式

- 先找到 `*AuthStrategy` 实现类（密码/社交/客户端已有示例）。
- 新建 `SmsAuthStrategy`，Bean 名 `smsAuthStrategy`（IAuthStrategy 按 grantType+AuthStrategy 路由）。
- 实现 `login(body, client, grantType)`：先校验 client → 校验短信验证码 → 查用户 → checkLogin → buildLoginUser → LoginHelper.login → 返回 LoginVo。

## 案例 7：新增 SnailJob 任务执行器

### 用户提问示例

```text
使用 $ruoyi-plus-ai-coding 在 ruoyi-job 模块新增一个静态分片任务，按日期分 8 片汇总账单。
参考 TestStaticShardingJob，使用 @JobExecutor 注解。
```

### 期望执行方式

- 先读 `ruoyi-job/src/main/java/.../jobtask/TestStaticShardingJob`。
- 新建 `SummaryDailyStaticShardingTask`，加 `@JobExecutor(taskName = "SummaryDailyStaticShardingTask")`。
- 实现分片逻辑：`shardingArgs` 按日期分片，`execute` 处理分片并写入汇总表。
- 幂等保护：同一日期+分片已处理过直接跳过。

## 案例 8：新增 MCP Server Tool

### 用户提问示例

```text
使用 $ruoyi-plus-ai-coding 新增一个 MCP Server Tool，用于获取当前登录用户的在线设备列表。
参考 McpDemoServerTool 的 @McpTool 写法，返回用户名、IP、登录时间、设备类型。
```

### 期望执行方式

- 先读 `McpDemoServerTool` 了解 `@McpTool`/`@McpResource` 注解和组件风格。
- 新建 `UserOnlineMcpTool`（ruoyi-demo 或对应模块），加 `@Component`。
- 方法加 `@McpTool(name = "getOnlineUsers", description = "获取当前在线用户列表")`，内部查 SysUserOnline 或 Redis 在线集合。
- 如需 Client 侧调用示例，补充 `McpClientService` 调用代码片段。

## 案例 9：新增工作流扩展（办理人解析 + 监听）

### 用户提问示例

```text
使用 $ruoyi-plus-ai-coding 为 ruoyi-workflow 模块新增"部门负责人链路"办理人解析能力：
在 FlowParams 传部门ID，启动时自动取部门负责人作为下一节点办理人；
并加一个流程结束监听器把归档消息推送到消息中心 SysMessage。
参考 WorkflowPermissionHandler 和 WorkflowSideEffectListener。
```

### 期望执行方式

- 先读 `WorkflowPermissionHandler.convertPermissions()` 和 `IFlwTaskAssigneeService`。
- 在办理人解析入口增加部门ID存储格式识别（如 `DEPT:123`），取 `ISysDeptService` 查负责人→用户ID。
- 新增 `FlowArchiveMessageListener`，监听流程 finish 事件，用 `ISysMessageService` 发消息到启动人。
- 新增 Bean 检查是否需要 `@ConditionalOnEnable`，避免 warm-flow.enabled=false 启动失败。

## 案例 10：新增缓存 + 事件发布

### 用户提问示例

```text
使用 $ruoyi-plus-ai-coding 为 system 模块的字典类型新增"停用/启用"接口：
1. 修改状态后同步失效 SYS_DICT + SYS_DICT_TYPE 两个缓存
2. 发布一个 DictTypeChangeEvent 事件，其他模块可以监听
3. 参考 OssConfigChangeEvent + OssConfigChangeListener 的事件模型
```

### 期望执行方式

- 事件类用 Java 21 record：`public record DictTypeChangeEvent(Long dictId, String status) {}`
- 接口 `@PostMapping("/changeStatus")`：写 DB → `@CacheEvict(cacheNames = {SYS_DICT, SYS_DICT_TYPE}, allEntries = true)` → `SpringUtils.context().publishEvent(new DictTypeChangeEvent(...))`
- 在需要刷新的地方加监听器：`@TransactionalEventListener(AFTER_COMMIT) public void onDictChange(DictTypeChangeEvent e) { ... }`

## 案例 11：推荐的高质量任务描述

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
