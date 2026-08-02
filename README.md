# Maple Growth Tracker

Maple Growth Tracker is a documentation-first MVP for anonymous MapleStory character growth tracking.
It pairs a Spring Boot backend, a Next.js frontend, and a Supabase PostgreSQL schema to show a character's current snapshot, recent 7-day combat-power trend, and basic growth events.

## Project Layout

- `backend/` - Spring Boot 3.x backend, Nexon OpenAPI client, persistence, scheduler, and REST API.
- `frontend/` - Next.js 14+ TypeScript dashboard.
- `database/` - Executable SQL schema for PostgreSQL/Supabase.
- `doc/` - Domain, API, UI, and ops documentation.
- `docs/plans/` - Compound Engineering implementation plans.

## Setup

1. Create or select a PostgreSQL database.
2. Apply the schema in [`database/schema.sql`](database/schema.sql).
3. Copy [`backend/.env.example`](backend/.env.example) to a local secret file and fill in backend values.
4. Copy [`frontend/.env.example`](frontend/.env.example) to a local secret file and fill in frontend values.
5. Start the backend from `backend/`.
6. Start the frontend from `frontend/`.

### Backend

Backend configuration is loaded from environment variables. The important values are:

- `NEXON_API_KEY`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_TIMEZONE`
- `APP_SNAPSHOT_CRON`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_NEXON_BASE_URL`
- `APP_NEXON_TIMEOUT_SECONDS`

For local development against PostgreSQL, the example file includes `SPRING_DATASOURCE_DRIVER=org.postgresql.Driver`.
The backend test profile can still start with the H2 defaults in `backend/src/main/resources/application.yml`.

Common local commands:

```bash
cd backend
./gradlew test
./gradlew bootRun
```

### Frontend

Frontend configuration is loaded from browser-visible `NEXT_PUBLIC_` variables only:

- `NEXT_PUBLIC_API_BASE_URL`
- `NEXT_PUBLIC_APP_TIMEZONE`

Common local commands:

```bash
cd frontend
npm install
npm run typecheck
npm run build
npm run dev
```

## Smoke Verification

The first manual smoke path is:

1. Start the backend.
2. Start the frontend.
3. Open `/` and search for a character nickname.
4. Confirm the app routes to `/character/[name]`.
5. Confirm the dashboard renders either character data or a well-formed failure state.

Useful verification commands:

```bash
cd backend
./gradlew test

cd frontend
npm run typecheck
npm run build
```

## Documentation Map

- [`doc/plans/mvp_requirements.md`](doc/plans/mvp_requirements.md)
- [`doc/domain/snapshot_policy.md`](doc/domain/snapshot_policy.md)
- [`doc/domain/growth_event_rules.md`](doc/domain/growth_event_rules.md)
- [`doc/api/api_contract.md`](doc/api/api_contract.md)
- [`doc/ui/ui_states.md`](doc/ui/ui_states.md)
- [`doc/ops/env_and_deployment.md`](doc/ops/env_and_deployment.md)
- [`doc/db/schema_design.md`](doc/db/schema_design.md)
- [`docs/plans/2026-08-02-001-feat-maple-growth-mvp-plan.md`](docs/plans/2026-08-02-001-feat-maple-growth-mvp-plan.md)

## Notes

- Keep real secrets out of Git.
- Do not put `NEXON_API_KEY`, database URLs, or database passwords under any `NEXT_PUBLIC_` variable.
- `database/schema.sql` is the executable schema source of truth for local setup.
