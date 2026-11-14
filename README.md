# RuoYi-Vue-Plus 本地启动指南

本仓库包含后端和 Vue 前端。日常开发只需要运行主后端和前端，不需要把每个 Java 模块或扩展服务都单独启动。

## 项目结构与服务

| 位置 / 启动类 | 用途 | 本地开发是否必需 |
| --- | --- | --- |
| `ruoyi-admin` / `org.dromara.DromaraApplication` | 主后端，提供登录、系统管理、工作流等业务接口；默认端口 `8080` | 是 |
| `plus-ui/` | Vue 3 + TypeScript + Element Plus 前端；开发服务器默认端口 `80` | 需要页面时启动 |
| `ruoyi-extend/ruoyi-monitor-admin` / `MonitorAdminApplication` | Spring Boot Admin 应用监控；默认端口 `9090` | 否 |
| `ruoyi-extend/ruoyi-snailjob-server` / `SnailJobServerApplication` | SnailJob 任务调度服务端；控制台默认端口 `8800` | 使用任务调度时启动 |
| `ruoyi-extend/ruoyi-snailai-server` / `SnailAiServerApplication` | SnailAI 服务端；默认端口 `8900` | 使用 SnailAI 时启动 |

`ruoyi-common`、`ruoyi-api`、`ruoyi-modules` 是后端依赖模块，不需要分别运行；数据库实体类也不是启动入口。PostgreSQL 和 Redis 是外部服务，需要在运行后端前自行启动。

## 环境准备

- JDK 21、Maven、PostgreSQL、Redis。
- Node.js 满足 `plus-ui/package.json` 的 `engines.node` 要求（目前为 `^22.12.0 || ^24.0.0 || >=26.0.0`），pnpm 10 或以上。
- 默认 Maven profile 为 `dev`，对应 `ruoyi-admin/src/main/resources/application-dev.yml`。正式环境使用 `application-prod.yml`，目前仍配置 MySQL；以下步骤针对开发环境。

## 数据库与 Redis 配置

1. 在 PostgreSQL 创建数据库；把 `ruoyi-admin/src/main/resources/application-dev.yml` 中 `spring.datasource.dynamic.datasource.master` 的 `url`、`username`、`password` 改为实际连接信息。当前开发配置使用 PostgreSQL；数据库名、账号和密码以你本机为准。
2. 向该数据库导入 `script/sql/postgres/postgres_ry_vue.sql`；使用工作流时再导入 `script/sql/postgres/postgres_ry_workflow.sql`。执行脚本前确认目标库，避免导入到其他数据库。
3. 在同一份 `application-dev.yml` 中配置 `spring.data.redis` 的地址和端口，并启动 Redis；本地 Redis 无密码时，不配置 `password` 项。

可在项目根目录使用以下命令导入（将占位符换成实际值）：

```bash
psql -h <数据库地址> -p 5432 -U <数据库用户> -d <数据库名> -f script/sql/postgres/postgres_ry_vue.sql
psql -h <数据库地址> -p 5432 -U <数据库用户> -d <数据库名> -f script/sql/postgres/postgres_ry_workflow.sql
```

不要把实际生产密码写入仓库。`application.yml` 中配置后端端口及公共选项，`application-dev.yml` 配置开发环境数据库、Redis 和扩展服务开关。

## 启动后端

在 IDEA 中从项目根目录导入 Maven 项目，选择 JDK 21，运行 `ruoyi-admin/src/main/java/org/dromara/DromaraApplication.java` 的 `main` 方法即可，不用启动其他业务模块。

也可以在项目根目录打包并运行：

```bash
mvn -pl ruoyi-admin -am -DskipTests package
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

后端默认地址：`http://localhost:8080`。如果 IDEA 提示项目内的 `ruoyi-common-bom` 无法解析，先重新加载 Maven 项目；必要时运行 `mvn -f ruoyi-common/ruoyi-common-bom/pom.xml install` 后再刷新。

## 启动前端

前端源码已放在本仓库的 [`plus-ui/`](plus-ui/) 目录中，无需另行 clone：

```bash
cd plus-ui
pnpm install
pnpm dev
```

开发环境配置在 `plus-ui/.env.development`；`VITE_APP_PORT` 默认是 `80`，页面地址为 `http://localhost:80`。如端口不可用，可修改该变量。前端通过 `plus-ui/vite.config.ts` 把 `/dev-api` 请求代理到 `http://localhost:8080`，修改后端端口时也要同步修改代理目标。前端的构建和其他脚本见 `plus-ui/package.json`，详细说明见 [`plus-ui/README.md`](plus-ui/README.md)。

## 按需启动扩展服务

- **监控中心**：运行 `MonitorAdminApplication`，访问 `http://localhost:9090/admin`；主后端需要在 `application-dev.yml` 中启用 `spring.boot.admin.client.enabled` 才会上报到监控中心。
- **SnailJob**：运行 `SnailJobServerApplication`，访问 `http://localhost:8800/snail-job`；主后端需要启用 `snail-job.enabled`。调度服务的数据库连接单独配置在 `ruoyi-extend/ruoyi-snailjob-server/src/main/resources/application-dev.yml`，相关 PostgreSQL 脚本为 `script/sql/postgres/postgres_ry_job.sql`。
- **SnailAI**：运行 `SnailAiServerApplication`，访问 `http://localhost:8900/snail-ai`；主后端需要启用 `snail-ai.enabled`。AI 服务的数据库连接单独配置在 `ruoyi-extend/ruoyi-snailai-server/src/main/resources/application-dev.yml`，相关 PostgreSQL 脚本为 `script/sql/postgres/postgres_ry_ai.sql`。

主后端开发配置中，上述三个客户端开关目前均为 `false`。SnailJob 和 SnailAI 服务端的开发配置目前仍使用 **MySQL**；如果要让它们连接 PostgreSQL，需分别调整各自的数据源、驱动依赖并准备对应数据库，不能只修改主后端的数据源。
