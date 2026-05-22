# Stars Library Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Note:** `/write-plan` 命令已弃用；本计划由 **writing-plans** skill 生成，依据 `openspec/changes/stars-library/` 下 proposal / design / specs / tasks。

**Goal:** 在 RuoYi-Vue-Plus 中交付 GitHub Stars 知识库：后端 `stars-library` 模块（导入、DeepSeek 概述/分类/标签）+ 独立前端 `stars-web`（Vue3 + TS + Element Plus + Vite，PC/H5）。

**Architecture:** 后端挂载 `ruoyi-admin`，MySQL 存全局 repo 缓存与用户关系；GitHub API 异步导入；Kafka 队列触发 DeepSeek enrichment；前端独立 Vite 项目通过 Sa-Token JWT 调用 `/stars/**` REST API。

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, Kafka, Spring AI DeepSeek, Vue 3, TypeScript, Element Plus, Vite, Pinia

**Source of truth:**
- PRD: `docs/prd/github-stars-knowledge-base.md`
- OpenSpec: `openspec/changes/stars-library/`

---

## File Structure Map

### Backend — create

```
ruoyi-modules/stars-library/
├── pom.xml
└── src/main/
    ├── java/com/wudgaby/stars/
    │   ├── config/
    │   │   ├── StarsProperties.java          # stars.* 配置绑定
    │   │   ├── StarsAiConfig.java            # DeepSeek ChatClient
    │   │   └── StarsKafkaConfig.java         # Kafka 工厂
    │   ├── controller/
    │   │   ├── StarsGithubController.java    # PAT 绑定
    │   │   ├── StarsImportController.java    # 导入任务
    │   │   ├── StarsRepoController.java      # 列表/详情/更新/重生成
    │   │   └── StarsTagController.java       # 标签 CRUD
    │   ├── domain/
    │   │   ├── StarsGithubAccount.java
    │   │   ├── StarsRepo.java
    │   │   ├── StarsUserRepo.java
    │   │   ├── StarsTag.java
    │   │   ├── StarsUserRepoTag.java
    │   │   ├── StarsImportJob.java
    │   │   ├── bo/ / vo/ / enums/
    │   │   └── ai/RepoEnrichmentResult.java
    │   ├── mapper/                           # MyBatis Mapper 接口
    │   ├── github/
    │   │   ├── GitHubApiClient.java
    │   │   └── GitHubStarredPage.java
    │   ├── ai/
    │   │   ├── RepoEnrichmentClient.java
    │   │   └── RepoEnrichmentPromptFactory.java
    │   ├── messaging/
    │   │   ├── EnrichmentCommand.java
    │   │   ├── EnrichmentProducer.java
    │   │   └── EnrichmentConsumer.java
    │   ├── service/
    │   │   ├── IStarsGithubService.java
    │   │   ├── IStarsImportService.java
    │   │   ├── IStarsRepoService.java
    │   │   ├── IStarsTagService.java
    │   │   ├── IStarsEnrichmentService.java
    │   │   └── impl/*
    │   └── observe/StarsMetrics.java
    └── resources/
        └── mapper/stars/*.xml

script/sql/stars_library.sql                  # 6 张表 + 菜单权限 seed
```

### Backend — modify

```
ruoyi-modules/pom.xml                         # 添加 module
ruoyi-admin/pom.xml                           # 添加 stars-library 依赖
ruoyi-admin/src/main/resources/application-dev.yml  # stars.* / spring.ai.deepseek
pom.xml (root)                                # dependencyManagement 如有需要
```

### Frontend — create

```
stars-web/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── index.html
├── .env.development
└── src/
    ├── main.ts
    ├── App.vue
    ├── router/index.ts
    ├── stores/auth.ts
    ├── api/http.ts                           # axios + Sa-Token header
    ├── api/stars.ts                          # typed /stars/** client
    ├── composables/useBreakpoint.ts          # 768px PC/H5
    ├── layouts/
    │   ├── PcLayout.vue                      # 侧边栏
    │   └── MobileLayout.vue                  # 底部 Tab
    └── views/
        ├── LoginView.vue
        ├── RepoListView.vue
        ├── RepoDetailView.vue
        ├── ImportCenterView.vue
        └── TagManageView.vue
```

