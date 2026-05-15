# 后端约定

## 优先参考的代码来源

- `ruoyi-modules/ruoyi-gen/src/main/resources/vm/java/*.vm`
- `ruoyi-modules/ruoyi-demo/...`
- `ruoyi-modules/ruoyi-system/...`
- `ruoyi-common/ruoyi-common-mybatis/...`

## 决策顺序

写代码时按下面顺序取样：

1. 当前业务模块下最近似实现。
2. 当前仓库公共能力模块中的统一约定。
3. generator 模板。
4. 通用 Spring / MyBatis-Plus 默认习惯。

如果规则冲突，优先相信当前仓库真实代码。

## 分层结构

标准 CRUD 代码应优先遵循下面这套结构：

- `domain/Entity.java`
- `domain/bo/EntityBo.java`
- `domain/vo/EntityVo.java`
- `mapper/EntityMapper.java`
- `service/IEntityService.java`
- `service/impl/EntityServiceImpl.java`
- `controller/EntityController.java`

## Entity 规则

- 除非所在模块明显另有约定，否则实体类继承 `org.dromara.common.mybatis.core.domain.BaseEntity`。
- 使用 Lombok `@Data` 和 `@EqualsAndHashCode(callSuper = true)`。
- 使用 `@TableName("table_name")`。
- 主键使用 `@TableId`。
- 存在 `delFlag` 时保留 `@TableLogic`，存在乐观锁字段时保留 `@Version`。
- 如果附近实体已经使用 `@OrderBy` 等额外注解，应继续保持。

## BO 规则

- 实现 `Serializable`。
- 添加 `@AutoMapper(target = Entity.class, reverseConvertGenerate = false)`。
- 请求专用字段、查询专用字段放在 BO 中，包括 `params`。
- 在生成器或附近代码已有分组校验时，继续使用：`AddGroup`、`EditGroup`、`QueryGroup`。
- `@Xss`、`@Email`、`@Size`、`@NotBlank`、`@NotNull` 要按真实业务语义添加，不要一股脑全套上。
- 查询存在日期范围或扩展条件时，保留 `params = new HashMap<>()`。

## VO 规则

- 实现 `Serializable`。
- 添加 `@AutoMapper(target = Entity.class)`。
- 生成器风格的导出对象通常带 `@ExcelIgnoreUnannotated`。
- `@ExcelProperty`、`@ExcelDictFormat`、`ExcelDictConvert`、`@ExcelRequired`、`@ExcelNotation`、`@DateTimeFormat` 只在导入导出场景下使用。
- 如果附近代码会把 ID 翻译成展示字段，沿用 `@Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "createBy")` 这类写法。
- 展示型派生字段放在 VO，不放在 Entity。

## Mapper 规则

- 默认形式是 `interface XxxMapper extends BaseMapperPlus<Xxx, XxxVo>`。
- 不要为简单的 entity 转 vo 手写重复代码，优先依赖 `BaseMapperPlus`。
- 模块已经使用 `@DataPermission` 时，在重写方法和自定义查询上继续保留。
- 复杂模块里 mapper 可能同时继承 `MPJBaseMapper<Entity>` 并使用 `JoinWrappers.lambda(...)`，遇到这种风格要延续，不要换一种写法。
- 只有在 `selectVoList/selectVoPage` 不够用时，才补 XML 或自定义 mapper 方法。
- Mapper 默认方法可以承载短小的 wrapper 查询；涉及复杂业务编排、缓存、事务或跨 mapper 写入时放到 service。
- `ruoyi-system` 的用户、角色、菜单、部门等模块常带数据权限、MPJ 联表、角色状态过滤，修改前先读对应 mapper/service。

### Mapper 建议结构

标准 mapper 一般按这个顺序组织：

1. 接口声明
2. 默认查询方法
3. 自定义分页或列表方法
4. 特殊数据权限重写
5. 辅助构造方法

### 什么时候需要 XML

- 复杂联表 SQL 无法仅靠 wrapper 清晰表达时。
- 需要手写查询列和结果映射时。
- 项目当前模块已经大量使用 XML 时。

如果 `BaseMapperPlus + wrapper` 已足够，优先不要补 XML。

## Service 规则

