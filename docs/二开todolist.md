# PMS 模块二次开发 ToDoList

## 阶段 0: 项目初始化与理解

- [x] **阅读核心文档:**
  - [x] 仔细阅读《PMS数据模型.md》，完全理解PMS核心表的结构、字段定义、关系以及主键/外键/索引规范。
  - [x] 仔细阅读《PMS需求.md》，明确PMS核心模块 [P0] 阶段必须实现的功能需求（房态、订单核心生命周期、基础财务支付、租户级系统管理基础）。
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
  - [x] （可选）在 Cursor 中为项目设置特定上下文，如关键模块路径、技术栈等，以便更好地辅助开发。

## 阶段 1: 后端开发 - 模块搭建与核心表结构

- [ ] **创建后端Maven子模块 `ruoyi-pms`** (参考《最佳实践》Chapter 2.2, 4.2, 8.3):
  - [ ] 在 `ruoyi-vue-plus/ruoyi-modules` 目录下创建新的 Maven 子模块 `ruoyi-pms`。
  - [ ] 配置 `ruoyi-pms/pom.xml`：
    - [ ] 设置 `<parent>` 指向 `ruoyi-modules`。
    - [ ] 添加必要的公共模块依赖 (如: `ruoyi-common-core`, `ruoyi-common-mybatis`, `ruoyi-common-web`, `ruoyi-common-satoken`, `ruoyi-common-excel`, `ruoyi-common-translate`, `ruoyi-common-tenant`)。
  - [ ] 在项目根目录 `pom.xml` 和 `ruoyi-modules/pom.xml` 的 `<modules>` 部分注册 `ruoyi-pms`。
- [ ] **创建后端基础包结构** (参考《最佳实践》Chapter 4.2):
  - [ ] 在 `ruoyi-pms/src/main/java/` 下创建基础包，例如 `org.dromara.pms`。
  - [ ] 在 `org.dromara.pms` 下创建标准分层包：`controller`, `service`, `service.impl`, `mapper`, `domain` (包含 `entity`, `bo`, `vo`)。
- [ ] **数据库表创建与初始化**:
  - [ ] **执行SQL脚本**: 确保已在开发数据库中执行 `script/sql/pms_tables.sql` 文件，创建所有PMS相关的表。
  - [ ] **验证表结构**: 对照《PMS数据模型.md》仔细检查已创建的表结构、字段类型、约束、索引是否正确。
  - [ ] **验证基础数据**: 确认 `pms_core_channels` 和 `pms_finance_payment_methods` 表的基础数据已按 `pms_tables.sql` 中的`INSERT`语句正确插入。
- [ ] **集成新模块到系统**:
  - [ ] (如果需要)在 `ruoyi-admin` 模块的 `application.yml` 中，确保新模块的包路径 (如 `org.dromara.pms`) 被扫描到。
  - [ ] (如果需要)配置MyBatis Plus扫描新模块的Mapper XML文件路径。
- [ ] **后端国际化资源文件**:
  - [ ] 在 `ruoyi-pms/src/main/resources/` 下创建 `i18n` 目录。
  - [ ] 在 `i18n` 目录下创建基础的国际化属性文件，如 `messages_zh_CN.properties` 和 `messages_en_US.properties`。

## 阶段 2: 后端开发 - 核心业务功能 [P0]

**通用后端开发规范 (参考《最佳实践》Chapter 4 & 7.1):**
  - [ ] 遵循分层架构约定 (Controller -> Service -> Mapper)。
  - [ ] 严格区分 Entity, BO, VO 的职责和使用场景。
  - [ ] 使用 MapStruct Plus (`@AutoMapper`) 进行对象转换。
  - [ ] Service 层实现类的方法应添加 `@Transactional` 注解。
  - [ ] Controller 层方法应添加 `@Log` 操作日志注解和 `@SaCheckPermission` 权限注解。
  - [ ] BO 对象字段使用 JSR 303/380 注解进行参数校验。
  - [ ] VO 对象字段按需使用 `@Translation` 和 `@Sensitive` 注解。
  - [ ] 编写清晰的 Javadoc 注释和行内注释。
  - [ ] 使用 Cursor 辅助生成代码、分析逻辑、优化代码。

**具体功能模块开发:**

- [ ] **代码生成器应用 (可选但推荐)** (参考《最佳实践》Chapter 2.4):
  - [ ] 针对《PMS数据模型.md》中定义的核心表 (如 `pms_room_types`, `pms_room_rooms`, `pms_core_orders` 等)，使用若依代码生成器生成初始的 Entity, Mapper, Service, Controller, BO, VO。
  - [ ] **Cursor辅助**: "请Cursor分析代码生成器为 `pms_room_types` 表生成的后端代码，并指出哪些部分需要根据《PMS需求.md》和《最佳实践》进行调整。"

