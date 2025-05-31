# PMS 模块二次开发 ToDoList

## 阶段 0: 项目初始化与理解

- [x] **阅读核心文档:**
  - [x] 仔细阅读《PMS数据模型.md》(v5.7)，完全理解PMS核心表的结构、字段定义、关系以及主键/外键/索引规范，特别是第5节核心业务枚举值定义。
  - [x] 仔细阅读《PMS需求.md》(v4.3)，明确PMS核心模块 [P0] 阶段必须实现的功能需求（联系人基础、房态核心、价格基础、订单核心生命周期、基础财务、系统配置基础）。
  - [x] 通读《RuoYi-Vue-Plus二次开发最佳实践.md》，重点关注以下章节：
    - Chapter 1: 引言 (文档目的, 模块化特性, **二次开发核心原则**, **Cursor使用优势**)
    - Chapter 2: 环境准备与项目结构 (前后端环境, **前后端项目结构**, **代码生成器与Cursor协同**)
    - Chapter 3: 模块深入分析 (用户管理模块的启示)
    - Chapter 4: 后端开发最佳实践 (Cursor辅助技巧, 模块设计, 分层约定, Domain对象规范)
    - Chapter 5: 前端开发最佳实践 (Cursor辅助技巧, 项目结构, 功能模块开发规范, 组件案例, 前后端交互)
    - Chapter 6: 前后端协作规范
    - Chapter 7: 代码规范与风格
    - Chapter 8: 新模块添加流程与实践

- [x] **环境与工具准备:**
  - [x] 确保后端开发环境 (JDK, Maven, Redis, MySQL) 符合《最佳实践》Chapter 2.1 要求。
  - [x] 确保前端开发环境 (Node.js, pnpm) 符合《最佳实践》Chapter 2.1 要求。
  - [x] 安装并配置好 Cursor IDE，将整个 `ruoyi-vue-plus` 项目导入工作区。
  - [x] 在 Cursor 中为项目设置特定上下文，包括关键模块路径、技术栈、编码规范等，以便更好地辅助开发。

- [x] **版本控制与代码管理准备:**
  - [x] 配置Git多仓库管理策略（主仓库 + 子模块）
  - [x] 设置upstream远程源以同步官方更新
  - [x] 创建开发分支策略
  - [x] 确保`.gitignore`配置正确，排除target、node_modules等目录

## 阶段 1: 后端开发 - 模块搭建与核心表结构

- [x] **创建后端Maven子模块 `ruoyi-pms`** (参考《最佳实践》Chapter 2.2, 4.2, 8.3):
  - [x] 在 `ruoyi-vue-plus/ruoyi-modules` 目录下创建新的 Maven 子模块 `ruoyi-pms`。
  - [x] 配置 `ruoyi-pms/pom.xml`：
    - [x] 设置 `<parent>` 指向 `ruoyi-modules`。
    - [x] 添加必要的公共模块依赖 (如: `ruoyi-common-core`, `ruoyi-common-mybatis`, `ruoyi-common-web`, `ruoyi-common-satoken`, `ruoyi-common-excel`, `ruoyi-common-translate`, `ruoyi-common-tenant`)。
  - [x] 在项目根目录 `pom.xml` 和 `ruoyi-modules/pom.xml` 的 `<modules>` 部分注册 `ruoyi-pms`。

- [x] **创建后端基础包结构** (参考《最佳实践》Chapter 4.2):
  - [x] 在 `ruoyi-pms/src/main/java/` 下创建基础包 `org.dromara.pms` (应用名: pms)。
  - [x] 在 `org.dromara.pms` 下创建标准分层包：`controller`, `service`, `service.impl`, `mapper`, `domain` (包含 `entity`, `bo`, `vo`)。

- [x] **后端国际化资源文件**:
  - [x] 在 `ruoyi-pms/src/main/resources/` 下创建 `i18n` 目录。
  - [x] 在 `i18n` 目录下创建基础的国际化属性文件，如 `messages_zh_CN.properties` 和 `messages_en_US.properties`。

- [x] **API文档配置**:
  - [x] 在 `ruoyi-admin` 的 `application.yml` 中配置springdoc扫描PMS模块 (`packages-to-scan: org.dromara.pms`)
  - [x] 验证Swagger UI可以显示PMS模块的API文档

- [x] **集成新模块到主项目**:
  - [x] 在 `ruoyi-admin` 模块的 `pom.xml` 中添加 `ruoyi-pms` 依赖
  - [x] 验证项目可以正常启动并加载PMS模块

- [x] **数据库表创建与初始化** (参考《PMS数据模型.md》v5.7):
  - [x] **执行SQL脚本**: 确保已在开发数据库中执行了包含所有PMS相关表（如 `pms_customer_contacts`, `pms_contact_tags`, `pms_contact_tag_relations` 等）的创建和初始化脚本。
  - [x] **验证表结构**: 对照《PMS数据模型.md》(v5.7)仔细检查已创建的表结构、字段类型、约束（包括 `del_flag NOT NULL`）、审计字段、索引是否正确。
  - [x] **验证枚举值和基础数据**: 确认联系人相关的字典数据及所有枚举值相关的表字段设计已按《PMS数据模型.md》第5节正确实现。
  - [x] **Cursor辅助**: 已完成PMS联系人模块相关表结构的创建和验证。

- [x] **MyBatis Plus配置优化**:
  - [x] 在 `ruoyi-pms` 模块中创建配置类，添加 `@MapperScan("org.dromara.pms.mapper")` 注解
  - [x] 配置MyBatis Plus的全局配置（ID生成策略、逻辑删除，确保与《PMS数据模型.md》中的 `del_flag` 定义一致）
  - [x] **多租户配置**: 确保 `tenant.excludes` 配置与《PMS数据模型.md》6.2.1节一致，特别是 `pms_suppliers`, `pms_partners` 的排除。
  - [x] 验证数据库连接和基础CRUD操作

