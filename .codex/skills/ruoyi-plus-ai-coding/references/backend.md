# 后端约定

## 优先参考的代码来源

- `ruoyi-modules/ruoyi-gen/src/main/resources/fm/java/*.ftl`
- `ruoyi-modules/ruoyi-demo/...`
- `ruoyi-modules/ruoyi-system/...`
- `ruoyi-modules/ruoyi-workflow/...`
- `ruoyi-common/ruoyi-common-mybatis/...`

## 决策顺序

写代码时按下面顺序取样：

1. 当前业务模块下最近似实现。
2. 当前仓库公共能力模块中的统一约定。
3. generator 模板。
4. 通用 Spring / MyBatis-Plus 默认习惯。

如果规则冲突，优先相信当前仓库真实代码。

## 分层结构

标准 CRUD 代码应优先遵循下面这套结构：

- `domain/Entity.java`
- `domain/bo/EntityBo.java`
- `domain/vo/EntityVo.java`
- `mapper/EntityMapper.java`
- `service/IEntityService.java`
- `service/impl/EntityServiceImpl.java`
- `controller/EntityController.java`

## Entity 规则

- 除非所在模块明显另有约定，否则实体类继承 `org.dromara.common.mybatis.core.domain.BaseEntity`。
- 使用 Lombok `@Data` 和 `@EqualsAndHashCode(callSuper = true)`。
- 使用 `@TableName("table_name")`。
- 主键使用 `@TableId`。
- 存在 `delFlag` 时保留 `@TableLogic`，存在乐观锁字段时保留 `@Version`。
- 如果附近实体已经使用 `@OrderBy` 等额外注解，应继续保持。

## BO 规则

- 实现 `Serializable`。
- 添加 `@AutoMapper(target = Entity.class, reverseConvertGenerate = false)`。
- 请求专用字段、查询专用字段放在 BO 中，包括 `params`。
- 在生成器或附近代码已有分组校验时，继续使用：`AddGroup`、`EditGroup`、`QueryGroup`。
- `@Xss`、`@Email`、`@Size`、`@NotBlank`、`@NotNull` 要按真实业务语义添加，不要一股脑全套上。
- 查询存在日期范围或扩展条件时，保留 `params = new HashMap<>()`。

## VO 规则

- 实现 `Serializable`。
- 添加 `@AutoMapper(target = Entity.class)`。
- 生成器风格的导出对象通常带 `@ExcelIgnoreUnannotated`。
- `@ExcelProperty`、`@ExcelDictFormat`、`ExcelDictConvert`、`@ExcelRequired`、`@ExcelNotation`、`@DateTimeFormat` 只在导入导出场景下使用。
- 如果附近代码会把 ID 翻译成展示字段，沿用 `@Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "createBy")` 这类写法。
- 展示型派生字段放在 VO，不放在 Entity。

## Mapper 规则

- 默认形式是 `interface XxxMapper extends BaseMapperPlus<Xxx, XxxVo>`。
- 不要为简单的 entity 转 vo 手写重复代码，优先依赖 `BaseMapperPlus`。
- 模块已经使用 `@DataPermission` 时，在重写方法和自定义查询上继续保留。
- 复杂模块里 mapper 可能同时继承 `MPJBaseMapper<Entity>` 并使用 `QueryBuilder.lambdaJoin(...)` 构造 MPJ 查询，遇到这种风格要延续，不要换一种写法。
- 只有在 `selectVoList/selectVoPage` 不够用时，才补 XML 或自定义 mapper 方法。
- Mapper 默认方法可以承载短小的 wrapper 查询；涉及复杂业务编排、缓存、事务或跨 mapper 写入时放到 service。
- `ruoyi-system` 的用户、角色、菜单、部门等模块常带数据权限、MPJ 联表、角色状态过滤，修改前先读对应 mapper/service。

### Mapper 建议结构

标准 mapper 一般按这个顺序组织：

1. 接口声明
2. 默认查询方法
3. 自定义分页或列表方法
4. 特殊数据权限重写
5. 辅助构造方法

### 什么时候需要 XML

- 复杂联表 SQL 无法仅靠 wrapper 清晰表达时。
- 需要手写查询列和结果映射时。
- 项目当前模块已经大量使用 XML 时。

