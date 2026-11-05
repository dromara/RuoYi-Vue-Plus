# Maven 模块结构

## 需要修改的 3 个 pom.xml

### 1. ruoyi-modules/pom.xml（追加 module 声明）

在 `<modules>` 中追加一行：

```xml
<modules>
    <module>ruoyi-demo</module>
    <module>ruoyi-gen</module>
    <module>ruoyi-job</module>
    <module>ruoyi-system</module>
    <module>ruoyi-workflow</module>
    <module>ruoyi-ai</module>
    <module>ruoyi-{module}</module>  <!-- 新增 -->
</modules>
```

### 2. 新建 ruoyi-modules/ruoyi-{module}/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>org.dromara</groupId>
        <artifactId>ruoyi-modules</artifactId>
        <version>${revision}</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>ruoyi-{module}</artifactId>

    <description>
        {module中文名}模块
    </description>

    <dependencies>
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

        <!-- 按需追加 -->
    </dependencies>

</project>
```

**关键点**：
- parent 是 `ruoyi-modules`，不是 `ruoyi-vue-plus`。
- 版本用 `${revision}`，不写死 `6.0.0`。
- 依赖**不写 `<version>`**，由根 pom `dependencyManagement` 统一管理。
- 不需要 `<packaging>`，默认 jar。

### 3. ruoyi-admin/pom.xml（追加依赖）

在 `<dependencies>` 中追加：

```xml
<!-- {module中文名}模块 -->
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>ruoyi-{module}</artifactId>
</dependency>
```

如果模块希望像 `ruoyi-gen` 一样通过 profile 控制是否加载，改用 profile 方式：

```xml
<profiles>
    <profile>
        <id>{module}</id>
        <dependencies>
            <dependency>
                <groupId>org.dromara</groupId>
                <artifactId>ruoyi-{module}</artifactId>
            </dependency>
        </dependencies>
        <activation>
            <activeByDefault>true</activeByDefault>  <!-- 默认激活 -->
        </activation>
    </profile>
</profiles>
```

## 依赖选择指南

按功能需求选择 common 子模块：

| 需求 | 依赖 artifactId | 说明 |
|------|-----------------|------|
| 基础工具/常量/异常 | `ruoyi-common-core` | 必选 |
| 跨模块接口 | `ruoyi-api` | 如需对外暴露或调用其他模块 |
| 数据库 CRUD | `ruoyi-common-mybatis` | BaseMapperPlus/QueryBuilder/数据权限 |
| Web/Controller | `ruoyi-common-web` | BaseController/@RepeatSubmit/@RateLimiter |
| Sa-Token 权限 | `ruoyi-common-security` | @SaCheckPermission/路由拦截 |
| 操作日志 | `ruoyi-common-log` | @Log 注解 |
| Excel 导入导出 | `ruoyi-common-excel` | @ExcelProperty/ExcelListener |
| 缓存 | `ruoyi-common-redis` | RedisUtils/CacheUtils/@Cacheable |
| 翻译 | `ruoyi-common-translation` | @Translation/TranslationInterface |
| 脱敏 | `ruoyi-common-sensitive` | @Sensitive |
| 字段加解密 | `ruoyi-common-encrypt` | @EncryptField/@ApiEncrypt |
| OSS 文件存储 | `ruoyi-common-oss` | ISysOssService |
| 消息推送 | `ruoyi-common-push` | WebSocket/SSE |
| 接口文档 | `ruoyi-common-doc` | SpringDoc/Swagger |
| 短信 | `ruoyi-common-sms` | sms4j |
| 邮件 | `ruoyi-common-mail` | Jakarta Mail |
| MQTT | `ruoyi-common-mqtt` | mica-mqtt |
| MCP | `ruoyi-common-mcp` | @McpTool/@McpResource |
| ES 搜索 | `ruoyi-common-elasticsearch` | Easy-Es |

**参考现有模块**：
- 精简模块（只有 CRUD）：参考 [ruoyi-gen/pom.xml](file:///workspace/ruoyi-modules/ruoyi-gen/pom.xml)（5 个依赖）
- 标准业务模块：参考 [ruoyi-system/pom.xml](file:///workspace/ruoyi-modules/ruoyi-system/pom.xml)（12 个依赖）
- 全功能演示：参考 [ruoyi-demo/pom.xml](file:///workspace/ruoyi-modules/ruoyi-demo/pom.xml)（17 个依赖）

## 包结构

```
ruoyi-modules/ruoyi-{module}/
└── src/main/
    ├── java/org/dromara/{module}/
    │   ├── controller/              # Controller
    │   │   └── {Module}{Business}Controller.java
    │   ├── domain/                  # Entity
    │   │   ├── {Module}{Business}.java
    │   │   ├── bo/                  # Business Object（请求/查询）
    │   │   │   └── {Module}{Business}Bo.java
    │   │   └── vo/                  # View Object（响应/导出）
    │   │       └── {Module}{Business}Vo.java
    │   ├── mapper/                  # Mapper 接口
    │   │   └── {Module}{Business}Mapper.java
    │   ├── service/                 # Service 接口
    │   │   ├── I{Module}{Business}Service.java
    │   │   └── impl/                # Service 实现
    │   │       └── {Module}{Business}ServiceImpl.java
    │   └── (可选)
    │       ├── event/               # Spring 事件
    │       ├── listener/            # 事件监听器
    │       ├── handler/             # 处理器
    │       ├── runner/              # 启动钩子
    │       └── config/              # 模块内配置类
    └── resources/
        └── mapper/{module}/         # Mapper XML
            └── {Module}{Business}Mapper.xml
```

**命名约定**：
- Entity/BO/VO/Mapper/Service/Controller 类名统一以模块前缀开头，如 `CrmCustomer`、`CrmCustomerBo`、`CrmCustomerVo`。
- 不要省略模块前缀，避免与 system 模块的同名类冲突。
- Service 接口以 `I` 开头：`ICrmCustomerService`。

## 无需修改的配置

以下配置已在 [application.yml](file:///workspace/ruoyi-admin/src/main/resources/application.yml) 中通过通配符覆盖，新模块**不需要额外配置**：

```yaml
mybatis-plus:
  mapperPackage: org.dromara.**.mapper        # 自动扫描
  mapperLocations: classpath*:mapper/**/*Mapper.xml  # 自动加载
  typeAliasesPackage: org.dromara.**.domain    # 自动扫描
```

- `@SpringBootApplication` 扫描 `org.dromara` 基包 → `@RestController`、`@Service`、`@Component` 自动注册。
- Mapper 接口自动扫描 → 无需 `@MapperScan`。
- Mapper XML 自动加载 → 无需在 yml 追加路径。
- 实体类别名自动注册 → XML 中可直接用类名。

## 编译验证

```bash
# 只编译新模块及其依赖
./mvnw clean compile -pl ruoyi-modules/ruoyi-{module} -am -q

# 如果修改了 ruoyi-api，需要全量编译
./mvnw clean compile -q
```

编译通过后，启动 [DromaraApplication](file:///workspace/ruoyi-admin/src/main/java/org/dromara/DromaraApplication.java)，访问新模块的 `/list` 接口应返回空数据（无权限时返回 403 是正常的）。
