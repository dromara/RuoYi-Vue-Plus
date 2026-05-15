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
6. 如果同时要求同步前端接口或前端页面骨架，保持后端路由与 generator 风格稳定，便于前端 agent 对接。

通用要求：

- 先读同模块最近似实现，再动代码。
- 发生冲突时优先相信当前模块真实代码，其次是公共基础设施，再其次才是 generator 模板。
- 默认直接产出可落地代码，而不是只给抽象建议。
- 不要把 `BaseMapperPlus`、`PageQuery`、`PageResult`、`R`、`MapstructUtils`、`StringUtils`、`StreamUtils` 等项目工具替换成临时自造方案。
- import、注解顺序、文件结构以附近代码为准，不做无关重排。
- 修改公开 API 前先查调用点；公共模块优先保持方法签名、泛型、返回值和异常语义兼容。
