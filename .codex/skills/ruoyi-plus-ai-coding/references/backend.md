# 后端约定

## 优先参考的代码来源

- `ruoyi-modules/ruoyi-gen/src/main/resources/fm/java/*.ftl`
- `ruoyi-modules/ruoyi-demo/...`
- `ruoyi-modules/ruoyi-system/...`
- `ruoyi-modules/ruoyi-workflow/...`
- `ruoyi-common/ruoyi-common-mybatis/...`

## 决策顺序

写代码时按下面顺序取样：

1. 当前业务模块下最近似实现。
2. 当前仓库公共能力模块中的统一约定。
3. generator 模板。
4. 通用 Spring / MyBatis-Plus 默认习惯。

用户当前要求及本技能的明确编码要求优先；真实代码和 generator 模板只用于补充未明确约定的细节，不得据此继续引入手写 SQL、重复校验等旧写法。

## Java 21、分支与格式

- 以 JDK 21 为基线，优先使用正式支持且能简化实现的语法和 API，例如模式匹配、switch 表达式、文本块和集合 API；不启用预览特性，不为使用新语法增加无意义抽象。
- `if` 条件必须使用圆括号；`if`、`else if`、`else` 的分支体必须使用大括号，即使只有一行。循环体同样使用大括号。
- 复杂布尔表达式按业务分组加圆括号，避免依赖不直观的运算符优先级；优先用清晰的提前返回减少嵌套。
- 判空依据入口约束、API 返回契约和实际可空语义；在确实可能为空并影响后续处理的位置统一处理，不逐层重复判空，也不把业务错误静默转换成默认值。
- 遵循 `.editorconfig`、Java 命名和附近代码的合理排版；只整理本次修改涉及的代码，保持缩进、空格、换行和 import 一致。

## 分层结构

标准 CRUD 代码应优先遵循下面这套结构：

- `domain/Entity.java`
- `domain/bo/EntityBo.java`
- `domain/vo/EntityVo.java`
- `mapper/EntityMapper.java`
- `service/IEntityService.java`
- `service/impl/EntityServiceImpl.java`
- `controller/EntityController.java`
- `utils/BusinessUtils.java`（工具类及按能力划分的子包）

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
- 新增、修改的必填、长度、格式、范围等字段约束优先通过 BO 的 Jakarta Bean Validation 注解声明；按新增、修改差异使用 `AddGroup`、`EditGroup`，查询约束按需使用 `QueryGroup`。
- `@Xss`、`@Email`、`@Size`、`@NotBlank`、`@NotNull` 要按真实业务语义添加，不要一股脑全套上。
- 查询存在日期范围或扩展条件时，保留 `params = new HashMap<>()`。

### 校验边界

- Controller 通过 `@Validated(AddGroup.class)`、`@Validated(EditGroup.class)` 或适用的 `@Valid` 触发 BO 校验，嵌套对象按需级联；不要只加注解而漏掉入口触发。
- 代码中需要主动执行对象校验时，优先使用 `org.dromara.common.core.utils.ValidatorUtils.validate(object, groups...)`，例如 `ValidatorUtils.validate(bo, AddGroup.class)`；复用对象上的约束注解，不自行创建 Validator 或重复编写字段检查。
- BO 已校验的字段，不在 Controller、Service、工具方法中重复手写必填、长度、格式、范围校验；禁止没有业务依据的校验。
- 数据库查询得到的 Entity/VO 不再执行 BO 式字段校验，不重复检查数据库非空约束已经保证的字段；不要为了防御性编程给每个 getter 添加判空。
- 查询是否命中、当前状态是否允许操作、唯一性、关联关系、权限及并发约束属于业务判断，按实际需求保留一次；这不等同于重新校验数据库对象的字段。
- 导入、消息消费等非 HTTP 入口确需处理外部输入时，可在对应入口通过 `ValidatorUtils` 统一触发必要校验；HTTP 入口已完成相同对象和分组校验时，不在下层重复调用。

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
- 多表查询必须使用 `MPJBaseMapper<Entity>` 及 MPJ wrapper，优先通过 `QueryBuilder.lambdaJoin(...)` 构造；核对现有继承链是否已提供该能力，未提供时补充继承。
- `selectVoList/selectVoPage` 不满足时，使用 MPJ 查询或封装 wrapper 的 Mapper 默认方法，不新增手写 SQL。
- Mapper 默认方法可以承载短小的 wrapper 查询；涉及复杂业务编排、缓存、事务或跨 mapper 写入时放到 service。
- `ruoyi-system` 的用户、角色、菜单、部门等模块常带数据权限、MPJ 联表、角色状态过滤，修改前先读对应 mapper/service。