---

## Phase A: Backend Module Skeleton

### Task 1: Maven module scaffold

**Files:**
- Create: `ruoyi-modules/stars-library/pom.xml`
- Modify: `ruoyi-modules/pom.xml`
- Modify: `ruoyi-admin/pom.xml`

- [ ] **Step 1: Create `stars-library/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.dromara</groupId>
        <artifactId>ruoyi-modules</artifactId>
        <version>${revision}</version>
    </parent>
    <groupId>com.wudgaby.stars</groupId>
    <artifactId>stars-library</artifactId>
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
    </properties>
    <dependencies>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-core</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-mybatis</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-web</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-security</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-redis</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-log</artifactId></dependency>
        <dependency><groupId>org.dromara</groupId><artifactId>ruoyi-common-encrypt</artifactId></dependency>
        <dependency><groupId>org.springframework.kafka</groupId><artifactId>spring-kafka</artifactId></dependency>
        <dependency><groupId>org.springframework.ai</groupId><artifactId>spring-ai-starter-model-deepseek</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
        <dependency><groupId>io.micrometer</groupId><artifactId>micrometer-registry-prometheus</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Register module in `ruoyi-modules/pom.xml`**

在 `<modules>` 内添加：

```xml
<module>stars-library</module>
```

- [ ] **Step 3: Add dependency in `ruoyi-admin/pom.xml`**

```xml
<dependency>
    <groupId>com.wudgaby.stars</groupId>
    <artifactId>stars-library</artifactId>
</dependency>
```

- [ ] **Step 4: Verify compile**

Run: `mvn -pl ruoyi-modules/stars-library,ruoyi-admin -am compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add ruoyi-modules/stars-library/pom.xml ruoyi-modules/pom.xml ruoyi-admin/pom.xml
git commit -m "feat(stars): scaffold stars-library maven module"
```

---

### Task 2: Database schema

**Files:**
- Create: `script/sql/stars_library.sql`

- [ ] **Step 1: Write SQL migration**

```sql
-- stars_library.sql
CREATE TABLE stars_github_account (
    id            BIGINT       NOT NULL PRIMARY KEY COMMENT '主键',
    user_id       BIGINT       NOT NULL COMMENT 'RuoYi 用户 ID',
    github_login  VARCHAR(100) NULL COMMENT 'GitHub 用户名',
    access_token  VARCHAR(500) NOT NULL COMMENT 'AES 加密 PAT',
    token_scope   VARCHAR(200) NULL,
    bind_time     DATETIME     NULL,
    update_time   DATETIME     NULL,
    UNIQUE KEY uk_user_id (user_id)
) COMMENT 'GitHub 账号绑定';

CREATE TABLE stars_repo (
    id                BIGINT       NOT NULL PRIMARY KEY,
    full_name         VARCHAR(200) NOT NULL COMMENT 'owner/repo',
    owner             VARCHAR(100) NOT NULL,
    repo_name         VARCHAR(100) NOT NULL,
    description       TEXT         NULL,
    language          VARCHAR(50)  NULL,
    stargazers_count  INT          DEFAULT 0,
    html_url          VARCHAR(500) NULL,
    readme_snippet    TEXT         NULL,
    readme_cached_at  DATETIME     NULL,
    github_updated_at DATETIME     NULL,
    create_time       DATETIME     NULL,
    update_time       DATETIME     NULL,
    UNIQUE KEY uk_full_name (full_name)
) COMMENT '仓库全局缓存';