- [ ] **1. 房型管理 (`pms_room_types`)**:
  - [ ] **Domain**: 创建/调整 `PmsRoomType`, `PmsRoomTypeBo`, `PmsRoomTypeVo`。
  - [ ] **Mapper**: 创建/调整 `PmsRoomTypeMapper.java` 和对应的 `PmsRoomTypeMapper.xml`。
  - [ ] **Service**: 创建/调整 `IPmsRoomTypeService.java` 和 `PmsRoomTypeServiceImpl.java` (实现CRUD逻辑)。
  - [ ] **Controller**: 创建/调整 `PmsRoomTypeController.java` (暴露RESTful API)。

- [ ] **2. 房间管理 (`pms_room_rooms`)**:
  - [ ] **Domain**: `PmsRoomRoom`, `PmsRoomRoomBo`, `PmsRoomRoomVo`.
  - [ ] **Mapper**: `PmsRoomRoomMapper.java` & XML.
  - [ ] **Service**: `IPmsRoomRoomService.java` & Impl (CRUD, 更新房间状态/清洁状态逻辑).
  - [ ] **Controller**: `PmsRoomRoomController.java`.

- [ ] **3. 房间锁定 (`pms_room_locks`)**:
  - [ ] **Domain**: `PmsRoomLock`, `PmsRoomLockBo`, `PmsRoomLockVo`.
  - [ ] **Mapper**: `PmsRoomLockMapper.java` & XML.
  - [ ] **Service**: `IPmsRoomLockService.java` & Impl (CRUD).
  - [ ] **Controller**: `PmsRoomLockController.java`.

- [ ] **4. 核心订单管理 (`pms_core_orders`, `pms_core_order_items`)** (参考《PMS需求.md》4.2.2 [P0]):
  - [ ] **Domain**:
    - `PmsCoreOrder`, `PmsCoreOrderBo`, `PmsCoreOrderVo` (应包含订单项列表).
    - `PmsCoreOrderItem`, `PmsCoreOrderItemBo`, `PmsCoreOrderItemVo`.
  - [ ] **Mapper**: `PmsCoreOrderMapper`, `PmsCoreOrderItemMapper` & XMLs.
  - [ ] **Service**: `IPmsCoreOrderService`, `IPmsCoreOrderItemService` & Impls (实现订单创建、查询、修改、取消、状态流转核心逻辑；订单项增删改查).
    - [ ] 实现订单状态转换逻辑 (pending_confirmation -> confirmed -> checked_in -> checked_out / cancelled / no_show).
    - [ ] 订单创建时关联/创建 `cmn_contacts` (基础字段)。
    - [ ] 订单创建时关联 `pms_core_channels`。
    - [ ] 订单创建/确认时，自动创建或关联 `pms_finance_folios`。
  - [ ] **Controller**: `PmsCoreOrderController`.

- [ ] **5. 订单来源渠道 (`pms_core_channels`)**:
  - [ ] **Domain**: `PmsCoreChannel`, `PmsCoreChannelBo`, `PmsCoreChannelVo`.
  - [ ] **Mapper**: `PmsCoreChannelMapper` & XML.
  - [ ] **Service**: `IPmsCoreChannelService` & Impl (CRUD).
  - [ ] **Controller**: `PmsCoreChannelController`.

- [ ] **6. 财务-账单管理 (`pms_finance_folios`)** (参考《PMS需求.md》4.2.3 [P0]):
  - [ ] **Domain**: `PmsFinanceFolio`, `PmsFinanceFolioBo`, `PmsFinanceFolioVo` (应包含交易流水列表).
  - [ ] **Mapper**: `PmsFinanceFolioMapper` & XML.
  - [ ] **Service**: `IPmsFinanceFolioService` & Impl (CRUD, 更新账单状态, 计算余额逻辑 - 应用层计算).
  - [ ] **Controller**: `PmsFinanceFolioController`.

- [ ] **7. 财务-交易流水 (`pms_finance_transactions`)**:
  - [ ] **Domain**: `PmsFinanceTransaction`, `PmsFinanceTransactionBo`, `PmsFinanceTransactionVo`.
  - [ ] **Mapper**: `PmsFinanceTransactionMapper` & XML.
  - [ ] **Service**: `IPmsFinanceTransactionService` & Impl (CRUD, 关联 `pms_finance_payment_methods`).
  - [ ] **Controller**: `PmsFinanceTransactionController`.

