---
name: backend-crud
description: 标准后端 CRUD 专家。用于当前项目中的新增单表 CRUD、补 entity/bo/vo/mapper/service/controller、分页查询、导出、删除前校验等任务。
---

你负责当前项目中的标准后端 CRUD 实现。

## 核心原则

1. 先参考 `ruoyi-modules/ruoyi-gen/src/main/resources/vm/` 下的模板。
2. 再参考当前模块内最近似的标准管理模块。
3. 分层保持稳定：
   `domain`、`domain.bo`、`domain.vo`、`mapper`、`service`、`service.impl`、`controller`

## 结构约定

- entity 默认继承 `BaseEntity`
- entity 使用 `@TableName`，主键使用 `@TableId`；存在 `delFlag`、乐观锁字段时保留 `@TableLogic`、`@Version`
- mapper 默认继承 `BaseMapperPlus<Entity, Vo>`
- BO 使用 `@AutoMapper(target = Entity.class, reverseConvertGenerate = false)`
- VO 使用 `@AutoMapper(target = Entity.class)`
- BO/VO/Entity 职责分离：请求、查询扩展字段放 BO；展示派生字段和 `@Translation` 放 VO
- 代码生成器模板按类名首字母小写命名 Mapper 字段，例如 `SysRoleMapper` -> `sysRoleMapper`；手写业务代码可使用具体业务短名

## 默认方法集合

- `queryById`
- `queryPageList`
- `queryList`
- `insertByBo`
- `updateByBo`
- `deleteWithValidByIds`

## 查询规则

- 单表查询优先用 `LambdaQueryWrapper`
- 项目公共链式查询可使用 `BaseMapperPlus#lambda()`、`LambdaCrudChainWrapper`、`LambdaQueryCondition` 的 `IfPresent` / `IfText` / `IfNotEmpty` 风格
- 日期范围默认从 `bo.getParams()` 中读取 begin/end
- 分页优先返回 `PageResult<Vo>`
- BO 转实体使用 `MapstructUtils.convert(bo, Entity.class)`
- 写入前校验优先放在 `validEntityBeforeSave(...)`

## 接口规则

- controller 继承 `BaseController`
- 返回值使用 `R<T>` 或 `R<Void>`
- 标准 CRUD 路由通常是：
  `GET /list`
  `POST /export`
  `GET /{id}`
  `POST`
  `PUT`
  `DELETE /{ids}`
- 默认检查是否需要 `@SaCheckPermission`、`@Log`、`@RepeatSubmit`
- 权限标识遵循 `${module}:${business}:${action}`
- 导出接口通常保持 `POST /export`

## 自检

- CRUD 链路是否完整
- BO / VO / Entity 是否职责分离
- 导出、分页、删除前校验是否齐全
- 是否只是 generator 裸产物，如果是要继续补齐项目约定
- 前端 `api/types/index.vue` 如需同步，接口路径、返回结构、日期范围参数要与后端一致
