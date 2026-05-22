# Stars 库 · 原型全局设计说明

| 属性 | 值 |
|------|-----|
| **产品** | GitHub Stars 知识库（stars-web） |
| **依据** | `openspec/changes/stars-library/` |
| **设计系统** | `design-system/stars-library/MASTER.md`（Kami 纸感） |
| **页面 Override** | `design-system/stars-library/pages/*.md` |
| **技术栈** | Vue 3 + TS + Element Plus + Vite |
| **断点** | H5 `<768px` · PC `≥768px` |
| **版本** | v1.1 · Kami |

---

## 1. 功能描述

### 1.1 产品定位

Stars 库是面向开发者的 **GitHub Stars 知识库**：将分散在 GitHub 的 Stars 导入为可检索、可理解、可分类的个人资产库。导入后由 **DeepSeek** 自动撰写中文概述、主分类与标签，并支持跳转 zread / DeepWiki 深度阅读。

### 1.2 核心功能模块

| 模块 | 用户价值 | 对应 Spec |
|------|----------|-----------|
| **认证** | RuoYi 账号登录，数据按用户隔离 | `stars-web-app` |
| **Stars 列表** | 搜索、分类/标签筛选、排序、卡片浏览 | `stars-organization` |
| **导入中心** | 绑定 GitHub PAT、同步自己的 Stars、导入他人 Stars、查看任务进度 | `github-stars-import` |
| **仓库详情** | 查看/编辑概述、分类、标签、收藏理由；外链与重新生成 | `ai-repo-enrichment` + `stars-organization` |
| **标签管理** | 自定义标签 CRUD；PC 完整 / H5 精简 | `stars-organization` |

### 1.3 MVP 边界

- ✅ 公开 Stars；❌ 私有仓库
- ✅ 自动分类/标签（可编辑）；❌ MCP 内嵌
- ✅ PC + H5 响应式；❌ 团队共享清单

### 1.4 页面清单

| # | 页面 | 路由 | PC | H5 |
|---|------|------|----|----|
| P0 | 登录 | `/login` | ✓ | ✓ |
| P1 | Stars 列表（首页） | `/repos` | ✓ | ✓ |
| P2 | 导入中心 | `/import` | ✓ | ✓ |
| P3 | 仓库详情 | `/repos/:id` | ✓ | ✓ |
| P4 | 标签管理 | `/tags` | ✓ | 精简 |

---

## 2. 页面流转

### 2.1 全局路由结构

```mermaid
flowchart TD
    A[/login 登录] -->|成功| B[/repos 列表]
    B --> C[/repos/:id 详情]
    B --> D[/import 导入中心]
    B --> E[/tags 标签管理]
    D -->|导入完成| B
    C -->|保存/返回| B
    E --> B
    F[未登录访问受保护路由] --> A
```

### 2.2 首次使用路径

```
登录 → 导入中心 → 绑定 GitHub PAT → 同步我的 Stars
     → 等待导入进度 → 列表出现卡片（概述：生成中）
     → 概述/分类/标签陆续完成 → 筛选浏览
```

### 2.3 日常使用路径

```
登录 → 列表 → 关键词/分类/标签筛选 → 点击卡片 → 详情
     → [可选] 编辑备注 / 重新生成 / 外链 zread
```

### 2.4 参考他人清单路径

```
导入中心 → 输入 GitHub 用户名 → 确认导入 → 进度页
        → 列表筛选「来源：{username}」→ 浏览 curated list
```

### 2.5 导航模型

| 端 | 主导航 | 次级入口 |
|----|--------|----------|
| **PC** | 左侧固定 Sidebar：列表 / 导入 / 标签 | 顶栏：搜索框、用户菜单 |
| **H5** | 底部 Tab：列表 / 导入 / 标签 | 顶栏：标题 + 筛选抽屉按钮 |

---

## 3. 全局交互与手势

### 3.1 通用交互原则

