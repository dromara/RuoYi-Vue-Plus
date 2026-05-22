# Page Override: 仓库详情 `/repos/:id`

## 气质

**一篇短评 / 藏书卡片**，不是 CRUD 表单页。

## 结构（自上而下）

1. **顶栏**：← 返回 · 仓库名 mono · 元数据
2. **概述**（section-title「概述」）
   - 一句：衬线较大字号
   - 正文：3 行以内，`--olive`
   - meta：「自动撰写 · 可编辑」或「已手动修改」`--stone`
3. **分类** — 下拉或单选 Chip taxonomy
4. **标签** — chips + 添加
5. **收藏备注** — textarea，placeholder「为何收藏此项目…」
6. **延伸阅读** — 三个文字链（GitHub / Zread / DeepWiki），横排
7. **操作区**：「保存修改」primary · 「重新撰写概述」secondary

## 编辑 vs 只读

- 默认概览只读样式；点「编辑」进入编辑态（PC）
- H5：字段始终可编辑，底部 fixed 保存栏 ivory 底

## 重新生成

- 二次确认对话框：衬线标题「重新撰写概述？」说明将覆盖当前概述与自动标签

## 禁止

- 紫色 AI 按钮
- 大段 JSON / 调试信息
