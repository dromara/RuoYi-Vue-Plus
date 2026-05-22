# Page Override: Stars 列表 `/repos`

> 首页 / 核心浏览页。编辑型目录，非数据大盘。

## 布局（PC）

- 左 Sidebar + 右主区（见 MASTER）
- 顶栏：**一条**搜索框（暖边 ivory 底），右：排序下拉 + 用户
- 筛选区：分类 Chip 一行 + 标签 Chip 一行；用 `section-title`「筛选」
- 列表：**双列卡片**（宽屏）或单列（窄 PC）；**不用** dense table 为默认

## 布局（H5）

- 顶栏标题「Stars 目录」+ 筛选按钮（打开 Sheet）
- 搜索框全宽
- 单列卡片流
- 底部 Tab

## 卡片内容优先级

1. 仓库名（mono）
2. 元数据：Star 数 · 语言 · 来源（`--stone` 一行）
3. **概述一句**（serif，`--dark-warm`）— 左竖线 2px brand 表「已概述」
4. 标签行（`--brand-tag` chips，最多显示 3 +「+N」）
5. 底栏：文字链接 GitHub / Zread / DeepWiki（非实心按钮堆）
6. 右下：「阅读 →」ghost 链接

## 概述状态（去 AI 风）

| status | 展示 |
|--------|------|
| pending | 灰字「等待撰写概述」 |
| processing | 「正在撰写概述…」+ 细进度条 |
| done | 显示概述文本 |
| failed | 「概述未完成」+ 链接「重试」 |

## 空态

- 插图：线框文件夹 SVG（stroke `--stone`）
- 文案衬线：「目录还是空的」
- CTA：「去导入 Stars」

## 搜索无结果

- 建议文案：「试试更短的关键词，或清除筛选」
- 不用冷冰冰的 "0 results"

## 批量操作（仅 PC）

- 多选后出现 **底部浮动条** ivory 底，非 Material FAB
