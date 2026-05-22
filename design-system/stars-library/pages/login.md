# Page Override: 登录 `/login`

> 覆盖 MASTER 的登录页专项规则。

## 布局

- **单栏居中**，最大宽度 400px，垂直居中于视口
- 背景：`--parchment`，可选极淡纸纹（无需图片，用 `linear-gradient` 暖色即可）
- **不用** 全屏蓝紫渐变

## 内容结构

1. 品牌区：衬线标题「Stars 库」+ 副标题「整理你的 GitHub Stars」（`--olive`）
2. 表单：用户名、密码、验证码（RuoYi 标准）
3. 主按钮「进入」：`btn-primary` 全宽
4. 页脚小字：链接到帮助「如何获取账号」（`--stone`）

## 字体

- 标题：`--serif` 24px
- 表单 label：`--ui` 12px uppercase tracking，`--stone`
- 输入框：`--ui` 14px

## 交互

- 输入 focus：边框 `--brand`，ring `color-mix(brand 20%)`
- 验证码图：暖沙底 `#e8e6dc`，非冷灰块

## H5

- 卡片占满宽度 minus 32px padding
- 键盘弹起时按钮保持可见（`padding-bottom: env(safe-area-inset-bottom)`）