如果 `BaseMapperPlus + wrapper` 已足够，优先不要补 XML。

## Service 规则

- 类声明通常是 `@RequiredArgsConstructor`、`@Service`，按需补 `@Slf4j`。
- 手写 mapper 注入字段使用具体业务短名；代码生成器模板按类名首字母小写命名。
- 命名时去掉清晰的模块/系统前缀后使用 lowerCamel + `Mapper`，例如 `SysRoleMapper` -> `roleMapper`、`SysDictDataMapper` -> `dictDataMapper`。
- 如果去掉前缀会产生歧义或命名冲突，保留必要前缀。
- 读操作通常返回 `Vo`、`List<Vo>` 或 `PageResult<Vo>`。
- BO 转实体用 `MapstructUtils.convert(bo, Entity.class)`。
- 查询条件优先返回 `LambdaQueryWrapper`；新增 generator 风格代码优先用 `QueryBuilder.lambda(Entity.class)`，老模块已有 `Wrappers.lambdaQuery()` 时可继续保持。
- 字符串和空值条件优先用 `eqIfText`、`likeIfText`、`eqIfPresent`、`inIfNotEmpty`、`betweenParams` 等项目扩展；老代码已有直接 `StringUtils.isNotBlank(...)` 和 null 判断时可增量保持。
- 分页查询优先采用：
  `Page<Vo> result = entityMapper.selectVoPage(pageQuery.build(), lqw);`
  `return PageResult.build(result.getRecords(), result.getTotal());`
- 生成器风格模块保留 `validEntityBeforeSave(...)` 这种扩展点。
- 多表写操作使用 `@Transactional(rollbackFor = Exception.class)`。
- 明确的业务失败，尤其是权限、数据完整性、删除校验，使用 `ServiceException`。
- 不要绕过模块现有的数据权限、角色校验、删除前校验。

### Service 建议结构

标准 service impl 一般按下面顺序组织：

1. 查询单条
2. 分页查询
3. 列表查询
4. 构建查询条件
5. 新增
6. 修改
7. 保存前校验
8. 删除前校验与删除
9. 其他扩展业务方法

### 查询逻辑建议

- 单表查询优先返回 `LambdaQueryWrapper`，生成器风格优先通过 `QueryBuilder.lambda(Entity.class).build()` 构造。
- 条件判断直接放在 wrapper 链式条件上，不要额外写大量 if 套壳。
- 日期范围统一从 `bo.getParams()` 取 begin/end；生成器默认使用 `betweenParams(Entity::getField, params, "beginField", "endField")`。
- 复杂联表查询优先查同模块是否已有 MPJ 风格可复用；新写法优先用 `QueryBuilder.lambdaJoin("u", Entity.class)`。

### 写入逻辑建议

- BO 转实体统一走 `MapstructUtils.convert`。
- 批量关系维护时优先拆成私有方法，例如角色、岗位、用户关联。
- 修改前优先保留已有防误删、防越权、防并发覆盖逻辑。

## Controller 规则

- 继承 `BaseController`。
- 类上通常带 `@Validated`、`@RestController`、`@RequiredArgsConstructor`、`@RequestMapping`。
- 返回值使用 `R<T>` 或 `R<Void>`。
- 标准 CRUD 接口通常是：`GET /list`、`POST /export`、`GET /{id}`、`POST`、`PUT`、`DELETE /{ids}`。
- 树表接口通常不分页，`list` 返回 `R<List<Vo>>`；导出路由以目标模块或 generator 模板为准，旧 demo 树表存在 `GET /export`，新版生成器通常是 `POST /export`。
- `@SaCheckPermission` 权限格式遵循 `${module}:${business}:${action}`。
- 写操作、导入导出接口通常加 `@Log(title = "...", businessType = BusinessType.X)`。
- 附近接口已有防重时，写接口继续使用 `@RepeatSubmit`。
- 适合分组校验时，使用 `@Validated(AddGroup.class)` 和 `@Validated(EditGroup.class)`。
- 特殊接口直接复用模块内现成做法，例如导入导出、`@ApiEncrypt`、multipart 上传、数据权限检查、写入前唯一性校验。

### Controller 建议结构

标准 controller 一般按下面顺序组织：

1. 列表
2. 导出
3. 详情
4. 新增
5. 修改
6. 删除
7. 特殊接口

