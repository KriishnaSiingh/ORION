# Orion Intelligence — Initial Product Frontend

## Goal
Build a cohesive, production-quality enterprise frontend that demonstrates the complete Orion Intelligence platform. The initial release will use realistic local demo data and working client-side interactions, with dark mode as the default and light mode available.

## Product structure
- Add minimal sign-in and registration screens with a demo entry path; no real account backend in this frontend-only release.
- Build a persistent application shell with collapsible navigation, global search, notifications, profile controls, role-aware Admin visibility, theme switching, and a command palette.
- Create dedicated pages for Dashboard, Knowledge Explorer, Investigation Boards, Ontology Manager, Data Ingestion, Applications, AI Assistant, Settings, and Admin.
- Give detail and builder workflows dedicated URLs so direct links and browser refreshes work correctly.

## Core experiences
- **Dashboard:** KPI summaries, operational charts, recent activity, system health, and quick actions.
- **Knowledge Explorer:** hybrid search, filtering, sorting, pagination, object creation, object details, linked records, graph, lineage, and history views.
- **Investigation Boards:** board library and an interactive canvas with pinned records, notes, connectors, comments, and collaboration presence.
- **Ontology Manager:** object and link type management, property editing/reordering, visual schema graph, and version history.
- **Data Ingestion:** pipeline list, source setup, field mapping, run history, logs, and status states.
- **Applications:** app catalog plus a builder with widget library, canvas, inspector, preview, and draft/published states.
- **AI Assistant:** threaded conversations with dedicated URLs, browser-saved history, source/evidence cards, referenced objects, and an Investigate action. The first release will provide a polished demo interaction rather than paid live model calls.
- **Settings and Admin:** profile, tenant, API key, team/roles, appearance, users, audit, and platform controls.

## Design and interaction
- Establish a semantic token system for deep graphite surfaces, cool neutral borders, electric blue accents, and restrained emerald/amber status colors.
- Use compact enterprise typography, stable dense layouts, subtle motion, accessible focus states, loading skeletons, empty states, dialogs, drawers, badges, tables, and toast feedback.
- Support desktop and tablet layouts; navigation collapses to an icon rail and remains recoverable.
- Use charts for analytics and React Flow for knowledge, ontology, and board canvases.

## Technical approach
- Keep the required TanStack Start router while delivering the requested React routing behavior through file-based page routes.
- Add Zustand for shell and workspace state, React Flow for graph/canvas views, and AI Elements for the Assistant transcript and composer.
- Keep all seeded data in typed frontend modules and persist only Assistant threads plus small user preferences in browser storage.
- Create reusable shell, page-header, metric, data-table, status, graph, and panel components rather than duplicating page markup.
- Add unique metadata for every content route and verify route loading, theme switching, navigation, dialogs, command palette, graph rendering, thread persistence, and tablet layout.

## Scope boundary
This release is a fully navigable frontend prototype. Authentication, multi-user collaboration, data ingestion execution, publishing, permissions enforcement, API keys, and live AI answers will be represented by realistic UI and local state, not production backend services.
