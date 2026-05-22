## ADDED Requirements

### Requirement: GitHub account binding

The system SHALL allow an authenticated user to bind a GitHub Personal Access Token (PAT) with scopes sufficient to read starred repositories (`read:user` and public repo access).

#### Scenario: Successful binding

- **WHEN** user submits a valid GitHub PAT
- **THEN** system encrypts and stores the token associated with the user's RuoYi account
- **AND** system displays the bound GitHub username

#### Scenario: Invalid token rejected

- **WHEN** user submits an invalid or expired PAT
- **THEN** system returns an error message without storing the token

### Requirement: Import own starred repositories

The system SHALL allow a user with a bound GitHub account to import or sync all repositories starred by the authenticated GitHub user.

#### Scenario: Full import with progress

- **WHEN** user triggers "Sync My Stars"
- **THEN** system creates an asynchronous import job
- **AND** system displays job progress (total, processed, failed)
- **AND** system upserts each repository using `owner/repo` as unique key per user

#### Scenario: Incremental sync preserves user data

- **WHEN** user syncs again after a previous import
- **THEN** system adds new starred repositories
- **AND** system updates repository metadata for existing entries
- **AND** system MUST NOT remove user tags, notes, or manual classification

### Requirement: Import another user's public stars

The system SHALL allow any authenticated user to import the public starred repository list of a given GitHub username.

#### Scenario: Import public stars list

- **WHEN** user submits a valid public GitHub username and confirms import
- **THEN** system fetches all publicly starred repositories via GitHub API pagination
- **AND** system records `import_source` as the source username

#### Scenario: Invalid or private user

- **WHEN** the GitHub username does not exist or stars are not accessible
- **THEN** system returns a friendly error and marks the job as failed

### Requirement: Import job resilience

The system SHALL handle GitHub API rate limits during import jobs without data corruption.

#### Scenario: Rate limit pause and resume

- **WHEN** GitHub API returns 403 or 429 during import
- **THEN** system pauses the job according to Retry-After or configured backoff
- **AND** system resumes processing until complete or user-visible failure

### Requirement: User data isolation

The system SHALL isolate each user's imported repositories, tags, and notes by RuoYi `user_id`.

#### Scenario: User sees only own data

- **WHEN** a non-admin user lists repositories
- **THEN** system returns only repositories belonging to that user