### Controller 边界

- controller 负责接参、校验、权限、日志、返回值转换。
- 重业务逻辑尽量放 service，不要在 controller 里堆长逻辑。
- 但前置权限检查、唯一性提示、显式业务失败提示可以留在 controller，前提是同模块已有这种习惯。

## 查询与工具规则

- 分页统一使用 `PageQuery` 和 `PageResult`，不要无故引入新的分页 DTO。
- 优先使用项目工具类：`MapstructUtils`、`StringUtils`、`StreamUtils`、`ValidatorUtils`、`SpringUtils`、`RedisUtils`。
- 数组转列表按附近代码习惯使用 `List.of(ids)` 或 `Arrays.asList(ids)`。
- 日期范围查询通常从 `bo.getParams()` 中读取 `beginTime`、`endTime` 或 `beginFieldName`、`endFieldName`。
- 构建查询优先识别 `QueryBuilder.lambda(...)`、`QueryBuilder.lambdaJoin(...)`、`BaseMapperPlus#lambda()` 三类入口，不要退回临时手写 SQL 或自造 wrapper。

## common-mybatis 规则

- 链式查询能力优先沿用 `QueryBuilder.lambda(...)`、`QueryBuilder.lambdaJoin(...)`、`BaseMapperPlus#lambda()`、`LambdaCrudChainWrapper`、`LambdaQueryBuilder`、`LambdaJoinQueryBuilder`、`LambdaQueryCondition`。
- 条件辅助方法使用项目已有命名：`eqIfPresent`、`eqIfText`、`neIfPresent`、`likeIfText`、`betweenIfPresent`、`betweenParams`、`inIfNotEmpty`、`findInSetIfPresent`。
- 新增 wrapper 方法时保持链式返回 `this` / `typedThis`，不要返回底层 `LambdaQueryWrapper` 破坏调用链。
- `LambdaCrudChainWrapper` 既承担查询又承担更新 set 片段，新增能力时要同时考虑 `getSqlSelect`、`getSqlSet`、`clear`、`instance` 的状态复制和清理。
- MPJ 联表查询沿用别名风格，例如 `QueryBuilder.lambdaJoin("u", SysUser.class)`、`.leftJoin(..., "d", ...)`、`.eq("u", Entity::getField, value)`。
- 数据权限注解使用 `@DataPermission` + `@DataColumn`，列名需和实际 SQL 别名一致，例如 `d.dept_id`、`u.create_by`。

## translation / JSON 增强规则

- 翻译实现类实现 `TranslationInterface<T>` 并标注 `@TranslationType(type = ...)`。
- 使用方在 VO 字段上通过 `@Translation(type = ..., mapper = "...", other = "...")` 指定翻译来源。
- 批量翻译必须优先实现 `translationBatch(Set<Object> keys, String other)`，避免默认逐条查询。
- 支持逗号分隔 ID 的翻译实现应复用 `collectLongIds`、`parseLongIds`、`joinMappedValues`。
- `TranslationJsonFieldProcessor` 遵循三阶段：`collect` 收集待翻译值，`prepare` 批量查询，`process` 写入翻译结果；新增处理器也应优先套这个模型。
- 翻译失败时保持降级返回原值或 `null` 的现有语义，不要让响应增强中断主流程。

## 缓存与异步/监听规则

- 已有 service 使用 `@Cacheable`、`@CachePut`、`@CacheEvict`、`@Caching` 或 `CacheUtils.evict/clear` 时，新增写操作要同步考虑缓存失效。
- 部门、字典、OSS 配置等模块已有缓存初始化或失效逻辑，不要只改数据库不处理缓存；字典这类模块常同时维护 `CacheNames.SYS_DICT` 与 `CacheNames.SYS_DICT_TYPE`。
- Excel 导入监听器实现 `ExcelListener` 时，保留 `getExcelResult()` 的回执语义和错误聚合方式。
- 定时任务、MQTT、SSE、异步回调等框架方法一般按接口覆写语义实现，除非业务不直观，不要添加冗长注释。

## 认证授权核心规则

### IAuthStrategy 策略模式

