# API Contract - Maple Growth Tracker MVP

## 1. 목적

이 문서는 Maple Growth Tracker MVP에서 프론트엔드와 백엔드가 주고받을 REST API 계약을 정의한다.
목표는 캐릭터 검색, 대시보드 조회, 수동 새로고침, 성장 차트, 이벤트 타임라인을 구현할 때 응답 형태와 실패 상태를 임의로 해석하지 않게 하는 것이다.

## 2. 공통 규칙

- A1. 모든 API 응답은 JSON을 사용한다.
- A2. 시간 값은 ISO 8601 문자열로 전달한다.
- A3. 날짜 값은 `YYYY-MM-DD` 문자열로 전달한다.
- A4. 날짜와 "오늘" 판단은 한국 시간(`Asia/Seoul`) 기준이다.
- A5. 숫자 값은 포맷된 문자열이 아니라 number로 전달한다.
- A6. 화면 표시용 천 단위 구분, 색상, 부호 표시는 프론트엔드가 담당한다.
- A7. 백엔드는 사용자가 이해해야 하는 상태를 `state`와 `message`로 명시한다.
- A8. Nexon API 원본 JSON 전체는 MVP 응답에 그대로 노출하지 않는다.

## 3. 공통 응답 래퍼

성공 응답은 아래 형태를 따른다.

```json
{
  "success": true,
  "data": {},
  "meta": {
    "serverTime": "2026-08-02T04:10:00+09:00",
    "timezone": "Asia/Seoul"
  }
}
```

실패 응답은 아래 형태를 따른다.

```json
{
  "success": false,
  "error": {
    "code": "CHARACTER_NOT_FOUND",
    "message": "캐릭터를 찾을 수 없습니다.",
    "retryable": false
  },
  "meta": {
    "serverTime": "2026-08-02T04:10:00+09:00",
    "timezone": "Asia/Seoul"
  }
}
```

## 4. 오류 코드

| HTTP Status | Code | Retryable | 설명 |
| :--- | :--- | :--- | :--- |
| 400 | `INVALID_CHARACTER_NAME` | false | 닉네임이 비어 있거나 허용되지 않는 형식이다. |
| 404 | `CHARACTER_NOT_FOUND` | false | Nexon OpenAPI에서 캐릭터를 찾을 수 없다. |
| 429 | `RATE_LIMITED` | true | 외부 API 또는 서비스 호출 제한에 걸렸다. |
| 502 | `NEXON_API_AUTH_FAILED` | false | Nexon OpenAPI 키가 유효하지 않다. |
| 502 | `NEXON_API_FAILED` | true | Nexon OpenAPI 호출이 실패했다. |
| 503 | `NEXON_API_UNAVAILABLE` | true | Nexon OpenAPI가 일시적으로 사용할 수 없다. |
| 403 | `OPERATIONS_ACCESS_DENIED` | false | 운영 상태 API 토큰이 없거나 올바르지 않다. |
| 500 | `INTERNAL_ERROR` | true | 서비스 내부 오류다. |

## 5. 공통 데이터 타입

### 5.1. CharacterProfile

```json
{
  "id": "8e9c2d4a-6c8b-4b2a-b3b5-c5f2f6f1d222",
  "ocid": "nexon-ocid",
  "name": "Aries92",
  "worldName": "루나",
  "jobName": "나이트로드",
  "gender": "male",
  "imageUrl": "https://example.cdn/character.png",
  "isAutoTrack": true
}
```

### 5.2. SnapshotSummary

```json
{
  "snapshotId": 123,
  "snapshotDate": "2026-08-02",
  "level": 278,
  "exp": 123456789,
  "expRate": 42.1234,
  "combatPower": 7420500,
  "unionLevel": 8500,
  "unionArtifactLevel": 42,
  "hexaMatrixLevelSum": 135,
  "capturedAt": "2026-08-02T04:00:12+09:00"
}
```

### 5.3. SyncState

```json
{
  "state": "fresh",
  "lastSuccessAt": "2026-08-02T04:00:12+09:00",
  "lastAttemptAt": "2026-08-02T04:00:12+09:00",
  "message": "오늘 04:00 수집됨"
}
```

허용되는 `state` 값:

| State | 의미 |
| :--- | :--- |
| `fresh` | 오늘 기준 최신 수집이 성공했다. |
| `stale` | 저장된 데이터는 있지만 오늘 수집 성공 기록이 없다. |
| `refreshing` | 수동 새로고침 처리 중이다. |
| `failed_with_cache` | 최신 수집은 실패했지만 기존 데이터를 표시할 수 있다. |
| `failed_empty` | 표시할 기존 데이터가 없고 수집도 실패했다. |

