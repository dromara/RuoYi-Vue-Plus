# Stars Library — Design System Master (Kami)

> **风格锚点：** [Kami 紙](https://github.com/tw93/kami) — 暖色羊皮纸、墨蓝单一强调色、衬线承载层级、编辑级留白。  
> **禁止：** 冷灰 SaaS 仪表盘、多色强调、紫色「AI 感」、硬阴影、纯平 AI 渐变。

**Project:** Stars Library · `stars-web`  
**Locale:** zh-CN  
**Override 规则：** 构建具体页面时，先读 `design-system/stars-library/pages/[page].md`，存在则覆盖本文。

---

## 1. Kami 十项不变量（Web 适配）

| # | 规则 | Web 实现 |
|---|------|----------|
| 1 | 页面背景羊皮纸 `#f5f4ed` | `html, body { background: var(--parchment) }` |
| 2 | 单一强调色墨蓝 `#1B365D` | 主按钮、选中态、章节左竖线；**禁止**第二 chromatic 色 |
| 3 | 全部暖灰，无冷灰 | 禁用 `#64748B` `#E2E8F0` `#F8FAFC` 等 slate 系 |
| 4 | 中文：标题衬线，正文/UI 标签可用系统 sans | 标题 `var(--serif)`；按钮/标签/meta 可用 PingFang/system-ui |
| 5 | 衬线字重 500，不用 bold | `font-weight: 500` |
| 6 | 行高：标题 1.2–1.3，正文 1.5–1.55 | 中文正文 `letter-spacing: 0.3pt` |
| 7 | Tag 背景必须 solid hex | `#E4ECF5` / `#EEF2F7`，禁止 rgba tag |
| 8 | 深度用 ring / whisper shadow | `0 1px 0 var(--border-soft), 0 8px 24px rgba(20,19,19,.06)` |
| 9 | 墨蓝面积 ≤5% 画面 | 克制用于 CTA 与 accent bar |
| 10 | 不用 emoji 作图标 | SVG line icons，1.5px stroke |

---

## 2. Color Tokens

```css
:root {
  --parchment:    #f5f4ed;
  --ivory:        #faf9f5;
  --warm-sand:    #e8e6dc;
  --brand:        #1B365D;
  --brand-light:  #2D5A8A;
  --brand-tint:   #EEF2F7;
  --brand-tag:    #E4ECF5;
  --near-black:   #141413;
  --dark-warm:    #3d3d3a;
  --olive:        #504e49;
  --stone:        #6b6a64;
  --border:       #e8e6dc;
  --border-soft:  #e5e3d8;
  /* 状态色：暖调、低饱和，非 neon */
  --status-ok:    #3d5a4a;
  --status-warn:  #6b5a3d;
  --status-fail:  #6b3d3d;
}
```

**移除（旧 AI 风）：** `#2563EB` `#7C3AED` `#16A34A` pill badges `#0F172A` dark theme

---

## 3. Typography

```css
:root {
  --serif: "TsangerJinKai02", "Source Han Serif SC", "Noto Serif CJK SC",
           "Songti SC", Georgia, serif;
  --ui: "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
  --mono: "JetBrains Mono", "SF Mono", Consolas, monospace;
}
```

| 用途 | 字体 | 字号 |
|------|------|------|
| 页面标题 / 区块标题 | serif 500 | 22–24px / 16–18px |
| 仓库名 | mono 500 | 14px |
| 正文概述 | serif 或 ui | 14px，行高 1.55 |
| 按钮 / Tab / 筛选标签 | ui 500 | 13–14px |
| 元数据（Star 数、日期） | ui | 12px，`--stone` |

---

## 4. Layout

| Token | PC | H5 |
|-------|----|----|
| 断点 | ≥768px | <768px |
| 侧边栏 | 200px，ivory 底 + 右边线 | — |
| 底栏 Tab | — | 52px + safe-area |
| 内容 max-width | 1120px 居中 | 100% - 32px padding |
| 区块间距 | 32px | 20px |
| 卡片间距 | 16px | 12px |

---

## 5. Components (Kami)

### 5.1 Section Title（章节标题）

```css
.section-title {
  font-family: var(--serif);
  font-weight: 500;
  font-size: 17px;
  color: var(--near-black);
  padding-left: 12px;
  border-left: 3px solid var(--brand);
  margin-bottom: 16px;
}
```

### 5.2 Primary Button

```css
.btn-primary {
  background: var(--brand);
  color: var(--ivory);
  border: none;
  border-radius: 6px;
  padding: 10px 18px;
  font-family: var(--ui);
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-primary:hover { background: var(--brand-light); }
```

### 5.3 Card（仓库卡片）

```css
.repo-card {
  background: var(--ivory);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 18px 20px;
  box-shadow: 0 1px 0 var(--border-soft);
  transition: box-shadow 0.2s, border-color 0.2s;
  cursor: pointer;
}
.repo-card:hover {
  border-color: color-mix(in srgb, var(--brand) 25%, var(--border));
  box-shadow: 0 8px 24px rgba(20, 19, 19, 0.06);
}
```

### 5.4 Tag Chip

```css
.chip {
  background: var(--brand-tag);
  color: var(--brand);
  border: 1px solid var(--border-soft);
  border-radius: 4px; /* Kami: 小圆角，非 pill 荧光 */
  padding: 2px 10px;
  font-size: 12px;
  font-family: var(--ui);
}
.chip.selected {
  background: var(--brand);
  color: var(--ivory);
  border-color: var(--brand);
}
```

### 5.5 AI 概述标识（去 AI 紫）

- **不用** ✦ 紫色前缀
- 用左侧 2px `--brand` 竖线 + 小字 meta「自动概述」`--stone`
- 生成中：文字「正在撰写概述…」+ 细线 indeterminate bar（brand 15% 透明度）

### 5.6 Sidebar Nav

- 选中：左边 3px `brand` bar + `--brand-tint` 背景
- 未选中：`--olive` 文字

---

## 6. Anti-Patterns

- ❌ 冷灰蓝 SaaS 仪表盘
- ❌ 多色 Tag / 彩虹分类
- ❌ 紫色「AI / 智能」强调
- ❌ Fira Code 全局标题（仅仓库名 mono）
- ❌ 大圆角 pill + neon badge
- ❌ 深色 header 文档 chrome

---

## 7. Page Overrides Index

| 页面 | Override 文件 |
|------|---------------|
| 登录 | `pages/login.md` |
| Stars 列表 | `pages/repos-list.md` |
| 导入中心 | `pages/import.md` |
| 仓库详情 | `pages/repo-detail.md` |
| 标签管理 | `pages/tags.md` |
| 组件状态 | `pages/components-states.md` |

---

## 8. Element Plus 主题映射（实现时）

| EP Token | Kami 值 |
|----------|---------|
| `--el-color-primary` | `#1B365D` |
| `--el-bg-color` | `#f5f4ed` |
| `--el-bg-color-overlay` | `#faf9f5` |
| `--el-border-color` | `#e8e6dc` |
| `--el-text-color-primary` | `#141413` |
| `--el-text-color-regular` | `#504e49` |
