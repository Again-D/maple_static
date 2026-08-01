---
title: Initial Definition Gaps - Plan
type: docs
date: 2026-08-02
topic: initial-definition-gaps
artifact_contract: ce-unified-plan/v1
artifact_readiness: requirements-only
product_contract_source: ce-brainstorm
execution: code
---

# Initial Definition Gaps - Plan

## Goal Capsule

- **Objective:** Capture the project definitions that should be written before the first implementation pass for Maple Growth Tracker.
- **Product authority:** This document refines the existing planning, database, and UI documents under `doc/` without replacing them.
- **Open blockers:** None block planning, but MVP scope, snapshot policy, event rules, and API contract should be defined before backend and frontend implementation diverge.

---

## Product Contract

### Summary

Maple Growth Tracker already has a clear product direction, technical stack, database outline, and dashboard concept.
Before implementation starts, the project needs a small set of decision documents that define first-release scope, data collection behavior, growth-event rules, and frontend/backend API expectations.

### Problem Frame

The current documents describe the intended full service: a MapleStory character growth dashboard that stores daily snapshots, compares them, and visualizes growth over time.
That is enough to understand the product, but not enough to prevent implementation-time product decisions from being made ad hoc.
The riskiest gaps are the ones that affect persisted data meaning or user-visible behavior: when a snapshot is taken, what counts as a growth event, what the frontend receives, and what is explicitly excluded from the first release.

### Requirements

**MVP scope**

- R1. The project must define a first-release scope before implementation begins.
- R2. The MVP scope must distinguish included features from deferred features, especially character search, first snapshot creation, manual refresh, 7-day charts, event timeline, popular-character carousel, OpenGraph thumbnails, login, and favorites.
- R3. The MVP scope must define the minimum user-visible success path from nickname search to dashboard display.

**Snapshot and sync behavior**

- R4. The project must define the canonical snapshot date and timezone policy.
- R5. The project must resolve the current mismatch between the backend plan's daily midnight scheduler and the UI example that says data was collected at 04:00.
- R6. The project must define whether manual refresh creates, updates, or refuses a same-day snapshot.
- R7. The project must define what users see when Nexon API data is unavailable, delayed, incomplete, or rate-limited.

**Growth-event rules**

- R8. The project must define event rules before storing production-like event logs.
- R9. The event rules must specify how `LEVEL_UP`, `COMBAT_POWER_CHANGE`, `ITEM_REPLACED`, `HEXA_UPGRADED`, and `UNION_UPGRADED` are detected.
- R10. The event rules must define thresholds for noisy changes, especially combat-power deltas.
- R11. The event rules must define duplicate prevention for repeated syncs on the same date.

**API and UI states**

- R12. The project must define the frontend/backend response contract for character profile, growth history, summary cards, and timeline events.
- R13. The response contract must include empty, loading, not-found, API-failure, and insufficient-snapshot states.
- R14. The UI state document must cover first-time character lookup, no prior history, mobile layout, and failed refresh behavior.

**Operational readiness**

- R15. The project should define local environment variables, secret handling, CORS expectations, and deployment targets before wiring the real Nexon API and Supabase.
- R16. The repository should include a README before broader development continues, so GitHub readers can understand the project shape quickly.

### Key Decisions

- **Write narrow definition docs before code.** The next documentation pass should focus on decisions that would otherwise be invented during implementation, not a full product bible.
- **Prioritize persisted-data semantics.** Snapshot policy and event rules should be defined before backend implementation because changing them later changes the meaning of stored records.
- **Keep first-release scope explicit.** The current implementation plan describes phases, but the first usable release still needs a concrete include/defer list.
- **Treat UI error and empty states as product behavior.** The current UI document focuses on the ideal dashboard state; implementation needs the non-ideal states too.

### Recommended Documents

| Priority | Document | Purpose |
| --- | --- | --- |
| 1 | `doc/plans/mvp_requirements.md` | Define first-release scope, non-goals, success path, and acceptance criteria. |
| 2 | `doc/domain/snapshot_policy.md` | Define snapshot timing, timezone, same-day refresh behavior, API failure behavior, and retention expectations. |
| 3 | `doc/domain/growth_event_rules.md` | Define event detection rules, thresholds, importance levels, and duplicate prevention. |
| 4 | `doc/api/api_contract.md` | Define backend response shapes and frontend state handling for profile, history, summary, and timeline data. |
| 5 | `doc/ui/ui_states.md` | Define loading, empty, error, insufficient-data, and mobile states. |
| 6 | `doc/ops/env_and_deployment.md` | Define local environment, secrets, CORS, deployment targets, and operational assumptions. |
| 7 | `README.md` | Explain the project, stack, current status, and development entry points. |

### Scope Boundaries

- Detailed backend architecture, class names, endpoint implementation, and scheduler implementation are deferred to implementation planning.
- Exact chart component structure and CSS implementation are deferred to frontend planning.
- Authentication, favorites, and personalized tracking should remain out of MVP unless `doc/plans/mvp_requirements.md` explicitly pulls them in.
- Popular-character rankings and OpenGraph thumbnails should be treated as later features unless the MVP document says otherwise.

### Dependencies / Assumptions

- Existing source documents are `doc/plans/implementation_plan.md`, `doc/db/schema_design.md`, `doc/ui/screen_design.md`, and `database/schema.sql`.
- The current codebase is primarily documentation plus the database schema; backend and frontend application folders have not been created yet.
- The project will use Nexon OpenAPI data, so API failure and data freshness behavior should be considered first-class product rules.
- Supabase free-plan limits make data retention and JSONB storage policy worth defining before broad snapshot collection.

### Outstanding Questions

**Resolve Before Planning**

- Which MVP depth should the first implementation target: minimal searchable dashboard, dashboard plus manual refresh, or dashboard plus event timeline?
- What is the canonical daily snapshot collection time in Korea time?
- Should same-day manual refresh overwrite the daily snapshot or only update `last_fetched_at` and return live data?

**Deferred to Planning**

- Which backend deployment host should be used for the first runnable version?
- Which frontend deployment target should be used after local development works?
- How much automated test coverage is required before the first private demo?

### Sources

- `doc/plans/implementation_plan.md`
- `doc/db/schema_design.md`
- `doc/ui/screen_design.md`
- `database/schema.sql`