- 类声明通常是 `@RequiredArgsConstructor`、`@Service`，按需补 `@Slf4j`。
- 手写 mapper 注入字段使用具体业务短名；代码生成器模板按类名首字母小写命名。
- 命名时去掉清晰的模块/系统前缀后使用 lowerCamel + `Mapper`，例如 `SysRoleMapper` -> `roleMapper`、`SysDictDataMapper` -> `dictDataMapper`。
- 如果去掉前缀会产生歧义或命名冲突，保留必要前缀。
- 读操作通常返回 `Vo`、`List<Vo>` 或 `PageResult<Vo>`。
- BO 转实体用 `MapstructUtils.convert(bo, Entity.class)`。
- 查询条件优先用 `LambdaQueryWrapper` 和 `Wrappers.lambdaQuery()`。
- 在 wrapper 条件里直接写 `StringUtils.isNotBlank(...)` 和 null 判断。
- 分页查询优先采用：
  `Page<Vo> result = entityMapper.selectVoPage(pageQuery.build(), lqw);`
  `return PageResult.build(result.getRecords(), result.getTotal());`
- 生成器风格模块保留 `validEntityBeforeSave(...)` 这种扩展点。
- 多表写操作使用 `@Transactional(rollbackFor = Exception.class)`。
- 明确的业务失败，尤其是权限、数据完整性、删除校验，使用 `ServiceException`。
- 不要绕过模块现有的数据权限、角色校验、删除前校验。

### Service 建议结构

标准 service impl 一般按下面顺序组织：

1. 查询单条
2. 分页查询
3. 列表查询
4. 构建查询条件
5. 新增
6. 修改
7. 保存前校验
8. 删除前校验与删除
9. 其他扩展业务方法

### 查询逻辑建议

- 单表查询优先使用 `LambdaQueryWrapper`。
- 条件判断直接放在 wrapper 上，不要额外写大量 if 套壳。
- 日期范围统一从 `bo.getParams()` 取 begin/end。
- 复杂联表查询优先查同模块是否已有 MPJ 风格可复用。

### 写入逻辑建议

- BO 转实体统一走 `MapstructUtils.convert`。
- 批量关系维护时优先拆成私有方法，例如角色、岗位、用户关联。
- 修改前优先保留已有防误删、防越权、防并发覆盖逻辑。

## Controller 规则

- 继承 `BaseController`。
- 类上通常带 `@Validated`、`@RestController`、`@RequiredArgsConstructor`、`@RequestMapping`。
- 返回值使用 `R<T>` 或 `R<Void>`。
- 标准 CRUD 接口通常是：`GET /list`、`POST /export`、`GET /{id}`、`POST`、`PUT`、`DELETE /{ids}`。
- `@SaCheckPermission` 权限格式遵循 `${module}:${business}:${action}`。
- 写操作、导入导出接口通常加 `@Log(title = "...", businessType = BusinessType.X)`。
- 附近接口已有防重时，写接口继续使用 `@RepeatSubmit`。
- 适合分组校验时，使用 `@Validated(AddGroup.class)` 和 `@Validated(EditGroup.class)`。
- 特殊接口直接复用模块内现成做法，例如导入导出、`@ApiEncrypt`、multipart 上传、数据权限检查、写入前唯一性校验。

### Controller 建议结构

标准 controller 一般按下面顺序组织：

1. 列表
2. 导出
3. 详情
4. 新增
5. 修改
6. 删除
7. 特殊接口

### Controller 边界

- controller 负责接参、校验、权限、日志、返回值转换。
- 重业务逻辑尽量放 service，不要在 controller 里堆长逻辑。
- 但前置权限检查、唯一性提示、显式业务失败提示可以留在 controller，前提是同模块已有这种习惯。

## 查询与工具规则

- 分页统一使用 `PageQuery` 和 `PageResult`，不要无故引入新的分页 DTO。
- 优先使用项目工具类：`MapstructUtils`、`StringUtils`、`StreamUtils`、`ValidatorUtils`、`SpringUtils`、`RedisUtils`。
- 数组转列表按附近代码习惯使用 `List.of(ids)` 或 `Arrays.asList(ids)`。
- 日期范围查询通常从 `bo.getParams()` 中读取 `beginTime`、`endTime` 或 `beginFieldName`、`endFieldName`。

## common-mybatis 规则

- 链式查询能力优先沿用 `BaseMapperPlus#lambda()`、`LambdaCrudChainWrapper`、`LambdaQueryBuilder`、`LambdaQueryCondition`。
- 条件辅助方法使用项目已有命名：`eqIfPresent`、`eqIfText`、`likeIfText`、`betweenIfPresent`、`inIfNotEmpty`、`findInSetIfPresent`。
- 新增 wrapper 方法时保持链式返回 `this` / `typedThis`，不要返回底层 `LambdaQueryWrapper` 破坏调用链。
- `LambdaCrudChainWrapper` 既承担查询又承担更新 set 片段，新增能力时要同时考虑 `getSqlSelect`、`getSqlSet`、`clear`、`instance` 的状态复制和清理。
- MPJ 联表查询沿用别名风格，例如 `JoinWrappers.lambda("u", SysUser.class)`、`.leftJoin(..., "d", ...)`、`.eq("u", Entity::getField, value)`。
- 数据权限注解使用 `@DataPermission` + `@DataColumn`，列名需和实际 SQL 别名一致，例如 `d.dept_id`、`u.create_by`。

