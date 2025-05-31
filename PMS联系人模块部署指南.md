# PMS联系人模块部署指南

## 概述

本文档提供PMS模块中联系人、标签、联系人标签关系三个功能的完整部署指南，确保前端页面能正常访问。

## 已完成功能清单

### ✅ 后端开发
- **Controller层**：完整的REST API接口
- **Service层**：完整的业务逻辑，包含复杂关联操作
- **Mapper层**：完整的数据访问层，支持复杂查询
- **Domain对象**：Entity、BO、VO完整定义

### ✅ 前端开发
- **页面组件**：
  - 联系人管理：`/src/views/pms/contact/index.vue`
  - 标签管理：`/src/views/pms/tag/index.vue`
  - 标签关联管理：`/src/views/pms/tag-relation/index.vue`
- **表单组件**：ContactForm.vue、TagForm.vue、RelationForm.vue
- **API服务层**：完整的API接口封装
- **TypeScript类型**：完整的类型定义

### ✅ 路由配置
- 路由自动生成完成
- 国际化配置完成

## 部署步骤

### 第一步：数据库部署

执行完整的数据库部署脚本：

```bash
# 在MySQL中执行
mysql -u root -p your_database < script/sql/pms/deploy_pms_complete.sql
```

该脚本包含：
1. **表结构创建**：
   - `pms_customer_contacts` - 客户联系人表
   - `pms_contact_tags` - 联系人标签表
   - `pms_contact_tag_relations` - 联系人标签关联表

2. **基础字典数据**：
   - 联系人类型、状态、性别、会员等级
   - 标签分类等字典配置

3. **菜单权限配置**：
   - PMS主菜单
   - 三个子功能菜单及按钮权限
   - 超级管理员权限分配

4. **测试数据**：
   - 3个测试联系人
   - 5个测试标签
   - 标签关联关系

### 第二步：后端服务启动

```bash
cd ruoyi-admin
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

验证后端服务：
- 访问 `http://localhost:8080/doc.html`
- 检查PMS模块API是否正常显示

### 第三步：前端服务启动

```bash
cd ruoyi-plus-soybean
npm run dev
```

验证前端服务：
- 访问 `http://localhost:3200`
- 登录系统后检查PMS菜单是否显示

### 第四步：功能验证

1. **菜单访问验证**：
   - PMS管理 → 客户联系人
   - PMS管理 → 联系人标签  
   - PMS管理 → 标签关联管理

2. **功能操作验证**：
   - 联系人的增删改查
   - 标签的增删改查
   - 标签关联的管理

## 功能特性

### 联系人管理
- ✅ 完整的CRUD操作
- ✅ 高级搜索（姓名、电话、类型、状态）
- ✅ 标签关联管理
- ✅ 数据导出功能
- ✅ 权限控制

### 标签管理
- ✅ 完整的CRUD操作
- ✅ 颜色选择器
- ✅ 分类管理
- ✅ 门店级/租户级标签
- ✅ 排序功能

### 标签关联管理
- ✅ 联系人与标签的关联
- ✅ 远程搜索功能
- ✅ 批量操作
- ✅ 关联关系验证

## 技术架构

### 后端技术栈
- Spring Boot 3.x
- MyBatis Plus
- Sa-Token权限框架
- 多租户支持

### 前端技术栈
- Vue 3 + TypeScript
- Naive UI组件库
- Soybean Admin Pro框架
- Vite构建工具

## 权限配置

### 菜单权限
- `pms:contacts:*` - 联系人管理权限
- `pms:contactTags:*` - 标签管理权限
- `pms:contactTagRelations:*` - 标签关联管理权限

### 按钮权限
每个功能模块包含：
- `query` - 查询权限
- `add` - 新增权限
- `edit` - 编辑权限
- `remove` - 删除权限
- `export` - 导出权限

## 数据模型

### 核心表关系
```
pms_customer_contacts (联系人表)
    ↓ 1:N
pms_contact_tag_relations (关联表)
    ↓ N:1
pms_contact_tags (标签表)
```

### 多租户支持
- 所有表支持租户级数据隔离
- 标签支持门店级和租户级两种范围
- 自动租户数据过滤

## 故障排查

### 常见问题

1. **菜单不显示**
   - 检查数据库菜单配置是否正确执行
   - 检查用户角色权限分配

2. **API调用失败**
   - 检查后端服务是否正常启动
   - 检查数据库连接配置

3. **前端页面报错**
   - 检查前端依赖是否安装完整
   - 检查API接口路径是否正确

4. **数据查询为空**
   - 检查测试数据是否正确插入
   - 检查多租户配置

### 日志查看
- 后端日志：`ruoyi-admin/logs/`
- 前端控制台：浏览器开发者工具

## 后续扩展

### 计划功能
- 联系人详情页面优化
- 标签统计分析
- 批量导入功能
- 高级筛选条件

### 性能优化
- 数据库索引优化
- 前端虚拟滚动
- API响应缓存

## 联系支持

如遇到部署问题，请检查：
1. 数据库脚本是否完整执行
2. 服务启动日志是否有错误
3. 网络和端口配置是否正确

---

**部署完成标志**：能够正常访问三个页面并进行基本的增删改查操作。 