- [x] **基础测试验证**:
  - [x] 创建简单的测试Controller验证模块正常工作
  - [x] 验证包扫描、依赖注入、数据库连接等基础功能
  - [x] 确保项目启动无错误，日志输出正常

- [ ] **缓存策略实现** (参考《PMS需求.md》16.2节):
  - [ ] **客户标签缓存**: 实现租户级标签缓存，减少数据库查询
  - [ ] **房间状态缓存**: 实现房间可用性实时缓存更新
  - [ ] **价格规则缓存**: 缓存活跃的价格规则，提升计算性能
  - [ ] **权限数据缓存**: 实现用户权限和门店信息缓存
  - [ ] **配置缓存**: 实现系统配置信息缓存，支持热更新

- [ ] **性能优化实现** (参考《PMS需求.md》16.3节):
  - [ ] **客户查询优化**: 使用复合索引支持多条件查询
  - [ ] **标签关联优化**: 实现批量查询客户标签关系
  - [ ] **房态查询优化**: 房间状态查询使用索引优化
  - [ ] **价格计算优化**: 实现规则引擎缓存和并行计算
  - [ ] **分页查询优化**: 所有列表页面支持分页和排序

- [ ] **数据安全与加密实现** (参考《PMS需求.md》15.3节):
  - [ ] **敏感数据加密**: 实现客户身份证号等敏感数据AES-256加密存储
  - [ ] **传输安全**: 确保所有API接口使用HTTPS/TLS 1.3加密传输
  - [ ] **访问控制**: 实现基于RBAC的细粒度权限控制
  - [ ] **操作审计日志**: 实现完整的操作审计日志，保存180天
  - [ ] **数据脱敏**: 实现敏感数据在前端的脱敏显示

## 阶段 2: 后端开发 - 核心业务功能 [P0]

**通用后端开发规范 (参考《最佳实践》Chapter 4 & 7.1 及《PMS需求.md》技术要求):**
  - [ ] 遵循分层架构约定 (Controller -> Service -> Mapper)。
  - [ ] 严格区分 Entity, BO, VO 的职责和使用场景。Entity对应《PMS数据模型.md》的表结构。
  - [ ] 使用 MapStruct Plus (`@AutoMapper`) 进行对象转换。
  - [ ] Service 层实现类的方法应添加 `@Transactional` 注解。
  - [ ] Controller 层方法应添加 `@Log` 操作日志注解和 `@SaCheckPermission` 权限注解。权限点参考《PMS需求.md》第13节API设计。
  - [ ] BO 对象字段使用 JSR 303/380 注解进行参数校验，校验规则参考《PMS需求.md》API设计中的请求体定义。
  - [ ] VO 对象字段按需使用 `@Translation` 和 `@Sensitive` 注解。
  - [ ] **枚举处理**: 后端使用Java枚举类对应《PMS数据模型.md》第5节定义的枚举值，并确保与数据库存储的 `VARCHAR(50)` 字符串值正确转换。
  - [ ] **状态流转**: 在Service层实现《PMS数据模型.md》第4节定义的订单、房间等状态流转逻辑。
  - [ ] **数据权限**: 严格按照《PMS数据模型.md》6.2.2节和《PMS需求.md》16.5节实现多租户和门店级数据权限。
  - [ ] 编写清晰的 Javadoc 注释和行内注释。
  - [ ] 使用 Cursor 辅助生成代码、分析逻辑、优化代码。

**Cursor AI协作技巧提升:**
  - [ ] **代码生成提示优化**: "请Cursor根据《PMS数据模型.md》中的 `pms_customer_contacts` 表结构和《PMS需求.md》中13.1节的API规范，为客户联系人模块生成符合《最佳实践》的后端Controller, Service, Mapper, Entity, BO, VO 代码框架。确保包含[P0]功能：基础联系人管理。"
  - [ ] **代码审查**: "请Cursor审查我为 `PmsCoreOrderServiceImpl` 编写的订单创建逻辑，检查是否符合事务管理要求、数据校验是否全面、以及状态流转是否正确。"
  - [ ] **错误诊断**: "我的 `PmsRoomRoomController` 在查询房态时返回空列表，请Cursor分析代码和相关MyBatis XML，找出可能的原因。日志信息如下..."
  - [ ] **重构建议**: "请Cursor分析 `PmsFinanceFolioServiceImpl` 中的账单计算逻辑，是否有可优化的部分，并提供重构建议。"

**具体功能模块开发 ([P0] 优先级，参考《PMS需求.md》和《PMS数据模型.md》):**

- [ ] **代码生成器应用与优化** (参考《最佳实践》Chapter 2.4):
  - [ ] **准备代码生成器配置**: 
    - [ ] 在RuoYi管理后台配置代码生成器，导入《PMS数据模型.md》定义的所有PMS核心业务表（如`pms_customer_contacts`, `pms_room_types`, `pms_room_rooms`, `pms_core_orders`等）。
    - [ ] 配置生成参数（包名 `org.dromara.pms`、模块名 `pms`、作者、表前缀 `pms_` 处理等）。
    - [ ] 设置字段属性（是否必填、查询条件、显示类型等），特别是枚举字段的处理方式。
  - [ ] **批量生成基础代码**: 针对《PMS数据模型.md》中定义的核心表生成初始CRUD代码。
  - [ ] **Cursor辅助优化生成代码**:
    - [ ] "请Cursor分析代码生成器为 `pms_customer_contacts` 表生成的后端代码，并对比《PMS需求.md》13.1节的API定义和《PMS数据模型.md》的表结构，指出哪些部分需要手动调整或补充业务逻辑（如加密、唯一性校验、枚举转换）。"
    - [ ] "请Cursor帮我优化 `PmsRoomType.java` Entity类，确保所有字段与《PMS数据模型.md》一致，并为相关字段（如`status`）添加正确的Java枚举类型和MyBatis TypeHandler（如果需要）。"
    - [ ] "请Cursor检查 `IPmsCoreOrderService.java` 接口是否缺少《PMS需求.md》9.1.1节[P0]核心订单生命周期管理所需的业务方法，并补充完整。"

