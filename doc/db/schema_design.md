# 🗄️ Database Schema Design (메이플스토리 성장 분석 서비스)

본 문서는 **Maple Growth Tracker** 서비스의 데이터베이스 구조, 테이블 설계, 인덱싱 전략 및 JSONB 데이터 포맷을 설명합니다.

---

## 🏗️ 1. 개요 (Overview)

* **Database Engine**: PostgreSQL 15+ (Supabase)
* **주요 목적**:
  1. 캐릭터 기본 인적사항 및 넥슨 `ocid` 관리
  2. 일별 스탯, 경험치, 전투력, 유니온, 헥사스탯 및 장비 스냅샷 누적
  3. 스냅샷 간 차이(Diff) 감지를 통한 성장 이벤트 타임라인 기록

---

## 📊 2. ERD & 테이블 구조 (Table Schemas)

```
+-------------------+       +-----------------------+       +------------------------+
|    characters     | 1   N |    daily_snapshots    | 1   N |   growth_event_logs    |
+-------------------+-------+-----------------------+-------+------------------------+
| id (PK, UUID)     |<------| id (PK, BIGINT)       |<------| id (PK, BIGINT)        |
| ocid (UNIQUE)     |       | character_id (FK)     |       | character_id (FK)      |
| character_name    |       | snapshot_date         |       | snapshot_id (FK)       |
| world_name        |       | level, exp, exp_rate  |       | event_type             |
| job_name          |       | combat_power          |       | title, description     |
| image_url         |       | raw_equipment_json    |       | detail_json            |
+-------------------+       +-----------------------+       +------------------------+
```

---

## 📑 3. 상세 테이블 스펙

### 3.1. `characters` (캐릭터 마스터 테이블)
| 컬럼명 | 데이터 타입 | 제약 조건 | 설명 |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY, DEFAULT uuid_generate_v4() | 내부 캐릭터 고유 식별자 |
| `ocid` | VARCHAR(100) | UNIQUE, NOT NULL | 넥슨 OpenAPI 고유 캐릭터 ID |
| `character_name` | VARCHAR(50) | UNIQUE, NOT NULL, INDEX | 캐릭터 닉네임 |
| `world_name` | VARCHAR(50) | NOT NULL | 월드/서버 이름 (예: 루나, 크로아) |
| `job_name` | VARCHAR(50) | NOT NULL | 직업 (예: 신궁, 히어로) |
| `character_gender`| VARCHAR(10) | | 성별 |
| `character_image_url`| TEXT | | 넥슨 아바타 이미지 CDN URL |
| `is_auto_track` | BOOLEAN | DEFAULT TRUE | 매일 새벽 배치 수집 대상 여부 |
| `last_fetched_at` | TIMESTAMP | | 마지막 넥슨 API 동기화 시각 |
| `last_sync_attempted_at` | TIMESTAMP | | 마지막 동기화 시도 시각 |
| `last_sync_error_code` | VARCHAR(50) | | 마지막 동기화 실패 코드 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 최초 검색/등록 일시 |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 수정 일시 |

### 3.2. `daily_snapshots` (일일 성장 스냅샷 테이블)
| 컬럼명 | 데이터 타입 | 제약 조건 | 설명 |
| :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PRIMARY KEY | 스냅샷 고유 식별자 |
| `character_id` | UUID | FOREIGN KEY (characters.id) | 캐릭터 FK |
| `snapshot_date` | DATE | NOT NULL | 스냅샷 수집 날짜 (YYYY-MM-DD) |
| `level` | INT | NOT NULL | 캐릭터 레벨 |
| `exp` | BIGINT | NOT NULL | 현재 경험치 수치 |
| `exp_rate` | NUMERIC(7, 4)| | 경험치 백분율 (예: 85.1234%) |
| `combat_power` | BIGINT | NULL | 전투력 수치 |
| `union_level` | INT | NULL | 유니온 레벨 |
| `union_artifact_level` | INT | NULL | 유니온 아티팩트 레벨 |
| `hexa_matrix_level_sum` | INT | NULL | 헥사코어 레벨 합산 |
| `raw_stat_json` | JSONB | | 스탯 상세 원본 JSON (주스탯, 보공, 방무 등) |
| `raw_equipment_json` | JSONB | | 장비 착용 정보 원본 JSON |
| `raw_hexa_json` | JSONB | | 헥사 매트릭스 상세 원본 JSON |
| `captured_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 가장 최근 대표 수집 시각 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 생성 시각 |

* **복합 유니크 제약**: `UNIQUE(character_id, snapshot_date)` (캐릭터당 하루 1개의 스냅샷만 허용)

### 3.3. `growth_event_logs` (성장 이벤트 타임라인 로그)
| 컬럼명 | 데이터 타입 | 제약 조건 | 설명 |
| :--- | :--- | :--- | :--- |
| `id` | BIGSERIAL | PRIMARY KEY | 이벤트 로그 ID |
| `character_id` | UUID | FOREIGN KEY (characters.id) | 캐릭터 FK |
| `snapshot_id` | BIGINT | FOREIGN KEY (daily_snapshots.id) | 감지된 스냅샷 FK |
| `event_date` | DATE | NOT NULL | 이벤트 발생 일자 |
| `event_type` | VARCHAR(50) | NOT NULL | `LEVEL_UP`, `COMBAT_POWER_CHANGE`, `ITEM_REPLACED`, `HEXA_UPGRADED`, `UNION_UPGRADED` |
| `event_key` | VARCHAR(255) | NOT NULL | 이벤트 중복 방지용 안정 키 |
| `title` | VARCHAR(255)| NOT NULL | 타임라인 제목 (예: "Lv.285 → Lv.286 레벨업!") |
| `description` | TEXT | | 변동 상세 내역 문구 |
| `detail_json` | JSONB | | 이전/이후 차이(Diff) 데이터 JSON |
| `importance_level`| INT | DEFAULT 1 | 1: 일반, 2: 장비교체, 3: 레벨업/주요 업적 |

---

## ⚡ 4. 인덱스 최적화 전략 (Indexes)

1. **`idx_characters_name`**: `characters(character_name)`
   - 유저가 닉네임 검색 시 `SELECT * FROM characters WHERE character_name = ?` 쿼리 최고속 처리.
2. **`idx_snapshots_character_date`**: `daily_snapshots(character_id, snapshot_date DESC)`
   - 최근 7일/30일 성장 그래프용 스냅샷 조회 시 인덱스 스캔 처리.
3. **`idx_event_logs_character_date`**: `growth_event_logs(character_id, event_date DESC)`
   - 캐릭터 타임라인 피드 생성 시 빠른 최신순 페이징 지원.
