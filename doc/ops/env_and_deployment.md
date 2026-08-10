# Environment and Deployment - Maple Growth Tracker MVP

## 1. 목적

이 문서는 Maple Growth Tracker MVP를 로컬과 배포 환경에서 실행하기 위해 필요한 환경 변수, 비밀값 관리, 스케줄러 설정, 배포 전 확인 기준을 정의한다.
구현 단계에서 Nexon API Key, Supabase 접속 정보, 프론트엔드 API 주소를 임의로 다루지 않도록 하는 것이 목표다.

## 2. 환경 구분

| 환경 | 목적 | 데이터 | 비고 |
| :--- | :--- | :--- | :--- |
| `local` | 개발자 로컬 실행 | 개발 DB 또는 개인 Supabase 프로젝트 | 실제 Nexon API 호출 가능 |
| `preview` | PR 또는 배포 전 검증 | 별도 preview DB 권장 | MVP에서는 선택 사항 |
| `production` | 실제 사용자 접근 | 운영 Supabase DB | 비밀값 노출 금지 |

- O1. MVP 구현은 `local`과 `production`을 우선 지원한다.
- O2. `preview` 환경은 배포 플랫폼이 준비된 뒤 추가한다.
- O3. 환경별 DB는 가능하면 분리한다.
- O4. 운영 DB의 데이터를 로컬 개발에 직접 사용하지 않는다.

## 3. 비밀값 관리 원칙

- O5. Nexon API Key는 백엔드에서만 사용한다.
- O6. Nexon API Key를 Next.js 클라이언트 번들에 포함하지 않는다.
- O7. Supabase DB 접속 문자열은 백엔드 서버 환경 변수로만 주입한다.
- O8. `.env`, `.env.local`, `application-local.yml` 같은 로컬 비밀값 파일은 Git에 커밋하지 않는다.
- O9. 문서와 예시 파일에는 실제 키 대신 placeholder만 적는다.
- O10. 운영 비밀값은 배포 플랫폼의 secret manager 또는 environment variable 설정 기능에 저장한다.

## 4. 백엔드 환경 변수

Spring Boot 백엔드는 아래 환경 변수를 기준으로 설정한다.

| 이름 | 필수 | 예시 | 설명 |
| :--- | :--- | :--- | :--- |
| `NEXON_API_KEY` | yes | `nx_...` | Nexon OpenAPI 호출용 키 |
| `SPRING_DATASOURCE_URL` | yes | `jdbc:postgresql://host:5432/postgres` | Supabase PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | yes | `postgres` | DB 사용자 |
| `SPRING_DATASOURCE_PASSWORD` | yes | `********` | DB 비밀번호 |
| `APP_TIMEZONE` | yes | `Asia/Seoul` | 서비스 날짜 계산 기준 |
| `APP_SNAPSHOT_CRON` | yes | `0 0 4 * * *` | 자동 스냅샷 수집 cron |
| `APP_SCHEDULER_DUPLICATE_WAIT_SECONDS` | no | `300` | 중복 스케줄 실행 대기 상한(초) |
| `APP_OPERATIONS_API_TOKEN` | yes | `replace-with-an-operations-token` | 운영 상태 API 접근 토큰 |
| `APP_COLLECTION_RETRY_CRON` | no | `0 */15 * * * *` | 재시도 작업 polling cron |
| `APP_COLLECTION_RETRY_BATCH_SIZE` | no | `20` | 한 번에 처리할 최대 재시도 작업 수 |
| `APP_COLLECTION_RETRY_MAX_ATTEMPTS` | no | `3` | 재시도 작업의 최대 시도 횟수 |
| `APP_COLLECTION_RETRY_INITIAL_BACKOFF_SECONDS` | no | `300` | 첫 재시도까지의 대기 시간(초) |
| `APP_COLLECTION_RETRY_LEASE_SECONDS` | no | `900` | 중단된 claim을 회수하기까지의 시간(초) |
| `APP_CORS_ALLOWED_ORIGINS` | yes | `http://localhost:3000` | 프론트엔드 허용 origin |
| `APP_NEXON_BASE_URL` | no | `https://open.api.nexon.com` | Nexon API base URL |
| `APP_NEXON_TIMEOUT_SECONDS` | no | `10` | Nexon API 호출 timeout |