- [x] **1. 联系人管理 (`pms_customer_contacts`, `pms_contact_tags`, `pms_contact_tag_relations`)** [P0]
  - [x] **Domain对象优化**: 
    - [x] `PmsCustomerContacts`, `PmsCustomerContactsBo`, `PmsCustomerContactsVo` (参考《PMS需求.md》13.1.1 响应体)。
    - [x] `PmsContactTag`, `PmsContactTagBo`, `PmsContactTagVo`.
    - [x] `PmsContactTagRelation` (通常只有Entity和BO).
    - [x] 实现《PMS数据模型.md》5.1节定义的客户相关枚举 (contact_type, contact_status, gender, member_level, tag_category)。
    - [x] 字段校验：`fullName`必填, `phoneNumber`唯一性 (租户内), `idNumber`加密存储。
  - [x] **数据库操作层**: 
    - [x] `PmsCustomerContactsMapper`: 实现按姓名、电话、状态、类型、标签等查询。支持租户级数据共享。
    - [x] `PmsContactTagMapper`: 支持租户级和门店级标签查询 (dept_id IS NULL 或特定值)。
  - [x] **业务逻辑层**: `IPmsCustomerContactsService`, `IPmsContactTagService`
    - [x] 实现客户档案创建、查询、更新、逻辑删除。
    - [x] 实现客户标签的增删改查，以及客户与标签的关联/解关联。
    - [x] 业务校验逻辑：如电话号码唯一性，标签权限等。
  - [x] **控制器层**: `PmsCustomerContactsController`, `PmsContactTagController`
    - [x] 实现《PMS需求.md》13.1节定义的客户管理API和13.2节的标签API。
    - [x] 权限控制: `pms:customerContacts:list`, `add`, `edit`, `delete`, `manageTags`等。

- [x] **2. 房型管理 (`pms_room_types`)** [P0]
  - [x] **Domain对象**: `PmsRoomType`, `PmsRoomTypeBo`, `PmsRoomTypeVo`.
    - [x] 实现 `status` 枚举。
  - [x] **数据库操作**: `PmsRoomTypeMapper`. 门店级隔离。
  - [x] **业务逻辑**: `IPmsRoomTypeService`. 实现房型增删改查，状态管理。
  - [x] **控制器**: `PmsRoomTypeController`.

- [x] **3. 房间管理 (`pms_room_rooms`)** [P0]
  - [x] **Domain对象**: `PmsRoomRoom`, `PmsRoomRoomBo`, `PmsRoomRoomVo` (可包含房型名称等关联信息)。
    - [x] 实现 `room_status`, `cleaning_status` 枚举 (参考《PMS数据模型.md》5.4节)。
  - [x] **数据库操作**: `PmsRoomRoomMapper`. 门店级隔离。查询需支持房型、楼层、状态组合。
  - [x] **业务逻辑**: `IPmsRoomRoomService`.
    - [x] 实现房间增删改查。
    - [x] 房间物理状态和清洁状态的更新与管理逻辑。
    - [x] 实现《PMS数据模型.md》4.2节房间状态流转。
  - [x] **控制器**: `PmsRoomRoomController`.

- [x] **4. 房间锁定管理 (`pms_room_locks`)** [P0]
  - [x] **Domain对象**: `PmsRoomLock`, `PmsRoomLockBo`, `PmsRoomLockVo`.
    - [x] 实现 `lock_type` 枚举。
  - [x] **业务逻辑**: `IPmsRoomLockService`. 实现房间锁定、解锁、查询，锁定期间冲突检测。
  - [x] **控制器**: `PmsRoomLockController`.

- [ ] **房态实时推送功能** [P1]
  - [ ] **WebSocket集成**: 集成RuoYi框架的WebSocket功能，支持房态实时推送
  - [ ] **房态变更监听**: 实现房间状态变更的事件监听机制
  - [ ] **实时推送服务**: 创建房态实时推送服务，支持按门店推送
  - [ ] **前端WebSocket客户端**: 实现前端WebSocket连接和房态更新处理
  - [ ] **推送性能优化**: 确保房态变更推送延迟 < 1秒
  - [ ] **连接管理**: 实现WebSocket连接的管理和异常处理

