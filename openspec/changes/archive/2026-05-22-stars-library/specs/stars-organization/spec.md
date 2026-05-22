## ADDED Requirements

### Requirement: Repository list with search and filters

The system SHALL provide a paginated list of the user's imported repositories with search and filter capabilities.

#### Scenario: Keyword search

- **WHEN** user enters a keyword
- **THEN** system searches across repository name, owner, description, Chinese summary, note, category, and tag names
- **AND** system returns paginated results with P95 latency ≤ 500ms for up to 5000 records per user

#### Scenario: Filter by category

- **WHEN** user selects one or more categories
- **THEN** system returns only matching repositories

#### Scenario: Filter by tags

- **WHEN** user selects one or more tags
- **THEN** system returns repositories matching any selected tag (OR semantics in MVP)

#### Scenario: Filter by import source

- **WHEN** user filters by import source (self or a GitHub username)
- **THEN** system returns only repositories from that source

### Requirement: Tag management

The system SHALL allow users to create, rename, and delete their own tags.

#### Scenario: Create tag

- **WHEN** user creates a tag with a unique name for their account
- **THEN** system persists the tag with optional color

#### Scenario: Delete tag

- **WHEN** user deletes a tag
- **THEN** system removes associations from repositories
- **AND** system deletes the tag record

### Requirement: Repository notes

The system SHALL allow users to add or edit a note (收藏理由) up to 500 characters per user-repository record.

#### Scenario: Note saved and searchable

- **WHEN** user saves a note on a repository
- **THEN** system persists the note
- **AND** note content is included in keyword search

### Requirement: Deep exploration external links

The system SHALL provide one-click external links for each repository.

#### Scenario: External links available

- **WHEN** user views a repository card or detail
- **THEN** system provides links to GitHub repository URL
- **AND** system provides link to `https://zread.ai/{owner}/{repo}`
- **AND** system provides link to configured DeepWiki URL template

### Requirement: Batch tag assignment

The system SHALL allow users to assign tags to multiple selected repositories in one action.

#### Scenario: Batch tagging

- **WHEN** user selects multiple repositories and assigns tags
- **THEN** system adds the tags to each selected repository without removing existing tags