| 原则 | 说明 |
|------|------|
| **即时反馈** | 所有按钮点击 200ms 内给出 loading 或状态变化 |
| **可撤销感** | 删除标签、重新生成 AI 内容需二次确认 |
| **不阻塞** | 导入与概述撰写异步；用户可继续浏览列表 |
| **空态引导** | 无数据时给出明确 CTA（去导入中心） |
| **错误可恢复** | 网络/Token/限流错误展示原因 + 重试 |

### 3.2 PC 交互

| 场景 | 交互 |
|------|------|
| 列表筛选 | 顶栏搜索框 Enter 搜索；分类/标签多选后立即刷新 |
| 卡片 | Hover 阴影加深 + `cursor-pointer`；点击进入详情 |
| 外链 | 新标签页打开 GitHub / zread / DeepWiki |
| 批量打标 | 列表多选 checkbox → 浮动工具栏「添加标签」 |
| 分页 | 底部分页器；每页 20/50 可选 |

### 3.3 H5 交互与手势

| 场景 | 手势/交互 |
|------|-----------|
| 筛选 | 点击「筛选」→ 底部 Sheet 滑出（分类/标签/来源） |
| 列表 | 纵向卡片流；下拉刷新列表（可选 P1） |
| 详情 | 全屏页；顶栏返回 `<` |
| Tab 切换 | 底部固定 Tab；当前项高亮 |
| 长按 | 不做（MVP）；批量操作为 PC 专属 |
| 安全区 | 底部 Tab 预留 `env(safe-area-inset-bottom)` |

### 3.4 键盘与无障碍

- 所有可点击元素可 Tab 聚焦，可见 focus ring（`#1B365D` 2px outline）
- 表单输入关联 `<label>`
- 图标按钮提供 `aria-label`
- 支持 `prefers-reduced-motion: reduce` 关闭动画

---

## 4. 视觉与布局规范

> 设计 Token 来源：**Kami 纸感**（`design-system/stars-library/MASTER.md`）— 暖羊皮纸、墨蓝单一强调、衬线层级。  
> **禁止：** 冷灰 SaaS 仪表盘、多色强调、紫色「AI 感」、neon pill badge。

### 4.1 色彩

| Token | Hex | 用途 |
|-------|-----|------|
| `--parchment` | `#f5f4ed` | 页面背景 |
| `--ivory` | `#faf9f5` | Sidebar、卡片底 |
| `--warm-sand` | `#e8e6dc` | 分割线、边框 |
| `--brand` | `#1B365D` | 主 CTA、选中态、章节左竖线 |
| `--brand-light` | `#2D5A8A` | CTA hover |
| `--brand-tint` | `#EEF2F7` | 选中筛选底 |
| `--brand-tag` | `#E4ECF5` | Tag 背景（solid） |
| `--near-black` | `#141413` | 主文字 |
| `--dark-warm` | `#3d3d3a` | 次级标题 |
| `--olive` | `#504e49` | 导航文字 |
| `--stone` | `#6b6a64` | 元数据、muted |
| `--status-ok` | `#3d5a4a` | 成功、已完成 |
| `--status-warn` | `#6b5a3d` | 撰写中、部分成功 |
| `--status-fail` | `#6b3d3d` | 失败、删除 |

**移除（旧 AI/SaaS 风）：** `#2563EB` `#7C3AED` `#64748B` `#E2E8F0` `#F8FAFC`

### 4.2 字体

| 用途 | 字体 | 字号 |
|------|------|------|
| 页面/区块标题 | TsangerJinKai02 / Noto Serif SC / 宋体 | 22–24px / 16–18px |
| 正文概述 | 衬线或 PingFang / 微软雅黑 | 14px，行高 1.55 |
| 按钮 / Tab / 筛选 | system-ui sans | 13–14px，字重 500 |
| 元数据 | ui sans | 12px，`--stone` |
| 仓库名 | JetBrains Mono | 13–14px |

```css
:root {
  --serif: "TsangerJinKai02", "Source Han Serif SC", "Noto Serif CJK SC",
           "Songti SC", Georgia, serif;
  --ui: "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
  --mono: "JetBrains Mono", "SF Mono", Consolas, monospace;
}
```

### 4.3 间距与圆角