- [ ] **5. 核心订单管理 (`pms_core_orders`, `pms_core_order_items`)** [P0]
  - [ ] **Domain对象设计**:
    - [ ] `PmsCoreOrder`, `PmsCoreOrderBo`, `PmsCoreOrderVo` (应包含订单项列表、主要联系人简要信息、预订房型及分配房间信息，参考《PMS需求.md》13.5)。
    - [ ] `PmsCoreOrderItem`, `PmsCoreOrderItemBo`, `PmsCoreOrderItemVo`.
    - [ ] 实现 `order_status`, `order_source` 枚举 (参考《PMS数据模型.md》5.6节)。
  - [ ] **复杂查询实现**: `PmsCoreOrderMapper`
    - [ ] 多表关联查询（订单+订单项+房型+联系人）。
    - [ ] 按日期范围、状态、门店、联系人等复合条件查询。
  - [ ] **核心业务逻辑**: `IPmsCoreOrderService`
    - [ ] **订单创建**: 业务校验（房间可用性、价格计算基于`pms_room_pricing_rules`、库存检查、联系人关联）。
    - [ ] **订单状态流转**: 实现《PMS数据模型.md》4.1节定义的订单状态流转逻辑 (pending_confirmation -> confirmed -> checked_in -> checked_out / cancelled / no_show)。
    - [ ] 订单修改和取消逻辑（特定条件下）。
    - [ ] **自动创建财务账单** (`pms_finance_folios`) 基础框架。
    - [ ] **房态联动**: 订单操作（创建、确认、取消、入住、退房）应触发房态更新。
  - [ ] **事务管理**: 确保订单及关联操作的数据一致性。
  - [ ] **控制器**: `PmsCoreOrderController`. 实现《PMS需求.md》13.5节定义的订单管理核心API。

- [ ] **6. 订单状态历史追踪 (`pms_order_status_history`)** [P0]
  - [ ] **Domain对象**: `PmsOrderStatusHistory`, `PmsOrderStatusHistoryBo`, `PmsOrderStatusHistoryVo`.
    - [ ] 实现 `change_reason` 枚举。
  - [ ] **业务逻辑**: `IPmsOrderStatusHistoryService`. 
    - [ ] 订单状态变更时自动记录历史。
    - [ ] 查询订单状态变更历史。
    - [ ] 支持状态回滚操作（特殊情况）。
  - [ ] **控制器**: `PmsOrderStatusHistoryController`.

- [ ] **7. 入住客人信息管理 (`pms_order_guests`)** [P1]
  - [ ] **Domain对象**: `PmsOrderGuest`, `PmsOrderGuestBo`, `PmsOrderGuestVo`.
    - [ ] 实现 `guest_type`, `id_type`, `age_group` 枚举。
    - [ ] 支持与客户档案关联 (`contact_id` 字段)。
  - [ ] **业务逻辑**: `IPmsOrderGuestService`.
    - [ ] 入住客人信息管理。
    - [ ] 主客人与联系人档案关联。
    - [ ] 同行客人选择性关联。
    - [ ] 智能档案匹配和新档案创建。
  - [ ] **控制器**: `PmsOrderGuestController`.

- [ ] **8. 订单来源渠道管理 (`pms_core_channels`)** [P0]
  - [ ] **Domain对象**: `PmsCoreChannel`, `PmsCoreChannelBo`, `PmsCoreChannelVo`.
    - [ ] 实现 `channel_type` 枚举。
  - [ ] **业务逻辑**: `IPmsCoreChannelService`. 基础增删改查。门店级隔离（或按需租户级）。
  - [ ] **控制器**: `PmsCoreChannelController`.

- [ ] **9. 财务-账单管理 (`pms_finance_folios`)** [P0]
  - [ ] **Domain对象**: `PmsFinanceFolio`, `PmsFinanceFolioBo`, `PmsFinanceFolioVo` (可含交易流水列表，余额通过计算列或业务逻辑实现)。
    - [ ] 实现 `folio_status` 枚举 (参考《PMS数据模型.md》5.7节)。
  - [ ] **业务逻辑**: `IPmsFinanceFolioService`.
    - [ ] 订单确认时自动创建账单（基础框架）。
    - [ ] 查询账单、更新账单状态。
  - [ ] **控制器**: `PmsFinanceFolioController`. 实现《PMS需求.md》13.6节定义的账单查询API。

- [ ] **10. 财务-交易流水管理 (`pms_finance_transactions`)** [P0]
  - [ ] **Domain对象**: `PmsFinanceTransaction`, `PmsFinanceTransactionBo`, `PmsFinanceTransactionVo`.
    - [ ] 实现 `transaction_type` 枚举。
  - [ ] **业务逻辑**: `IPmsFinanceTransactionService`. 记录收款、退款等基础交易。
  - [ ] **控制器**: `PmsFinanceTransactionController`. 实现《PMS需求.md》13.6节定义的记录交易API。

- [ ] **11. 财务-支付方式管理 (`pms_finance_payment_methods`)** [P0]
  - [ ] **Domain对象**: `PmsFinancePaymentMethod`, `PmsFinancePaymentMethodBo`, `PmsFinancePaymentMethodVo`.
    - [ ] 实现 `method_type` 枚举。
  - [ ] **业务逻辑**: `IPmsFinancePaymentMethodService`. 基础增删改查。门店级（或租户级）。
  - [ ] **控制器**: `PmsFinancePaymentMethodController`.

- [x] **12. 价格管理-价格规则基础 (`pms_room_pricing_rules`)** [P0]
  - [x] **Domain对象**: `PmsRoomPricingRule`, `PmsRoomPricingRuleBo`, `PmsRoomPricingRuleVo`.
    - [x] 实现 `price_adjustment_type` 枚举。
    - [x] 实现 `pricing_rule_status` 枚举。
    - [x] 创建枚举转换器 `PriceAdjustmentTypeConverter`, `PricingRuleStatusConverter`。
  - [x] **业务逻辑**: `IPmsRoomPricingRuleService`. 
    - [x] 基础增删改查。
    - [x] 价格规则优先级管理。
    - [x] 规则有效性校验。
  - [x] **价格计算引擎**: `IPmsPricingCalculationService`.
    - [x] 实现动态价格计算逻辑。
    - [x] 多规则叠加计算。
    - [x] 价格计算历史记录。
    - [x] 集成缓存机制优化性能。
    - [x] 支持并行计算（长期住宿）。
    - [x] 特殊日期价格处理。
  - [x] **缓存策略实现**: `PricingCacheService`
    - [x] 活跃价格规则缓存（Redis + 本地缓存）。
    - [x] 特殊日期价格缓存。
    - [x] 房型基础价格缓存。
    - [x] 价格计算结果缓存。
    - [x] 缓存失效和预热机制。
  - [x] **控制器**: `PmsRoomPricingRuleController`, `PmsPricingCalculationController`.

