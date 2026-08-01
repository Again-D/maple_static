# [Plan] 메이플스토리 캐릭터 성장 분석 웹 서비스 (Maple Growth Tracker)

메이플스토리 캐릭터의 스펙 성장 과정(전투력, 레벨, 장비 교체, 헥사스탯, 유니온 등)을 시각화하고 일별 스냅샷을 수집/비교해 주는 웹 서비스 구축 계획입니다.

---

## 🏗️ 최종 확정 기술 스택 (Tech Stack)

| 레이어 | 선택 기술 | 주요 역할 및 특징 |
| :--- | :--- | :--- |
| **Backend** | **Spring Boot 3.x (Java 17/21)** | REST API 서버, 넥슨 OpenAPI 병렬 통신, 매일 새벽 스냅샷 배치 스케줄러 (`@Scheduled`), API Key 은닉 |
| **Frontend** | **Next.js 14+ (TypeScript)** | 성장 분석 대시보드 UI, Recharts 성장 그래프, 장비 타임라인, OpenGraph 썸네일 지원 |
| **Database** | **Supabase (PostgreSQL)** | 캐릭터 정보 및 일일 성장 스냅샷, 장비 교체 이력 저장 (무료 플랜 500MB 활용) |
| **Styling** | **Vanilla CSS (CSS Modules)** | 메이플 특유의 프리미엄 다크 테마, 글래스모피즘, 모던 대시보드 연출 |

---

## 📁 프로젝트 폴더 구조 계획

```
Maple/
├── backend/                  # Spring Boot 3.x 애플리케이션
│   ├── src/main/java/com/maple/growth/
│   │   ├── config/           # WebClient, CorsConfig 등
│   │   ├── controller/       # REST API 컨트롤러
│   │   ├── dto/              # Nexon API 및 Response DTO
│   │   ├── entity/           # JPA 엔티티 (Character, DailySnapshot, EventLog)
│   │   ├── repository/       # Spring Data JPA 리포지토리
│   │   ├── service/          # 넥슨 API 통신 & 성장 Diff 비교 비즈니스 로직
│   │   └── scheduler/        # 매일 새벽 일일 스냅샷 수집 스케줄러
│   └── src/main/resources/
│       └── application.yml   # 넥슨 API Key 및 Supabase DB 연동 설정
│
├── frontend/                 # Next.js 14 (TypeScript) 애플리케이션
│   ├── app/                  # App Router 페이지 (메인 검색, 캐릭터 성장 대시보드)
│   ├── components/           # UI 컴포넌트 (성장 그래프, 타임라인 카드, 장비 비교)
│   ├── lib/                  # Spring Boot REST API 통신 클라이언트 모듈
│   └── styles/               # CSS Modules & Global 디자인 토큰
│
└── database/                 # Supabase PostgreSQL DDL 스크립트
    └── schema.sql            # 테이블 및 인덱스 정의
```

---

## 📋 단계별 실행 계획 (Proposed Changes)

### Phase 1: 프로젝트 뼈대 및 데이터베이스 세팅
- [NEW] Supabase PostgreSQL 연동을 위한 DDL 스크립트 (`database/schema.sql`) 정의
- [NEW] Spring Boot 3.x 백엔드 프로젝트 초기화 (`backend/`)
- [NEW] Next.js 14 프론트엔드 프로젝트 초기화 (`frontend/`)
- [NEW] 로컬/운영 환경 변수와 배포 기준은 `doc/ops/env_and_deployment.md`를 따른다.

### Phase 2: Spring Boot 백엔드 개발
- 넥슨 OpenAPI 통합 호출 모듈 (`NexonApiClient` via `WebClient`)
- JPA 엔티티 (`CharacterEntity`, `DailySnapshotEntity`, `GrowthEventLogEntity`) 및 Repository 작성
- 스펙 비교(Diff) 알고리즘 서비스 구현 (이전 스냅샷 vs 오늘 스냅샷 비교하여 레벨, 전투력, 교체된 장비 감지)
- REST API 컨트롤러 구현 (`GET /api/v1/characters/{name}`, dashboard/history/events/refresh 계약은 `doc/api/api_contract.md` 참조)
- `@Scheduled` 스케줄러 작성 (등록된 캐릭터 매일 새벽 04:00 KST 스냅샷 자동 저장, 세부 기준은 `doc/domain/snapshot_policy.md` 참조)

### Phase 3: Next.js 프론트엔드 개발
- 디자인 시스템 (Dark glassmorphism theme, Maple gold/cyan accent colors, CSS Variables) 설정
- 메인 검색 페이지 (`/`) 및 캐릭터 성장 분석 페이지 (`/character/[name]`)
- 성장 추이 인터랙티브 차트 (Recharts) & 장비 교체/성장 타임라인 UI 컴포넌트 개발

### Phase 4: 통합 및 검증 (Verification)
- Spring Boot REST API와 Next.js 연동 테스트
- 캐릭터 검색 -> 넥슨 API Fetch -> DB 저장 -> 성장 Diff 분석 -> UI 시각화 전체 흐름 수동 및 자동 검증

---

## 🧪 검증 계획 (Verification Plan)

### 1. 백엔드 검증 (Spring Boot)
- Spring Boot 애플리케이션 빌드 (`./gradlew build` 또는 `./mvnw package`)
- 넥슨 OpenAPI 호출 테스트 및 DB 스냅샷 정상 누적 확인.
- 백엔드 비밀값과 스케줄러 설정은 `doc/ops/env_and_deployment.md`의 배포 전 체크리스트로 확인.

### 2. 프론트엔드 검증 (Next.js)
- `npm run build`를 통한 TypeScript 및 JSX 문법 오류 검증.
- `http://localhost:3000` 접속 후 캐릭터 검색 및 차트 렌더링 확인.
