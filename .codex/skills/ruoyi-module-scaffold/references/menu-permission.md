# sys_menu 菜单与权限 SQL

## sys_menu 表结构

```sql
-- 菜单类型 menu_type：M=目录  C=菜单(页面)  F=按钮
-- 超级管理员(role_id=1761300000000000001)无需 sys_role_menu，代码 isSuperAdmin() 自动放行
```

| 字段 | 类型 | 说明 |
|------|------|------|
| menu_id | bigint | 菜单ID（雪花ID，需全局唯一） |
| menu_name | varchar(50) | 菜单名称 |
| parent_id | bigint | 父菜单ID（顶级为 0） |
| order_num | int | 显示顺序 |
| path | varchar(200) | 路由地址（如 `crm`、`customer`） |
| component | varchar(255) | 组件路径（如 `crm/customer/index`，目录和按钮为 null） |
| query_param | varchar(255) | 路由参数 |
| is_frame | char(1) | 是否外链（Y/N，默认 N） |
| is_cache | char(1) | 是否缓存（Y/N，默认 Y） |
| menu_type | char(1) | **M=目录 C=菜单 F=按钮** |
| visible | char(1) | 0=显示 1=隐藏 |
| status | char(1) | 0=正常 1=停用 |
| perms | varchar(100) | 权限标识（如 `crm:customer:list`） |
| icon | varchar(100) | 菜单图标（按钮为 `#`） |
| create_dept | bigint | 创建部门 |
| create_by | bigint | 创建者 |
| create_time | datetime | 创建时间 |
| remark | varchar(500) | 备注 |

## ID 段分配规则