- 实现类 Bean 名必须为 `{grantType}AuthStrategy`，例如 `passwordAuthStrategy`、`smsAuthStrategy`；`IAuthStrategy.login(body, client, grantType)` 按此名称从 `SpringUtils` 取 Bean。
- 登录参数统一调用 `IAuthStrategy.buildLoginParameter(SysClientVo client, Consumer<SaLoginParameter> customizer)`，不要手动构造 `SaLoginParameter`。
- 策略内部第一步先通过 `ISysClientService` 校验客户端（clientId + clientSecret + grantType 白名单）。

### LoginHelper 上下文

- 取当前登录上下文统一用 `LoginHelper.getLoginUser()`/`getUserId()`/`getUsername()`/`getDeptId()`/`isSuperAdmin()`/`isLogin()`，不要直接调 Sa-Token `StpUtil`。
- 主动登录用 `LoginHelper.login(LoginUser user, SaLoginParameter param)`；框架会自动填充 IP/地理/浏览器/OS/设备类型到登录日志。
- 核心常量：`LOGIN_USER_KEY`、`USER_KEY`、`CLIENT_KEY`、`CLIENT_ACCESS_PATH_KEY`、`CLIENT_IP_WHITELIST_KEY` 如需自定义检查，通过常量读取。

### SysLoginService 校验与加载

- 失败次数限制走 `checkLogin(LoginType, username, Supplier<Boolean>)`，Redis 计数；默认 5 次锁定 10 分钟（可在配置中调整 `user.password.maxRetryCount/lockTime`）。
- 构建登录对象走 `buildLoginUser(SysUserVo)`，内部用虚拟线程并行加载菜单/角色/数据范围/岗位，不要自行串行查询。
- 登录记录发布 `recordLoginInfo(username, status, message)` → `LoginInfoEvent`，不要直接写登录日志表。
- 三方社交绑定用 `socialRegister(AuthUser)`，已加 `@Lock4j` 分布式锁保护；新增三方登录继续沿用此方法或加等价锁。

## 数据访问与自动填充

- 审计字段（createBy/createTime/updateBy/updateTime/createDept）由 `InjectionMetaObjectHandler` 自动填充，从 `LoginHelper` 取当前用户，未登录时填 -1；业务代码不要手动 set。
- `MybatisPlusConfig` 拦截器链固定顺序：`PlusDataPermissionInterceptor → PaginationInnerInterceptor → OptimisticLockerInnerInterceptor`，不要插入或调整顺序。
- 主键默认雪花 ID，由 `IdentifierGenerator`（基于网卡 IP）生成；非必要不要改 `mybatis-plus.idType`。
- 数据权限临时绕过使用 `DataPermissionHelper.ignore(() -> { ... })`，例如更新登录 IP、更新缓存重建等系统级操作。

## 缓存与限流核心规则

### 工具类选择

- 通用缓存操作 → `RedisUtils`（set/get/expire/delete/keys）
- Spring Cache 注解协同 → `CacheUtils`（evict/put/get）
- 队列能力 → `QueueUtils`（延迟队列/优先队列/消息队列）
- 不要在业务代码里直接注入 `RedissonClient` 调用原生 API，优先走上面三个工具。

### @RateLimiter 令牌桶限流

```java
@RateLimiter(key = "#phoneNumber", time = 60, count = 1)           // SpEL key，60秒1次
@RateLimiter(time = 60, count = 10, limitType = LimitType.IP)     // IP维度
@RateLimiter(time = 60, count = 10, limitType = LimitType.CLUSTER) // 集群维度
```

- 支持 `LimitType.DEFAULT/IP/CLUSTER`；消息走国际化，不要硬编码中文。
- 同一接口已有 `@RepeatSubmit` 或 `@Idempotent` 时，优先保持近邻注解组合不变。

### 二级缓存与 cacheNames

- `PlusSpringCacheManager` 采用 Redis + Caffeine 二级缓存，cacheNames 格式：`name#ttl#maxIdleTime#maxSize#local`。
- 常见缓存名直接用常量：`ONLINE_TOKEN_KEY`、`SYS_OSS_CONFIG`、`SYS_DICT`、`SYS_DICT_TYPE`、`SYS_CONFIG`。
- 字典、OSS 配置、部门、菜单写操作必须同步维护缓存失效：要么加 `@CacheEvict/@Caching`，要么显式 `CacheUtils.evict(...)`，禁止只改 DB 不失效缓存。
- `OssConfigChangeEvent` → `OssConfigChangeListener` 会刷新 Redis + 失效 OssClient；OSS 配置变更发布此事件而不是自搞清理逻辑。