### 5.4. GrowthSummary

```json
{
  "rangeDays": 7,
  "hasEnoughSnapshots": true,
  "combatPowerDelta": 1420500,
  "combatPowerDeltaRate": 1.8,
  "levelFrom": 277,
  "levelTo": 278,
  "expRateFrom": 35.2,
  "expRateTo": 42.1,
  "unionLevelDelta": 120,
  "hexaMatrixLevelDelta": 3,
  "eventCount": 4
}
```

스냅샷이 부족하면 `hasEnoughSnapshots`는 `false`이고 delta 값은 `null`일 수 있다.

### 5.5. ChartPoint

```json
{
  "snapshotDate": "2026-08-02",
  "combatPower": 7420500,
  "level": 278,
  "expRate": 42.1234,
  "unionLevel": 8500,
  "hexaMatrixLevelSum": 135
}
```

### 5.6. GrowthEvent

```json
{
  "id": 987,
  "eventDate": "2026-08-02",
  "eventType": "COMBAT_POWER_CHANGE",
  "importanceLevel": 1,
  "title": "전투력 1,420,500 상승",
  "description": "7,420,500 -> 8,841,000",
  "detail": {
    "from": 7420500,
    "to": 8841000,
    "delta": 1420500,
    "deltaRate": 1.8,
    "direction": "up"
  }
}
```

## 6. API 목록

### 6.1. 캐릭터 조회 또는 최초 등록

`GET /api/v1/characters/{name}`

닉네임으로 캐릭터를 조회한다.
DB에 없는 캐릭터는 Nexon OpenAPI에서 조회해 저장하고, 당일 대표 스냅샷을 생성한다.

성공 응답:

```json
{
  "success": true,
  "data": {
    "profile": {
      "id": "8e9c2d4a-6c8b-4b2a-b3b5-c5f2f6f1d222",
      "ocid": "nexon-ocid",
      "name": "Aries92",
      "worldName": "루나",
      "jobName": "나이트로드",
      "gender": "male",
      "imageUrl": "https://example.cdn/character.png",
      "isAutoTrack": true
    },
    "latestSnapshot": {
      "snapshotId": 123,
      "snapshotDate": "2026-08-02",
      "level": 278,
      "exp": 123456789,
      "expRate": 42.1234,
      "combatPower": 7420500,
      "unionLevel": 8500,
      "unionArtifactLevel": 42,
      "hexaMatrixLevelSum": 135,
      "capturedAt": "2026-08-02T04:00:12+09:00"
    },
    "syncState": {
      "state": "fresh",
      "lastSuccessAt": "2026-08-02T04:00:12+09:00",
      "lastAttemptAt": "2026-08-02T04:00:12+09:00",
      "message": "오늘 04:00 수집됨"
    }
  },
  "meta": {
    "serverTime": "2026-08-02T04:10:00+09:00",
    "timezone": "Asia/Seoul"
  }
}
```

오류 응답:

- `INVALID_CHARACTER_NAME`
- `CHARACTER_NOT_FOUND`
- `RATE_LIMITED`
- `NEXON_API_AUTH_FAILED`
- `NEXON_API_FAILED`
- `NEXON_API_UNAVAILABLE`

### 6.2. 캐릭터 대시보드 조회

`GET /api/v1/characters/{name}/dashboard`

대시보드 첫 화면에 필요한 데이터를 한 번에 조회한다.
프론트엔드는 MVP에서 이 API를 기본 화면 데이터 소스로 사용한다.

성공 응답:

```json
{
  "success": true,
  "data": {
    "profile": {},
    "latestSnapshot": {},
    "summary": {
      "rangeDays": 7,
      "hasEnoughSnapshots": true,
      "combatPowerDelta": 1420500,
      "combatPowerDeltaRate": 1.8,
      "levelFrom": 277,
      "levelTo": 278,
      "expRateFrom": 35.2,
      "expRateTo": 42.1,
      "unionLevelDelta": 120,
      "hexaMatrixLevelDelta": 3,
      "eventCount": 4
    },
    "chart": {
      "rangeDays": 7,
      "points": []
    },
    "timeline": {
      "events": [],
      "hasMore": false
    },
    "equipment": {
      "available": true,
      "snapshotDate": "2026-08-02",
      "capturedAt": "2026-08-02T04:00:12+09:00",
      "items": []
    },
    "syncState": {}
  },
  "meta": {
    "serverTime": "2026-08-02T04:10:00+09:00",
    "timezone": "Asia/Seoul"
  }
}
```

