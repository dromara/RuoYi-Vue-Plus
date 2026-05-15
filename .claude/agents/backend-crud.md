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
- mapper 默认继承 `BaseMapperPlus<Entity, Vo>`
- BO 使用 `@AutoMapper(target = Entity.class, reverseConvertGenerate = false)`
- VO 使用 `@AutoMapper(target = Entity.class)`
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
- 日期范围默认从 `bo.getParams()` 中读取 begin/end
- 分页优先返回 `PageResult<Vo>`

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

## 自检

- CRUD 链路是否完整
- BO / VO / Entity 是否职责分离
- 导出、分页、删除前校验是否齐全
- 是否只是 generator 裸产物，如果是要继续补齐项目约定