- [x] **13. 价格计算历史追踪 (`pms_pricing_calculations`)** [P0]
  - [x] **Domain对象**: `PmsPricingCalculation`, `PmsPricingCalculationBo`, `PmsPricingCalculationVo`.
  - [x] **业务逻辑**: `IPmsPricingCalculationService`.
    - [x] 记录每次价格计算的详细过程。
    - [x] 规则应用详情记录。
    - [x] 历史价格查询和趋势分析。
  - [x] **控制器**: `PmsPricingCalculationController`.

- [x] **14. 特殊日期价格管理 (`pms_special_date_pricing`)** [P1]
  - [x] **Domain对象**: `PmsSpecialDatePricing`, `PmsSpecialDatePricingBo`, `PmsSpecialDatePricingVo`.
    - [x] 实现 `date_type` 枚举 (`SpecialDateType`)。
    - [x] 实现 `special_date_status` 枚举 (`SpecialDateStatus`)。
    - [x] 创建枚举转换器 `SpecialDateTypeConverter`, `SpecialDateStatusConverter`。
  - [x] **业务逻辑**: `IPmsSpecialDatePricingService`.
    - [x] 特殊日期价格设置。
    - [x] 批量设置连续日期价格。
    - [x] 优先级控制和规则覆盖。
    - [x] 冲突检测和解决。
  - [x] **控制器**: `PmsSpecialDatePricingController`.

- [ ] **15. 库存管理与房态联动 (`pms_room_inventory_snapshot`)** [P1]
  - [ ] **Domain对象**: `PmsRoomInventorySnapshot`, `PmsRoomInventorySnapshotBo`, `PmsRoomInventorySnapshotVo`.
  - [ ] **业务逻辑**: `IPmsRoomInventorySnapshotService`.
    - [ ] 实时库存查询。
    - [ ] 库存快照生成。
    - [ ] 超售保护机制。
    - [ ] 房态实时同步。
  - [ ] **控制器**: `PmsRoomInventorySnapshotController`.

- [x] **系统配置管理模块建表语句**:
  - [x] 创建 `script/sql/pms_system_tables.sql` 文件
  - [x] 包含租户配置表：`pms_tenant_settings`
  - [x] 包含小程序配置表：`pms_mp_settings`
  - [x] 包含用户设备表：`pms_tenant_user_devices`
  - [x] 确保所有表结构与《PMS数据模型.md》v5.7完全一致

- [x] **系统配置管理功能菜单创建**:
  - [x] 创建 `script/sql/pms_system_menu.sql` 文件
  - [x] 包含系统配置主菜单和子菜单
  - [x] 包含相关权限点定义
  - [x] 按照RuoYi框架的菜单结构标准创建

## 阶段 3: 数据库表结构创建与菜单配置

- [x] **订单管理模块建表语句**:
  - [x] 创建 `script/sql/pms_order_tables.sql` 文件
  - [x] 包含订单核心表：`pms_core_orders`, `pms_core_order_items`, `pms_core_channels`
  - [x] 包含订单增强表：`pms_order_status_history`, `pms_order_guests`
  - [x] 包含库存管理表：`pms_room_inventory_snapshot`
  - [x] 确保所有表结构与《PMS数据模型.md》v5.7完全一致

- [x] **价格管理模块建表语句**:
  - [x] 创建 `script/sql/pms_pricing_tables.sql` 文件
  - [x] 包含价格规则表：`pms_room_pricing_rules`
  - [x] 包含价格计算表：`pms_pricing_calculations`
  - [x] 包含特殊日期价格表：`pms_special_date_pricing`
  - [x] 确保所有表结构与《PMS数据模型.md》v5.7完全一致

- [x] **订单管理功能菜单创建**:
  - [x] 创建 `script/sql/pms_order_menu.sql` 文件
  - [x] 包含订单管理主菜单和子菜单
  - [x] 包含相关权限点定义
  - [x] 按照RuoYi框架的菜单结构标准创建

- [x] **价格管理功能菜单创建**:
  - [x] 创建 `script/sql/pms_pricing_menu.sql` 文件
  - [x] 包含价格管理主菜单和子菜单
  - [x] 包含相关权限点定义
  - [x] 按照RuoYi框架的菜单结构标准创建

## 阶段 4: 价格管理功能完整开发

- [x] **价格管理实体类创建**:
  - [x] 创建 `PmsRoomPricingRule.java` 实体类
  - [x] 创建 `PmsPricingCalculation.java` 实体类
  - [x] 创建 `IPricingService.java` 核心服务接口
  - [x] 包含完整的字段定义和校验注解

- [x] **价格规则管理功能**:
  - [x] 实现价格规则的CRUD操作
  - [x] 实现规则优先级管理
  - [x] 实现规则有效性校验
  - [x] 实现规则启用/禁用功能

- [x] **动态价格计算引擎**:
  - [x] 实现基础价格获取
  - [x] 实现多规则叠加计算逻辑
  - [x] 实现价格计算缓存机制
  - [x] 实现价格计算历史记录

