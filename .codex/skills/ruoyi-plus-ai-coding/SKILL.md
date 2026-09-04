---
name: ruoyi-plus-ai-coding
description: 在仓库内按代码生成器模板、项目 reference 文档和既有约定生成或修改代码。用于新增或修改 CRUD 模块、controller/service/mapper/BO/VO/entity、MyBatis-Plus/MPJ 查询、数据权限、缓存、翻译/JSON 增强、公共 common 模块能力、JavaDoc 注释，以及与后端接口配套的 Vue 或 React 前端页面、types 和 api 文件；触发后应先按任务类型读取对应 references，再阅读目标模块真实代码和 generator 模板。
---

# RuoYi Plus AI 编码规范

先对齐代码生成器产物，再叠加仓库里真实业务代码已经形成的更强约定。

## 适用场景

在下面这些任务里优先使用此 skill：

- 新增标准 CRUD 模块。
- 根据新表结构补齐 entity、bo、vo、mapper、service、controller。
- 修改已有模块的查询、校验、导入导出、数据权限、事务逻辑。
- 修改 `ruoyi-common` 公共能力，例如 mybatis 查询构造器、translation、json enhance、excel、oss、redis、web、encrypt、satoken 配置。
- 补充或修正 JavaDoc 注释，尤其是公共 API、接口、BO/VO/Entity 字段、Mapper 默认方法、Service/Controller 方法。
- 在系统、监控、工作流、demo、job、ai 等模块内按现有约定扩展业务代码。
- 为后端新增接口同步补前端 `api/types` 和 Vue `index.vue` 或 React `index.tsx` 页面骨架。
- 扩展认证授权策略（新增 grantType、OAuth2 客户端、三方社交登录绑定）。
- 扩展工作流编排（新增 Warm-Flow 节点、LiteFlow 链路、办理人解析、流程事件监听）。
- 基础设施扩展（SnailJob 任务执行器、MCP Server/Client Tool、Spring 事件发布/监听、启动钩子 Runner）。

## 不适用场景

下面这些任务不要机械套用本 skill 的 CRUD 规则：

- 基础框架升级、Spring Boot 主版本迁移、JDK 大版本升级。
- 与当前分层明显不同的实验性模块。
- 第三方中间件深度接入、基础设施底层改造（如替换 ORM、替换缓存中间件）。
- 完全脱离 generator 体系的独立子系统。
- 前端工程独立目录（plus-ui / plus-ui-react）的框架级改造。

---

## 项目架构速览

**一级模块拓扑（Maven 多模块）**：

```
ruoyi-vue-plus (root, pom)
├── ruoyi-admin          # Web 启动入口：认证 Controller、验证码、登录、启动类
├── ruoyi-api            # 跨模块契约层：接口 + DTO，common 层唯一对外依赖
├── ruoyi-common         # 通用能力层：25 个子模块，BOM 统一版本
├── ruoyi-modules        # 业务模块：system / gen / job / demo / workflow / ai
└── ruoyi-extend         # 扩展独立应用：monitor-admin / snailjob-server / snailai-server
```

**分层依赖方向**：`admin → modules → api → common-core ← common-*`。业务模块之间禁止直接依赖，必须通过 `ruoyi-api` 暴露接口。

**25 个 common 子模块速查**：