CREATE TABLE stars_user_repo (
    id                    BIGINT       NOT NULL PRIMARY KEY,
    user_id               BIGINT       NOT NULL,
    repo_id               BIGINT       NOT NULL,
    import_source         VARCHAR(50)  NOT NULL COMMENT 'self | github_username',
    note                  VARCHAR(500) NULL,
    category              VARCHAR(50)  NULL,
    classification_source VARCHAR(20)  NULL COMMENT 'ai|manual',
    summary_one_liner     VARCHAR(100) NULL,
    summary_text          VARCHAR(500) NULL,
    summary_status        VARCHAR(20)  DEFAULT 'pending',
    summary_source        VARCHAR(20)  NULL COMMENT 'ai|manual',
    import_time           DATETIME     NULL,
    update_time           DATETIME     NULL,
    UNIQUE KEY uk_user_repo (user_id, repo_id),
    KEY idx_user_category (user_id, category),
    KEY idx_user_status (user_id, summary_status)
) COMMENT '用户-仓库关系';

CREATE TABLE stars_tag (
    id          BIGINT      NOT NULL PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    name        VARCHAR(50) NOT NULL,
    color       VARCHAR(20) NULL,
    create_time DATETIME    NULL,
    UNIQUE KEY uk_user_tag (user_id, name)
) COMMENT '用户标签';

CREATE TABLE stars_user_repo_tag (
    user_repo_id BIGINT NOT NULL,
    tag_id       BIGINT NOT NULL,
    PRIMARY KEY (user_repo_id, tag_id)
) COMMENT '用户仓库标签关联';