- [x] **特殊日期价格管理**:
  - [x] 实现特殊日期价格设置
  - [x] 实现批量日期价格操作
  - [x] 实现价格优先级控制
  - [x] 实现节假日价格自动应用

- [x] **价格管理API接口**:
  - [x] 实现价格查询API
  - [x] 实现价格计算API
  - [x] 实现价格规则管理API
  - [x] 实现特殊日期价格API

- [x] **价格管理MyBatis XML映射文件**:
  - [x] 创建 `PmsRoomPricingRuleMapper.xml` 映射文件
  - [x] 创建 `PmsPricingCalculationMapper.xml` 映射文件
  - [x] 创建 `PmsSpecialDatePricingMapper.xml` 映射文件
  - [x] 实现复杂查询SQL语句

## 阶段 4.5: 系统配置管理功能完整开发

- [x] **系统配置管理实体类创建**:
  - [x] 创建 `PmsTenantSetting.java` 实体类
  - [x] 创建 `PmsTenantSettingBo.java` 和 `PmsTenantSettingVo.java`
  - [x] 创建 `PmsTenantSettingMapper.java` 接口
  - [x] 创建 `IPmsTenantSettingService.java` 服务接口
  - [x] 创建 `PmsMpSetting.java` 实体类
  - [x] 创建 `PmsMpSettingBo.java` 和 `PmsMpSettingVo.java`
  - [x] 创建 `PmsTenantUserDevice.java` 实体类
  - [x] 实现相关枚举值定义 (`SettingType` 等)

- [ ] **租户配置管理功能**:
  - [ ] 实现配置项的CRUD操作
  - [ ] 实现配置分组管理
  - [ ] 实现租户级和门店级配置继承
  - [ ] 实现敏感配置的加密存储
  - [ ] 实现配置项类型验证和转换

- [ ] **配置继承与覆盖机制**:
  - [ ] 实现租户级配置作为默认配置
  - [ ] 实现门店级配置覆盖租户级配置
  - [ ] 实现配置查询优先级逻辑
  - [ ] 实现配置合并策略
  - [ ] 实现配置删除后的继承恢复

- [ ] **敏感配置安全管理**:
  - [ ] 实现敏感配置AES加密存储
  - [ ] 实现敏感配置访问权限控制
  - [ ] 实现敏感配置操作审计日志
  - [ ] 实现前端敏感信息脱敏显示
  - [ ] 实现敏感配置权限分级管理

- [ ] **配置模板管理功能**:
  - [ ] 实现预定义配置模板创建
  - [ ] 实现配置模板分类管理
  - [ ] 实现配置模板一键应用功能
  - [ ] 实现配置模板导入导出
  - [ ] 实现配置模板版本管理和回滚

- [ ] **系统监控与审计功能**:
  - [ ] 实现配置变更监控和记录
  - [ ] 实现用户行为监控和分析
  - [ ] 实现系统性能监控指标
  - [ ] 实现安全审计日志管理
  - [ ] 实现异常配置变更报警机制

- [ ] **小程序配置管理功能**:
  - [ ] 实现小程序主题配置
  - [ ] 实现功能开关管理
  - [ ] 实现支付配置管理
  - [ ] 实现通知配置管理
  - [ ] 实现租户级和门店级配置

- [ ] **用户设备管理功能**:
  - [ ] 实现设备注册和管理
  - [ ] 实现推送令牌管理
  - [ ] 实现设备状态跟踪
  - [ ] 实现多设备登录控制
  - [ ] 实现设备安全审计

- [ ] **系统配置API接口**:
  - [ ] 实现配置查询API
  - [ ] 实现配置更新API
  - [ ] 实现配置批量操作API
  - [ ] 实现配置导入导出API
  - [ ] 实现设备管理API
  - [ ] 实现配置模板管理API
  - [ ] 实现系统监控API

- [ ] **系统配置前端页面**:
  - [ ] 租户配置管理页面
  - [ ] 门店配置管理页面
  - [ ] 小程序配置页面
  - [ ] 用户设备管理页面
  - [ ] 配置导入导出页面
  - [ ] 配置模板管理页面
  - [ ] 系统监控仪表板页面

- [ ] **预定义配置项实现**:
  - [ ] 预订规则配置项
  - [ ] 财务参数配置项
  - [ ] 界面外观配置项
  - [ ] 业务流程配置项
  - [ ] 小程序功能配置项

## 阶段 5: 前端开发 - 核心功能界面 [P0]

**通用前端开发规范 (参考《最佳实践》Chapter 5 & 7.2):**
  - [ ] 遵循Vue3 + TypeScript + Element Plus技术栈。
  - [ ] 使用Composition API和`<script setup>`语法。
  - [ ] 严格区分页面组件、业务组件、通用组件的职责。
  - [ ] 使用Pinia进行状态管理，按模块划分store。
  - [ ] 使用VueUse提供的组合式函数提升开发效率。
  - [ ] API调用统一使用封装的request方法，支持TypeScript类型定义。
  - [ ] 表单校验使用Element Plus的表单验证，结合自定义校验规则。
  - [ ] 国际化支持使用vue-i18n，按模块组织翻译文件。
  - [ ] 响应式设计，适配桌面端和移动端。
  - [ ] 使用 Cursor 辅助生成组件、优化代码、解决问题。

**Cursor AI协作技巧提升:**
  - [ ] **组件生成**: "请Cursor根据《PMS需求.md》14.1节客户管理页面设计要求，生成符合《最佳实践》的Vue3客户列表组件，包含筛选、分页、批量操作功能。"
  - [ ] **API集成**: "请Cursor帮我为客户管理模块创建TypeScript类型定义和API调用方法，对应后端《PMS需求.md》13.1节定义的接口。"
  - [ ] **状态管理**: "请Cursor设计客户管理模块的Pinia store，包含客户列表、标签管理、搜索状态等。"
  - [ ] **表单优化**: "请Cursor优化客户信息编辑表单的校验逻辑和用户体验，确保符合《PMS需求.md》的业务规则。"