`equipment.items` contains normalized active non-cash equipment from the latest successful snapshot. Raw Nexon equipment JSON is not exposed. Optional item detail groups are omitted or empty when the source value is unavailable; the frontend must not infer zero values.

데이터 부족 응답도 HTTP 200으로 반환한다.
이 경우 `summary.hasEnoughSnapshots`는 `false`, `chart.points`는 가능한 만큼만 포함하고, `timeline.events`는 빈 배열일 수 있다.

### 6.3. 성장 이력 조회

`GET /api/v1/characters/{name}/growth-history?range=7d&metric=combatPower`

차트에 사용할 스냅샷 시계열을 조회한다.
허용하는 `range` 값은 `7d`, `30d`, `all`이다.
허용하는 `metric` 값은 `combatPower`, `level`, `expRate`, `unionLevel`, `hexaMatrixLevelSum`이다.
`range`, `metric`을 생략하면 기본값은 `7d` + `combatPower`다.

성공 응답:

```json
{
  "success": true,
  "data": {
    "range": "7d",
    "metric": "combatPower",
    "hasEnoughSnapshots": true,
    "points": [
      {
        "snapshotDate": "2026-08-01",
        "combatPower": 7300000,
        "level": 277,
        "expRate": 88.42,
        "unionLevel": 8380,
        "hexaMatrixLevelSum": 132
      },
      {
        "snapshotDate": "2026-08-02",
        "combatPower": 7420500,
        "level": 278,
        "expRate": 42.1234,
        "unionLevel": 8500,
        "hexaMatrixLevelSum": 135
      }
    ]
  },
  "meta": {
    "serverTime": "2026-08-02T04:10:00+09:00",
    "timezone": "Asia/Seoul"
  }
}
```

### 6.4. 이벤트 타임라인 조회

`GET /api/v1/characters/{name}/events?limit=20`

캐릭터의 성장 이벤트를 최신순으로 조회한다.

성공 응답:

```json
{
  "success": true,
  "data": {
    "events": [
      {
        "id": 987,
        "eventDate": "2026-08-02",
        "eventType": "LEVEL_UP",
        "importanceLevel": 3,
        "title": "Lv.277 -> Lv.278 레벨업",
        "description": "이전 대표 스냅샷 대비 레벨이 상승했습니다.",
        "detail": {
          "fromLevel": 277,
          "toLevel": 278,
          "delta": 1
        }
      }
    ],
    "hasMore": false,
    "nextCursor": null
  },
  "meta": {
    "serverTime": "2026-08-02T04:10:00+09:00",
    "timezone": "Asia/Seoul"
  }
}
```

### 6.5. 수동 새로고침

`ITEM_REPLACED` 이벤트의 `detail`은 다음 형태를 사용한다. 여러 슬롯 변경도 이벤트 한 건의 `changes` 배열로 묶으며, preset/옵션/스타포스 비교 결과는 포함하지 않는다.

```json
{
  "changeCount": 2,
  "changes": [
    { "slot": "무기", "previousItemName": "아케인 스태프", "currentItemName": "에테르넬 스태프" },
    { "slot": "신발", "previousItemName": "아케인 슈즈", "currentItemName": "에테르넬 슈즈" }
  ]
}
```

`POST /api/v1/characters/{name}/refresh`

Nexon OpenAPI에서 최신 데이터를 가져와 당일 대표 스냅샷을 생성하거나 갱신한다.
성공 시 이전 날짜 대표 스냅샷과 비교해 이벤트를 재계산한다.

성공 응답:

```json
{
  "success": true,
  "data": {
    "profile": {},
    "latestSnapshot": {},
    "createdSnapshot": false,
    "updatedSnapshot": true,
    "createdEventCount": 2,
    "syncState": {
      "state": "fresh",
      "lastSuccessAt": "2026-08-02T12:30:00+09:00",
      "lastAttemptAt": "2026-08-02T12:30:00+09:00",
      "message": "오늘 12:30 수집됨"
    }
  },
  "meta": {
    "serverTime": "2026-08-02T12:30:01+09:00",
    "timezone": "Asia/Seoul"
  }
}
```

실패 응답은 공통 실패 래퍼를 사용한다.
기존 데이터가 있는 경우 프론트엔드는 기존 대시보드 상태를 유지하고 실패 안내를 표시한다.

