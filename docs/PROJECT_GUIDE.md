# 项目理解指南

> **用途：** 供人与 AI 快速建立对仓库的一致认知。  
> **维护：** 功能/架构/命令变更时 **务必** 同步更新本文件；详细 Agent 规则见根目录 [`AGENTS.md`](../AGENTS.md)。  
> **最后核对：** 2026-05-22（以 `pom.xml` revision、`stars-library` 现状为准）

---

## 1. 项目是什么

| 项 | 说明 |
|----|------|
| 基座 | [RuoYi-Vue-Plus](https://gitee.com/dromara/RuoYi-Vue-Plus) 5.6.1 — 多租户 Spring Boot 3 后台 |
| 本 fork 重点 | **GitHub Stars 知识库**（`stars-library` + 独立前端 `stars-web`） |
| 官方文档 | [plus-doc.dromara.org](https://plus-doc.dromara.org) |
| 规格来源 | PRD → OpenSpec → 代码（**禁止** 跳过规格直接改行为） |

---

## 2. 技术栈

### 后端（仓库内）

| 类别 | 技术 | 版本/说明 |
|------|------|-----------|
| 语言 | Java | **17**（兼容 21） |
| 框架 | Spring Boot | **3.5.14** |
| ORM | MyBatis-Plus | 3.5.x |
| 认证 | Sa-Token + JWT | 权限注解 `@SaCheckPermission` |
| 缓存/锁 | Redis + Redisson | 本地 dev 必启 |
| 消息 | Spring Kafka | stars enrichment 异步 |
| AI | Spring AI DeepSeek | 环境变量 `DEEPSEEK_API_KEY` |
| 主键 | 雪花 ID | **禁止** 表级 `AUTO_INCREMENT` |
| 参考实现 | `ruoyi-modules/ai-structured` | Kafka / LLM / 熔断模式 |

### 前端

| 应用 | 路径 | 技术 | 说明 |
|------|------|------|------|
| 管理端（官方） | 独立仓库 [plus-ui](https://gitee.com/JavaLionLi/plus-ui) | Vue3 + TS + Element Plus | **不在** 本仓库 |
| Stars 业务端 | `stars-web/` | Vue 3.5、Vite 8、TS 6、Element Plus 2、Pinia 3 | **必须** 独立部署，不嵌入 plus-ui |
| 响应式 | — | 断点 **768px** | PC 侧栏 / H5 底部 Tab + 卡片流 |

---

## 3. 目录结构（精简）

```
RuoYi-Vue-Plus/
├── ruoyi-admin/              # 启动入口（聚合模块）
├── ruoyi-common/             # 公共能力（security、mybatis、redis、encrypt…）
├── ruoyi-modules/
│   ├── ruoyi-system/         # 系统管理
│   ├── ai-structured/        # AI+Kafka 参考模块
│   └── stars-library/        # Stars 业务（com.wudgaby.stars）
├── ruoyi-extend/             # 监控、SnailJob 等
├── stars-web/                # Stars 独立前端
├── script/sql/               # 库表脚本（含 stars_library.sql）
├── script/docker/            # MySQL / Redis / Nginx / Kafka 编排
├── docs/                     # PRD、本指南、实现计划
└── openspec/changes/         # 变更规格（stars-library）
```

**自定义包命名：** 框架 `org.dromara.*` · 业务 `com.wudgaby.stars.*` / `com.wudgaby.ticket.*`

---

## 4. 常用命令

### 4.1 构建

```bash
# 全量打包（默认 skipTests=true）
mvn -pl ruoyi-admin -am package

# 仅编译 Stars 模块
mvn -pl ruoyi-modules/stars-library -am compile
```

### 4.2 测试

```bash
# 显式跑 stars-library 单测（覆盖根 POM 的 skipTests）
mvn -pl ruoyi-modules/stars-library test

# 框架自带单测（按需指定模块）
mvn -pl ruoyi-modules/ruoyi-system test
```

### 4.3 本地运行

```bash
# 后端（profile: dev；依赖 MySQL、Redis、Kafka）
mvn -pl ruoyi-admin spring-boot:run -Dspring-boot.run.profiles=dev

# 前端
cd stars-web && npm install && npm run dev
```

### 4.4 数据库

```bash
# Stars 表 + 菜单权限（库名与 application-dev.yml 一致，默认 ry-vue）
mysql -u root -p ry-vue < script/sql/stars_library.sql
```

### 4.5 部署 / 基础设施

```bash
# Docker 基础环境（MySQL、Redis、Nginx 等，见 script/docker/docker-compose.yml）
cd script/docker && docker compose -f docker-compose.yml up -d

# Kafka（Stars enrichment 需要）
docker compose -f script/docker/docker-compose-kafka.yml up -d

# 生产包：ruoyi-admin/target/ruoyi-admin.jar
# 前端：stars-web 执行 npm run build，静态资源由 Nginx 反代 API
```

**环境变量（Stars，禁止提交仓库）：**

| 变量 | 用途 |
|------|------|
| `DEEPSEEK_API_KEY` | DeepSeek API |
| `STARS_TOKEN_ENCRYPT_KEY` | GitHub PAT AES 密钥（32 字节） |

---

## 5. 代码风格指南

| 层级 | 约定 |
|------|------|
| Java | 遵守 Alibaba 规范；Lombok + Hutool；**禁止** 在 Controller 写复杂业务 |
| 分层 | `controller` → `service`/`impl` → `mapper`；领域包 `github/` `ai/` `messaging/` |
| API | 统一 `R<T>`；分页 `PageQuery` + `TableDataInfo` |
| VO | MapStruct-Plus（`@AutoMapper`），编译期生成 |
| SQL | 表名 `snake_case`；`@TableName` 与表一致；变更 **必须** 带迁移脚本 |
| 前端 | TS 严格类型；API 集中在 `stars-web/src/api/`；状态 Pinia |
| 提交 | **禁止** 提交 `.env`、PAT、API Key；日志 **禁止** 打印 token 明文 |

---

## 6. 开发规范与注意事项

### 6.1 必须遵守（MUST）

1. **规格优先：** 改 `stars-library` / `stars-web` 前读 `docs/prd/github-stars-knowledge-base.md` 与 `openspec/changes/stars-library/`。
2. **用户隔离：** 所有 Stars 查询/写入 **必须** 带当前登录 `user_id`（Sa-Token 上下文）。
3. **PAT 安全：** GitHub Token **必须** AES 加密落库；API 响应与日志 **不得** 回显明文。
4. **异步 enrichment：** HTTP 只投递任务；DeepSeek **必须** 走 Kafka Consumer；单用户并发 **≤ 3**；失败重试 **≤ 3**。
5. **re-sync 保护：** 导入 upsert **必须** 保留用户手改的 note、tags、manual 分类字段。
6. **README 截断：** 送入 LLM **最多 3000 字符**；完整片段存 `stars_repo.readme_snippet`。
7. **最小 diff：** 只改任务相关文件；**禁止** 顺手重构无关模块。

### 6.2 禁止事项（MUST NOT）

- **禁止** 将 `stars-web` 并入 plus-ui 或共用路由壳。
- **禁止** 在 MVP 引入：私有仓、团队共享清单、GitHub OAuth App、Webhook 自动同步（见 PRD Non-goals）。
- **禁止** 表结构/API 变更而不同步 PRD、OpenSpec spec、`script/sql/stars_library.sql`。

### 6.3 建议（SHOULD）

- 新异步/AI 能力 **优先** 复制 `ai-structured` 模式，避免新造轮子。
- 列表 API 关注索引与 P95（千级数据 ≤ 500ms，见 PRD）。
- 本地无 Kafka 时：文档化 fallback 或启动 `docker-compose-kafka.yml`。

---

## 7. 分层配置（团队协作）

配置按 **环境 → 模块 → 密钥** 三层拆分，避免单人本地配置污染团队默认值。

| 层级 | 位置 | 职责 |
|------|------|------|
| 环境 | `application-{local,dev,prod}.yml` | 数据源、Redis、Kafka 地址 |
| 模块 | `stars.*`（`application-dev.yml`） | GitHub 分页、Kafka topic、并发/重试、深链模板 |
| 密钥 | 环境变量 / CI Secret | `DEEPSEEK_API_KEY`、`STARS_TOKEN_ENCRYPT_KEY` |

```yaml
# 模块配置示例（勿把真实密钥写入 yml）
stars:
  import-config:
    default-limit: 100   # 未传 limit 时导入最近 100 条 Star
    max-limit: 5000
  github:
    api-base: https://api.github.com
    token-encrypt-key: ${STARS_TOKEN_ENCRYPT_KEY}
  summary:
    kafka:
      request-topic: stars.enrichment.request
      max-poll-records: 10   # 单次 poll 批量条数，整批处理完再提交 offset
    max-concurrent-per-pod: 5    # 单 Pod Kafka 监听线程 / 本机槽位
    max-concurrent-global: 30  # 全集群 DeepSeek 并发（Redis 信号量）
    max-concurrent-per-user: 3
    readme-max-chars: 3000
```

**协作约定：**

- 默认值以 **dev profile + 本指南** 为准；个人覆盖用 `application-local.yml`（**不提交**）或 IDE 环境变量。
- 改 `stars.*` 或表结构 → **同时** 更新 OpenSpec + 本文件「常用命令/注意事项」相关小节。

---

## 8. 文档同步清单（改完功能请勾选）

- [ ] `docs/prd/github-stars-knowledge-base.md`
- [ ] `openspec/changes/stars-library/specs/*.md` 与 `tasks.md`
- [ ] `script/sql/stars_library.sql`
- [ ] **本文件** `docs/PROJECT_GUIDE.md`
- [ ] `AGENTS.md`（Agent 专用规则与进度）

---

## 9. 快速验证

```bash
mvn -pl ruoyi-modules/stars-library -am compile
mvn -pl ruoyi-modules/stars-library test
cd stars-web && npm run build
```

集成场景：绑定 PAT → 导入 Stars → 等待 enrichment → 列表筛选 / 编辑标签 → PC 与 H5（375px / 768px）可走通主路径。

---

## 10. 延伸阅读

| 文档 | 内容 |
|------|------|
| [`AGENTS.md`](../AGENTS.md) | AI/开发者仓库地图、风险区、验证命令 |
| [`docs/prd/github-stars-knowledge-base.md`](prd/github-stars-knowledge-base.md) | 产品需求与 API 契约 |
| [`openspec/changes/stars-library/`](../openspec/changes/stars-library/) | 设计与任务分解 |
| [`docs/superpowers/plans/2026-05-22-stars-library.md`](superpowers/plans/2026-05-22-stars-library.md) | 分阶段实现计划 |