**具体前端功能模块开发:**

- [ ] **前端项目结构搭建** (参考《最佳实践》Chapter 5.2):
  - [ ] 在 `ruoyi-plus-soybean/src/views/` 下创建 `pms` 目录。
  - [ ] 在 `pms` 目录下按功能模块创建子目录：`customer`, `room`, `order`, `finance`, `pricing`, `system`。
  - [ ] 在 `ruoyi-plus-soybean/src/service/api/` 下创建 `pms.ts`，定义PMS相关的API调用方法。
  - [ ] 在 `ruoyi-plus-soybean/src/typings/api/` 下创建 `pms.ts`，定义PMS相关的TypeScript类型。
  - [ ] 在 `ruoyi-plus-soybean/src/store/modules/` 下创建 `pms.ts`，定义PMS相关的Pinia store。

- [ ] **1. 客户管理前端模块** [P0]
  - [ ] **客户列表页面** (`src/views/pms/customer/index.vue`):
    - [ ] 实现客户列表展示，支持分页。
    - [ ] 实现多条件筛选：客户类型、状态、标签、姓名、电话。
    - [ ] 实现批量操作：添加标签、状态变更。
    - [ ] 集成《PMS需求.md》13.1.1定义的客户查询API。
  - [ ] **客户详情页面** (`src/views/pms/customer/detail.vue`):
    - [ ] 显示客户完整信息，支持编辑。
    - [ ] 标签管理：添加/移除标签，颜色显示。
    - [ ] 住宿历史：显示租户内所有门店的住宿记录。
    - [ ] 消费统计：总消费、入住次数、平均消费。
  - [ ] **客户标签管理** (`src/views/pms/customer/tags.vue`):
    - [ ] 标签列表管理，支持租户级和门店级标签。
    - [ ] 标签创建、编辑、删除，颜色选择。
    - [ ] 标签分类管理。

- [ ] **2. 房态管理前端模块** [P0]
  - [ ] **房态总览页面** (`src/views/pms/room/status.vue`):
    - [ ] 房间状态可视化：网格布局显示所有房间状态。
    - [ ] 颜色编码：可用(绿色)、占用(红色)、维护(黄色)、锁定(灰色)。
    - [ ] 实时更新：房态变更自动刷新。
    - [ ] 快速操作：点击房间快速变更状态。
  - [ ] **房型管理页面** (`src/views/pms/room/types.vue`):
    - [ ] 房型列表、创建、编辑、删除。
    - [ ] 房型设施配置、图片上传。
  - [ ] **房间管理页面** (`src/views/pms/room/rooms.vue`):
    - [ ] 房间列表、状态管理。
    - [ ] 房间锁定/解锁操作。
    - [ ] 清洁状态更新。

- [ ] **3. 订单管理前端模块** [P0]
  - [ ] **订单列表页面** (`src/views/pms/order/index.vue`):
    - [ ] 按门店筛选，显示当前门店订单。
    - [ ] 客户信息显示：关联租户级客户档案。
    - [ ] 支持快速操作：房间分配、状态变更。
    - [ ] 订单状态流转可视化。
  - [ ] **订单详情页面** (`src/views/pms/order/detail.vue`):
    - [ ] 订单完整信息展示和编辑。
    - [ ] 入住客人信息管理。
    - [ ] 订单状态历史追踪。
  - [ ] **订单创建页面** (`src/views/pms/order/create.vue`):
    - [ ] 订单创建向导。
    - [ ] 客户选择和关联。
    - [ ] 房型选择和价格计算。

- [ ] **4. 价格管理前端模块** [P0]
  - [ ] **价格规则管理页面** (`src/views/pms/pricing/rules.vue`):
    - [ ] 规则列表：显示所有价格规则及状态。
    - [ ] 规则创建向导：分步骤创建复杂规则。
    - [ ] 规则优先级拖拽排序。
    - [ ] 规则效果预览：显示规则应用后的价格变化。
  - [ ] **特殊日期价格管理** (`src/views/pms/pricing/special-dates.vue`):
    - [ ] 特殊日期价格设置。
    - [ ] 批量设置连续日期价格。
    - [ ] 日历视图显示特殊价格。
  - [ ] **价格计算历史** (`src/views/pms/pricing/calculations.vue`):
    - [ ] 价格计算历史查询。
    - [ ] 计算过程详情展示。
    - [ ] 价格趋势分析图表。

- [ ] **5. 财务管理前端模块** [P0]
  - [ ] **账单管理页面** (`src/views/pms/finance/folios.vue`):
    - [ ] 账单列表、状态管理。
    - [ ] 账单详情查看。
    - [ ] 费用项目添加。
  - [ ] **交易流水页面** (`src/views/pms/finance/transactions.vue`):
    - [ ] 交易记录查询。
    - [ ] 收款、退款操作。
    - [ ] 财务报表生成。

## 阶段 6: 小程序端开发 - 核心管理功能 [P2]

**小程序开发规范 (参考《最佳实践》和《PMS需求.md》17.1.1):**
  - [ ] 使用微信小程序原生开发或uni-app跨平台开发
  - [ ] 遵循小程序设计规范和用户体验标准
  - [ ] 实现与后端API的安全对接
  - [ ] 支持多租户和门店级数据隔离
  - [ ] 优化小程序性能和加载速度

**小程序核心功能模块:**