## 字段增强（脱敏/翻译/加密/API 加密）

统一通过 `ruoyi-common-json` 的 `JsonFieldProcessor` 接口在 Jackson 序列化阶段处理（加密走 MyBatis 拦截器 + HTTP Filter）。

| 能力 | 注解 | 实现位置 | 执行时机 |
|------|------|----------|----------|
| 脱敏 | `@Sensitive(strategy, roleKey, perms)` | `SensitiveJsonFieldProcessor` | Jackson 序列化 |
| 翻译 | `@Translation(type, mapper, other)` | `TranslationJsonFieldProcessor` | Jackson 序列化 |
| 字段加密 | `@EncryptField(algorithm, ...)` | `MybatisEncryptInterceptor` / `MybatisDecryptInterceptor` | MyBatis 存取 |
| API 传输加密 | `@ApiEncrypt` | `CryptoFilter` | HTTP 请求/响应过滤（AES+RSA） |

- 翻译实现优先实现 `translationBatch(Set<Object> keys, String other)` 避免 N+1；不要退化成默认逐条查询。
- `TranslationInterface` 支持逗号分隔 ID 的实现可复用 `collectLongIds`/`parseLongIds`/`joinMappedValues`。
- 翻译失败或空值保持降级（返回原值或 null），不要抛异常中断主流程。

## 工作流编排核心规则

### 条件装配

- `ruoyi-workflow` 下所有 Bean 默认检查是否需要 `@ConditionalOnEnable`（= `@ConditionalOnProperty("warm-flow.enabled")`），`liteflow.enable` 跟随 `warm-flow.enabled`。
- 新增 Controller/Service/Listener/Component 前先看同包是否已有条件注解，保持一致性。

### Warm-Flow 全局监听

`WorkflowGlobalListener` 四个回调的约定：
- `start`：解析节点 ext 扩展配置，提取抄送人写入 `FLOW_COPY_LIST`。
- `assignment`：处理指定办理人，申请节点自动追加启动人。
- `finish`：发布流程/任务内部事件 → 触发抄送与消息通知 → 清理临时变量。
- 新增自定义扩展优先走新增 `Listener` 而不是改 `WorkflowGlobalListener` 本身，除非是框架级共性调整。

### LiteFlow 链路编排

- 链路定义 XML：`task-chain.el.xml`（`startProcessChain`/`completeTaskChain`/`taskOperationChain`）、`instance-chain.el.xml`（`deleteInstanceChain`）。
- 组件类用 `@LiteflowComponent`，默认实现 `NodeComponent`，上下文用 `CompleteTaskContext`/`StartProcessContext` 等 BO。
- 典型链路结构：`THEN(prepare, execute, IF(needAutoPass, autoPass, noop))`；新增组件命名保持动词 + 名词驼峰，对应 `nodeId`。

### 办理人与权限

- `WorkflowPermissionHandler.permissions()` 返回当前用户 ID；`convertPermissions(List<String>)` 通过 `IFlwTaskAssigneeService.fetchUsersByStorageIds` 将角色/部门存储 ID 转为实际用户 ID，新增办理人解析走这条链路。

## SnailJob 任务执行器规则

- 任务实现类加 `@JobExecutor(taskName = "...")`，位于 `ruoyi-job/src/main/java/.../jobtask/` 包。
- 9 种参考模式（按需对应选样例）：
  - DAG 工作流：`AlipayBillTask` / `WechatBillTask` / `SummaryBillTask`
  - 注解模式：`TestAnnoJobExecutor`
  - 类继承：`TestClassJobExecutor`
  - 广播：`TestBroadcastJob`
  - Map 模式：`TestMapJobAnnotation`
  - MapReduce：`TestMapReduceAnnotation1`
  - 静态分片：`TestStaticShardingJob`
- 写操作任务记得考虑幂等；需要回调状态时用项目约定的执行回执或日志写入方式，不要在任务里暴露 HTTP 接口。

## MCP 双向扩展规则

