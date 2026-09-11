# RuoYi-Vue-Plus Code Wiki

> 版本：6.0.0 ｜ Spring Boot 4.1.0 ｜ JDK 21/25 ｜ 协议：MIT
> 仓库：[gitee](https://gitee.com/dromara/RuoYi-Vue-Plus) ｜ [github](https://github.com/dromara/RuoYi-Vue-Plus)
> 文档：[plus-doc.dromara.org](https://plus-doc.dromara.org)

Dromara RuoYi-Vue-Plus 是重写 RuoYi-Vue 针对**分布式集群**场景全方位升级的后台管理系统（与原框架不兼容）。本 Wiki 基于源码梳理项目整体架构、模块职责、关键类与函数、依赖关系及运行方式。

---

## 目录

- [一、项目整体架构](#一项目整体架构)
  - [1.1 技术栈概览](#11-技术栈概览)
  - [1.2 模块拓扑](#12-模块拓扑)
  - [1.3 分层与依赖关系](#13-分层与依赖关系)
  - [1.4 核心设计理念](#14-核心设计理念)
- [二、主要模块职责](#二主要模块职责)
  - [2.1 ruoyi-admin（Web 入口）](#21-ruoyi-adminweb-入口)
  - [2.2 ruoyi-api（跨模块契约层）](#22-ruoyi-api跨模块契约层)
  - [2.3 ruoyi-common（通用能力层）](#23-ruoyi-common通用能力层)
  - [2.4 ruoyi-modules（业务模块）](#24-ruoyi-modules业务模块)
  - [2.5 ruoyi-extend（扩展独立应用）](#25-ruoyi-extend扩展独立应用)
- [三、关键类与函数说明](#三关键类与函数说明)
  - [3.1 认证授权核心](#31-认证授权核心)
  - [3.2 数据访问核心](#32-数据访问核心)
  - [3.3 缓存与限流核心](#33-缓存与限流核心)
  - [3.4 字段增强核心](#34-字段增强核心)
  - [3.5 工作流编排核心](#35-工作流编排核心)
- [四、依赖关系](#四依赖关系)
  - [4.1 内部模块依赖](#41-内部模块依赖)
  - [4.2 核心第三方依赖](#42-核心第三方依赖)
- [五、项目运行方式](#五项目运行方式)
  - [5.1 环境前置](#51-环境前置)
  - [5.2 方式一：本地开发](#52-方式一本地开发)
  - [5.3 方式二：Docker 全量部署](#53-方式二docker-全量部署)
  - [5.4 方式三：裸 jar 部署](#54-方式三裸-jar-部署)
  - [5.5 端口总览](#55-端口总览)
  - [5.6 默认账号](#56-默认账号)
- [六、配置体系](#六配置体系)
- [七、AI 协作辅助文件](#七ai-协作辅助文件)

---

## 一、项目整体架构

### 1.1 技术栈概览

| 维度 | 选型 |
|------|------|
| 语言/运行时 | Java 21（亦支持 JDK 25），Liberica OpenJDK |
| 基础框架 | Spring Boot 4.1.0 |
| Web 容器 | Jetty（基于 Netty，替代 Tomcat） |
| 安全认证 | Sa-Token 1.45 + JWT（替代 Spring Security） |
| ORM | MyBatis-Plus 3.5.17 + MyBatis-Plus-Join 1.5.9 |
| 多数据源 | dynamic-datasource 4.5.0（HikariCP 连接池） |
| 缓存 | Redis ≥ 6 + Redisson 4.6.1 + Caffeine（二级缓存） |
| 分布式锁 | Lock4j 2.2.7（基于 Redisson） |
| 任务调度 | SnailJob 2.0.2（分布式，替代 Quartz） |
| 工作流 | Warm-Flow 1.8.9 + LiteFlow 2.16.1.2（规则编排） |
| 对象存储 | AWS S3 SDK v2（兼容 MinIO/阿里/腾讯/七牛） |
| 短信 | sms4j 3.3.5 |
| 邮件 | Jakarta Mail（Angus 实现） |
| Excel | Apache Fesod 2.0.2（原 EasyExcel 孵化版） |
| 接口文档 | SpringDoc 3.0.3 + therapi-javadoc（零注解） |
| 加密 | BouncyCastle 1.85（AES/RSA/SM2/SM4） |
| 三方登录 | JustAuth 3.0.1 |
| 搜索引擎 | Easy-Es 3.0.2 + Elasticsearch 7.17 |
| AI | Spring AI 2.0.0 + Snail AI 1.1.1 + MCP |
| MQTT | mica-mqtt 2.6.8 |
| 监控 | Spring Boot Admin 4.1.2 |
| 序列化 | Jackson（Spring 官方内置） |
| 工具 | Hutool 5.8.47、Lombok、MapStruct-Plus 1.5.1 |
| 数据库主键 | 雪花 ID（基于网卡 IP，集群安全） |
| GC | ZGC |

### 1.2 模块拓扑

项目采用 Maven 多模块结构，根 pom `packaging=pom` 聚合 5 个一级模块：

```
ruoyi-vue-plus (root, pom)
├── ruoyi-admin          # Web 服务入口（启动类、认证、验证码）
├── ruoyi-common         # 通用能力层（25 个子模块，BOM 统一版本）
├── ruoyi-modules        # 业务模块（6 个子模块）
├── ruoyi-api            # 跨模块契约层（接口 + DTO）
└── ruoyi-extend         # 扩展独立应用（3 个独立 Spring Boot 应用）
```

### 1.3 分层与依赖关系

整体采用**插件化 + 扩展包**形式，自底向上分层：

```
┌─────────────────────────────────────────────────────────────┐
│  ruoyi-admin (启动入口)                                       │
│    依赖: ruoyi-api + ruoyi-system + ruoyi-job + ruoyi-ai +  │
│          ruoyi-demo + ruoyi-workflow + ruoyi-gen(默认激活)  │
└─────────────────────────────────────────────────────────────┘
                          ▲ 依赖
┌─────────────────────────────────────────────────────────────┐
│  ruoyi-modules (业务模块)                                     │
│    ruoyi-system / ruoyi-gen / ruoyi-job /                   │
│    ruoyi-demo / ruoyi-workflow / ruoyi-ai                   │
└─────────────────────────────────────────────────────────────┘
                          ▲ 依赖
┌─────────────────────────────────────────────────────────────┐
│  ruoyi-api (契约层)                                           │
│    system.api.* (8 接口) + workflow.api.* (1 接口)          │
│    仅依赖 ruoyi-common-core（极度轻量）                      │
└─────────────────────────────────────────────────────────────┘
                          ▲ 依赖
┌─────────────────────────────────────────────────────────────┐
│  ruoyi-common (通用能力层, 25 子模块)                         │
│    core → json → redis → satoken → security → mybatis       │
│    web / log / oss / excel / encrypt / sensitive /          │
│    translation / job / mail / sms / push / mqtt /           │
│    ai / mcp / doc / liteflow / elasticsearch / social       │
└─────────────────────────────────────────────────────────────┘

ruoyi-extend (独立应用, 与主应用并行运行)
  ruoyi-monitor-admin / ruoyi-snailjob-server / ruoyi-snailai-server
```

### 1.4 核心设计理念

1. **契约与实现分离**：`ruoyi-api` 仅定义接口与 DTO，业务模块提供实现，消费方通过 Spring DI 注入接口，避免模块强耦合与循环依赖。
2. **插件化自动装配**：所有 common 子模块使用 Spring Boot 3+ 的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 机制，配合 `@ConditionalOnProperty` 按需启用。
3. **策略模式认证**：`IAuthStrategy` 接口 + Bean 名称约定（`grantType + "AuthStrategy"`），新增登录方式零侵入。
4. **事件驱动解耦**：操作日志、登录日志、工作流事件、在线用户同步等均通过 Spring 事件机制异步解耦（`@TransactionalEventListener` + `@Async`）。
5. **字段级统一增强**：脱敏、翻译、加密通过 `ruoyi-common-json` 的 `JsonFieldProcessor` + `JsonValueEnhancer` 在 Jackson 序列化阶段统一完成，业务无感知。
6. **虚拟线程友好**：JDK 21 虚拟线程开关（`SpringUtils.isVirtual()`），线程池、MQTT、LiteFlow 均支持切换。
7. **集群安全**：雪花 ID 基于网卡 IP 防集群重复、Redisson 分布式锁与限流、Sa-Token + Redis 会话同步。

---

## 二、主要模块职责

### 2.1 ruoyi-admin（Web 入口）

**职责**：系统启动入口、认证授权、验证码、注册。

**关键组成**：

| 包/文件 | 职责 |
|---------|------|
| [DromaraApplication.java](file:///workspace/ruoyi-admin/src/main/java/org/dromara/DromaraApplication.java) | 主启动类，`@SpringBootApplication` + `BufferingApplicationStartup(2048)` |
| [DromaraServletInitializer.java](file:///workspace/ruoyi-admin/src/main/java/org/dromara/DromaraServletInitializer.java) | WAR 部署支持 |
| [AuthController](file:///workspace/ruoyi-admin/src/main/java/org/dromara/web/controller/AuthController.java) | 登录/注册/登出/三方绑定，`/auth/**`，`@SaIgnore` |
| [CaptchaController](file:///workspace/ruoyi-admin/src/main/java/org/dromara/web/controller/CaptchaController.java) | 图片验证码 + 短信/邮箱验证码，限流保护 |
| [IndexController](file:///workspace/ruoyi-admin/src/main/java/org/dromara/web/controller/IndexController.java) | 首页引导 `GET /` |
| [IAuthStrategy](file:///workspace/ruoyi-admin/src/main/java/org/dromara/web/service/IAuthStrategy.java) | 认证策略接口（核心） |
| [SysLoginService](file:///workspace/ruoyi-admin/src/main/java/org/dromara/web/service/SysLoginService.java) | 登录失败次数限制、登录用户组装、登出 |
| [SysRegisterService](file:///workspace/ruoyi-admin/src/main/java/org/dromara/web/service/SysRegisterService.java) | 用户注册 + 验证码校验 |
| [UserActionListener](file:///workspace/ruoyi-admin/src/main/java/org/dromara/web/listener/UserActionListener.java) | Sa-Token 监听器 → 发布 `UserLoginSuccessEvent` |
| [UserLoginSuccessListener](file:///workspace/ruoyi-admin/src/main/java/org/dromara/web/listener/UserLoginSuccessListener.java) | 监听登录成功 → 写在线用户缓存 + 记日志 |

**5 种认证策略**：

| 策略类 | grantType | 登录体 |
|--------|-----------|--------|
| `PasswordAuthStrategy` | password | `PasswordLoginBody`（用户名 + 密码 + 可选图形码） |
| `SmsAuthStrategy` | sms | `SmsLoginBody`（手机号 + 短信码） |
| `EmailAuthStrategy` | email | `EmailLoginBody`（邮箱 + 邮箱码） |
| `SocialAuthStrategy` | social | `SocialLoginBody`（JustAuth 三方） |
| `XcxAuthStrategy` | xcx | `XcxLoginBody`（微信小程序 code） |

**配置文件**：
- [application.yml](file:///workspace/ruoyi-admin/src/main/resources/application.yml) — 公共配置（端口 8080、Sa-Token、MyBatis-Plus、MCP Server 等）
- [application-dev.yml](file:///workspace/ruoyi-admin/src/main/resources/application-dev.yml) — 开发环境（开 SQL 日志、小连接池）
- [application-prod.yml](file:///workspace/ruoyi-admin/src/main/resources/application-prod.yml) — 生产环境（关 SQL 日志、大连接池）

### 2.2 ruoyi-api（跨模块契约层）

**职责**：定义跨模块调用的接口与 DTO，打包为轻量 jar 被所有模块依赖，实现"契约与实现分离"。

**system.api 服务接口（8 个）**：

| 接口 | 路径 | 关键方法 |
|------|------|---------|
| `ConfigService` | [ConfigService.java](file:///workspace/ruoyi-api/src/main/java/org/dromara/system/api/ConfigService.java) | `getConfigValue/getConfigBool/Int/Long/Decimal`、`getConfigMap`、泛型 `getConfigObject<T>` |
| `UserService` | [UserService.java](file:///workspace/ruoyi-api/src/main/java/org/dromara/system/api/UserService.java) | 用户/昵称/手机/邮箱查询、按角色/部门/岗位查用户、批量查询 |
| `DeptService` | [DeptService.java](file:///workspace/ruoyi-api/src/main/java/org/dromara/system/api/DeptService.java) | 部门名/负责人查询 |
| `RoleService` | [RoleService.java](file:///workspace/ruoyi-api/src/main/java/org/dromara/system/api/RoleService.java) | 角色名批量查询 |
| `PostService` | [PostService.java](file:///workspace/ruoyi-api/src/main/java/org/dromara/system/api/PostService.java) | 岗位名批量查询 |
| `OssService` | [OssService.java](file:///workspace/ruoyi-api/src/main/java/org/dromara/system/api/OssService.java) | 文件 URL/详情批量查询 |
| `MessageService` | [MessageService.java](file:///workspace/ruoyi-api/src/main/java/org/dromara/system/api/MessageService.java) | 单用户/全局/批量消息推送 |
| `TaskAssigneeService` | [TaskAssigneeService.java](file:///workspace/ruoyi-api/src/main/java/org/dromara/system/api/TaskAssigneeService.java) | 工作流办理人（角色/部门/岗位/用户）解析 |

**workflow.api**：
- [WorkflowService](file:///workspace/ruoyi-api/src/main/java/org/dromara/workflow/api/WorkflowService.java) — 11 方法覆盖流程全生命周期（启动、办理、变量读写、状态查询、删除）
- 3 个 Spring 事件：`ProcessEvent`、`ProcessTaskEvent`、`ProcessDeleteEvent`（松耦合通知）

**DTO 清单**：`UserDTO`、`DeptDTO`、`RoleDTO`、`PostDTO`、`OssDTO`、`UserOnlineDTO`、`TaskAssigneeDTO`、`PushPayloadDTO`、`LoginUser`、`XcxLoginUser`、6 个登录体、`StartProcessDTO`/`CompleteTaskDTO`/`FlowCopyDTO`/`FlowInstanceBizExtDTO`/`StartProcessReturnDTO`。

> **使用方式**：消费方 Maven 依赖 `ruoyi-api`，`@Autowired` 注入接口；实现类（如 `UserServiceImpl`）在业务模块标注 `@Service`，Spring 按类型装配。非 RPC，单 JVM 内 Bean 调用，但 DTO 实现 `Serializable` 以支持 Redis 缓存与 Sa-Token 会话存储。

### 2.3 ruoyi-common（通用能力层）

父 pom `/workspace/ruoyi-common/pom.xml` 聚合 25 个子模块，`ruoyi-common-bom` 统一版本管理。以下按依赖层次分组：

#### 基础层

| 模块 | 职责 | 关键类 |
|------|------|--------|
| **ruoyi-common-core** | 核心基石：统一响应、异常、常量、工具、AOP、线程池 | `R<T>`、`PageResult`、`ServiceException`、`SpringUtils`、`ThreadPoolConfig`、`MessageUtils`、`CacheNames`、`@Xss` |
| **ruoyi-common-bom** | 25 个 common 模块版本集中管理（pom） | — |
| **ruoyi-common-json** | Jackson 序列化：大数防失真、日期格式、字段增强框架 | `JacksonConfig`、`JsonUtils`、`JsonValueEnhancer`、`JsonFieldProcessor`（脱敏/翻译实现此接口） |

#### 缓存与认证层

| 模块 | 职责 | 关键类 |
|------|------|--------|
| **ruoyi-common-redis** | Redisson + Caffeine 二级缓存、分布式限流、防重提交、队列 | `RedisUtils`、`CacheUtils`、`QueueUtils`、`@RateLimiter`、`@RepeatSubmit`、`PlusSpringCacheManager`、`RateLimiterAspect` |
| **ruoyi-common-satoken** | Sa-Token + JWT 认证、登录上下文 | `LoginHelper`（核心）、`SaTokenConfig`、`PlusSaTokenDao`、`SaPermissionImpl` |
| **ruoyi-common-security** | 路由拦截、客户端访问路径/IP 白名单校验、Actuator Basic Auth | `SecurityConfig`、`AllUrlHandler` |

#### 数据访问层

| 模块 | 职责 | 关键类 |
|------|------|--------|
| **ruoyi-common-mybatis** | MyBatis-Plus 配置、数据权限、乐观锁、分页、SQL 日志、字段自动填充、雪花 ID | `MybatisPlusConfig`、`@DataPermission`、`PlusDataPermissionInterceptor`、`InjectionMetaObjectHandler`、`SqlLogInterceptor`、`MPJSqlInjector`、`DataPermissionHelper` |
| **ruoyi-common-elasticsearch** | Easy-ES 集成 | `EasyEsConfiguration`（`@EsMapperScan`） |

#### Web 与文档层

| 模块 | 职责 | 关键类 |
|------|------|--------|
| **ruoyi-common-web** | Jetty 容器、全局异常、XSS 过滤、CORS、i18n、验证码、响应增强 | `GlobalExceptionHandler`、`ResourcesConfig`、`XssFilter`、`I18nLocaleResolver`、`WaveAndCircleCaptcha`、`ResponseEnhancementAdvice` |
| **ruoyi-common-doc** | SpringDoc + therapi-javadoc 零注解文档 | `SpringDocConfig`、`JavadocOperationCustomizer`、`SaTokenAnnotationMetadataJavadocResolver` |
| **ruoyi-common-log** | 操作日志 AOP + 登录日志事件 | `@Log`、`LogAspect`、`OperLogEvent`、`LoginInfoEvent` |

#### 功能扩展层

| 模块 | 职责 | 关键类 |
|------|------|--------|
| **ruoyi-common-oss** | S3 协议对象存储工厂（MinIO/阿里/腾讯/七牛） | `OssClient`、`OssFactory`、`DefaultOssClientImpl` |
| **ruoyi-common-excel** | Fesod（EasyExcel）封装，链式 API、模板、下拉、合并 | `ExcelBuilder`、`@CellMerge`、`@ExcelDictFormat`、`DefaultExcelListener` |
| **ruoyi-common-encrypt** | 字段级加解密（MyBatis 拦截器）+ API 传输加解密 | `@EncryptField`、`@ApiEncrypt`、`EncryptorManager`、`MybatisEncryptInterceptor`、`CryptoFilter`、`AlgorithmType`（BASE64/AES/RSA/SM2/SM4） |
| **ruoyi-common-sensitive** | 数据脱敏（Jackson 序列化期） | `@Sensitive`、`SensitiveStrategy`（18 种策略）、`SensitiveJsonFieldProcessor` |
| **ruoyi-common-translation** | 数据翻译（Jackson 序列化期） | `@Translation`、`TranslationInterface`、`TranslationJsonFieldProcessor` |
| **ruoyi-common-job** | SnailJob 客户端 + 远程日志上报 | `SnailJobConfig` |
| **ruoyi-common-mail** | 邮件（Angus Mail） | `MailConfig`、`MailBuilder` |
| **ruoyi-common-sms** | sms4j 短信 + Redis 限流 | `SmsAutoConfiguration`、`PlusSmsDao` |
| **ruoyi-common-push** | 消息推送（SSE / WebSocket 双模式 + Redis 主题集群分发） | `MessageAutoConfiguration`、`SseEmitterSessionManager`、`WebSocketSessionManager`、`PushHelper` |
| **ruoyi-common-mqtt** | mica-mqtt 客户端（虚拟线程） | `MqttAutoConfiguration` |
| **ruoyi-common-ai** | Snail AI Agent + OpenAPI | `SnailAiConfig` |
| **ruoyi-common-mcp** | Spring AI MCP Server/Client 封装 | `McpAutoConfiguration`、`McpClientTemplate`（`listTools`/`callTool`/`readResource`） |
| **ruoyi-common-liteflow** | LiteFlow 公共节点（noop/fail/alwaysTrue 等） | `LiteFlowAutoConfiguration`、`LiteFlowUtils` |
| **ruoyi-common-social** | JustAuth 三方登录 + Redis state 缓存 | `SocialAutoConfiguration`、`AuthRedisStateCache`、`SocialUtils` |

### 2.4 ruoyi-modules（业务模块）

#### ruoyi-system（核心系统管理）

**职责**：用户/角色/部门/菜单/字典/参数/通知/日志/文件/客户端/社交/消息中心管理，是体积最大的业务模块。

**领域实体**（`org.dromara.system.domain`，均继承 `BaseEntity`，除日志表）：

| 实体 | 表 | 说明 |
|------|----|------|
| `SysUser` | sys_user | 用户，`isSuperAdmin()` 判断超管 |
| `SysRole` | sys_role | 角色，dataScope 1-6 数据范围 |
| `SysDept` | sys_dept | 部门树（ancestors 祖级列表） |
| `SysMenu` | sys_menu | 菜单（M目录/C菜单/F按钮），内置路由解析方法 |
| `SysConfig` | sys_config | 系统参数 |
| `SysDictType`/`SysDictData` | sys_dict_type/data | 字典 |
| `SysPost` | sys_post | 岗位 |
| `SysNotice` | sys_notice | 通知公告 |
| `SysOperLog` | sys_oper_log | 操作日志（不继承 BaseEntity） |
| `SysLoginInfo` | sys_login_info | 登录日志 |
| `SysOss`/`SysOssConfig`/`SysOssExt` | sys_oss/config | 文件与存储配置 |
| `SysClient` | sys_client | OAuth2 客户端 |
| `SysSocial` | sys_social | 三方社交关系 |
| `SysMessage` | sys_message | 消息中心 |
| 关联表 | sys_role_menu/role_dept/user_role/user_post | 多对多 |

**Service 接口（19 个）**：`ISysUserService`、`ISysRoleService`、`ISysDeptService`、`ISysMenuService`、`ISysConfigService`、`ISysDictTypeService`、`ISysDictDataService`、`ISysOssService`、`ISysOssConfigService`、`ISysPermissionService`、`ISysDataScopeService`、`ISysClientService`、`ISysMessageService`、`ISysOperLogService`、`ISysLoginInfoService`、`ISysNoticeService`、`ISysPostService`、`ISysSocialService`、`ISysTaskAssigneeService`、`ISysSensitiveService`。

**事件**（Java 21 record + `@TransactionalEventListener(AFTER_COMMIT)`）：
- `OnlineUserCleanEvent` → `OnlineUserCleanListener`（`@Async` 踢出在线用户）
- `OssConfigChangeEvent` → `OssConfigChangeListener`（刷新 Redis 缓存 + 失效 OssClient）

**启动钩子**：[SystemApplicationRunner](file:///workspace/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/runner/SystemApplicationRunner.java) 初始化 OSS 配置缓存。

**Controller**：`controller/monitor`（缓存/登录日志/操作日志/在线用户）+ `controller/system`（用户/角色/部门/菜单/配置/字典/岗位/通知/OSS/客户端/社交/个人中心/消息盒子）。

#### ruoyi-gen（代码生成器）

**职责**：基于数据库表元数据，一键生成完整 CRUD 前后端代码（Java + Vue/React + SQL + XML）。

**核心**：
- [GenController](file:///workspace/ruoyi-modules/ruoyi-gen/src/main/java/org/dromara/gen/controller/GenController.java) — `/tool/gen`，导入/预览/下载/同步
- [GenUtils](file:///workspace/ruoyi-modules/ruoyi-gen/src/main/java/org/dromara/gen/util/GenUtils.java) — 字段类型智能推断（字符串长度、布尔、状态/类型后缀、图片/文件后缀等）
- [TemplateEngineUtils](file:///workspace/ruoyi-modules/ruoyi-gen/src/main/java/org/dromara/gen/util/TemplateEngineUtils.java) — FreeMarker 渲染，构建上下文 + 文件名推导
- `MyBatisDataSourceMonitor` — 适配 dynamic-datasource 给 Anyline 提供元数据
- **模板**：`fm/java/`（7）、`fm/vue/`（4）、`fm/react/`（4）、`fm/sql/`（4 数据库变体）、`fm/xml/`（1）
- **配置**：`generator.yml`（author、packageName、tablePrefix）

#### ruoyi-job（定时任务）

**职责**：SnailJob 任务示例集，覆盖所有调度模式。

9 个任务执行器（`@JobExecutor`）：`AlipayBillTask`/`WechatBillTask`/`SummaryBillTask`（DAG 工作流）、`TestAnnoJobExecutor`（注解）、`TestClassJobExecutor`（类继承）、`TestBroadcastJob`（广播）、`TestMapJobAnnotation`（Map）、`TestMapReduceAnnotation1`（MapReduce）、`TestStaticShardingJob`（静态分片）。

#### ruoyi-demo（示例/教学）

**职责**：覆盖几乎所有 common 能力的集成示范，是新开发者的学习入口。

19+ Controller 演示：ES CRUD、邮件、MCP、MQTT、Redis（Cache/Lock/PubSub/RateLimiter/Queue）、Sa-Token（16 种权限场景）、SMS、Swagger、批量操作、单表 CRUD、加密、Excel、i18n、脱敏、树表、WebSocket、优先队列。

**MCP 双向演示**：`McpDemoServerTool`（Server 端 `@McpTool`/`@McpResource`）+ `McpDemoClientService`（Client 端调用）+ `McpDemoClientHandlers`（Server 反向请求 Client 的 `@McpSampling`/`@McpElicitation` 回调）。

#### ruoyi-workflow（工作流引擎）

**职责**：集成 Warm-Flow 工作流 + LiteFlow 规则编排，是架构最复杂的模块。

**条件装配**：[ConditionalOnEnable](file:///workspace/ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/common/ConditionalOnEnable.java)（`@ConditionalOnProperty("warm-flow.enabled")`），可整体禁用工作流。

**核心组件**：
- **Controller（6）**：`FlwDefinitionController`（流程定义）、`FlwInstanceController`（实例）、`FlwTaskController`（任务核心）、`FlwCategoryController`（分类）、`FlwSpelController`（SpEL）、`TestLeaveController`（请假示例）
- **Handler**：`FlowProcessEventHandler`（流程事件发布）、`WorkflowPermissionHandler`（办理人解析）、`FlowExceptionHandler`
- **Listener**：`WorkflowGlobalListener`（Warm-Flow 全局监听，处理 create/start/assignment/finish）、`WorkflowSideEffectListener`（消费内部事件做抄送/消息通知）
- **LiteFlow 编排**：3 条 EL 链路（`startProcessChain`/`completeTaskChain`/`taskOperationChain`）+ 12 个 `@LiteflowComponent`，将复杂流程操作组件化
- **Service（11 接口）**：`IFlwTaskService`（任务核心）、`IFlwInstanceService`、`IFlwDefinitionService`、`IFlwCategoryService`、`IFlwSpelService`、`IFlwNodeExtService`、`IFlwTaskAssigneeService`、`IFlwCommonService`、`ITestLeaveService` 等
- **规则**：`SpelRuleComponent`（SpEL 表达式统一入口，如查部门负责人）

**LiteFlow 链路定义**：
- `instance-chain.el.xml` — `deleteInstanceChain`
- `task-chain.el.xml` — `startProcessChain` / `completeTaskChain` / `taskOperationChain`

#### ruoyi-ai（AI 特性）

**职责**：Snail AI OpenAPI 用户注册入口。

唯一 Controller `SnailAiController`（`/snail-ai`），`POST /user/register` 通过 `LoginHelper` 取当前用户，调用 `OpenApiUserClient.register` 注册到 Snail AI 平台。

### 2.5 ruoyi-extend（扩展独立应用）

3 个独立 Spring Boot 应用，与主应用并行运行，均通过 `spring-boot-admin-starter-client` 向监控中心注册。

| 应用 | 端口 | 角色 | 启动类 |
|------|------|------|--------|
| **ruoyi-monitor-admin** | 9090 | Spring Boot Admin 监控中心 | `MonitorAdminApplication`（`@EnableAdminServer`） |
| **ruoyi-snailjob-server** | 8800 + 17888 | 分布式任务调度中心 | `SnailJobServerApplication`（委托 aizuda 框架） |
| **ruoyi-snailai-server** | 8900 + 18888(gRPC) | AI/RAG 服务 | `SnailAiServerApplication`（委托 aizuda 框架） |

**monitor-admin 特性**：使用 Undertow 容器、Spring Security 表单登录、`CustomNotifier` 中文状态通知、自监控。
**snailai-server 特性**：RAG 文档解析（Docling + PaddleOCR）、本地/MinIO 存储、向量检索（ES）。
**snailjob-server 特性**：分桶调度、日志合并、负载均衡，显式指定 Scala 2.13.9。

---

## 三、关键类与函数说明

### 3.1 认证授权核心

#### IAuthStrategy（策略模式入口）

```java
// 策略分发：grantType + "AuthStrategy" 拼接 Bean 名称
static LoginVo login(String body, SysClientVo client, String grantType) {
    String beanName = grantType + BASE_NAME;
    if (!SpringUtils.containsBean(beanName)) {
        throw new ServiceException("授权类型不正确!");
    }
    return SpringUtils.getBean(beanName).login(body, client);
}

// 统一构建 Sa-Token 登录参数
static SaLoginParameter buildLoginParameter(SysClientVo client, Consumer<SaLoginParameter> customizer);
```

#### LoginHelper（登录上下文助手）

核心常量：`LOGIN_USER_KEY`、`USER_KEY`、`CLIENT_KEY`、`CLIENT_ACCESS_PATH_KEY`、`CLIENT_IP_WHITELIST_KEY`。

关键方法：`login(LoginUser, SaLoginParameter)`、`getLoginUser()`、`getUserId()`、`getUsername()`、`getDeptId()`、`isSuperAdmin()`、`isLogin()`。登录时自动填充 IP/地址/浏览器/OS/设备类型。

#### SysLoginService

- `checkLogin(LoginType, username, Supplier<Boolean>)` — 失败次数限制核心，Redis 计数，默认 5 次锁定 10 分钟
- `buildLoginUser(SysUserVo)` — 虚拟线程并行加载菜单/角色/数据范围/岗位
- `recordLoginInfo(username, status, message)` — 发布 `LoginInfoEvent`
- `socialRegister(AuthUser)` — `@Lock4j` 分布式锁保护三方绑定

### 3.2 数据访问核心

#### MybatisPlusConfig

组装拦截器链：`PlusDataPermissionInterceptor`（数据权限）→ `PaginationInnerInterceptor`（分页）→ `OptimisticLockerInnerInterceptor`（乐观锁）。注册 `InjectionMetaObjectHandler`（自动填充 createBy/createTime/updateBy/updateTime/createDept）、`IdentifierGenerator`（基于网卡 IP 雪花 ID）。

#### @DataPermission + DataPermissionHelper

```java
@DataPermission({
    @DataColumn(alias = "d", name = "dept_id"),
    @DataColumn(alias = "u", name = "user_id")
})
List<SysUserVo> selectUserList(...);
```

`DataPermissionHelper.ignore(() -> ...)` 绕过数据权限执行（如更新登录 IP）。

#### InjectionMetaObjectHandler

插入/更新自动填充审计字段，从 `LoginHelper` 取当前用户，未登录用 -1。

### 3.3 缓存与限流核心

#### RedisUtils / CacheUtils / QueueUtils

- `RedisUtils` — 通用缓存操作（set/get/expire/delete/keys）
- `CacheUtils` — Spring Cache 工具（`evict`/`put`/`get`），配合 `@Cacheable` 注解
- `QueueUtils` — 基于 Redis 的延迟队列/优先队列/消息队列

#### @RateLimiter（令牌桶限流）

```java
@RateLimiter(key = "#phoneNumber", time = 60, count = 1)  // 60秒1次
@RateLimiter(time = 60, count = 10, limitType = LimitType.IP)  // IP维度
```

支持 SpEL key、`LimitType`（DEFAULT/IP/CLUSTER）、国际化消息。

#### PlusSpringCacheManager

Redis + Caffeine 二级缓存，cacheNames 格式 `name#ttl#maxIdleTime#maxSize#local`，如 `ONLINE_TOKEN_KEY`、`SYS_OSS_CONFIG`。

### 3.4 字段增强核心

统一通过 `ruoyi-common-json` 的 `JsonFieldProcessor` 接口在 Jackson 序列化阶段处理：

| 能力 | 注解 | 处理器 | 时机 |
|------|------|--------|------|
| 脱敏 | `@Sensitive(strategy, roleKey, perms)` | `SensitiveJsonFieldProcessor` | Jackson 序列化 |
| 翻译 | `@Translation(type, mapper, other)` | `TranslationJsonFieldProcessor` | Jackson 序列化 |
| 加密 | `@EncryptField(algorithm, ...)` | `MybatisEncryptInterceptor`/`MybatisDecryptInterceptor` | MyBatis 拦截器（存取期） |
| API 加密 | `@ApiEncrypt` | `CryptoFilter` | HTTP 过滤器（动态 AES+RSA） |

`TranslationInterface` 支持 `translationBatch`（批量翻译，避免 N+1）。

### 3.5 工作流编排核心

#### WorkflowGlobalListener（Warm-Flow 全局监听）

四个回调：
- `start` — 解析节点 ext 扩展配置，提取抄送人写入 `FLOW_COPY_LIST`
- `assignment` — 处理指定办理人，申请节点自动加启动人
- `finish` — 发布流程/任务事件，触发抄送与消息通知，清理临时变量

#### LiteFlow 链路（以 completeTaskChain 为例）

```xml
completeTaskChain = THEN(
    completePrepare,
    completeExecute,
    IF(completeNeedAutoPass, completeAutoPass, noop)
);
```

`CompleteExecuteComponent` 从 `CompleteTaskContext` 取 BO，构建 `FlowParams.PASS`，调用 `taskService.skip`，回写 autoPass 标志。

#### WorkflowPermissionHandler

`permissions()` 返回当前用户 ID；`convertPermissions(List<String>)` 通过 `IFlwTaskAssigneeService.fetchUsersByStorageIds` 将角色/部门存储 ID 转为实际用户 ID。

---

## 四、依赖关系

### 4.1 内部模块依赖

```
ruoyi-admin
  ├── ruoyi-api
  ├── ruoyi-system
  ├── ruoyi-job
  ├── ruoyi-ai
  ├── ruoyi-demo
  ├── ruoyi-workflow
  └── ruoyi-gen (profile=gen, 默认激活)

ruoyi-modules/*
  └── 依赖 ruoyi-common/* + ruoyi-api

ruoyi-api
  └── ruoyi-common-core

ruoyi-common 层次:
  core (基石)
   ↓
  json (序列化)
   ↓
  redis (缓存) ← satoken (认证) ← security (路由)
   ↓                ↓
  mybatis (数据)    mybatis 也依赖 satoken
   ↓
  web / log / oss / excel / encrypt / sensitive / translation / ...
```

**关键依赖链**：
- `ruoyi-common-mybatis` 依赖 `ruoyi-common-satoken`（数据权限需登录上下文）+ `ruoyi-api`（自动填充需用户信息）
- `ruoyi-common-security` 依赖 `ruoyi-common-satoken`（路由拦截需 Sa-Token）
- `ruoyi-common-push` 依赖 `ruoyi-common-satoken`（WebSocket 握手需登录校验）
- 几乎所有功能模块依赖 `ruoyi-common-json`（序列化）+ `ruoyi-common-core`（基础）

### 4.2 核心第三方依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| spring-boot | 4.1.0 | 基础框架 |
| mybatis-plus | 3.5.17 | ORM |
| dynamic-datasource | 4.5.0 | 多数据源 |
| redisson | 4.6.1 | Redis 客户端 |
| sa-token | 1.45.0 | 认证授权 |
| lock4j | 2.2.7 | 分布式锁 |
| snailjob | 2.0.2 | 任务调度 |
| warm-flow | 1.8.9 | 工作流 |
| liteflow | 2.16.1.2 | 规则编排 |
| aws-sdk | 2.48.1 | 对象存储 |
| sms4j | 3.3.5 | 短信 |
| fesod | 2.0.2 | Excel |
| hutool | 5.8.47 | 工具 |
| mapstruct-plus | 1.5.1 | 对象映射 |
| spring-ai | 2.0.0 | AI |
| justauth | 3.0.1 | 三方登录 |
| bouncycastle | 1.85 | 加密 |
| easy-es | 3.0.2 | ES 搜索 |
| mica-mqtt | 2.6.8 | MQTT |
| spring-boot-admin | 4.1.2 | 监控 |

---

## 五、项目运行方式

### 5.1 环境前置

- JDK 21+（推荐 Liberica OpenJDK）
- Maven 3.9+（或使用项目自带 mvnw）
- MySQL 8+（库 `ry-vue`）
- Redis 6+
- （可选）MinIO、Nginx

### 5.2 方式一：本地开发

1. **启动基础设施**（在 `script/docker/` 执行）：
   ```bash
   docker-compose up -d mysql redis minio
   ```

2. **初始化数据库**：在 `ry-vue` 库依次执行：
   ```
   script/sql/ry_vue.sql       # 系统核心库
   script/sql/ry_job.sql        # SnailJob 调度库
   script/sql/ry_workflow.sql   # Warm-Flow 工作流库
   script/sql/ry_ai.sql         # Snail AI 库
   ```

3. **构建后端**（根目录）：
   ```bash
   ./mvnw clean package
   ```

4. **启动主应用**：IDE 运行 [DromaraApplication](file:///workspace/ruoyi-admin/src/main/java/org/dromara/DromaraApplication.java)，或：
   ```bash
   java -jar ruoyi-admin/target/ruoyi-admin.jar
   ```

5. **扩展服务（可选）**：分别运行 `MonitorAdminApplication`、`SnailJobServerApplication`、`SnailAiServerApplication`。

6. **前端**：单独 clone [plus-ui](https://gitee.com/JavaLionLi/plus-ui)（Vue3+ElementPlus）或 [plus-ui-react](https://gitee.com/JavaLionLi/plus-ui/tree/6.X-React/)，构建后部署到 nginx。

### 5.3 方式二：Docker 全量部署

1. **构建镜像**（IDEA 用 `.run/*.run.xml` 一键构建，或手动）：
   ```bash
   docker build -t ruoyi/ruoyi-server:6.0.0 ruoyi-admin/
   docker build -t ruoyi/ruoyi-monitor-admin:6.0.0 ruoyi-extend/ruoyi-monitor-admin/
   docker build -t ruoyi/ruoyi-snailjob-server:6.0.0 ruoyi-extend/ruoyi-snailjob-server/
   docker build -t ruoyi/ruoyi-snailai-server:6.0.0 ruoyi-extend/ruoyi-snailai-server/
   ```

2. **启动全部服务**（在 `script/docker/` 执行）：
   ```bash
   docker-compose up -d
   ```

   将启动 9 个服务（mysql/nginx/redis/minio + 主应用双实例 + 3 个扩展服务），nginx 自动负载均衡 `/prod-api/` → 8080/8081。

### 5.4 方式三：裸 jar 部署

```bash
# Linux
script/bin/ry.sh start    # 启动
script/bin/ry.sh stop      # 停止
script/bin/ry.sh restart   # 重启
script/bin/ry.sh status    # 状态

# Windows
script/bin/ry.bat
```

JVM 参数：`-Xms512m -Xmx1024m -XX:+UseZGC -Duser.timezone=Asia/Shanghai`，日志输出到 `logs/`。

### 5.5 端口总览

| 端口 | 服务 | 说明 |
|------|------|------|
| 80 / 443 | nginx-web | 前端入口 + 反向代理 |
| 3306 | mysql | 数据库 `ry-vue`（root/root） |
| 6379 | redis | 密码 `ruoyi123`，database 0 |
| 8080 / 8081 | ruoyi-server1/2 | 主应用双实例集群 |
| 9090 | ruoyi-monitor-admin | 监控中心（context-path=/admin） |
| 8800 | ruoyi-snailjob-server | SnailJob HTTP（context-path=/snail-job） |
| 17888 | ruoyi-snailjob-server | SnailJob 调度通信 |
| 8900 | ruoyi-snailai-server | Snail AI HTTP（context-path=/snail-ai） |
| 18888 | ruoyi-snailai-server | Snail AI gRPC |
| 9000 / 9001 | minio | API / 控制台（ruoyi/ruoyi123） |
| 28080-28081 | SnailJob 客户端 | 随主应用端口漂移 |
| 38080-38081 | Snail AI 客户端 | 随主应用端口漂移 |

### 5.6 默认账号

`admin / admin123`

---

## 六、配置体系

项目采用 Maven Profile + Spring Profile 双层配置：

**Maven Profile**（根 pom）：`local` / `dev`（默认）/ `prod`，通过 `@profiles.active@` 占位符注入 Spring，并控制 `logging.level`、监控账号密码。

**Spring Profile**：`application.yml`（公共）+ `application-{profile}.yml`（环境特定）。

**关键配置项**（application.yml）：

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `server.port` | 8080 | 主应用端口 |
| `spring.threads.virtual.enabled` | false | 虚拟线程开关（JDK21） |
| `sa-token.is-concurrent` | true | 允许同账号并发登录 |
| `sa-token.is-share` | false | 每次登录新建 token |
| `mybatis-plus.idType` | ASSIGN_ID | 雪花 ID |
| `mybatis-plus.enableLogicDelete` | true | 逻辑删除 |
| `captcha.enable` | true | 验证码开关 |
| `captcha.type` | math | 数学验证码 |
| `user.password.maxRetryCount` | 5 | 登录失败锁定阈值 |
| `user.password.lockTime` | 10 | 锁定分钟数 |
| `api-decrypt.enabled` | true | API 传输加密 |
| `message.transport` | sse | 消息推送模式（sse/websocket） |
| `warm-flow.enabled` | true | 工作流开关 |
| `liteflow.enable` | ${warm-flow.enabled} | LiteFlow 随工作流 |
| `mqtt.client.enabled` | false | MQTT 默认关闭 |
| `easy-es.enable` | false | ES 默认关闭 |
| `spring.ai.mcp.server.enabled` | true | MCP Server 默认开启 |
| `snail-job.enabled` | false（dev） | SnailJob 客户端开关 |
| `snail-ai.enabled` | false（dev） | Snail AI 客户端开关 |

**配置加载机制**：common 子模块普遍使用 `@PropertySource("classpath:xxx.yml", factory = YmlPropertySourceFactory.class)` 加载独立 yml（如 `common-mybatis.yml`、`common-satoken.yml`、`generator.yml`）。

---

## 七、AI 协作辅助文件

项目内置两套 AI 编码辅助配置，确保 AI 生成的代码与项目规范对齐：

### .codex/skills/ruoyi-plus-ai-coding/

**SKILL.md** 定义代码生成器风格对齐规则：
- 优先级：当前模块真实业务代码 > 公共基础模块约定 > 代码生成器 FreeMarker 模板 > 通用 Spring Boot 习惯
- 5 种任务分型：标准单表 CRUD、强业务模块扩展、基础能力复用、公共基础模块修改、注释修正
- 关键约定：Mapper 继承 `BaseMapperPlus<Entity, Vo>`、BO 用 `@AutoMapper(target = Entity.class, reverseConvertGenerate = false)`、权限标识 `${module}:${business}:${action}`、查询优先 `LambdaQueryWrapper`
- 触发：`$ruoyi-plus-ai-coding`

### .claude/agents/

6 个 Claude Code 子 agent 构成后端工程多 agent 协作体系：

| Agent | 职责 |
|-------|------|
| `backend-engineering` | 总入口，识别任务类型并路由 |
| `backend-crud` | 标准单表 CRUD 专家 |
| `backend-module-enhancement` | 复杂模块增量增强 |
| `backend-query-permission` | 查询/联表/数据权限 |
| `backend-common-infrastructure` | 公共基础模块 |
| `backend-javadoc` | JavaDoc 注释修正 |

共享原则：优先相信当前模块真实代码，不替换 `BaseMapperPlus`/`PageQuery`/`PageResult`/`R`/`MapstructUtils` 等项目工具。

---

> 本 Wiki 基于源码静态分析生成，反映仓库当前状态。如需了解最新动态，请参考 [官方文档](https://plus-doc.dromara.org)。
