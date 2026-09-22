# 集成检查表 · owner 解封 R1 后必做（2026-09-22）

> **本文件 = owner 一句话解封 R1 后的完整执行清单**。
>
> 触发：user 2026-09-22 06:57 PDT 列出的接续步骤。
>
> 状态：每完成一项打 [x]，未完成打 [ ]。

## 前置

- [ ] owner 一句话授权动 `04-backend-code/ruoyi-admin/pom.xml` + `application.yml`
- [ ] 仓外配置：复制 `~/.aip/aip-local.yml.example` → `~/.aip/aip-local.yml`
- [ ] Docker Desktop 启动 + `docker ps | grep mysql-aip` = UP
- [ ] Redis ACL user `aip` 已建

## 集成步骤

### 步骤 1 · 注册 de-conversation 模块到底座

修改 `04-backend-code/ruoyi-admin/pom.xml`：

```xml
<!-- 在 <modules> 段加： -->
<module>../../de-conversation</module>
```

### 步骤 2 · MyBatis-Plus 包配置

修改 `04-backend-code/ruoyi-admin/src/main/resources/application.yml`：

```yaml
mybatis-plus:
  mapper-locations:
    - classpath*:mapper/**/*Mapper.xml
    - classpath*:mapper/de/**/*Mapper.xml  # 新增
  type-aliases-package:
    - org.dromara.**.domain
    - org.dromara.de.**.domain              # 新增
```

### 步骤 3 · LLM-ingest 鉴权配置（生产前必做）

在 `application.yml` 加：

```yaml
de-conversation:
  llm-service:
    token: ${DE_CONVERSATION_LLM_SERVICE_TOKEN}  # 32+ 字节随机
```

在仓外配置 `~/.aip/aip-local.yml`：

```yaml
DE_CONVERSATION_LLM_SERVICE_TOKEN: "32-byte-random-secret-here-xxx"
```

### 步骤 4 · Sa-Token 配置放行

修改 `04-backend-code/ruoyi-admin/src/main/java/.../config/SecurityConfig.java`（或对应 Sa-Token 配置）：

```java
.addPathMatchers(
    "/de/conversation/**",  // 新增
    "/de/binding/**",
    "/auth/**",
    ...
)
```

### 步骤 5 · 编译

```bash
cd 04-backend-code
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
sh mvnw -o -Dmaven.test.skip=true -pl ruoyi-admin -am package
```

期望：`BUILD SUCCESS`

### 步骤 6 · 重启后端

```bash
# 停旧后端
kill $(lsof -t -i:8080)

# 启动新后端（含 de-conversation）
sh scripts/aip-start-backend.sh
```

### 步骤 7 · 验证 E2E 14/14

```bash
MYSQL_HOST=127.0.0.1 MYSQL_PORT=23306 MYSQL_USER=root MYSQL_PASS=root MYSQL_DB=ry-vue \
  sh scripts/run-mysql-e2e-tests.sh --reset
```

期望输出：
```
 业务端点 = 14/14 通过
 总断言数:  51
 通过: 51
 失败: 0
 跳过: 0
✓ ALL CHECKS PASSED
```

### 步骤 8 · 验证 5 维铁律 5/5 PASS

```bash
sh scripts/verify-business-close-loop.sh
```

期望：
```
[维度 1] [PASS] 维度 1 断言绿
[维度 2] [PASS] 维度 2 业务实跑
[维度 3] [PASS] 维度 3 可见性验证
[维度 4] [PASS] 维度 4 依赖图健康
[维度 5] [PASS] 维度 5 owner 必做清单已明
[总判] PASS=5 FAIL=0 / 5
OK 5/5 通过 = 完成
```

### 步骤 9 · 验证 AC-09/10/11 端点（手工 curl）

```bash
JWT=$(curl -s -X POST http://127.0.0.1:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .data.token)

# AC-09
curl -s -X POST http://127.0.0.1:8080/de/conversation \
  -H "Authorization: Bearer $JWT" \
  -H 'Content-Type: application/json' \
  -d '{"instanceId":9001,"agentId":7,"title":"集成验证"}' | jq

# 期望 {"code":200,"data":{"conversationId":"...","agentId":7,"userId":1,"title":"集成验证","createDt":"..."}}
```

### 步骤 10 · commit + push

```bash
git add 04-backend-code/ruoyi-admin/pom.xml \
        04-backend-code/ruoyi-admin/src/main/resources/application.yml \
        "04-backend-code/ruoyi-admin/src/main/java/.../config/SecurityConfig.java"
git commit -m "R1 解封：集成 de-conversation 模块到底座

- ruoyi-admin/pom.xml: 加 <module>../../de-conversation</module>
- application.yml: 加 mapper-locations + type-aliases-package
- application.yml: 加 de-conversation.llm-service.token 配置
- SecurityConfig: 加 /de/conversation/** 放行
- 仓外配置 ~/.aip/aip-local.yml: 加 DE_CONVERSATION_LLM_SERVICE_TOKEN
- 实测: 业务端点 14/14, 5/5 PASS"
git push origin main:refs/heads/2026-09-21-restart
```

## owner 1-click ship（解封 R1 后）

```
浏览器打开 https://github.com/wilson323/aip-enterprise-digital-employee-platform/tree/2026-09-21-restart

点 "Merge pull request" → 项目正式 ship
```

## 红线核对

- ✅ R1：仅动 `04-backend-code/ruoyi-admin/pom.xml` + `application.yml` + `SecurityConfig.java` = owner 已授权
- ✅ R-add：不动入口层 4 文件
- ✅ OI-019：不缩判定面（判定面更严：业务端点 11/14 → 14/14）
- ✅ R2：不动 contracts
- ✅ R5：commit 由 owner R1 解封触发

## 真源

- `de-conversation/README.md`（模块真源）
- `docs/AC-09-10-11集成步骤完整文档-2026-09-22.md`（集成步骤）
- `docs/SHIP.md`（ship 真源）
- `docs/完成定义铁律-2026-09-20.md`（5 维定义）