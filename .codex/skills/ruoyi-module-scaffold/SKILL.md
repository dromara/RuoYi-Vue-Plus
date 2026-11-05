---
name: ruoyi-module-scaffold
description: 在 ruoyi-modules 下从零新建独立业务模块的脚手架。用于创建 Maven 模块骨架（pom.xml/包结构/resources）、生成 sys_menu 菜单与按钮权限 SQL、在 ruoyi-api 暴露跨模块接口、注册前端路由。触发后先读取对应 references，再按项目真实模块结构逐步生成。不适用基础框架迁移、中间件底层改造或在已有模块内新增单表 CRUD（后者用 ruoyi-plus-ai-coding）。
---

# RuoYi Plus 新模块脚手架

在 `ruoyi-modules` 下从零创建一个独立业务模块，覆盖 Maven 结构、菜单权限、跨模块接口、前端路由四个维度。

## 适用场景

- 需要新建一个完整独立业务模块（如 `ruoyi-crm`、`ruoyi-iot`），不只是在一个已有模块里加 CRUD。
- 新模块需要自己的菜单目录、页面权限、按钮权限。
- 新模块需要对外暴露接口供其他模块调用（通过 `ruoyi-api`）。
- 新模块需要在前端工程注册路由和页面。

## 不适用场景

- 在 `ruoyi-system` 等已有模块内新增单表 CRUD → 用 `ruoyi-plus-ai-coding`。
- 基础框架升级、Spring Boot 主版本迁移。
- 替换 ORM、缓存中间件等底层改造。
- 只改注释或 JavaDoc。

## 执行流程

1. 确认模块信息：模块名（如 `crm`）、artifactId（如 `ruoyi-crm`）、包名（如 `org.dromara.crm`）、中文名（如「客户管理」）。
2. 按「文档读取规则」读取需要的 reference。
3. 创建 Maven 模块骨架 → 见 [references/maven-structure.md](references/maven-structure.md)。
4. 如果需要跨模块接口 → 创建 `ruoyi-api` 接口 → 见 [references/cross-module-api.md](references/cross-module-api.md)。
5. 生成 `sys_menu` 菜单与权限 SQL → 见 [references/menu-permission.md](references/menu-permission.md)。
6. 生成前端路由注册指引 → 见 [references/frontend-route.md](references/frontend-route.md)。
7. 编译验证：
   ```bash
   ./mvnw clean compile -pl ruoyi-modules/ruoyi-{module} -am -q
   ```
8. 交付前按「快速检查清单」逐项确认。

## 文档读取规则

- Maven 模块创建、pom.xml 修改、包结构、依赖选择 → 先读 [references/maven-structure.md](references/maven-structure.md)。
- sys_menu 菜单/按钮权限 SQL、ID 段分配、角色授权 → 先读 [references/menu-permission.md](references/menu-permission.md)。
- ruoyi-api 跨模块接口、DTO、双接口实现模式 → 先读 [references/cross-module-api.md](references/cross-module-api.md)。
- 前端 plus-ui / plus-ui-react 目录约定、路由注册 → 先读 [references/frontend-route.md](references/frontend-route.md)。

只读取当前任务相关的 reference，不一次性展开所有文档。

## 优先级规则

发生冲突时按下面顺序决策：

1. 当前仓库已有模块的真实写法（如 `ruoyi-system`、`ruoyi-demo` 的 pom.xml 和包结构）。
2. 本 skill 的 reference 文档。
3. 通用 Maven / Spring Boot 习惯。

## 关键约定

### 模块命名

| 维度 | 规则 | 示例 |
|------|------|------|
| artifactId | `ruoyi-{module}` | `ruoyi-crm` |
| 包名 | `org.dromara.{module}` | `org.dromara.crm` |
| 菜单路径 | `{module}` | `crm` |
| 权限前缀 | `{module}:{business}:{action}` | `crm:customer:list` |
| 前端目录 | `src/views/{module}/{business}/index.vue` | `src/views/crm/customer/index.vue` |
| Mapper XML | `resources/mapper/{module}/XxxMapper.xml` | `resources/mapper/crm/CrmCustomerMapper.xml` |

### 自动扫描机制

项目通过以下配置自动发现新模块，**无需额外配置 ComponentScan 或 MapperScan**：