- O11. `APP_TIMEZONE`의 기본값은 `Asia/Seoul`이어야 한다.
- O12. `APP_SNAPSHOT_CRON`의 MVP 기본값은 한국 시간 기준 매일 04:00 실행이다.
- O13. 서버 물리 타임존과 무관하게 스냅샷 날짜 계산은 `APP_TIMEZONE`을 사용한다.
- O14. 운영 환경의 CORS origin은 실제 프론트엔드 도메인만 허용한다.
- O14a. `APP_SCHEDULER_DUPLICATE_WAIT_SECONDS`의 기본값은 300초(5분)이다.
- O14b. 이미 자동 수집이 진행 중이면 후속 실행은 설정된 대기 상한까지 기다린 뒤 중복 수집을 건너뛴다.
- O14c. `APP_SCHEDULER_DUPLICATE_WAIT_SECONDS`가 0 이하이면 애플리케이션 시작을 실패시키고 스케줄러를 활성화하지 않는다.
- O14d. 운영 상태 API는 `X-Operations-Token`이 `APP_OPERATIONS_API_TOKEN`과 일치할 때만 응답한다.
- O14e. 운영 토큰은 백엔드 secret manager에만 저장하고 `NEXT_PUBLIC_` 환경변수나 로그에 넣지 않는다.
- O14f. retry batch size, max attempts, initial backoff, lease seconds는 양수로 검증한다.

## 5. 프론트엔드 환경 변수

Next.js 프론트엔드는 아래 환경 변수를 기준으로 설정한다.

| 이름 | 필수 | 예시 | 설명 |
| :--- | :--- | :--- | :--- |
| `NEXT_PUBLIC_API_BASE_URL` | yes | `http://localhost:8080` | Spring Boot API 서버 주소 |
| `NEXT_PUBLIC_APP_TIMEZONE` | yes | `Asia/Seoul` | 화면의 날짜/시간 표시 기준 |

- O15. `NEXT_PUBLIC_` 접두사가 붙은 값은 브라우저에 노출된다는 전제로 작성한다.
- O16. Nexon API Key, DB URL, DB 비밀번호는 `NEXT_PUBLIC_` 변수에 절대 넣지 않는다.
- O17. 프론트엔드는 서버 시간과 timezone을 API `meta`에서 받아 표시 기준을 맞춘다.

## 6. 로컬 실행 기준

로컬 MVP 실행은 아래 순서를 기준으로 한다.

1. Supabase 프로젝트 또는 로컬 PostgreSQL을 준비한다.
2. `database/schema.sql`을 실행한다.
3. 백엔드 환경 변수를 설정한다.
4. Spring Boot 백엔드를 실행한다.
5. 프론트엔드 환경 변수를 설정한다.
6. Next.js 프론트엔드를 실행한다.
7. 브라우저에서 `/`에 접속해 캐릭터 검색을 검증한다.

Docker Compose로 실행할 때는 아래 두 경로를 선택할 수 있다.

- 로컬 PostgreSQL: `docker compose -f docker-compose.yml -f docker-compose.local.yml --env-file .env.local up --build`
- Supabase PostgreSQL: `docker compose -f docker-compose.yml -f docker-compose.supabase.yml --env-file .env.supabase up --build`

각 경로의 예시 변수는 [`.env.local.example`](../../.env.local.example)와 [`.env.supabase.example`](../../.env.supabase.example)를 따른다.

- O18. 로컬 DB schema는 `database/schema.sql`을 기준으로 생성한다.
- O19. 백엔드는 `/api/v1/characters/{name}` 호출이 Nexon API와 DB 저장까지 수행되는지 먼저 확인한다.
- O20. 프론트엔드는 `NEXT_PUBLIC_API_BASE_URL`을 통해서만 백엔드와 통신한다.
- O21. 로컬에서도 시간 기준은 `Asia/Seoul`로 고정한다.

## 7. 배포 구성 원칙

MVP의 기본 배포 형태는 아래 구조를 전제로 한다.

| 구성 요소 | 권장 배포 단위 |
| :--- | :--- |
| Frontend | 정적/서버 렌더링 가능한 Next.js 배포 플랫폼 |
| Backend | Spring Boot를 실행할 수 있는 서버 또는 컨테이너 |
| Database | Supabase PostgreSQL |
| Scheduler | Spring Boot 내부 `@Scheduled` |

- O22. MVP에서는 별도 워커 서비스 없이 백엔드 프로세스 내부 스케줄러를 사용한다.
- O23. 운영 환경에서 백엔드 인스턴스를 여러 개 띄우는 경우 중복 스케줄 실행 방지 전략을 별도로 정의해야 한다.
- O24. MVP 단일 인스턴스 배포에서는 `@Scheduled`만으로 자동 수집을 시작한다.
- O25. 운영 배포 전 `APP_CORS_ALLOWED_ORIGINS`가 운영 프론트엔드 도메인으로 제한되어야 한다.

## 8. 스케줄러 운영 기준

