-- 메이플스토리 캐릭터 성장 분석 서비스 (Maple Growth Tracker) DB DDL Schema
-- Database: PostgreSQL (Supabase Compatible)

-- 1. UUID 확장 모듈 활성화
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. 캐릭터 테이블 (characters)
CREATE TABLE IF NOT EXISTS characters (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ocid VARCHAR(100) UNIQUE NOT NULL,
    character_name VARCHAR(50) UNIQUE NOT NULL,
    world_name VARCHAR(50) NOT NULL,
    job_name VARCHAR(50) NOT NULL,
    character_gender VARCHAR(10),
    character_image_url TEXT,
    is_auto_track BOOLEAN DEFAULT TRUE,
    last_fetched_at TIMESTAMP WITH TIME ZONE,
    last_sync_attempted_at TIMESTAMP WITH TIME ZONE,
    last_sync_error_code VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스: 닉네임 빠른 검색
CREATE INDEX IF NOT EXISTS idx_characters_name ON characters(character_name);

-- 3. 일일 성장 스냅샷 테이블 (daily_snapshots)
CREATE TABLE IF NOT EXISTS daily_snapshots (
    id BIGSERIAL PRIMARY KEY,
    character_id UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    snapshot_date DATE NOT NULL,
    
    -- 기본 스탯 및 성과 수치
    level INT NOT NULL,
    exp BIGINT NOT NULL,
    exp_rate NUMERIC(7, 4), -- 경험치 백분율 (예: 85.1234%)
    combat_power BIGINT, -- 전투력 수치
    union_level INT, -- 유니온 레벨
    union_artifact_level INT, -- 유니온 아티팩트 레벨
    hexa_matrix_level_sum INT, -- 헥사코어 레벨 합산

    -- 원본 데이터 저장 (Diff 계산 및 세부 분석용 JSONB)
    raw_stat_json JSONB,      -- 주스탯, 보공, 방무, 크뎀 등 스탯 상세
    raw_equipment_json JSONB, -- 장비 착용 정보
    raw_hexa_json JSONB,      -- 헥사 매트릭스 상세

    captured_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- 캐릭터당 하루에 하나의 스냅샷만 유지
    CONSTRAINT uq_character_snapshot_date UNIQUE (character_id, snapshot_date)
);

-- 인덱스: 캐릭터ID + 날짜별 검색 및 정렬 속도 최적화
CREATE INDEX IF NOT EXISTS idx_snapshots_character_date ON daily_snapshots(character_id, snapshot_date DESC);

-- 4. 성장 이벤트 로그 테이블 (growth_event_logs)
-- 이전 스냅샷과 오늘 스냅샷을 비교(Diff)하여 감지된 성장/장비교체 이벤트 저장
CREATE TABLE IF NOT EXISTS growth_event_logs (
    id BIGSERIAL PRIMARY KEY,
    character_id UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    snapshot_id BIGINT NOT NULL REFERENCES daily_snapshots(id) ON DELETE CASCADE,
    event_date DATE NOT NULL,
    
    -- 이벤트 종류: LEVEL_UP, COMBAT_POWER_CHANGE, ITEM_REPLACED, HEXA_UPGRADED, UNION_UPGRADED
    event_type VARCHAR(50) NOT NULL,
    event_key VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL, -- 예: "Lv.285 → Lv.286 레벨업!" 또는 "아케인셰이드 글러브 교체"
    description TEXT, -- 상세 변동 내역
    detail_json JSONB, -- 이전/이후 장비 정보나 스탯 차이 JSON
    importance_level INT DEFAULT 1, -- 1: 일반, 2: 중요(장비교체), 3: 주요 업적(레벨업/전투력 급증)

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스: 캐릭터별 타임라인 이벤트 조회 최적화
CREATE INDEX IF NOT EXISTS idx_event_logs_character_date ON growth_event_logs(character_id, event_date DESC);

-- 이벤트 중복 방지
CREATE UNIQUE INDEX IF NOT EXISTS uq_growth_event_snapshot_type_key
    ON growth_event_logs(snapshot_id, event_type, event_key);

-- 5. 수집 실행 이력 (collection_runs)
CREATE TABLE IF NOT EXISTS collection_runs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trigger_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    target_count INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    failure_count INT NOT NULL DEFAULT 0,
    retry_queued_count INT NOT NULL DEFAULT 0,
    skip_reason VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_collection_run_counts CHECK (
        target_count >= 0 AND success_count >= 0 AND failure_count >= 0 AND retry_queued_count >= 0
    )
);

CREATE INDEX IF NOT EXISTS idx_collection_runs_started_at ON collection_runs(started_at DESC);

-- 6. 재시도 작업 (collection_retry_jobs)
CREATE TABLE IF NOT EXISTS collection_retry_jobs (
    id BIGSERIAL PRIMARY KEY,
    character_id UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    source_run_id UUID REFERENCES collection_runs(id) ON DELETE SET NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    claimed_at TIMESTAMP WITH TIME ZONE,
    claim_token UUID,
    last_attempted_at TIMESTAMP WITH TIME ZONE,
    last_error_code VARCHAR(50),
    last_error_message VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_collection_retry_attempt_count CHECK (attempt_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_collection_retry_due
    ON collection_retry_jobs(status, next_attempt_at, id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_collection_retry_active_character
    ON collection_retry_jobs(character_id)
    WHERE status IN ('PENDING', 'CLAIMED');