| Token | 值 |
|-------|-----|
| 页面边距 PC | 24px |
| 页面边距 H5 | 16px |
| 卡片内边距 | 16px |
| 卡片间距 | 12px（H5）/ 16px（PC） |
| 圆角卡片 | 8px（Kami 克制，非 12px SaaS） |
| 圆角按钮 | 6px |
| 圆角 Tag | 4px（**非 pill**） |

### 4.4 布局框架

**PC（≥768px）**

```
┌──────────┬────────────────────────────────────┐
│ Sidebar  │ Topbar: 搜索 + 排序 + 用户          │
│ 200px    ├────────────────────────────────────┤
│          │  Filter chips（分类/标签/来源）       │
│ · 列表   ├────────────────────────────────────┤
│ · 导入   │  Card Grid（3 列）或 Table           │
│ · 标签   │                                    │
│          │  Pagination                        │
└──────────┴────────────────────────────────────┘
```

**H5（<768px）**

```
┌─────────────────────────────┐
│ Topbar: 标题 + 筛选按钮      │
├─────────────────────────────┤
│ 搜索框                       │
├─────────────────────────────┤
│  Card 流（单列）              │
│  ...                         │
├─────────────────────────────┤
│ Tab: 列表 | 导入 | 标签       │
└─────────────────────────────┘
```

### 4.5 图标

- 统一使用 **Lucide 风格 SVG**（24×24 viewBox）
- 禁止用 Emoji 充当图标
- 外链：GitHub / BookOpen(zread) / Layers(DeepWiki)

### 4.6 动效

| 场景 | 时长 | 曲线 |
|------|------|------|
| Hover 背景/边框 | 200ms | ease |
| Sheet 滑入（H5 筛选） | 250ms | ease-out |
| 进度条 | 300ms | linear |
| 骨架屏 shimmer | 1.5s | infinite（可 reduced-motion 关闭） |

---

## 5. 全局业务规则

### 5.1 数据与权限

| 规则 | 说明 |
|------|------|
| 用户隔离 | 仅展示当前登录用户的 Stars 数据 |
| PAT 安全 | 前端永不展示完整 Token；仅显示绑定状态与 GitHub 用户名 |
| 导入去重 | 同一 `owner/repo` 不重复插入 |
| 增量同步 | 再次同步不覆盖用户手动编辑的概述/分类/标签/备注 |

### 5.2 概述撰写展示规则

| `summary_status` | UI 表现 |
|--------------------|---------|
| `pending` | 暖灰标签「等待撰写」+ 骨架一行 |
| `processing` | 墨蓝浅底标签「撰写中」+ Spinner |
| `done` | 展示 `one_liner` + `summary`；左侧 3px 墨蓝竖线 + meta「自动撰写」 |
| `failed` | 暖红标签「撰写失败」+「重试」按钮；fallback 显示 GitHub description |

### 5.3 分类 Taxonomy（预设）

`AI/RAG` · `后端框架` · `前端组件` · `DevOps` · `数据库` · `工具库` · `学习参考` · `待评估`

- 列表筛选以 Chip 展示
- 详情页可下拉修改；修改后标记 `manual`

### 5.4 标签规则

- 每仓库自动标签最多 5 个
- 标签名用户内唯一
- 删除标签解除所有仓库关联
- 批量打标为**追加**，不删除已有标签

### 5.5 导入任务

| 状态 | 文案 | UI |
|------|------|-----|
| `pending` | 排队中 | 进度 0% |
| `running` | 导入中 | 进度条 + 已收录/总数 |
| `done` | 完成 | `--status-ok` 完成态 + 跳转列表 |
| `partial` | 部分失败 | 警告 + 失败数 |
| `failed` | 失败 | 错误原因 + 重试 |

### 5.6 外链

| 名称 | URL 模板 |
|------|----------|
| GitHub | 仓库 `html_url` |
| Zread | `https://zread.ai/{owner}/{repo}` |
| DeepWiki | `https://deepwiki.com/{owner}/{repo}` |

全部 `target="_blank"` + `rel="noopener"`

---

## 6. 通用组件状态

### 6.1 按钮