- [ ] **小程序基础框架搭建**:
  - [ ] 创建小程序项目结构
  - [ ] 配置小程序基础信息和权限
  - [ ] 集成API请求封装和错误处理
  - [ ] 实现用户登录和身份验证
  - [ ] 配置多租户和门店识别机制

- [ ] **移动端房态管理** [P2]
  - [ ] 房态总览页面：网格或列表展示房间状态
  - [ ] 房间状态变更：清洁、维护、占用状态更新
  - [ ] 房间锁定/解锁操作
  - [ ] 实时房态同步和推送通知

- [ ] **移动端客户管理** [P2]
  - [ ] 客户信息快速查询
  - [ ] 客户基本信息查看和编辑
  - [ ] 客户标签管理
  - [ ] 客户入住历史查询

- [ ] **移动端订单管理** [P2]
  - [ ] 订单列表查看和筛选
  - [ ] 订单详情查看和编辑
  - [ ] 快速办理入住/退房
  - [ ] 房间分配和变更
  - [ ] 订单状态更新

- [ ] **移动端清洁维护** [P2]
  - [ ] 清洁任务列表和分配
  - [ ] 清洁状态更新和确认
  - [ ] 维护任务管理
  - [ ] 工作量统计和报告

- [ ] **移动端基础价格查看** [P2]
  - [ ] 房型价格查询
  - [ ] 价格规则查看
  - [ ] 特殊日期价格显示

- [ ] **移动端系统通知** [P2]
  - [ ] 重要系统通知接收
  - [ ] 业务告警推送
  - [ ] 消息中心管理
  - [ ] 推送设置管理

## 阶段 7: 集成测试与优化

- [ ] **后端集成测试**:
  - [ ] 编写单元测试覆盖核心业务逻辑。
  - [ ] 编写集成测试验证API接口。
  - [ ] 测试多租户数据隔离。
  - [ ] 测试权限控制机制。

- [ ] **前后端联调测试**:
  - [ ] 验证API接口对接。
  - [ ] 测试数据流转正确性。
  - [ ] 测试用户操作流程。
  - [ ] 性能测试和优化。

- [ ] **用户体验优化**:
  - [ ] 界面响应式适配。
  - [ ] 操作流程优化。
  - [ ] 错误处理和提示优化。
  - [ ] 国际化支持完善。

## 阶段 8: 部署与文档

- [ ] **部署准备**:
  - [ ] 生产环境配置。
  - [ ] 数据库迁移脚本。
  - [ ] 系统监控配置。

- [ ] **文档完善**:
  - [ ] API文档更新。
  - [ ] 用户操作手册。
  - [ ] 部署运维文档。
  - [ ] 开发者文档。

---

**重要提醒:**
1. **严格按照优先级开发**: [P0] 功能必须优先完成并测试通过后，再进行 [P1] 和 [P2] 功能开发。
2. **文档驱动开发**: 所有开发都应严格按照《PMS需求.md》v4.3和《PMS数据模型.md》v5.7的定义进行。
3. **Cursor AI协作**: 充分利用Cursor的AI能力，提升开发效率和代码质量。
4. **代码质量**: 确保代码符合《最佳实践》的规范要求，包括命名、注释、结构等。
5. **测试驱动**: 每个功能模块完成后都要进行充分测试，确保功能正确性和稳定性。

- [x] **国际化翻译文件完善**:
  - [x] 完善PMS模块中文翻译文件 `pms.zh-cn.ts`
  - [x] 完善PMS模块英文翻译文件 `pms.en-us.ts`
  - [x] 添加价格管理相关翻译 `pricingRules`, `pricingCalculations`, `specialDates`
  - [x] 修复前端国际化键缺失问题

- [x] **前端主题设置优化**:
  - [x] 启用面包屑导航显示
  - [x] 启用标签栏显示
  - [x] 启用标签栏信息缓存
  - [x] 启用全屏水印显示
  - [x] 设置水印文本为 "RuoYi-Vue-Plus PMS系统"
  - [x] 修改菜单样式：PC端不自动折叠，移动端保留折叠功能
  - [x] 混合模式菜单优化：在顶部菜单混合模式下点击二级菜单不自动折叠侧边栏
  - [x] **菜单自动折叠问题完整修复**：
    - [x] 分析默认设置状态下菜单自动折叠的根本原因
    - [x] 简化菜单折叠逻辑：PC端完全不自动折叠，移动端保留自动折叠
    - [x] 移除复杂的布局模式判断，统一按设备类型处理
    - [x] 确保所有布局模式下PC端菜单行为一致
  - [x] 修复国际化翻译缺失问题
  - [x] 修复PmsSpecialDatePricing实体类字段映射问题

- [x] **数据库字段映射问题修复**:
  - [x] 修复 `specialDate` 字段映射到 `specific_date` 数据库字段
  - [x] 修复 `dateName` 字段映射到 `name` 数据库字段
  - [x] 添加缺失的数据库字段：`dateRangeStart`, `dateRangeEnd`, `fixedPrice`, `isRecurringYearly`, `minLengthOfStay`, `maxLengthOfStay`, `channelRestrictionsJson`
  - [x] 确保实体类与数据库表结构完全匹配

- [x] **数据模型文档字段命名一致性修正**:
  - [x] 更新 `pms_special_date_pricing` 表的字段定义，与实际数据库表结构保持完全一致
  - [x] 修正文档中的字段名称：`special_date` → `specific_date`, `date_name` → `name`
  - [x] 补充完整的字段定义，包括所有数据库表中的字段
  - [x] 确认实体类通过 `@TableField` 注解正确映射字段名称差异
  - [x] 维护数据库设计文档的权威性和准确性