CREATE TABLE stars_import_job (
    id              BIGINT       NOT NULL PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    job_type        VARCHAR(20)  NOT NULL COMMENT 'self_sync|import_user',
    source_login    VARCHAR(100) NULL,
    status          VARCHAR(20)  NOT NULL COMMENT 'pending|running|done|failed|partial',
    total_count     INT          DEFAULT 0,
    processed_count INT          DEFAULT 0,
    failed_count    INT          DEFAULT 0,
    error_message   TEXT         NULL,
    start_time      DATETIME     NULL,
    end_time        DATETIME     NULL,
    KEY idx_user_job (user_id, status)
) COMMENT '导入任务';
```

- [ ] **Step 2: Apply SQL locally**

Run against dev MySQL (adjust connection):

```bash
mysql -u root -p ry-vue < script/sql/stars_library.sql
```

Expected: 6 tables created

- [ ] **Step 3: Commit**

```bash
git add script/sql/stars_library.sql
git commit -m "feat(stars): add database schema for stars library"
```

---

### Task 3: Domain entities & mappers

**Files:**
- Create: `ruoyi-modules/stars-library/src/main/java/com/wudgaby/stars/domain/*.java`
- Create: `ruoyi-modules/stars-library/src/main/java/com/wudgaby/stars/mapper/*.java`

- [ ] **Step 1: Create enums**

`SummaryStatus`: `PENDING`, `PROCESSING`, `DONE`, `FAILED`
`ImportJobStatus`: `PENDING`, `RUNNING`, `DONE`, `FAILED`, `PARTIAL`
`ImportJobType`: `SELF_SYNC`, `IMPORT_USER`

- [ ] **Step 2: Create entity `StarsUserRepo`**

```java
@Data
@TableName("stars_user_repo")
public class StarsUserRepo {
    @TableId private Long id;
    private Long userId;
    private Long repoId;
    private String importSource;
    private String note;
    private String category;
    private String classificationSource;
    private String summaryOneLiner;
    private String summaryText;
    private String summaryStatus;
    private String summarySource;
    private LocalDateTime importTime;
    private LocalDateTime updateTime;
}
```

按同样模式创建 `StarsRepo`, `StarsGithubAccount`, `StarsTag`, `StarsImportJob`。

- [ ] **Step 3: Create Mapper interfaces**

```java
public interface StarsUserRepoMapper extends BaseMapperPlus<StarsUserRepo, StarsUserRepoVo> {}
```

- [ ] **Step 4: Commit**

```bash
git commit -m "feat(stars): add domain entities and mappers"
```

---

### Task 4: Application configuration

**Files:**
- Create: `com/wudgaby/stars/config/StarsProperties.java`
- Modify: `ruoyi-admin/src/main/resources/application-dev.yml`

- [ ] **Step 1: Create `StarsProperties`**

```java
@ConfigurationProperties(prefix = "stars")
public record StarsProperties(
    Github github,
    Summary summary,
    DeepLink deepLink
) {
    public record Github(String apiBase, int pageSize, int requestIntervalMs) {}
    public record Summary(Kafka kafka, int maxConcurrentPerUser, int readmeMaxChars, int retryMax) {
        public record Kafka(String requestTopic, String consumerGroup) {}
    }
    public record DeepLink(String zreadTemplate, String deepwikiTemplate) {}
}
```

- [ ] **Step 2: Add yml block**

```yaml
spring:
  ai:
    deepseek:
      api-key: ${DEEPSEEK_API_KEY}
      chat:
        options:
          model: deepseek-chat

stars:
  github:
    api-base: https://api.github.com
    page-size: 100
    request-interval-ms: 100
  summary:
    kafka:
      request-topic: stars.enrichment.request
      consumer-group: stars-enrichment-consumer
    max-concurrent-per-user: 3
    readme-max-chars: 3000
    retry-max: 3
  deep-link:
    zread-template: "https://zread.ai/{owner}/{repo}"
    deepwiki-template: "https://deepwiki.com/{owner}/{repo}"
```

- [ ] **Step 3: Enable `@EnableConfigurationProperties(StarsProperties.class)`**

- [ ] **Step 4: Commit**

---

## Phase B: GitHub Import

### Task 5: GitHub API client (TDD)

**Files:**
- Create: `github/GitHubApiClient.java`
- Create: `github/dto/GitHubStarredRepo.java`
- Test: `src/test/java/com/wudgaby/stars/github/GitHubApiClientTest.java`

- [ ] **Step 1: Write failing test for pagination parsing**

```java
@Test
void fetchStarredPage_parsesItemsAndNextLink() {
    var client = new GitHubApiClient(WebClient.builder(), props);
    // use MockWebServer: GET /user/starred?per_page=100&page=1
    // assert page.items().size() == 1
    // assert page.hasNext() == true
}
```

- [ ] **Step 2: Run test — expect FAIL**

Run: `mvn -pl ruoyi-modules/stars-library test -Dtest=GitHubApiClientTest`
Expected: compilation failure or test failure

- [ ] **Step 3: Implement minimal client**

```java
public GitHubStarredPage fetchUserStarred(String token, String username, int page) {
    String path = username == null ? "/user/starred" : "/users/" + username + "/starred";
    // WebClient + Authorization: Bearer {token}
    // parse Link header for next page
    // on 403/429 throw GitHubRateLimitException(retryAfterSeconds)
}
```

- [ ] **Step 4: Implement README fetch with Base64 decode + truncate**

```java
public String fetchReadmeSnippet(String owner, String repo, int maxChars) {
    // GET /repos/{owner}/{repo}/readme
    // Base64 decode content, truncate to maxChars
}
```

- [ ] **Step 5: Run test — expect PASS**

- [ ] **Step 6: Commit**

---

### Task 6: GitHub PAT bind/unbind

**Files:**
- Create: `controller/StarsGithubController.java`
- Create: `service/impl/StarsGithubServiceImpl.java`

- [ ] **Step 1: Implement bind — validate token via `GET /user`**

```java
@PostMapping("/stars/github/bind")
@SaCheckPermission("stars:github:bind")
public R<Void> bind(@RequestBody @Valid BindGithubRequest req) {
    githubService.bind(getUserId(), req.token());
    return R.ok();
}
```

使用 `ruoyi-common-encrypt` AES 加密存储 PAT。

- [ ] **Step 2: Implement status & unbind endpoints**

`GET /stars/github/status` → `{ bound: true, login: "xxx" }`
`DELETE /stars/github/unbind`

- [ ] **Step 3: Manual test with curl**

```bash
curl -X POST http://localhost:8080/stars/github/bind \
  -H "Authorization: Bearer {sa-token}" \
  -H "Content-Type: application/json" \
  -d '{"token":"ghp_xxx"}'
```

- [ ] **Step 4: Commit**

---

### Task 7: Import job service

**Files:**
- Create: `service/impl/StarsImportServiceImpl.java`
- Create: `controller/StarsImportController.java`

- [ ] **Step 1: `POST /stars/import/self` — create job, run async**

```java
@Async
public void runSelfSync(Long userId, Long jobId) {
    String token = githubService.decryptToken(userId);
    int page = 1;
    while (true) {
        var starred = githubClient.fetchUserStarred(token, null, page);
        for (var item : starred.items()) {
            upsertRepo(userId, item, "self");
            jobMapper.incrementProcessed(jobId);
        }
        if (!starred.hasNext()) break;
        page++;
        Thread.sleep(props.github().requestIntervalMs());
    }
    jobMapper.markDone(jobId);
}
```

- [ ] **Step 2: `upsertRepo` — dedupe + preserve manual fields**

```java
private void upsertRepo(Long userId, GitHubStarredRepo gh, String source) {
    StarsRepo repo = repoService.upsertFromGitHub(gh);
    StarsUserRepo existing = userRepoMapper.selectByUserAndRepo(userId, repo.getId());
    if (existing == null) {
        StarsUserRepo ur = new StarsUserRepo();
        ur.setUserId(userId);
        ur.setRepoId(repo.getId());
        ur.setImportSource(source);
        ur.setSummaryStatus(SummaryStatus.PENDING.name());
        userRepoMapper.insert(ur);
        enrichmentProducer.enqueue(new EnrichmentCommand(userId, ur.getId(), repo.getId()));
    } else {
        // update import_time only, do NOT touch note/category/tags/summary if manual
    }
}
```

- [ ] **Step 3: `POST /stars/import/user/{login}`**

同上，source = login；无 PAT 时用服务端 fallback token（配置项 `stars.github.fallback-token`，可选）。

- [ ] **Step 4: Progress APIs**

`GET /stars/import/jobs` — 分页
`GET /stars/import/jobs/{id}` — `{ status, totalCount, processedCount, failedCount }`

- [ ] **Step 5: Rate limit pause/resume**

捕获 `GitHubRateLimitException` → 更新 job status `running` + `errorMessage` → `@Scheduled` 或延迟队列续跑。

- [ ] **Step 6: Commit**

---

## Phase C: DeepSeek AI Enrichment

### Task 8: Enrichment result model & prompt

**Files:**
- Create: `domain/ai/RepoEnrichmentResult.java`
- Create: `ai/RepoEnrichmentPromptFactory.java`
- Create: `ai/RepoEnrichmentClient.java`

- [ ] **Step 1: Define structured result**

```java
public record RepoEnrichmentResult(
    String oneLiner,
    String summary,
    String category,
    List<String> tags
) {}
```

- [ ] **Step 2: Configure DeepSeek ChatClient**

参考 `ai-structured` 的 `AiClientConfig.deepseekChatClient`：

```java
@Bean
ChatClient starsEnrichmentChatClient(DeepSeekChatModel model) {
    return ChatClient.builder(model).build();
}
```

- [ ] **Step 3: Implement prompt factory**

```java
public String build(StarsRepo repo, String readmeSnippet) {
    return """
        你是技术项目分析助手。根据 GitHub 仓库信息输出 JSON：
        {"one_liner":"...","summary":"...","category":"...","tags":["..."]}
        category 优先从 taxonomy 选择：AI/RAG,后端框架,前端组件,DevOps,数据库,工具库,学习参考,待评估
        tags 最多5个中文标签。
        仓库：%s
        Description：%s
        Language：%s
        README：
        %s
        """.formatted(repo.getFullName(), repo.getDescription(), repo.getLanguage(), readmeSnippet);
}
```

- [ ] **Step 4: Implement client with `.entity(RepoEnrichmentResult.class)`**

- [ ] **Step 5: Unit test prompt + JSON parsing (mock ChatClient)**

- [ ] **Step 6: Commit**

---

### Task 9: Kafka enrichment pipeline

**Files:**
- Create: `messaging/EnrichmentCommand.java`
- Create: `messaging/EnrichmentProducer.java`
- Create: `messaging/EnrichmentConsumer.java`
- Create: `service/impl/StarsEnrichmentServiceImpl.java`

- [ ] **Step 1: Producer enqueue on new user_repo**

```java
public void enqueue(EnrichmentCommand cmd) {
    kafkaTemplate.send(props.summary().kafka().requestTopic(), cmd.userRepoId().toString(), cmd);
}
```

- [ ] **Step 2: Consumer — reference TicketAnalysisConsumer pattern**

```java
@KafkaListener(topics = "${stars.summary.kafka.request-topic}", groupId = "${stars.summary.kafka.consumer-group}")
public void onMessage(@Payload EnrichmentCommand cmd) {
    enrichmentService.process(cmd);
}
```

- [ ] **Step 3: Process — fetch README cache, call DeepSeek, apply results**

```java
@Transactional
public void process(EnrichmentCommand cmd) {
    userRepoMapper.updateStatus(cmd.userRepoId(), PROCESSING);
    StarsRepo repo = repoMapper.selectById(cmd.repoId());
    String readme = repoService.getOrFetchReadme(repo);
    RepoEnrichmentResult result = enrichmentClient.enrich(repo, readme);
    userRepoMapper.applyEnrichment(cmd.userRepoId(), result);
    tagService.applyAiTags(cmd.userId(), cmd.userRepoId(), result.tags());
}
```

- [ ] **Step 4: Per-user concurrency limit (3) via Redis semaphor or Bulkhead**

- [ ] **Step 5: Retry max 3 — on failure set `summary_status=failed`**

- [ ] **Step 6: `POST /stars/repos/{id}/regenerate-summary`**

- [ ] **Step 7: Commit**

---

### Task 10: Auto-apply category & tags

**Files:**
- Modify: `StarsEnrichmentServiceImpl.java`
- Create: `service/impl/StarsTagServiceImpl.java`

- [ ] **Step 1: `applyEnrichment` sets category + classification_source=ai**

- [ ] **Step 2: `applyAiTags` — find or create tags by name per user**

```java
public void applyAiTags(Long userId, Long userRepoId, List<String> tagNames) {
    List<String> limited = tagNames.stream().limit(5).toList();
    for (String name : limited) {
        StarsTag tag = tagMapper.selectByUserAndName(userId, name)
            .orElseGet(() -> tagMapper.insertTag(userId, name));
        userRepoTagMapper.insertIgnore(userRepoId, tag.getId());
    }
}
```

- [ ] **Step 3: Integration test (Testcontainers Kafka + H2/MySQL)**

Mock DeepSeek → assert category/tags persisted

- [ ] **Step 4: Commit**

---

## Phase D: Organization & Query APIs

### Task 11: Repo list & search

**Files:**
- Create: `controller/StarsRepoController.java`
- Create: `service/impl/StarsRepoServiceImpl.java`
- Create: `resources/mapper/stars/StarsUserRepoMapper.xml`

- [ ] **Step 1: `GET /stars/repos` with PageQuery**

Query params: `keyword`, `category`, `tagIds`, `importSource`, `summaryStatus`, `orderBy`

SQL JOIN `stars_user_repo` + `stars_repo` + optional tag filter

- [ ] **Step 2: Response VO includes deep links**

```java
public record StarsRepoCardVo(
    Long id, String fullName, String owner, String repoName,
    String language, Integer stargazersCount,
    String category, List<String> tags,
    String summaryOneLiner, String summaryStatus,
    String githubUrl, String zreadUrl, String deepwikiUrl
) {}
```

- [ ] **Step 3: `GET /stars/repos/{id}` detail**

- [ ] **Step 4: `PUT /stars/repos/{id}` — update note/summary/category/tags, set source=manual**

- [ ] **Step 5: `POST /stars/repos/batch-tags`**

- [ ] **Step 6: Commit**

---

### Task 12: Tag CRUD & permissions

**Files:**
- Create: `controller/StarsTagController.java`
- Modify: `script/sql/stars_library.sql` (menu/permission seed)

- [ ] **Step 1: Tag CRUD endpoints**

`GET/POST/PUT/DELETE /stars/tags`

- [ ] **Step 2: Add Sa-Token permissions**

`stars:repo:list`, `stars:repo:edit`, `stars:repo:import`, `stars:tag:edit`, `stars:github:bind`

- [ ] **Step 3: Insert sys_menu seed SQL (optional deep link to stars-web)**

- [ ] **Step 4: Commit**

---

## Phase E: Frontend `stars-web`

### Task 13: Vite project scaffold

**Files:**
- Create: `stars-web/*`

- [ ] **Step 1: Scaffold project**

```bash
cd d:/wudgaby/workspace/RuoYi-Vue-Plus
npm create vite@latest stars-web -- --template vue-ts
cd stars-web
npm install element-plus vue-router pinia axios @vueuse/core
npm install -D unplugin-vue-components unplugin-auto-import sass
```

- [ ] **Step 2: Configure `vite.config.ts` proxy**

```typescript
export default defineConfig({
  server: {
    proxy: {
      '/dev-api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/dev-api/, '')
      }
    }
  }
})
```

- [ ] **Step 3: `useBreakpoint` composable**

```typescript
export function useBreakpoint() {
  const isMobile = useMediaQuery('(max-width: 767px)')
  return { isMobile }
}
```

- [ ] **Step 4: Layout switch in `App.vue`**

```vue
<PcLayout v-if="!isMobile"><router-view /></PcLayout>
<MobileLayout v-else><router-view /></MobileLayout>
```

- [ ] **Step 5: Commit**

```bash
git add stars-web/
git commit -m "feat(stars-web): scaffold vue3 vite project"
```

---

### Task 14: Auth & API client

**Files:**
- Create: `stars-web/src/stores/auth.ts`
- Create: `stars-web/src/api/http.ts`
- Create: `stars-web/src/api/stars.ts`

- [ ] **Step 1: Login via RuoYi `/auth/login`**

Store token in localStorage; axios interceptor adds `Authorization: Bearer {token}`

- [ ] **Step 2: Typed API client**

```typescript
export interface StarsRepoCard {
  id: number
  fullName: string
  category: string | null
  tags: string[]
  summaryOneLiner: string | null
  summaryStatus: 'pending' | 'processing' | 'done' | 'failed'
  zreadUrl: string
}

export const listRepos = (params: RepoQuery) =>
  http.get<TableData<StarsRepoCard>>('/stars/repos', { params })
```

- [ ] **Step 3: Route guards — redirect to `/login` if no token**

- [ ] **Step 4: Commit**

---

### Task 15: Repo list page (PC + H5)

**Files:**
- Create: `stars-web/src/views/RepoListView.vue`

- [ ] **Step 1: Search bar + category select + tag multi-select**

- [ ] **Step 2: Card grid — PC 3 columns, H5 1 column**

Show enrichment spinner when `summaryStatus !== 'done'`

- [ ] **Step 3: External link buttons (GitHub / Zread / DeepWiki)**

- [ ] **Step 4: Manual test at 375px and 1280px viewport**

- [ ] **Step 5: Commit**

---

### Task 16: Import center page

**Files:**
- Create: `stars-web/src/views/ImportCenterView.vue`

- [ ] **Step 1: GitHub bind form (PAT input + bind button + status display)**

- [ ] **Step 2: Sync my stars button + import other user form**

- [ ] **Step 3: Job progress polling every 2s**

```typescript
const pollJob = async (jobId: number) => {
  const { data } = await getImportJob(jobId)
  progress.value = data
  if (data.status === 'running' || data.status === 'pending') {
    setTimeout(() => pollJob(jobId), 2000)
  }
}
```

- [ ] **Step 4: Commit**

---

### Task 17: Repo detail & tag management

**Files:**
- Create: `stars-web/src/views/RepoDetailView.vue`
- Create: `stars-web/src/views/TagManageView.vue`

- [ ] **Step 1: Detail page — editable summary, category, tags, note**

Save calls `PUT /stars/repos/{id}`; Regenerate calls `POST .../regenerate-summary`

- [ ] **Step 2: Tag management — full CRUD on PC**

- [ ] **Step 3: H5 simplified tag list (create/delete only)**

- [ ] **Step 4: Commit**

---

## Phase F: Observability, Testing, Deploy

### Task 18: Metrics & tests

**Files:**
- Create: `observe/StarsMetrics.java`
- Test files under `src/test/java`

- [ ] **Step 1: Prometheus counters**

`stars_import_jobs_total`, `stars_enrichment_success_total`, `stars_enrichment_fail_total`, `stars_enrichment_duration`

- [ ] **Step 2: Unit tests — GitHub pagination, enrichment JSON, tag apply**

- [ ] **Step 3: Run full module tests**

Run: `mvn -pl ruoyi-modules/stars-library test`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

---

### Task 19: Documentation & deploy

**Files:**
- Create: `stars-web/README.md`
- Create: `script/nginx/stars-web.conf`
- Modify: `docs/prd/github-stars-knowledge-base.md`

- [ ] **Step 1: stars-web README — dev, build, env vars**

```bash
npm run dev      # http://localhost:5173
npm run build
VITE_API_BASE=/dev-api
```

- [ ] **Step 2: Nginx sample — static + /dev-api proxy**

- [ ] **Step 3: PRD 添加 OpenSpec / plan 交叉链接**

- [ ] **Step 4: E2E checklist**

| # | 步骤 | PC | H5 |
|---|------|----|----|
| 1 | 登录 | ✓ | ✓ |
| 2 | 绑定 PAT | ✓ | ✓ |
| 3 | 导入 10+ stars | ✓ | ✓ |
| 4 | 等待 AI 分类/标签出现 | ✓ | ✓ |
| 5 | 标签筛选 | ✓ | ✓ |
| 6 | 编辑概述 | ✓ | ✓ |
| 7 | 外链跳转 | ✓ | ✓ |

- [ ] **Step 5: Commit**

---

## Spec Coverage Checklist

| Spec | Requirement | Task |
|------|-------------|------|
| github-stars-import | PAT bind | Task 6 |
| github-stars-import | Import own stars | Task 7 |
| github-stars-import | Import others | Task 7 |
| github-stars-import | Rate limit resilience | Task 5, 7 |
| github-stars-import | User isolation | Task 7, 11 |
| ai-repo-enrichment | DeepSeek async enqueue | Task 9 |
| ai-repo-enrichment | Chinese summary | Task 8, 9 |
| ai-repo-enrichment | Auto category/tags | Task 10 |
| ai-repo-enrichment | User override | Task 11 |
| ai-repo-enrichment | Regenerate | Task 9 |
| ai-repo-enrichment | Rate limiting | Task 9 |
| ai-repo-enrichment | README cache | Task 5, 9 |
| stars-organization | Search/filters | Task 11 |
| stars-organization | Tag CRUD | Task 12 |
| stars-organization | Notes | Task 11 |
| stars-organization | External links | Task 11, 15 |
| stars-organization | Batch tags | Task 11 |
| stars-web-app | Vue3+TS+EP+Vite | Task 13 |
| stars-web-app | PC/H5 responsive | Task 13, 15-17 |
| stars-web-app | Auth integration | Task 14 |
| stars-web-app | Core pages | Task 15-17 |
| stars-web-app | Enrichment status UI | Task 15 |
| stars-web-app | Typed API client | Task 14 |

**Gaps:** 无

---

## Suggested Execution Order

```
Phase A (Tasks 1-4)  → 可编译空模块 + 表结构
Phase B (Tasks 5-7)  → 可导入 Stars（无 AI）
Phase C (Tasks 8-10) → 导入后自动 DeepSeek 分类/标签/概述
Phase D (Tasks 11-12)→ REST API 完整
Phase E (Tasks 13-17)→ 前端 PC/H5 可用
Phase F (Tasks 18-19)→ 指标、文档、上线
```

**Estimated duration:** 5–6 周（1 人全职），或 2–3 周（2 人并行：后端 A–D / 前端 E）

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-22-stars-library.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** — 每个 Task 派发独立 subagent，任务间 review，迭代快
2. **Inline Execution** — 当前会话按 Task 顺序执行，阶段性 checkpoint Review

**Which approach?**

Also runnable via **`/opsx:apply`** against OpenSpec change `stars-library`.
