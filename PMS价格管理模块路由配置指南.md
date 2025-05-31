# PMS价格管理模块路由配置指南

## 背景说明

由于elegant-router系统的限制，价格管理模块的路由需要通过系统菜单管理进行配置，而不能直接修改路由文件。

## 配置步骤

### 1. 登录系统管理后台

使用管理员账号登录系统，进入"系统管理" -> "菜单管理"

### 2. 创建价格管理主菜单

点击"新增"按钮，创建价格管理主菜单：

```
菜单名称：价格管理
菜单类型：目录
路由地址：/pms/pricing
组件路径：Layout
显示排序：4
菜单图标：icon-park-outline:calculator
菜单状态：正常
```

### 3. 创建价格规则管理菜单

在"价格管理"菜单下创建子菜单：

```
菜单名称：价格规则
菜单类型：菜单
路由地址：/pms/pricing/rules
组件路径：pms/pricing-rules/index
显示排序：1
菜单图标：icon-park-outline:setting-config
菜单状态：正常
权限标识：pms:pricingRules:list
```

### 4. 创建价格计算历史菜单

```
菜单名称：价格计算
菜单类型：菜单
路由地址：/pms/pricing/calculations
组件路径：pms/pricing-calculations/index
显示排序：2
菜单图标：icon-park-outline:calculator
菜单状态：正常
权限标识：pms:pricingCalculations:list
```

### 5. 创建特殊日期价格菜单

```
菜单名称：特殊日期
菜单类型：菜单
路由地址：/pms/pricing/special-dates
组件路径：pms/special-dates/index
显示排序：3
菜单图标：icon-park-outline:calendar
菜单状态：正常
权限标识：pms:specialDates:list
```

## 权限配置

### 1. 创建权限点

在"系统管理" -> "权限管理"中添加以下权限：

#### 价格规则权限
```
pms:pricingRules:list - 价格规则查询
pms:pricingRules:add - 价格规则新增
pms:pricingRules:edit - 价格规则编辑
pms:pricingRules:remove - 价格规则删除
pms:pricingRules:enable - 价格规则启用
pms:pricingRules:disable - 价格规则禁用
pms:pricingRules:copy - 价格规则复制
pms:pricingRules:export - 价格规则导出
```

#### 价格计算权限
```
pms:pricingCalculations:list - 价格计算查询
pms:pricingCalculations:calculate - 价格计算
pms:pricingCalculations:detail - 价格计算详情
pms:pricingCalculations:remove - 价格计算删除
pms:pricingCalculations:export - 价格计算导出
pms:pricingCalculations:analysis - 价格分析
```

#### 特殊日期权限
```
pms:specialDates:list - 特殊日期查询
pms:specialDates:add - 特殊日期新增
pms:specialDates:edit - 特殊日期编辑
pms:specialDates:remove - 特殊日期删除
pms:specialDates:enable - 特殊日期启用
pms:specialDates:disable - 特殊日期禁用
pms:specialDates:copy - 特殊日期复制
pms:specialDates:export - 特殊日期导出
```

### 2. 分配权限给角色

在"系统管理" -> "角色管理"中，为相应角色分配权限：

- **PMS管理员**：分配所有价格管理权限
- **前台接待**：分配价格查询和计算权限
- **财务人员**：分配价格规则管理和分析权限

## 菜单层级结构

配置完成后的菜单结构如下：

```
PMS系统
├── 联系人管理
├── 房型管理
├── 房间管理
├── 价格管理 (新增)
│   ├── 价格规则
│   ├── 价格计算
│   └── 特殊日期
└── 其他模块...
```

## 验证配置

### 1. 检查菜单显示

登录系统后，在左侧导航栏应该能看到"价格管理"菜单及其子菜单。

### 2. 检查页面访问

点击各个菜单项，确认能正常访问对应的页面：

- `/pms/pricing/rules` - 价格规则管理页面
- `/pms/pricing/calculations` - 价格计算历史页面
- `/pms/pricing/special-dates` - 特殊日期价格页面

### 3. 检查权限控制

使用不同权限的用户登录，验证权限控制是否生效：

- 有权限的用户能看到菜单和操作按钮
- 无权限的用户看不到相关菜单或按钮被禁用

## 常见问题

### Q1: 菜单配置后不显示
**A**: 检查以下几点：
1. 菜单状态是否为"正常"
2. 当前用户是否有对应权限
3. 路由地址是否正确
4. 组件路径是否存在

### Q2: 页面访问404错误
**A**: 检查：
1. 组件路径是否正确
2. Vue组件文件是否存在
3. 路由地址是否与菜单配置一致

### Q3: 权限控制不生效
**A**: 检查：
1. 权限标识是否正确
2. 用户角色是否分配了对应权限
3. 前端组件中的权限判断是否正确

## 技术说明

### elegant-router限制

elegant-router是一个基于文件系统的路由生成器，它有以下限制：

1. **自动生成**：路由是根据文件结构自动生成的
2. **类型安全**：有严格的TypeScript类型检查
3. **不可直接编辑**：生成的路由文件不能手动修改

### 解决方案

通过系统菜单管理配置路由的优势：

1. **动态配置**：可以在运行时动态配置菜单和路由
2. **权限集成**：与权限系统无缝集成
3. **用户友好**：管理员可以通过界面配置，无需修改代码

## 总结

通过系统菜单管理配置价格管理模块的路由是最佳实践，它不仅解决了elegant-router的限制问题，还提供了更好的权限控制和用户体验。

配置完成后，用户就可以正常使用价格管理模块的所有功能了。 