- **Server Tool**：类上加 `@Component`，方法上加 `@McpTool`，资源类方法加 `@McpResource`，参考 `ruoyi-demo` → `McpDemoServerTool`。
- **Client 调用**：通过 `McpClientService` 注入后调用，参考 `McpDemoClientService`，不要直接手写 HTTP/gRPC 调用。
- **Server 反向请求 Client**（采样/启发式回调）：用 `@McpSampling`、`@McpElicitation`，参考 `McpDemoClientHandlers`。
- 新 MCP 能力默认放 `ruoyi-demo` 验证，再视通用度下沉到 `ruoyi-admin` 或抽独立模块。

## Spring 事件与启动钩子

- **事件类**：Java 21 `record`，例如 `public record OssConfigChangeEvent() {}`。
- **发布**：`SpringUtils.context().publishEvent(event)`，或通过 `ApplicationEventPublisher` 注入。
- **监听**：`@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`；异步加 `@Async`；不要用 `@EventListener` 在事务提交前触发副作用（发消息/推送/清缓存）。
- **启动钩子**：实现 `ApplicationRunner`，放 `runner/` 包；例如 `SystemApplicationRunner` 初始化 OSS 配置缓存。启动失败即直接终止，不要吞异常静默启动。

## 工作流模块规则

- `ruoyi-workflow` 通常带 `@ConditionalOnEnable`，新增 workflow bean、controller、service 时检查同包是否需要该条件。
- 流程分类、任务、实例等查询常带分类权限或用户维度过滤，先读同类 mapper/service 再改。
- 工作流的翻译实现可以放在 workflow 模块内，例如流程分类 ID 到名称，仍应遵守 `TranslationInterface` 批量翻译规则。

## JavaDoc 注释规则

- 公共 API、接口、VO/BO/Entity 字段、Mapper 默认方法、Service/Controller 方法应有简洁 JavaDoc。
- 注释描述“做什么”和关键参数语义，不复述显而易见的实现细节。
- `void` 方法不要写 `@return`；返回布尔值时说明 `true/false` 含义。
- 私有方法只有在业务规则、算法、映射关系不直观时补注释。
- 框架覆写方法如果只是标准回调，可不重复注释；但当前文件已有统一注释风格时保持一致。
- 只改注释时，不重排 import、不格式化全文件、不修改代码行为。

## 前后端联动规则

- 新增后端接口时，路径和权限前缀尽量保持 generator 约定，方便前端目录和 API 命名同步。
- 新增日期范围查询时，记得保留 `bo.params` 结构，避免前端 `addDateRange` 无法对接。
- 导出接口通常保持 `POST /export` 风格，便于前端直接复用现有下载逻辑。
- 批量删除接口通常使用 `DELETE /{ids}`，便于前端直接传数组或逗号串。

## 生成器优先模式

从零新增 CRUD 时，优先对齐生成器默认方法集合：

- `queryById`
- `queryPageList`
- `queryList`
- `insertByBo`
- `updateByBo`
- `deleteWithValidByIds`

然后再叠加模块内已有增强，例如：

- 唯一性校验
- 数据权限注解
- MPJ 联表查询
- 缓存注解
- Excel 导入导出监听器
- 关联表维护逻辑

## 什么时候优先看 generator

- 新增一个标准单表 CRUD 时。
- 只有表结构和基本接口需求，没有现成业务模块可参考时。
- 需要快速补齐整套骨架代码时。

## 什么时候优先看现有模块

- 目标模块已经有类似业务。
- 涉及数据权限、联表、缓存、角色岗位关系、导入导出、工作流扩展时。
- 任务是“修改已有模块”而不是“新建模块”时。

## 避免事项

- 不要在 controller 里直接暴露 entity 代替 BO/VO。
- 不要给新的管理接口漏掉权限注解。
- 没有明确必要时，不要从 `BaseMapperPlus` 风格退回手工映射。
- 前端查询页用了日期范围时，不要删掉后端 `params` 相关处理。
- 不要把 `ruoyi-system` 这类复杂逻辑强行简化成生成器式单表 CRUD。

## 交付前自检

交付前至少检查这些点：

- CRUD 主链路是否完整。
- BO / VO / Entity 职责是否清晰。
- 分页、查询、删除校验是否与前端对得上。
- 权限、日志、防重、事务是否遗漏。
- 是否只是 generator 裸产物，如果是，需要继续补齐同模块已有增强。