- O26. 자동 스냅샷 수집은 `doc/domain/snapshot_policy.md`의 정책을 따른다.
- O27. 기본 실행 시각은 한국 시간 기준 매일 04:00이다.
- O28. 일부 캐릭터 수집 실패가 전체 배치를 중단하지 않아야 한다.
- O29. 실패한 캐릭터는 기존 데이터를 유지하고 성공 시각처럼 `last_fetched_at`을 갱신하지 않는다.
- O30. 재시도 가능한 자동 수집 실패는 PostgreSQL 기반 retry job으로 저장하고 설정된 backoff와 최대 시도 횟수에 따라 재처리한다.
- O30a. 최대 시도 횟수를 넘긴 작업은 dead-letter 상태로 남기며 자동으로 다시 호출하지 않는다.
- O30b. 수동 새로고침 실패는 background retry job을 만들지 않는다.
- O31. 운영 로그에는 실패한 캐릭터 식별자, 실패 원인, 발생 시각을 남긴다.

## 9. 로그와 관측성

MVP에서 최소로 남겨야 하는 로그는 아래와 같다.

| 이벤트 | 로그 내용 |
| :--- | :--- |
| 캐릭터 최초 등록 성공 | 캐릭터 이름, ocid, snapshot date |
| Nexon API 실패 | API 종류, HTTP status, retryable 여부 |
| 수동 새로고침 성공/실패 | 캐릭터 이름, 요청 시각, 결과 |
| 자동 수집 시작/종료 | 대상 수, 성공 수, 실패 수 |
| 수집 실행 이력 | run id, trigger, 상태, 대상/성공/실패 수 |
| retry job 상태 변경 | job id, 상태, 시도 횟수, error code |
| 중복 스케줄 실행 건너뜀 | 대기 시간, 제한 초과 여부, 건너뛴 사유 |
| 이벤트 생성 | 캐릭터 이름, snapshot id, event type |

- O32. 로그에 Nexon API Key, DB 비밀번호, 전체 원본 응답을 남기지 않는다.
- O33. 원본 JSON은 필요한 경우 DB의 JSONB 필드에 저장하되 로그에는 요약만 남긴다.
- O34. 사용자가 보는 오류 메시지와 개발자 로그 메시지는 구분한다.
- O35. `GET /api/v1/operations/collections`는 원본 Nexon 응답, stack trace, 비밀값을 반환하지 않는다.

## 10. 배포 전 체크리스트

- AC1. `NEXON_API_KEY`가 백엔드 환경에만 존재한다.
- AC2. 프론트엔드 빌드 결과에 Nexon API Key 또는 DB 접속 정보가 포함되지 않는다.
- AC3. 운영 DB에 `database/schema.sql`이 적용되어 있다.
- AC4. `APP_TIMEZONE`이 `Asia/Seoul`로 설정되어 있다.
- AC5. `APP_SNAPSHOT_CRON`이 `0 0 4 * * *` 또는 동등한 04:00 KST 실행값으로 설정되어 있다.
- AC6. 운영 CORS origin이 실제 프론트엔드 도메인으로 제한되어 있다.
- AC7. 캐릭터 최초 검색이 캐릭터 저장과 당일 스냅샷 저장까지 성공한다.
- AC8. 같은 날짜 수동 새로고침이 새 row를 만들지 않고 당일 스냅샷을 갱신한다.
- AC9. Nexon API 실패 시 기존 데이터가 있는 대시보드는 유지된다.
- AC10. 자동 수집 로그에서 대상 수, 성공 수, 실패 수를 확인할 수 있다.
- AC11. 중복 실행 시 대기 상한, 제한 초과 후 건너뛰기, 잘못된 대기값에 대한 시작 실패를 확인할 수 있다.
- AC12. retryable 자동 수집 실패가 실행 이력과 pending retry job에 함께 기록된다.
- AC13. retry job이 성공하면 succeeded 상태가 되고, 최대 시도 횟수를 넘기면 dead-letter 상태가 된다.
- AC14. 올바른 운영 토큰 없이 운영 상태 API를 호출하면 수집 데이터가 반환되지 않는다.

## 11. MVP 이후 재검토 항목

- 다중 백엔드 인스턴스에서 스케줄러 중복 실행 방지
- 관리자용 수집 상태 대시보드와 operator-triggered replay
- 장기 스냅샷 보관/압축/삭제 정책
- preview 환경과 테스트 DB 자동 생성
- 외부 관측성 도구 연동

## 12. 관련 문서

- `doc/plans/mvp_requirements.md`
- `doc/plans/implementation_plan.md`
- `doc/domain/snapshot_policy.md`
- `doc/api/api_contract.md`
- `doc/ui/ui_states.md`
- `doc/db/schema_design.md`
- `database/schema.sql`