## translation / JSON 增强规则

- 翻译实现类实现 `TranslationInterface<T>` 并标注 `@TranslationType(type = ...)`。
- 使用方在 VO 字段上通过 `@Translation(type = ..., mapper = "...", other = "...")` 指定翻译来源。
- 批量翻译必须优先实现 `translationBatch(Set<Object> keys, String other)`，避免默认逐条查询。
- 支持逗号分隔 ID 的翻译实现应复用 `collectLongIds`、`parseLongIds`、`joinMappedValues`。
- `TranslationJsonFieldProcessor` 遵循三阶段：`collect` 收集待翻译值，`prepare` 批量查询，`process` 写入翻译结果；新增处理器也应优先套这个模型。
- 翻译失败时保持降级返回原值或 `null` 的现有语义，不要让响应增强中断主流程。

## 缓存与异步/监听规则

- 已有 service 使用 `@Cacheable`、`@CacheEvict`、`@Caching` 时，新增写操作要同步考虑缓存失效。
- 部门、字典、OSS 配置等模块已有缓存初始化或失效逻辑，不要只改数据库不处理缓存。
- Excel 导入监听器实现 `ExcelListener` 时，保留 `getExcelResult()` 的回执语义和错误聚合方式。
- 定时任务、MQTT、SSE、异步回调等框架方法一般按接口覆写语义实现，除非业务不直观，不要添加冗长注释。

## 工作流模块规则

- `ruoyi-workflow` 通常带 `@ConditionalOnEnable`，新增 workflow bean、controller、service 时检查同包是否需要该条件。
- 流程分类、任务、实例等查询常带分类权限或用户维度过滤，先读同类 mapper/service 再改。
- 工作流的翻译实现可以放在 workflow 模块内，例如流程分类 ID 到名称，仍应遵守 `TranslationInterface` 批量翻译规则。

## JavaDoc 注释规则

- 公共 API、接口、VO/BO/Entity 字段、Mapper 默认方法、Service/Controller 方法应有简洁 JavaDoc。
- 注释描述“做什么”和关键参数语义，不复述显而易见的实现细节。
- `void` 方法不要写 `@return`；返回布尔值时说明 `true/false` 含义。
- 私有方法只有在业务规则、算法、映射关系不直观时补注释。
- 框架覆写方法如果只是标准回调，可不重复注释；但当前文件已有统一注释风格时保持一致。
- 只改注释时，不重排 import、不格式化全文件、不修改代码行为。

## 前后端联动规则

- 新增后端接口时，路径和权限前缀尽量保持 generator 约定，方便前端目录和 API 命名同步。
- 新增日期范围查询时，记得保留 `bo.params` 结构，避免前端 `addDateRange` 无法对接。
- 导出接口通常保持 `POST /export` 风格，便于前端直接复用现有下载逻辑。
- 批量删除接口通常使用 `DELETE /{ids}`，便于前端直接传数组或逗号串。

## 生成器优先模式

从零新增 CRUD 时，优先对齐生成器默认方法集合：

- `queryById`
- `queryPageList`
- `queryList`
- `insertByBo`
- `updateByBo`
- `deleteWithValidByIds`

然后再叠加模块内已有增强，例如：

- 唯一性校验
- 数据权限注解
- MPJ 联表查询
- 缓存注解
- Excel 导入导出监听器
- 关联表维护逻辑

## 什么时候优先看 generator

- 新增一个标准单表 CRUD 时。
- 只有表结构和基本接口需求，没有现成业务模块可参考时。
- 需要快速补齐整套骨架代码时。

## 什么时候优先看现有模块

- 目标模块已经有类似业务。
- 涉及数据权限、联表、缓存、角色岗位关系、导入导出、工作流扩展时。
- 任务是“修改已有模块”而不是“新建模块”时。

## 避免事项

- 不要在 controller 里直接暴露 entity 代替 BO/VO。
- 不要给新的管理接口漏掉权限注解。
- 没有明确必要时，不要从 `BaseMapperPlus` 风格退回手工映射。
- 前端查询页用了日期范围时，不要删掉后端 `params` 相关处理。
- 不要把 `ruoyi-system` 这类复杂逻辑强行简化成生成器式单表 CRUD。

## 交付前自检

交付前至少检查这些点：

- CRUD 主链路是否完整。
- BO / VO / Entity 职责是否清晰。
- 分页、查询、删除校验是否与前端对得上。
- 权限、日志、防重、事务是否遗漏。
- 是否只是 generator 裸产物，如果是，需要继续补齐同模块已有增强。