### Mapper 建议结构

标准 mapper 一般按这个顺序组织：

1. 接口声明
2. 默认查询方法
3. 自定义分页或列表方法
4. 特殊数据权限重写
5. 辅助构造方法

### 禁止手写 SQL

- 禁止在 XML、`@Select` / `@Update` 等注解、SQL Provider、JDBC 或拼接字符串中编写 SQL。
- 不使用 `apply`、`last`、`inSql`、`exists`、`setSql` 等入口夹带手写 SQL 片段，也不为绕过约定新增自定义执行器。
- 单表查询和更新使用 MyBatis-Plus 的类型安全 wrapper；多表查询使用 MPJ 的关联、条件和投影 API。
- 现有模块存在 XML 不构成新增手写 SQL 的理由；修改涉及的查询时按本规则实现，保留数据权限、过滤、排序和分页语义。

## Service 规则

- 类声明通常是 `@RequiredArgsConstructor`、`@Service`，按需补 `@Slf4j`。
- 手写 mapper 注入字段使用具体业务短名；代码生成器模板按类名首字母小写命名。
- 命名时去掉清晰的模块/系统前缀后使用 lowerCamel + `Mapper`，例如 `SysRoleMapper` -> `roleMapper`、`SysDictDataMapper` -> `dictDataMapper`。
- 如果去掉前缀会产生歧义或命名冲突，保留必要前缀。
- 读操作通常返回 `Vo`、`List<Vo>` 或 `PageResult<Vo>`。
- BO 转实体用 `MapstructUtils.convert(bo, Entity.class)`。
- 查询条件优先返回 `LambdaQueryWrapper`；新增 generator 风格代码优先用 `QueryBuilder.lambda(Entity.class)`，老模块已有 `Wrappers.lambdaQuery()` 时可继续保持。
- 字符串和空值条件优先用 `eqIfText`、`likeIfText`、`eqIfPresent`、`inIfNotEmpty`、`betweenParams` 等项目扩展；老代码已有直接 `StringUtils.isNotBlank(...)` 和 null 判断时可增量保持。
- 分页查询优先采用：
  `Page<Vo> result = entityMapper.selectVoPage(pageQuery.build(), lqw);`
  `return PageResult.build(result.getRecords(), result.getTotal());`
- `validEntityBeforeSave(...)` 仅承载确有必要且未在其他位置完成的业务校验；不为套用模板新增空方法，不重复 BO 字段校验。
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

- 单表查询优先返回 `LambdaQueryWrapper`，生成器风格优先通过 `QueryBuilder.lambda(Entity.class).build()` 构造。
- 条件判断直接放在 wrapper 链式条件上，不要额外写大量 if 套壳。
- 日期范围统一从 `bo.getParams()` 取 begin/end；生成器默认使用 `betweenParams(Entity::getField, params, "beginField", "endField")`。
- 多表查询必须使用 `MPJBaseMapper`，先查同模块已有 MPJ 实现，再通过 `QueryBuilder.lambdaJoin("u", Entity.class)` 等现有入口构造，禁止退回手写 SQL。

### 写入逻辑建议

- BO 转实体统一走 `MapstructUtils.convert`。
- 批量关系维护时优先拆成私有方法，例如角色、岗位、用户关联。
- 修改前优先保留已有防误删、防越权、防并发覆盖逻辑。

## Controller 规则

- 继承 `BaseController`。
- 类上通常带 `@Validated`、`@RestController`、`@RequiredArgsConstructor`、`@RequestMapping`。
- 返回值使用 `R<T>` 或 `R<Void>`。
- 标准 CRUD 接口通常是：`GET /list`、`POST /export`、`GET /{id}`、`POST`、`PUT`、`DELETE /{ids}`。
- 树表接口通常不分页，`list` 返回 `R<List<Vo>>`；导出路由以目标模块或 generator 模板为准，旧 demo 树表存在 `GET /export`，新版生成器通常是 `POST /export`。
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
- 同等业务语义且当前模块依赖可用时，字符串、集合、日期、转换等通用处理优先使用 Hutool 工具类，不手写已有能力。
- 项目已封装业务语义或框架约定的能力继续直接复用，例如 `MapstructUtils`、`StringUtils`、`StreamUtils`、`ValidatorUtils`、`SpringUtils`、`RedisUtils`；不为切换 Hutool 丢失项目约定，也不添加仅转调 Hutool 的包装。
- 复用前核对空值、非法输入、默认值、异常和数值精度（含溢出、截断）的行为，不能为了复用改变业务语义；依赖行为以当前版本的实现或测试为准。
- 例如容错 ID 转换可先查已有调用中的 Hutool `Convert.toLong`，核对所用重载的失败返回值与默认值是否符合需求；必须抛错的严格解析不能改成返回 `null` 或默认值，也要核对小数、越界等输入的处理。此处只示范核对方式，不维护全量工具 API 清单。
- 数组转列表按附近代码习惯使用 `List.of(ids)` 或 `Arrays.asList(ids)`。
- 日期范围查询通常从 `bo.getParams()` 中读取 `beginTime`、`endTime` 或 `beginFieldName`、`endFieldName`。
- 构建查询优先识别 `QueryBuilder.lambda(...)`、`QueryBuilder.lambdaJoin(...)`、`BaseMapperPlus#lambda()` 三类入口，不要退回临时手写 SQL 或自造 wrapper。

