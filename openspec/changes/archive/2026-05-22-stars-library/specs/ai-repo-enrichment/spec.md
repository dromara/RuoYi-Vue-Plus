## ADDED Requirements

### Requirement: DeepSeek-powered enrichment on import

The system SHALL enqueue an asynchronous enrichment task for each newly imported user-repository record. Enrichment MUST use the **DeepSeek** model via Spring AI (`deepseek-chat` or configured DeepSeek ChatClient).

#### Scenario: Enrichment triggered after import

- **WHEN** a repository is successfully linked to a user during import
- **THEN** system enqueues an enrichment task with status `pending`
- **AND** system processes the task asynchronously without blocking the import UI

### Requirement: Generated Chinese summary

The system SHALL generate a Chinese project summary from repository metadata and README excerpt (up to 3000 characters).

#### Scenario: Summary fields populated

- **WHEN** enrichment completes successfully
- **THEN** system stores `summary_one_liner` (≤50 Chinese characters)
- **AND** system stores `summary_text` (≤200 Chinese characters, up to 3 lines)
- **AND** system sets `summary_status` to `done` and `summary_source` to `ai`

#### Scenario: Summary generation failure

- **WHEN** enrichment fails after maximum retries
- **THEN** system sets `summary_status` to `failed`
- **AND** system retains GitHub `description` as fallback display where available

### Requirement: Auto-generated category and tags

The system SHALL use DeepSeek to generate and **automatically apply** a primary **category** and up to **5 tags** for each imported repository.

#### Scenario: Category and tags applied on success

- **WHEN** enrichment completes successfully
- **THEN** system assigns one `category` value to the user-repository record
- **AND** system creates missing tag records for the user if they do not exist
- **AND** system associates generated tags with the user-repository record
- **AND** system sets `classification_source` to `ai`

#### Scenario: Category taxonomy guidance

- **WHEN** DeepSeek generates a category
- **THEN** system SHOULD prefer values from the configured taxonomy: `AI/RAG`, `后端框架`, `前端组件`, `DevOps`, `数据库`, `工具库`, `学习参考`, `待评估`
- **AND** system MAY store a close-match category outside taxonomy if none fit

### Requirement: User override of AI classification

The system SHALL allow users to edit or replace AI-generated category, tags, and summary.

#### Scenario: Manual edit marks source

- **WHEN** user edits category or tags on a repository detail page
- **THEN** system persists the changes
- **AND** system sets `classification_source` or `summary_source` to `manual` for edited fields

#### Scenario: Regenerate enrichment

- **WHEN** user clicks "Regenerate" on a repository
- **THEN** system re-runs DeepSeek enrichment
- **AND** system overwrites AI fields unless user chooses to keep manual overrides (MVP: overwrite all AI fields)

### Requirement: Enrichment rate limiting

The system SHALL limit concurrent enrichment tasks to protect DeepSeek quota and system stability.

#### Scenario: Per-user concurrency cap

- **WHEN** multiple repositories are imported for one user
- **THEN** system processes at most 3 concurrent enrichment tasks per user (configurable)
- **AND** remaining tasks stay queued

### Requirement: Enrichment input sources

The system SHALL fetch README content via GitHub API when available and cache it to reduce repeated API calls.

#### Scenario: README used as enrichment input

- **WHEN** GitHub README is accessible
- **THEN** system decodes README, truncates to 3000 characters, and includes it in the DeepSeek prompt
- **AND** system caches README snippet on the shared `stars_repo` record for 24 hours
