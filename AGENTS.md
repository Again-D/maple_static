# AGENTS.md

## Project

Maple Growth Tracker is a documentation-first MVP for anonymous MapleStory character growth tracking.
The MVP shows profile data, current metrics, a recent 7-day combat-power trend, and basic growth events.

Workspace shape:

- `backend/`: Spring Boot 3.x, Nexon OpenAPI, Supabase PostgreSQL, snapshot sync, events, scheduler.
- `frontend/`: Next.js 14+ TypeScript dashboard.
- `database/`: executable SQL schema.
- `doc/`: product/domain/API/UI/ops docs.
- `docs/plans/`: Compound Engineering implementation plans.

## Source Of Truth

Use these in order, and let newer MVP docs override older broad notes when they conflict:

1. `docs/plans/2026-08-02-001-feat-maple-growth-mvp-plan.md`
2. `doc/plans/mvp_requirements.md`
3. `doc/domain/snapshot_policy.md`
4. `doc/domain/growth_event_rules.md`
5. `doc/api/api_contract.md`
6. `doc/ui/ui_states.md`
7. `doc/ops/env_and_deployment.md`
8. `database/schema.sql`
9. `doc/plans/implementation_plan.md` and `doc/ui/screen_design.md` only when they do not conflict with the MVP docs above.

## MVP Scope

Implement:

- Anonymous nickname search.
- Nexon lookup on DB miss.
- Character profile and current metric display.
- One representative KST snapshot per character per date.
- Same-day snapshot upsert.
- Recent 7-day combat-power chart.
- `LEVEL_UP`, `COMBAT_POWER_CHANGE`, `HEXA_UPGRADED`, `UNION_UPGRADED` events.
- Manual refresh.
- 04:00 KST auto-collection for auto-tracked characters.
- Loading, empty, not-found, API failure, rate-limited, refresh-loading, mobile, and accessibility states.

Do not implement:

- Login/accounts, favorites, popular carousel, OpenGraph, equipment diff UI, automatic `ITEM_REPLACED`, boss/meso/node events, admin dashboard, or multi-instance scheduler locking unless production requires it.

## Implementation Order

Follow the plan units in `docs/plans/2026-08-02-001-feat-maple-growth-mvp-plan.md`.
Prefer backend/domain work before frontend polish.

Unit order:

1. U1 scaffolds
2. U2 schema hardening
3. U3 Nexon API client
4. U4 snapshot sync
5. U5 growth events
6. U6 REST API
7. U7 scheduler/ops config
8. U8 frontend shell/API client
9. U9 dashboard states
10. U10 docs + smoke verification

## Data And Backend Rules

- All service dates use `Asia/Seoul`.
- `snapshot_date` is KST-based.
- Same-day refresh updates the existing snapshot, not a new row.
- Growth events compare the current snapshot with the latest prior-date snapshot.
- Cached dashboard data must stay visible when Nexon fails.
- Failed sync attempts must not overwrite the last successful fetch time.
- Unknown optional metrics stay `null`, not `0`.
- Nexon calls happen only in the backend, with `x-nxopen-api-key`.
- Never expose `NEXON_API_KEY`, DB URL, DB username, or DB password to frontend code.
- Use transactions for first registration, snapshot upsert, and event recomputation.
- Keep raw Nexon JSON and API keys out of public API responses and logs.

Before service work, harden `database/schema.sql` to add:

- `daily_snapshots.captured_at`
- `characters.last_sync_attempted_at`
- `characters.last_sync_error_code`
- `growth_event_logs.event_key`
- a unique dedupe constraint for equivalent generated events

## Frontend Rules

- Use `/` for search and `/character/[name]` for the dashboard.
- Trim and validate nickname input before submit.
- Prevent duplicate search and refresh submissions while pending.
- URL-encode character names in routes and API calls.
- Refetch the dashboard after successful refresh.
- Keep cached content visible during manual refresh.
- Show cached failure banners when data exists; show a full retryable error when it does not.
- Distinguish `CHARACTER_NOT_FOUND` from retryable API failures.
- Do not show recent-search chips, popular carousel, active `30d`/`all` ranges, or equipment diff drawer in MVP.
- Rising/falling values must include text or signs, not color alone.

## Environment And Secrets

Backend env vars:

- `NEXON_API_KEY`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_TIMEZONE=Asia/Seoul`
- `APP_SNAPSHOT_CRON=0 0 4 * * *`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_NEXON_BASE_URL`
- `APP_NEXON_TIMEOUT_SECONDS`

Frontend public env vars:

- `NEXT_PUBLIC_API_BASE_URL`
- `NEXT_PUBLIC_APP_TIMEZONE=Asia/Seoul`

`NEXT_PUBLIC_` variables are browser-visible.
Never put secrets under `NEXT_PUBLIC_`.
Do not commit real `.env`, `.env.local`, `application-local.yml`, API keys, DB passwords, or production credentials.

## Testing Expectations

Add tests with each feature-bearing unit.

Backend tests should cover KST boundaries, first-registration transactions, same-day upserts, Nexon error mapping, cached-dashboard behavior, event thresholds/idempotency, scheduler partial failure, API wrapper shape, and secret non-exposure.

Frontend tests should cover blank search, trimmed/encoded navigation, API mapping, dashboard loading and empty states, refresh states, not-found and rate-limited states, and basic mobile/accessibility behavior.

Smoke verification should cover DB initialization, backend startup, frontend startup, and search-to-dashboard behavior.

## Documentation Rules

- Keep docs in sync with behavior changes.
- Use `doc/` for product/domain/API/UI/ops docs.
- Use `docs/plans/` for CE implementation plans.
- Use repo-relative paths only.
- Do not add non-portable local file URI links.
- Keep `README.md` focused on purpose, setup, doc map, and smoke verification once implementation starts.

## Git And Worktree Hygiene

- Do not revert user changes unless explicitly asked.
- Keep generated scaffolding and dead-end experiments out of the final diff.
- Commit only intentional project files.
- Do not commit secrets or build outputs such as `.next/`, `node_modules/`, `.gradle/`, or `target/`.