### 工具类封装

- 新增前完成技能主文件要求的已有能力检索；只有现有能力不能满足实际语义时才新增。
- 无需注入依赖、无状态的工具能力封装为工具类，放在所属模块的 `utils` 包或其能力子包，名称使用 `XxxUtils`；不要散落在 Controller、Service 中或另建没有职责含义的目录。
- 工具类禁止实例化，方法使用 `static`，按项目现有方式使用私有构造器或对应 Lombok 注解；不声明为 Spring Bean，不保存可变业务状态。
- 需要依赖注入、数据库访问、事务或业务编排的逻辑按职责放 Service 等组件，不强行包装成静态工具；不要把仅当前业务流程使用的私有步骤都提取成工具类。
- 工具类及对外方法补齐 JavaDoc，说明用途、参数、返回值以及不直观的空值和异常语义。

## common-mybatis 规则

- 链式查询能力优先沿用 `QueryBuilder.lambda(...)`、`QueryBuilder.lambdaJoin(...)`、`BaseMapperPlus#lambda()`、`LambdaCrudChainWrapper`、`LambdaQueryBuilder`、`LambdaJoinQueryBuilder`、`LambdaQueryCondition`。
- 条件辅助方法使用项目已有命名：`eqIfPresent`、`eqIfText`、`neIfPresent`、`likeIfText`、`betweenIfPresent`、`betweenParams`、`inIfNotEmpty`、`findInSetIfPresent`。
- 新增 wrapper 方法时保持链式返回 `this` / `typedThis`，不要返回底层 `LambdaQueryWrapper` 破坏调用链。
- `LambdaCrudChainWrapper` 既承担查询又承担更新 set 片段，新增能力时要同时考虑 `getSqlSelect`、`getSqlSet`、`clear`、`instance` 的状态复制和清理。
- MPJ 联表查询沿用别名风格，例如 `QueryBuilder.lambdaJoin("u", SysUser.class)`、`.leftJoin(..., "d", ...)`、`.eq("u", Entity::getField, value)`。
- 数据权限注解使用 `@DataPermission` + `@DataColumn`，列名需和实际 SQL 别名一致，例如 `d.dept_id`、`u.create_by`。

## translation / JSON 增强规则

- 翻译实现类实现 `TranslationInterface<T>` 并标注 `@TranslationType(type = ...)`。
- 使用方在 VO 字段上通过 `@Translation(type = ..., mapper = "...", other = "...")` 指定翻译来源。
- 批量翻译必须优先实现 `translationBatch(Set<Object> keys, String other)`，避免默认逐条查询。
- 支持逗号分隔 ID 的翻译实现应复用 `collectLongIds`、`parseLongIds`、`joinMappedValues`。
- `TranslationJsonFieldProcessor` 遵循三阶段：`collect` 收集待翻译值，`prepare` 批量查询，`process` 写入翻译结果；新增处理器也应优先套这个模型。
- 翻译失败时保持降级返回原值或 `null` 的现有语义，不要让响应增强中断主流程。

## 缓存与异步/监听规则

- 已有 service 使用 `@Cacheable`、`@CachePut`、`@CacheEvict`、`@Caching` 或 `CacheUtils.evict/clear` 时，新增写操作要同步考虑缓存失效。
- 部门、字典、OSS 配置等模块已有缓存初始化或失效逻辑，不要只改数据库不处理缓存；字典这类模块常同时维护 `CacheNames.SYS_DICT` 与 `CacheNames.SYS_DICT_TYPE`。
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
- 是否优先复用 Hutool 和项目现有能力，新增工具是否位于 `utils` 包且为不可实例化的静态工具类。
- 是否存在手写 SQL，多表查询是否通过 `MPJBaseMapper` 和 MPJ wrapper 实现。
- BO 字段约束是否在入口触发，是否存在无用校验、重复校验或数据库结果字段重校验。
- JDK 21 正式语法和 API 是否使用恰当，分支大括号、必要判空、JavaDoc 和格式是否符合约定。