- `@SpringBootApplication`（[DromaraApplication](file:///workspace/ruoyi-admin/src/main/java/org/dromara/DromaraApplication.java)）扫描 `org.dromara` 基包 → 所有 `org.dromara.{module}` 自动生效。
- `mybatis-plus.mapperPackage: org.dromara.**.mapper` → 自动扫描所有 Mapper 接口。
- `mybatis-plus.mapperLocations: classpath*:mapper/**/*Mapper.xml` → 自动加载所有 XML。
- `mybatis-plus.typeAliasesPackage: org.dromara.**.domain` → 自动扫描所有实体。

### Maven 依赖声明位置

新模块的 `ruoyi-common-*` 依赖**不需要写版本号**，版本由根 pom 的 `dependencyManagement` 统一管理。只需声明 `groupId` + `artifactId`。

### 最小依赖集

新模块 pom.xml 至少依赖：

```xml
<!-- 基础工具 -->
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>ruoyi-common-core</artifactId>
</dependency>
<!-- 跨模块接口 -->
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>ruoyi-api</artifactId>
</dependency>
<!-- 数据库 -->
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>ruoyi-common-mybatis</artifactId>
</dependency>
<!-- Web + 权限 -->
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>ruoyi-common-web</artifactId>
</dependency>
```

按需追加：`ruoyi-common-security`（Sa-Token）、`ruoyi-common-log`（操作日志）、`ruoyi-common-excel`（导入导出）、`ruoyi-common-redis`（缓存）、`ruoyi-common-translation`（翻译）、`ruoyi-common-oss`（文件存储）等。

详细依赖选择指南见 [references/maven-structure.md](references/maven-structure.md)。

## 需要修改的文件清单

新建一个 `ruoyi-{module}` 模块需要改动以下文件：

| 序号 | 文件 | 操作 | 说明 |
|------|------|------|------|
| 1 | `ruoyi-modules/pom.xml` | 修改 | `<modules>` 追加 `<module>ruoyi-{module}</module>` |
| 2 | `ruoyi-modules/ruoyi-{module}/pom.xml` | 新建 | parent 指向 `ruoyi-modules`，声明依赖 |
| 3 | `ruoyi-admin/pom.xml` | 修改 | `<dependencies>` 追加新模块依赖 |
| 4 | `ruoyi-modules/ruoyi-{module}/src/main/java/org/dromara/{module}/` | 新建 | 包结构骨架 |
| 5 | `ruoyi-modules/ruoyi-{module}/src/main/resources/mapper/{module}/` | 新建 | Mapper XML 目录 |
| 6 | `ruoyi-api/src/main/java/org/dromara/{module}/api/` | 新建（如需跨模块接口） | 接口 + DTO |
| 7 | `script/sql/` | 新建 | `sys_menu` INSERT SQL |

## 快速检查清单

- [ ] `ruoyi-modules/pom.xml` 的 `<modules>` 已追加新模块。
- [ ] 新模块 `pom.xml` 的 parent 是 `ruoyi-modules`，不是 `ruoyi-vue-plus`。
- [ ] `ruoyi-admin/pom.xml` 已追加新模块依赖。
- [ ] 包名是 `org.dromara.{module}`，不是 `org.dromara.system.{module}`。
- [ ] `resources/mapper/{module}/` 目录已创建。
- [ ] `mybatis-plus` 配置无需修改（`org.dromara.**` 通配已覆盖）。
- [ ] `sys_menu` SQL 包含：1 个目录菜单 + 1 个页面菜单 + 5-6 个按钮权限。
- [ ] 菜单 `perms` 字段与 Controller `@SaCheckPermission` 值完全对应。
- [ ] 如有跨模块接口，`ruoyi-api` 下接口包名是 `org.dromara.{module}.api`。
- [ ] ServiceImpl 同时 `implements I{Module}Service, {Module}Service`（如需暴露跨模块接口）。
- [ ] `./mvnw clean compile -pl ruoyi-modules/ruoyi-{module} -am -q` 编译通过。
- [ ] 前端路由 path、component 路径与后端 `@RequestMapping` 一一对应。

## 推荐提问方式

```text
使用 $ruoyi-module-scaffold 新建一个 ruoyi-crm 模块：
1. 模块名：crm，中文名：客户管理
2. 需要客户表 crud + 数据权限 + 导出
3. 需要对外暴露客户查询接口（供 workflow 模块调用）
4. 菜单挂在「系统管理」目录下
5. 参考现有 ruoyi-system 模块结构
```
