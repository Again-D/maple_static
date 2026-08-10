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

## Local Setup

1. Apply [`database/schema.sql`](database/schema.sql) to PostgreSQL or Supabase first.
2. Keep backend-only secrets such as `NEXON_API_KEY` in a backend env file, not in the frontend.
3. Keep browser-visible values limited to `NEXT_PUBLIC_` variables in the frontend env file.
4. Use [`.env.local.example`](.env.local.example) for the local Docker path.
5. Use [`.env.supabase.example`](.env.supabase.example) for the Supabase Docker path.
6. Start the backend before the frontend.

### Docker

There are two selectable Docker paths:

#### Local PostgreSQL

1. Copy [`.env.local.example`](.env.local.example) to `.env.local` and fill in any values you want to override.
2. Start the stack:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml --env-file .env.local up --build
```

This starts:

- PostgreSQL on `localhost:5432`
- Spring Boot backend on `localhost:8080`
- Next.js frontend on `localhost:3000`

The local database is seeded from [`database/schema.sql`](database/schema.sql).

#### Supabase PostgreSQL

1. Copy [`.env.supabase.example`](.env.supabase.example) to `.env.supabase` and fill in your Supabase values.
2. Start the stack:

```bash
docker compose -f docker-compose.yml -f docker-compose.supabase.yml --env-file .env.supabase up --build
```

This keeps the frontend and backend in Docker, but points the backend at Supabase instead of the local `postgres` container.

### Backend

Backend configuration is loaded from environment variables. The important values are:

- `NEXON_API_KEY`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_TIMEZONE`
- `APP_SNAPSHOT_CRON`
- `APP_SCHEDULER_DUPLICATE_WAIT_SECONDS`
- `APP_OPERATIONS_API_TOKEN`
- `APP_COLLECTION_RETRY_CRON`
- `APP_COLLECTION_RETRY_BATCH_SIZE`
- `APP_COLLECTION_RETRY_MAX_ATTEMPTS`
- `APP_COLLECTION_RETRY_INITIAL_BACKOFF_SECONDS`
- `APP_COLLECTION_RETRY_LEASE_SECONDS`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_NEXON_BASE_URL`
- `APP_NEXON_TIMEOUT_SECONDS`

For local development against PostgreSQL, the example file includes `SPRING_DATASOURCE_DRIVER=org.postgresql.Driver`.
The backend test profile can still start with the H2 defaults in `backend/src/main/resources/application.yml`.

The token-protected operations endpoint is `GET /api/v1/operations/collections` with the `X-Operations-Token` header. It exposes sanitized collection summaries and retry counts for operators; never expose the token through a `NEXT_PUBLIC_` variable.

To confirm that your local Nexon key is real:

1. Open `.env.local` or the file you pass to `--env-file`.
2. Make sure `NEXON_API_KEY` is not the example placeholder value.
3. After the stack starts, check the injected value inside the backend container:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml exec backend printenv NEXON_API_KEY
```

If that command prints a placeholder like `replace-with-your-nexon-api-key` or a `test_...` value, update the env file and restart the stack.

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
6. If Nexon or credentials are unavailable, confirm the dashboard shows the correct retryable or not-found state and keeps cached data visible when it exists.

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
