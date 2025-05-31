# PMS 客户联系人功能配置清单

## 概述
本文档记录了为 `pms_customer_contacts` 功能完成的配置文件和 TypeScript 文件的创建和更新工作。

## 已完成的配置

### 1. 后端代码生成 ✅
- [x] 使用代码生成器生成了以下后端代码：
  - `PmsCustomerContactsController` - 客户联系人控制器
  - `PmsContactTagsController` - 联系人标签控制器  
  - `PmsContactTagRelationsController` - 联系人标签关联控制器
  - 对应的 Service、Mapper、Domain (Entity/BO/VO) 类
  - MyBatis XML 映射文件

### 2. 前端 TypeScript 类型定义 ✅
- [x] 更新 `ruoyi-plus-soybean/src/typings/api/pms.api.d.ts`
  - 添加 `CustomerContacts` 类型定义
  - 添加 `ContactTags` 类型定义
  - 添加 `ContactTagRelations` 类型定义
  - 添加对应的搜索参数和操作参数类型
  - 添加枚举类型：`ContactType`、`ContactStatus`、`Gender`、`IdType`、`MemberLevel`、`TagCategory`

### 3. 前端 API 服务文件 ✅
- [x] 创建 `ruoyi-plus-soybean/src/service/api/pms/customer-contacts.ts`
  - 客户联系人的增删改查 API
  - 导出功能 API
  - 数据字典选项获取 API
- [x] 创建 `ruoyi-plus-soybean/src/service/api/pms/contact-tags.ts`
  - 联系人标签的增删改查 API
  - 导出功能 API
  - 标签分类选项获取 API
- [x] 创建 `ruoyi-plus-soybean/src/service/api/pms/contact-tag-relations.ts`
  - 联系人标签关联的增删改查 API
  - 批量标签操作 API
  - 根据联系人获取标签 API

### 4. 国际化配置 ✅
- [x] 更新 `ruoyi-plus-soybean/src/locales/langs/zh-cn.ts`
  - 添加 `pms_customercontacts: '客户联系人'` 路由翻译
- [x] 更新 `ruoyi-plus-soybean/src/locales/langs/en-us.ts`
  - 添加 PMS 模块相关的英文翻译
  - `pms: 'PMS Management'`
  - `pms_contacts: 'Contact Management'`
  - `pms_customercontacts: 'Customer Contacts'`

### 5. 数据字典配置 ✅
- [x] 创建 `script/sql/pms_dict_data.sql`
  - 联系人类型字典 (`pms_contact_type`)
  - 联系人状态字典 (`pms_contact_status`)
  - 性别字典 (`pms_gender`)
  - 证件类型字典 (`pms_id_type`)
  - 会员等级字典 (`pms_member_level`)
  - 标签分类字典 (`pms_tag_category`)

### 6. API 索引更新 ✅
- [x] 更新 `ruoyi-plus-soybean/src/service/api/index.ts`
  - 导出 PMS 模块的所有 API 服务

## 路由配置状态
- [x] 路由已自动生成并配置在 `elegant-router` 系统中
  - `pms` - PMS 管理主路由
  - `pms_contacts` - 联系人管理
  - `pms_customercontacts` - 客户联系人

## 需要手动执行的步骤

### 1. 数据库初始化
```bash
# 1. 执行表结构创建脚本
mysql -u root -p your_database < script/sql/pms_tables.sql

# 2. 执行数据字典初始化脚本
mysql -u root -p your_database < script/sql/pms_dict_data.sql
```

### 2. 权限配置
需要在系统管理 -> 菜单管理中添加以下权限：
- `pms:customerContacts:list` - 查看客户联系人列表
- `pms:customerContacts:query` - 查询客户联系人详情
- `pms:customerContacts:add` - 新增客户联系人
- `pms:customerContacts:edit` - 编辑客户联系人
- `pms:customerContacts:remove` - 删除客户联系人
- `pms:customerContacts:export` - 导出客户联系人

类似地为 `contactTags` 和 `contactTagRelations` 添加对应权限。

### 3. 后端业务逻辑完善
需要在后端 Service 实现类中完善以下内容：
- `validEntityBeforeSave` 方法中的业务校验逻辑
- 唯一性约束检查（如手机号、邮箱等）
- 外键关联验证
- 数据完整性检查

### 4. 前端页面开发
虽然路由已配置，但需要开发对应的 Vue 页面：
- `ruoyi-plus-soybean/src/views/pms/customerContacts/index.vue`
- 相关的表单组件和列表组件

## API 接口说明

### 客户联系人 API
- `GET /pms/customerContacts/list` - 获取客户联系人列表
- `GET /pms/customerContacts/{contactId}` - 获取客户联系人详情
- `POST /pms/customerContacts` - 新增客户联系人
- `PUT /pms/customerContacts` - 修改客户联系人
- `DELETE /pms/customerContacts/{contactIds}` - 删除客户联系人
- `POST /pms/customerContacts/export` - 导出客户联系人

### 数据字典 API
- `GET /system/dict/data/type/pms_contact_type` - 获取联系人类型选项
- `GET /system/dict/data/type/pms_contact_status` - 获取联系人状态选项
- `GET /system/dict/data/type/pms_gender` - 获取性别选项
- `GET /system/dict/data/type/pms_id_type` - 获取证件类型选项
- `GET /system/dict/data/type/pms_member_level` - 获取会员等级选项

## 注意事项

1. **多租户支持**：所有 Entity 类都继承了 `TenantEntity`，确保数据隔离
2. **参数验证**：BO 类中已添加基础验证注解，可根据业务需求进一步完善
3. **权限控制**：所有 Controller 方法都添加了 `@SaCheckPermission` 注解
4. **日志记录**：增删改操作都添加了 `@Log` 注解
5. **重复提交**：新增和修改操作都添加了 `@RepeatSubmit` 注解

## 下一步工作

1. 开发前端页面组件
2. 完善后端业务逻辑验证
3. 添加单元测试和集成测试
4. 完善 API 文档
5. 进行功能测试和性能优化 