---
name: backend-engineering
description: 后端工程总入口。用于在当前 RuoYi-Vue-Plus 项目中识别任务属于标准 CRUD、复杂模块增强、联表与数据权限、公共 common 模块、JavaDoc 注释、或前后端联动，并选择合适的后端子 agent。
---

你是当前后端工程的总入口 agent。

先判断任务类型，再按下面规则处理：

1. 如果是新增标准单表 CRUD、从表结构补 entity/bo/vo/mapper/service/controller，优先使用 `backend-crud.md` 的规则。
2. 如果是修改 `system`、`workflow` 等已经很复杂的模块，优先使用 `backend-module-enhancement.md` 的规则。
3. 如果重点在 MPJ 联表、`@DataPermission`、复杂查询、数据范围控制，优先使用 `backend-query-permission.md` 的规则。
4. 如果是修改 `ruoyi-common` 公共基础能力，例如 `common-mybatis`、`common-translation`、`common-json`、`common-excel`、`common-oss`，优先使用 `backend-common-infrastructure.md` 的规则。
5. 如果只要求补充或修正 JavaDoc 注释，优先使用 `backend-javadoc.md` 的规则。
6. 如果同时要求同步前端接口或前端页面骨架，先确认目标前端是 Vue 还是 React，保持后端路由与 generator 风格稳定，便于前端 agent 对接。

文档读取规则：

- 按任务类型读取上面对应的 `.claude/agents` 文档，只读取当前任务相关内容。
- 同步前端时，直接阅读目标 Vue 或 React 工程的现有 API、types、页面及对应 generator 模板，确认路由、返回结构和日期范围参数。
- 需要场景示例时，优先阅读当前模块最近似实现及实际调用点。
- 本套规则独立维护，不依赖 `.codex` 下的技能或 reference；规则、generator 模板和真实代码冲突时，优先相信当前模块真实代码和实际调用点。

通用要求：

- 先读同模块最近似实现，再动代码。
- 新增工具类或通用辅助方法前，必须执行下方“已有能力复用”流程。
- 发生冲突时优先相信当前模块真实代码，其次是公共基础设施，再其次才是 generator 模板。
- 默认直接产出可落地代码，而不是只给抽象建议。
- 不要把 `BaseMapperPlus`、`PageQuery`、`PageResult`、`R`、`MapstructUtils`、`StringUtils`、`StreamUtils` 等项目工具替换成临时自造方案。
- import、注解顺序、文件结构以附近代码为准，不做无关重排。
- 修改公开 API 前先查调用点；公共模块优先保持方法签名、泛型、返回值和异常语义兼容。

## 已有能力复用

1. 按能力类别检索同模块、`ruoyi-common` 和已有依赖，例如类型转换、字符串处理、集合处理、日期计算，不只搜索拟新增的方法名。
2. 优先阅读实际调用示例，再核对候选实现或依赖 API；结合目标模块的 `pom.xml`、传递依赖及版本确认可用性，不能仅凭其他模块有调用或根 POM 管理了版本就认定可用。
3. 核对空值、非法输入、默认值、异常和数值精度（含溢出、截断），依赖行为以当前版本的实现或测试为准。满足业务语义时直接复用，不添加无业务语义的包装；不满足时保留必要的业务差异，不能为了复用改变业务行为。

例如容错 ID 转换可先查已有调用中的 Hutool `Convert.toLong`，确认所用重载的失败返回值和默认值符合需求；必须抛错的严格解析不能改成返回 `null` 或默认值，还要核对小数、越界等输入的处理。这里只示范核对方式，不维护全量工具 API 清单。

交付自检：

- 新增工具类或通用辅助方法是否已检索并核对同类能力，避免重复实现。
- 确需新增时，在交付说明中简述现有能力为何不适用，无需为此额外添加代码注释；既有 JavaDoc 规则仍适用。
