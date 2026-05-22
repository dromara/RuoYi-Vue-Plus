# Page Override: 导入中心 `/import`

## 信息架构

按 **章节** 划分（Kami section + left bar），非 wizard 步骤条：

1. **GitHub 连接** — PAT 绑定状态
2. **我的 Stars** — 同步按钮
3. **他人清单** — 用户名输入 + 导入
4. **进行中的任务** — 进度列表

## 绑定态

- 已绑定：ivory 面板 + 左边线 green-warm `#3d5a4a` 细线（非 bright green badge）
- 显示 `@github_login`，解绑为 ghost 危险文字链

## 未绑定态

- 说明文字：`--olive`，链接到「如何创建 PAT」
- Token 输入：`type=password`，monospace 字段

## 进度

- 进度条：track `--warm-sand`，fill `--brand`（高度 6px，非 chunky blue bar）
- 文案：`已收录 320 / 500 项` · `失败 0`（`--stone`）

## 导入他人

- 输入框 placeholder：`GitHub 用户名，如 torvalds`
- 主按钮「收录清单」

## H5

- 各 section 折叠为独立 ivory 卡片
- 同步按钮 sticky 在首屏卡片内，不悬浮

## 错误

- Rate limit：暖色 alert 面板，文案说明「GitHub 限流，任务将自动续跑」
