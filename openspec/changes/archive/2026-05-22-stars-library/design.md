## Context

RuoYi-Vue-Plus 已有 `ai-structured` 模块，验证了 Spring AI + DeepSeek + Kafka 异步处理模式。本次新增 `stars-library` 后端模块与独立 `stars-web` 前端，解决 GitHub Stars 检索与理解问题。

**需求澄清（v1.1）：**
1. 导入 Stars 时，DeepSeek **自动生成并应用**分类（category）与标签（tags）
2. 前端独立搭建：**Vue3 + TS + Element Plus + Vite**，支持 **PC + H5**
3. 概述与分类/标签统一使用 **DeepSeek** 模型

参考文档：`docs/prd/github-stars-knowledge-base.md`

## Goals / Non-Goals

**Goals:**
- 后端模块 `stars-library`：导入、组织、DeepSeek  enrichment、REST API
- 前端 `stars-web`：响应式 PC/H5 UI，对接 RuoYi 认证
- 单用户 5000 条 Stars；导入与 enrichment 异步化
- 导入后自动分类/标签，用户可编辑

**Non-Goals:**
- 私有仓库
- zread MCP 内嵌、DeepWiki 自托管
- 团队共享清单
- GitHub OAuth App（MVP 用 PAT）
- 自动 Webhook 同步 Stars 变更

## Decisions

### D1: 后端模块位置 — `ruoyi-modules/stars-library`

**选择：** 新建 Maven 子模块，包名 `com.wudgaby.stars`，挂载到 `ruoyi-admin`。

**理由：** 与 `ai-structured` 并列，复用 RuoYi 用户体系与权限；Stars 是独立业务域，不宜塞入 `ruoyi-system`。

**备选：** 独立微服务 — 过重，MVP 不需要。

### D2: 前端 — 独立 `stars-web/` Vite 项目

**选择：** 仓库根目录新建 `stars-web/`，Vue3 + TS + Element Plus + Vite。

**理由：** 用户明确要求独立前端 + H5；与 RuoYi 管理后台解耦，UI 可针对 Stars 场景优化。

**PC/H5 策略：**
- CSS 断点 `768px`
- Element Plus 响应式栅格 + 条件渲染导航（侧边栏 vs 底部 Tab）
- 列表页 H5 用卡片流，PC 可选表格/卡片切换

**备选：** 嵌入现有 plus-ui — 用户已否决。

### D3: AI 模型 — DeepSeek only

**选择：** Spring AI `DeepSeekChatModel` / `deepseekChatClient`（参考 `ai-structured` 的 `AiClientConfig`）。

**范围：** 单次 enrichment 调用输出结构化 JSON：
```json
{
  "one_liner": "...",
  "summary": "...",
  "category": "AI/RAG",
  "tags": ["Spring AI", "RAG", "参考实现"]
}
```

**理由：** 用户指定 DeepSeek；一次调用同时生成概述+分类+标签，降低成本与延迟。

**备选：** 概述与分类分两次调用 — 延迟与成本更高，不采纳。

### D4: 分类与标签自动应用策略

**选择：**
- `category`：存于 `stars_user_repo.category`（单值）
- `tags`：DeepSeek 返回的标签名 → 查找或创建 `stars_tag` → 写入 `stars_user_repo_tag`
- `classification_source = ai`；用户手动改后为 `manual`
- 预设 taxonomy 写入 Prompt，模型优先选择；无匹配时允许自由文本 category

**理由：** 导入即可浏览分类/标签，无需用户逐步确认（PRD 要求自动应用）。

**备选：** 仅「建议标签」待用户确认 — 与澄清需求不符。

### D5: 异步架构 — Kafka 双 Topic

**选择：**
- `stars.import.job` — 导入分页任务（可选，或 import service 内同步分页 + DB job 表）
- `stars.enrichment.request` — 每条 user_repo 的 DeepSeek enrichment

**Consumer：** 参考 `TicketAnalysisConsumer`，Resilience4j 熔断，Prometheus 指标。

**理由：** 与现有 `ai-structured` 一致；导入 1000 条时不阻塞 HTTP。

### D6: 数据模型 — 全局 repo 缓存 + 用户关系表

**选择：** `stars_repo`（全局元数据 + README 缓存）+ `stars_user_repo`（用户维度：category、summary、note、import_source）。

**理由：** 多用户导入同一 `owner/repo` 时共享 GitHub 元数据与 README，减少 API 与 LLM 重复输入（enrichment 仍 per-user，因 note/tags 可能不同；README 缓存可共享）。

**优化：** 若同一 `repo_id` 已有近期 AI 结果且用户未 manual 覆盖，可考虑复制 summary/category/tags（P1）；MVP 每用户独立 enrichment。

### D7: GitHub 集成 — PAT MVP

**选择：** 用户绑定 PAT，加密存储（AES）；导入他人 Stars 可用服务端 fallback token 或用户 PAT 提高限额。

**理由：** 实现快；OAuth App 放 P1。

### D8: 认证 — 复用 RuoYi Sa-Token

**选择：** `stars-web` 调用 `ruoyi-admin` 登录接口获取 token；Stars API 路径 `/stars/**` 走同一鉴权。

**理由：** 多用户隔离依赖 RuoYi `user_id`。

## Architecture

```
┌─────────────┐     HTTPS/JWT      ┌──────────────────┐
│  stars-web  │ ◄──────────────► │  ruoyi-admin     │
│ Vue3+Vite   │                  │  + stars-library │
│ PC / H5     │                  └────────┬─────────┘
└─────────────┘                           │
                    ┌─────────────────────┼─────────────────────┐
                    ▼                     ▼                     ▼
              ┌──────────┐         ┌──────────┐         ┌──────────┐
              │  MySQL   │         │  Kafka   │         │ DeepSeek │
              │ stars_*  │         │ enrich   │         │   API    │
              └──────────┘         └──────────┘         └──────────┘
                    │
                    ▼
              ┌──────────┐
              │ GitHub   │
              │ REST API │
              └──────────┘
```

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| DeepSeek 批量 enrichment 成本高 | 单 Pod 并发≤5、集群≤30（Redis）；每用户≤3；README 缓存；失败重试上限 3 |
| AI 分类/标签不准确 | taxonomy 约束 Prompt；用户可编辑；支持重新生成 |
| GitHub rate limit | PAT 绑定；分页间隔；任务暂停续跑 |
| H5 复杂表格体验差 | 手机端强制卡片流；批量操作仅 PC |
| 1000+ 条首次导入全量 AI | 进度条分阶段：导入完成 → enrichment 队列进度 |
| 自动创建过多标签 | 合并同义标签（P1）；MVP 限制每 repo ≤5 AI 标签 |

## Migration Plan

1. 执行 SQL 脚本创建 `stars_*` 表
2. 部署 `stars-library` 模块到 `ruoyi-admin`
3. 配置 DeepSeek API Key、Kafka Topic、GitHub 相关配置
4. 构建部署 `stars-web`（Nginx 反代 `/api` → ruoyi-admin）
5. 注册 RuoYi 菜单与权限（可选，若从管理端跳转）
6. 回滚：关闭模块依赖；前端下线；表保留

## Open Questions

| # | 问题 | 暂定 |
|---|------|------|
| Q1 | enrichment 结果跨用户复用？ | MVP 不复用，每 user_repo 独立调用 |
| Q2 | `stars-web` 与 admin 同域还是子域？ | 建议同域 Nginx 反代，避免 CORS |
| Q3 | 重新生成是否覆盖 manual 字段？ | MVP 全覆盖 AI 字段，前端二次确认（P1 可做选择性保留） |