现有 sys_menu ID 分布（见 [ry_vue.sql](file:///workspace/script/sql/ry_vue.sql)）：

| 类型 | ID 段 | 示例 |
|------|--------|------|
| 顶级目录 | `1761400000000000001` ~ `1761400000000000009` | 系统管理=001, 系统监控=002, 系统工具=003 |
| 页面菜单 | `1761400000000000100` ~ `1761400000000000999` | 用户管理=100, 角色管理=101... |
| 按钮权限 | `1761400000000001001` ~ `1761400000000009999` | 用户查询=1001, 用户新增=1002... |

**新模块推荐 ID 段**（按模块序号分配，避免冲突）：

```sql
-- 假设新模块序号为 N（从 10 开始，预留 1-9 给官方模块）
-- 顶级目录：176140000000000000{N}
-- 页面菜单：1761400000000000{N}00 ~ 1761400000000000{N}99
-- 按钮权限：17614000000000{N}001 ~ 17614000000000{N}999
```

**或者使用更安全的方式**：用当前时间戳生成雪花 ID，确保不冲突。

## SQL 模板

以新增 `ruoyi-crm` 模块的「客户管理」为例（假设模块序号 N=10）：

```sql
-- =============================================
-- ruoyi-crm 模块菜单与权限 SQL
-- 执行前请确认 menu_id 不与现有数据冲突
-- =============================================

-- 1. 顶级目录（menu_type=M）
-- 如果新模块挂到已有目录下（如系统管理），跳过此步，parent_id 直接用已有目录 ID
insert into sys_menu values
(1761400000000000010, '客户管理', 0, 6, 'crm', null, '', 'N', 'Y', 'M', '0', '0', '', 'people', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '客户管理目录');

-- 2. 页面菜单（menu_type=C）
-- parent_id 指向顶级目录；component 对应前端 views 下的路径；perms 对应 Controller @SaCheckPermission
insert into sys_menu values
(1761400000000000100, '客户信息', 1761400000000000010, 1, 'customer', 'crm/customer/index', '', 'N', 'Y', 'C', '0', '0', 'crm:customer:list', 'list', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '客户信息菜单');

-- 3. 按钮权限（menu_type=F）
-- parent_id 指向页面菜单；perms 与 Controller 注解一一对应
insert into sys_menu values
(1761400000000010001, '客户查询', 1761400000000000100, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'crm:customer:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values
(1761400000000010002, '客户新增', 1761400000000000100, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'crm:customer:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values
(1761400000000010003, '客户修改', 1761400000000000100, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'crm:customer:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values
(1761400000000010004, '客户删除', 1761400000000000100, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'crm:customer:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values
(1761400000000010005, '客户导出', 1761400000000000100, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'crm:customer:export', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

-- 4.（可选）导入按钮，如果模块支持 Excel 导入
insert into sys_menu values
(1761400000000010006, '客户导入', 1761400000000000100, 6, '', '', '', 'N', 'Y', 'F', '0', '0', 'crm:customer:import', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

-- 5.（可选）角色授权 — 为非超级管理员角色分配菜单权限
-- 超级管理员(role_id=1761300000000000001)无需此步，isSuperAdmin() 自动放行
-- 以下为测试角色(role_id=1761300000000000003)分配客户管理全部权限
insert into sys_role_menu values
(1761300000000000003, 1761400000000000010),
(1761300000000000003, 1761400000000000100),
(1761300000000000003, 1761400000000010001),
(1761300000000000003, 1761400000000010002),
(1761300000000000003, 1761400000000010003),
(1761300000000000003, 1761400000000010004),
(1761300000000000003, 1761400000000010005);
```

## perms 与 Controller 对应关系

`sys_menu.perms` 必须与 Controller 的 `@SaCheckPermission` 值完全一致：

| sys_menu.perms | Controller 注解 | 对应接口 |
|----------------|-----------------|----------|
| `crm:customer:list` | `@SaCheckPermission("crm:customer:list")` | `GET /crm/customer/list` |
| `crm:customer:query` | `@SaCheckPermission("crm:customer:query")` | `GET /crm/customer/{id}` |
| `crm:customer:add` | `@SaCheckPermission("crm:customer:add")` | `POST /crm/customer` |
| `crm:customer:edit` | `@SaCheckPermission("crm:customer:edit")` | `PUT /crm/customer` |
| `crm:customer:remove` | `@SaCheckPermission("crm:customer:remove")` | `DELETE /crm/customer/{ids}` |
| `crm:customer:export` | `@SaCheckPermission("crm:customer:export")` | `POST /crm/customer/export` |
| `crm:customer:import` | `@SaCheckPermission("crm:customer:import")` | `POST /crm/customer/import` |

## 挂载到已有目录

如果新模块不需要自己的顶级目录，而是挂到「系统管理」(menu_id=`1761400000000000001`)下：

```sql
-- 跳过顶级目录 SQL，页面菜单的 parent_id 直接用已有目录 ID
insert into sys_menu values
(1761400000000000110, '客户信息', 1761400000000000001, 10, 'customer', 'crm/customer/index', ...);
--                                                 ↑ 挂到「系统管理」目录下
```

现有顶级目录 ID：

| 目录 | menu_id | path |
|------|---------|------|
| 系统管理 | `1761400000000000001` | `system` |
| 系统监控 | `1761400000000000002` | `monitor` |
| 系统工具 | `1761400000000000003` | `tool` |
| 测试菜单 | `1761400000000000005` | `demo` |

## 多业务页面扩展

一个模块通常有多个业务页面（如 crm 模块有客户、联系人、合同等）。每个业务页面重复上面的「页面菜单 + 按钮权限」模式：

```sql
-- 客户信息（页面 ID = 1761400000000000100）
-- 联系人管理（页面 ID = 1761400000000000200）
insert into sys_menu values
(1761400000000000200, '联系人管理', 1761400000000000010, 2, 'contact', 'crm/contact/index', '', 'N', 'Y', 'C', '0', '0', 'crm:contact:list', 'phone', '', ...);
-- 联系人按钮（按钮 ID = 176140000000002001~005）
insert into sys_menu values
(1761400000000020001, '联系人查询', 1761400000000000200, 1, ...);
-- ...
```

**页面 ID 规则**：每个业务页面用百位区分（100/200/300...），按钮 ID 用千位区分（1001/2001/3001...）。

## SQL 文件位置

新模块的 SQL 文件放在 `script/sql/` 下，命名规则：`ry_{module}.sql`

```
script/sql/
├── ry_vue.sql          # 系统核心库
├── ry_workflow.sql     # 工作流库
├── ry_ai.sql           # AI 库
├── ry_job.sql          # 调度库
└── ry_{module}.sql     # 新模块（新建）
```

如果新模块的表也在同一个 `ry-vue` 库中（最常见），sys_menu SQL 直接追加到 `ry_{module}.sql` 文件，与建表 SQL 放在一起。