- [ ] **8. 财务-支付方式 (`pms_finance_payment_methods`)**:
  - [ ] **Domain**: `PmsFinancePaymentMethod`, `PmsFinancePaymentMethodBo`, `PmsFinancePaymentMethodVo`.
  - [ ] **Mapper**: `PmsFinancePaymentMethodMapper` & XML.
  - [ ] **Service**: `IPmsFinancePaymentMethodService` & Impl (CRUD).
  - [ ] **Controller**: `PmsFinancePaymentMethodController`.

- [ ] **9. 财务-附加费用项目 (`pms_finance_extra_charge_items`)**:
  - [ ] **Domain**: `PmsFinanceExtraChargeItem`, `PmsFinanceExtraChargeItemBo`, `PmsFinanceExtraChargeItemVo`.
  - [ ] **Mapper**: `PmsFinanceExtraChargeItemMapper` & XML.
  - [ ] **Service**: `IPmsFinanceExtraChargeItemService` & Impl (CRUD).
  - [ ] **Controller**: `PmsFinanceExtraChargeItemController`.

- [ ] **10. 价格规则 (`pms_room_pricing_rules`)**:
  - [ ] **Domain**: `PmsRoomPricingRule`, `PmsRoomPricingRuleBo`, `PmsRoomPricingRuleVo`.
  - [ ] **Mapper**: `PmsRoomPricingRuleMapper` & XML.
  - [ ] **Service**: `IPmsRoomPricingRuleService` & Impl (CRUD).
  - [ ] **Controller**: `PmsRoomPricingRuleController`.

- [ ] **11. 联系人基础 (`cmn_contacts`)** (P0阶段主要为订单关联所需字段):
  - [ ] **Domain**: `CmnContact`, `CmnContactBo`, `CmnContactVo` (仅包含P0阶段所需字段，如姓名、电话).
  - [ ] **Mapper**: `CmnContactMapper` & XML.
  - [ ] **Service**: `ICmnContactService` & Impl (提供基础的联系人查询、创建接口供订单模块调用).
  - [ ] **Controller**: (P0阶段可能不需要完整独立的Controller，主要通过订单业务间接操作).

- [ ] **12. 租户/部门特定配置 (`pms_tenant_settings`, `pms_mp_settings`)**:
  - [ ] **Domain**: `PmsTenantSetting`, `PmsTenantSettingBo`, `PmsTenantSettingVo` & 类似的 `PmsMpSetting` 对象.
  - [ ] **Mapper**: `PmsTenantSettingMapper`, `PmsMpSettingMapper` & XMLs.
  - [ ] **Service**: `IPmsTenantSettingService`, `IPmsMpSettingService` & Impls (CRUD, 按 group/key 查询).
  - [ ] **Controller**: `PmsTenantSettingController`, `PmsMpSettingController`.

- [ ] **13. 租户用户设备 (`pms_tenant_user_devices`)**: (用于民宿管理小程序推送等)
  - [ ] **Domain**: `PmsTenantUserDevice`, `PmsTenantUserDeviceBo`, `PmsTenantUserDeviceVo`.
  - [ ] **Mapper**: `PmsTenantUserDeviceMapper` & XML.
  - [ ] **Service**: `IPmsTenantUserDeviceService` & Impl (CRUD, 设备注册/更新逻辑).
  - [ ] **Controller**: `PmsTenantUserDeviceController`.

- [ ] **API文档**:
  - [ ] 为所有Controller和DTO添加清晰的Swagger/Knife4j注解 (参考《最佳实践》Chapter 8.3)。

## 阶段 3: 前端开发 - 核心管理界面 [P0] (Soybean Admin Pro)

**通用前端开发规范 (参考《最佳实践》Chapter 5 & 7.2):**
  - [ ] 遵循 Soybean Admin Pro 的项目结构和编码规范。
  - [ ] 使用 Vue 3 Composition API + `<script setup>`。
  - [ ] 为 props, emits, reactive state, methods 提供 TypeScript 类型。
  - [ ] 优先使用框架提供的 Hooks (`useTable`, `useForm` 等) 和 Naive UI 组件。
  - [ ] 组件样式使用 scoped CSS 或 CSS Modules。
  - [ ] 使用 Cursor 辅助生成组件、分析代码、实现逻辑。

**具体功能模块开发:**

