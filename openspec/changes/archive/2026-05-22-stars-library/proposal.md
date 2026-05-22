## Why

RuoYi 平台开发者用户的 GitHub Stars 规模从几十到上千不等，原生 Stars 列表缺少中文概述、场景化分类与标签，导致检索慢、理解成本高。现需在 RuoYi-Vue-Plus 中新增 **Stars 知识库模块**，在导入时通过 **DeepSeek** 自动生成概述、分类与标签，并以 **Vue3 + TS + Element Plus + Vite** 独立前端支持 PC 与 H5 访问。

## What Changes

- 新增后端模块 `ruoyi-modules/stars-library`：GitHub Stars 导入（自己 / 他人）、用户级数据隔离、异步任务
- 导入完成后通过 **DeepSeek** 异步生成：中文概述、**主分类（category）**、**标签（tags）**，并**自动应用到用户仓库记录**
- 用户可编辑/覆盖 AI 生成的分类、标签与概述
- 新增独立前端项目 `stars-web`：**Vue3 + TypeScript + Element Plus + Vite**，响应式布局支持 **PC + H5**
- 提供标签筛选、关键词搜索、备注、外链（GitHub / zread / DeepWiki）
- MVP 不包含：私有仓库、团队共享、zread MCP 内嵌

## Capabilities

### New Capabilities

- `github-stars-import`: GitHub 账号绑定（PAT）、导入/同步自己的 Stars、导入他人公开 Stars、异步导入任务与进度
- `ai-repo-enrichment`: DeepSeek 驱动的中文概述、自动分类与标签生成；导入后自动应用；失败重试与手动覆盖
- `stars-organization`: 用户标签 CRUD、分类/标签/关键词筛选、收藏备注、深度探索外链
- `stars-web-app`: Vue3 + TS + Element Plus + Vite 前端；JWT 对接后端；PC 与 H5 响应式 UI

### Modified Capabilities

（无现有 spec，留空）

## Impact

- **后端**：新增 `stars-library` Maven 模块；`ruoyi-admin` 引入依赖；新增 6 张数据表；Kafka 异步队列；Spring AI DeepSeek 集成（复用 `ai-structured` 模式）
- **前端**：新建 `stars-web/` 独立 Vite 项目；对接 RuoYi 认证与 Stars API
- **配置**：GitHub API、DeepSeek API Key、Kafka Topic、外链模板
- **文档**：`docs/prd/github-stars-knowledge-base.md` 已更新至 v1.1