| 子模块 | 核心能力 |
|--------|----------|
| `common-core` | 基础常量、异常 `ServiceException`、`R<T>`、`BaseEntity`、工具类包占位 |
| `common-json` | Jackson 序列化、`JsonFieldProcessor` 三阶段、翻译/脱敏处理器 |
| `common-redis` | `RedisUtils`、`CacheUtils`、`QueueUtils`、Redisson 配置 |
| `common-satoken` | Sa-Token 配置、`LoginHelper`、权限工具、Redis 二级缓存 |
| `common-security` | 路由拦截、`@SaCheckPermission`、黑白名单 |
| `common-mybatis` | `BaseMapperPlus`、`QueryBuilder`、`LambdaCrudChainWrapper`、`@DataPermission`、乐观锁、逻辑删除、自动填充 |
| `common-web` | Jetty 容器、`BaseController`、全局异常、`@RepeatSubmit`、`@RateLimiter`、SSE/WebSocket 推送 |
| `common-log` | `@Log` 注解、操作日志、登录日志事件发布 |
| `common-oss` | OSS 抽象、S3 SDK v2、MinIO/阿里/腾讯/七牛适配、`ISysOssConfigService` |
| `common-excel` | Fesod（EasyExcel 孵化版）、`ExcelListener`、`@ExcelProperty`/`@ExcelDictFormat` |
| `common-encrypt` | 字段加解密 `@EncryptField`、API 传输加密 `@ApiEncrypt`/`CryptoFilter`（AES+RSA） |
| `common-sensitive` | `@Sensitive` 脱敏策略、按角色/权限脱敏开关 |
| `common-translation` | `@Translation`、`TranslationInterface`、`translationBatch` 批量翻译 |
| `common-idempotent` | `@Idempotent` 幂等注解 |
| `common-duplicate` | 防重复提交内部实现 |
| `common-tenant` | 租户隔离支持 |
| `common-datascope` | `@DataPermission` 内部实现 |
| `common-bom` | 依赖版本管理（BOM） |
| `common-i18n` | 国际化支持 |
| `common-swagger` | SpringDoc + therapi-javadoc 零注解文档 |
| `common-mail` | Jakarta Mail（Angus）邮件发送 |
| `common-sms` | sms4j 多厂商短信 |
| `common-ratelimiter` | `@RateLimiter` 令牌桶限流（IP/CLUSTER/DEFAULT） |
| `common-push` | WebSocket 推送、`@McpSampling`/`@McpElicitation` 反向回调握手 |
| `common-mapstruct` | `MapstructUtils`、`@AutoMapper` 对象映射 |

**6 个业务模块速查**：

| 模块 | 核心职责 | 优先参考文件 |
|------|----------|--------------|
| `ruoyi-system` | 用户/角色/部门/菜单/字典/岗位/通知/OSS/客户端/社交/消息/日志 | `ISysUserService`、`SysLoginService`、`SysPermissionHandler` |
| `ruoyi-gen` | 代码生成器：表元数据→FreeMarker→Java/Vue/React/SQL/XML | `GenUtils`、`TemplateEngineUtils`、`fm/java/*.ftl` |
| `ruoyi-job` | SnailJob 任务示例：9 个 `@JobExecutor` 覆盖所有调度模式 | `AlipayBillTask`（DAG）、`TestBroadcastJob`（广播） |
| `ruoyi-demo` | 19+ 全功能教学示例：ES/邮件/MCP/MQTT/Redis/Sa-Token/SMS/Excel/i18n/脱敏/树表/WebSocket | `McpDemoServerTool`、`McpDemoClientService` |
| `ruoyi-workflow` | Warm-Flow + LiteFlow：6 Controller/11 Service/12 `@LiteflowComponent` | `WorkflowGlobalListener`、`FlwTaskController`、`task-chain.el.xml` |
| `ruoyi-ai` | Snail AI OpenAPI 注册入口：`/snail-ai/user/register` | `SnailAiController` |

## 执行流程

1. 先判断任务类型，并按“文档读取规则”读取当前任务需要的 reference。
2. 确认目标模块，优先复用同模块中最近似功能的写法。
3. 新增标准 CRUD 代码前，先读取 `ruoyi-modules/ruoyi-gen/src/main/resources/fm/` 下的 FreeMarker 模板。
4. 命名和分层保持与仓库一致：
   `domain` entity、`domain.bo`、`domain.vo`、`mapper`、`service`、`service.impl`、`controller`。
5. 优先在生成器结构上扩展，不要自行发明新的分层。
6. 修改 `ruoyi-system` 这类复杂模块前，先阅读同类现有实现，因为这些模块通常比生成器默认产物多出数据权限、联表、缓存、安全校验等逻辑。
7. 修改 `ruoyi-common` 公共模块前，先阅读同包接口、实现类和调用点，优先保持已有 API 语义与兼容性。
8. 只补注释或文档时，不运行无关格式化，不重排 import，不改代码逻辑。

## 文档读取规则

使用本 skill 时，先按任务类型读取适用 reference，不一次性展开所有文档：