- [ ] **前端项目结构搭建** (参考《最佳实践》Chapter 5.2):
  - [ ] 在 `src/views/`下创建 `pms` 目录，并为各管理功能创建子目录 (如 `room-type`, `room`, `order`, `folio` 等)。
  - [ ] 在 `src/service/api/` 下创建 `pms` 目录，并为各后端Controller创建对应的 `ts` 服务文件 (如 `roomType.ts`, `order.ts`)。
  - [ ] 在 `src/typings/api/` 下创建 `pms` 目录，定义与后端BO/VO对应的TypeScript接口。
  - [ ] 创建 `src/router/routes/modules/pms.ts` 路由配置文件。
  - [ ] (可选) 在 `src/store/modules/` 下创建 `pms` 目录，用于存放PMS相关的Pinia store。
  - [ ] **Cursor辅助**: "请Cursor根据《最佳实践》前端项目结构，为PMS模块生成基础的目录和文件占位符。"

- [ ] **1. 房型管理界面 (`src/views/pms/room-type/`)**:
  - [ ] **API服务**: `src/service/api/pms/roomType.ts` (实现对后端 `PmsRoomTypeController` 的调用)。
  - [ ] **类型定义**: `src/typings/api/pms/roomType.ts` (定义 `PmsRoomTypeVo` 等接口)。
  - [ ] **列表页**: `index.vue` (表格展示房型列表，支持搜索、分页、新增、编辑、删除按钮)。
  - [ ] **表单组件**: `components/RoomTypeForm.vue` (用于新增/编辑房型的弹窗或抽屉表单，包含字段校验)。

- [ ] **2. 房间管理界面 (`src/views/pms/room/`)**:
  - [ ] **API服务**: `src/service/api/pms/room.ts`.
  - [ ] **类型定义**: `src/typings/api/pms/room.ts`.
  - [ ] **列表页**: `index.vue` (展示房间列表，支持按房型、状态等搜索，操作按钮)。
  - [ ] **表单组件**: `components/RoomForm.vue`.
  - [ ] **房态图/日历视图基础**: (《PMS需求.md》4.2.1 [P0] 网格日历视图 - 此为复杂功能，P0阶段可能先实现基础列表和状态管理)

- [ ] **3. 订单管理界面 (`src/views/pms/order/`)**:
  - [ ] **API服务**: `src/service/api/pms/order.ts`.
  - [ ] **类型定义**: `src/typings/api/pms/order.ts`.
  - [ ] **列表页**: `index.vue` (展示订单列表，支持按日期、状态、联系人等搜索)。
  - [ ] **详情页/表单**: `detail.vue` 或 `components/OrderForm.vue` (用于创建/编辑/查看订单详情，包含订单项管理)。
    - [ ] 实现订单状态变更操作。
    - [ ] 关联联系人选择/创建。
    - [ ] 关联房型/房间选择。

- [ ] **4. 财务-账单管理界面 (`src/views/pms/folio/`)**:
  - [ ] **API服务**: `src/service/api/pms/folio.ts`.
  - [ ] **类型定义**: `src/typings/api/pms/folio.ts`.
  - [ ] **列表页/详情页**: (展示账单列表，点击可查看账单详情及交易流水)。

- [ ] **5. 财务-支付方式管理界面 (`src/views/pms/payment-method/`)**:
  - [ ] **API服务**: `src/service/api/pms/paymentMethod.ts`.
  - [ ] **类型定义**: `src/typings/api/pms/paymentMethod.ts`.
  - [ ] **列表页**: `index.vue` (管理支付方式)。
  - [ ] **表单组件**: `components/PaymentMethodForm.vue`.

- [ ] **6. 财务-附加费用项目管理界面 (`src/views/pms/extra-charge-item/`)**:
  - [ ] **API服务**: `src/service/api/pms/extraChargeItem.ts`.
  - [ ] **类型定义**: `src/typings/api/pms/extraChargeItem.ts`.
  - [ ] **列表页**: `index.vue`.
  - [ ] **表单组件**: `components/ExtraChargeItemForm.vue`.

- [ ] **7. 价格规则管理界面 (`src/views/pms/pricing-rule/`)**:
  - [ ] **API服务**: `src/service/api/pms/pricingRule.ts`.
  - [ ] **类型定义**: `src/typings/api/pms/pricingRule.ts`.
  - [ ] **列表页**: `index.vue`.
  - [ ] **表单组件**: `components/PricingRuleForm.vue`.

- [ ] **8. 租户/部门特定配置界面 (`src/views/pms/settings/`)**:
  - [ ] (按需实现，可能集成到系统现有配置管理或单独页面)

