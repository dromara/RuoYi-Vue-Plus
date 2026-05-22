# Stars Web

GitHub Stars 知识库独立前端，基于 Vue 3 + TypeScript + Vite + Element Plus。

## 技术栈

- Vue 3 + TypeScript
- Vite 8
- Element Plus
- Vue Router + Pinia
- Axios + @vueuse/core

## 前置条件

- Node.js 18+
- 本地已启动 RuoYi 后端（默认 `http://localhost:8080`）
- 已执行 `script/sql/stars_library.sql` 并完成 Stars 模块部署
- 后端需启用 Kafka（enrichment 异步）与 DeepSeek 配置（见 `application-dev.yml` 中 `stars.*`）

## 开发

```bash
cd stars-web
npm install
npm run dev
```

开发服务器默认运行在 `http://localhost:5173`。

Vite 已将 `/dev-api` 代理到 `http://localhost:8080`，无需额外 CORS 配置。

## 构建

```bash
npm run build
npm run preview
```

构建产物输出到 `dist/`。

## 生产部署（Nginx）

1. 执行 `npm run build` 生成 `dist/`。
2. 将 `dist/` 同步到服务器（例如 `/var/www/stars-web`）。
3. 参考仓库根目录示例配置：[script/nginx/stars-web.conf](../script/nginx/stars-web.conf)。
4. 确保 Nginx 将 `/dev-api/` 反向代理到 RuoYi 后端（去掉前缀后转发到 `8080`）。

生产访问示例：`https://stars.example.com` → 静态资源；`https://stars.example.com/dev-api/stars/...` → 后端 API。

## 环境说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| API 代理 | `/dev-api` → `localhost:8080` | 见 `vite.config.ts` |
| RuoYi Client ID | `e5cd7e4891bf95d1d19206ce24a7b32e` | 见 `src/constants/index.ts` |
| 响应式断点 | 768px | PC 侧边栏 / H5 底部 Tab |
| Token 存储键 | `stars-web-token` | `localStorage` |

当前无 `.env` 文件；如需切换 API 地址，可修改 `vite.config.ts` 的 `server.proxy`，或在 Nginx 层统一代理。

## 页面

| 路由 | 页面 | 说明 |
|------|------|------|
| `/login` | 登录 | RuoYi 账号登录，含验证码 |
| `/repos` | 仓库列表 | 搜索、分类/标签筛选、卡片列表 |
| `/repos/:id` | 仓库详情 | 编辑概述/分类/标签/备注，重新生成 |
| `/import` | 导入中心 | PAT 绑定、同步、任务进度 |
| `/tags` | 标签管理 | PC 完整 CRUD，H5 简化创建/删除 |

## API 前缀

所有 Stars 接口经 axios 访问 `/dev-api/stars/**`（开发由 Vite 代理，生产由 Nginx 代理）。主要域：

- `/stars/github/**` — PAT 绑定
- `/stars/import/**` — 导入任务
- `/stars/repos/**` — 仓库列表与详情
- `/stars/tags/**` — 标签 CRUD

## 默认账号

开发环境可使用 RuoYi 默认账号：

- 用户名：`admin`
- 密码：`admin123`

## 认证

登录成功后 Token 存储在 `localStorage`（键名 `stars-web-token`）。

Axios 请求自动附加：

- `Authorization: Bearer {token}`
- `clientid: {CLIENT_ID}`

## 相关文档

- 产品需求：[docs/prd/github-stars-knowledge-base.md](../docs/prd/github-stars-knowledge-base.md)
- OpenSpec 变更：[openspec/changes/stars-library/](../openspec/changes/stars-library/)
- 后端模块：`ruoyi-modules/stars-library`