- 后端 Java、Mapper、Service、Controller、BO、VO、Entity、权限、查询、公共模块或 JavaDoc 任务，先读 [references/backend.md](references/backend.md)。
- 前端 Vue、React、TypeScript、api、types 或页面任务，先读 [references/frontend.md](references/frontend.md)。
- 不确定任务边界、需要标准调用方式或需要对照典型场景时，再读 [references/examples.md](references/examples.md)。

reference 用来约束实现方式和自检范围；发生冲突时，仍以当前模块真实代码和实际调用点为准。

## 优先级规则

发生冲突时按下面顺序决策：

1. 当前模块内最近似业务代码。
2. 当前仓库公共基础模块约定，例如 `common-mybatis`、`common-core`、`common-web`。
3. 代码生成器模板。
4. 通用 Spring Boot / MyBatis-Plus 习惯。

也就是说：

- 同模块已有成熟实现时，优先复用该实现。
- 同模块没有现成代码时，再参考 generator 模板。
- 不要因为“更通用”就覆盖掉项目已形成的强约定。

## 后端规则

Java、MyBatis-Plus、BO/VO/entity、controller、mapper、service 的具体规则见 [references/backend.md](references/backend.md)。

## 前端规则

Vue 3、React、TypeScript API 文件、生成式列表页、表单状态、字典和日期范围约定见 [references/frontend.md](references/frontend.md)。

## 使用案例

具体调用方式见 [references/examples.md](references/examples.md)。

## 仓库通用规则

- 遵循 [`.editorconfig`](../../../.editorconfig)：UTF-8、LF，默认 4 空格，JSON/YAML 为 2 空格。
- 不要把 `BaseMapperPlus`、`PageQuery`、`PageResult`、`R`、`MapstructUtils` 或项目工具类替换成临时自造方案。
- 仓库已使用 `List.of(...)` 的地方，数组转列表优先继续沿用。
- import、注解顺序、文件结构以附近代码为准，不要顺手重排整个文件。
- 只有在业务逻辑不直观时才加简短注释。

## 决策规则

- 如果任务是围绕单表的标准 CRUD，尽量贴近生成器默认产物。
- 如果目标模块已经存在自定义校验、数据权限、事务、缓存、Excel 导入导出、联表查询等逻辑，应在此基础上扩展，不要为了“简洁”把它们削平。
- 如果附近 controller 接口已经带有权限、日志、防重、加密、分组校验等注解，新接口默认同步保持一致，除非有明确理由不这样做。
- 如果 BO 或 VO 需要字段校验、翻译、Excel 注解，应优先参考同模块同用途对象，不要机械套通用注解。
- 如果修改公共基础模块，优先保持公开 API 兼容，新增能力要查调用点和同包风格。
- 如果任务只涉及注释，默认补 JavaDoc 并保持实现不变；框架覆写方法不强行重复注释，除非业务语义不直观。

## 目录映射规则

标准后端模块通常按下面结构组织：

- `src/main/java/.../domain/Entity.java`
- `src/main/java/.../domain/bo/EntityBo.java`
- `src/main/java/.../domain/vo/EntityVo.java`
- `src/main/java/.../mapper/EntityMapper.java`
- `src/main/java/.../service/IEntityService.java`
- `src/main/java/.../service/impl/EntityServiceImpl.java`
- `src/main/java/.../controller/EntityController.java`

标准生成器模板通常对应：

- `fm/java/domain.java.ftl` -> entity
- `fm/java/bo.java.ftl` -> bo
- `fm/java/vo.java.ftl` -> vo
- `fm/java/mapper.java.ftl` -> mapper
- `fm/java/service.java.ftl` -> service interface
- `fm/java/serviceImpl.java.ftl` -> service impl
- `fm/java/controller.java.ftl` -> controller
- `fm/xml/mapper.xml.ftl` -> 自定义 XML mapper 起点

## 任务分型

### 1. 标准单表 CRUD

优先按 generator 模板落骨架，再补校验、权限、导出、翻译等项目约定。

### 2. 强业务模块扩展

如果目标模块像 `system`、`workflow` 一样已经有复杂逻辑，优先增量修改，不要回退成模板式简化代码。