- [ ] **路由配置 (`src/router/routes/modules/pms.ts`)**:
  - [ ] 为以上所有PMS管理页面配置路由。
  - [ ] 配置菜单项 (名称、图标、排序、权限标识) (参考《最佳实践》Chapter 5.3)。
  - [ ] **Cursor辅助**: "请Cursor根据已创建的PMS前端页面，生成对应的 `pms.ts` 路由配置文件，并包含菜单定义。"

- [ ] **前端国际化**:
  - [ ] 将所有界面上的文本添加到 `src/locales/langs/zh-CN.ts` 和 `en-US.ts` 中。

- [ ] **权限控制**:
  - [ ] 使用 `useAuth` Hook 和权限指令，根据后端返回的权限标识控制按钮的显示/隐藏和页面的访问。

## 阶段 4: 民宿管理小程序 (Owner We App) [P0] - 基础功能

**技术栈**: Uni-App (参考《PMS需求.md》4.2.8)

- [ ] **项目搭建**: 创建 Uni-App 项目。
- [ ] **API对接**:
  - [ ] 封装调用后端PMS接口的请求函数 (复用或参考前端Web的API服务)。
  - [ ] 处理用户认证和token。
- [ ] **核心功能实现**:
  - [ ] **房态查看**:
    - [ ] 调用后端接口获取房态数据。
    - [ ] 以列表或简化日历形式展示房间状态。
  - [ ] **订单提醒**: (P0阶段可能为简单的订单列表，按预抵/预离排序)
    - [ ] 调用后端接口获取订单列表。
  - [ ] **日程查看 (今日预抵/预离)**:
    - [ ] 筛选并展示当日预抵和预离的订单。
  - [ ] **状态处理 (简单入住/退房操作)**:
    - [ ] 提供按钮或操作，调用后端接口更新订单状态为 `checked_in` 或 `checked_out`。
  - [ ] **数据概览 (核心收支数据)**: (P0阶段可简化，如当日收款总额)
  - [ ] **门店切换功能**: 如果用户关联多个门店，允许切换当前操作的门店上下文。
- [ ] **UI/UX设计**: 简洁易用，符合移动端操作习惯。
- [ ] **打包与测试**: 在微信开发者工具和真机上进行测试。

## 阶段 5: 集成、测试与部署

- [ ] **后端单元测试**:
  - [ ] 为核心Service层方法编写JUnit单元测试。
- [ ] **前后端联调** (参考《最佳实践》Chapter 6, 8.5):
  - [ ] 逐个功能模块进行前后端接口联调，确保数据交互正确。
  - [ ] **Cursor辅助**: "后端 `PmsOrderController` 的 `getOrderById` 接口返回数据格式与前端期望不一致，请Cursor分析可能的原因。"
- [ ] **系统集成测试**:
  - [ ] 测试PMS模块与若依基础模块 (用户、部门、租户、权限、字典等) 的集成情况。
  - [ ] 测试多租户、多部门数据隔离是否正确。
- [ ] **[P0] 功能验收测试 (UAT)**:
  - [ ] 根据《PMS需求.md》中[P0]功能点，逐一进行业务场景测试。
- [ ] **Bug修复与回归测试**:
- [ ] **数据库菜单与权限SQL**:
  - [ ] 编写将PMS模块前端路由配置的菜单信息插入到 `sys_menu` 表的SQL脚本。
  - [ ] (如果需要新的权限标识) 编写插入到 `sys_permission` (或其他权限相关表) 的SQL脚本，并关联到角色。
- [ ] **文档完善**:
  - [ ] 更新/创建PMS模块相关的开发文档、API文档 (确保Swagger/Knife4j是最新的)。
- [ ] **部署准备** (参考《最佳实践》Chapter 8.5):
  - [ ] 后端打包 (如 `mvn package -DskipTests`)。
  - [ ] 前端打包 (`pnpm build`)。
  - [ ] 小程序打包。
- [ ] **部署到测试/生产环境并验证**。

## 阶段 6: 持续迭代与优化 [P1+]

- [ ] 根据《PMS需求.md》中的 [P1], [P2] 优先级，规划后续迭代功能。
  - [ ] **订单管理[P1]**: 订单修改、款项管理、取消退款细则、自动化通知等。
  - [ ] **联系人与会员管理[P1]**: 统一联系人档案、证件管理。
  - [ ] **简易报表与分析[P1]**: 核心运营报表。
- [ ] 性能优化。
- [ ] 用户体验改进。
- [ ] 安全加固。

---
**注意**: 此ToDoList仅为初步计划，具体执行时需根据实际情况灵活调整。请频繁使用Cursor进行代码生成、分析、重构和问题排查，以提高开发效率和代码质量。