## 7. 프론트엔드 상태 매핑

| API 상황 | HTTP | 프론트 상태 |
| :--- | :--- | :--- |
| 캐릭터 검색 중 | pending | 검색 로딩 |
| 캐릭터 조회 성공, 스냅샷 1개 | 200 | 대시보드 + 데이터 부족 안내 |
| 캐릭터 조회 성공, 스냅샷 2개 이상 | 200 | 완전한 MVP 대시보드 |
| 캐릭터 없음 | 404 | 캐릭터 없음 상태 |
| API 실패, 기존 데이터 있음 | 502/503 | 기존 대시보드 유지 + 실패 배너 |
| API 실패, 기존 데이터 없음 | 502/503 | 재시도 가능한 오류 상태 |
| 수동 새로고침 중 | pending | 새로고침 버튼 로딩 |
| 수동 새로고침 성공 | 200 | 대시보드 데이터 갱신 |
| 수동 새로고침 실패 | 429/502/503 | 기존 데이터 유지 + 실패 안내 |

## 8. API별 수용 기준

- AC1. `GET /api/v1/characters/{name}`는 캐릭터 프로필, 최신 스냅샷, 동기화 상태를 반환한다.
- AC2. DB에 없는 캐릭터를 처음 조회하면 성공 시 캐릭터와 당일 대표 스냅샷이 생성된다.
- AC3. `GET /api/v1/characters/{name}/dashboard`는 MVP 대시보드 첫 화면에 필요한 데이터를 한 번에 반환한다.
- AC4. 스냅샷이 2개 미만이어도 대시보드 API는 200을 반환하고 데이터 부족 상태를 표현한다.
- AC5. `GET /api/v1/characters/{name}/growth-history`는 `range in {7d,30d,all}`와 `metric in {combatPower,level,expRate,unionLevel,hexaMatrixLevelSum}`를 지원하고, 응답에 `range`, `metric`, `hasEnoughSnapshots`, `points`를 포함한다.
- AC6. `GET /api/v1/characters/{name}/events`는 이벤트를 최신순으로 반환한다.
- AC7. `POST /api/v1/characters/{name}/refresh`는 당일 대표 스냅샷을 생성하거나 갱신한다.
- AC8. 모든 실패 응답은 `error.code`, `error.message`, `error.retryable`을 포함한다.

## 9. 운영 상태 API

### 9.1. 수집 상태 조회

`GET /api/v1/operations/collections?limit=20`

요청에는 `X-Operations-Token` 헤더가 필요하다. `limit`은 1 이상 100 이하이며 최근 수집 실행을 최신순으로 반환한다.

응답에는 실행 상태와 retry job 개수만 포함한다. Nexon 원본 JSON, stack trace, API key, DB credentials는 반환하지 않는다.

```json
{
  "success": true,
  "data": {
    "recentRuns": [
      {
        "id": "8e9c2d4a-6c8b-4b2a-b3b5-c5f2f6f1d222",
        "triggerType": "SCHEDULED",
        "status": "PARTIALLY_FAILED",
        "startedAt": "2026-08-02T04:00:00+09:00",
        "completedAt": "2026-08-02T04:02:00+09:00",
        "targetCount": 3,
        "successCount": 2,
        "failureCount": 1,
        "retryQueuedCount": 1,
        "skipReason": null
      }
    ],
    "pendingRetryCount": 1,
    "claimedRetryCount": 0,
    "succeededRetryCount": 2,
    "deadLetteredRetryCount": 0
  },
  "meta": {
    "serverTime": "2026-08-02T04:10:00+09:00",
    "timezone": "Asia/Seoul"
  }
}
```

### 9.2. 운영 상태 API 실패

운영 토큰이 없거나 일치하지 않으면 HTTP 403과 `OPERATIONS_ACCESS_DENIED`를 반환한다.

## 10. MVP 이후로 미루는 API

- 로그인/회원 API
- 즐겨찾기 API
- 인기 캐릭터 랭킹 API
- OpenGraph 이미지 API
- 장비 옵션/잠재능력/스타포스 상세 Diff API
- 운영자 강제 replay API
- preset-aware 장비 비교 API

## 11. 관련 문서

- `doc/plans/mvp_requirements.md`
- `doc/domain/snapshot_policy.md`
- `doc/domain/growth_event_rules.md`
- `doc/ui/ui_states.md`
- `doc/db/schema_design.md`
- `database/schema.sql`
