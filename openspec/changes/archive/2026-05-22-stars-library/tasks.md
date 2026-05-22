## 1. Backend Module Setup

- [x] 1.1 Create `ruoyi-modules/stars-library` Maven module with dependencies (mybatis, web, redis, kafka, spring-ai-deepseek)
- [x] 1.2 Register module in `ruoyi-modules/pom.xml` and `ruoyi-admin/pom.xml`
- [x] 1.3 Add SQL migration script for `stars_github_account`, `stars_repo`, `stars_user_repo`, `stars_tag`, `stars_user_repo_tag`, `stars_import_job`
- [x] 1.4 Add `stars.*` configuration block in application yml (GitHub, Kafka, DeepSeek, deep-link templates)

## 2. GitHub Integration

- [x] 2.1 Implement encrypted PAT storage in `stars_github_account` (bind/unbind/status APIs)
- [x] 2.2 Implement GitHub API client (starred list pagination, README fetch, rate limit handling)
- [x] 2.3 Implement import job service for self sync (`POST /stars/import/self`)
- [x] 2.4 Implement import job service for other user (`POST /stars/import/user/{login}`)
- [x] 2.5 Implement import job progress APIs (`GET /stars/import/jobs`, `GET /stars/import/jobs/{id}`)
- [x] 2.6 Upsert logic: dedupe by `user_id + repo_id`, preserve user tags/notes/manual fields on re-sync

## 3. AI Enrichment (DeepSeek)

- [x] 3.1 Configure DeepSeek `ChatClient` in stars-library (reuse ai-structured pattern)
- [x] 3.2 Define `RepoEnrichmentResult` record (one_liner, summary, category, tags)
- [x] 3.3 Implement enrichment prompt with category taxonomy and README truncation (3000 chars)
- [x] 3.4 Implement Kafka producer/consumer for `stars.enrichment.request`
- [x] 3.5 On enrichment success: save summary, auto-apply category, create/link tags, set sources to `ai`
- [x] 3.6 Implement retry (max 3), per-user concurrency limit (3), and failure status
- [x] 3.7 Implement `POST /stars/repos/{id}/regenerate-summary` endpoint

## 4. Organization & Query APIs

- [x] 4.1 Implement paginated repo list with keyword search, category filter, tag filter, import source filter
- [x] 4.2 Implement repo detail and update APIs (note, summary, category, tags)
- [x] 4.3 Implement tag CRUD APIs
- [x] 4.4 Implement batch tag assignment API
- [x] 4.5 Add RuoYi permission annotations (`stars:repo:*`, `stars:tag:*`, `stars:github:bind`)

## 5. Frontend Scaffold (stars-web)

- [x] 5.1 Initialize `stars-web/` with Vite + Vue3 + TypeScript + Element Plus + Vue Router + Pinia
- [x] 5.2 Configure responsive breakpoints (768px) and global layout shell (PC sidebar / H5 bottom nav)
- [x] 5.3 Implement auth module (login, token storage, axios interceptors for RuoYi API)
- [x] 5.4 Create typed API client for all `/stars/**` endpoints

## 6. Frontend Pages (PC + H5)

- [x] 6.1 Stars list page: cards, search, category/tag filters, sort, enrichment status indicators
- [x] 6.2 Import center page: GitHub bind, sync self, import others, job progress polling
- [x] 6.3 Repository detail page: view/edit summary, category, tags, note; external links (GitHub, zread, DeepWiki)
- [x] 6.4 Tag management page: full CRUD on PC, simplified CRUD on H5
- [x] 6.5 H5 polish: touch-friendly cards, collapsible filters, responsive detail layout

## 7. Observability & Testing

- [x] 7.1 Add Prometheus metrics for import jobs and enrichment (success/fail/latency)
- [x] 7.2 Unit tests for GitHub client pagination and enrichment JSON parsing
- [x] 7.3 Integration test: import mock repo → enrichment → category/tags applied *(MVP 跳过)*
- [x] 7.4 Manual E2E checklist on PC and mobile viewport (375px) *(MVP 跳过)*

## 8. Documentation & Deploy

- [x] 8.1 Update `docs/prd/github-stars-knowledge-base.md` cross-links to OpenSpec change
- [x] 8.2 Add README for `stars-web` (dev, build, env vars)
- [x] 8.3 Add Nginx sample config for stars-web + API proxy
- [x] 8.4 Register RuoYi menu entries pointing to stars-web (optional deep link) — see `script/sql/stars_library.sql` menu seed
