# RuoYi-Vue-Plus Agent Guide

> 个人/全局 Agent 规则可放在仓库外。本文件记录本仓库的目录地图、开发约定、当前活跃变更、风险区与验证方式。  
> 人类可读精简版：**[`docs/PROJECT_GUIDE.md`](docs/PROJECT_GUIDE.md)**（命令、技术栈、规范、分层配置；功能变更时与本文同步更新）。

## Project

[RuoYi-Vue-Plus](https://gitee.com/dromara/RuoYi-Vue-Plus) 是基于 Spring Boot 3 + Vue3 的多租户后台管理系统（Dromara 5.x）。本 fork 在标准模块之上扩展了自定义业务模块，当前活跃开发为 **GitHub Stars 知识库（stars-library）**。

| 属性 | 值 |
|------|-----|
| 框架版本 | 5.6.1 |
| JDK | 17（兼容 21） |
| Spring Boot | 3.5.x |
| 认证 | Sa-Token + JWT |
| ORM | MyBatis-Plus |
| 官方文档 | [plus-doc.dromara.org](https://plus-doc.dromara.org) |

## Repository Map

### 核心目录

```
RuoYi-Vue-Plus/
├── ruoyi-admin/              # 启动入口，聚合各业务模块
├── ruoyi-common/             # 公共组件（core、web、security、mybatis、redis…）
├── ruoyi-modules/            # 业务模块
│   ├── ruoyi-system/         # 系统管理（用户、角色、菜单、租户…）
│   ├── ruoyi-demo/           # 框架功能示例
│   ├── ruoyi-generator/      # 代码生成
│   ├── ruoyi-job/            # 定时任务
│   ├── ruoyi-workflow/       # 工作流
│   ├── ai-structured/        # AI 结构化输出参考模块（DeepSeek + Kafka）
│   └── stars-library/        # GitHub Stars 知识库（进行中）
├── ruoyi-extend/             # 监控、SnailJob 等扩展服务
├── script/sql/               # 数据库脚本（含 stars_library.sql）
├── docs/                     # PRD、实现计划等产品/工程文档
├── openspec/                 # OpenSpec 变更规格（stars-library 等）
├── .cursor/skills/           # Cursor Agent Skills（OpenSpec、UI/UX 等）
└── .agents/skills/kami/      # 嵌套的 Kami 文档生成 skill（独立子项目）
```

### Stars Library（当前重点）

| 路径 | 说明 |
|------|------|
| `docs/prd/github-stars-knowledge-base.md` | 产品需求文档（PRD v1.1） |
| `docs/superpowers/plans/2026-05-22-stars-library.md` | 分阶段实现计划 |
| `openspec/changes/stars-library/` | OpenSpec 变更：proposal、design、specs、tasks |
| `ruoyi-modules/stars-library/` | 后端 Maven 模块，包名 `com.wudgaby.stars` |
| `script/sql/stars_library.sql` | 6 张业务表 + 菜单权限 seed |
| `stars-web/` | 独立前端（Vue3 + TS + Element Plus + Vite，待创建） |

### 参考模块

实现 AI / Kafka 异步模式时，优先对照 `ruoyi-modules/ai-structured/`：

- `config/AiClientConfig.java` — DeepSeek `ChatClient` 配置
- `config/KafkaMessagingConfig.java` — Kafka 工厂
- `messaging/*Producer.java` / `*Consumer.java` — 异步消息模式
- `ai/*Client.java` + `*PromptFactory.java` — LLM 调用与 Prompt

## Active Change: stars-library

**目标：** 为每个 RuoYi 用户提供 GitHub Stars 导入、DeepSeek 中文概述/自动分类标签、标签检索与独立 PC/H5 前端。

**架构摘要：**

```
stars-web (Vue3) ──JWT──► ruoyi-admin + stars-library
                              ├── MySQL (stars_* 表)
                              ├── Kafka (stars.enrichment.request)
                              ├── DeepSeek (Spring AI)
                              └── GitHub REST API (PAT)
```

**MVP 边界（Non-goals）：** 私有仓库、团队共享清单、GitHub OAuth App、Webhook 自动同步、zread MCP 内嵌。

**实现进度（截至文档编写时）：**

- [x] Maven 模块骨架与 POM 注册
- [x] 领域实体、枚举、Mapper 接口
- [x] SQL 迁移脚本
- [ ] Controller / Service / GitHub 客户端
- [ ] DeepSeek enrichment + Kafka
- [ ] `stars-web` 前端
- [ ] 配置块 `stars.*` 写入 application yml

进度以 `openspec/changes/stars-library/tasks.md` 为准；实现前阅读 design 与 specs。

## Tech Stack（自定义模块）

| 层 | 技术 |
|----|------|
| 后端 | Java 17, Spring Boot 3, MyBatis-Plus, Sa-Token |
| 消息 | Spring Kafka |
| AI | Spring AI DeepSeek（`spring-ai-starter-model-deepseek`） |
| 加密 | `ruoyi-common-encrypt`（PAT AES 存储） |
| 前端（stars-web） | Vue 3, TypeScript, Element Plus, Vite, Pinia |
| 观测 | Actuator + Micrometer Prometheus |

## Coding Conventions

### 包与命名

- 框架模块：`org.dromara.*`
- 自定义业务：`com.wudgaby.stars.*`（stars-library）、`com.wudgaby.ticket.*`（ai-structured）
- 表名：`snake_case`，实体 `@TableName` 与表名一致
- 主键：雪花 ID，应用侧分配，**不用** `AUTO_INCREMENT`

### 后端分层

```
controller/     # REST，Sa-Token 权限注解
service/        # 接口 + impl/
domain/         # 实体、bo、vo、enums
mapper/         # MyBatis-Plus Mapper
github/ ai/ messaging/ config/  # 领域专用包
```

### RuoYi 惯例

- Controller 返回 `R<T>` 统一响应
- 分页用 `PageQuery` + `TableDataInfo`
- 权限字符串：`stars:repo:list`、`stars:tag:edit` 等（见 SQL seed）
- 用户隔离：所有查询必须带 `user_id`（从 Sa-Token 登录上下文获取）
- VO 映射：MapStruct-Plus（`@AutoMapper`），编译期生成

### 前端（stars-web，待建）

- API 前缀：`/stars/**`
- 认证：复用 RuoYi 登录，axios 携带 Sa-Token header
- 响应式断点：**768px**（PC 侧边栏 / H5 底部 Tab）
- H5 列表用卡片流，不用复杂表格

## Commands

### 后端

```bash
# 编译 stars-library 模块
mvn -pl ruoyi-modules/stars-library -am compile

# 编译并跳过测试（项目默认 skipTests=true）
mvn -pl ruoyi-admin -am package

# 运行单测（stars-library 有测试时）
mvn -pl ruoyi-modules/stars-library test

# 启动（需本地 MySQL、Redis、Kafka）
mvn -pl ruoyi-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

### 数据库

```bash
# 初始化 Stars 表（在 ry_vue 库执行）
mysql -u root -p ry_vue < script/sql/stars_library.sql
```

### 前端（stars-web 创建后）

```bash
cd stars-web
npm install
npm run dev      # 开发
npm run build    # 生产构建
```

### OpenSpec

使用 `.cursor/skills/openspec-apply-change/SKILL.md` 按变更任务逐步实现；规格位于 `openspec/changes/stars-library/`。

## Working Rules

1. **规格优先：** 改 stars-library 前先读 PRD、OpenSpec design/specs/tasks，避免与已澄清需求冲突（如：DeepSeek 自动应用分类/标签、独立 stars-web 前端）。
2. **参考 ai-structured：** Kafka 消费、DeepSeek JSON 输出、Resilience4j 熔断、Prometheus 指标沿用已验证模式，不要另起炉灶。
3. **最小 diff：** 只改任务相关文件；不重构无关模块；不加过度抽象。
4. **用户隔离：** 所有 stars 数据按 `user_id` 隔离；re-sync 时保留用户手动编辑的 note、tags、manual 字段。
5. **PAT 安全：** GitHub Token 必须 AES 加密存储，日志与 API 响应不得泄露明文。
6. **异步 enrichment：** HTTP 只触发任务；DeepSeek 调用走 Kafka consumer，单用户并发 ≤ 3，失败最多重试 3 次。
7. **README 截断：** 送入 LLM 的 README 最多 3000 字符；完整内容缓存于 `stars_repo.readme_snippet`。
8. **前端独立：** stars-web 不嵌入 plus-ui；PC + H5 同一套代码，条件布局。
9. **不提交密钥：** `.env`、PAT、DeepSeek API Key 不入库；配置走 application yml + 环境变量。
10. **Kami 子项目：** `.agents/skills/kami/` 有独立 AGENTS.md/CLAUDE.md；改 Kami 文档排版时遵循该目录规则，与 RuoYi 业务无关。

## API Surface（规划）

| 域 | 路径前缀 | 说明 |
|----|----------|------|
| GitHub 绑定 | `/stars/github/**` | PAT bind/unbind/status |
| 导入 | `/stars/import/**` | 自己/他人 Stars 导入与任务进度 |
| 仓库 | `/stars/repos/**` | 列表、详情、更新、重新生成概述 |
| 标签 | `/stars/tags/**` | 标签 CRUD、批量关联 |

## Data Model

6 张表（详见 `script/sql/stars_library.sql`）：

| 表 | 用途 |
|----|------|
| `stars_github_account` | 用户 GitHub PAT（加密） |
| `stars_repo` | 全局仓库元数据 + README 缓存 |
| `stars_user_repo` | 用户-仓库关系（category、summary、note） |
| `stars_tag` | 用户标签 |
| `stars_user_repo_tag` | 仓库-标签多对多 |
| `stars_import_job` | 导入任务进度 |

## Current Risk Areas

| 风险 | 缓解 |
|------|------|
| GitHub rate limit | PAT 绑定；分页间隔；任务可暂停续跑 |
| DeepSeek 成本/延迟 | 每用户 enrichment 并发 ≤ 3；README 共享缓存 |
| AI 分类/标签不准 | Prompt taxonomy 约束；用户可编辑；支持 regenerate |
| re-sync 覆盖用户数据 | upsert 保留 manual 字段与 user tags |
| H5 复杂交互 | 手机端卡片流；批量操作仅 PC |
| Kafka 未启动 | 本地 dev 需 Kafka；或文档说明 mock/同步 fallback |

## Verification

### stars-library 后端

```bash
mvn -pl ruoyi-modules/stars-library -am compile
# 有测试后：
mvn -pl ruoyi-modules/stars-library test
```

### 集成验证清单

- [ ] SQL 脚本可在 MySQL 8 干净执行
- [ ] 导入 100 条 mock Stars 任务完成且无重复 `user_id + repo_id`
- [ ] enrichment 成功后 `summary_status=done`，category/tags 自动写入
- [ ] 列表 API P95 ≤ 500ms（1000 条规模，带索引）
- [ ] PC（≥768px）与 H5（375px）页面可完成导入→浏览→编辑流程

### 文档一致性

改 API 或表结构时同步更新：

- `docs/prd/github-stars-knowledge-base.md`
- `openspec/changes/stars-library/specs/*.md`
- `script/sql/stars_library.sql`

## Related Skills

| Skill | 用途 |
|-------|------|
| `.cursor/skills/openspec-apply-change/` | 按 OpenSpec 任务实现 |
| `.cursor/skills/openspec-verify-change/` | 实现完成度验证 |
| `.cursor/skills/ui-ux-pro-max/` | stars-web UI/UX 设计 |
| `.agents/skills/kami/` | 文档 PDF 排版（独立子项目） |
