# Page Override: 通用组件状态

> 跨页面组件；实现 `stars-web` 时统一引用。

## 按钮

| 变体 | 背景 | 文字 |
|------|------|------|
| primary | `--brand` | `--ivory` |
| secondary | `--ivory` | `--brand` + border `--border` |
| ghost | transparent | `--brand-light` |
| danger-ghost | transparent | `--status-fail` |

Loading：按钮内文字保留，左侧 14px spinner（brand 15% track）

## 输入框

- 背景 `--ivory`，边框 `--border`
- focus：`--brand` border + soft ring
- error：`--status-fail` border + 下方 12px 说明

## Toast

- 位置：PC 右上 / H5 顶部居中
- 背景 `--near-black` 90% 上叠 ivory 边（或 ivory 底 + border）
- **不用** Material 彩色 snackbar

## Sheet（H5 筛选）

- 从底滑出，圆角顶 12px，背景 `--ivory`
- backdrop：`rgba(20,19,19,.25)`

## 分页

- 文字式「上一页 / 下一页」+ 页码，`--stone`
- 不用 heavy EP pagination 默认蓝

## 外链按钮

- 统一 `link-quiet`：无 border 实心块，下划线 hover
- 图标 16px stroke `--stone` → hover `--brand`

## 分类 Taxonomy Chips

固定顺序展示：`AI/RAG` `后端框架` `前端组件` `DevOps` `数据库` `工具库` `学习参考` `待评估`

## 骨架屏

- 暖灰 shimmer：`#e8e6dc` → `#f5f4ed`（非 cold gray）