### 3. 基础能力复用

如果涉及数据权限、缓存、事务、导入导出、字典、翻译、加密、分组校验，优先查项目已有做法并复用公共能力。

### 4. 公共基础模块修改

修改 `ruoyi-common` 下的基础能力时，优先保证二进制/API 兼容：不要轻易改公开方法签名、泛型、返回值或异常语义。新增注释和小范围能力时，先查同包现有风格，例如 `common-mybatis` 的链式 wrapper、`common-translation` 的 `TranslationInterface` 实现、`common-json` 的字段处理器。

### 5. 认证授权策略扩展

**适用**：新增 `grantType`（如 password/sms/wechat/email）、OAuth2 `SysClient` 配置、`SysSocial` 三方绑定、`IAuthStrategy` 实现、`LoginHelper` 登录上下文扩展。
**关键类**：`IAuthStrategy`（策略分发 `grantType + "AuthStrategy"`）、`LoginHelper`（`login()`/`getLoginUser()`/`isSuperAdmin()`）、`SysLoginService`（`checkLogin()` 失败次数锁定、`buildLoginUser()` 虚拟线程并行加载）、`ISysClientService`（客户端校验）、`ISysSocialService`（三方关系）。
**约定**：策略 Bean 名 = `grantType + AuthStrategy`；登录参数统一用 `IAuthStrategy.buildLoginParameter(SysClientVo, customizer)`；失败次数默认 Redis 5 次/10 分钟；三方绑定用 `@Lock4j` 分布式锁。

### 6. 工作流编排扩展

**适用**：新增 Warm-Flow 节点/监听器、LiteFlow 链路定义、办理人解析器、流程抄送/消息通知扩展。
**关键类**：`WorkflowGlobalListener`（start/assignment/finish 四回调 + `FLOW_COPY_LIST`）、`WorkflowPermissionHandler`（`permissions()` + `convertPermissions()` 角色/部门→用户ID）、`@ConditionalOnEnable`（`warm-flow.enabled` 条件装配）、`task-chain.el.xml`（`startProcessChain`/`completeTaskChain`/`taskOperationChain`）。
**约定**：条件装配默认带 `@ConditionalOnProperty("warm-flow.enabled")`；办理人存储 ID 转用户 ID 走 `IFlwTaskAssigneeService.fetchUsersByStorageIds`；流程事件后发布内部事件，抄送/通知由 `WorkflowSideEffectListener` 消费；新增 LiteFlow 组件用 `@LiteflowComponent`，链路定义追加到 `task-chain.el.xml`/`instance-chain.el.xml`。

### 7. 基础设施与事件/任务

**适用**：SnailJob `@JobExecutor` 任务、MCP `@McpTool`/`@McpResource` Server Tool 或 Client 调用、Spring `ApplicationEvent` + `@TransactionalEventListener`、`ApplicationRunner` 启动钩子、WebSocket/MQTT/SSE 接入。
**关键类**：`@JobExecutor`（SnailJob 9 种模式：注解/类继承/广播/Map/MapReduce/静态分片/DAG）、`McpDemoServerTool`/`McpDemoClientService`/`McpDemoClientHandlers`（MCP 双向演示）、`OnlineUserCleanEvent`→`OnlineUserCleanListener`（`@Async`）、`OssConfigChangeEvent`→`OssConfigChangeListener`（缓存刷新）、`SystemApplicationRunner`（初始化 OSS 配置缓存）。
**约定**：事件用 Java 21 record + `@TransactionalEventListener(phase = AFTER_COMMIT)`；异步监听加 `@Async`；启动钩子放 `runner/` 包；SnailJob 任务优先参考 `ruoyi-job` 对应模式；MCP Server Tool 用 `@McpTool`/`@McpResource`，Client 端用 `McpClientService`；Server 反向请求 Client 用 `@McpSampling`/`@McpElicitation`。

### 8. 注释修正任务

只要求"加注释/完善注释"时，默认补 JavaDoc，不改实现。优先补公共 API、接口方法、字段含义、复杂私有辅助方法；覆写框架回调方法只有在当前文件已有注释风格或业务语义不直观时才补。

## 输出要求

使用本 skill 时，默认期望产出应满足：

