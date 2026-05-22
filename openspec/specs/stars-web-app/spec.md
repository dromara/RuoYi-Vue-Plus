# Stars Web App

## Purpose

Standalone Vue 3 frontend for browsing, importing, and managing GitHub Stars knowledge base on PC and mobile.

## Requirements

### Requirement: Frontend technology stack

The system SHALL deliver a standalone frontend application named `stars-web` built with **Vue 3**, **TypeScript**, **Element Plus**, and **Vite**.

#### Scenario: Project scaffold

- **WHEN** frontend is initialized
- **THEN** repository contains a Vite-based Vue 3 + TypeScript project using Element Plus as the UI component library

### Requirement: Responsive PC and H5 layouts

The frontend SHALL support both desktop (PC) and mobile (H5) browsers with responsive layouts.

#### Scenario: Desktop layout

- **WHEN** viewport width is ≥ 768px
- **THEN** UI displays sidebar navigation and wide list/card layout optimized for PC

#### Scenario: Mobile layout

- **WHEN** viewport width is < 768px
- **THEN** UI displays mobile-friendly navigation (e.g., bottom tabs or collapsible menu)
- **AND** repository cards use a vertical card flow readable on phone screens

### Requirement: Authentication integration

The frontend SHALL authenticate against the RuoYi backend using the platform's token mechanism (JWT / Sa-Token).

#### Scenario: Login required

- **WHEN** unauthenticated user opens any protected route
- **THEN** frontend redirects to login
- **AND** after successful login, frontend attaches auth token to API requests

### Requirement: Core pages

The frontend SHALL implement the following pages for both PC and H5:

#### Scenario: Stars list page

- **WHEN** user opens the home/list route
- **THEN** user can browse repositories with search, category filter, tag filter, and sorting

#### Scenario: Import center page

- **WHEN** user opens import center
- **THEN** user can bind GitHub PAT, sync own stars, import another user's stars, and view import job progress

#### Scenario: Repository detail page

- **WHEN** user opens a repository detail
- **THEN** user can view Chinese summary, AI category, tags, notes, and external links
- **AND** user can edit category, tags, note, and summary

#### Scenario: Tag management page

- **WHEN** user opens tag management on PC
- **THEN** user can CRUD tags
- **WHEN** user opens tag management on H5
- **THEN** user can perform essential tag operations (list, create, delete) in a simplified UI

### Requirement: Enrichment status display

The frontend SHALL display AI enrichment status on repository cards.

#### Scenario: Pending enrichment indicator

- **WHEN** `summary_status` is `pending` or `processing`
- **THEN** card shows a loading or "generating" indicator

#### Scenario: Failed enrichment action

- **WHEN** `summary_status` is `failed`
- **THEN** card shows failure state with a "Retry" action

### Requirement: API client layer

The frontend SHALL use a typed API client (TypeScript) for all Stars Library backend endpoints.

#### Scenario: Typed API calls

- **WHEN** frontend calls backend APIs
- **THEN** requests and responses use TypeScript interfaces shared or generated per endpoint group
