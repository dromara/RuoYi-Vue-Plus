# 前端路由注册

## 前端工程位置

前端工程是独立仓库，不在当前 boot4 仓库内：

| 前端 | 仓库 | 分支 |
|------|------|------|
| Vue 3 + Element Plus | https://gitee.com/JavaLionLi/plus-ui | `6.X-Vue` |
| React + Ant Design Pro | https://gitee.com/JavaLionLi/plus-ui | `6.X-React` |

**后端仓库内的前端模板**：`ruoyi-modules/ruoyi-gen/src/main/resources/fm/vue/` 和 `fm/react/` 是代码生成器的 FreeMarker 模板，不是实际前端工程。

## 菜单驱动的路由

RuoYi-Vue-Plus 采用**后端菜单驱动路由**，前端路由由 `sys_menu` 表数据动态生成：

1. 用户登录后调用 `GET /system/menu/getRouters` 获取路由树。
2. 前端根据 `sys_menu` 的 `path`、`component`、`menu_type` 动态注册路由。
3. **不需要在前端手动写路由配置文件**。

因此，前端注册路由 = 在 `sys_menu` 表插入正确的记录（见 [menu-permission.md](menu-permission.md)）。

## 前端目录约定

### Vue（plus-ui）

```
plus-ui/src/
├── api/
│   └── {module}/
│       ├── {business}/
│       │   ├── index.ts        # API 请求函数
│       │   └── types.ts        # 类型定义（VO/Form/Query）
│       └── ...
├── views/
│   └── {module}/
│       ├── {business}/
│       │   └── index.vue       # 列表页（sys_menu.component 对应此路径）
│       └── ...
└── ...
```

`sys_menu.component` 值 = `views/` 下的相对路径（不含 `views/` 前缀和 `.vue` 后缀）。

例如 `component = 'crm/customer/index'` → `plus-ui/src/views/crm/customer/index.vue`

### React（plus-ui-react）

```
plus-ui-react/src/
├── api/
│   └── {module}/
│       ├── {business}/
│       │   ├── index.ts        # API 请求函数
│       │   └── types.ts        # 类型定义
│       └── ...
├── pages/
│   └── {module}/
│       └── {business}/
│           └── index.tsx       # 列表页
└── ...
```

## 前端文件生成

新模块的前端文件可以通过代码生成器一键生成：

1. 启动后端，访问代码生成器页面（`/tool/gen`）。
2. 导入新模块的数据库表。
3. 配置 `gen_table.frontend_type`（`vue` 或 `react`）。
4. 预览/下载生成的前端代码。
5. 将生成的文件放入前端工程对应目录。

或者直接参考 `ruoyi-modules/ruoyi-gen/src/main/resources/fm/vue/` 下的 FreeMarker 模板手动创建。

## API 文件约定

### Vue API 文件（api/{module}/{business}/index.ts）

```typescript
import request from '@/utils/request';
import { AxiosPromise } from '@/utils/api-types';
import { PageResult } from '@/api/types';
import { CrmCustomerVO, CrmCustomerForm, CrmCustomerQuery } from './types';

// 分页查询客户列表
export function listCustomer(query?: CrmCustomerQuery): AxiosPromise<PageResult<CrmCustomerVO>> {
  return request({
    url: '/crm/customer/list',
    method: 'get',
    params: query
  });
}

// 查询客户详情
export function getCustomer(customerId: string | number): AxiosPromise<CrmCustomerVO> {
  return request({
    url: '/crm/customer/' + customerId,
    method: 'get'
  });
}

// 新增客户
export function addCustomer(data: CrmCustomerForm) {
  return request({
    url: '/crm/customer',
    method: 'post',
    data: data
  });
}

// 修改客户
export function updateCustomer(data: CrmCustomerForm) {
  return request({
    url: '/crm/customer',
    method: 'put',
    data: data
  });
}

// 删除客户
export function delCustomer(customerIds: string | number | (string | number)[]) {
  return request({
    url: '/crm/customer/' + customerIds,
    method: 'delete'
  });
}
```

### Vue 类型文件（api/{module}/{business}/types.ts）

```typescript
import { BaseEntity, PageQuery } from '@/api/types';

export interface CrmCustomerVO {
  customerId: string | number;
  customerName: string;
  contactName: string;
  phoneNumber: string;
  // ...
}

export interface CrmCustomerForm {
  customerId?: string | number;
  customerName: string;
  contactName: string;
  phoneNumber: string;
}

export interface CrmCustomerQuery extends PageQuery {
  customerName?: string;
  phoneNumber?: string;
  params?: any;  // 日期范围等扩展参数
}
```

### React API 文件

```typescript
import { request } from '@/api/request';
import { R, PageResult } from '@/api/types';
import { CrmCustomerVO, CrmCustomerForm, CrmCustomerQuery } from './types';

// 分页查询
export function listCustomer(params?: CrmCustomerQuery) {
  return request<R<PageResult<CrmCustomerVO>>>({
    url: '/crm/customer/list',
    method: 'get',
    params
  });
}
```

## 路由对应关系检查清单

| sys_menu 字段 | Vue 对应 | React 对应 |
|---------------|----------|------------|
| `path` = `crm` | 路由 `/crm` | 路由 `/crm` |
| `path` = `customer` | 子路由 `/crm/customer` | 子路由 `/crm/customer` |
| `component` = `crm/customer/index` | `views/crm/customer/index.vue` | `pages/crm/customer/index.tsx` |
| `perms` = `crm:customer:list` | `v-hasPermi="['crm:customer:list']"` | `hasPermi(userInfo, ['crm:customer:list'])` |

## 注意事项

- 前端工程不在当前后端仓库内，本 skill 只生成前端文件的内容和路径约定，实际文件需要手动放入前端工程。
- 如果使用代码生成器生成前端代码，生成的文件已经符合上述目录约定。
- `sys_menu` 的 `component` 字段必须与前端实际文件路径一致，否则页面加载 404。
- 新模块如果不需要前端页面（纯后端服务模块如 `ruoyi-job`），跳过前端文件生成，`sys_menu` 只建按钮权限（`menu_type=F`）。