- 后端分层完整，不直接在 controller 里堆业务逻辑。
- `BO/VO/Entity` 职责分明。
- 查询、分页、删除校验、写入校验逻辑闭环完整。
- 权限、日志、防重、事务、数据权限尽量贴近同模块现有实现。
- 如果同步改前端，前端 API 路径和后端接口保持一致。

## 快速检查清单

- 包路径和 `@RequestMapping` 与模块保持一致。
- 权限标识遵循 `${module}:${business}:${action}`。
- Mapper 继承 `BaseMapperPlus<Entity, Vo>`。
- 手写 Service 注入 Mapper 时使用具体业务短名；代码生成器模板按类名首字母小写命名，例如 `SysRoleMapper` 生成 `sysRoleMapper`。
- Service 按场景返回 `PageResult` 或 `List<Vo>`。
- 查询代码优先使用 `LambdaQueryWrapper`，复杂模块沿用既有 MPJ 联表风格。
- 公共 Mapper 链式能力优先沿用 `LambdaCrudChainWrapper`、`LambdaQueryBuilder`、`LambdaQueryCondition` 的 `IfPresent` / `IfText` / `IfNotEmpty` 风格。
- 翻译能力优先沿用 `TranslationInterface` + `@TranslationType` + `@Translation`，批量翻译实现 `translationBatch`，避免退化成逐条查询。
- JSON 响应增强优先沿用 `JsonFieldProcessor` 的 `collect` / `prepare` / `process` 三阶段模型。
- BO 使用 `@AutoMapper(target = Entity.class, reverseConvertGenerate = false)`。
- VO 使用 `@AutoMapper(target = Entity.class)`。
- 前端 API 路径与后端路由完全对应。
- 前端列表页继续使用对应前端工程已有工具：Vue 侧如 `proxy?.addDateRange`、`proxy?.$modal`、`proxy?.download`、`useDict`、`pagination`；React 侧如 `ProTable`、`ModalForm`、`useTableSelection`、`useDateRangeQuery`、`useTableExport`。
- 跨模块调用必须走 `ruoyi-api`，业务模块之间禁止直接引入依赖。
- 新增认证策略 Bean 名必须是 `{grantType}AuthStrategy`，否则 `IAuthStrategy` 路由失败。
- 工作流 Bean 新增时检查 `@ConditionalOnEnable`，避免 `warm-flow.enabled=false` 时启动报错。
- 事件监听使用 `@TransactionalEventListener(phase = AFTER_COMMIT)`，异步加 `@Async`，不要在事务内触发副作用。
- OSS/字典/部门等写操作同步维护缓存失效（`@CacheEvict` 或 `CacheUtils.evict`），不要只改数据库。
- 限流/幂等/防重注解按近邻接口保持一致，不要给新接口漏掉。
- 自动填充审计字段由 `InjectionMetaObjectHandler` 处理，不要手动 set createBy/updateBy/createTime/updateTime/createDept。

## 推荐提问方式

推荐把任务描述到下面这个粒度：

- 目标模块和业务名
- 是新建模块还是修改已有模块
- 表名或接口前缀
- 是否需要分页、导出、导入、数据权限、字典、翻译、联表
- 希望参考哪个现有模块

例如：

- 使用 `$ruoyi-plus-ai-coding` 在 `system` 模块新增一个标准单表 CRUD，参考 `SysConfig` 与 generator 模板。
- 使用 `$ruoyi-plus-ai-coding` 修改 `workflow/category` 的查询和导出逻辑，保持现有模块风格。
- 使用 `$ruoyi-plus-ai-coding` 新增一个 grantType=sms 的 IAuthStrategy 策略，参考现有 PasswordAuthStrategy。
- 使用 `$ruoyi-plus-ai-coding` 为 ruoyi-job 模块新增一个广播模式 SnailJob 任务执行器，参考 TestBroadcastJob。
- 使用 `$ruoyi-plus-ai-coding` 新增一个 MCP Server Tool，参考 McpDemoServerTool 写法。
- 使用 `$ruoyi-plus-ai-coding` 新增流程抄送扩展逻辑，参考 WorkflowGlobalListener 和 WorkflowSideEffectListener。