| 类型 | 样式 | 用途 |
|------|------|------|
| Primary | 墨蓝底白字 `#1B365D` | 主操作：登录、同步、保存 |
| Secondary | ivory 底 + `--warm-sand` 边 | 次要：取消、绑定 PAT |
| Ghost | 透明 + `--brand` 文字 | 外链、重新撰写 |
| Danger | `--status-fail` 文字/浅底 | 解绑、删除标签 |

**状态：** default → hover（`--brand-light`）→ loading（Spinner + disabled）→ disabled（40% opacity）

### 6.2 输入框

| 状态 | 表现 |
|------|------|
| default | 边框 `--warm-sand` |
| focus | 边框 `--brand` + focus ring |
| error | 边框 `--status-fail` + 下方错误文案 |
| disabled | 灰底，不可编辑 |

### 6.3 仓库卡片（RepoCard）

```
┌─────────────────────────────────────────────┐
│ owner/repo                    ★ 12.3k  Java │
│ [AI/RAG] [Spring AI] [RAG]                   │
│ Spring 官方 AI 应用开发框架…（one_liner）      │
│ 提供文档切片、向量检索…（muted）               │
│ 来源：self · 撰写中…                          │
│ [GitHub] [Zread] [DeepWiki]        [详情 →] │
└─────────────────────────────────────────────┘
```

| 状态 | 差异 |
|------|------|
| default | ivory 底，whisper shadow |
| hover | ring 加深，`cursor-pointer` |
| selected | 左边框 3px `--brand`（批量模式） |
| enriching | 概述区 skeleton + 「撰写中」badge |

### 6.4 Tag Chip

| 状态 | 样式 |
|------|------|
| default | 底 `#E4ECF5`，文字 `--dark-warm` |
| selected（筛选） | 底 `#EEF2F7`，文字 `--brand` + 1px `--brand` 边 |
| auto-generated | 与普通 tag 同形；列表卡片用 meta「自动撰写」区分，**不用** ✦ 或紫色 |
| removable | 带 × 按钮 |

### 6.5 进度条（ImportJob）

- 高度 6px，圆角 3px
- 背景 `--warm-sand`，填充 `--brand`
- 文案：`已收录 128 / 500 项` · `失败 2`

### 6.6 空状态（EmptyState）

| 场景 | 插图 | 文案 | CTA |
|------|------|------|-----|
| 无 Stars | 空文件夹 SVG | 目录还是空的 | 去导入中心 |
| 搜索无结果 | 搜索 SVG | 未找到匹配项目，试试其他关键词 | 清除筛选 |
| 未绑定 GitHub | GitHub SVG | 绑定 PAT 后可同步你的 Stars | 绑定 Token |

### 6.7 Toast / Message

| 类型 | 色 | 场景 |
|------|-----|------|
| success | `--status-ok` | 保存成功、绑定成功 |
| error | `--status-fail` | Token 无效、导入失败 |
| warning | `--status-warn` | 部分导入失败 |
| info | `--brand` | 任务已创建，后台处理中 |

---

## 7. 原型交付物

| 文件 | 说明 |
|------|------|
| 本文档 | 全局设计说明 |
| [`prototype-all-pages.html`](prototype-all-pages.html) | **全页面平铺原型**（PC + H5 画板，Kami 纸感） |
| `design-system/stars-library/MASTER.md` | Kami 设计 Token Master |
| `design-system/stars-library/pages/*.md` | 页面级 Override（login / repos / import / detail / tags / states） |

---

## 8. 与 OpenSpec 映射

| Spec 能力 | 设计覆盖 |
|-----------|----------|
| `stars-web-app` | §1 页面清单、§2 流转、§4 布局、§6 组件 |
| `github-stars-import` | P2 导入中心、§5.5 任务规则 |
| `ai-repo-enrichment` | §5.2 概述撰写状态、P3 详情重新撰写 |
| `stars-organization` | P1 列表筛选、P4 标签、§5.3–5.4 |

---

## 9. 下一步

1. 设计评审 → 确认 Kami 原型
2. 将 Token 写入 `stars-web` CSS 变量 / Element Plus theme override（见 MASTER §8）
3. 按 `pages/*.md` 逐页落地 Vue 组件
